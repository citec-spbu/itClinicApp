package com.spbu.projecttrack.rating.presentation.userstats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spbu.projecttrack.core.settings.localizeRuntime
import com.spbu.projecttrack.core.settings.localizedString
import com.spbu.projecttrack.core.storage.createAppPreferences
import com.spbu.projecttrack.core.logging.AppLog
import com.spbu.projecttrack.core.theme.AppColors
import com.spbu.projecttrack.core.theme.AppFonts
import com.spbu.projecttrack.core.theme.appPalette
import com.spbu.projecttrack.core.theme.subtleBorder
import com.spbu.projecttrack.core.ui.AppSnackbarHost
import com.spbu.projecttrack.core.ui.lazyListEdgeFadeMask
import com.spbu.projecttrack.rating.data.StatsScreenSettingsPersistence
import com.spbu.projecttrack.rating.data.model.ProjectStatsIssueSectionUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsMetricSectionUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsOwnershipSectionUi
import com.spbu.projecttrack.rating.data.model.UserStatsUiModel
import com.spbu.projecttrack.rating.data.model.filterByParticipant
import com.spbu.projecttrack.rating.presentation.details.StatsDetailScreen
import com.spbu.projecttrack.rating.presentation.settings.StatsScreenSection
import com.spbu.projecttrack.rating.presentation.settings.StatsScreenSettingsScreen
import com.spbu.projecttrack.rating.presentation.settings.StatsScreenSettingsTarget
import com.spbu.projecttrack.rating.presentation.settings.statsScreenSectionsFromIds
import com.spbu.projecttrack.rating.export.ProjectStatsExportPayload
import com.spbu.projecttrack.rating.export.ProjectStatsSection
import com.spbu.projecttrack.rating.export.ProjectStatsSummaryCard
import com.spbu.projecttrack.rating.export.ProjectStatsTable
import com.spbu.projecttrack.rating.export.ProjectStatsTableRow
import com.spbu.projecttrack.rating.export.buildRapidPullRequestDetailExportContent
import com.spbu.projecttrack.rating.export.rememberProjectStatsExporter
import com.spbu.projecttrack.rating.presentation.projectstats.ActionPillButton
import com.spbu.projecttrack.rating.presentation.projectstats.AnimatedSection
import com.spbu.projecttrack.rating.presentation.projectstats.ChartCard
import com.spbu.projecttrack.rating.presentation.projectstats.DominantWeekDaySection
import com.spbu.projecttrack.rating.presentation.projectstats.DoubleMetricRow
import com.spbu.projecttrack.rating.presentation.projectstats.EmptyDetailedInfoCard
import com.spbu.projecttrack.rating.presentation.projectstats.ErrorState
import com.spbu.projecttrack.rating.presentation.projectstats.FileStatsCard
import com.spbu.projecttrack.rating.presentation.projectstats.FooterActions
import com.spbu.projecttrack.rating.presentation.projectstats.IssueProgressCard
import com.spbu.projecttrack.rating.presentation.projectstats.RepositorySelectorCard
import com.spbu.projecttrack.rating.presentation.projectstats.ScoreCard
import com.spbu.projecttrack.rating.presentation.projectstats.SectionHeader
import com.spbu.projecttrack.rating.presentation.projectstats.SingleMetricCard
import com.spbu.projecttrack.rating.presentation.projectstats.StatsBackgroundLogo
import com.spbu.projecttrack.rating.presentation.projectstats.rememberStatsBackDispatcher
import com.spbu.projecttrack.rating.presentation.projectstats.StatsDateRangePickerDialog
import com.spbu.projecttrack.rating.presentation.projectstats.StatsTopBar
import com.spbu.projecttrack.rating.presentation.projectstats.StatsTopBarTotalHeight
import com.spbu.projecttrack.rating.presentation.projectstats.buildCommitExportTableRows
import com.spbu.projecttrack.rating.presentation.projectstats.buildDailyCountChart
import com.spbu.projecttrack.rating.presentation.projectstats.buildPullRequestExportTableRows
import com.spbu.projecttrack.rating.common.StatsExportCopy
import com.spbu.projecttrack.rating.presentation.projectstats.toExportSection
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.backhandler.BackHandler
import projecttrack.composeapp.generated.resources.Res
import projecttrack.composeapp.generated.resources.spbu_logo

