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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.CompositingStrategy
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
import com.spbu.projecttrack.analytics.compose.TrackScreen
import com.spbu.projecttrack.analytics.compose.TrackScrollDepth
import com.spbu.projecttrack.analytics.compose.TrackVisibility
import com.spbu.projecttrack.analytics.compose.rememberAnalyticsContext
import com.spbu.projecttrack.analytics.compose.trackTap
import com.spbu.projecttrack.analytics.model.BlockType
import com.spbu.projecttrack.core.logging.AppLog
import com.spbu.projecttrack.core.settings.localizeRuntime
import com.spbu.projecttrack.core.settings.localizedString
import com.spbu.projecttrack.core.storage.createAppPreferences
import com.spbu.projecttrack.core.theme.AppColors
import com.spbu.projecttrack.core.theme.appPalette
import com.spbu.projecttrack.core.theme.subtleBorder
import com.spbu.projecttrack.core.theme.AppFonts
import com.spbu.projecttrack.core.ui.lazyListEdgeFadeMask
import com.spbu.projecttrack.core.ui.AppSnackbarHost
import com.spbu.projecttrack.rating.data.StatsScreenSettingsPersistence
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
import com.spbu.projecttrack.rating.export.ProjectStatsTable
import com.spbu.projecttrack.rating.export.ProjectStatsTableRow
import com.spbu.projecttrack.rating.export.buildRapidPullRequestDetailExportContent
import com.spbu.projecttrack.rating.common.StatsExportCopy
import com.spbu.projecttrack.rating.export.rememberProjectStatsExporter
import com.spbu.projecttrack.rating.presentation.details.StatsDetailScreen
import com.spbu.projecttrack.rating.presentation.settings.StatsScreenSection
import com.spbu.projecttrack.rating.presentation.settings.StatsScreenSettingsScreen
import com.spbu.projecttrack.rating.presentation.settings.StatsScreenSettingsTarget
import com.spbu.projecttrack.rating.presentation.settings.statsScreenSectionsFromIds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.backhandler.BackHandler
import projecttrack.composeapp.generated.resources.Res
import projecttrack.composeapp.generated.resources.arrow_back
import projecttrack.composeapp.generated.resources.spbu_logo
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
private val FileStatsValueColumnWidth = 92.dp
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
    onMemberStatsClick: (ProjectStatsMemberUi) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = appPalette()
    val logTag = "ProjectStatsScreen"
    val analytics = rememberAnalyticsContext()
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
                appPreferences.getProjectStatsScreenSettingsJson(),
            )
        )
    }
    val activeSections = remember(activeSectionIds) { statsScreenSectionsFromIds(activeSectionIds) }
    val detailTransitionState = remember { MutableTransitionState(false) }
    val successModel = (uiState as? ProjectStatsUiState.Success)?.data
    val screenAnalyticsSession = if (successModel != null) {
        rememberProjectStatsAnalyticsSession(
            analyticsContext = analytics,
            projectId = successModel.projectId,
            initialRepositoryId = successModel.selectedRepositoryId,
            initialStartIsoDate = successModel.visibleRange.startIsoDate,
            initialEndIsoDate = successModel.visibleRange.endIsoDate,
            initialRapidThresholdMinutes = successModel.rapidThreshold.totalMinutes,
        )
    } else null

    LaunchedEffect(viewModel) {
        viewModel.load()
    }

    LaunchedEffect(activeSectionIds) {
        appPreferences.saveProjectStatsScreenSettingsJson(
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

    val dispatchBack = rememberStatsBackDispatcher(logTag) {
        "settingsVisible=$showSettingsScreen detailSection=${activeDetailSection?.id ?: "none"}"
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
                modifier = Modifier.fillMaxSize().align(Alignment.Center),
                contentScale = ContentScale.Fit,
                alpha = appPalette().spbuBackdropLogoAlpha,
            )

            when (val state = uiState) {
                ProjectStatsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = appPalette().accent)
                    }
                }

                is ProjectStatsUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.retry() },
                        onBackClick = { dispatchBack("error_back") { onBackClick() } },
                    )
                }

                is ProjectStatsUiState.Success -> {
                    val model = state.data
                    if (screenAnalyticsSession != null) {
                        val analyticsSession = screenAnalyticsSession
                        ProjectStatsContent(
                            model = model,
                            analyticsContext = analytics,
                            analyticsSession = analyticsSession,
                            visibleSections = activeSections,
                            onBackClick = { dispatchBack("stats_top_bar") { onBackClick() } },
                            onRepositorySelected = { repositoryId ->
                                analyticsSession.onRepositorySelected(repositoryId)
                                viewModel.selectRepository(repositoryId)
                            },
                            onDateRangeSelected = { startIsoDate, endIsoDate ->
                                analyticsSession.onDateRangeSelected(startIsoDate, endIsoDate)
                                viewModel.selectDateRange(startIsoDate, endIsoDate)
                            },
                            onRapidThresholdChanged = { days, hours, minutes ->
                                analyticsSession.onRapidThresholdChanged(days, hours, minutes)
                                viewModel.updateRapidThreshold(days, hours, minutes)
                            },
                            onMemberStatsClick = { member ->
                                analyticsSession.onMemberStatsOpened(member.userId)
                                onMemberStatsClick(member)
                            },
                            onDetailsClick = { section ->
                                analyticsSession.onDetailsOpened(section)
                                AppLog.d(logTag, "openDetail section=${section.id}")
                                activeDetailSection = section
                            },
                            onSettingsClick = {
                                analyticsSession.onSettingsOpened()
                                AppLog.d(logTag, "openSettings")
                                showSettingsScreen = true
                            },
                            onExportPdfClick = {
                                analyticsSession.onExportRequested(
                                    format = "pdf",
                                    scope = "screen",
                                )
                                scope.launch {
                                    val payload = model.toExportPayload()
                                    val result = exporter.exportPdf(payload)
                                    val message = result.getOrNull()?.let { export ->
                                        StatsExportCopy.pdfSaved(export.fileName)
                                    } ?: StatsExportCopy.exportPdfFailed()
                                    snackbarHostState.showSnackbar(message)
                                }
                            },
                            onExportExcelClick = {
                                analyticsSession.onExportRequested(
                                    format = "csv",
                                    scope = "screen",
                                )
                                scope.launch {
                                    val payload = model.toExportPayload()
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
            }

            AnimatedVisibility(
                visible = showSettingsScreen,
                enter = slideInVertically(animationSpec = tween(260)) { fullHeight -> fullHeight / 8 } +
                    fadeIn(animationSpec = tween(220)) +
                    scaleIn(initialScale = 0.985f, animationSpec = tween(220)),
                exit = slideOutVertically(animationSpec = tween(220)) { fullHeight -> fullHeight / 12 } +
                    fadeOut(animationSpec = tween(180)) +
                    scaleOut(targetScale = 0.992f, animationSpec = tween(180)),
                modifier = Modifier.fillMaxSize(),
            ) {
                val settingsProjectId = (uiState as? ProjectStatsUiState.Success)?.data?.projectId
                StatsScreenSettingsScreen(
                    target = StatsScreenSettingsTarget.Project,
                    activeSectionIds = activeSectionIds,
                    onActiveSectionIdsChange = { activeSectionIds = it },
                    onBackClick = { dispatchBack("settings_top_bar") { showSettingsScreen = false } },
                    analyticsProjectId = settingsProjectId,
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
                    if (screenAnalyticsSession != null) {
                        val analyticsSession = screenAnalyticsSession
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
                            onBackClick = { dispatchBack("detail_top_bar") { activeDetailSection = null } },
                            onRepositorySelected = { repositoryId ->
                                analyticsSession.onRepositorySelected(repositoryId)
                                viewModel.selectRepository(repositoryId)
                            },
                            onDateRangeSelected = { startIsoDate, endIsoDate ->
                                analyticsSession.onDateRangeSelected(startIsoDate, endIsoDate)
                                viewModel.selectDateRange(startIsoDate, endIsoDate)
                            },
                            onRapidThresholdChanged = { days, hours, minutes ->
                                analyticsSession.onRapidThresholdChanged(days, hours, minutes)
                                viewModel.updateRapidThreshold(days, hours, minutes)
                            },
                            onExportPdfClick = { participantId ->
                                analyticsSession.onExportRequested(
                                    format = "pdf",
                                    scope = "detail",
                                    sectionId = detailSection.id,
                                    participantId = participantId,
                                )
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
                                analyticsSession.onExportRequested(
                                    format = "csv",
                                    scope = "detail",
                                    sectionId = detailSection.id,
                                    participantId = participantId,
                                )
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
            }

            AppSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
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
    val unavailableTitle = localizedString("Статистика недоступна", "Statistics unavailable")
    val backLabel = localizedString("Назад", "Back")
    val retryLabel = localizedString("Повторить", "Retry")
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
                text = unavailableTitle,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = appPalette().accent
            )
            Text(
                text = message,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = appPalette().primaryText,
                textAlign = TextAlign.Center
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionPillButton(
                    text = backLabel,
                    onClick = onBackClick
                )
                ActionPillButton(
                    text = retryLabel,
                    onClick = onRetry
                )
            }
        }
    }
}

@Composable
private fun ProjectStatsContent(
    model: ProjectStatsUiModel,
    analyticsContext: com.spbu.projecttrack.analytics.compose.AnalyticsContext,
    analyticsSession: ProjectStatsAnalyticsSession,
    visibleSections: List<StatsScreenSection>,
    onBackClick: () -> Unit,
    onRepositorySelected: (String) -> Unit,
    onDateRangeSelected: (String, String) -> Unit,
    onRapidThresholdChanged: (Int, Int, Int) -> Unit,
    onMemberStatsClick: (ProjectStatsMemberUi) -> Unit,
    onDetailsClick: (StatsScreenSection) -> Unit,
    onSettingsClick: () -> Unit,
    onExportPdfClick: () -> Unit,
    onExportExcelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDateRangePicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val screenName = "project_stats_screen"

    TrackScreen(screenName, analyticsContext)
    TrackScrollDepth(
        screenName = screenName,
        scrollState = listState,
        analyticsContext = analyticsContext,
        onDepthTracked = analyticsSession::onScrollTracked,
    )
    val customerTitle = localizedString("Заказчик", "Client")
    val statsTitle = localizedString("Статистика", "Statistics")
    var topBarHeight by remember { mutableStateOf(0.dp) }
    val topContentPadding = maxOf(topBarHeight, StatsTopBarTotalHeight) + 8.dp

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .lazyListEdgeFadeMask(
                    listState = listState,
                    topInset = topContentPadding,
                ),
            contentPadding = PaddingValues(
                start = ScreenHorizontalPadding,
                end = ScreenHorizontalPadding,
                top = topContentPadding,
                bottom = 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = model.title,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 20.sp,
                        lineHeight = 20.sp,
                        fontFamily = AppFonts.OpenSans,
                        fontWeight = FontWeight.Bold,
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
                val blockSpec = MetricBlockSpec(
                    blockId = "block_customer_card",
                    blockType = BlockType.METRIC_CARD,
                    position = 1,
                )
                TrackVisibility(
                    blockId = blockSpec.blockId,
                    blockType = blockSpec.blockType,
                    screenName = screenName,
                    analyticsContext = analyticsContext,
                    onViewed = analyticsSession::onBlockViewed,
                    onFocus = { analyticsSession.onBlockFocused(it, blockSpec.position) },
                    modifier = Modifier.trackTap(
                        blockId = blockSpec.blockId,
                        blockType = blockSpec.blockType,
                        screenName = screenName,
                        analyticsContext = analyticsContext,
                        onTapTracked = { analyticsSession.onBlockTapped(it, blockSpec.position) },
                    ),
                ) {
                    AnimatedSection {
                        StatsValueCard(
                            title = customerTitle,
                            content = {
                                Text(
                                    text = model.customer,
                                    fontFamily = AppFonts.OpenSans,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = appPalette().primaryText
                                )
                            }
                        )
                    }
                }
            }
            item {
                val blockSpec = MetricBlockSpec(
                    blockId = "block_team_members",
                    blockType = BlockType.STUDENT_STATS,
                    position = 2,
                )
                TrackVisibility(
                    blockId = blockSpec.blockId,
                    blockType = blockSpec.blockType,
                    screenName = screenName,
                    analyticsContext = analyticsContext,
                    onViewed = analyticsSession::onBlockViewed,
                    onFocus = { analyticsSession.onBlockFocused(it, blockSpec.position) },
                    modifier = Modifier.trackTap(
                        blockId = blockSpec.blockId,
                        blockType = blockSpec.blockType,
                        screenName = screenName,
                        analyticsContext = analyticsContext,
                        onTapTracked = { analyticsSession.onBlockTapped(it, blockSpec.position) },
                    ),
                ) {
                    AnimatedSection {
                        TeamMembersCard(
                            members = model.members,
                            onMemberStatsClick = onMemberStatsClick
                        )
                    }
                }
            }
            item {
                val blockSpec = MetricBlockSpec(
                    blockId = "block_repo_selector",
                    blockType = BlockType.FILTER_BAR,
                    position = 3,
                )
                TrackVisibility(
                    blockId = blockSpec.blockId,
                    blockType = blockSpec.blockType,
                    screenName = screenName,
                    analyticsContext = analyticsContext,
                    onViewed = analyticsSession::onBlockViewed,
                    onFocus = { analyticsSession.onBlockFocused(it, blockSpec.position) },
                    modifier = Modifier.trackTap(
                        blockId = blockSpec.blockId,
                        blockType = blockSpec.blockType,
                        screenName = screenName,
                        analyticsContext = analyticsContext,
                        onTapTracked = { analyticsSession.onBlockTapped(it, blockSpec.position) },
                    ),
                ) {
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
            }
            if (model.repositories.isNotEmpty()) {
                items(
                    items = visibleSections,
                    key = { it.id },
                ) { section ->
                    val blockSpec = MetricBlockSpec(
                        blockId = "section_${section.id}",
                        blockType = BlockType.CHART,
                        position = visibleSections.indexOf(section) + 4,
                    )
                    TrackVisibility(
                        blockId = blockSpec.blockId,
                        blockType = blockSpec.blockType,
                        screenName = screenName,
                        analyticsContext = analyticsContext,
                        onViewed = analyticsSession::onBlockViewed,
                        onFocus = { analyticsSession.onBlockFocused(it, blockSpec.position) },
                        modifier = Modifier.trackTap(
                            blockId = blockSpec.blockId,
                            blockType = blockSpec.blockType,
                            screenName = screenName,
                            analyticsContext = analyticsContext,
                            onTapTracked = { analyticsSession.onBlockTapped(it, blockSpec.position) },
                        ),
                    ) {
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
            title = statsTitle,
            onBackClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { topBarHeight = with(density) { it.height.toDp() } },
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
        alpha = maxOf(appPalette().spbuWatermarkLogoAlpha, 0.08f),
    )
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
    val teamMembersTitle = localizedString("Участники команды", "Team members")
    val memberStatsLabel = localizedString("Статистика", "Statistics")
    val youShort = localizedString("Вы", "You")
    StatsCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StatsCardTitle(text = teamMembersTitle)

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
                                                color = appPalette().accent,
                                                fontWeight = FontWeight.Bold
                                            )
                                        ) {
                                            append("($youShort)")
                                        }
                                    }
                                },
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                lineHeight = 20.sp,
                                color = appPalette().primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = member.role,
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = FontWeight.Light,
                                fontSize = 10.sp,
                                lineHeight = 15.sp,
                                color = appPalette().subtleBorder
                            )
                        }

                        TeamMemberStatsButton(
                            text = memberStatsLabel,
                            onClick = { onMemberStatsClick(member) },
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
private fun TeamMemberStatsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 720f),
        label = "team_member_stats_scale",
    )
    val bgColor by animateColorAsState(
        targetValue = if (isPressed) appPalette().subtleBorder.copy(alpha = 0.22f) else Color.Transparent,
        animationSpec = tween(160),
        label = "team_member_stats_bg",
    )
    val textColor by animateColorAsState(
        targetValue = if (isPressed) appPalette().accent else appPalette().primaryText,
        animationSpec = tween(140),
        label = "team_member_stats_text",
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = textColor,
        )
    }
}

