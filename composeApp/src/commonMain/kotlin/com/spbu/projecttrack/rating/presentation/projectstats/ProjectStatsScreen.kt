package com.spbu.projecttrack.rating.presentation.projectstats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import com.spbu.projecttrack.core.theme.AppColors
import com.spbu.projecttrack.core.theme.AppFonts
import com.spbu.projecttrack.rating.data.model.ProjectStatsChartPointUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsChartType
import com.spbu.projecttrack.rating.data.model.ProjectStatsCodeChurnSectionUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsDonutSliceUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsIssueSectionUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsMemberUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsMetricRowUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsMetricSectionUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsOwnershipSectionUi
import com.spbu.projecttrack.rating.data.model.StatsDetailDataUi
import com.spbu.projecttrack.rating.data.model.StatsDetailParticipantUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsUiModel
import com.spbu.projecttrack.rating.data.model.ProjectStatsWeekDaySectionUi
import com.spbu.projecttrack.rating.data.model.filterByParticipant
import com.spbu.projecttrack.rating.export.ProjectStatsChart
import com.spbu.projecttrack.rating.export.ProjectStatsChartPoint
import com.spbu.projecttrack.rating.export.ProjectStatsChartSegment
import com.spbu.projecttrack.rating.export.ProjectStatsExportPayload
import com.spbu.projecttrack.rating.export.ProjectStatsMemberRow
import com.spbu.projecttrack.rating.export.ProjectStatsSection
import com.spbu.projecttrack.rating.export.ProjectStatsSummaryCard
import com.spbu.projecttrack.rating.export.ProjectStatsTableRow
import com.spbu.projecttrack.rating.export.buildRapidPullRequestDetailExportContent
import com.spbu.projecttrack.rating.export.rememberProjectStatsExporter
import com.spbu.projecttrack.rating.presentation.details.StatsDetailScreen
import com.spbu.projecttrack.rating.presentation.settings.StatsScreenSection
import com.spbu.projecttrack.rating.presentation.settings.StatsScreenSettingsScreen
import com.spbu.projecttrack.rating.presentation.settings.StatsScreenSettingsTarget
import com.spbu.projecttrack.rating.presentation.settings.defaultStatsScreenSectionIds
import com.spbu.projecttrack.rating.presentation.settings.statsScreenSectionsFromIds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.backhandler.BackHandler
import projecttrack.composeapp.generated.resources.Res
import projecttrack.composeapp.generated.resources.spbu_logo
import projecttrack.composeapp.generated.resources.stats_back
import projecttrack.composeapp.generated.resources.stats_calendar
import projecttrack.composeapp.generated.resources.stats_dropdown_chevron
import projecttrack.composeapp.generated.resources.stats_footer_excel
import projecttrack.composeapp.generated.resources.stats_footer_pdf
import projecttrack.composeapp.generated.resources.stats_footer_settings
import projecttrack.composeapp.generated.resources.stats_tooltip_close
import kotlin.math.PI
import kotlin.math.roundToInt

internal val CardShape = RoundedCornerShape(10.dp)
private val CompactControlShape = RoundedCornerShape(5.dp)
private val ActionButtonShape = RoundedCornerShape(5.dp)
private val OverallRatingShape = RoundedCornerShape(10.dp)
private val ScreenHorizontalPadding = 21.dp
private val FileStatsValueColumnWidth = 92.dp
internal val StatsTopBarTotalHeight = 74.dp
private val AccentGradient = Brush.verticalGradient(
    colors = listOf(AppColors.GradientStart, AppColors.GradientEndAlt)
)
private val TableHeaderColor = Color(0x4DBDBDBD)
private val TableDividerColor = Color(0xFFBDBDBD)
private val ChartGridColor = Color(0xFFE3E3E6)
private val ChartAxisColor = ChartGridColor
private val BarShape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)
private val ChartPlotTopPadding = 10.dp
private val ChartPlotBottomPadding = 6.dp
private val ChartHorizontalPadding = 8.dp
private val ChartYAxisGap = 4.dp
private val ChartXAxisGap = 4.dp
internal val CompactStatsCardHeight = 70.dp
internal val MetricCardHorizontalPadding = 8.dp
internal val CompactStatsCardPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ProjectStatsScreen(
    viewModel: ProjectStatsViewModel,
    onBackClick: () -> Unit,
    onOverallRatingClick: () -> Unit,
    onMemberStatsClick: (ProjectStatsMemberUi) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val exporter = rememberProjectStatsExporter()
    var showSettingsScreen by remember { mutableStateOf(false) }
    var activeDetailSection by remember { mutableStateOf<StatsScreenSection?>(null) }
    var renderedDetailSection by remember { mutableStateOf<StatsScreenSection?>(null) }
    var activeSectionIds by rememberSaveable { mutableStateOf(defaultStatsScreenSectionIds()) }
    val activeSections = remember(activeSectionIds) { statsScreenSectionsFromIds(activeSectionIds) }
    val detailTransitionState = remember { MutableTransitionState(false) }

    LaunchedEffect(viewModel) {
        viewModel.load()
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
            Image(
                painter = painterResource(Res.drawable.spbu_logo),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().align(Alignment.Center),
                contentScale = ContentScale.Fit,
                alpha = 1.0f,
            )

            when (val state = uiState) {
                ProjectStatsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.Color3)
                    }
                }

                is ProjectStatsUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.retry() },
                        onBackClick = onBackClick
                    )
                }

                is ProjectStatsUiState.Success -> {
                    val model = state.data
                    ProjectStatsContent(
                        model = model,
                        visibleSections = activeSections,
                        onBackClick = onBackClick,
                        onRepositorySelected = viewModel::selectRepository,
                        onDateRangeSelected = viewModel::selectDateRange,
                        onRapidThresholdChanged = viewModel::updateRapidThreshold,
                        onOverallRatingClick = onOverallRatingClick,
                        onMemberStatsClick = onMemberStatsClick,
                        onDetailsClick = { section ->
                            activeDetailSection = section
                        },
                        onSettingsClick = {
                            showSettingsScreen = true
                        },
                        onExportPdfClick = {
                            scope.launch {
                                val payload = model.toExportPayload()
                                val result = exporter.exportPdf(payload)
                                val message = result.getOrNull()?.let { export ->
                                    "PDF сохранен: ${export.fileName}"
                                } ?: "Не удалось экспортировать PDF"
                                snackbarHostState.showSnackbar(message)
                            }
                        },
                        onExportExcelClick = {
                            scope.launch {
                                val payload = model.toExportPayload()
                                val result = exporter.exportExcelCsv(payload)
                                val message = result.getOrNull()?.let { export ->
                                    "CSV сохранен: ${export.fileName}"
                                } ?: "Не удалось экспортировать Excel"
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    )
                }
            }

            if (showSettingsScreen) {
                StatsScreenSettingsScreen(
                    target = StatsScreenSettingsTarget.Project,
                    activeSectionIds = activeSectionIds,
                    onActiveSectionIdsChange = { activeSectionIds = it },
                    onBackClick = { showSettingsScreen = false },
                )
            }

            AnimatedVisibility(
                visibleState = detailTransitionState,
                enter = slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(tween(300)),
                exit = slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut(tween(300)),
                modifier = Modifier.fillMaxSize(),
            ) {
                val detailSection = renderedDetailSection
                if (detailSection != null && uiState is ProjectStatsUiState.Success) {
                    val model = (uiState as ProjectStatsUiState.Success).data
                    StatsDetailScreen(
                        section = detailSection,
                        repositories = model.repositories,
                        selectedRepositoryId = model.selectedRepositoryId,
                        visibleRange = model.visibleRange,
                        rapidThreshold = model.rapidThreshold,
                        details = model.details,
                        allowParticipantFilter = true,
                        initialParticipantId = null,
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
                        onExportPdfClick = { participantId ->
                            scope.launch {
                                val payload = model.toSectionExportPayload(
                                    section = detailSection,
                                    participantId = participantId,
                                )
                                val result = exporter.exportPdf(payload)
                                val message = result.getOrNull()?.let { export ->
                                    "PDF сохранен: ${export.fileName}"
                                } ?: "Не удалось экспортировать PDF"
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
                                    "CSV сохранен: ${export.fileName}"
                                } ?: "Не удалось экспортировать Excel"
                                snackbarHostState.showSnackbar(message)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Статистика недоступна",
                fontFamily = AppFonts.OpenSansBold,
                fontSize = 24.sp,
                color = AppColors.Color3
            )
            Text(
                text = message,
                fontFamily = AppFonts.OpenSansRegular,
                fontSize = 14.sp,
                color = AppColors.Color2,
                textAlign = TextAlign.Center
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionPillButton(
                    text = "Назад",
                    onClick = onBackClick
                )
                ActionPillButton(
                    text = "Повторить",
                    onClick = onRetry
                )
            }
        }
    }
}

@Composable
private fun ProjectStatsContent(
    model: ProjectStatsUiModel,
    visibleSections: List<StatsScreenSection>,
    onBackClick: () -> Unit,
    onRepositorySelected: (String) -> Unit,
    onDateRangeSelected: (String, String) -> Unit,
    onRapidThresholdChanged: (Int, Int, Int) -> Unit,
    onOverallRatingClick: () -> Unit,
    onMemberStatsClick: (ProjectStatsMemberUi) -> Unit,
    onDetailsClick: (StatsScreenSection) -> Unit,
    onSettingsClick: () -> Unit,
    onExportPdfClick: () -> Unit,
    onExportExcelClick: () -> Unit,
    modifier: Modifier = Modifier
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
                start = ScreenHorizontalPadding,
                end = ScreenHorizontalPadding,
                top = StatsTopBarTotalHeight + 8.dp,
                bottom = if (model.showOverallRatingButton) 148.dp else 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = model.title,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 20.sp,
                        lineHeight = 20.sp,
                        fontFamily = AppFonts.OpenSansBold,
                        color = Color(0xFF000000),
                        letterSpacing = 0.2.sp,
                    ),
                    textAlign = TextAlign.Left,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 3.dp, end = 20.dp)
                )
            }
            item {
                AnimatedSection {
                    StatsValueCard(
                        title = "Заказчик",
                        content = {
                            Text(
                                text = model.customer,
                                fontFamily = AppFonts.OpenSansRegular,
                                fontSize = 13.sp,
                                color = AppColors.Color2
                            )
                        }
                    )
                }
            }
            item {
                AnimatedSection {
                    TeamMembersCard(
                        members = model.members,
                        onMemberStatsClick = onMemberStatsClick
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
            if (model.repositories.isNotEmpty()) {
                items(
                    items = visibleSections,
                    key = { it.id },
                ) { section ->
                    when (section) {
                        StatsScreenSection.Commits -> AnimatedSection {
                            MetricSection(
                                section = model.commits,
                                onDetailsClick = { onDetailsClick(StatsScreenSection.Commits) }
                            )
                        }

                        StatsScreenSection.Issues -> AnimatedSection {
                            IssueSection(
                                section = model.issues,
                                onDetailsClick = { onDetailsClick(StatsScreenSection.Issues) }
                            )
                        }

                        StatsScreenSection.PullRequests -> AnimatedSection {
                            MetricSection(
                                section = model.pullRequests,
                                onDetailsClick = { onDetailsClick(StatsScreenSection.PullRequests) }
                            )
                        }

                        StatsScreenSection.RapidPullRequests -> AnimatedSection {
                            MetricSection(
                                section = model.rapidPullRequests,
                                rapidThreshold = model.rapidThreshold,
                                onRapidThresholdChanged = onRapidThresholdChanged,
                                onDetailsClick = { onDetailsClick(StatsScreenSection.RapidPullRequests) }
                            )
                        }

                        StatsScreenSection.CodeChurn -> AnimatedSection {
                            CodeChurnSection(
                                section = model.codeChurn,
                                onDetailsClick = { onDetailsClick(StatsScreenSection.CodeChurn) }
                            )
                        }

                        StatsScreenSection.CodeOwnership -> AnimatedSection {
                            OwnershipSection(
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
                    .border(1.dp, AppColors.BorderColor, OverallRatingShape)
                    .background(brush = AccentGradient, shape = OverallRatingShape)
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
internal fun AnimatedSection(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) { content() }
}

@Composable
internal fun BoxScope.StatsBackgroundLogo(
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(Res.drawable.spbu_logo),
        contentDescription = null,
        modifier = modifier
            .fillMaxWidth()
            .align(Alignment.Center)
            .offset(y = (-12).dp),
        contentScale = ContentScale.FillWidth,
        alpha = 0.08f,
    )
}

@Composable
internal fun StatsTopBar(
    title: String,
    onBackClick: () -> Unit,
    titleFontSize: androidx.compose.ui.unit.TextUnit = 40.sp,
    titleLineHeight: androidx.compose.ui.unit.TextUnit = 20.sp,
    titleMaxLines: Int = 1,
    modifier: Modifier = Modifier
) {
    val backInteractionSource = remember { MutableInteractionSource() }
    val backPressed by backInteractionSource.collectIsPressedAsState()
    val backScale by animateFloatAsState(
        targetValue = if (backPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "stats_topbar_back_scale"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 4.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(horizontal = ScreenHorizontalPadding, vertical = 12.dp)
                .height(50.dp)
        ) {
            Image(
                painter = painterResource(Res.drawable.stats_back),
                contentDescription = "Назад",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = backScale
                        scaleY = backScale
                    }
                    .clickable(
                        interactionSource = backInteractionSource,
                        indication = null,
                        onClick = onBackClick,
                    )
            )
            Text(
                text = title,
                fontFamily = AppFonts.OpenSansBold,
                fontSize = titleFontSize,
                lineHeight = titleLineHeight,
                letterSpacing = if (titleFontSize >= 40.sp) 0.4.sp else 0.16.sp,
                color = AppColors.Color3,
                textAlign = TextAlign.Center,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 44.dp)
            )
        }
    }
}

@Composable
private fun StatsValueCard(
    title: String,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    StatsCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            StatsCardTitle(text = title)
            content()
        }
    }
}

@Composable
private fun TeamMembersCard(
    members: List<ProjectStatsMemberUi>,
    onMemberStatsClick: (ProjectStatsMemberUi) -> Unit,
    modifier: Modifier = Modifier
) {
    StatsCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StatsCardTitle(text = "Участники команды")

            members.forEachIndexed { index, member ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = buildAnnotatedString {
                                    append(member.name)
                                    if (member.isCurrentUser) {
                                        append(" ")
                                        withStyle(
                                            style = androidx.compose.ui.text.SpanStyle(
                                                color = AppColors.Color3,
                                                fontWeight = FontWeight.Bold
                                            )
                                        ) {
                                            append("(Вы)")
                                        }
                                    }
                                },
                                fontFamily = AppFonts.OpenSansRegular,
                                fontSize = 12.sp,
                                lineHeight = 20.sp,
                                color = AppColors.Color2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = member.role,
                                fontFamily = AppFonts.OpenSansLight,
                                fontSize = 10.sp,
                                lineHeight = 15.sp,
                                color = AppColors.Color1
                            )
                        }

                        Text(
                            text = "Статистика",
                            fontFamily = AppFonts.OpenSansSemiBold,
                            fontSize = 12.sp,
                            color = Color.Black,
                            modifier = Modifier.clickable {
                                onMemberStatsClick(member)
                            }
                        )
                    }

                    if (index < members.lastIndex) {
                        DividerLine()
                    }
                }
            }
        }
    }
}