private val UserStatsCardShape = RoundedCornerShape(10.dp)
private val UserStatsButtonShape = RoundedCornerShape(10.dp)
private val UserStatsHorizontalPadding = 21.dp
private val UserStatsAccentGradient = Brush.verticalGradient(
    colors = listOf(AppColors.GradientStart, AppColors.GradientEndAlt)
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun UserStatsScreen(
    viewModel: UserStatsViewModel,
    onBackClick: () -> Unit,
    onProjectClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val logTag = "UserStatsScreen"
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val exporter = rememberProjectStatsExporter()
    val appPreferences = remember { createAppPreferences() }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var activeDetailSection by remember { mutableStateOf<StatsScreenSection?>(null) }
    var renderedDetailSection by remember { mutableStateOf<StatsScreenSection?>(null) }
    var activeSectionIds by rememberSaveable {
        mutableStateOf(
            StatsScreenSettingsPersistence.decode(
                appPreferences.getUserStatsScreenSettingsJson(),
            )
        )
    }
    val activeSections = remember(activeSectionIds) { statsScreenSectionsFromIds(activeSectionIds) }
    val detailTransitionState = remember { MutableTransitionState(false) }
    val palette = appPalette()
    val dispatchBack = rememberStatsBackDispatcher(logTag) {
        "settingsVisible=$showSettingsScreen detailSection=${activeDetailSection?.id ?: "none"}"
    }

    LaunchedEffect(viewModel) {
        viewModel.load()
    }

    LaunchedEffect(activeSectionIds) {
        appPreferences.saveUserStatsScreenSettingsJson(
            StatsScreenSettingsPersistence.encode(activeSectionIds),
        )
    }

    LaunchedEffect(activeDetailSection) {
        if (activeDetailSection != null) {
            renderedDetailSection = activeDetailSection
            detailTransitionState.targetState = true
        } else {
            detailTransitionState.targetState = false
        }
    }

    LaunchedEffect(detailTransitionState.isIdle, detailTransitionState.currentState, activeDetailSection) {
        if (detailTransitionState.isIdle && !detailTransitionState.currentState && activeDetailSection == null) {
            renderedDetailSection = null
        }
    }

    BackHandler(enabled = activeDetailSection != null || showSettingsScreen) {
        dispatchBack("system_back") {
            when {
                activeDetailSection != null -> activeDetailSection = null
                showSettingsScreen -> showSettingsScreen = false
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = palette.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
        ) {
            Image(
                painter = painterResource(Res.drawable.spbu_logo),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit,
                alpha = palette.spbuWatermarkLogoAlpha,
            )

            when (val state = uiState) {
                UserStatsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = palette.accent)
                    }
                }

                is UserStatsUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.retry() },
                        onBackClick = { dispatchBack("error_back") { onBackClick() } }
                    )
                }

                is UserStatsUiState.Success -> {
                    UserStatsContent(
                        model = state.data,
                        visibleSections = activeSections,
                        onBackClick = { dispatchBack("stats_top_bar") { onBackClick() } },
                        onProjectClick = onProjectClick,
                        onRepositorySelected = viewModel::selectRepository,
                        onDateRangeSelected = viewModel::selectDateRange,
                        onRapidThresholdChanged = viewModel::updateRapidThreshold,
                        onDetailsClick = { section ->
                            AppLog.d(logTag, "openDetail section=${section.id}")
                            activeDetailSection = section
                        },
                        onSettingsClick = {
                            AppLog.d(logTag, "openSettings")
                            showSettingsScreen = true
                        },
                        onExportPdfClick = {
                            scope.launch {
                                val payload = state.data.toExportPayload()
                                val result = exporter.exportPdf(payload)
                                val message = result.getOrNull()?.let { export ->
                                    StatsExportCopy.pdfSaved(export.fileName)
                                } ?: StatsExportCopy.exportPdfFailed()
                                snackbarHostState.showSnackbar(message)
                            }
                        },
                        onExportExcelClick = {
                            scope.launch {
                                val payload = state.data.toExportPayload()
                                val result = exporter.exportExcelCsv(payload)
                                val message = result.getOrNull()?.let { export ->
                                    StatsExportCopy.csvSaved(export.fileName)
                                } ?: StatsExportCopy.exportExcelFailed()
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    )
                }
            }

            if (showSettingsScreen) {
                StatsScreenSettingsScreen(
                    target = StatsScreenSettingsTarget.User,
                    activeSectionIds = activeSectionIds,
                    onActiveSectionIdsChange = { activeSectionIds = it },
                    onBackClick = { dispatchBack("settings_top_bar") { showSettingsScreen = false } },
                )
            }

            AnimatedVisibility(
                visibleState = detailTransitionState,
                enter = slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(tween(300)),
                exit = slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut(tween(300)),
                modifier = Modifier.fillMaxSize(),
            ) {
                val detailSection = renderedDetailSection
                if (detailSection != null && uiState is UserStatsUiState.Success) {
                    val model = (uiState as UserStatsUiState.Success).data
                    StatsDetailScreen(
                        section = detailSection,
                        repositories = model.repositories,
                        selectedRepositoryId = model.selectedRepositoryId,
                        visibleRange = model.visibleRange,
                        rapidThreshold = model.rapidThreshold,
                        details = model.details,
                        allowParticipantFilter = false,
                        initialParticipantId = model.details.defaultParticipantId,
                        overallRank = when (detailSection) {
                            StatsScreenSection.Commits -> model.commits.rank
                            StatsScreenSection.Issues -> model.issues.rank
                            StatsScreenSection.PullRequests -> model.pullRequests.rank
                            StatsScreenSection.RapidPullRequests -> model.rapidPullRequests.rank
                            StatsScreenSection.CodeChurn -> model.codeChurn.rank
                            StatsScreenSection.CodeOwnership -> model.codeOwnership.rank
                            StatsScreenSection.DominantWeekDay -> null
                        },
                        overallScore = when (detailSection) {
                            StatsScreenSection.Commits -> model.commits.score
                            StatsScreenSection.Issues -> model.issues.score
                            StatsScreenSection.PullRequests -> model.pullRequests.score
                            StatsScreenSection.RapidPullRequests -> model.rapidPullRequests.score
                            StatsScreenSection.CodeChurn -> model.codeChurn.score
                            StatsScreenSection.CodeOwnership -> model.codeOwnership.score
                            StatsScreenSection.DominantWeekDay -> model.dominantWeekDay.score
                        },
                        onBackClick = { dispatchBack("detail_top_bar") { activeDetailSection = null } },
                        onRepositorySelected = viewModel::selectRepository,
                        onDateRangeSelected = viewModel::selectDateRange,
                        onRapidThresholdChanged = viewModel::updateRapidThreshold,
                        onExportPdfClick = { participantId ->
                            scope.launch {
                                val payload = model.toSectionExportPayload(
                                    section = detailSection,
                                    participantId = participantId,
                                )
                                val result = exporter.exportPdf(payload)
                                val message = result.getOrNull()?.let { export ->
                                    StatsExportCopy.pdfSaved(export.fileName)
                                } ?: StatsExportCopy.exportPdfFailed()
                                snackbarHostState.showSnackbar(message)
                            }
                        },
                        onExportExcelClick = { participantId ->
                            scope.launch {
                                val payload = model.toSectionExportPayload(
                                    section = detailSection,
                                    participantId = participantId,
                                )
                                val result = exporter.exportExcelCsv(payload)
                                val message = result.getOrNull()?.let { export ->
                                    StatsExportCopy.csvSaved(export.fileName)
                                } ?: StatsExportCopy.exportExcelFailed()
                                snackbarHostState.showSnackbar(message)
                            }
                        },
                    )
                }
            }

            AppSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun UserStatsContent(
    model: UserStatsUiModel,
    visibleSections: List<StatsScreenSection>,
    onBackClick: () -> Unit,
    onProjectClick: (String) -> Unit,
    onRepositorySelected: (String) -> Unit,
    onDateRangeSelected: (String, String) -> Unit,
    onRapidThresholdChanged: (Int, Int, Int) -> Unit,
    onDetailsClick: (StatsScreenSection) -> Unit,
    onSettingsClick: () -> Unit,
    onExportPdfClick: () -> Unit,
    onExportExcelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDateRangePicker by remember { mutableStateOf(false) }
    val palette = appPalette()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val density = LocalDensity.current
    var topBarHeight by remember { mutableStateOf(0.dp) }
    val topContentPadding = maxOf(topBarHeight, StatsTopBarTotalHeight) + 8.dp

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .lazyListEdgeFadeMask(
                    listState = listState,
                    topInset = topContentPadding,
                ),
            contentPadding = PaddingValues(
                start = UserStatsHorizontalPadding,
                end = UserStatsHorizontalPadding,
                top = topContentPadding,
                bottom = 40.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                PersonalStatsSummary(
                    userName = model.userName,
                    role = model.role,
                )
            }
            item {
                AnimatedSection {
                    ProjectLinkCard(
                        projectTitle = model.projectTitle,
                        onClick = { onProjectClick(model.projectId) }
                    )
                }
            }
            item {
                AnimatedSection {
                    if (model.repositories.isEmpty()) {
                        EmptyDetailedInfoCard()
                    } else {
                        RepositorySelectorCard(
                            repositories = model.repositories,
                            selectedId = model.selectedRepositoryId,
                            visibleRange = model.visibleRange,
                            onRepositorySelected = onRepositorySelected,
                            onDateRangeClick = { showDateRangePicker = true }
                        )
                    }
                }
            }
            item {
                AnimatedSection {
                    SingleMetricCard(
                        value = model.teamRank?.toString() ?: "—",
                        caption = localizedString("место в команде", "team rank")
                    )
                }
            }
            items(
                items = visibleSections,
                key = { it.id },
            ) { section ->
                when (section) {
                    StatsScreenSection.Commits -> AnimatedSection {
                        PersonalMetricSection(
                            section = model.commits,
                            onDetailsClick = { onDetailsClick(StatsScreenSection.Commits) }
                        )
                    }

                    StatsScreenSection.Issues -> AnimatedSection {
                        PersonalIssueSection(
                            section = model.issues,
                            onDetailsClick = { onDetailsClick(StatsScreenSection.Issues) }
                        )
                    }

                    StatsScreenSection.PullRequests -> AnimatedSection {
                        PersonalMetricSection(
                            section = model.pullRequests,
                            onDetailsClick = { onDetailsClick(StatsScreenSection.PullRequests) }
                        )
                    }

                    StatsScreenSection.RapidPullRequests -> AnimatedSection {
                        PersonalRapidPullSection(
                            section = model.rapidPullRequests,
                            onRapidThresholdChanged = onRapidThresholdChanged,
                            onDetailsClick = { onDetailsClick(StatsScreenSection.RapidPullRequests) }
                        )
                    }

                    StatsScreenSection.CodeChurn -> AnimatedSection {
                        PersonalCodeChurnSection(
                            section = model.codeChurn,
                            onDetailsClick = { onDetailsClick(StatsScreenSection.CodeChurn) }
                        )
                    }

                    StatsScreenSection.CodeOwnership -> AnimatedSection {
                        PersonalOwnershipSection(
                            section = model.codeOwnership,
                            onDetailsClick = { onDetailsClick(StatsScreenSection.CodeOwnership) }
                        )
                    }

                    StatsScreenSection.DominantWeekDay -> AnimatedSection {
                        DominantWeekDaySection(
                            section = model.dominantWeekDay,
                            onDetailsClick = { onDetailsClick(StatsScreenSection.DominantWeekDay) }
                        )
                    }
                }
            }
            item {
                FooterActions(
                    onSettingsClick = onSettingsClick,
                    onExportPdfClick = onExportPdfClick,
                    onExportExcelClick = onExportExcelClick
                )
            }
        }

        StatsTopBar(
            title = localizedString("Статистика", "Statistics"),
            onBackClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { topBarHeight = with(density) { it.height.toDp() } }
        )

        if (showDateRangePicker) {
            StatsDateRangePickerDialog(
                initialStartIsoDate = model.visibleRange.startIsoDate,
                initialEndIsoDate = model.visibleRange.endIsoDate,
                onDismiss = { showDateRangePicker = false },
                onConfirm = { startIsoDate, endIsoDate ->
                    showDateRangePicker = false
                    onDateRangeSelected(startIsoDate, endIsoDate)
                }
            )
        }

    }
}