@Composable
internal fun EmptyDetailedInfoCard(
    modifier: Modifier = Modifier
) {
    val emptyMessage = localizedString(
        "Нет подробной информации по репозиториям",
        "No detailed repository information",
    )
    StatsCard(modifier = modifier) {
        Text(
            text = emptyMessage,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = appPalette().primaryText
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
    val repoPickerTitle = localizedString("Выбор репозитория", "Choose repository")
    val periodPickerTitle = localizedString("Выбор периода", "Choose period")

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DropdownSelector(
            title = repoPickerTitle,
            value = selectedRepository.title,
            options = repositories.map { it.id to it.title },
            selectedKey = selectedId,
            onSelected = onRepositorySelected
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatsCardTitle(text = periodPickerTitle)
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
            title = StatsExportCopy.metricScoreTitleForSection(section.title)
        )
    }
}

@Composable
private fun IssueSection(
    section: ProjectStatsIssueSectionUi,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val openIssuesCaption = localizedString("открытых Issue", "open issues")
    val closedIssuesCaption = localizedString("закрытых Issue", "closed issues")
    val rankCaption = localizedString("место в рейтинге", "rank")
    val issueCountTableTitle = localizedString(
        "Количество Issue(открытые/закрытые)",
        "Issue count (open/closed)",
    )
    val issueScoreTitle = localizedString("оценка Issue", "Issue score")
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
                leftCaption = openIssuesCaption,
                rightValue = section.closedIssues.toString(),
                rightCaption = closedIssuesCaption
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
                caption = rankCaption
            )
        }

        TableCard(
            title = issueCountTableTitle,
            rows = section.tableRows
        )

        ScoreCard(
            score = section.score,
            title = issueScoreTitle
        )
    }
}

