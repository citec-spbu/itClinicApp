package com.spbu.projecttrack.rating.presentation

import com.spbu.projecttrack.rating.data.model.RankingData
import com.spbu.projecttrack.rating.data.model.RankingFilters
import com.spbu.projecttrack.rating.data.model.rankingDefaultFilters
import com.spbu.projecttrack.rating.data.repository.IRankingRepository
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
import kotlin.test.assertTrue

/**
 * ViewModel-layer tests for the Ranking screen.
 * Verify [RankingUiState] transitions and the isRefreshing flag.
 * The real network layer is replaced by a fake repository [FakeRankingRepository].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RankingViewModelTest {

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

    private fun emptyData() = RankingData(
        projects = emptyList(),
        students = emptyList(),
    )

    private fun fakeData(projectCount: Int = 2, studentCount: Int = 2) = RankingData(
        projects = List(projectCount) {
            com.spbu.projecttrack.rating.data.model.RankingItem(
                key = "p$it", title = "Проект $it", score = (it + 1).toDouble(), scoreText = "${it + 1}.00"
            )
        },
        students = List(studentCount) {
            com.spbu.projecttrack.rating.data.model.RankingItem(
                key = "s$it", title = "Студент $it", score = (it + 1).toDouble(), scoreText = "${it + 1}.00"
            )
        },
    )

    private fun successRepo(data: RankingData = fakeData()): FakeRankingRepository =
        FakeRankingRepository(listOf(Result.success(data)))

    private fun failingRepo(message: String = "Network error"): FakeRankingRepository =
        FakeRankingRepository(listOf(Result.failure(RuntimeException(message))))

    // ─────────────────────────────────────────────
    // load()
    // ─────────────────────────────────────────────

    @Test
    fun load_success_transitionsToSuccessState() = runTest {
        val data = fakeData()
        val vm = RankingViewModel(successRepo(data))

        vm.load()

        assertIs<RankingUiState.Success>(vm.uiState.value)
        assertEquals(data, (vm.uiState.value as RankingUiState.Success).data)
    }

    @Test
    fun load_failure_transitionsToErrorState() = runTest {
        val vm = RankingViewModel(failingRepo("Сеть недоступна"))

        vm.load()

        assertIs<RankingUiState.Error>(vm.uiState.value)
        assertTrue((vm.uiState.value as RankingUiState.Error).message.isNotBlank())
    }

    @Test
    fun load_secondCallWithoutForce_doesNotReload() = runTest {
        val repo = successRepo()
        val vm = RankingViewModel(repo)

        vm.load()
        vm.load()  // second call without force must be ignored

        assertEquals(1, repo.callCount)
    }

    @Test
    fun load_forcedAfterSuccess_triggersReload() = runTest {
        val data1 = fakeData(projectCount = 1)
        val data2 = fakeData(projectCount = 3)
        val repo = FakeRankingRepository(listOf(Result.success(data1), Result.success(data2)))
        val vm = RankingViewModel(repo)

        vm.load()
        assertEquals(1, (vm.uiState.value as RankingUiState.Success).data.projects.size)

        vm.load(force = true)
        assertEquals(3, (vm.uiState.value as RankingUiState.Success).data.projects.size)
        assertEquals(2, repo.callCount)
    }

    // ─────────────────────────────────────────────
    // retry()
    // ─────────────────────────────────────────────

    @Test
    fun retry_afterFailure_loadsSuccessfully() = runTest {
        val repo = FakeRankingRepository(listOf(
            Result.failure(RuntimeException("ошибка")),
            Result.success(fakeData()),
        ))
        val vm = RankingViewModel(repo)

        vm.load()
        assertIs<RankingUiState.Error>(vm.uiState.value)

        vm.retry()
        assertIs<RankingUiState.Success>(vm.uiState.value)
    }

    @Test
    fun retry_resetsHasLoadedFlag() = runTest {
        val repo = successRepo()
        val vm = RankingViewModel(repo)

        vm.load()  // first load — hasLoaded = true
        assertEquals(1, repo.callCount)

        vm.retry()  // must reset hasLoaded and reload
        assertEquals(2, repo.callCount)
    }

    // ─────────────────────────────────────────────
    // refresh()
    // ─────────────────────────────────────────────

    @Test
    fun refresh_success_updatesSuccessState() = runTest {
        val data1 = fakeData(projectCount = 1)
        val data2 = fakeData(projectCount = 5)
        val repo = FakeRankingRepository(listOf(Result.success(data1), Result.success(data2)))
        val vm = RankingViewModel(repo)

        vm.load()
        assertEquals(1, (vm.uiState.value as RankingUiState.Success).data.projects.size)

        vm.refresh()
        assertEquals(5, (vm.uiState.value as RankingUiState.Success).data.projects.size)
    }

    @Test
    fun refresh_failure_preservesExistingSuccessState() = runTest {
        val data = fakeData()
        val repo = FakeRankingRepository(listOf(Result.success(data), Result.failure(RuntimeException("ошибка"))))
        val vm = RankingViewModel(repo)

        vm.load()
        assertIs<RankingUiState.Success>(vm.uiState.value)

        vm.refresh()
        // On refresh failure the existing Success state must be preserved
        assertIs<RankingUiState.Success>(vm.uiState.value)
    }

    @Test
    fun refresh_isRefreshingIsFalseAfterCompletion() = runTest {
        val vm = RankingViewModel(successRepo())

        vm.load()
        vm.refresh()

        assertFalse(vm.isRefreshing.value)
    }

    // ─────────────────────────────────────────────
    // applyFilters()
    // ─────────────────────────────────────────────

    @Test
    fun applyFilters_triggersFreshLoad() = runTest {
        val repo = successRepo()
        val vm = RankingViewModel(repo)

        vm.load()
        assertEquals(1, repo.callCount)

        vm.applyFilters(rankingDefaultFilters())
        assertEquals(2, repo.callCount)
    }

    @Test
    fun applyFilters_passesFiltersToRepository() = runTest {
        val repo = successRepo()
        val vm = RankingViewModel(repo)

        vm.load()

        val customFilters = RankingFilters()
        vm.applyFilters(customFilters)

        assertEquals(customFilters, repo.lastFilters)
    }

    // ─────────────────────────────────────────────
    // reset()
    // ─────────────────────────────────────────────

    @Test
    fun reset_returnsToIdleState() = runTest {
        val vm = RankingViewModel(successRepo())

        vm.load()
        assertIs<RankingUiState.Success>(vm.uiState.value)

        vm.reset()
        assertIs<RankingUiState.Idle>(vm.uiState.value)
    }

    @Test
    fun reset_allowsSubsequentLoadAfterReset() = runTest {
        val repo = successRepo()
        val vm = RankingViewModel(repo)

        vm.load()
        assertEquals(1, repo.callCount)

        vm.reset()
        vm.load()  // after reset hasLoaded = false → load must proceed
        assertEquals(2, repo.callCount)
    }
}

// ─────────────────────────────────────────────
// Fake
// ─────────────────────────────────────────────

/**
 * Fake ranking repository.
 * Returns the provided results sequentially.
 * The last result is repeated on subsequent calls.
 */
private class FakeRankingRepository(
    private val results: List<Result<RankingData>>,
) : IRankingRepository {

    var callCount = 0
    var lastFilters: RankingFilters? = null

    override suspend fun loadRatings(
        filters: RankingFilters,
        forceRefresh: Boolean,
    ): Result<RankingData> {
        lastFilters = filters
        val index = callCount.coerceAtMost(results.size - 1)
        callCount++
        return results[index]
    }
}
