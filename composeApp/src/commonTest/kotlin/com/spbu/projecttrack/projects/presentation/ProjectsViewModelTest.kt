package com.spbu.projecttrack.projects.presentation

import com.spbu.projecttrack.projects.data.model.Project
import com.spbu.projecttrack.projects.data.model.ProjectsResponse
import com.spbu.projecttrack.projects.data.model.Tag
import com.spbu.projecttrack.projects.data.repository.IProjectsRepository
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
 * ViewModel-layer tests for the project list screen.
 * Verify initialisation, page loading, and error handling.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProjectsViewModelTest {

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

    /**
     * Creates a page of projects of the given size.
     * If [size] >= 5 the ViewModel will set hasMorePages = true.
     */
    private fun page(size: Int, offset: Int = 0) = ProjectsResponse(
        projects = List(size) { Project(id = "p${offset + it}", name = "Проект ${offset + it}") },
        tags = listOf(Tag(id = 1, name = "backend")),
    )

    // ─────────────────────────────────────────────
    // init → loadProjects()
    // ─────────────────────────────────────────────

    @Test
    fun init_success_setsSuccessState() = runTest {
        val response = page(size = 3)
        val vm = ProjectsViewModel(FakeProjectsRepository(listOf(Result.success(response))))

        val state = assertIs<ProjectsUiState.Success>(vm.uiState.value)
        assertEquals(3, state.projects.size)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun init_failure_setsErrorState() = runTest {
        val vm = ProjectsViewModel(
            FakeProjectsRepository(listOf(Result.failure(RuntimeException("Нет сети"))))
        )

        assertIs<ProjectsUiState.Error>(vm.uiState.value)
    }

    @Test
    fun init_success_setsHasMorePagesWhenPageFull() = runTest {
        val vm = ProjectsViewModel(FakeProjectsRepository(listOf(Result.success(page(size = 5)))))

        val state = assertIs<ProjectsUiState.Success>(vm.uiState.value)
        assertTrue(state.hasMorePages)
    }

    @Test
    fun init_success_setsHasMorePagesFalseWhenPageNotFull() = runTest {
        val vm = ProjectsViewModel(FakeProjectsRepository(listOf(Result.success(page(size = 4)))))

        val state = assertIs<ProjectsUiState.Success>(vm.uiState.value)
        assertFalse(state.hasMorePages)
    }

    // ─────────────────────────────────────────────
    // loadMoreProjects()
    // ─────────────────────────────────────────────

    @Test
    fun loadMoreProjects_appendsNextPageToList() = runTest {
        val repo = FakeProjectsRepository(listOf(
            Result.success(page(size = 5, offset = 0)),   // page 1
            Result.success(page(size = 3, offset = 5)),   // page 2
        ))
        val vm = ProjectsViewModel(repo)

        val afterInit = assertIs<ProjectsUiState.Success>(vm.uiState.value)
        assertEquals(5, afterInit.projects.size)

        vm.loadMoreProjects()

        val afterMore = assertIs<ProjectsUiState.Success>(vm.uiState.value)
        assertEquals(8, afterMore.projects.size)
    }

    @Test
    fun loadMoreProjects_mergesTagsWithoutDuplicates() = runTest {
        val sharedTag = Tag(id = 1, name = "backend")
        val repo = FakeProjectsRepository(listOf(
            Result.success(ProjectsResponse(projects = List(5) { Project(id = "p$it", name = "P$it") }, tags = listOf(sharedTag))),
            Result.success(ProjectsResponse(projects = List(3) { Project(id = "q$it", name = "Q$it") }, tags = listOf(sharedTag))),
        ))
        val vm = ProjectsViewModel(repo)

        vm.loadMoreProjects()

        val state = assertIs<ProjectsUiState.Success>(vm.uiState.value)
        // The duplicate tag must appear only once
        assertEquals(1, state.tags.count { it.id == sharedTag.id })
    }

    @Test
    fun loadMoreProjects_whenNoMorePages_doesNotLoad() = runTest {
        val repo = FakeProjectsRepository(listOf(Result.success(page(size = 3))))  // < 5 → hasMorePages = false
        val vm = ProjectsViewModel(repo)

        assertEquals(1, repo.callCount)
        vm.loadMoreProjects()
        assertEquals(1, repo.callCount)  // no additional request was made
    }

    @Test
    fun loadMoreProjects_whenAlreadyLoadingMore_doesNotDoubleLoad() = runTest {
        // Use StandardTestDispatcher to control coroutine execution
        val lazyDispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testDispatcher.scheduler)
        Dispatchers.setMain(lazyDispatcher)

        val repo = FakeProjectsRepository(listOf(
            Result.success(page(size = 5, offset = 0)),
            Result.success(page(size = 3, offset = 5)),
        ))
        val vm = ProjectsViewModel(repo)
        lazyDispatcher.scheduler.advanceUntilIdle()  // finish init

        // Call twice: isLoadingMore becomes true after the first call,
        // the second call must be ignored
        vm.loadMoreProjects()
        vm.loadMoreProjects()
        lazyDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, repo.callCount)  // init + 1 load-more (not 3)

        Dispatchers.setMain(testDispatcher)  // restore for teardown
    }

    @Test
    fun loadMoreProjects_onFailure_restoresCurrentState() = runTest {
        val repo = FakeProjectsRepository(listOf(
            Result.success(page(size = 5, offset = 0)),
            Result.failure(RuntimeException("ошибка")),
        ))
        val vm = ProjectsViewModel(repo)

        val before = assertIs<ProjectsUiState.Success>(vm.uiState.value)
        vm.loadMoreProjects()

        // On load-more failure the previous Success state must be restored
        val after = assertIs<ProjectsUiState.Success>(vm.uiState.value)
        assertEquals(before.projects.size, after.projects.size)
        assertFalse(after.isLoadingMore)
    }

    // ─────────────────────────────────────────────
    // retry()
    // ─────────────────────────────────────────────

    @Test
    fun retry_afterError_reloadsSuccessfully() = runTest {
        val repo = FakeProjectsRepository(listOf(
            Result.failure(RuntimeException("ошибка")),
            Result.success(page(size = 3)),
        ))
        val vm = ProjectsViewModel(repo)

        assertIs<ProjectsUiState.Error>(vm.uiState.value)

        vm.retry()

        assertIs<ProjectsUiState.Success>(vm.uiState.value)
    }
}

// ─────────────────────────────────────────────
// Fake
// ─────────────────────────────────────────────

/**
 * Fake project repository.
 * Returns results sequentially; the last result repeats on subsequent calls.
 */
private class FakeProjectsRepository(
    private val results: List<Result<ProjectsResponse>>,
) : IProjectsRepository {

    var callCount = 0

    override suspend fun getProjects(page: Int): Result<ProjectsResponse> {
        val index = callCount.coerceAtMost(results.size - 1)
        callCount++
        return results[index]
    }
}