@Composable
private fun CodeChurnSection(
    section: ProjectStatsCodeChurnSectionUi,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val changedFilesCaption = localizedString("изменено файлов", "files changed")
    val rankCaption = localizedString("место в рейтинге", "rank")
    val changedFilesTableTitle = localizedString(
        "Количество измененных файлов",
        "Changed files count",
    )
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

        TableCard(
            title = changedFilesTableTitle,
            rows = section.tableRows
        )

        ScoreCard(
            score = section.score,
            title = churnScoreTitle
        )
    }
}

@Composable
private fun OwnershipSection(
    section: ProjectStatsOwnershipSectionUi,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val youShort = localizedString("Вы", "You")
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
                                        if (slice.highlight) append(" ($youShort)")
                                    },
                                    fontFamily = AppFonts.OpenSans,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = appPalette().primaryText
                                )
                                Text(
                                    text = slice.secondaryLabel,
                                    fontFamily = AppFonts.OpenSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = appPalette().primaryText
                                )
                            }
                        }
                    }
                }
            }
        }

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

@Composable
internal fun DominantWeekDaySection(
    section: ProjectStatsWeekDaySectionUi,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dominantWeekScoreTitle = localizedString(
        "оценка доминирующего дня недели",
        "Dominant weekday score",
    )
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
                        fontFamily = AppFonts.OpenSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        lineHeight = 20.sp,
                        letterSpacing = 0.32.sp,
                        color = appPalette().accent,
                    )
                    Text(
                        text = section.subtitle,
                        fontFamily = AppFonts.OpenSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        letterSpacing = 0.14.sp,
                        color = appPalette().primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        ScoreCard(
            score = section.score,
            title = dominantWeekScoreTitle
        )
    }
}

