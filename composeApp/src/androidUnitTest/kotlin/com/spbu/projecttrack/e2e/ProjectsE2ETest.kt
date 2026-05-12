package com.spbu.projecttrack.e2e

import com.spbu.projecttrack.projects.data.api.ProjectsApi
import com.spbu.projecttrack.projects.data.repository.ProjectsRepository
import com.spbu.projecttrack.projects.presentation.ProjectsUiState
import com.spbu.projecttrack.projects.presentation.ProjectsViewModel
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * End-to-end tests for the Projects feature.
 *
 * Stack: MockEngine → HttpClient → ProjectsApi → ProjectsRepository → ProjectsViewModel.
 * No intermediate layer is replaced with a fake — real code at every level is exercised.
 * Network transport is replaced by Ktor MockEngine; no real HTTP requests are made.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProjectsE2ETest {

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

    /** Page with 3 projects — hasMorePages = false (size < 5). */
    private val page1SmallJson = """
        {
          "projects": [
            {"id":"p1","name":"Проект 1"},
            {"id":"p2","name":"Проект 2"},
            {"id":"p3","name":"Проект 3"}
          ],
          "tags": [{"id":1,"name":"backend"}]
        }
    """.trimIndent()

    /** Full page with 5 projects — hasMorePages = true. */
    private val page1FullJson = """
        {
          "projects": [
            {"id":"p1","name":"Проект 1"},
            {"id":"p2","name":"Проект 2"},
            {"id":"p3","name":"Проект 3"},
            {"id":"p4","name":"Проект 4"},
            {"id":"p5","name":"Проект 5"}
          ],
          "tags": [{"id":1,"name":"backend"}]
        }
    """.trimIndent()

    /** Second page with 2 projects — hasMorePages = false. */
    private val page2Json = """
        {
          "projects": [
            {"id":"p6","name":"Проект 6"},
            {"id":"p7","name":"Проект 7"}
          ],
          "tags": [{"id":2,"name":"frontend"}]
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
     * Builds a ViewModel wired through real API and Repository layers.
     * [responses] are JSON response bodies returned in order.
     * The last response is repeated for any subsequent requests.
     */
    private fun buildVm(vararg responses: String): ProjectsViewModel {
        var index = 0
        val engine = MockEngine { _ ->
            val body = responses.getOrElse(index) { responses.last() }
            index++
            respond(body, HttpStatusCode.OK, jsonHeaders)
        }
        return ProjectsViewModel(
            repository = ProjectsRepository(ProjectsApi(httpClient(engine)))
        )
    }

    /**
     * Builds a ViewModel where the first [failCount] requests fail with [errorCode],
     * and subsequent requests return [successBody].
     */
    private fun buildVmWithInitialFailure(
        failCount: Int = 1,
        errorCode: HttpStatusCode = HttpStatusCode.InternalServerError,
        successBody: String = page1SmallJson,
    ): ProjectsViewModel {
        var callCount = 0
        val engine = MockEngine { _ ->
            callCount++
            if (callCount <= failCount) {
                respond("Server Error", errorCode, jsonHeaders)
            } else {
                respond(successBody, HttpStatusCode.OK, jsonHeaders)
            }
        }
        return ProjectsViewModel(
            repository = ProjectsRepository(ProjectsApi(httpClient(engine)))
        )
    }

    // ─────────────────────────────────────────────
    // Tests
    // ─────────────────────────────────────────────

    /**
     * Happy path: on a successful server response the ViewModel transitions to
     * Success and contains the correct number of projects.
     *
     * Stack: MockEngine → ProjectsApi.getProjects() → ProjectsRepository → ProjectsViewModel
     */
    @Test
    fun fullStack_loadProjects_setsSuccessStateWithCorrectProjectCount() = runTest {
        val vm = buildVm(page1SmallJson)

        val state = assertIs<ProjectsUiState.Success>(vm.uiState.value)
        assertEquals(3, state.projects.size)
        assertEquals("p1", state.projects[0].id)
        assertEquals("Проект 1", state.projects[0].name)
        assertFalse(state.isLoadingMore)
        assertFalse(state.hasMorePages) // size < 5 → end of list
    }

    /**
     * On an HTTP server error the failure propagates through all layers
     * and the ViewModel emits an Error state.
     *
     * Stack: MockEngine(500) → ProjectsApi → ProjectsRepository → ProjectsViewModel
     */
    @Test
    fun fullStack_httpError_setsErrorStateInViewModel() = runTest {
        var callCount = 0
        val engine = MockEngine { _ ->
            callCount++
            respond("Internal Server Error", HttpStatusCode.InternalServerError, jsonHeaders)
        }
        val vm = ProjectsViewModel(
            repository = ProjectsRepository(ProjectsApi(httpClient(engine)))
        )

        assertIs<ProjectsUiState.Error>(vm.uiState.value)
        assertEquals(1, callCount) // exactly one request was made
    }

    /**
     * Pagination: after loading a full first page, `loadMoreProjects()` fetches
     * the second page and merges both lists.
     *
     * Stack: MockEngine(page1 + page2) → ProjectsApi → ProjectsRepository → ProjectsViewModel
     */
    @Test
    fun fullStack_loadMoreProjects_appendsSecondPageToList() = runTest {
        val vm = buildVm(page1FullJson, page2Json)

        // After init: 5 projects, hasMorePages = true
        val afterInit = assertIs<ProjectsUiState.Success>(vm.uiState.value)
        assertEquals(5, afterInit.projects.size)
        assertTrue(afterInit.hasMorePages)

        vm.loadMoreProjects()

        // After load-more: 5 + 2 = 7 projects, tags merged
        val afterMore = assertIs<ProjectsUiState.Success>(vm.uiState.value)
        assertEquals(7, afterMore.projects.size)
        assertEquals("p6", afterMore.projects[5].id)
        assertFalse(afterMore.hasMorePages)
        // Tags from both pages are merged without duplicates
        assertEquals(2, afterMore.tags.size)
    }

    /**
     * Retry after a network error: `retry()` re-triggers the full request chain
     * and transitions the ViewModel to Success on a subsequent successful response.
     *
     * Stack: MockEngine(500 → 200) → ProjectsApi → ProjectsRepository → ProjectsViewModel
     */
    @Test
    fun fullStack_retryAfterNetworkError_recoversToSuccessState() = runTest {
        val vm = buildVmWithInitialFailure(failCount = 1, successBody = page1SmallJson)

        // init: first request fails → Error
        assertIs<ProjectsUiState.Error>(vm.uiState.value)

        vm.retry()

        // retry: second request succeeds → Success
        val state = assertIs<ProjectsUiState.Success>(vm.uiState.value)
        assertEquals(3, state.projects.size)
    }
}