@Composable
private fun PersonalStatsSummary(
    userName: String,
    role: String,
    modifier: Modifier = Modifier,
) {
    val palette = appPalette()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = userName,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.2.sp,
            color = palette.primaryText,
            modifier = Modifier.padding(start = 3.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 3.dp)
        ) {
            Text(
                text = role,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Light,
                fontSize = 16.sp,
                color = palette.primaryText
            )
        }
    }
}

@Composable
private fun ProjectLinkCard(
    projectTitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = appPalette()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = palette.surface,
        shape = UserStatsCardShape,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = projectTitle,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = palette.primaryText,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "→",
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = palette.primaryText
            )
        }
    }
}

@Composable
private fun PersonalMetricSection(
    section: ProjectStatsMetricSectionUi,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(
            title = section.title,
            onDetailsClick = onDetailsClick
        )
        DoubleMetricRow(
            leftValue = section.primaryValue,
            leftCaption = section.primaryCaption,
            rightValue = section.rank?.toString() ?: "—",
            rightCaption = section.rankCaption
        )
        if (!section.supplementaryValue.isNullOrBlank() && !section.supplementaryCaption.isNullOrBlank()) {
            SingleMetricCard(
                value = section.supplementaryValue,
                caption = section.supplementaryCaption,
            )
        }
        if (section.chartPoints.isNotEmpty()) {
            ChartCard(
                title = section.chartTitle,
                chartType = section.chartType,
                points = section.chartPoints,
                tooltipTitle = section.tooltipTitle
            )
        }
        ScoreCard(
            score = section.score,
            title = StatsExportCopy.metricScoreTitleForSection(section.title)
        )
    }
}

