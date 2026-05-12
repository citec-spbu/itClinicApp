package com.spbu.projecttrack.rating.data.api

import com.spbu.projecttrack.rating.data.model.RatingSyncIdentifier
import com.spbu.projecttrack.rating.data.model.RatingSyncMember
import com.spbu.projecttrack.rating.data.model.RatingSyncProject
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for the HTTP layer of [MetricApi].
 * Uses Ktor MockEngine — no real network requests are made.
 * Verifies response parsing, HTTP error handling, and request/response logic.
 */
class MetricApiTest {

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    /** Wraps a [MockEngine] in an HttpClient with JSON ContentNegotiation. */
    private fun clientWith(engine: MockEngine): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    /** Creates a client that always responds with the same fixed body. */
    private fun mockClient(
        statusCode: HttpStatusCode = HttpStatusCode.OK,
        body: String = ""
    ): HttpClient = clientWith(MockEngine { respond(body, statusCode, jsonHeaders) })

    /** Creates a [MetricApi] with a fixed response and a test baseUrl. */
    private fun mockApi(
        statusCode: HttpStatusCode = HttpStatusCode.OK,
        body: String = ""
    ): MetricApi = MetricApi(client = mockClient(statusCode, body), baseUrl = "http://test")

    /** Creates a [MetricApi] on top of an arbitrary [MockEngine] with a test baseUrl. */
    private fun apiWith(engine: MockEngine): MetricApi =
        MetricApi(client = clientWith(engine), baseUrl = "http://test")

    /** Reads the outgoing request body as a string (works for JSON sent via setBody). */
    private fun HttpRequestData.bodyAsText(): String = when (val b = body) {
        is TextContent -> b.text
        is ByteArrayContent -> b.bytes().decodeToString()
        else -> ""
    }

    // ─────────────────────────────────────────────
    // getProjects
    // ─────────────────────────────────────────────

    @Test
    fun getProjects_happyPath_parsesProjectList() = runTest {
        val json = """
            [
              {"id":"p1","name":"Проект А","grade":"4.5"},
              {"id":"p2","name":"Проект Б","grade":"3.0"}
            ]
        """.trimIndent()
        val api = mockApi(body = json)

        val result = api.getProjects()

        assertTrue(result.isSuccess)
        val projects = result.getOrThrow()
        assertEquals(2, projects.size)
        assertEquals("p1", projects[0].id)
        assertEquals("Проект А", projects[0].name)
        assertEquals("4.5", projects[0].grade)
        assertEquals("p2", projects[1].id)
    }

    @Test
    fun getProjects_emptyArray_returnsEmptyList() = runTest {
        val api = mockApi(body = "[]")
        val result = api.getProjects()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun getProjects_optionalFieldsMissing_usesDefaults() = runTest {
        val json = """[{"id":"p1","name":"Minimal"}]"""
        val api = mockApi(body = json)
        val result = api.getProjects()
        assertTrue(result.isSuccess)
        val project = result.getOrThrow().first()
        assertNull(project.grade)
        assertNull(project.description)
        assertTrue(project.platforms.isEmpty())
    }

    @Test
    fun getProjects_500Response_returnsFailure() = runTest {
        val api = mockApi(statusCode = HttpStatusCode.InternalServerError)
        val result = api.getProjects()
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message?.contains("500") == true)
    }

    @Test
    fun getProjects_404Response_returnsFailure() = runTest {
        val api = mockApi(statusCode = HttpStatusCode.NotFound)
        val result = api.getProjects()
        assertFalse(result.isSuccess)
    }

    // ─────────────────────────────────────────────
    // getProjectRatings — derived from getProjects
    // ─────────────────────────────────────────────

    @Test
    fun getProjectRatings_sortedByScoreDescending() = runTest {
        val json = """
            [
              {"id":"p1","name":"Низкий","grade":"2.0"},
              {"id":"p2","name":"Высокий","grade":"5.0"},
              {"id":"p3","name":"Средний","grade":"3.5"}
            ]
        """.trimIndent()
        val api = mockApi(body = json)

        val result = api.getProjectRatings()

        assertTrue(result.isSuccess)
        val items = result.getOrThrow()
        assertEquals("p2", items[0].id)
        assertEquals("p3", items[1].id)
        assertEquals("p1", items[2].id)
    }