@Composable
internal fun EmptyDetailedInfoCard(
    modifier: Modifier = Modifier
) {
    StatsCard(modifier = modifier) {
        Text(
            text = "Нет подробной информации по репозиториям",
            fontFamily = AppFonts.OpenSansMedium,
            fontSize = 14.sp,
            color = AppColors.Color2
        )
    }
}

@Composable
internal fun RepositorySelectorCard(
    repositories: List<com.spbu.projecttrack.rating.data.model.ProjectStatsRepositoryUi>,
    selectedId: String,
    visibleRange: com.spbu.projecttrack.rating.data.model.ProjectStatsDateRangeUi,
    onRepositorySelected: (String) -> Unit,
    onDateRangeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedRepository = repositories.firstOrNull { it.id == selectedId } ?: repositories.first()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DropdownSelector(
            title = "Выбор репозитория",
            value = selectedRepository.title,
            options = repositories.map { it.id to it.title },
            selectedKey = selectedId,
            onSelected = onRepositorySelected
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatsCardTitle(text = "Выбор периода")
            DateRangeSelector(
                startLabel = visibleRange.startLabel,
                endLabel = visibleRange.endLabel,
                onClick = onDateRangeClick,
            )
        }
    }
}

@Composable
private fun MetricSection(
    section: ProjectStatsMetricSectionUi,
    rapidThreshold: com.spbu.projecttrack.rating.data.model.ProjectStatsThresholdUi? = null,
    onRapidThresholdChanged: ((Int, Int, Int) -> Unit)? = null,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(
            title = section.title,
            onDetailsClick = onDetailsClick
        )

        if (rapidThreshold != null && onRapidThresholdChanged != null) {
            RapidThresholdSelector(
                threshold = rapidThreshold,
                onThresholdChanged = onRapidThresholdChanged
            )
        }

        if (
            section.title == "Pull Requests" &&
            !section.supplementaryValue.isNullOrBlank() &&
            !section.supplementaryCaption.isNullOrBlank()
        ) {
            SingleMetricCard(
                value = section.supplementaryValue,
                caption = section.supplementaryCaption,
            )

            DoubleMetricRow(
                leftValue = section.primaryValue,
                leftCaption = section.primaryCaption,
                rightValue = section.rank?.toString() ?: "—",
                rightCaption = section.rankCaption
            )
        } else if (!section.supplementaryValue.isNullOrBlank() && !section.supplementaryCaption.isNullOrBlank()) {
            TripleMetricRow(
                firstValue = section.supplementaryValue,
                firstCaption = section.supplementaryCaption,
                secondValue = section.primaryValue,
                secondCaption = section.primaryCaption,
                thirdValue = section.rank?.toString() ?: "—",
                thirdCaption = section.rankCaption,
            )
        } else {
            DoubleMetricRow(
                leftValue = section.primaryValue,
                leftCaption = section.primaryCaption,
                rightValue = section.rank?.toString() ?: "—",
                rightCaption = section.rankCaption
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

        TableCard(
            title = section.tableTitle,
            rows = section.tableRows
        )

        ScoreCard(
            score = section.score,
            title = metricScoreTitle(section.title)
        )
    }
}

@Composable
private fun IssueSection(
    section: ProjectStatsIssueSectionUi,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(
            title = section.title,
            onDetailsClick = onDetailsClick
        )

        if (section.openIssues + section.closedIssues > 0) {
            DoubleMetricRow(
                leftValue = section.openIssues.toString(),
                leftCaption = "открытых Issue",
                rightValue = section.closedIssues.toString(),
                rightCaption = "закрытых Issue"
            )
        }

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
                caption = "место в рейтинге"
            )
        }

        TableCard(
            title = "Количество Issue(открытые/закрытые)",
            rows = section.tableRows
        )

        ScoreCard(
            score = section.score,
            title = "оценка Issue"
        )
    }
}

