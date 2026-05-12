package com.spbu.projecttrack.e2e

import com.spbu.projecttrack.projects.data.api.ProjectsApi
import com.spbu.projecttrack.rating.data.api.MetricApi
import com.spbu.projecttrack.rating.data.repository.RankingRepository
import com.spbu.projecttrack.rating.presentation.RankingUiState
import com.spbu.projecttrack.rating.presentation.RankingViewModel
import com.spbu.projecttrack.user.data.api.UserProfileApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * End-to-end tests for the Ranking feature.
 *
 * Stack: MockEngine → HttpClient → MetricApi / ProjectsApi / UserProfileApi
 *        → RankingRepository → RankingViewModel.
 *
 * Two separate MockEngines are used:
 *  - [metricEngine]   — intercepts MetricApi calls   (project list, details, students, sync).
 *  - [projectsEngine] — intercepts ProjectsApi + UserProfileApi calls (catalog, profile).
 *
 * Routing within each engine is based on HTTP method and request path.
 * No real network requests are made.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RankingE2ETest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    // ─────────────────────────────────────────────
    // Setup / Teardown
    // ─────────────────────────────────────────────

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    // ─────────────────────────────────────────────
    // JSON fixtures
    // ─────────────────────────────────────────────

    // ── MetricApi ──

    /** Metric project list (GET /project on metric backend). */
    private val metricProjectListJson = """
        [
          {"id":"alpha","name":"Alpha Project","grade":"4.50"},
          {"id":"beta","name":"Beta Project","grade":"3.00"}
        ]
    """.trimIndent()

    /** Metric project detail for alpha (GET /project/alpha). */
    private val metricAlphaDetailJson = """
        {
          "id": "alpha",
          "name": "Alpha Project",
          "users": [
            {
              "name": "Иванов Иван Иванович",
              "roles": ["developer"],
              "identifiers": [{"platform":"GitHub","value":"ivan-ivanov"}]
            }
          ],
          "resources": []
        }
    """.trimIndent()

    /** Metric project detail for beta (GET /project/beta). */
    private val metricBetaDetailJson = """
        {
          "id": "beta",
          "name": "Beta Project",
          "users": [],
          "resources": []
        }
    """.trimIndent()

    /** Student ratings (GET /rating/students). */
    private val studentRatingsJson = """
        [
          {"id":"s1","name":"Иванов Иван Иванович","score":4.2}
        ]
    """.trimIndent()

    // ── ProjectsApi / UserProfileApi ──

    /** Project catalog (POST /project/findmany) — 1 project, pagination terminates immediately. */
    private val catalogJson = """
        {
          "projects": [{"id":"alpha","name":"Alpha Project"}],
          "tags": [{"id":10,"name":"ml"}]
        }
    """.trimIndent()

    /** Catalog project detail (GET /project/alpha). */
    private val catalogDetailJson = """
        {
          "project": {"id":"alpha","name":"Alpha Project"},
          "tags": [],
          "members": [],
          "users": []
        }
    """.trimIndent()

    /** Current user profile (GET /user/profile). */
    private val profileJson = """
        {
          "projects": [],
          "user": {
            "email": "user@example.com",
            "fullName": {"name":"","surname":"","patronymic":""}
          }
        }
    """.trimIndent()

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private fun httpClient(engine: MockEngine): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    /**
     * MockEngine for MetricApi.
     * Routes by HTTP method and request path.
     *
     * @param projectListStatus  response status for GET /project (project list).
     */
    private fun metricEngine(
        projectListStatus: HttpStatusCode = HttpStatusCode.OK,
    ): MockEngine = MockEngine { request ->
        val path = request.url.encodedPath
        val method = request.method.value

        when {
            // POST /rating/sync — project sync; result is ignored by the repository
            method == "POST" && path.endsWith("/rating/sync") ->
                respond("{}", HttpStatusCode.OK, jsonHeaders)

            // GET /project — metric backend project list
            method == "GET" && path == "/project" ->
                respond(metricProjectListJson, projectListStatus, jsonHeaders)

            // GET /rating/students — student rankings
            method == "GET" && path.endsWith("/rating/students") ->
                respond(studentRatingsJson, HttpStatusCode.OK, jsonHeaders)

            // GET /project/{id} — project detail
            method == "GET" && path.startsWith("/project/") -> {
                val projectId = path.substringAfterLast("/")
                val body = if (projectId == "beta") metricBetaDetailJson else metricAlphaDetailJson
                respond(body, HttpStatusCode.OK, jsonHeaders)
            }

            else -> respond("{}", HttpStatusCode.OK, jsonHeaders)
        }
    }

    /**
     * MockEngine for ProjectsApi and UserProfileApi.
     * Routes by HTTP method and request path.
     *
     * @param statusCode  HTTP status for all responses from this engine.
     *                    When not successful, an empty body is returned with the given error code.
     */
    private fun projectsEngine(
        statusCode: HttpStatusCode = HttpStatusCode.OK,
    ): MockEngine = MockEngine { request ->
        val path = request.url.encodedPath
        val method = request.method.value

        if (statusCode.value !in 200..299) {
            return@MockEngine respond("", statusCode, jsonHeaders)
        }

        when {
            // POST /project/findmany — project catalog (paginated)
            method == "POST" && path.endsWith("/project/findmany") ->
                respond(catalogJson, HttpStatusCode.OK, jsonHeaders)

            // GET /user/profile — current user profile
            method == "GET" && path.endsWith("/user/profile") ->
                respond(profileJson, HttpStatusCode.OK, jsonHeaders)

            // GET /project/{id} — catalog project detail
            method == "GET" && path.startsWith("/project/") ->
                respond(catalogDetailJson, HttpStatusCode.OK, jsonHeaders)

            else -> respond("{}", HttpStatusCode.OK, jsonHeaders)
        }
    }

    /**
     * Assembles the full stack: MetricApi + ProjectsApi + UserProfileApi
     * → RankingRepository → RankingViewModel.
     */
    private fun buildVm(
        metricStatus: HttpStatusCode = HttpStatusCode.OK,
        catalogStatus: HttpStatusCode = HttpStatusCode.OK,
    ): RankingViewModel {
        val metricClient = httpClient(metricEngine(projectListStatus = metricStatus))
        val projectsClient = httpClient(projectsEngine(statusCode = catalogStatus))

        val repo = RankingRepository(
            api = MetricApi(client = metricClient, baseUrl = "http://metric-test"),
            projectsApi = ProjectsApi(client = projectsClient),
            userProfileApi = UserProfileApi(client = projectsClient),
        )
        return RankingViewModel(repo)
    }

    // ─────────────────────────────────────────────
    // Tests
    // ─────────────────────────────────────────────

    /**
     * Happy path: all APIs respond successfully.
     * ViewModel must reach Success and contain projects from MetricApi.
     *
     * Stack: MetricEngine(OK) + ProjectsEngine(OK) → RankingRepository → RankingViewModel
     */
    @Test
    fun fullStack_loadRanking_setsSuccessStateWithProjects() = runTest {
        val vm = buildVm()

        vm.load()

        val state = assertIs<RankingUiState.Success>(vm.uiState.value)
        assertTrue(state.data.projects.isNotEmpty(), "Project list must not be empty")
        // Both projects from MetricApi must appear in the ranking
        val keys = state.data.projects.map { it.key }.toSet()
        assertTrue("alpha" in keys, "Project alpha must be in the ranking")
        assertTrue("beta" in keys, "Project beta must be in the ranking")
    }

    /**
     * A student from MetricProjectDetail appears in the student ranking.
     * The fallback score is taken from /rating/students when no metrics exist.
     */
    @Test
    fun fullStack_loadRanking_studentsListContainsMetricProjectUsers() = runTest {
        val vm = buildVm()

        vm.load()

        val state = assertIs<RankingUiState.Success>(vm.uiState.value)
        // "Иванов Иван Иванович" is listed as a user in the alpha project detail
        val studentTitles = state.data.students.map { it.title }
        assertTrue(
            studentTitles.any { it.contains("Иванов", ignoreCase = true) },
            "Student from MetricProjectDetail must appear in the student ranking. " +
                "Found: $studentTitles"
        )
    }

    /**
     * When MetricApi is unavailable the error propagates through all layers
     * and the ViewModel emits an Error state with a non-blank message.
     *
     * Stack: MetricEngine(500) + ProjectsEngine(OK) → RankingRepository → RankingViewModel
     */
    @Test
    fun fullStack_metricApiDown_setsErrorStateInViewModel() = runTest {
        val vm = buildVm(metricStatus = HttpStatusCode.InternalServerError)

        vm.load()

        val state = assertIs<RankingUiState.Error>(vm.uiState.value)
        assertTrue(state.message.isNotBlank(), "Error message must not be blank")
    }

    /**
     * Graceful degradation: the project catalog (ProjectsApi) is unavailable
     * but MetricApi works. The repository must load the ranking from MetricApi
     * data alone without throwing an error.
     *
     * Stack: MetricEngine(OK) + ProjectsEngine(404) → RankingRepository → RankingViewModel
     */
    @Test
    fun fullStack_catalogApiDown_gracefullyLoadsFromMetricApiOnly() = runTest {
        val vm = buildVm(catalogStatus = HttpStatusCode.NotFound)

        vm.load()

        // Despite the catalog being unavailable, the ranking is built from MetricApi
        val state = assertIs<RankingUiState.Success>(vm.uiState.value)
        assertTrue(
            state.data.projects.isNotEmpty(),
            "MetricApi projects must appear in the ranking even when the catalog is unavailable"
        )
    }

    /**
     * After a successful first load, `retry()` resets the hasLoaded flag
     * and re-fetches data through the entire stack.
     */
    @Test
    fun fullStack_retryAfterSuccess_triggersReload() = runTest {
        var metricCallCount = 0
        val countingMetricEngine = MockEngine { request ->
            val path = request.url.encodedPath
            val method = request.method.value
            when {
                method == "POST" && path.endsWith("/rating/sync") ->
                    respond("{}", HttpStatusCode.OK, jsonHeaders)
                method == "GET" && path == "/project" -> {
                    metricCallCount++
                    respond(metricProjectListJson, HttpStatusCode.OK, jsonHeaders)
                }
                method == "GET" && path.endsWith("/rating/students") ->
                    respond(studentRatingsJson, HttpStatusCode.OK, jsonHeaders)
                method == "GET" && path.startsWith("/project/") -> {
                    val id = path.substringAfterLast("/")
                    respond(
                        if (id == "beta") metricBetaDetailJson else metricAlphaDetailJson,
                        HttpStatusCode.OK,
                        jsonHeaders
                    )
                }
                else -> respond("{}", HttpStatusCode.OK, jsonHeaders)
            }
        }

        val repo = RankingRepository(
            api = MetricApi(client = httpClient(countingMetricEngine), baseUrl = "http://metric-test"),
            projectsApi = ProjectsApi(client = httpClient(projectsEngine())),
            userProfileApi = UserProfileApi(client = httpClient(projectsEngine())),
        )
        val vm = RankingViewModel(repo)

        vm.load()
        assertIs<RankingUiState.Success>(vm.uiState.value)
        assertEquals(1, metricCallCount, "After first load there must be exactly 1 call to GET /project")

        vm.retry()
        assertIs<RankingUiState.Success>(vm.uiState.value)
        assertEquals(2, metricCallCount, "retry() must re-fetch data through the full stack")
    }
}
