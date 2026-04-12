package com.spbu.projecttrack.rating.presentation.userstats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spbu.projecttrack.core.theme.AppColors
import com.spbu.projecttrack.core.theme.AppFonts
import com.spbu.projecttrack.rating.data.model.ProjectStatsDonutSliceUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsIssueSectionUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsMetricSectionUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsOwnershipSectionUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsWeekDaySectionUi
import com.spbu.projecttrack.rating.data.model.UserStatsUiModel
import com.spbu.projecttrack.rating.presentation.details.StatsDetailScreen
import com.spbu.projecttrack.rating.presentation.settings.StatsScreenSection
import com.spbu.projecttrack.rating.presentation.settings.StatsScreenSettingsScreen
import com.spbu.projecttrack.rating.presentation.settings.StatsScreenSettingsTarget
import com.spbu.projecttrack.rating.presentation.settings.defaultStatsScreenSectionIds
import com.spbu.projecttrack.rating.presentation.settings.statsScreenSectionsFromIds
import com.spbu.projecttrack.rating.presentation.projectstats.ActionPillButton
import com.spbu.projecttrack.rating.presentation.projectstats.AnimatedSection
import com.spbu.projecttrack.rating.presentation.projectstats.ChartCard
import com.spbu.projecttrack.rating.presentation.projectstats.DoubleMetricRow
import com.spbu.projecttrack.rating.presentation.projectstats.EmptyDetailedInfoCard
import com.spbu.projecttrack.rating.presentation.projectstats.ErrorState
import com.spbu.projecttrack.rating.presentation.projectstats.FileStatsCard
import com.spbu.projecttrack.rating.presentation.projectstats.FooterActions
import com.spbu.projecttrack.rating.presentation.projectstats.RepositorySelectorCard
import com.spbu.projecttrack.rating.presentation.projectstats.ScoreCard
import com.spbu.projecttrack.rating.presentation.projectstats.SectionHeader
import com.spbu.projecttrack.rating.presentation.projectstats.SingleMetricCard
import com.spbu.projecttrack.rating.presentation.projectstats.StatsBackgroundLogo
import com.spbu.projecttrack.rating.presentation.projectstats.StatsDateRangePickerDialog
import com.spbu.projecttrack.rating.presentation.projectstats.StatsTopBar
import com.spbu.projecttrack.rating.presentation.projectstats.StatsTopBarTotalHeight
import com.spbu.projecttrack.rating.presentation.projectstats.WeekdayLegendText
import com.spbu.projecttrack.rating.presentation.projectstats.metricScoreTitle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.backhandler.BackHandler
import projecttrack.composeapp.generated.resources.Res

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
    onOverallRatingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showSettingsScreen by remember { mutableStateOf(false) }
    var activeDetailSection by remember { mutableStateOf<StatsScreenSection?>(null) }
    var activeSectionIds by rememberSaveable { mutableStateOf(defaultStatsScreenSectionIds()) }
    val activeSections = remember(activeSectionIds) { statsScreenSectionsFromIds(activeSectionIds) }

    LaunchedEffect(viewModel) {
        viewModel.load()
    }

    BackHandler(enabled = activeDetailSection != null || showSettingsScreen) {
        when {
            activeDetailSection != null -> activeDetailSection = null
            showSettingsScreen -> showSettingsScreen = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            StatsBackgroundLogo()

            when (val state = uiState) {
                UserStatsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.Color3)
                    }
                }

                is UserStatsUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.retry() },
                        onBackClick = onBackClick
                    )
                }

                is UserStatsUiState.Success -> {
                    UserStatsContent(
                        model = state.data,
                        visibleSections = activeSections,
                        onBackClick = onBackClick,
                        onProjectClick = onProjectClick,
                        onRepositorySelected = viewModel::selectRepository,
                        onDateRangeSelected = viewModel::selectDateRange,
                        onRapidThresholdChanged = viewModel::updateRapidThreshold,
                        onOverallRatingClick = onOverallRatingClick,
                        onDetailsClick = { section ->
                            activeDetailSection = section
                        },
                        onSettingsClick = {
                            showSettingsScreen = true
                        },
                        onExportPdfClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Экспорт в PDF скоро появится")
                            }
                        },
                        onExportExcelClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Экспорт в Excel скоро появится")
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
                    onBackClick = { showSettingsScreen = false },
                )
            }

            val detailSection = activeDetailSection
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
                    onBackClick = { activeDetailSection = null },
                    onRepositorySelected = viewModel::selectRepository,
                    onDateRangeSelected = viewModel::selectDateRange,
                    onRapidThresholdChanged = viewModel::updateRapidThreshold,
                    onExportPdfClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Экспорт в PDF скоро появится")
                        }
                    },
                    onExportExcelClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Экспорт в Excel скоро появится")
                        }
                    },
                )
            }
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
    onOverallRatingClick: () -> Unit,
    onDetailsClick: (StatsScreenSection) -> Unit,
    onSettingsClick: () -> Unit,
    onExportPdfClick: () -> Unit,
    onExportExcelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDateRangePicker by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                ),
            contentPadding = PaddingValues(
                start = UserStatsHorizontalPadding,
                end = UserStatsHorizontalPadding,
                top = StatsTopBarTotalHeight + 8.dp,
                bottom = if (model.showOverallRatingButton) 108.dp else 40.dp,
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
                        caption = "место в команде"
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
                        PersonalDominantWeekDaySection(
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
            title = "Статистика",
            onBackClick = onBackClick,
            modifier = Modifier.align(Alignment.TopCenter)
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

        AnimatedVisibility(
            visible = model.showOverallRatingButton,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .clickable(onClick = onOverallRatingClick)
                    .border(1.dp, AppColors.BorderColor, UserStatsButtonShape)
                    .background(brush = UserStatsAccentGradient, shape = UserStatsButtonShape)
            ) {
                Text(
                    text = "Общий рейтинг",
                    fontFamily = AppFonts.OpenSansBold,
                    fontSize = 16.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun PersonalStatsSummary(
    userName: String,
    role: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = userName,
            fontFamily = AppFonts.OpenSansBold,
            fontSize = 20.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.2.sp,
            color = Color.Black,
            modifier = Modifier.padding(start = 3.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 3.dp)
        ) {
            Text(
                text = role,
                fontFamily = AppFonts.OpenSansLight,
                fontSize = 16.sp,
                color = AppColors.Color2
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
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.White,
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
                fontFamily = AppFonts.OpenSansMedium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = AppColors.Color2,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "→",
                fontFamily = AppFonts.OpenSansBold,
                fontSize = 24.sp,
                color = AppColors.Color2
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
            title = metricScoreTitle(section.title)
        )
    }
}

@Composable
private fun PersonalIssueSection(
    section: ProjectStatsIssueSectionUi,
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
            leftValue = section.openIssues.toString(),
            leftCaption = "открытых Issue",
            rightValue = section.closedIssues.toString(),
            rightCaption = "закрытых Issue"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                color = Color.White,
                shape = UserStatsCardShape,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .background(Color(0xFFE9E9E9), RoundedCornerShape(6.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(section.progress.coerceIn(0f, 1f))
                                .height(30.dp)
                                .background(AppColors.Color3, RoundedCornerShape(6.dp))
                        )
                    }
                    Text(
                        text = section.remainingText,
                        fontFamily = AppFonts.OpenSansMedium,
                        fontSize = 13.sp,
                        color = AppColors.Color2
                    )
                }
            }
            SingleMetricCard(
                modifier = Modifier.weight(1f),
                value = section.rank?.toString() ?: "—",
                caption = "место в рейтинге"
            )
        }

        ScoreCard(
            score = section.score,
            title = "оценка Issue"
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
                caption = "место в рейтинге"
            )
        }

        ScoreCard(
            score = section.score,
            title = "оценка быстрых PR"
        )
    }
}