@Composable
private fun CodeChurnSection(
    section: ProjectStatsCodeChurnSectionUi,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            leftCaption = "изменено файлов",
            rightValue = section.rank?.toString() ?: "—",
            rightCaption = "место в рейтинге"
        )

        TableCard(
            title = "Количество измененных файлов",
            rows = section.tableRows
        )

        ScoreCard(
            score = section.score,
            title = "оценка изменчивости кода"
        )
    }
}

@Composable
private fun OwnershipSection(
    section: ProjectStatsOwnershipSectionUi,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(
            title = section.title,
            onDetailsClick = onDetailsClick
        )

        StatsCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DonutChart(slices = section.slices)
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
                            Column {
                                Text(
                                    text = buildString {
                                        append(slice.label)
                                        if (slice.highlight) append(" (Вы)")
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
internal fun DominantWeekDaySection(
    section: ProjectStatsWeekDaySectionUi,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(
            title = section.title,
            onDetailsClick = onDetailsClick
        )

        WeekDayDistributionCard(slices = section.slices)

        if (section.slices.any { it.value > 0f }) {
            CompactStatsCard {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
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
                        lineHeight = 16.sp,
                        letterSpacing = 0.14.sp,
                        color = AppColors.Color2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        ScoreCard(
            score = section.score,
            title = "оценка доминирующего дня недели"
        )
    }
}

@Composable
internal fun WeekDayDistributionCard(
    slices: List<ProjectStatsDonutSliceUi>,
    modifier: Modifier = Modifier,
    emptyText: String = "Действий ещё не было",
) {
    val hasActivity = slices.any { it.value > 0f }

    StatsCard(modifier = modifier) {
        if (!hasActivity) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyText,
                    fontFamily = AppFonts.OpenSansRegular,
                    fontSize = 14.sp,
                    color = AppColors.Color2,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DonutChart(slices = slices)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
    }
}

@Composable
internal fun WeekdayLegendText(
    label: String,
    secondaryLabel: String,
    emphasizeLabel: Boolean,
    modifier: Modifier = Modifier,
) {
    val (valuePart, suffixPart) = remember(secondaryLabel) { splitMetricCount(secondaryLabel) }
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    fontWeight = if (emphasizeLabel) FontWeight.Bold else FontWeight.SemiBold,
                    color = AppColors.Color2,
                )
            ) {
                append(label)
            }
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Normal,
                    color = AppColors.Color2,
                )
            ) {
                append(" - ")
            }
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Color3,
                )
            ) {
                append(valuePart)
            }
            if (suffixPart.isNotBlank()) {
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Normal,
                        color = AppColors.Color2,
                    )
                ) {
                    append(" ")
                    append(suffixPart)
                }
            }
        },
        fontFamily = AppFonts.OpenSansRegular,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.14.sp,
        color = AppColors.Color2,
        modifier = modifier,
    )
}

@Composable
internal fun SectionHeader(
    title: String,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatsCardTitle(text = title)
        ActionPillButton(
            text = "Подробнее",
            onClick = onDetailsClick
        )
    }
}

@Composable
internal fun ActionPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 650f),
        label = "action_pill_scale"
    )
    val shadowElevation by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 8.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 700f),
        label = "action_pill_shadow"
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = ActionButtonShape,
        color = Color.Transparent,
        shadowElevation = shadowElevation
    ) {
        Box(
            modifier = Modifier
                .background(brush = AccentGradient, shape = ActionButtonShape)
                .width(120.dp)
                .height(30.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontFamily = AppFonts.OpenSansBold,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.16.sp,
                color = Color.White,
            )
        }
    }
}

@Composable
internal fun DoubleMetricRow(
    leftValue: String,
    leftCaption: String,
    rightValue: String,
    rightCaption: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SingleMetricCard(
            modifier = Modifier.weight(1f),
            value = leftValue,
            caption = leftCaption
        )
        SingleMetricCard(
            modifier = Modifier.weight(1f),
            value = rightValue,
            caption = rightCaption
        )
    }
}

@Composable
internal fun TripleMetricRow(
    firstValue: String,
    firstCaption: String,
    secondValue: String,
    secondCaption: String,
    thirdValue: String,
    thirdCaption: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SingleMetricCard(
            modifier = Modifier.weight(1f),
            value = firstValue,
            caption = firstCaption
        )
        SingleMetricCard(
            modifier = Modifier.weight(1f),
            value = secondValue,
            caption = secondCaption
        )
        SingleMetricCard(
            modifier = Modifier.weight(1f),
            value = thirdValue,
            caption = thirdCaption
        )
    }
}