@Composable
internal fun WeekDayDistributionCard(
    slices: List<ProjectStatsDonutSliceUi>,
    modifier: Modifier = Modifier,
    emptyText: String? = null,
) {
    val resolvedEmpty = emptyText ?: localizedString(
        "Действий ещё не было",
        "No activity yet",
    )
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
                    text = resolvedEmpty,
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = appPalette().primaryText,
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
                    color = appPalette().primaryText,
                )
            ) {
                append(label)
            }
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Normal,
                    color = appPalette().primaryText,
                )
            ) {
                append(" - ")
            }
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = appPalette().accent,
                )
            ) {
                append(valuePart)
            }
            if (suffixPart.isNotBlank()) {
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Normal,
                        color = appPalette().primaryText,
                    )
                ) {
                    append(" ")
                    append(suffixPart)
                }
            }
        },
        fontFamily = AppFonts.OpenSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.14.sp,
        color = appPalette().primaryText,
        modifier = modifier,
    )
}

@Composable
internal fun SectionHeader(
    title: String,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val detailsLabel = localizedString("Подробнее", "Details")
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatsCardTitle(text = title)
        ActionPillButton(
            text = detailsLabel,
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
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.16.sp,
                color = appPalette().buttonText,
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
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = valueFontSize,
                    lineHeight = valueLineHeight,
                    color = appPalette().accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            caption = {
                Text(
                    text = caption,
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    color = appPalette().primaryText,
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
                        .background(appPalette().accent, RoundedCornerShape(6.dp))
                )
            }
            Text(
                text = remainingText,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                color = appPalette().primaryText,
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
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = valueFontSize,
                    lineHeight = valueFontSize,
                    color = color,
                )
            },
            caption = {
                Text(
                    text = title,
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    color = appPalette().primaryText,
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
@Composable
internal fun projectScoreColor(score: Double?): Color {
    if (score == null) return appPalette().primaryText

    val low = appPalette().accent
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
                            fontFamily = AppFonts.OpenSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            lineHeight = 12.sp,
                            letterSpacing = 0.11.sp,
                            color = appPalette().primaryText,
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
                val chartLineColor = appPalette().primaryText
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
                        color = chartLineColor,
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
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    letterSpacing = 0.12.sp,
                    color = appPalette().primaryText,
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
                                appPalette().primaryText.copy(alpha = 0.4f)
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
    val tl = tooltipTitle.lowercase()
    val count = point.value.roundToInt()
    val valueText = when {
        tl.contains("коммит") || (tl.contains("commit") && !tl.contains("pull")) -> {
            val ru = "$count ${pluralizeRussian(count, "коммит", "коммита", "коммитов")}"
            val en = "$count ${if (count == 1) "commit" else "commits"}"
            localizeRuntime(ru, en)
        }
        (tl.contains("pull request") || tl.contains("pull requests") || tl.contains("pr")) &&
            !tl.contains("быст") && !tl.contains("rapid") -> {
            localizeRuntime(
                "$count ${if (count == 1) "открытый PR" else "открытых PR"}",
                "$count ${if (count == 1) "open PR" else "open PRs"}",
            )
        }
        else -> {
            if (!includeLabel) tooltipTitle else point.hint.ifBlank { tooltipTitle }
        }
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
        color = appPalette().buttonText,
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
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.1.sp,
                color = appPalette().primaryText,
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
                    .background(appPalette().accent, CircleShape)
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
                contentDescription = localizedString("Закрыть", "Close"),
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
                    .border(1.5.dp, appPalette().accent.copy(alpha = 0.7f), CircleShape)
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
                    .background(appPalette().accent.copy(alpha = pulseAlpha), CircleShape)
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
                .background(appPalette().accent.copy(alpha = haloAlpha), CircleShape)
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
                    .background(if (selected) appPalette().accent else appPalette().primaryText, CircleShape)
                    .border(2.dp, appPalette().surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(innerSize)
                        .background(appPalette().surface, CircleShape)
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
        targetValue = if (selected) appPalette().accent else Color(0xFFBDBDBD),
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
        if (burstProgress.value > 0f && barHeight > 0.dp) {
            Box(
                modifier = Modifier
                    .width(barWidth + (burstProgress.value * 6f).dp)
                    .height((barHeight + (burstProgress.value * 4f).dp).coerceAtLeast(4.dp))
                    .graphicsLayer {
                        alpha = (1f - burstProgress.value) * 0.50f
                    }
                    .border(1.5.dp, appPalette().accent.copy(alpha = 0.55f), BarShape),
            )
        }
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
                            fontFamily = AppFonts.OpenSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = appPalette().primaryText,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = row.value,
                            fontFamily = AppFonts.OpenSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (row.highlight) appPalette().accent else appPalette().primaryText
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
    val fileStatsTitle = localizedString("Статистика по файлам", "File statistics")
    val fileColumn = localizedString("Файл", "File")
    val countColumn = localizedString("Кол-во", "Count")
    StatsCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StatsCardTitle(text = fileStatsTitle)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = appPalette().buttonText,
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
                            text = fileColumn,
                            fontFamily = AppFonts.OpenSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            letterSpacing = 0.14.sp,
                            color = appPalette().primaryText,
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
                            text = countColumn,
                            fontFamily = AppFonts.OpenSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            letterSpacing = 0.14.sp,
                            color = appPalette().primaryText,
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
                                    fontFamily = AppFonts.OpenSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    letterSpacing = 0.13.sp,
                                    color = appPalette().primaryText,
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
                                    fontFamily = AppFonts.OpenSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    letterSpacing = 0.14.sp,
                                    color = appPalette().primaryText,
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
                                    if (slice.highlight) append(localizedString("(Вы)", "(You)"))
                                },
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = appPalette().primaryText
                            )
                            Text(
                                text = slice.secondaryLabel,
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = appPalette().primaryText
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
                // Preserve the design proportions and only scale the chart down when space is limited.
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

            // Keep labels centered inside the ring so shrinking the chart stays visually balanced.
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
                            fontFamily = AppFonts.OpenSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            lineHeight = 15.sp,
                            letterSpacing = 0.sp,
                            color = appPalette().buttonText,
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
    val screenSettings = localizedString("Настройки экрана", "Screen settings")
    val exportPdf = localizedString("Экспорт в PDF", "Export to PDF")
    val exportExcel = localizedString("Экспорт в Excel", "Export to Excel")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        FooterActionRow(
            text = screenSettings,
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
            text = exportPdf,
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
            text = exportExcel,
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
        // Reserve space for the widest icon so every row starts its label from the same X offset.
        Box(
            modifier = Modifier.width(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Text(
            text = text,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = appPalette().primaryText
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
        color = appPalette().subtleBorder,
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
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.14.sp,
                color = appPalette().buttonText,
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
                    contentDescription = localizedString("Календарь", "Calendar"),
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
        fontFamily = AppFonts.OpenSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.16.sp,
        color = appPalette().primaryText,
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
                color = appPalette().subtleBorder
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
                        fontFamily = AppFonts.OpenSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        letterSpacing = 0.14.sp,
                        color = appPalette().buttonText,
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
    val periodHeading = localizedString("Период:", "Period:")
    val dayShort = localizedString("д", "d")
    val hourShort = localizedString("ч", "h")
    val minShort = localizedString("мин", "min")
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatsCardTitle(text = periodHeading)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InlineValueSelector(
                label = dayShort,
                value = threshold.days,
                values = (0..30).toList(),
                onSelected = { onThresholdChanged(it, threshold.hours, threshold.minutes) },
                modifier = Modifier.weight(1f)
            )
            InlineValueSelector(
                label = hourShort,
                value = threshold.hours,
                values = (0..23).toList(),
                onSelected = { onThresholdChanged(threshold.days, it, threshold.minutes) },
                modifier = Modifier.weight(1f)
            )
            InlineValueSelector(
                label = minShort,
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
    val palette = appPalette()
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) palette.accent else palette.surface,
        tonalElevation = if (selected) 2.dp else 0.dp,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD8D8DA))
    ) {
        Text(
            text = text,
            fontFamily = AppFonts.OpenSans,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 12.sp,
            color = if (selected) palette.buttonText else palette.primaryText,
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
        color = appPalette().buttonText,
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
        generatedAtLabel = StatsExportCopy.now(),
        summaryCards = listOf(
            ProjectStatsSummaryCard(StatsExportCopy.commits(), commits.primaryValue, commits.primaryCaption),
            ProjectStatsSummaryCard(
                StatsExportCopy.issues(),
                (issues.openIssues + issues.closedIssues).toString(),
                StatsExportCopy.total(),
            ),
            ProjectStatsSummaryCard(
                StatsExportCopy.pullRequests(),
                pullRequests.primaryValue,
                pullRequests.primaryCaption,
            ),
            ProjectStatsSummaryCard(
                StatsExportCopy.rapidPrShort(),
                rapidPullRequests.primaryValue,
                rapidPullRequests.primaryCaption,
            ),
        ),
        members = members.map { member ->
            ProjectStatsMemberRow(
                name = member.name,
                role = member.role,
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
        generatedAtLabel = StatsExportCopy.now(),
        members = members.map { member ->
            ProjectStatsMemberRow(
                name = member.name,
                role = member.role,
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
                            columnFractions = listOf(0.30f, 0.13f, 0.12f, 0.08f, 0.08f, 0.07f, 0.09f, 0.13f),
                        ),
                    )
                )
            )
        }

        StatsScreenSection.Issues -> {
            val allIssues = details.issues.filterByParticipant(participantId)
            val openCount = allIssues.count { it.closedAtIso.isNullOrBlank() }
            val closedCount = allIssues.size - openCount

            val creatorCounts = linkedMapOf<String, Int>()
            allIssues.forEach { issue ->
                val name = issue.creatorName.ifBlank { issue.creatorId ?: StatsExportCopy.unknown() }
                creatorCounts[name] = (creatorCounts[name] ?: 0) + 1
            }

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

            val labelCounts = linkedMapOf<String, Int>()
            allIssues.flatMap { it.labels }.forEach { label ->
                labelCounts[label] = (labelCounts[label] ?: 0) + 1
            }

            val sections = buildList {
                add(ProjectStatsSection(
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
                                    if (issue.assigneeNames.isNotEmpty()) {
                                        append("  ${StatsExportCopy.assigneesField()} ${issue.assigneeNames.joinToString()}")
                                    }
                                    if (issue.labels.isNotEmpty()) {
                                        append("  ${StatsExportCopy.labelsField()} ${issue.labels.joinToString()}")
                                    }
                                    issue.number?.let { append("  #$it") }
                                }
                            )
                        }
                ))
                if (creatorCounts.isNotEmpty()) {
                    add(ProjectStatsSection(
                        title = StatsExportCopy.issueCreatorsTitle(),
                        rows = creatorCounts.entries
                            .sortedByDescending { it.value }
                            .map { (name, count) ->
                                ProjectStatsTableRow(
                                    label = name,
                                    value = StatsExportCopy.createdIssuesCount(count),
                                )
                            }
                    ))
                }
                if (assigneeClosedCounts.isNotEmpty()) {
                    add(ProjectStatsSection(
                        title = StatsExportCopy.issueAssigneesTitle(),
                        rows = assigneeClosedCounts.entries
                            .sortedByDescending { it.value }
                            .map { (name, closedCount) ->
                                ProjectStatsTableRow(
                                    label = name,
                                    value = StatsExportCopy.closedIssuesCount(closedCount),
                                )
                            }
                    ))
                }
                if (labelCounts.isNotEmpty()) {
                    add(ProjectStatsSection(
                        title = StatsExportCopy.labelsTitle(),
                        rows = labelCounts.entries
                            .sortedByDescending { it.value }
                            .map { (label, count) ->
                                ProjectStatsTableRow(label = label, value = StatsExportCopy.issueCountLabel(count))
                            }
                    ))
                }
            }

            base.copy(
                summaryCards = listOf(
                    ProjectStatsSummaryCard(
                        StatsExportCopy.issues(),
                        allIssues.size.toString(),
                        StatsExportCopy.total(),
                    ),
                    ProjectStatsSummaryCard(StatsExportCopy.openIssuesShort(), openCount.toString(), ""),
                    ProjectStatsSummaryCard(StatsExportCopy.closedIssuesShort(), closedCount.toString(), ""),
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
                    ProjectStatsSummaryCard(
                        StatsExportCopy.pullRequests(),
                        allPRs.size.toString(),
                        StatsExportCopy.total(),
                    ),
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
                            columnFractions = listOf(0.20f, 0.07f, 0.09f, 0.10f, 0.10f, 0.10f, 0.07f, 0.07f, 0.06f, 0.07f, 0.07f),
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
            val rankingLabel = if (participantId == null) {
                StatsExportCopy.rankingInRating()
            } else {
                StatsExportCopy.rankingInTeam()
            }

            base.copy(
                summaryCards = listOf(
                    ProjectStatsSummaryCard(
                        StatsExportCopy.totalLines(),
                        totalLines.toString(),
                        StatsExportCopy.ownershipCaption(),
                    ),
                    ProjectStatsSummaryCard(rankingLabel, rank?.toString() ?: "—"),
                    ProjectStatsSummaryCard(
                        StatsExportCopy.ownershipScoreTitle(),
                        score?.let(::formatScoreValue) ?: "—",
                    ),
                ),
                sections = buildList {
                    add(
                        ProjectStatsSection(
                            title = StatsExportCopy.ownershipSectionTitle(),
                            score = score,
                            rows = listOf(
                                ProjectStatsTableRow(StatsExportCopy.totalLines(), totalLines.toString()),
                                ProjectStatsTableRow(rankingLabel, rank?.toString() ?: "—"),
                            ),
                            chart = ownershipRows.takeIf { it.isNotEmpty() }?.let { rows ->
                                ProjectStatsChart.Donut(
                                    title = StatsExportCopy.ownershipDistributionTitle(),
                                    segments = rows.map { row ->
                                        ProjectStatsChartSegment(
                                            label = row.name,
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
                                title = StatsExportCopy.participantsTableTitle(),
                                rows = ownershipRows.map { row ->
                                    ProjectStatsTableRow(
                                        label = if (row.isCurrentUser) {
                                            row.name
                                        } else {
                                            row.name
                                        },
                                        value = StatsExportCopy.changesCount(row.changes),
                                        note = StatsExportCopy.linesNote(
                                            row.additions,
                                            row.deletions,
                                            row.lines,
                                        ),
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
                subtitle = StatsExportCopy.participant(),
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

internal fun buildDailyCountChart(
    title: String,
    dates: List<String>,
    pointHint: (Int) -> String,
): ProjectStatsChart.Bar? {
    val points = buildDailyCountChartPoints(dates, pointHint)
    if (points.isEmpty()) return null
    return ProjectStatsChart.Bar(
        title = title,
        points = points,
    )
}

private fun buildDailyCountChartPoints(
    dates: List<String>,
    pointHint: (Int) -> String,
): List<ProjectStatsChartPoint> {
    if (dates.isEmpty()) return emptyList()
    data class ChartEntry(
        val label: String,
        val epochMs: Long,
        var count: Int,
    )

    val grouped = linkedMapOf<String, ChartEntry>()
    dates.mapNotNull(::parseExportInstant).forEach { instant ->
        val date = instant.toLocalDateTime(TimeZone.UTC).date
        val label = formatExportDate(date.dayOfMonth, date.monthNumber, date.year)
        val existing = grouped[label]
        if (existing == null) {
            grouped[label] = ChartEntry(
                label = label,
                epochMs = instant.toEpochMilliseconds(),
                count = 1,
            )
        } else {
            existing.count += 1
        }
    }

    return grouped.values
        .sortedBy { it.epochMs }
        .map { entry ->
            ProjectStatsChartPoint(
                label = entry.label,
                value = entry.count.toDouble(),
                note = pointHint(entry.count),
            )
        }
}

internal fun buildCommitExportTableRows(
    commits: List<com.spbu.projecttrack.rating.data.model.StatsDetailCommitUi>,
): List<List<String>> {
    return commits.map { commit ->
        listOf(
            commit.message,
            commit.committedAtLabel,
            commit.authorName,
            "+${commit.additions}",
            "-${commit.deletions}",
            commit.files.size.toString(),
            commit.sha?.take(7).orEmpty(),
            commit.url?.takeIf { it.isNotBlank() }?.let {
                localizeRuntime("Ссылка", "Link")
            }.orEmpty(),
        )
    }
}

internal fun buildPullRequestExportTableRows(
    pullRequests: List<com.spbu.projecttrack.rating.data.model.StatsDetailPullRequestUi>,
): List<List<String>> {
    return pullRequests.map { pullRequest ->
        listOf(
            buildString {
                pullRequest.number?.let { append("#$it ") }
                append(pullRequest.title)
            },
            pullRequest.state?.uppercase().orEmpty(),
            pullRequest.authorName,
            pullRequest.assigneeNames.joinToString(),
            pullRequest.createdAtLabel,
            pullRequest.closedAtLabel.orEmpty(),
            pullRequest.comments?.toString().orEmpty(),
            pullRequest.commitsCount?.toString().orEmpty(),
            pullRequest.changedFiles?.toString().orEmpty(),
            if (pullRequest.additions != null || pullRequest.deletions != null) {
                "+${pullRequest.additions ?: 0}/-${pullRequest.deletions ?: 0}"
            } else {
                ""
            },
            pullRequest.url?.takeIf { it.isNotBlank() }?.let {
                localizeRuntime("Ссылка", "Link")
            }.orEmpty(),
        )
    }
}

private fun parseExportInstant(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return runCatching { Instant.parse(value) }.getOrNull()
}

private fun formatExportDate(
    dayOfMonth: Int,
    monthNumber: Int,
    year: Int,
): String {
    val day = dayOfMonth.toString().padStart(2, '0')
    val month = monthNumber.toString().padStart(2, '0')
    val shortYear = (year % 100).toString().padStart(2, '0')
    return "$day.$month.$shortYear"
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
        score = score,
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
        score = score,
        rows = buildList {
            add(ProjectStatsTableRow(StatsExportCopy.openIssuesRow(), openIssues.toString()))
            add(ProjectStatsTableRow(StatsExportCopy.closedIssuesRow(), closedIssues.toString()))
            add(ProjectStatsTableRow(StatsExportCopy.progressRow(), "${(progress * 100).toInt()}%"))
            add(ProjectStatsTableRow(StatsExportCopy.ratingRow(), rank?.toString() ?: "—"))
            tableRows.forEach { row ->
                add(ProjectStatsTableRow(row.name, row.value))
            }
        }
    )
}

internal fun ProjectStatsCodeChurnSectionUi.toExportSection(): ProjectStatsSection {
    val parsedRows = fileRows.mapNotNull { row ->
        row.value.toIntOrNull()?.let { changes -> row.fileName to changes }
    }
    val visibleRows = parsedRows.filter { (_, changes) -> changes >= 100 }
    val hiddenRows = parsedRows.filter { (_, changes) -> changes < 100 }
    return ProjectStatsSection(
        title = title,
        score = score,
        rows = listOf(
            ProjectStatsTableRow(StatsExportCopy.changedFilesRow(), changedFilesCount.toString()),
            ProjectStatsTableRow(StatsExportCopy.ratingRow(), rank?.toString() ?: "—"),
        ),
        table = ProjectStatsTable(
            title = StatsExportCopy.changedFilesHeader(),
            headers = listOf(
                localizeRuntime("Файл", "File"),
                localizeRuntime("Изменения", "Changes"),
            ),
            rows = visibleRows.map { (fileName, changes) ->
                listOf(fileName, changes.toString())
            }.ifEmpty {
                listOf(listOf(StatsExportCopy.noData(), "—"))
            },
            columnFractions = listOf(0.82f, 0.18f),
        ),
        chart = if (slices.isNotEmpty()) ProjectStatsChart.Donut(
            title = StatsExportCopy.fileDistributionTitle(),
            segments = slices.map { slice ->
                ProjectStatsChartSegment(
                    label = slice.label,
                    value = slice.value.toDouble(),
                    colorHint = slice.percentLabel
                )
            }
        ) else null,
        chartFirst = true,
        notes = buildList {
            mostChangedFileName?.takeIf { it.isNotBlank() }?.let {
                add(StatsExportCopy.mostChangedFile(it))
            }
            if (hiddenRows.isNotEmpty()) {
                val hiddenCount = hiddenRows.size
                val hiddenChanges = hiddenRows.sumOf { it.second }
                add(
                    localizeRuntime(
                        "Скрыто файлов с изменениями меньше 100: $hiddenCount, суммарно изменений: $hiddenChanges",
                        "Hidden files with fewer than 100 changes: $hiddenCount, total changes: $hiddenChanges",
                    )
                )
            }
        }
    )
}

internal fun ProjectStatsOwnershipSectionUi.toExportSection(): ProjectStatsSection {
    return ProjectStatsSection(
        title = title,
        score = score,
        rows = listOf(ProjectStatsTableRow(StatsExportCopy.ratingRow(), rank?.toString() ?: "—")),
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
    val dominantLabel = slices.maxByOrNull { it.value }?.label?.takeIf { hasActivity }
        ?: StatsExportCopy.noDataUpper()
    val leastActiveLabel = slices.minByOrNull { it.value }?.label?.takeIf { hasActivity }
        ?: StatsExportCopy.noDataUpper()

    return ProjectStatsSection(
        title = title,
        score = score,
        rows = listOf(
            ProjectStatsTableRow(StatsExportCopy.mostActiveWeekday(), dominantLabel),
            ProjectStatsTableRow(StatsExportCopy.leastActiveWeekday(), leastActiveLabel),
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