@Composable
private fun PersonalCodeChurnSection(
    section: com.spbu.projecttrack.rating.data.model.ProjectStatsCodeChurnSectionUi,
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
        FileStatsCard(rows = section.fileRows)
        DoubleMetricRow(
            leftValue = section.changedFilesCount.toString(),
            leftCaption = "изменено файлов",
            rightValue = section.rank?.toString() ?: "—",
            rightCaption = "место в рейтинге"
        )
        ScoreCard(
            score = section.score,
            title = "оценка изменчивости кода"
        )
    }
}

@Composable
private fun PersonalOwnershipSection(
    section: ProjectStatsOwnershipSectionUi,
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
        Surface(
            color = Color.White,
            shape = UserStatsCardShape,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                com.spbu.projecttrack.rating.presentation.projectstats.DonutChart(slices = section.slices)
                DonutLegend(
                    slices = section.slices,
                    highlightSuffix = " (Вы)"
                )
            }
        }
        SingleMetricCard(
            value = section.rank?.toString() ?: "—",
            caption = "место в рейтинге"
        )
        ScoreCard(
            score = section.score,
            title = "оценка владения кодом"
        )
    }
}

@Composable
private fun PersonalDominantWeekDaySection(
    section: ProjectStatsWeekDaySectionUi,
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
        Surface(
            color = Color.White,
            shape = UserStatsCardShape,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                com.spbu.projecttrack.rating.presentation.projectstats.DonutChart(slices = section.slices)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    section.slices.forEach { slice ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(slice.colorHex), CircleShape)
                            )
                            WeekdayLegendText(
                                label = slice.label,
                                secondaryLabel = slice.secondaryLabel,
                                emphasizeLabel = slice.highlight,
                            )
                        }
                    }
                }
            }
        }
        Surface(
            color = Color.White,
            shape = UserStatsCardShape,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Text(
                    text = section.headline.uppercase(),
                    fontFamily = AppFonts.OpenSansBold,
                    fontSize = 32.sp,
                    lineHeight = 20.sp,
                    letterSpacing = 0.32.sp,
                    color = AppColors.Color3,
                )
                Text(
                    text = section.subtitle,
                    fontFamily = AppFonts.OpenSansRegular,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    letterSpacing = 0.14.sp,
                    color = AppColors.Color2,
                )
            }
        }
        ScoreCard(
            score = section.score,
            title = "оценка доминирующего дня недели"
        )
    }
}

@Composable
private fun DonutLegend(
    slices: List<ProjectStatsDonutSliceUi>,
    highlightSuffix: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        slices.forEach { slice ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(slice.colorHex), CircleShape)
                )
                Column {
                    Text(
                        text = buildString {
                            append(slice.label)
                            if (slice.highlight) append(highlightSuffix)
                        },
                        fontFamily = AppFonts.OpenSansRegular,
                        fontSize = 13.sp,
                        color = AppColors.Color2
                    )
                    Text(
                        text = slice.secondaryLabel,
                        fontFamily = AppFonts.OpenSansBold,
                        fontSize = 12.sp,
                        color = AppColors.Color2
                    )
                }
            }
        }
    }
}

private enum class UserStatsDatePickerTarget {
    Start,
    End,
}
