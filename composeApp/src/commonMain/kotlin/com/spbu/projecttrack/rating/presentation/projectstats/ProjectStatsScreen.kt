package com.spbu.projecttrack.rating.presentation.projectstats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
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
import com.spbu.projecttrack.rating.data.model.ProjectStatsUiModel
import com.spbu.projecttrack.rating.data.model.ProjectStatsWeekDaySectionUi
import com.spbu.projecttrack.rating.export.ProjectStatsChart
import com.spbu.projecttrack.rating.export.ProjectStatsChartPoint
import com.spbu.projecttrack.rating.export.ProjectStatsChartSegment
import com.spbu.projecttrack.rating.export.ProjectStatsExportPayload
import com.spbu.projecttrack.rating.export.ProjectStatsMemberRow
import com.spbu.projecttrack.rating.export.ProjectStatsSection
import com.spbu.projecttrack.rating.export.ProjectStatsSummaryCard
import com.spbu.projecttrack.rating.export.ProjectStatsTableRow
import com.spbu.projecttrack.rating.export.rememberProjectStatsExporter
import com.spbu.projecttrack.rating.presentation.details.StatsDetailScreen
import com.spbu.projecttrack.rating.presentation.settings.StatsScreenSection
import com.spbu.projecttrack.rating.presentation.settings.StatsScreenSettingsScreen
import com.spbu.projecttrack.rating.presentation.settings.StatsScreenSettingsTarget
import com.spbu.projecttrack.rating.presentation.settings.defaultStatsScreenSectionIds
import com.spbu.projecttrack.rating.presentation.settings.statsScreenSectionsFromIds
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

