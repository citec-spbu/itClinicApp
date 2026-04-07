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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.launch
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import projecttrack.composeapp.generated.resources.Res
import projecttrack.composeapp.generated.resources.calendar_icon
import projecttrack.composeapp.generated.resources.spbu_logo
import kotlin.math.PI
import kotlin.math.roundToInt

private val CardShape = RoundedCornerShape(18.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectStatsScreen(
    viewModel: ProjectStatsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val exporter = rememberProjectStatsExporter()

    LaunchedEffect(viewModel) {
        viewModel.load()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ) {
            Image(
                painter = painterResource(Res.drawable.spbu_logo),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .offset(y = (-60).dp),
                contentScale = ContentScale.FillWidth,
                alpha = 0.08f
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
                        onBackClick = onBackClick,
                        onRepositorySelected = viewModel::selectRepository,
                        onStartDateSelected = viewModel::selectStartDate,
                        onEndDateSelected = viewModel::selectEndDate,
                        onRapidThresholdChanged = viewModel::updateRapidThreshold,
                        onPlaceholderClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Раздел в разработке")
                            }
                        },
                        onSettingsClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Настройки экрана скоро появятся")
                            }
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
        }
    }
}

@Composable
private fun ErrorState(
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
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = AppColors.Color3
            )
            Text(
                text = message,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Normal,
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
    onBackClick: () -> Unit,
    onRepositorySelected: (String) -> Unit,
    onStartDateSelected: (String) -> Unit,
    onEndDateSelected: (String) -> Unit,
    onRapidThresholdChanged: (Int, Int, Int) -> Unit,
    onPlaceholderClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onExportPdfClick: () -> Unit,
    onExportExcelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var datePickerTarget by remember { mutableStateOf<DatePickerTarget?>(null) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = if (model.showOverallRatingButton) 156.dp else 72.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                StatsHeader(
                    title = model.title,
                    onBackClick = onBackClick
                )
            }
            item {
                AnimatedSection {
                    StatsValueCard(
                        title = "Заказчик",
                        content = {
                            Text(
                                text = model.customer,
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = FontWeight.Normal,
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
                        onPlaceholderClick = onPlaceholderClick
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
                            onStartDateClick = { datePickerTarget = DatePickerTarget.Start },
                            onEndDateClick = { datePickerTarget = DatePickerTarget.End }
                        )
                    }
                }
            }
            if (model.repositories.isNotEmpty()) {
                item {
                    AnimatedSection {
                        MetricSection(
                            section = model.commits,
                            onDetailsClick = onPlaceholderClick
                        )
                    }
                }
                item {
                    AnimatedSection {
                        IssueSection(
                            section = model.issues,
                            onDetailsClick = onPlaceholderClick
                        )
                    }
                }
                item {
                    AnimatedSection {
                        MetricSection(
                            section = model.pullRequests,
                            onDetailsClick = onPlaceholderClick
                        )
                    }
                }
                item {
                    AnimatedSection {
                        MetricSection(
                            section = model.rapidPullRequests,
                            rapidThreshold = model.rapidThreshold,
                            onRapidThresholdChanged = onRapidThresholdChanged,
                            onDetailsClick = onPlaceholderClick
                        )
                    }
                }
                item {
                    AnimatedSection {
                        CodeChurnSection(
                            section = model.codeChurn,
                            onDetailsClick = onPlaceholderClick
                        )
                    }
                }
                item {
                    AnimatedSection {
                        OwnershipSection(
                            section = model.codeOwnership,
                            onDetailsClick = onPlaceholderClick
                        )
                    }
                }
                item {
                    AnimatedSection {
                        DominantWeekDaySection(
                            section = model.dominantWeekDay,
                            onDetailsClick = onPlaceholderClick
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

        datePickerTarget?.let { target ->
            SingleDatePickerDialog(
                title = if (target == DatePickerTarget.Start) {
                    "Начальная дата"
                } else {
                    "Конечная дата"
                },
                initialIsoDate = if (target == DatePickerTarget.Start) {
                    model.visibleRange.startIsoDate
                } else {
                    model.visibleRange.endIsoDate
                },
                onDismiss = { datePickerTarget = null },
                onConfirm = { isoDate ->
                    datePickerTarget = null
                    if (target == DatePickerTarget.Start) {
                        onStartDateSelected(isoDate)
                    } else {
                        onEndDateSelected(isoDate)
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = model.showOverallRatingButton,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 28.dp, vertical = 20.dp),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = AppColors.Color3,
                shadowElevation = 8.dp
            ) {
                Text(
                    text = "Общий рейтинг",
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun AnimatedSection(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 6 }),
        exit = fadeOut()
    ) {
        content()
    }
}

@Composable
private fun StatsHeader(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Назад",
                tint = AppColors.Color2,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(24.dp)
                    .clickable(onClick = onBackClick)
            )
            Text(
                text = "Статистика",
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = AppColors.Color3,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Text(
            text = title,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
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
            Text(
                text = title,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = AppColors.Color2
            )
            content()
        }
    }
}

@Composable
private fun TeamMembersCard(
    members: List<ProjectStatsMemberUi>,
    onPlaceholderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    StatsCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Участники команды",
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = AppColors.Color2
            )

            members.forEachIndexed { index, member ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = buildString {
                                    append(member.name)
                                    if (member.isCurrentUser) append(" (Вы)")
                                },
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = AppColors.Color2
                            )
                            Text(
                                text = member.role,
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                color = AppColors.Color1
                            )
                        }

                        Text(
                            text = "Статистика",
                            fontFamily = AppFonts.OpenSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Color.Black,
                            modifier = Modifier.clickable(onClick = onPlaceholderClick)
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
private fun EmptyDetailedInfoCard(
    modifier: Modifier = Modifier
) {
    StatsCard(modifier = modifier) {
        Text(
            text = "Нет подробной информации по репозиториям",
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = AppColors.Color2
        )
    }
}

@Composable
private fun RepositorySelectorCard(
    repositories: List<com.spbu.projecttrack.rating.data.model.ProjectStatsRepositoryUi>,
    selectedId: String,
    visibleRange: com.spbu.projecttrack.rating.data.model.ProjectStatsDateRangeUi,
    onRepositorySelected: (String) -> Unit,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit,
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
            onSelected = onRepositorySelected
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Выбор периода",
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.Black
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DateBadge(
                    text = visibleRange.startLabel,
                    onClick = onStartDateClick,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "—",
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                DateBadge(
                    text = visibleRange.endLabel,
                    onClick = onEndDateClick,
                    modifier = Modifier.weight(1f)
                )
            }
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

        DoubleMetricRow(
            leftValue = section.primaryValue,
            leftCaption = section.primaryCaption,
            rightValue = section.rank?.toString() ?: "—",
            rightCaption = section.rankCaption
        )

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
            title = "оценка ${section.title.lowercase()}"
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
            StatsCard(
                modifier = Modifier.weight(1f)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        fontFamily = AppFonts.OpenSans,
                        fontWeight = FontWeight.Medium,
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
                                    fontFamily = AppFonts.OpenSans,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = AppColors.Color2
                                )
                                Text(
                                    text = slice.secondaryLabel,
                                    fontFamily = AppFonts.OpenSans,
                                    fontWeight = FontWeight.Bold,
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
                            Text(
                                text = buildAnnotatedString {
                                    append(slice.label)
                                    append(" ")
                                    withStyle(
                                        style = androidx.compose.ui.text.SpanStyle(
                                            color = AppColors.Color3,
                                            fontWeight = FontWeight.Bold
                                        )
                                    ) {
                                        append(slice.secondaryLabel)
                                    }
                                },
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = if (slice.highlight) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                color = AppColors.Color2
                            )
                        }
                    }
                }
            }
        }

        StatsCard {
            Column {
                Text(
                    text = section.headline,
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = AppColors.Color3
                )
                Text(
                    text = section.subtitle,
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = AppColors.Color2
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
private fun SectionHeader(
    title: String,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Color.Black
        )
        ActionPillButton(
            text = "Подробнее",
            onClick = onDetailsClick
        )
    }
}

@Composable
private fun ActionPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = AppColors.Color3,
        shadowElevation = 4.dp
    ) {
        Text(
            text = text,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun DoubleMetricRow(
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
private fun SingleMetricCard(
    value: String,
    caption: String,
    modifier: Modifier = Modifier
) {
    StatsCard(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = AppColors.Color3
            )
            Text(
                text = caption,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                color = AppColors.Color2
            )
        }
    }
}

@Composable
private fun ScoreCard(
    score: Double?,
    title: String,
    modifier: Modifier = Modifier
) {
    val scoreText = score?.let(::formatScoreValue) ?: "—"
    val color = projectScoreColor(score)
    StatsCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = scoreText,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = color
            )
            Text(
                text = title,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = AppColors.Color2
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

@Composable
private fun ChartCard(
    title: String,
    chartType: ProjectStatsChartType,
    points: List<ProjectStatsChartPointUi>,
    tooltipTitle: String,
    modifier: Modifier = Modifier
) {
    StatsCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = title,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = AppColors.Color2
            )

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
    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }
    val maxValue = (points.maxOfOrNull { it.value } ?: 1f).coerceAtLeast(1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
    ) {
        val chartHeight = 144.dp
        val maxBarHeight = 92.dp
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
            ) {
                GridBackground(
                    maxValue = maxValue,
                    highlightBaseline = true
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(start = 28.dp, end = 8.dp, bottom = 1.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    points.forEachIndexed { index, point ->
                        val targetFraction = (point.value / maxValue).coerceIn(0f, 1f)
                        val animatedFraction by animateFloatAsState(
                            targetValue = targetFraction,
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
                            label = "bar_height_$index"
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            val isSelected = selectedIndex == index
                            if (isSelected) {
                                TooltipBubble(
                                    text = point.hint,
                                    onClose = { selectedIndex = null }
                                )
                            }
                            Spacer(modifier = Modifier.height(if (isSelected) 10.dp else 34.dp))

                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(maxBarHeight + 14.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(maxBarHeight + 10.dp)
                                            .background(Color(0xFFD8D8DB))
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .width(34.dp)
                                        .height((maxBarHeight * animatedFraction).coerceAtLeast(4.dp))
                                        .background(Color(0xFFC6C6C8), RoundedCornerShape(6.dp))
                                        .clickable {
                                            selectedIndex = if (isSelected) null else index
                                        }
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 8.dp, top = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                points.forEach { point ->
                    Text(
                        text = point.label,
                        fontFamily = AppFonts.OpenSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = AppColors.Color2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
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
    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }
    val maxValue = (points.maxOfOrNull { it.value } ?: 1f).coerceAtLeast(1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
    ) {
        val width = maxWidth
        val height = 144.dp
        val pointPositions = remember(points, width, height, maxValue) {
            val startX = 30.dp
            val endX = width - 12.dp
            val usableWidth = (endX - startX).value.coerceAtLeast(0f)
            val step = if (points.size <= 1) 0f else usableWidth / (points.size - 1).toFloat()
            points.mapIndexed { index, point ->
                Offset(
                    x = startX.value + step * index,
                    y = height.value - 8f - (point.value / maxValue) * (height.value - 20f)
                )
            }
        }

        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
            ) {
                GridBackground(
                    maxValue = maxValue,
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

                    selectedIndex?.let { index ->
                        val point = pointPositions[index]
                        drawLine(
                            color = Color(0xFFD8D8DB),
                            start = Offset(point.x.dp.toPx(), 8.dp.toPx()),
                            end = Offset(point.x.dp.toPx(), (height - 1.dp).toPx()),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    pointPositions.forEachIndexed { index, point ->
                        drawCircle(
                            color = if (selectedIndex == index) AppColors.Color3 else AppColors.Color2,
                            radius = if (selectedIndex == index) 5.dp.toPx() else 4.dp.toPx(),
                            center = Offset(point.x.dp.toPx(), point.y.dp.toPx())
                        )
                    }
                }

                selectedIndex?.let { index ->
                    val tooltipX = (pointPositions[index].x - 52f).dp
                        .coerceIn(20.dp, width - 112.dp)
                    TooltipBubble(
                        text = points[index].hint,
                        onClose = { selectedIndex = null },
                        modifier = Modifier
                            .offset(
                                x = tooltipX,
                                y = (pointPositions[index].y - 42f).dp
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
                    .padding(start = 24.dp, end = 8.dp, top = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                points.forEach { point ->
                    Text(
                        text = point.label,
                        fontFamily = AppFonts.OpenSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = AppColors.Color2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GridBackground(
    maxValue: Float,
    highlightBaseline: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val labels = listOf(maxValue, maxValue * 0.66f, maxValue * 0.33f, 0f)
            labels.forEach { label ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label.roundToInt().toString(),
                        fontFamily = AppFonts.OpenSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = AppColors.Color2,
                        modifier = Modifier.width(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (highlightBaseline && label == 0f) 2.dp else 1.dp)
                            .background(
                                if (highlightBaseline && label == 0f) {
                                    AppColors.Color2.copy(alpha = 0.4f)
                                } else {
                                    Color(0xFFE3E3E6)
                                }
                            )
                    )
                }
            }
        }
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
        shape = RoundedCornerShape(6.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp,
                color = AppColors.Color2
            )
            Text(
                text = "×",
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = AppColors.Color2,
                modifier = Modifier.clickable(onClick = onClose)
            )
        }
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
            Text(
                text = title,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = AppColors.Color2
            )
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
                            color = AppColors.Color2,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = row.value,
                            fontFamily = AppFonts.OpenSans,
                            fontWeight = FontWeight.SemiBold,
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
private fun FileStatsCard(
    rows: List<com.spbu.projecttrack.rating.data.model.ProjectStatsFileRowUi>,
    modifier: Modifier = Modifier
) {
    StatsCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Статистика по файлам",
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = AppColors.Color2
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCECED2))
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Файл",
                            fontFamily = AppFonts.OpenSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = AppColors.Color2,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(22.dp)
                                .background(Color(0xFFCECED2))
                        )
                        Text(
                            text = "Кол-во",
                            fontFamily = AppFonts.OpenSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = AppColors.Color2,
                            modifier = Modifier
                                .widthIn(min = 72.dp)
                                .padding(horizontal = 16.dp),
                            textAlign = TextAlign.End
                        )
                    }

                    rows.forEachIndexed { index, row ->
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = row.fileName,
                                    fontFamily = AppFonts.OpenSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = AppColors.Color2,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 16.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(22.dp)
                                        .background(Color(0xFFCECED2))
                                )
                                Text(
                                    text = row.value,
                                    fontFamily = AppFonts.OpenSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = AppColors.Color2,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier
                                        .widthIn(min = 72.dp)
                                        .padding(horizontal = 16.dp)
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
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = AppColors.Color2
                            )
                            Text(
                                text = slice.secondaryLabel,
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = FontWeight.Bold,
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
private fun DonutChart(
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
            .height(230.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(220.dp)) {
            var startAngle = -90f
            val strokeWidth = 28.dp.toPx()
            slices.forEach { slice ->
                val sweep = ((slice.value / total.toFloat()) * 360f) * progress
                drawArc(
                    color = Color(slice.colorHex),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }

        var start = -90f
        slices.forEach { slice ->
            val sweep = (slice.value / total.toFloat()) * 360f
            val middle = start + sweep / 2f
            val angle = middle * (PI / 180.0)
            val radius = if (sweep < 24f) 58f else 66f
            val x = kotlin.math.cos(angle).toFloat() * radius
            val y = kotlin.math.sin(angle).toFloat() * radius
            if (slice.value > 0f) {
                Text(
                    text = slice.percentLabel,
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.offset(x = x.dp, y = y.dp)
                )
            }
            start += sweep
        }
    }
}

@Composable
private fun FooterActions(
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
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = AppColors.Color2
                )
            },
            onClick = onSettingsClick
        )
        FooterActionRow(
            text = "Экспорт в PDF",
            icon = {
                Icon(
                    imageVector = Icons.Outlined.PictureAsPdf,
                    contentDescription = null,
                    tint = AppColors.Color2
                )
            },
            onClick = onExportPdfClick
        )
        FooterActionRow(
            text = "Экспорт в Excel",
            icon = {
                Icon(
                    imageVector = Icons.Outlined.TableChart,
                    contentDescription = null,
                    tint = AppColors.Color2
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
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            color = AppColors.Color2
        )
    }
}

private enum class DatePickerTarget {
    Start,
    End,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleDatePickerDialog(
    title: String,
    initialIsoDate: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val datePickerState = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = initialIsoDate.toDatePickerMillis()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Text(
                text = "Готово",
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = AppColors.Color3,
                modifier = Modifier.clickable {
                    datePickerState.selectedDateMillis
                        ?.toIsoDate()
                        ?.let(onConfirm)
                }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        },
        dismissButton = {
            Text(
                text = "Отмена",
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = AppColors.Color2,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = title,
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = AppColors.Color2,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        )
    }
}

private fun String.toDatePickerMillis(): Long? {
    val parts = split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    return kotlinx.datetime.LocalDate(year, month, day)
        .atStartOfDayIn(kotlinx.datetime.TimeZone.UTC)
        .toEpochMilliseconds()
}

private fun Long.toIsoDate(): String {
    val date = kotlinx.datetime.Instant
        .fromEpochMilliseconds(this)
        .toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
        .date
    val month = date.monthNumber.toString().padStart(2, '0')
    val day = date.dayOfMonth.toString().padStart(2, '0')
    return "${date.year}-$month-$day"
}

@Composable
private fun DateBadge(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFF2F2F4)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = AppColors.Color2,
                modifier = Modifier.weight(1f, fill = false)
            )
            Image(
                painter = painterResource(Res.drawable.calendar_icon),
                contentDescription = "Календарь",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun DropdownSelector(
    title: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    width: Dp? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.Black
            )
        }

        Box {
            Surface(
                modifier = Modifier
                    .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
                    .clickable { expanded = true },
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF2F2F4)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = value,
                        fontFamily = AppFonts.OpenSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = AppColors.Color2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = AppColors.Color2
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (key, titleValue) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = titleValue,
                                fontFamily = AppFonts.OpenSans
                            )
                        },
                        onClick = {
                            expanded = false
                            onSelected(key)
                        }
                    )
                }
            }
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
        Text(
            text = "Период:",
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color.Black
        )

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
        width = null
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
            fontFamily = AppFonts.OpenSans,
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
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            content = content
        )
    }
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