@Composable
private fun PersonalIssueSection(
    section: ProjectStatsIssueSectionUi,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val openIssuesCaption = localizedString("открытых Issue", "open issues")
    val closedIssuesCaption = localizedString("закрытых Issue", "closed issues")
    val rankCaption = localizedString("место в рейтинге", "rank")
    val issueScoreTitle = localizedString("оценка Issue", "Issue score")
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(
            title = section.title,
            onDetailsClick = onDetailsClick
        )

        DoubleMetricRow(
            leftValue = section.openIssues.toString(),
            leftCaption = openIssuesCaption,
            rightValue = section.closedIssues.toString(),
            rightCaption = closedIssuesCaption
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IssueProgressCard(
                progress = section.progress,
                remainingText = section.remainingText,
                modifier = Modifier.weight(1f),
            )
            SingleMetricCard(
                modifier = Modifier.weight(1f),
                value = section.rank?.toString() ?: "—",
                caption = rankCaption
            )
        }

        ScoreCard(
            score = section.score,
            title = issueScoreTitle
        )
    }
}

@Composable
private fun PersonalRapidPullSection(
    section: ProjectStatsMetricSectionUi,
    onRapidThresholdChanged: (Int, Int, Int) -> Unit,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rankCaption = localizedString("место в рейтинге", "rank")
    val rapidScoreTitle = localizedString("оценка быстрых PR", "Rapid PR score")
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(
            title = section.title,
            onDetailsClick = onDetailsClick
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SingleMetricCard(
                modifier = Modifier.weight(1f),
                value = section.primaryValue,
                caption = section.primaryCaption
            )
            SingleMetricCard(
                modifier = Modifier.weight(1f),
                value = section.rank?.toString() ?: "—",
                caption = rankCaption
            )
        }

        ScoreCard(
            score = section.score,
            title = rapidScoreTitle
        )
    }
}

