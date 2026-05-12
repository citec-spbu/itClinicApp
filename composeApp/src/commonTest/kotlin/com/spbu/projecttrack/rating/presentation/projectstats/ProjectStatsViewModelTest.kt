package com.spbu.projecttrack.rating.presentation.projectstats

import com.spbu.projecttrack.rating.data.model.ProjectStatsChartType
import com.spbu.projecttrack.rating.data.model.ProjectStatsCodeChurnSectionUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsDateRangeUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsIssueSectionUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsMetricSectionUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsOwnershipSectionUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsThresholdUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsUiModel
import com.spbu.projecttrack.rating.data.model.ProjectStatsWeekDaySectionUi
import com.spbu.projecttrack.rating.data.model.StatsDetailDataUi
import com.spbu.projecttrack.rating.data.repository.IProjectStatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * ViewModel-layer tests for project statistics.
 * Verify state transitions, load idempotency,
 * date clamping logic, and error behaviour during refresh.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProjectStatsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

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
    // Helpers
    // ─────────────────────────────────────────────

    private fun fakeModel(
        repoId: String = "repo-1",
        startDate: String = "2024-01-01",
        endDate: String = "2024-03-31",
        rapidMinutes: Int = 150,
    ) = ProjectStatsUiModel(
        projectId = "proj-1",
        title = "Тестовый проект",
        customer = "Клиент",
        members = emptyList(),
        repositories = emptyList(),
        selectedRepositoryId = repoId,
        visibleRange = ProjectStatsDateRangeUi(
            startIsoDate = startDate,
            endIsoDate = endDate,
            startLabel = startDate,
            endLabel = endDate,
        ),
        commits = emptyMetricSection(),
        issues = ProjectStatsIssueSectionUi(
            title = "Issues",
            score = null,
            openIssues = 0,
            closedIssues = 0,
            progress = 0f,
            remainingText = "",
            rank = null,
            tableRows = emptyList(),
        ),
        pullRequests = emptyMetricSection(),
        rapidPullRequests = emptyMetricSection(),
        codeChurn = ProjectStatsCodeChurnSectionUi(
            title = "Code Churn",
            score = null,
            changedFilesCount = 0,
            rank = null,
            fileRows = emptyList(),
            tableRows = emptyList(),
        ),
        codeOwnership = ProjectStatsOwnershipSectionUi(
            title = "Code Ownership",
            score = null,
            rank = null,
            slices = emptyList(),
        ),
        dominantWeekDay = ProjectStatsWeekDaySectionUi(
            title = "Weekday",
            score = null,
            headline = "",
            subtitle = "",
            slices = emptyList(),
        ),
        details = StatsDetailDataUi(),
        rapidThreshold = ProjectStatsThresholdUi(
            totalMinutes = rapidMinutes,
            days = 0,
            hours = 2,
            minutes = rapidMinutes % 60,
        ),
    )

    private fun emptyMetricSection() = ProjectStatsMetricSectionUi(
        title = "",
        score = null,
        primaryValue = "0",
        primaryCaption = "",
        rank = null,
        rankCaption = "",
        chartTitle = "",
        chartType = ProjectStatsChartType.Bars,
        chartPoints = emptyList(),
        tableTitle = "",
        tableRows = emptyList(),
        tooltipTitle = "",
    )

    /**
     * Creates a ViewModel with a fake repository.
     * [computationDispatcher] replaces Dispatchers.Default to prevent
     * real thread-pool switching in tests.
     */
    private fun vmWith(
        repo: FakeProjectStatsRepository,
        projectId: String = "proj-1",
    ) = ProjectStatsViewModel(
        repository = repo,
        projectId = projectId,
        computationDispatcher = testDispatcher,
    )

    private fun successRepo(vararg models: ProjectStatsUiModel): FakeProjectStatsRepository =
        FakeProjectStatsRepository(models.map { Result.success(it) })

    private fun failingRepo(message: String = "Ошибка сети"): FakeProjectStatsRepository =
        FakeProjectStatsRepository(listOf(Result.failure(RuntimeException(message))))

    // ─────────────────────────────────────────────
    // load()
    // ─────────────────────────────────────────────

    @Test
    fun load_success_setsSuccessState() = runTest {
        val model = fakeModel()
        val vm = vmWith(successRepo(model))

        vm.load()

        val state = assertIs<ProjectStatsUiState.Success>(vm.uiState.value)
        assertEquals(model, state.data)
    }

    @Test
    fun load_failure_setsErrorState() = runTest {
        val vm = vmWith(failingRepo())

        vm.load()

        assertIs<ProjectStatsUiState.Error>(vm.uiState.value)
    }

    @Test
    fun load_secondCallWithoutForce_doesNotReload() = runTest {
        val repo = successRepo(fakeModel())
        val vm = vmWith(repo)

        vm.load()
        vm.load()  // repeated call without force must be ignored

        assertEquals(1, repo.callCount)
    }

    @Test
    fun load_withForce_reloadsEvenAfterSuccess() = runTest {
        val model1 = fakeModel(repoId = "repo-1")
        val model2 = fakeModel(repoId = "repo-2")
        val repo = FakeProjectStatsRepository(listOf(Result.success(model1), Result.success(model2)))
        val vm = vmWith(repo)

        vm.load()
        assertEquals("repo-1", (vm.uiState.value as ProjectStatsUiState.Success).data.selectedRepositoryId)

        vm.load(force = true)
        assertEquals("repo-2", (vm.uiState.value as ProjectStatsUiState.Success).data.selectedRepositoryId)
        assertEquals(2, repo.callCount)
    }

    // ─────────────────────────────────────────────
    // retry()
    // ─────────────────────────────────────────────

    @Test
    fun retry_afterFailure_loadsSuccessfully() = runTest {
        val repo = FakeProjectStatsRepository(listOf(
            Result.failure<ProjectStatsUiModel>(RuntimeException("ошибка")),
            Result.success(fakeModel()),
        ))
        val vm = vmWith(repo)

        vm.load()
        assertIs<ProjectStatsUiState.Error>(vm.uiState.value)

        vm.retry()
        assertIs<ProjectStatsUiState.Success>(vm.uiState.value)
        assertEquals(2, repo.callCount)
    }

    // ─────────────────────────────────────────────
    // refresh()
    // ─────────────────────────────────────────────

    @Test
    fun refresh_success_updatesState() = runTest {
        val model1 = fakeModel(repoId = "old-repo")
        val model2 = fakeModel(repoId = "new-repo")
        val repo = FakeProjectStatsRepository(listOf(Result.success(model1), Result.success(model2)))
        val vm = vmWith(repo)

        vm.load()
        vm.refresh()

        assertEquals("new-repo", (vm.uiState.value as ProjectStatsUiState.Success).data.selectedRepositoryId)
    }

    @Test
    fun refresh_failure_preservesExistingSuccessState() = runTest {
        val model = fakeModel()
        val repo = FakeProjectStatsRepository(listOf(
            Result.success(model),
            Result.failure<ProjectStatsUiModel>(RuntimeException("ошибка")),
        ))
        val vm = vmWith(repo)

        vm.load()
        assertIs<ProjectStatsUiState.Success>(vm.uiState.value)

        vm.refresh()
        // On refresh failure the Success state must be preserved
        val state = assertIs<ProjectStatsUiState.Success>(vm.uiState.value)
        assertEquals(model, state.data)
    }

    @Test
    fun refresh_isRefreshingIsFalseAfterCompletion() = runTest {
        val vm = vmWith(successRepo(fakeModel(), fakeModel()))

        vm.load()
        vm.refresh()

        assertFalse(vm.isRefreshing.value)
    }

    // ─────────────────────────────────────────────
    // selectStartDate()
    // ─────────────────────────────────────────────

    @Test
    fun selectStartDate_whenEndIsBeforeNewStart_clampsEndDate() = runTest {
        val initialModel = fakeModel(startDate = "2024-01-01", endDate = "2024-03-31")
        val updatedModel = fakeModel(startDate = "2024-06-01", endDate = "2024-06-01")
        val repo = FakeProjectStatsRepository(listOf(Result.success(initialModel), Result.success(updatedModel)))
        val vm = vmWith(repo)

        vm.load()
        // Selecting a start date later than the current end date → end date must be clamped up
        vm.selectStartDate("2024-06-01")

        assertEquals("2024-06-01", repo.lastStartDate)
        assertEquals("2024-06-01", repo.lastEndDate)
    }

    @Test
    fun selectStartDate_whenEndIsAfterNewStart_keepsEndDate() = runTest {
        val initialModel = fakeModel(startDate = "2024-01-01", endDate = "2024-12-31")
        val updatedModel = fakeModel()
        val repo = FakeProjectStatsRepository(listOf(Result.success(initialModel), Result.success(updatedModel)))
        val vm = vmWith(repo)

        vm.load()
        vm.selectStartDate("2024-03-01")

        // End date 2024-12-31 > 2024-03-01, so it must remain unchanged
        assertEquals("2024-12-31", repo.lastEndDate)
    }

    // ─────────────────────────────────────────────
    // selectEndDate()
    // ─────────────────────────────────────────────

    @Test
    fun selectEndDate_whenStartIsAfterNewEnd_clampsStartDate() = runTest {
        val initialModel = fakeModel(startDate = "2024-06-01", endDate = "2024-12-31")
        val updatedModel = fakeModel()
        val repo = FakeProjectStatsRepository(listOf(Result.success(initialModel), Result.success(updatedModel)))
        val vm = vmWith(repo)

        vm.load()
        // Selecting an end date earlier than the current start date → start date must be clamped down
        vm.selectEndDate("2024-02-01")

        assertEquals("2024-02-01", repo.lastStartDate)
        assertEquals("2024-02-01", repo.lastEndDate)
    }

    // ─────────────────────────────────────────────
    // selectDateRange()
    // ─────────────────────────────────────────────

    @Test
    fun selectDateRange_whenInverted_normalizesOrder() = runTest {
        val initialModel = fakeModel()
        val updatedModel = fakeModel()
        val repo = FakeProjectStatsRepository(listOf(Result.success(initialModel), Result.success(updatedModel)))
        val vm = vmWith(repo)

        vm.load()
        // Dates passed in reverse order: end < start
        vm.selectDateRange(startIsoDate = "2024-12-01", endIsoDate = "2024-01-01")

        // ViewModel must swap them: smaller → start, larger → end
        assertEquals("2024-01-01", repo.lastStartDate)
        assertEquals("2024-12-01", repo.lastEndDate)
    }

    @Test
    fun selectDateRange_whenCorrectOrder_keepsOrder() = runTest {
        val initialModel = fakeModel()
        val updatedModel = fakeModel()
        val repo = FakeProjectStatsRepository(listOf(Result.success(initialModel), Result.success(updatedModel)))
        val vm = vmWith(repo)

        vm.load()
        vm.selectDateRange(startIsoDate = "2024-03-01", endIsoDate = "2024-09-30")

        assertEquals("2024-03-01", repo.lastStartDate)
        assertEquals("2024-09-30", repo.lastEndDate)
    }

    // ─────────────────────────────────────────────
    // updateRapidThreshold()
    // ─────────────────────────────────────────────

    @Test
    fun updateRapidThreshold_sendsCorrectTotalMinutes() = runTest {
        val initialModel = fakeModel()
        val updatedModel = fakeModel(rapidMinutes = 90)
        val repo = FakeProjectStatsRepository(listOf(Result.success(initialModel), Result.success(updatedModel)))
        val vm = vmWith(repo)

        vm.load()
        vm.updateRapidThreshold(days = 0, hours = 1, minutes = 30)

        assertEquals(90, repo.lastRapidThresholdMinutes)
    }
}

// ─────────────────────────────────────────────
// Fake
// ─────────────────────────────────────────────

/**
 * Fake project-stats repository.
 * Records parameters of each call for assertion in tests.
 */
private class FakeProjectStatsRepository(
    private val results: List<Result<ProjectStatsUiModel>>,
) : IProjectStatsRepository {

    var callCount = 0
    var lastStartDate: String? = null
    var lastEndDate: String? = null
    var lastRapidThresholdMinutes: Int? = null

    override suspend fun loadProjectStats(
        projectId: String,
        selectedRepositoryId: String?,
        selectedStartDate: String?,
        selectedEndDate: String?,
        selectedRapidThresholdMinutes: Int?,
        forceRefresh: Boolean,
    ): Result<ProjectStatsUiModel> {
        lastStartDate = selectedStartDate
        lastEndDate = selectedEndDate
        lastRapidThresholdMinutes = selectedRapidThresholdMinutes
        val index = callCount.coerceAtMost(results.size - 1)
        callCount++
        return results[index]
    }
}