    @Test
    fun getProjectRatings_nullGradeProjectsLastInRanking() = runTest {
        val json = """
            [
              {"id":"p1","name":"Без оценки"},
              {"id":"p2","name":"С оценкой","grade":"3.0"}
            ]
        """.trimIndent()
        val api = mockApi(body = json)

        val result = api.getProjectRatings()
        assertTrue(result.isSuccess)
        val items = result.getOrThrow()
        // Project with a grade must come first
        assertEquals("p2", items[0].id)
        assertNull(items[1].score)
    }

    @Test
    fun getProjectRatings_nAGradeTreatedAsNull() = runTest {
        val json = """[{"id":"p1","name":"N/A проект","grade":"N/A"}]"""
        val api = mockApi(body = json)

        val result = api.getProjectRatings()
        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow().first().score)
    }

    @Test
    fun getProjectRatings_propagatesHttpFailure() = runTest {
        val api = mockApi(statusCode = HttpStatusCode.ServiceUnavailable)
        val result = api.getProjectRatings()
        assertFalse(result.isSuccess)
    }

    // ─────────────────────────────────────────────
    // getProjectDetail
    // ─────────────────────────────────────────────

    @Test
    fun getProjectDetail_happyPath_parsesDetail() = runTest {
        val json = """
            {
              "id": "p1",
              "name": "Проект А",
              "description": "Описание",
              "users": [
                {
                  "name": "Иванов Иван",
                  "roles": ["Backend"],
                  "identifiers": [{"platform": "github", "value": "ivanov"}]
                }
              ],
              "resources": [
                {
                  "id": "r1",
                  "name": "GitHub репо",
                  "platform": "github",
                  "metrics": []
                }
              ]
            }
        """.trimIndent()
        val api = apiWith(MockEngine { request ->
            // Verify the request targets the correct path
            assertTrue(request.url.encodedPath.endsWith("/p1"))
            respond(json, HttpStatusCode.OK, jsonHeaders)
        })

        val result = api.getProjectDetail("p1")

        assertTrue(result.isSuccess)
        val detail = result.getOrThrow()
        assertEquals("p1", detail.id)
        assertEquals("Проект А", detail.name)
        assertEquals(1, detail.users.size)
        assertEquals("Иванов Иван", detail.users[0].name)
        assertEquals("github", detail.users[0].identifiers[0].platform)
        assertEquals(1, detail.resources.size)
        assertEquals("r1", detail.resources[0].id)
    }

    @Test
    fun getProjectDetail_emptyUsersAndResources_parsesSuccessfully() = runTest {
        val json = """{"id":"p1","name":"Пустой проект"}"""
        val api = mockApi(body = json)

        val result = api.getProjectDetail("p1")
        assertTrue(result.isSuccess)
        val detail = result.getOrThrow()
        assertTrue(detail.users.isEmpty())
        assertTrue(detail.resources.isEmpty())
    }

    @Test
    fun getProjectDetail_404Response_returnsFailure() = runTest {
        val api = mockApi(statusCode = HttpStatusCode.NotFound)
        val result = api.getProjectDetail("nonexistent")
        assertFalse(result.isSuccess)
    }

    @Test
    fun getProjectDetail_usesProjectIdInUrl() = runTest {
        var capturedPath = ""
        val api = apiWith(MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond("""{"id":"abc123","name":"Тест"}""", HttpStatusCode.OK, jsonHeaders)
        })

        api.getProjectDetail("abc123")
        assertTrue(capturedPath.endsWith("/abc123"), "Expected path ending with /abc123, got: $capturedPath")
    }

    // ─────────────────────────────────────────────
    // getStudentRatings
    // ─────────────────────────────────────────────

    @Test
    fun getStudentRatings_happyPath_parsesRankingItems() = runTest {
        val json = """
            [
              {"id":"s1","name":"Иванов Иван","score":4.8},
              {"id":"s2","name":"Петров Пётр","score":3.2}
            ]
        """.trimIndent()
        val api = mockApi(body = json)

        val result = api.getStudentRatings()

        assertTrue(result.isSuccess)
        val items = result.getOrThrow()
        assertEquals(2, items.size)
        assertEquals("s1", items[0].id)
        assertEquals(4.8, items[0].score)
        assertEquals("s2", items[1].id)
    }

    @Test
    fun getStudentRatings_emptyResponse_returnsEmptyList() = runTest {
        val api = mockApi(body = "[]")
        val result = api.getStudentRatings()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun getStudentRatings_nullScore_parsedAsNull() = runTest {
        val json = """[{"id":"s1","name":"Студент без оценки"}]"""
        val api = mockApi(body = json)

        val result = api.getStudentRatings()
        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow().first().score)
    }

    @Test
    fun getStudentRatings_500Response_returnsFailure() = runTest {
        val api = mockApi(statusCode = HttpStatusCode.InternalServerError)
        val result = api.getStudentRatings()
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message?.contains("500") == true)
    }

    // ─────────────────────────────────────────────
    // syncProjects
    // ─────────────────────────────────────────────

    @Test
    fun syncProjects_emptyList_returnsSuccessWithoutHttpCall() = runTest {
        var callCount = 0
        val api = apiWith(MockEngine {
            callCount++
            respond("", HttpStatusCode.OK, jsonHeaders)
        })

        val result = api.syncProjects(emptyList())

        assertTrue(result.isSuccess)
        assertEquals(0, callCount, "No HTTP requests should be made for an empty list")
    }

    @Test
    fun syncProjects_happyPath_returnsSuccess() = runTest {
        val api = mockApi(statusCode = HttpStatusCode.OK)
        val projects = listOf(
            RatingSyncProject(id = "p1", name = "Проект А")
        )

        val result = api.syncProjects(projects)
        assertTrue(result.isSuccess)
    }

    @Test
    fun syncProjects_sendsPostRequest() = runTest {
        var capturedMethod: HttpMethod? = null
        val api = apiWith(MockEngine { request ->
            capturedMethod = request.method
            respond("", HttpStatusCode.OK, jsonHeaders)
        })

        api.syncProjects(listOf(RatingSyncProject(id = "p1", name = "Проект")))

        assertEquals(HttpMethod.Post, capturedMethod)
    }

    @Test
    fun syncProjects_requestContainsProjectData() = runTest {
        var requestBody = ""
        val api = apiWith(MockEngine { request ->
            requestBody = request.bodyAsText()
            respond("", HttpStatusCode.OK, jsonHeaders)
        })
        val projects = listOf(
            RatingSyncProject(
                id = "p1",
                name = "Тестовый проект",
                members = listOf(
                    RatingSyncMember(
                        name = "Иванов Иван",
                        roles = listOf("Backend"),
                        identifiers = listOf(RatingSyncIdentifier("github", "ivanov"))
                    )
                )
            )
        )

        api.syncProjects(projects)

        assertTrue(requestBody.contains("p1"), "Request body must contain the project id")
        assertTrue(requestBody.contains("Тестовый проект"), "Request body must contain the project name")
        assertTrue(requestBody.contains("Иванов Иван"), "Request body must contain the member name")
    }

    @Test
    fun syncProjects_500Response_returnsFailure() = runTest {
        val api = mockApi(statusCode = HttpStatusCode.InternalServerError)
        val projects = listOf(RatingSyncProject(id = "p1", name = "Проект"))

        val result = api.syncProjects(projects)
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message?.contains("500") == true)
    }

    @Test
    fun syncProjects_withMultipleProjects_sendsAllInSingleRequest() = runTest {
        var callCount = 0
        var requestBody = ""
        val api = apiWith(MockEngine { request ->
            callCount++
            requestBody = request.bodyAsText()
            respond("", HttpStatusCode.OK, jsonHeaders)
        })
        val projects = listOf(
            RatingSyncProject(id = "p1", name = "Проект А"),
            RatingSyncProject(id = "p2", name = "Проект Б"),
            RatingSyncProject(id = "p3", name = "Проект В"),
        )

        api.syncProjects(projects)

        assertEquals(1, callCount, "All projects must be sent in a single request")
        assertTrue(requestBody.contains("p1"))
        assertTrue(requestBody.contains("p2"))
        assertTrue(requestBody.contains("p3"))
    }
}