@Composable
private fun PersonalCodeChurnSection(
    section: com.spbu.projecttrack.rating.data.model.ProjectStatsCodeChurnSectionUi,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val changedFilesCaption = localizedString("изменено файлов", "files changed")
    val rankCaption = localizedString("место в рейтинге", "rank")
    val churnScoreTitle = localizedString(
        "оценка изменчивости кода",
        "Code churn score",
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(
            title = section.title,
            onDetailsClick = onDetailsClick
        )
        FileStatsCard(rows = section.fileRows.take(5))
        DoubleMetricRow(
            leftValue = section.changedFilesCount.toString(),
            leftCaption = changedFilesCaption,
            rightValue = section.rank?.toString() ?: "—",
            rightCaption = rankCaption
        )
        ScoreCard(
            score = section.score,
            title = churnScoreTitle
        )
    }
}

@Composable
private fun PersonalOwnershipSection(
    section: ProjectStatsOwnershipSectionUi,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val userLines = section.slices.firstOrNull { it.highlight }?.value?.toInt() ?: 0
    val totalLines = section.slices.sumOf { it.value.toDouble() }.toInt()
    val yourLinesCaption = localizedString("ваших строк", "your lines")
    val linesInProjectCaption = localizedString("строк в проекте", "lines in project")
    val rankCaption = localizedString("место в рейтинге", "rank")
    val ownershipScoreTitle = localizedString(
        "оценка владения кодом",
        "Code ownership score",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(
            title = section.title,
            onDetailsClick = onDetailsClick
        )
        DoubleMetricRow(
            leftValue = userLines.toString(),
            leftCaption = yourLinesCaption,
            rightValue = totalLines.toString(),
            rightCaption = linesInProjectCaption
        )
        SingleMetricCard(
            value = section.rank?.toString() ?: "—",
            caption = rankCaption
        )
        ScoreCard(
            score = section.score,
            title = ownershipScoreTitle
        )
    }
}