private val CardShape = RoundedCornerShape(10.dp)
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
private val CompactStatsCardHeight = 70.dp
private val CompactStatsCardPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)

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

            val detailSection = activeDetailSection
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
                    },
                )
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
    modifier: Modifier = Modifier
) {
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
                    .clickable(onClick = onBackClick)
            )
            Text(
                text = title,
                fontFamily = AppFonts.OpenSansBold,
                fontSize = titleFontSize,
                lineHeight = titleLineHeight,
                letterSpacing = if (titleFontSize >= 40.sp) 0.4.sp else 0.16.sp,
                color = AppColors.Color3,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
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
            CompactStatsCard(
                modifier = Modifier.weight(1f)
            ) {
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
                                .fillMaxWidth(section.progress.coerceIn(0f, 1f))
                                .height(30.dp)
                                .background(AppColors.Color3, RoundedCornerShape(6.dp))
                        )
                    }
                    Text(
                        text = section.remainingText,
                        fontFamily = AppFonts.OpenSansMedium,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        color = AppColors.Color2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

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

        FileStatsCard(rows = section.fileRows)

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
private fun DominantWeekDaySection(
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

        ScoreCard(
            score = section.score,
            title = "оценка доминирующего дня недели"
        )
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
    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = ActionButtonShape,
        color = Color.Transparent,
        shadowElevation = 8.dp
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
    CompactStatsCard(modifier = modifier) {
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
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value,
                fontFamily = AppFonts.OpenSansBold,
                fontSize = valueFontSize,
                lineHeight = valueLineHeight,
                color = AppColors.Color3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = caption,
                fontFamily = AppFonts.OpenSansMedium,
                fontSize = 14.sp,
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
    CompactStatsCard(modifier = modifier) {
        val valueFontSize = 32.sp
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = scoreText,
                fontFamily = AppFonts.OpenSansBold,
                fontSize = valueFontSize,
                lineHeight = valueFontSize,
                color = color
            )
            Text(
                text = title,
                fontFamily = AppFonts.OpenSansMedium,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                color = AppColors.Color2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun projectScoreColor(score: Double?): Color {
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

private fun formatScoreValue(score: Double): String {
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
                            Box(
                                modifier = Modifier
                                    .width(slotWidth)
                                    .fillMaxHeight()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable {
                                            selectedIndex = if (selectedIndex == index) null else index
                                        },
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    val barHeight = when {
                                        animatedFraction <= 0f -> 0.dp
                                        else -> plotHeight * animatedFraction
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(barWidth)
                                            .height(barHeight)
                                            .background(Color(0xFFBDBDBD), BarShape)
                                    )
                                }
                            }
                        }
                    }
                }

                selectedIndex?.let { index ->
                    val centerX = plotStart + slotWidth * index + (slotWidth / 2)
                    TooltipBubble(
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
                                y = 18.dp,
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
        val slotWidth = ((width - plotStart - plotEndPadding) / displayPoints.size.coerceAtLeast(1))
            .coerceAtLeast(18.dp)
        val tooltipWidth = with(density) { tooltipSize.width.toDp() }
        val tooltipHalfWidth = tooltipWidth / 2
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

                    pointPositions.forEachIndexed { index, point ->
                        drawCircle(
                            color = if (selectedIndex == index) AppColors.Color3 else AppColors.Color2,
                            radius = if (selectedIndex == index) 5.dp.toPx() else 4.dp.toPx(),
                            center = Offset(point.x.dp.toPx(), point.y.dp.toPx())
                        )
                    }
                }

                selectedIndex?.let { index ->
                    val tooltipX = (pointPositions[index].x.dp - tooltipHalfWidth)
                        .coerceIn(plotStart, width - tooltipWidth)
                    TooltipBubble(
                        text = formatChartTooltip(
                            point = displayPoints[index],
                            tooltipTitle = tooltipTitle,
                        ),
                        onClose = { selectedIndex = null },
                        modifier = Modifier
                            .onSizeChanged { tooltipSize = it }
                            .offset(
                                x = tooltipX,
                                y = (pointPositions[index].y - 44f).dp
                            )
                    )
                }

                pointPositions.forEachIndexed { index, point ->
                    Box(
                        modifier = Modifier
                            .offset(x = (point.x - 16f).dp, y = (point.y - 16f).dp)
                            .size(32.dp)
                            .clickable {
                                selectedIndex = if (selectedIndex == index) null else index
                            }
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
): String {
    if (tooltipTitle.contains("коммит", ignoreCase = true)) {
        val count = point.value.roundToInt()
        return "$count ${pluralizeRussian(count, "коммит", "коммита", "коммитов")}"
    }
    if (tooltipTitle.contains("Pull Request", ignoreCase = true) &&
        !tooltipTitle.contains("быст", ignoreCase = true)
    ) {
        val count = point.value.roundToInt()
        return "$count ${if (count == 1) "открытый PR" else "открытых PR"}"
    }
    return point.hint.ifBlank { tooltipTitle }
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
        Box(
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
        ) {
            Text(
                text = text,
                fontFamily = AppFonts.OpenSansRegular,
                fontSize = 10.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
                color = AppColors.Color2,
                modifier = Modifier.padding(end = 24.dp)
            )
            Image(
                painter = painterResource(Res.drawable.stats_tooltip_close),
                contentDescription = "Закрыть",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(12.dp)
                    .clickable(onClick = onClose)
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
                                    .height(40.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = row.fileName,
                                    fontFamily = AppFonts.OpenSansMedium,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    letterSpacing = 0.13.sp,
                                    color = AppColors.Color2,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp, end = 10.dp),
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 10.dp)
            .height(208.dp),
        contentAlignment = Alignment.Center
    ) {
        val chartSize = 188.dp
        val outerRadius = chartSize / 2
        val innerRadius = outerRadius / 2
        val strokeWidth = outerRadius - innerRadius

        Canvas(modifier = Modifier.size(chartSize)) {
            var startAngle = -90f
            slices.forEach { slice ->
                val sweep = ((slice.value / total.toFloat()) * 360f) * progress
                drawArc(
                    color = Color(slice.colorHex),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }

        var start = -90f
        slices.forEach { slice ->
            val sweep = (slice.value / total.toFloat()) * 360f
            val middle = start + sweep / 2f
            val angle = middle * (PI / 180.0)
            val radius = innerRadius.value + ((outerRadius - innerRadius).value * 0.72f)
            val x = kotlin.math.cos(angle).toFloat() * radius
            val y = kotlin.math.sin(angle).toFloat() * radius
            val arcLength = 2 * PI.toFloat() * radius * (sweep / 360f)
            if (
                slices.size > 1 &&
                slice.value > 0f &&
                sweep >= 32f &&
                arcLength >= slice.percentLabel.length * 9f
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = x.dp, y = y.dp)
                        .defaultMinSize(minWidth = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = slice.percentLabel,
                        fontFamily = AppFonts.OpenSansBold,
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                        letterSpacing = 0.11.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            start += sweep
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
        modifier = modifier.fillMaxWidth(),
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        icon()
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
private fun StatsCard(
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
private fun CompactStatsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    StatsCard(
        modifier = modifier.height(CompactStatsCardHeight),
        contentPadding = CompactStatsCardPadding,
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
    return ProjectStatsExportPayload(
        projectId = projectId,
        projectName = title,
        customerName = customer,
        repositoryUrl = selectedRepository?.title,
        periodLabel = "${visibleRange.startLabel} - ${visibleRange.endLabel}",
        generatedAtLabel = "Сейчас",
        summaryCards = listOf(
            ProjectStatsSummaryCard("Коммиты", commits.primaryValue, commits.primaryCaption),
            ProjectStatsSummaryCard("Issue", issues.openIssues.toString(), "открытых"),
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
        sections = listOf(
            commits.toExportSection(),
            issues.toExportSection(),
            pullRequests.toExportSection(),
            rapidPullRequests.toExportSection(),
            codeChurn.toExportSection(),
            codeOwnership.toExportSection(),
            dominantWeekDay.toExportSection()
        )
    )
}

private fun ProjectStatsMetricSectionUi.toExportSection(): ProjectStatsSection {
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

private fun ProjectStatsIssueSectionUi.toExportSection(): ProjectStatsSection {
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

private fun ProjectStatsCodeChurnSectionUi.toExportSection(): ProjectStatsSection {
    return ProjectStatsSection(
        title = title,
        subtitle = "Score: ${score?.let(::formatScoreValue) ?: "—"}",
        rows = buildList {
            add(ProjectStatsTableRow("Изменено файлов", changedFilesCount.toString()))
            add(ProjectStatsTableRow("Рейтинг", rank?.toString() ?: "—"))
            fileRows.forEach { row ->
                add(ProjectStatsTableRow(row.fileName, row.value))
            }
            tableRows.forEach { row ->
                add(ProjectStatsTableRow(row.name, row.value))
            }
        }
    )
}

private fun ProjectStatsOwnershipSectionUi.toExportSection(): ProjectStatsSection {
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

private fun ProjectStatsWeekDaySectionUi.toExportSection(): ProjectStatsSection {
    return ProjectStatsSection(
        title = title,
        subtitle = "Score: ${score?.let(::formatScoreValue) ?: "—"}",
        rows = listOf(ProjectStatsTableRow(headline, subtitle)),
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