@Composable
internal fun SingleMetricCard(
    value: String,
    caption: String,
    modifier: Modifier = Modifier
) {
    CompactStatsCard(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = MetricCardHorizontalPadding, vertical = 0.dp),
    ) {
        val valueFontSize = when {
            value.length > 10 -> 24.sp
            value.length > 7 -> 28.sp
            else -> 32.sp
        }
        val valueLineHeight = when (valueFontSize) {
            24.sp -> 28.sp
            28.sp -> 30.sp
            else -> 32.sp
        }
        EqualVerticalMetricLayout(
            modifier = Modifier.fillMaxSize(),
            value = {
                Text(
                    text = value,
                    fontFamily = AppFonts.OpenSansBold,
                    fontSize = valueFontSize,
                    lineHeight = valueLineHeight,
                    color = AppColors.Color3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            caption = {
                Text(
                    text = caption,
                    fontFamily = AppFonts.OpenSansMedium,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    color = AppColors.Color2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
        )
    }
}

@Composable
internal fun IssueProgressCard(
    progress: Float,
    remainingText: String,
    modifier: Modifier = Modifier,
) {
    CompactStatsCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(Color(0xFFE9E9E9), RoundedCornerShape(6.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(30.dp)
                        .background(AppColors.Color3, RoundedCornerShape(6.dp))
                )
            }
            Text(
                text = remainingText,
                fontFamily = AppFonts.OpenSansMedium,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                color = AppColors.Color2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun ScoreCard(
    score: Double?,
    title: String,
    modifier: Modifier = Modifier
) {
    val scoreText = score?.let(::formatScoreValue) ?: "—"
    val color = projectScoreColor(score)
    CompactStatsCard(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = MetricCardHorizontalPadding, vertical = 0.dp),
    ) {
        val valueFontSize = 32.sp
        EqualVerticalMetricLayout(
            modifier = Modifier.fillMaxSize(),
            value = {
                Text(
                    text = scoreText,
                    fontFamily = AppFonts.OpenSansBold,
                    fontSize = valueFontSize,
                    lineHeight = valueFontSize,
                    color = color,
                )
            },
            caption = {
                Text(
                    text = title,
                    fontFamily = AppFonts.OpenSansMedium,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    color = AppColors.Color2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
        )
    }
}

@Composable
internal fun EqualVerticalMetricLayout(
    value: @Composable () -> Unit,
    caption: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        modifier = modifier,
        content = {
            Box { value() }
            Box { caption() }
        },
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val valuePlaceable = measurables[0].measure(childConstraints)
        val captionPlaceable = measurables[1].measure(childConstraints)

        val layoutWidth = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            maxOf(constraints.minWidth, valuePlaceable.width, captionPlaceable.width)
        }
        val contentHeight = valuePlaceable.height + captionPlaceable.height
        val layoutHeight = if (constraints.hasBoundedHeight) {
            constraints.maxHeight
        } else {
            maxOf(constraints.minHeight, contentHeight)
        }
        val gap = ((layoutHeight - contentHeight) / 3).coerceAtLeast(0)

        layout(layoutWidth, layoutHeight) {
            valuePlaceable.placeRelative(0, gap)
            captionPlaceable.placeRelative(0, gap * 2 + valuePlaceable.height)
        }
    }
}

internal fun projectScoreColor(score: Double?): Color {
    if (score == null) return AppColors.Color2

    val low = AppColors.Color3
    val mid = Color(0xFF9F9220)
    val high = Color(0xFF209F31)
    val clamped = score.coerceIn(1.0, 5.0).toFloat()

    return if (clamped <= 3f) {
        androidx.compose.ui.graphics.lerp(low, mid, (clamped - 1f) / 2f)
    } else {
        androidx.compose.ui.graphics.lerp(mid, high, (clamped - 3f) / 2f)
    }
}

internal fun formatScoreValue(score: Double): String {
    val rounded = (score * 10.0).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString().replace('.', ',')
    }
}

internal fun metricScoreTitle(title: String): String = when (title) {
    "Быстрые Pull Requests" -> "оценка быстрых PR"
    else -> "оценка ${title.lowercase()}"
}

@Composable
internal fun ChartCard(
    title: String,
    chartType: ProjectStatsChartType,
    points: List<ProjectStatsChartPointUi>,
    tooltipTitle: String,
    modifier: Modifier = Modifier
) {
    StatsCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatsCardTitle(text = title)

            when (chartType) {
                ProjectStatsChartType.Bars -> BarChart(points = points, tooltipTitle = tooltipTitle)
                ProjectStatsChartType.Line -> LineChart(points = points, tooltipTitle = tooltipTitle)
            }
        }
    }
}

@Composable
private fun BarChart(
    points: List<ProjectStatsChartPointUi>,
    tooltipTitle: String,
    modifier: Modifier = Modifier
) {
    val displayPoints = remember(points) { condenseChartPoints(points) }
    var selectedIndex by remember(displayPoints) { mutableStateOf<Int?>(null) }
    var tooltipSize by remember { mutableStateOf(IntSize.Zero) }
    val axisScale = remember(displayPoints) {
        buildChartAxisScale(displayPoints.maxOfOrNull { it.value } ?: 1f)
    }
    val maxValue = axisScale.maxValue
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
    ) {
        val width = maxWidth
        val chartHeight = 152.dp
        val plotHeight = chartHeight - ChartPlotTopPadding - ChartPlotBottomPadding
        val axisWidth = 24.dp
        val axisLabelGap = ChartYAxisGap
        val plotStart = ChartHorizontalPadding + axisWidth + axisLabelGap
        val plotEndPadding = plotStart
        val plotWidth = width - plotStart - plotEndPadding
        val slotWidth = (plotWidth / displayPoints.size.coerceAtLeast(1))
            .coerceAtLeast(18.dp)
        val barWidth = (slotWidth - 10.dp).coerceAtLeast(14.dp)
        val tooltipWidth = with(density) { tooltipSize.width.toDp() }
        val tooltipHalfWidth = tooltipWidth / 2
        var renderedTooltipIndex by remember(displayPoints) { mutableStateOf<Int?>(null) }
        LaunchedEffect(selectedIndex) {
            if (selectedIndex != null) {
                renderedTooltipIndex = selectedIndex
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
            ) {
                GridBackground(
                    axisLabels = axisScale.labels,
                    axisLabelWidth = axisWidth,
                    axisLabelGap = axisLabelGap,
                    highlightBaseline = true
                )
                Box(
                    modifier = Modifier
                        .width(plotWidth)
                        .height(plotHeight)
                        .align(Alignment.TopStart)
                        .offset(x = plotStart, y = ChartPlotTopPadding)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        displayPoints.forEachIndexed { index, point ->
                            val targetFraction = (point.value / maxValue).coerceIn(0f, 1f)
                            val animatedFraction by animateFloatAsState(
                                targetValue = targetFraction,
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
                                label = "bar_height_$index"
                            )
                            val barHeight = when {
                                animatedFraction <= 0f -> 0.dp
                                else -> plotHeight * animatedFraction
                            }
                            Box(
                                modifier = Modifier
                                    .width(slotWidth)
                                    .fillMaxHeight()
                            ) {
                                AnimatedBarSlot(
                                    selected = selectedIndex == index,
                                    barWidth = barWidth,
                                    barHeight = barHeight,
                                    onClick = {
                                        selectedIndex = if (selectedIndex == index) null else index
                                    }
                                )
                            }
                        }
                    }
                }
                renderedTooltipIndex?.let { index ->
                    val centerX = plotStart + slotWidth * index + (slotWidth / 2)
                    AnimatedTooltipBubble(
                        visible = selectedIndex != null,
                        text = formatChartTooltip(
                            point = displayPoints[index],
                            tooltipTitle = tooltipTitle,
                        ),
                        onClose = { selectedIndex = null },
                        modifier = Modifier
                            .onSizeChanged { tooltipSize = it }
                            .offset(
                                x = (centerX - tooltipHalfWidth).coerceIn(
                                    plotStart,
                                    width - tooltipWidth
                                ),
                                y = ChartPlotTopPadding,
                            ),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = plotStart, end = plotEndPadding, top = ChartXAxisGap),
                horizontalArrangement = Arrangement.Start
            ) {
                displayPoints.forEach { point ->
                    Box(
                        modifier = Modifier.width(slotWidth),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Text(
                            text = point.label,
                            fontFamily = AppFonts.OpenSansMedium,
                            fontSize = 11.sp,
                            lineHeight = 12.sp,
                            letterSpacing = 0.11.sp,
                            color = AppColors.Color2,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LineChart(
    points: List<ProjectStatsChartPointUi>,
    tooltipTitle: String,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
    ) {
        val width = maxWidth
        val chartHeight = 152.dp
        val plotHeight = chartHeight - ChartPlotTopPadding - ChartPlotBottomPadding
        val axisWidth = 24.dp
        val axisLabelGap = ChartYAxisGap
        val plotStart = ChartHorizontalPadding + axisWidth + axisLabelGap
        val plotEndPadding = plotStart
        val maxVisiblePoints = (((width - plotStart - plotEndPadding) / 28.dp).toInt())
            .coerceAtLeast(4)
        val displayPoints = remember(points, maxVisiblePoints) {
            condenseChartPoints(points, maxVisiblePoints)
        }
        var selectedIndex by remember(displayPoints) { mutableStateOf<Int?>(null) }
        var tooltipSize by remember { mutableStateOf(IntSize.Zero) }
        val axisScale = remember(displayPoints) {
            buildChartAxisScale(displayPoints.maxOfOrNull { it.value } ?: 1f)
        }
        val maxValue = axisScale.maxValue
        val density = LocalDensity.current
        val slotWidth = ((width - plotStart - plotEndPadding) / displayPoints.size.coerceAtLeast(1))
            .coerceAtLeast(18.dp)
        val tooltipWidth = with(density) { tooltipSize.width.toDp() }
        val tooltipHeight = with(density) { tooltipSize.height.toDp() }
        val tooltipHalfWidth = tooltipWidth / 2
        var renderedTooltipIndex by remember(displayPoints) { mutableStateOf<Int?>(null) }
        LaunchedEffect(selectedIndex) {
            if (selectedIndex != null) {
                renderedTooltipIndex = selectedIndex
            }
        }
        val pointPositions = remember(displayPoints, plotHeight, maxValue) {
            displayPoints.mapIndexed { index, point ->
                val fraction = (point.value / maxValue).coerceIn(0f, 1f)
                Offset(
                    x = (plotStart + slotWidth * index + (slotWidth / 2)).value,
                    y = (ChartPlotTopPadding + plotHeight * (1f - fraction)).value
                )
            }
        }

        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
            ) {
                GridBackground(
                    axisLabels = axisScale.labels,
                    axisLabelWidth = axisWidth,
                    axisLabelGap = axisLabelGap,
                    highlightBaseline = true
                )
                Canvas(modifier = Modifier.matchParentSize()) {
                    val path = Path()
                    pointPositions.forEachIndexed { index, point ->
                        val px = point.x.dp.toPx()
                        val py = point.y.dp.toPx()
                        if (index == 0) {
                            path.moveTo(px, py)
                        } else {
                            path.lineTo(px, py)
                        }
                    }
                    drawPath(
                        path = path,
                        color = AppColors.Color2,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                pointPositions.forEachIndexed { index, point ->
                    AnimatedChartPointButton(
                        center = point,
                        selected = selectedIndex == index,
                        onClick = {
                            selectedIndex = if (selectedIndex == index) null else index
                        }
                    )
                }

                renderedTooltipIndex?.let { index ->
                    val tooltipX = (pointPositions[index].x.dp - tooltipHalfWidth)
                        .coerceIn(plotStart, width - tooltipWidth)
                    val pointTopY = (pointPositions[index].y - 18f).dp
                    val tooltipY = (pointTopY - tooltipHeight - 8.dp)
                        .coerceAtLeast(ChartPlotTopPadding)
                    AnimatedTooltipBubble(
                        visible = selectedIndex != null,
                        text = formatChartTooltip(
                            point = displayPoints[index],
                            tooltipTitle = tooltipTitle,
                            includeLabel = true,
                        ),
                        onClose = { selectedIndex = null },
                        modifier = Modifier
                            .onSizeChanged { tooltipSize = it }
                            .offset(
                                x = tooltipX,
                                y = tooltipY,
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun GridBackground(
    axisLabels: List<Int>,
    axisLabelWidth: Dp,
    axisLabelGap: Dp,
    highlightBaseline: Boolean = false,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val plotHeight = maxHeight - ChartPlotTopPadding - ChartPlotBottomPadding
        val rowHeight = 16.dp
        val lastIndex = axisLabels.lastIndex.coerceAtLeast(1)

        axisLabels.forEachIndexed { index, label ->
            val lineY = ChartPlotTopPadding + plotHeight * (index / lastIndex.toFloat())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .padding(
                        start = ChartHorizontalPadding,
                        end = ChartHorizontalPadding + axisLabelWidth + axisLabelGap
                    )
                    .offset(y = lineY - (rowHeight / 2)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label.toString(),
                    fontFamily = AppFonts.OpenSansMedium,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    letterSpacing = 0.12.sp,
                    color = AppColors.Color2,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.width(axisLabelWidth)
                )
                Spacer(modifier = Modifier.width(axisLabelGap))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (highlightBaseline && label == 0) 2.dp else 1.dp)
                        .background(
                            if (highlightBaseline && label == 0) {
                                AppColors.Color2.copy(alpha = 0.4f)
                            } else {
                                ChartGridColor
                            }
                        )
                )
            }
        }

        Box(
            modifier = Modifier
                .offset(
                    x = ChartHorizontalPadding + axisLabelWidth + axisLabelGap,
                    y = ChartPlotTopPadding
                )
                .width(1.dp)
                .height(plotHeight)
                .background(ChartGridColor)
        )
    }
}

private fun formatChartTooltip(
    point: ProjectStatsChartPointUi,
    tooltipTitle: String,
    includeLabel: Boolean = false,
): String {
    val valueText = if (tooltipTitle.contains("коммит", ignoreCase = true)) {
        val count = point.value.roundToInt()
        "$count ${pluralizeRussian(count, "коммит", "коммита", "коммитов")}"
    } else if (tooltipTitle.contains("Pull Request", ignoreCase = true) &&
        !tooltipTitle.contains("быст", ignoreCase = true)
    ) {
        val count = point.value.roundToInt()
        "$count ${if (count == 1) "открытый PR" else "открытых PR"}"
    } else {
        if (!includeLabel) tooltipTitle else point.hint.ifBlank { tooltipTitle }
    }

    if (!includeLabel) return valueText

    val label = point.tooltipLabel.trim()
    return if (label.isBlank()) valueText else "$label\n$valueText"
}

private data class ChartAxisScale(
    val maxValue: Float,
    val labels: List<Int>,
)

private fun buildChartAxisScale(rawMaxValue: Float): ChartAxisScale {
    val safeMax = rawMaxValue.coerceAtLeast(1f)
    val step = kotlin.math.ceil(safeMax / 3f).toInt().coerceAtLeast(1)
    val axisMax = step * 3
    return ChartAxisScale(
        maxValue = axisMax.toFloat(),
        labels = listOf(axisMax, axisMax - step, axisMax - (step * 2), 0),
    )
}

private fun pluralizeRussian(
    value: Int,
    one: String,
    few: String,
    many: String,
): String {
    val mod100 = value % 100
    val mod10 = value % 10
    return when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
}

@Composable
private fun AnimatedTooltipBubble(
    visible: Boolean,
    text: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(durationMillis = 180)) +
            scaleIn(
                initialScale = 0.88f,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = 560f,
                ),
            ) +
            slideInVertically(
                initialOffsetY = { -it / 3 },
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = 640f,
                ),
            ),
        exit = fadeOut(animationSpec = tween(durationMillis = 140)) +
            scaleOut(
                targetScale = 0.92f,
                animationSpec = tween(durationMillis = 170),
            ) +
            slideOutVertically(
                targetOffsetY = { -it / 5 },
                animationSpec = tween(durationMillis = 170),
            ),
    ) {
        TooltipBubble(
            text = text,
            onClose = onClose,
        )
    }
}

@Composable
private fun TooltipBubble(
    text: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .padding(start = 8.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = text,
                fontFamily = AppFonts.OpenSansRegular,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.1.sp,
                color = AppColors.Color2,
                modifier = Modifier.weight(1f, fill = false)
            )
            AnimatedTooltipCloseButton(onClick = onClose)
        }
    }
}

@Composable
private fun AnimatedTooltipCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var tapCounter by remember { mutableStateOf(0) }
    var closePending by remember { mutableStateOf(false) }
    val tapRotation = remember { Animatable(0f) }
    val tapScale = remember { Animatable(1f) }
    val haloProgress = remember { Animatable(0f) }

    LaunchedEffect(tapCounter) {
        if (tapCounter > 0) {
            tapRotation.snapTo(0f)
            tapRotation.animateTo(
                targetValue = 92f,
                animationSpec = tween(durationMillis = 170, easing = EaseOutBack),
            )
            tapRotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
    }
    LaunchedEffect(tapCounter) {
        if (tapCounter > 0) {
            tapScale.snapTo(0.84f)
            tapScale.animateTo(
                targetValue = 1.12f,
                animationSpec = tween(durationMillis = 130, easing = EaseOutBack),
            )
            tapScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.76f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
    }
    LaunchedEffect(tapCounter) {
        if (tapCounter > 0) {
            haloProgress.snapTo(0f)
            haloProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 420, easing = EaseOutBack),
            )
            haloProgress.snapTo(0f)
        }
    }
    LaunchedEffect(closePending) {
        if (closePending) {
            delay(120)
            onClick()
            closePending = false
        }
    }

    Box(
        modifier = modifier.size(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Emit halo ring on tap
        if (haloProgress.value > 0f) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        val p = haloProgress.value
                        val s = 1f + p * 0.9f
                        scaleX = s
                        scaleY = s
                        alpha = (1f - p) * 0.45f
                    }
                    .background(AppColors.Color3, CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    scaleX = tapScale.value
                    scaleY = tapScale.value
                    rotationZ = tapRotation.value
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        if (!closePending) {
                            tapCounter++
                            closePending = true
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.stats_tooltip_close),
                contentDescription = "Закрыть",
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun AnimatedChartPointButton(
    center: Offset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val infiniteTransition = rememberInfiniteTransition(label = "chart_point_pulse")
    var tapCounter by remember { mutableStateOf(0) }
    val tapScale = remember { Animatable(1f) }
    val tapBurstProgress = remember { Animatable(0f) }
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 920),
        ),
        label = "chart_point_pulse_scale",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 920),
        ),
        label = "chart_point_pulse_alpha",
    )
    LaunchedEffect(tapCounter) {
        if (tapCounter > 0) {
            tapScale.snapTo(0.86f)
            tapScale.animateTo(
                targetValue = 1.16f,
                animationSpec = tween(durationMillis = 145, easing = EaseOutBack),
            )
            tapScale.animateTo(
                targetValue = if (selected) 1.08f else 1f,
                animationSpec = spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessMediumLow),
            )
        }
    }
    LaunchedEffect(tapCounter) {
        if (tapCounter > 0) {
            tapBurstProgress.snapTo(0f)
            tapBurstProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 320),
            )
            tapBurstProgress.snapTo(0f)
        }
    }
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 760f),
        label = "chart_point_scale",
    )
    val haloAlpha by animateFloatAsState(
        targetValue = if (selected) 0.2f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 700f),
        label = "chart_point_halo_alpha",
    )
    val outerSize by animateDpAsState(
        targetValue = if (selected) 18.dp else 16.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 780f),
        label = "chart_point_outer_size",
    )
    val innerSize by animateDpAsState(
        targetValue = if (selected) 8.dp else 6.dp,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = 820f),
        label = "chart_point_inner_size",
    )

    Box(
        modifier = Modifier
            .offset(x = (center.x - 18f).dp, y = (center.y - 18f).dp)
            .size(36.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (tapBurstProgress.value > 0f) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .graphicsLayer {
                        val progress = tapBurstProgress.value
                        val burstScale = 0.9f + progress * 0.85f
                        scaleX = burstScale
                        scaleY = burstScale
                        alpha = (1f - progress) * 0.42f
                    }
                    .border(1.5.dp, AppColors.Color3.copy(alpha = 0.7f), CircleShape)
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                    .background(AppColors.Color3.copy(alpha = pulseAlpha), CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .graphicsLayer {
                    val combinedScale = scale * tapScale.value
                    scaleX = combinedScale
                    scaleY = combinedScale
                }
                .background(AppColors.Color3.copy(alpha = haloAlpha), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        tapCounter++
                        onClick()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(outerSize)
                    .graphicsLayer {
                        val combinedScale = scale * tapScale.value
                        scaleX = combinedScale
                        scaleY = combinedScale
                    }
                    .background(if (selected) AppColors.Color3 else AppColors.Color2, CircleShape)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(innerSize)
                        .background(Color.White, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun AnimatedBarSlot(
    selected: Boolean,
    barWidth: Dp,
    barHeight: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var tapCounter by remember { mutableStateOf(0) }
    val tapScale = remember { Animatable(1f) }
    val burstProgress = remember { Animatable(0f) }

    // Scale bounce on tap
    LaunchedEffect(tapCounter) {
        if (tapCounter > 0) {
            tapScale.snapTo(0.88f)
            tapScale.animateTo(
                targetValue = 1.09f,
                animationSpec = tween(durationMillis = 120, easing = EaseOutBack),
            )
            tapScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.74f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
    }
    // Burst ring fade-out on tap
    LaunchedEffect(tapCounter) {
        if (tapCounter > 0) {
            burstProgress.snapTo(0f)
            burstProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 320),
            )
            burstProgress.snapTo(0f)
        }
    }

    val barColor by animateColorAsState(
        targetValue = if (selected) AppColors.Color3 else Color(0xFFBDBDBD),
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 580f),
        label = "bar_color",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {
                tapCounter++
                onClick()
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Expanding burst ring around the bar on tap
        if (burstProgress.value > 0f && barHeight > 0.dp) {
            Box(
                modifier = Modifier
                    .width(barWidth + (burstProgress.value * 6f).dp)
                    .height((barHeight + (burstProgress.value * 4f).dp).coerceAtLeast(4.dp))
                    .graphicsLayer {
                        alpha = (1f - burstProgress.value) * 0.50f
                    }
                    .border(1.5.dp, AppColors.Color3.copy(alpha = 0.55f), BarShape),
            )
        }
        // The bar itself — scales from its bottom center
        if (barHeight > 0.dp) {
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(barHeight)
                    .graphicsLayer {
                        scaleX = tapScale.value
                        scaleY = tapScale.value
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .background(barColor, BarShape),
            )
        } else if (selected) {
            // Tiny indicator dot for zero-value selected bar
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(3.dp)
                    .background(barColor, BarShape),
            )
        }
    }
}

private fun condenseChartPoints(
    points: List<ProjectStatsChartPointUi>,
    maxPoints: Int = 4,
): List<ProjectStatsChartPointUi> {
    if (points.size <= maxPoints) return points

    val chunkSize = kotlin.math.ceil(points.size / maxPoints.toDouble()).toInt().coerceAtLeast(1)
    return points.chunked(chunkSize).map { chunk ->
        val totalValue = chunk.sumOf { it.value.toDouble() }.toFloat()
        val startLabel = chunk.first().label
        val endLabel = chunk.last().label
        val axisLabel = if (chunk.size == 1) startLabel else chartRangeAxisLabel(startLabel, endLabel)
        val tooltipLabel = if (chunk.size == 1) startLabel else chartRangeTooltipLabel(startLabel, endLabel)
        ProjectStatsChartPointUi(
            label = axisLabel,
            tooltipLabel = tooltipLabel,
            value = totalValue,
            valueLabel = totalValue.roundToInt().toString(),
            hint = if (chunk.size == 1) {
                chunk.first().hint
            } else {
                "$tooltipLabel: ${totalValue.roundToInt()}"
            }
        )
    }
}

private fun chartRangeAxisLabel(startLabel: String, endLabel: String): String {
    if (startLabel == endLabel) return startLabel
    return if (looksLikeDateLabel(startLabel) && looksLikeDateLabel(endLabel)) {
        "$startLabel\n$endLabel"
    } else {
        "$startLabel\n$endLabel"
    }
}

private fun looksLikeDateLabel(label: String): Boolean {
    return label.length == 8 && label[2] == '.' && label[5] == '.'
}

private fun chartRangeTooltipLabel(startLabel: String, endLabel: String): String {
    return if (startLabel == endLabel) startLabel else "$startLabel - $endLabel"
}

private fun splitMetricCount(text: String): Pair<String, String> {
    val trimmed = text.trim()
    val firstSpace = trimmed.indexOf(' ')
    return if (firstSpace == -1) {
        trimmed to ""
    } else {
        trimmed.substring(0, firstSpace) to trimmed.substring(firstSpace + 1).trim()
    }
}

private fun Dp.coerceIn(minimumValue: Dp, maximumValue: Dp): Dp {
    return when {
        this < minimumValue -> minimumValue
        this > maximumValue -> maximumValue
        else -> this
    }
}

@Composable
private fun TableCard(
    title: String,
    rows: List<ProjectStatsMetricRowUi>,
    modifier: Modifier = Modifier
) {
    StatsCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StatsCardTitle(text = title)
            rows.forEachIndexed { index, row ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = row.name,
                            fontFamily = AppFonts.OpenSansRegular,
                            fontSize = 13.sp,
                            color = AppColors.Color2,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = row.value,
                            fontFamily = AppFonts.OpenSansSemiBold,
                            fontSize = 13.sp,
                            color = if (row.highlight) AppColors.Color3 else AppColors.Color2
                        )
                    }
                    if (index < rows.lastIndex) {
                        DividerLine()
                    }
                }
            }
        }
    }
}

