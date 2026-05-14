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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import com.spbu.projecttrack.rating.data.model.rankingDefaultFilters
import kotlin.coroutines.CoroutineContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

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
    private val immediateDispatcher = object : CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            block.run()
        }
    }
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

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

    private fun httpClient(engine: MockEngine): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private fun metricEngine(
        projectListStatus: HttpStatusCode = HttpStatusCode.OK,
    ): MockEngine = MockEngine { request ->
        val path = request.url.encodedPath
        val method = request.method.value

        when {
            method == "POST" && path.endsWith("/rating/sync") ->
                respond("{}", HttpStatusCode.OK, jsonHeaders)

            method == "GET" && path == "/project" ->
                respond(metricProjectListJson, projectListStatus, jsonHeaders)

            method == "GET" && path.endsWith("/rating/students") ->
                respond(studentRatingsJson, HttpStatusCode.OK, jsonHeaders)

            method == "GET" && path.startsWith("/project/") -> {
                val projectId = path.substringAfterLast("/")
                val body = if (projectId == "beta") metricBetaDetailJson else metricAlphaDetailJson
                respond(body, HttpStatusCode.OK, jsonHeaders)
            }

            else -> respond("{}", HttpStatusCode.OK, jsonHeaders)
        }
    }

    private fun projectsEngine(
        statusCode: HttpStatusCode = HttpStatusCode.OK,
    ): MockEngine = MockEngine { request ->
        val path = request.url.encodedPath
        val method = request.method.value

        if (statusCode.value !in 200..299) {
            return@MockEngine respond("", statusCode, jsonHeaders)
        }

        when {
            method == "POST" && path.endsWith("/project/findmany") ->
                respond(catalogJson, HttpStatusCode.OK, jsonHeaders)

            method == "GET" && path.endsWith("/user/profile") ->
                respond(profileJson, HttpStatusCode.OK, jsonHeaders)

            method == "GET" && path.startsWith("/project/") ->
                respond(catalogDetailJson, HttpStatusCode.OK, jsonHeaders)

            else -> respond("{}", HttpStatusCode.OK, jsonHeaders)
        }
    }

    private fun buildRepo(
        api: MetricApi,
        projectsApi: ProjectsApi,
        userProfileApi: UserProfileApi,
        computationDispatcher: CoroutineDispatcher,
    ) = RankingRepository(
        api = api,
        projectsApi = projectsApi,
        userProfileApi = userProfileApi,
        computationDispatcher = computationDispatcher,
    )

    private fun buildVm(
        metricStatus: HttpStatusCode = HttpStatusCode.OK,
        catalogStatus: HttpStatusCode = HttpStatusCode.OK,
        computationDispatcher: CoroutineDispatcher,
    ): RankingViewModel {
        val metricClient = httpClient(metricEngine(projectListStatus = metricStatus))
        val projectsClient = httpClient(projectsEngine(statusCode = catalogStatus))

        return RankingViewModel(
            repository = buildRepo(
                api = MetricApi(client = metricClient, baseUrl = "http://metric-test"),
                projectsApi = ProjectsApi(client = projectsClient),
                userProfileApi = UserProfileApi(client = projectsClient),
                computationDispatcher = computationDispatcher,
            ),
            uiDispatcher = computationDispatcher,
        )
    }

    private fun RankingViewModel.awaitState(
        timeoutMs: Long = 1_000,
        predicate: (RankingUiState) -> Boolean,
    ): RankingUiState {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val state = uiState.value
            if (predicate(state)) return state
            Thread.sleep(10)
        }
        fail("Timed out waiting for state. Last state: ${uiState.value}")
    }

    @Test
    fun fullStack_loadRanking_setsSuccessStateWithProjects() {
        val repo = buildRepo(
            api = MetricApi(client = httpClient(metricEngine()), baseUrl = "http://metric-test"),
            projectsApi = ProjectsApi(client = httpClient(projectsEngine())),
            userProfileApi = UserProfileApi(client = httpClient(projectsEngine())),
            computationDispatcher = immediateDispatcher,
        )
        val repoResult = runBlocking { repo.loadRatings(rankingDefaultFilters()) }
        assertTrue(repoResult.isSuccess, repoResult.exceptionOrNull()?.stackTraceToString() ?: "repo failed")
        val vm = RankingViewModel(repo, uiDispatcher = immediateDispatcher)

        vm.load()

        val state = assertIs<RankingUiState.Success>(vm.awaitState { it is RankingUiState.Success })
        assertTrue(state.data.projects.isNotEmpty(), "Project list must not be empty")
        val keys = state.data.projects.map { it.key }.toSet()
        assertTrue("alpha" in keys, "Project alpha must be in the ranking")
        assertTrue("beta" in keys, "Project beta must be in the ranking")
    }

    @Test
    fun fullStack_loadRanking_studentsListContainsMetricProjectUsers() {
        val vm = buildVm(computationDispatcher = immediateDispatcher)

        vm.load()

        val state = assertIs<RankingUiState.Success>(vm.awaitState { it is RankingUiState.Success })
        val studentTitles = state.data.students.map { it.title }
        assertTrue(
            studentTitles.any { it.contains("Иванов", ignoreCase = true) },
            "Student from MetricProjectDetail must appear in the student ranking. Found: $studentTitles"
        )
    }

    @Test
    fun fullStack_metricApiDown_setsErrorStateInViewModel() {
        val vm = buildVm(
            metricStatus = HttpStatusCode.InternalServerError,
            computationDispatcher = immediateDispatcher,
        )

        vm.load()

        val state = assertIs<RankingUiState.Error>(vm.awaitState { it is RankingUiState.Error })
        assertTrue(state.message.isNotBlank(), "Error message must not be blank")
    }

    @Test
    fun fullStack_catalogApiDown_gracefullyLoadsFromMetricApiOnly() {
        val vm = buildVm(
            catalogStatus = HttpStatusCode.NotFound,
            computationDispatcher = immediateDispatcher,
        )

        vm.load()

        val state = assertIs<RankingUiState.Success>(vm.awaitState { it is RankingUiState.Success })
        assertTrue(
            state.data.projects.isNotEmpty(),
            "MetricApi projects must appear in the ranking even when the catalog is unavailable"
        )
    }

    @Test
    fun fullStack_retryAfterSuccess_triggersReload() {
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

        val repo = buildRepo(
            api = MetricApi(client = httpClient(countingMetricEngine), baseUrl = "http://metric-test"),
            projectsApi = ProjectsApi(client = httpClient(projectsEngine())),
            userProfileApi = UserProfileApi(client = httpClient(projectsEngine())),
            computationDispatcher = immediateDispatcher,
        )
        val vm = RankingViewModel(repo, uiDispatcher = immediateDispatcher)

        vm.load()
        assertIs<RankingUiState.Success>(vm.awaitState { metricCallCount == 1 && it is RankingUiState.Success })
        assertEquals(1, metricCallCount, "After first load there must be exactly 1 call to GET /project")

        vm.retry()
        assertIs<RankingUiState.Success>(vm.awaitState { metricCallCount == 2 && it is RankingUiState.Success })
        assertEquals(2, metricCallCount, "retry() must re-fetch data through the full stack")
    }
}