private enum class UserStatsDatePickerTarget {
    Start,
    End,
}

private fun UserStatsUiModel.toExportPayload(): ProjectStatsExportPayload {
    val selectedRepository = repositories.firstOrNull { it.id == selectedRepositoryId }
    val rapidExport = buildRapidPullRequestDetailExportContent(
        details = details,
        participantId = details.defaultParticipantId,
        rapidThreshold = rapidThreshold,
        overallRank = rapidPullRequests.rank,
        overallScore = rapidPullRequests.score,
    )
    return ProjectStatsExportPayload(
        projectId = userId,
        projectName = userName,
        description = role,
        customerName = projectTitle,
        repositoryUrl = selectedRepository?.title,
        periodLabel = "${visibleRange.startLabel} - ${visibleRange.endLabel}",
        generatedAtLabel = StatsExportCopy.now(),
        summaryCards = listOf(
            ProjectStatsSummaryCard(StatsExportCopy.commits(), commits.primaryValue, commits.primaryCaption),
            ProjectStatsSummaryCard(
                "Issue",
                (issues.openIssues + issues.closedIssues).toString(),
                StatsExportCopy.total(),
            ),
            ProjectStatsSummaryCard("Pull Requests", pullRequests.primaryValue, pullRequests.primaryCaption),
            ProjectStatsSummaryCard(
                StatsExportCopy.rapidPrShort(),
                rapidPullRequests.primaryValue,
                rapidPullRequests.primaryCaption,
            ),
        ),
        sections = buildList {
            add(commits.toExportSection())
            add(issues.toExportSection())
            add(pullRequests.toExportSection())
            addAll(rapidExport.sections)
            add(codeChurn.toExportSection())
            add(codeOwnership.toExportSection())
            add(dominantWeekDay.toExportSection())
        }
    )
}