@Composable
internal fun FileStatsCard(
    rows: List<com.spbu.projecttrack.rating.data.model.ProjectStatsFileRowUi>,
    modifier: Modifier = Modifier
) {
    StatsCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StatsCardTitle(text = "Статистика по файлам")

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, TableDividerColor)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TableHeaderColor)
                            .height(40.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Файл",
                            fontFamily = AppFonts.OpenSansBold,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            letterSpacing = 0.14.sp,
                            color = AppColors.Color2,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                            textAlign = TextAlign.Center,
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(TableDividerColor)
                        )
                        Text(
                            text = "Кол-во",
                            fontFamily = AppFonts.OpenSansBold,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            letterSpacing = 0.14.sp,
                            color = AppColors.Color2,
                            modifier = Modifier
                                .width(FileStatsValueColumnWidth)
                                .padding(horizontal = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    rows.forEachIndexed { index, row ->
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = row.fileName.replace("/", "/\u200B"),
                                    fontFamily = AppFonts.OpenSansMedium,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    letterSpacing = 0.13.sp,
                                    color = AppColors.Color2,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                                    textAlign = TextAlign.Start,
                                )
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .fillMaxHeight()
                                        .background(TableDividerColor)
                                )
                                Text(
                                    text = row.value,
                                    fontFamily = AppFonts.OpenSansMedium,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    letterSpacing = 0.14.sp,
                                    color = AppColors.Color2,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .width(FileStatsValueColumnWidth)
                                        .padding(horizontal = 8.dp)
                                )
                            }
                            if (index < rows.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(TableDividerColor)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DonutSectionCard(
    slices: List<ProjectStatsDonutSliceUi>,
    modifier: Modifier = Modifier
) {
    StatsCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            DonutChart(slices = slices)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                    if (slice.highlight) append("(Вы)")
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
    }
}

