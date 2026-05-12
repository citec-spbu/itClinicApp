package com.spbu.projecttrack

import com.spbu.projecttrack.core.search.SearchUtilsTest
import com.spbu.projecttrack.e2e.ProjectsE2ETest
import com.spbu.projecttrack.e2e.RankingE2ETest
import com.spbu.projecttrack.projects.presentation.ProjectsViewModelTest
import com.spbu.projecttrack.rating.data.api.MetricApiTest
import com.spbu.projecttrack.rating.data.repository.RankingScoreEngineTest
import com.spbu.projecttrack.rating.data.repository.RatingPersonIdentityTest
import com.spbu.projecttrack.rating.presentation.RankingViewModelTest
import com.spbu.projecttrack.rating.presentation.projectstats.ProjectStatsViewModelTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * JUnit 4 Suite — runs all project tests in one click.
 *
 * How to run:
 *  - In Android Studio: right-click this class → Run 'AllTestsSuite'
 *  - From terminal: ./gradlew :composeApp:testDebugUnitTest
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Utilities
    SearchUtilsTest::class,

    // Rating business logic
    RankingScoreEngineTest::class,
    RatingPersonIdentityTest::class,

    // HTTP layer
    MetricApiTest::class,

    // ViewModels (unit tests with fake repositories)
    RankingViewModelTest::class,
    ProjectsViewModelTest::class,
    ProjectStatsViewModelTest::class,

    // End-to-end (MockEngine → API → Repository → ViewModel)
    ProjectsE2ETest::class,
    RankingE2ETest::class,
)
class AllTestsSuite