private fun UserStatsUiModel.toSectionExportPayload(
    section: StatsScreenSection,
    participantId: String? = details.defaultParticipantId,
): ProjectStatsExportPayload {
    val selectedRepository = repositories.firstOrNull { it.id == selectedRepositoryId }
    val base = ProjectStatsExportPayload(
        projectId = userId,
        projectName = userName,
        description = role,
        customerName = projectTitle,
        repositoryUrl = selectedRepository?.title,
        periodLabel = "${visibleRange.startLabel} - ${visibleRange.endLabel}",
        generatedAtLabel = StatsExportCopy.now(),
    )

    return when (section) {
        StatsScreenSection.Commits -> {
            val allCommits = details.commits
                .let { if (participantId != null) it.filter { c -> c.authorId == participantId } else it }
            val totalAdditions = allCommits.sumOf { it.additions }
            val totalDeletions = allCommits.sumOf { it.deletions }
            base.copy(
                summaryCards = listOf(
                    ProjectStatsSummaryCard(StatsExportCopy.commits(), allCommits.size.toString(), StatsExportCopy.total()),
                    ProjectStatsSummaryCard(StatsExportCopy.added(), "+$totalAdditions", StatsExportCopy.lines()),
                    ProjectStatsSummaryCard(StatsExportCopy.removed(), "-$totalDeletions", StatsExportCopy.lines()),
                ),
                sections = listOf(
                    ProjectStatsSection(
                        title = StatsExportCopy.commitListTitle(),
                        score = commits.score,
                        chart = buildDailyCountChart(
                            title = localizeRuntime("График коммитов", "Commit chart"),
                            dates = allCommits.mapNotNull { it.committedAtIso },
                            pointHint = { count ->
                                localizeRuntime(
                                    "$count ${if (count == 1) "коммит" else "коммитов"}",
                                    "$count ${if (count == 1) "commit" else "commits"}",
                                )
                            },
                        ),
                        table = ProjectStatsTable(
                            headers = listOf(
                                localizeRuntime("Коммит", "Commit"),
                                localizeRuntime("Дата", "Date"),
                                localizeRuntime("Автор", "Author"),
                                localizeRuntime("Добавлено", "Added"),
                                localizeRuntime("Удалено", "Removed"),
                                localizeRuntime("Файлы", "Files"),
                                "SHA",
                                localizeRuntime("Ссылка", "Link"),
                            ),
                            rows = buildCommitExportTableRows(
                                allCommits.sortedByDescending { it.committedAtIso },
                            ),
                        ),
                    )
                )
            )
        }

        StatsScreenSection.Issues -> {
            val allIssues = details.issues.filterByParticipant(participantId)
            val openCount = allIssues.count { it.closedAtIso.isNullOrBlank() }
            val closedCount = allIssues.size - openCount
            base.copy(
                summaryCards = listOf(
                    ProjectStatsSummaryCard("Issue", allIssues.size.toString(), StatsExportCopy.total()),
                    ProjectStatsSummaryCard(StatsExportCopy.openIssuesShort(), openCount.toString(), ""),
                    ProjectStatsSummaryCard(StatsExportCopy.closedIssuesShort(), closedCount.toString(), ""),
                ),
                sections = listOf(
                    ProjectStatsSection(
                        title = StatsExportCopy.issueListTitle(),
                        rows = allIssues
                            .sortedByDescending { it.createdAtIso }
                            .map { issue ->
                                ProjectStatsTableRow(
                                    label = issue.title,
                                    value = issue.state?.uppercase() ?: "—",
                                    note = buildString {
                                        append(issue.creatorName)
                                        append("  ${StatsExportCopy.createdField()} ${issue.createdAtLabel}")
                                        issue.closedAtLabel?.let {
                                            append("  ${StatsExportCopy.closedField()} $it")
                                        }
                                        issue.number?.let { append("  #$it") }
                                    }
                                )
                            }
                    )
                )
            )
        }

        StatsScreenSection.PullRequests -> {
            val allPRs = details.pullRequests
                .let { if (participantId != null) it.filter { pr -> pr.authorId == participantId } else it }
            val openCount = allPRs.count { it.closedAtIso.isNullOrBlank() }
            val closedCount = allPRs.size - openCount
            base.copy(
                summaryCards = listOf(
                    ProjectStatsSummaryCard("Pull Requests", allPRs.size.toString(), StatsExportCopy.total()),
                    ProjectStatsSummaryCard(StatsExportCopy.openIssuesShort(), openCount.toString(), ""),
                    ProjectStatsSummaryCard(StatsExportCopy.closedIssuesShort(), closedCount.toString(), ""),
                ),
                sections = listOf(
                    ProjectStatsSection(
                        title = StatsExportCopy.prListTitle(),
                        score = pullRequests.score,
                        chart = buildDailyCountChart(
                            title = localizeRuntime("График Pull Requests", "Pull request chart"),
                            dates = allPRs.mapNotNull { it.createdAtIso },
                            pointHint = { count ->
                                localizeRuntime(
                                    "$count ${if (count == 1) "PR" else "PR"}",
                                    "$count ${if (count == 1) "PR" else "PRs"}",
                                )
                            },
                        ),
                        table = ProjectStatsTable(
                            headers = listOf(
                                "PR",
                                localizeRuntime("Статус", "Status"),
                                localizeRuntime("Автор", "Author"),
                                localizeRuntime("Назначенные", "Assignees"),
                                localizeRuntime("Создано", "Created"),
                                localizeRuntime("Закрыто", "Closed"),
                                localizeRuntime("Комментарии", "Comments"),
                                localizeRuntime("Коммиты", "Commits"),
                                localizeRuntime("Файлы", "Files"),
                                localizeRuntime("Изменения", "Changes"),
                                localizeRuntime("Ссылка", "Link"),
                            ),
                            rows = buildPullRequestExportTableRows(
                                allPRs.sortedByDescending { it.createdAtIso },
                            ),
                        ),
                    )
                )
            )
        }

        StatsScreenSection.RapidPullRequests -> {
            val rapidExport = buildRapidPullRequestDetailExportContent(
                details = details,
                participantId = participantId,
                rapidThreshold = rapidThreshold,
                overallRank = rapidPullRequests.rank,
                overallScore = rapidPullRequests.score,
            )
            base.copy(
                summaryCards = rapidExport.summaryCards,
                sections = rapidExport.sections,
            )
        }

        StatsScreenSection.CodeChurn -> base.copy(
            sections = listOf(codeChurn.toExportSection()),
        )

        StatsScreenSection.CodeOwnership -> base.copy(
            sections = listOf(codeOwnership.toExportSection()),
        )

        StatsScreenSection.DominantWeekDay -> base.copy(
            sections = listOf(dominantWeekDay.toExportSection()),
        )
    }
}