@Composable
internal fun DonutChart(
    slices: List<ProjectStatsDonutSliceUi>,
    modifier: Modifier = Modifier
) {
    val total = slices.sumOf { it.value.toDouble() }.takeIf { it > 0 } ?: 1.0
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 500f),
        label = "donut_progress"
    )
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        val designOuterDiameter = 250.dp
        val designInnerDiameter = 150.dp
        val chartSize = maxWidth.coerceAtMost(designOuterDiameter)
        val scale = (chartSize.value / designOuterDiameter.value).coerceAtMost(1f)
        val outerRadius = (designOuterDiameter / 2) * scale
        val innerRadius = (designInnerDiameter / 2) * scale
        val strokeWidth = outerRadius - innerRadius

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartSize),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(chartSize)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val innerPx = innerRadius.toPx()
                val outerPx = outerRadius.toPx()
                val strokeWidthPx = strokeWidth.toPx()
                // Диаграмму никогда не увеличиваем сверх них, только уменьшаем при нехватке места.
                val arcPathPx = (innerPx + outerPx) / 2f

                var startAngle = -90f
                slices.forEach { slice ->
                    val sweep = ((slice.value / total.toFloat()) * 360f) * progress
                    drawArc(
                        color = Color(slice.colorHex),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(cx - arcPathPx, cy - arcPathPx),
                        size = androidx.compose.ui.geometry.Size(arcPathPx * 2f, arcPathPx * 2f),
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }

            // Подписи держим на середине кольца между inner/outer радиусами.
            val ringRadius = (innerRadius.value + outerRadius.value) / 2f
            var start = -90f
            slices.forEach { slice ->
                val sweep = (slice.value / total.toFloat()) * 360f
                val middle = start + sweep / 2f
                val angle = middle * (PI / 180.0)
                val x = kotlin.math.cos(angle).toFloat() * ringRadius
                val y = kotlin.math.sin(angle).toFloat() * ringRadius
                val arcLength = 2f * PI.toFloat() * ringRadius * (sweep / 360f)
                val text = slice.percentLabel
                if (slice.value > 0f && sweep >= 28f && arcLength >= text.length * 9f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = x.dp, y = y.dp)
                            .defaultMinSize(minWidth = 36.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = text,
                            fontFamily = AppFonts.OpenSansBold,
                            fontSize = 13.sp,
                            lineHeight = 15.sp,
                            letterSpacing = 0.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                start += sweep
            }
        }
    }
}

@Composable
internal fun FooterActions(
    onSettingsClick: () -> Unit,
    onExportPdfClick: () -> Unit,
    onExportExcelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        FooterActionRow(
            text = "Настройки экрана",
            icon = {
                Image(
                    painter = painterResource(Res.drawable.stats_footer_settings),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            onClick = onSettingsClick
        )
        FooterActionRow(
            text = "Экспорт в PDF",
            icon = {
                Image(
                    painter = painterResource(Res.drawable.stats_footer_pdf),
                    contentDescription = null,
                    modifier = Modifier
                        .width(16.dp)
                        .height(22.4.dp)
                )
            },
            onClick = onExportPdfClick
        )
        FooterActionRow(
            text = "Экспорт в Excel",
            icon = {
                Image(
                    painter = painterResource(Res.drawable.stats_footer_excel),
                    contentDescription = null,
                    modifier = Modifier
                        .width(16.dp)
                        .height(22.4.dp)
                )
            },
            onClick = onExportExcelClick
        )
    }
}

@Composable
private fun FooterActionRow(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "footer_press_scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Фиксированная ширина = самая широкая иконка (Settings, 20dp).
        // Все иконки центрируются внутри — их центры совпадают, текст начинается с одного X.
        Box(
            modifier = Modifier.width(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Text(
            text = text,
            fontFamily = AppFonts.OpenSansMedium,
            fontSize = 14.sp,
            color = AppColors.Color2
        )
    }
}

@Composable
private fun DateRangeSelector(
    startLabel: String,
    endLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DateBadge(
        text = "$startLabel - $endLabel",
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun DateBadge(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(22.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(5.dp),
        color = AppColors.Color1,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, end = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontFamily = AppFonts.OpenSansRegular,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.14.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier.size(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.stats_calendar),
                    contentDescription = "Календарь",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

@Composable
private fun StatsCardTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        fontFamily = AppFonts.OpenSansSemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.16.sp,
        color = Color.Black,
        modifier = modifier,
    )
}

@Composable
internal fun DropdownSelector(
    title: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectedKey: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    var anchorSize by remember { mutableStateOf(IntSize.Zero) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 700f),
        label = "stats_dropdown_chevron_rotation",
    )
    val density = LocalDensity.current
    val popupWidth = with(density) { anchorSize.width.toDp() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (title.isBlank()) 0.dp else 6.dp)
    ) {
        if (title.isNotBlank()) {
            StatsCardTitle(text = title)
        }

        Box {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { anchorSize = it }
                    .clickable { expanded = !expanded },
                shape = CompactControlShape,
                color = AppColors.Color1
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .padding(start = 10.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = value,
                        fontFamily = AppFonts.OpenSansRegular,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        letterSpacing = 0.14.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Image(
                        painter = painterResource(Res.drawable.stats_dropdown_chevron),
                        contentDescription = null,
                        modifier = Modifier
                            .width(8.5.dp)
                            .height(7.dp)
                            .rotate(chevronRotation)
                    )
                }
            }

            StatsDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                options = options,
                onSelected = onSelected,
                width = if (anchorSize.width > 0) popupWidth else null,
                offset = DpOffset(0.dp, 6.dp),
                selectedKey = selectedKey,
                selectedLabel = value,
            )
        }
    }
}

@Composable
private fun RapidThresholdSelector(
    threshold: com.spbu.projecttrack.rating.data.model.ProjectStatsThresholdUi,
    onThresholdChanged: (Int, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatsCardTitle(text = "Период:")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InlineValueSelector(
                label = "д",
                value = threshold.days,
                values = (0..30).toList(),
                onSelected = { onThresholdChanged(it, threshold.hours, threshold.minutes) },
                modifier = Modifier.weight(1f)
            )
            InlineValueSelector(
                label = "ч",
                value = threshold.hours,
                values = (0..23).toList(),
                onSelected = { onThresholdChanged(threshold.days, it, threshold.minutes) },
                modifier = Modifier.weight(1f)
            )
            InlineValueSelector(
                label = "мин",
                value = threshold.minutes,
                values = (0..59).toList(),
                onSelected = { onThresholdChanged(threshold.days, threshold.hours, it) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun InlineValueSelector(
    label: String,
    value: Int,
    values: List<Int>,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownSelector(
        title = "",
        value = "$value $label",
        options = values.map { it.toString() to "$it $label" },
        onSelected = { onSelected(it.toInt()) },
        modifier = modifier,
        selectedKey = value.toString(),
    )
}

@Composable
private fun FilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) AppColors.Color3 else Color.White,
        tonalElevation = if (selected) 2.dp else 0.dp,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD8D8DA))
    ) {
        Text(
            text = text,
            fontFamily = AppFonts.OpenSansRegular,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 12.sp,
            color = if (selected) Color.White else AppColors.Color2,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
internal fun StatsCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
    contentVerticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = contentVerticalArrangement,
            content = content
        )
    }
}

@Composable
internal fun CompactStatsCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = CompactStatsCardPadding,
    content: @Composable ColumnScope.() -> Unit
) {
    StatsCard(
        modifier = modifier.height(CompactStatsCardHeight),
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
private fun DividerLine(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFE4E4E6))
    )
}

private fun ProjectStatsUiModel.toExportPayload(): ProjectStatsExportPayload {
    val selectedRepository = repositories.firstOrNull { it.id == selectedRepositoryId }
    val rapidExport = buildRapidPullRequestDetailExportContent(
        details = details,
        participantId = null,
        rapidThreshold = rapidThreshold,
        overallRank = rapidPullRequests.rank,
        overallScore = rapidPullRequests.score,
    )
    return ProjectStatsExportPayload(
        projectId = projectId,
        projectName = title,
        customerName = customer,
        repositoryUrl = selectedRepository?.title,
        periodLabel = "${visibleRange.startLabel} - ${visibleRange.endLabel}",
        generatedAtLabel = "Сейчас",
        summaryCards = listOf(
            ProjectStatsSummaryCard("Коммиты", commits.primaryValue, commits.primaryCaption),
            ProjectStatsSummaryCard("Issue", (issues.openIssues + issues.closedIssues).toString(), "всего"),
            ProjectStatsSummaryCard("Pull Requests", pullRequests.primaryValue, pullRequests.primaryCaption),
            ProjectStatsSummaryCard("Быстрые PR", rapidPullRequests.primaryValue, rapidPullRequests.primaryCaption)
        ),
        members = members.map { member ->
            ProjectStatsMemberRow(
                name = member.name,
                role = member.role,
                marker = if (member.isCurrentUser) "Вы" else null
            )
        },
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

private fun ProjectStatsUiModel.toSectionExportPayload(
    section: StatsScreenSection,
    participantId: String? = null,
): ProjectStatsExportPayload {
    val selectedRepository = repositories.firstOrNull { it.id == selectedRepositoryId }
    val base = ProjectStatsExportPayload(
        projectId = projectId,
        projectName = title,
        customerName = customer,
        repositoryUrl = selectedRepository?.title,
        periodLabel = "${visibleRange.startLabel} - ${visibleRange.endLabel}",
        generatedAtLabel = "Сейчас",
        members = members.map { member ->
            ProjectStatsMemberRow(
                name = member.name,
                role = member.role,
                marker = if (member.isCurrentUser) "Вы" else null
            )
        },
    )

    return when (section) {
        StatsScreenSection.Commits -> {
            val allCommits = details.commits
                .let { if (participantId != null) it.filter { commit -> commit.authorId == participantId } else it }
            val totalAdditions = allCommits.sumOf { it.additions }
            val totalDeletions = allCommits.sumOf { it.deletions }
            base.copy(
                summaryCards = listOf(
                    ProjectStatsSummaryCard("Коммиты", allCommits.size.toString(), "всего"),
                    ProjectStatsSummaryCard("Добавлено", "+$totalAdditions", "строк"),
                    ProjectStatsSummaryCard("Удалено", "-$totalDeletions", "строк"),
                ),
                sections = listOf(
                    ProjectStatsSection(
                        title = "Список коммитов",
                        rows = allCommits
                            .sortedByDescending { it.committedAtIso }
                            .map { commit ->
                                ProjectStatsTableRow(
                                    label = commit.message,
                                    value = commit.committedAtLabel,
                                    note = buildString {
                                        append(commit.authorName)
                                        append("  +${commit.additions}/-${commit.deletions}")
                                        if (commit.files.isNotEmpty()) append("  ${commit.files.size} файлов")
                                        commit.sha?.take(7)?.let { append("  [$it]") }
                                    }
                                )
                            }
                    )
                )
            )
        }

        StatsScreenSection.Issues -> {
            val allIssues = details.issues.filterByParticipant(participantId)
            val openCount = allIssues.count { it.closedAtIso.isNullOrBlank() }
            val closedCount = allIssues.size - openCount

            // Создатели: сколько Issues создал каждый
            val creatorCounts = linkedMapOf<String, Int>()
            allIssues.forEach { issue ->
                val name = issue.creatorName.ifBlank { issue.creatorId ?: "Неизвестно" }
                creatorCounts[name] = (creatorCounts[name] ?: 0) + 1
            }

            // Исполнители: сколько Issues закрыл каждый
            val assigneeClosedCounts = linkedMapOf<String, Int>()
            allIssues.forEach { issue ->
                val isClosed = !issue.closedAtIso.isNullOrBlank()
                val names = if (issue.assigneeNames.isNotEmpty()) issue.assigneeNames
                            else issue.assigneeIds
                names.forEach { name ->
                    if (isClosed) assigneeClosedCounts[name] = (assigneeClosedCounts[name] ?: 0) + 1
                    else assigneeClosedCounts.getOrPut(name) { 0 }
                }
            }

            // Метки: сколько Issues с каждой меткой
            val labelCounts = linkedMapOf<String, Int>()
            allIssues.flatMap { it.labels }.forEach { label ->
                labelCounts[label] = (labelCounts[label] ?: 0) + 1
            }

            val sections = buildList {
                add(ProjectStatsSection(
                    title = "Список Issues",
                    rows = allIssues
                        .sortedByDescending { it.createdAtIso }
                        .map { issue ->
                            ProjectStatsTableRow(
                                label = issue.title,
                                value = issue.state?.uppercase() ?: "—",
                                note = buildString {
                                    append(issue.creatorName)
                                    append("  Создано: ${issue.createdAtLabel}")
                                    issue.closedAtLabel?.let { append("  Закрыто: $it") }
                                    if (issue.assigneeNames.isNotEmpty()) {
                                        append("  Исполнители: ${issue.assigneeNames.joinToString()}")
                                    }
                                    if (issue.labels.isNotEmpty()) {
                                        append("  Метки: ${issue.labels.joinToString()}")
                                    }
                                    issue.number?.let { append("  #$it") }
                                }
                            )
                        }
                ))
                if (creatorCounts.isNotEmpty()) {
                    add(ProjectStatsSection(
                        title = "Создатели Issues",
                        rows = creatorCounts.entries
                            .sortedByDescending { it.value }
                            .map { (name, count) ->
                                ProjectStatsTableRow(label = name, value = "создано $count Issue")
                            }
                    ))
                }
                if (assigneeClosedCounts.isNotEmpty()) {
                    add(ProjectStatsSection(
                        title = "Исполнители Issues",
                        rows = assigneeClosedCounts.entries
                            .sortedByDescending { it.value }
                            .map { (name, closedCount) ->
                                ProjectStatsTableRow(label = name, value = "закрыто $closedCount Issue")
                            }
                    ))
                }
                if (labelCounts.isNotEmpty()) {
                    add(ProjectStatsSection(
                        title = "Метки",
                        rows = labelCounts.entries
                            .sortedByDescending { it.value }
                            .map { (label, count) ->
                                ProjectStatsTableRow(label = label, value = "$count issue")
                            }
                    ))
                }
            }

            base.copy(
                summaryCards = listOf(
                    ProjectStatsSummaryCard("Issue", allIssues.size.toString(), "всего"),
                    ProjectStatsSummaryCard("Открытых", openCount.toString(), ""),
                    ProjectStatsSummaryCard("Закрытых", closedCount.toString(), ""),
                ),
                sections = sections,
            )
        }

        StatsScreenSection.PullRequests -> {
            val allPRs = details.pullRequests
                .let { if (participantId != null) it.filter { pr -> pr.authorId == participantId } else it }
            val openCount = allPRs.count { it.closedAtIso.isNullOrBlank() }
            val closedCount = allPRs.size - openCount
            base.copy(
                summaryCards = listOf(
                    ProjectStatsSummaryCard("Pull Requests", allPRs.size.toString(), "всего"),
                    ProjectStatsSummaryCard("Открытых", openCount.toString(), ""),
                    ProjectStatsSummaryCard("Закрытых", closedCount.toString(), ""),
                ),
                sections = listOf(
                    ProjectStatsSection(
                        title = "Список Pull Requests",
                        rows = allPRs
                            .sortedByDescending { it.createdAtIso }
                            .map { pr ->
                                ProjectStatsTableRow(
                                    label = pr.title,
                                    value = pr.state?.uppercase() ?: "—",
                                    note = buildString {
                                        append(pr.authorName)
                                        append("  Создано: ${pr.createdAtLabel}")
                                        pr.closedAtLabel?.let { append("  Закрыто: $it") }
                                        val add = pr.additions; val del = pr.deletions
                                        if (add != null || del != null) append("  +${add ?: 0}/-${del ?: 0}")
                                        pr.number?.let { append("  #$it") }
                                    }
                                )
                            }
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

        StatsScreenSection.CodeOwnership -> {
            val ownershipRows = buildOwnershipExportRows(details)
            val totalLines = ownershipRows.sumOf { it.lines }
            val selectedRow = ownershipRows.firstOrNull { it.id == participantId }
            val score = when {
                participantId == null -> codeOwnership.score
                selectedRow != null && totalLines > 0 && selectedRow.lines > 0 -> round2(
                    (2 + 3 * (selectedRow.lines.toDouble() / totalLines.toDouble())).coerceIn(0.0, 5.0)
                )
                else -> null
            }
            val rank = when {
                participantId == null -> codeOwnership.rank
                score == null -> null
                else -> ownershipRows
                    .mapNotNull { row ->
                        if (totalLines > 0 && row.lines > 0) {
                            round2((2 + 3 * (row.lines.toDouble() / totalLines.toDouble())).coerceIn(0.0, 5.0))
                        } else {
                            null
                        }
                    }
                    .sortedDescending()
                    .indexOfFirst { it == score }
                    .takeIf { it >= 0 }
                    ?.plus(1)
            }
            val rankingLabel = if (participantId == null) "Место в рейтинге" else "Место в команде"

            base.copy(
                summaryCards = listOf(
                    ProjectStatsSummaryCard("Всего строк", totalLines.toString(), "владение кодом"),
                    ProjectStatsSummaryCard(rankingLabel, rank?.toString() ?: "—"),
                    ProjectStatsSummaryCard("Оценка владения кодом", score?.let(::formatScoreValue) ?: "—"),
                ),
                sections = buildList {
                    add(
                        ProjectStatsSection(
                            title = "Владение кодом",
                            subtitle = "Score: ${score?.let(::formatScoreValue) ?: "—"}",
                            rows = listOf(
                                ProjectStatsTableRow("Всего строк", totalLines.toString()),
                                ProjectStatsTableRow(rankingLabel, rank?.toString() ?: "—"),
                            ),
                            chart = ownershipRows.takeIf { it.isNotEmpty() }?.let { rows ->
                                ProjectStatsChart.Donut(
                                    title = "Распределение владения кодом",
                                    segments = rows.map { row ->
                                        ProjectStatsChartSegment(
                                            label = if (row.isCurrentUser) "${row.name} (Вы)" else row.name,
                                            value = row.lines.toDouble(),
                                            colorHint = percentLabel(row.lines, totalLines),
                                        )
                                    }
                                )
                            }
                        )
                    )
                    if (ownershipRows.isNotEmpty()) {
                        add(
                            ProjectStatsSection(
                                title = "Таблица участников",
                                rows = ownershipRows.map { row ->
                                    ProjectStatsTableRow(
                                        label = if (row.isCurrentUser) "${row.name} (Вы)" else row.name,
                                        value = "${row.changes} изменений",
                                        note = "+${row.additions}/-${row.deletions}  ${row.lines} строк",
                                    )
                                }
                            )
                        )
                    }
                },
            )
        }

        StatsScreenSection.DominantWeekDay -> base.copy(
            sections = listOf(dominantWeekDay.toExportSection()),
        )
    }
}

private fun buildOwnershipExportRows(details: StatsDetailDataUi): List<OwnershipExportRow> {
    val participants = linkedMapOf<String, StatsDetailParticipantUi>()
    details.participants.forEach { participant ->
        participants[participant.id] = participant
    }
    details.commits.forEach { commit ->
        val authorId = commit.authorId ?: return@forEach
        if (!participants.containsKey(authorId)) {
            participants[authorId] = StatsDetailParticipantUi(
                id = authorId,
                name = commit.authorName,
                subtitle = "Участник",
                isCurrentUser = authorId == details.defaultParticipantId,
            )
        }
    }

    return participants.values
        .map { participant ->
            val participantCommits = details.commits.filter { it.authorId == participant.id }
            OwnershipExportRow(
                id = participant.id,
                name = participant.name,
                isCurrentUser = participant.isCurrentUser || participant.id == details.defaultParticipantId,
                additions = participantCommits.sumOf { it.additions },
                deletions = participantCommits.sumOf { it.deletions },
                changes = participantCommits.size,
                lines = participantCommits.sumOf { it.additions + it.deletions },
            )
        }
        .sortedByDescending { it.lines }
}

internal fun ProjectStatsMetricSectionUi.toExportSection(): ProjectStatsSection {
    val chart = when (chartType) {
        ProjectStatsChartType.Bars -> ProjectStatsChart.Bar(
            title = chartTitle,
            points = chartPoints.map { point ->
                ProjectStatsChartPoint(
                    label = point.label,
                    value = point.value.toDouble(),
                    note = point.hint
                )
            }
        )
        ProjectStatsChartType.Line -> ProjectStatsChart.Line(
            title = chartTitle,
            points = chartPoints.map { point ->
                ProjectStatsChartPoint(
                    label = point.label,
                    value = point.value.toDouble(),
                    note = point.hint
                )
            }
        )
    }
    return ProjectStatsSection(
        title = title,
        subtitle = "Score: ${score?.let(::formatScoreValue) ?: "—"}",
        rows = buildList {
            add(ProjectStatsTableRow(primaryCaption, primaryValue))
            add(ProjectStatsTableRow(rankCaption, rank?.toString() ?: "—"))
            tableRows.forEach { row ->
                add(ProjectStatsTableRow(row.name, row.value))
            }
        },
        chart = chart
    )
}

internal fun ProjectStatsIssueSectionUi.toExportSection(): ProjectStatsSection {
    return ProjectStatsSection(
        title = title,
        subtitle = "Score: ${score?.let(::formatScoreValue) ?: "—"}",
        rows = buildList {
            add(ProjectStatsTableRow("Открытых", openIssues.toString()))
            add(ProjectStatsTableRow("Закрытых", closedIssues.toString()))
            add(ProjectStatsTableRow("Прогресс", "${(progress * 100).toInt()}%"))
            add(ProjectStatsTableRow("Рейтинг", rank?.toString() ?: "—"))
            tableRows.forEach { row ->
                add(ProjectStatsTableRow(row.name, row.value))
            }
        }
    )
}

internal fun ProjectStatsCodeChurnSectionUi.toExportSection(): ProjectStatsSection {
    return ProjectStatsSection(
        title = title,
        subtitle = "Score: ${score?.let(::formatScoreValue) ?: "—"}",
        rows = buildList {
            add(ProjectStatsTableRow("Изменено файлов", changedFilesCount.toString()))
            add(ProjectStatsTableRow("Рейтинг", rank?.toString() ?: "—"))
            if (fileRows.isNotEmpty()) {
                add(ProjectStatsTableRow("— Измененные файлы —", ""))
                fileRows.forEach { row ->
                    add(ProjectStatsTableRow(row.fileName, row.value))
                }
            }
            if (tableRows.isNotEmpty()) {
                add(ProjectStatsTableRow("— Кол-во изменений по участникам —", ""))
                tableRows.forEach { row ->
                    add(ProjectStatsTableRow(row.name, row.value))
                }
            }
        },
        chart = if (slices.isNotEmpty()) ProjectStatsChart.Donut(
            title = "Распределение изменений файлов",
            segments = slices.map { slice ->
                ProjectStatsChartSegment(
                    label = slice.label,
                    value = slice.value.toDouble(),
                    colorHint = slice.percentLabel
                )
            }
        ) else null,
        notes = buildList {
            mostChangedFileName?.takeIf { it.isNotBlank() }?.let {
                add("Самый часто изменяемый файл: $it")
            }
        }
    )
}

internal fun ProjectStatsOwnershipSectionUi.toExportSection(): ProjectStatsSection {
    return ProjectStatsSection(
        title = title,
        subtitle = "Score: ${score?.let(::formatScoreValue) ?: "—"}",
        rows = listOf(ProjectStatsTableRow("Рейтинг", rank?.toString() ?: "—")),
        chart = ProjectStatsChart.Donut(
            title = title,
            segments = slices.map { slice ->
                ProjectStatsChartSegment(
                    label = slice.label,
                    value = slice.value.toDouble(),
                    colorHint = slice.percentLabel
                )
            }
        )
    )
}

internal fun ProjectStatsWeekDaySectionUi.toExportSection(): ProjectStatsSection {
    val hasActivity = slices.any { it.value > 0f }
    val dominantLabel = slices.maxByOrNull { it.value }?.label?.takeIf { hasActivity } ?: "НЕТ ДАННЫХ"
    val leastActiveLabel = slices.minByOrNull { it.value }?.label?.takeIf { hasActivity } ?: "НЕТ ДАННЫХ"

    return ProjectStatsSection(
        title = title,
        subtitle = "Score: ${score?.let(::formatScoreValue) ?: "—"}",
        rows = listOf(
            ProjectStatsTableRow("Самый активный день недели", dominantLabel),
            ProjectStatsTableRow("Самый неактивный день недели", leastActiveLabel),
        ),
        chart = ProjectStatsChart.Donut(
            title = title,
            segments = slices.map { slice ->
                ProjectStatsChartSegment(
                    label = slice.label,
                    value = slice.value.toDouble(),
                    colorHint = slice.percentLabel
                )
            }
        )
    )
}

private data class OwnershipExportRow(
    val id: String,
    val name: String,
    val isCurrentUser: Boolean,
    val additions: Int,
    val deletions: Int,
    val changes: Int,
    val lines: Int,
)

private fun percentLabel(value: Int, total: Int): String {
    if (total <= 0) return "0%"
    return "${((value.toDouble() / total.toDouble()) * 100.0).roundToInt()}%"
}

private fun round2(value: Double): Double = ((value * 100.0).roundToInt() / 100.0)
