package com.spbu.projecttrack.rating.presentation.details

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.SpanStyle
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
import com.spbu.projecttrack.rating.data.model.ProjectStatsDateRangeUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsDonutSliceUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsRepositoryUi
import com.spbu.projecttrack.rating.data.model.ProjectStatsThresholdUi
import com.spbu.projecttrack.rating.data.model.StatsDetailCommitUi
import com.spbu.projecttrack.rating.data.model.StatsDetailDataUi
import com.spbu.projecttrack.rating.data.model.StatsDetailIssueUi
import com.spbu.projecttrack.rating.data.model.StatsDetailParticipantUi
import com.spbu.projecttrack.rating.data.model.StatsDetailPullRequestUi
import com.spbu.projecttrack.rating.presentation.projectstats.ChartCard
import com.spbu.projecttrack.rating.presentation.projectstats.DonutChart
import com.spbu.projecttrack.rating.presentation.projectstats.DoubleMetricRow
import com.spbu.projecttrack.rating.presentation.projectstats.DropdownSelector
import com.spbu.projecttrack.rating.presentation.projectstats.EmptyDetailedInfoCard
import com.spbu.projecttrack.rating.presentation.projectstats.RepositorySelectorCard
import com.spbu.projecttrack.rating.presentation.projectstats.ScoreCard
import com.spbu.projecttrack.rating.presentation.projectstats.SingleMetricCard
import com.spbu.projecttrack.rating.presentation.projectstats.StatsBackgroundLogo
import com.spbu.projecttrack.rating.presentation.projectstats.StatsDateRangePickerDialog
import com.spbu.projecttrack.rating.presentation.projectstats.StatsTopBar
import com.spbu.projecttrack.rating.presentation.projectstats.StatsTopBarTotalHeight
import com.spbu.projecttrack.rating.presentation.settings.StatsScreenSection
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import projecttrack.composeapp.generated.resources.Res
import projecttrack.composeapp.generated.resources.stats_footer_excel
import projecttrack.composeapp.generated.resources.stats_footer_pdf
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private val DetailCardShape = RoundedCornerShape(10.dp)
private val DetailControlShape = RoundedCornerShape(5.dp)
private val DetailAccentGradient = Brush.verticalGradient(
    colors = listOf(AppColors.GradientStart, AppColors.GradientEndAlt)
)
private const val AllParticipantsId = "__all__"

@Composable
fun StatsDetailScreen(
    section: StatsScreenSection,
    repositories: List<ProjectStatsRepositoryUi>,
    selectedRepositoryId: String,
    visibleRange: ProjectStatsDateRangeUi,
    rapidThreshold: ProjectStatsThresholdUi,
    details: StatsDetailDataUi,
    allowParticipantFilter: Boolean,
    initialParticipantId: String?,
    overallRank: Int?,
    overallScore: Double?,
    onBackClick: () -> Unit,
    onRepositorySelected: (String) -> Unit,
    onDateRangeSelected: (String, String) -> Unit,
    onRapidThresholdChanged: (Int, Int, Int) -> Unit,
    onExportPdfClick: () -> Unit,
    onExportExcelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDateRangePicker by remember { mutableStateOf(false) }
    val startParticipant = remember(section, allowParticipantFilter, initialParticipantId, details.defaultParticipantId) {
        when {
            allowParticipantFilter -> initialParticipantId ?: AllParticipantsId
            else -> initialParticipantId ?: details.defaultParticipantId
        }
    }
    var selectedParticipantId by rememberSaveable(section.id, startParticipant) {
        mutableStateOf(startParticipant)
    }

    val participantSnapshots = remember(details, rapidThreshold.totalMinutes) {
        buildParticipantSnapshots(details, rapidThreshold.totalMinutes)
    }
    val effectiveParticipantId = selectedParticipantId?.takeUnless { it == AllParticipantsId }
    val selectedSnapshot = effectiveParticipantId?.let(participantSnapshots::get)
    val peerSnapshots = participantSnapshots.filterKeys { it != effectiveParticipantId }.values.toList()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        StatsBackgroundLogo()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                ),
            contentPadding = PaddingValues(
                start = 21.dp,
                top = StatsTopBarTotalHeight + 8.dp,
                end = 21.dp,
                bottom = 10.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                if (repositories.isEmpty()) {
                    EmptyDetailedInfoCard()
                } else {
                    RepositorySelectorCard(
                        repositories = repositories,
                        selectedId = selectedRepositoryId,
                        visibleRange = visibleRange,
                        onRepositorySelected = onRepositorySelected,
                        onDateRangeClick = { showDateRangePicker = true },
                    )
                }
            }

            if (allowParticipantFilter && details.participants.isNotEmpty()) {
                item {
                    DetailDropdownSelector(
                        title = "Выбор участника",
                        value = details.participants.firstOrNull { it.id == selectedParticipantId }?.name
                            ?: "Вся команда",
                        options = buildList {
                            add(AllParticipantsId to "Вся команда")
                            addAll(details.participants.map { it.id to it.name })
                        },
                        onSelected = { selectedParticipantId = it },
                        selectedKey = selectedParticipantId,
                    )
                }
            }

            when (section) {
                StatsScreenSection.Commits -> commitItems(
                    snapshots = participantSnapshots,
                    selectedSnapshot = selectedSnapshot,
                    peerSnapshots = peerSnapshots,
                    effectiveParticipantId = effectiveParticipantId,
                    overallRank = overallRank,
                    overallScore = overallScore,
                    details = details,
                )

                StatsScreenSection.Issues -> issueItems(
                    snapshots = participantSnapshots,
                    selectedSnapshot = selectedSnapshot,
                    peerSnapshots = peerSnapshots,
                    effectiveParticipantId = effectiveParticipantId,
                    overallRank = overallRank,
                    overallScore = overallScore,
                    details = details,
                )

                StatsScreenSection.PullRequests -> pullRequestItems(
                    snapshots = participantSnapshots,
                    selectedSnapshot = selectedSnapshot,
                    peerSnapshots = peerSnapshots,
                    effectiveParticipantId = effectiveParticipantId,
                    overallRank = overallRank,
                    overallScore = overallScore,
                    details = details,
                    rapidThreshold = rapidThreshold,
                )

                StatsScreenSection.RapidPullRequests -> rapidPullRequestItems(
                    snapshots = participantSnapshots,
                    selectedSnapshot = selectedSnapshot,
                    peerSnapshots = peerSnapshots,
                    effectiveParticipantId = effectiveParticipantId,
                    overallRank = overallRank,
                    overallScore = overallScore,
                    details = details,
                    rapidThreshold = rapidThreshold,
                    onRapidThresholdChanged = onRapidThresholdChanged,
                )

                StatsScreenSection.CodeChurn -> codeChurnItems(
                    snapshots = participantSnapshots,
                    selectedSnapshot = selectedSnapshot,
                    peerSnapshots = peerSnapshots,
                    effectiveParticipantId = effectiveParticipantId,
                    overallRank = overallRank,
                    overallScore = overallScore,
                    details = details,
                )

                StatsScreenSection.CodeOwnership -> ownershipItems(
                    snapshots = participantSnapshots,
                    selectedSnapshot = selectedSnapshot,
                    peerSnapshots = peerSnapshots,
                    effectiveParticipantId = effectiveParticipantId,
                    overallRank = overallRank,
                    overallScore = overallScore,
                    details = details,
                )

                StatsScreenSection.DominantWeekDay -> dominantWeekDayItems(
                    snapshots = participantSnapshots,
                    selectedSnapshot = selectedSnapshot,
                    peerSnapshots = peerSnapshots,
                    effectiveParticipantId = effectiveParticipantId,
                    overallRank = overallRank,
                    overallScore = overallScore,
                    details = details,
                )
            }

            item {
                DetailExportActions(
                    onExportPdfClick = onExportPdfClick,
                    onExportExcelClick = onExportExcelClick,
                )
            }
        }

        StatsTopBar(
            title = detailTitle(section),
            onBackClick = onBackClick,
            titleFontSize = if (section == StatsScreenSection.DominantWeekDay) 20.sp else 28.sp,
            titleLineHeight = if (section == StatsScreenSection.DominantWeekDay) 20.sp else 28.sp,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    if (showDateRangePicker) {
        StatsDateRangePickerDialog(
            initialStartIsoDate = visibleRange.startIsoDate,
            initialEndIsoDate = visibleRange.endIsoDate,
            onDismiss = { showDateRangePicker = false },
            onConfirm = { startIsoDate, endIsoDate ->
                showDateRangePicker = false
                onDateRangeSelected(startIsoDate, endIsoDate)
            }
        )
    }
}

private fun LazyListScope.commitItems(
    snapshots: Map<String, DetailParticipantSnapshot>,
    selectedSnapshot: DetailParticipantSnapshot?,
    peerSnapshots: List<DetailParticipantSnapshot>,
    effectiveParticipantId: String?,
    overallRank: Int?,
    overallScore: Double?,
    details: StatsDetailDataUi,
) {
    val filteredCommits = filterCommits(details, effectiveParticipantId)
    val current = selectedSnapshot
    val topFiles = buildFileAggregates(filteredCommits).take(10)
    val additions = filteredCommits.sumOf { it.additions }
    val deletions = filteredCommits.sumOf { it.deletions }
    val dailyCounts = buildDailyCounts(filteredCommits.mapNotNull { it.committedAtIso })
    val averagePerDay = if (dailyCounts.isEmpty()) 0.0 else filteredCommits.size.toDouble() / dailyCounts.size.toDouble()
    val maxPerDay = dailyCounts.maxOfOrNull { it.count } ?: 0
    val minPerDay = dailyCounts.minOfOrNull { it.count } ?: 0
    val linePoints = buildLineDeltaPoints(filteredCommits)
    val contributorRows = snapshots.values
        .sortedByDescending { it.commitCount }
        .map {
            DetailTableRow(
                title = appendYouSuffix(it.participant.name, it.participant.id == effectiveParticipantId),
                values = listOf(it.commitCount.toString()),
                highlight = it.participant.id == effectiveParticipantId,
            )
        }
    val score = resolveScore(
        overallScore = overallScore,
        overallScope = effectiveParticipantId == null,
        specificScore = current?.commitsScore,
    )
    val rank = resolveRank(
        overallRank = overallRank,
        overallScope = effectiveParticipantId == null,
        specificScore = current?.commitsScore,
        peerScores = peerSnapshots.map { it.commitsScore },
    )

    item {
        DoubleMetricRow(
            leftValue = filteredCommits.size.toString(),
            leftCaption = pluralize(filteredCommits.size, "коммит", "коммита", "коммитов"),
            rightValue = rank?.toString() ?: "—",
            rightCaption = if (effectiveParticipantId == null) "место в рейтинге" else "место в команде",
        )
    }
    item {
        ChartCard(
            title = "График коммитов",
            chartType = ProjectStatsChartType.Bars,
            points = buildChartPoints(filteredCommits.mapNotNull { it.committedAtIso }) {
                "$it ${pluralize(it, "коммит", "коммита", "коммитов")}"
            },
            tooltipTitle = "Коммиты",
        )
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SingleMetricCard(
                modifier = Modifier.weight(1f),
                value = formatCompactNumber(averagePerDay),
                caption = "ср. коммитов в день",
            )
            SingleMetricCard(
                modifier = Modifier.weight(1f),
                value = "+$additions",
                caption = "добавлено строк",
            )
            SingleMetricCard(
                modifier = Modifier.weight(1f),
                value = "-$deletions",
                caption = "удалено строк",
            )
        }
    }
    if (linePoints.isNotEmpty()) {
        item {
            DualLineChartCard(
                title = "Изменение строк",
                points = linePoints,
            )
        }
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SingleMetricCard(
                modifier = Modifier.weight(1f),
                value = maxPerDay.toString(),
                caption = "макс. коммитов / день",
            )
            SingleMetricCard(
                modifier = Modifier.weight(1f),
                value = minPerDay.toString(),
                caption = "мин. коммитов / день",
            )
        }
    }
    item {
        DetailFilesCard(
            title = "Топ-10 изменяемых файлов",
            rows = topFiles,
        )
    }
    if (contributorRows.isNotEmpty()) {
        item {
            DetailTableCard(
                title = "Количество коммитов",
                headers = listOf("Значение"),
                rows = contributorRows,
            )
        }
    }
    item {
        DetailEntityListCard(
            title = "Список коммитов",
            items = filteredCommits
                .sortedByDescending { parseInstant(it.committedAtIso)?.toEpochMilliseconds() ?: Long.MIN_VALUE }
                .take(20)
                .map { commit ->
                    DetailEntityCardData(
                        title = commit.message,
                        badge = commit.sha?.take(7),
                        status = null,
                        metaLines = listOf(
                            "Автор: ${commit.authorName}",
                            "Дата: ${commit.committedAtLabel}",
                            "Изменения: +${commit.additions} / -${commit.deletions}",
                        ),
                        link = commit.url,
                    )
                }
        )
    }
    item {
        ScoreCard(
            score = score,
            title = "оценка коммитов",
        )
    }
}

private fun LazyListScope.issueItems(
    snapshots: Map<String, DetailParticipantSnapshot>,
    selectedSnapshot: DetailParticipantSnapshot?,
    peerSnapshots: List<DetailParticipantSnapshot>,
    effectiveParticipantId: String?,
    overallRank: Int?,
    overallScore: Double?,
    details: StatsDetailDataUi,
) {
    val filteredIssues = filterIssues(details, effectiveParticipantId)
    val current = selectedSnapshot
    val openIssues = filteredIssues.count { it.closedAtIso == null }
    val closedIssues = filteredIssues.size - openIssues
    val averageLifetime = formatDurationLabel(buildAverageLifetimeMinutes(filteredIssues.mapNotNull {
        durationMinutes(it.createdAtIso, it.closedAtIso)
    }))
    val creatorRows = buildIssueCreatorRows(filteredIssues, effectiveParticipantId)
    val assigneeRows = buildIssueAssigneeRows(filteredIssues, effectiveParticipantId)
    val participantRows = snapshots.values
        .sortedByDescending { it.issueCount }
        .map {
            DetailTableRow(
                title = appendYouSuffix(it.participant.name, it.participant.id == effectiveParticipantId),
                values = listOf("${it.openIssueCount} / ${it.closedIssueCount}"),
                highlight = it.participant.id == effectiveParticipantId,
            )
        }
    val labelChips = buildLabelChips(filteredIssues)
    val score = resolveScore(
        overallScore = overallScore,
        overallScope = effectiveParticipantId == null,
        specificScore = current?.issueScore,
    )
    val rank = resolveRank(
        overallRank = overallRank,
        overallScope = effectiveParticipantId == null,
        specificScore = current?.issueScore,
        peerScores = peerSnapshots.map { it.issueScore },
    )

    item {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SingleMetricCard(
                modifier = Modifier.weight(1f),
                value = filteredIssues.size.toString(),
                caption = "всего Issue",
            )
            SingleMetricCard(
                modifier = Modifier.weight(1f),
                value = averageLifetime,
                caption = "ср. время жизни",
            )
        }
    }
    item {
        DoubleMetricRow(
            leftValue = openIssues.toString(),
            leftCaption = "открытых Issue",
            rightValue = closedIssues.toString(),
            rightCaption = "закрытых Issue",
        )
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DetailCard(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        filteredIssues.isEmpty() -> "Нет активных Issue"
                        openIssues > 0 -> "Закройте еще $openIssues Issue"
                        else -> "Все Issue закрыты"
                    },
                    fontFamily = AppFonts.OpenSansMedium,
                    fontSize = 13.sp,
                    color = AppColors.Color2,
                )
            }
            SingleMetricCard(
                modifier = Modifier.weight(1f),
                value = rank?.toString() ?: "—",
                caption = if (effectiveParticipantId == null) "место в рейтинге" else "место в команде",
            )
        }
    }
    if (participantRows.isNotEmpty()) {
        item {
            DetailTableCard(
                title = "Количество назначенных Issue",
                headers = listOf("Откр./Закр."),
                rows = participantRows,
            )
        }
    }
    item {
        DetailEntityListCard(
            title = "Список Issues",
            items = filteredIssues
                .sortedByDescending { parseInstant(it.createdAtIso)?.toEpochMilliseconds() ?: Long.MIN_VALUE }
                .take(20)
                .map { issue ->
                    DetailEntityCardData(
                        title = issue.title,
                        badge = issue.labels.firstOrNull(),
                        status = issue.state,
                        metaLines = buildList {
                            add("Создатель: ${issue.creatorName}")
                            if (issue.assigneeNames.isNotEmpty()) {
                                add("Исполнители: ${issue.assigneeNames.joinToString()}")
                            }
                            add("Создано: ${issue.createdAtLabel}")
                            issue.closedAtLabel?.let { add("Закрыто: $it") }
                        },
                        secondary = issue.number?.let { "#$it" },
                        trailing = issue.comments?.let { "$it комментариев" },
                        link = issue.url,
                    )
                }
        )
    }
    if (creatorRows.isNotEmpty()) {
        item {
            DetailTableCard(
                title = "Создатели Issues",
                headers = listOf("Кол-во"),
                rows = creatorRows,
            )
        }
    }
    if (assigneeRows.isNotEmpty()) {
        item {
            DetailTableCard(
                title = "Исполнители Issues",
                headers = listOf("Кол-во"),
                rows = assigneeRows,
            )
        }
    }
    if (labelChips.isNotEmpty()) {
        item {
            DetailChipSection(
                title = "Метки",
                chips = labelChips,
            )
        }
    }
    item {
        ScoreCard(
            score = score,
            title = "оценка Issue",
        )
    }
}

private fun LazyListScope.pullRequestItems(
    snapshots: Map<String, DetailParticipantSnapshot>,
    selectedSnapshot: DetailParticipantSnapshot?,
    peerSnapshots: List<DetailParticipantSnapshot>,
    effectiveParticipantId: String?,
    overallRank: Int?,
    overallScore: Double?,
    details: StatsDetailDataUi,
    rapidThreshold: ProjectStatsThresholdUi,
) {
    val filteredPullRequests = filterPullRequests(details, effectiveParticipantId)
    val current = selectedSnapshot
    val durationMinutes = filteredPullRequests.mapNotNull { durationMinutes(it.createdAtIso, it.closedAtIso) }
    val distributionSlices = buildPullRequestLifetimeSlices(durationMinutes)
    val contributorRows = snapshots.values
        .sortedByDescending { it.pullRequestCount }
        .map {
            DetailTableRow(
                title = appendYouSuffix(it.participant.name, it.participant.id == effectiveParticipantId),
                values = listOf(it.pullRequestCount.toString()),
                highlight = it.participant.id == effectiveParticipantId,
            )
        }
    val score = resolveScore(
        overallScore = overallScore,
        overallScope = effectiveParticipantId == null,
        specificScore = current?.pullRequestScore,
    )
    val rank = resolveRank(
        overallRank = overallRank,
        overallScope = effectiveParticipantId == null,
        specificScore = current?.pullRequestScore,
        peerScores = peerSnapshots.map { it.pullRequestScore },
    )
    val fastPullRequests = filteredPullRequests
        .filter { isRapidPullRequest(it, rapidThreshold.totalMinutes) }
        .sortedBy { durationMinutes(it.createdAtIso, it.closedAtIso) ?: Int.MAX_VALUE }
        .take(5)
    val slowPullRequests = filteredPullRequests
        .filter { durationMinutes(it.createdAtIso, it.closedAtIso) != null }
        .sortedByDescending { durationMinutes(it.createdAtIso, it.closedAtIso) ?: Int.MIN_VALUE }
        .take(5)

    item {
        ChartCard(
            title = "График Pull Requests",
            chartType = ProjectStatsChartType.Line,
            points = buildChartPoints(filteredPullRequests.mapNotNull { it.createdAtIso }) { "$it PR" },
            tooltipTitle = "Pull Requests",
        )
    }
    item {
        DoubleMetricRow(
            leftValue = formatDurationLabel(buildAverageLifetimeMinutes(durationMinutes)),
            leftCaption = "ср. время жизни",
            rightValue = filteredPullRequests.size.toString(),
            rightCaption = "всего Pull Request",
        )
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SingleMetricCard(
                modifier = Modifier.weight(1f),
                value = rank?.toString() ?: "—",
                caption = if (effectiveParticipantId == null) "место в рейтинге" else "место в команде",
            )
            SingleMetricCard(
                modifier = Modifier.weight(1f),
                value = formatDurationLabel(durationMinutes.minOrNull()),
                caption = "мин. время жизни",
            )
            SingleMetricCard(
                modifier = Modifier.weight(1f),
                value = formatDurationLabel(durationMinutes.maxOrNull()),
                caption = "макс. время жизни",
            )
        }
    }
    if (contributorRows.isNotEmpty()) {
        item {
            DetailTableCard(
                title = "Количество Pull Request",
                headers = listOf("Кол-во"),
                rows = contributorRows,
            )
        }
    }
    if (distributionSlices.isNotEmpty()) {
        item {
            DetailDonutCard(
                title = "Распределение времени жизни PR",
                slices = distributionSlices,
                highlightId = distributionSlices.maxByOrNull { it.value }?.label,
            )
        }
    }
    item {
        DetailEntityListCard(
            title = "Список Pull Requests",
            items = filteredPullRequests
                .sortedByDescending { parseInstant(it.createdAtIso)?.toEpochMilliseconds() ?: Long.MIN_VALUE }
                .take(20)
                .map { pullRequest ->
                    DetailEntityCardData(
                        title = pullRequest.title,
                        badge = pullRequest.number?.let { "#$it" },
                        status = pullRequest.state,
                        metaLines = buildList {
                            add("Автор: ${pullRequest.authorName}")
                            if (pullRequest.assigneeNames.isNotEmpty()) {
                                add("Участники: ${pullRequest.assigneeNames.joinToString()}")
                            }
                            add("Создано: ${pullRequest.createdAtLabel}")
                            pullRequest.closedAtLabel?.let { add("Закрыто: $it") }
                        },
                        trailing = formatDurationLabel(durationMinutes(pullRequest.createdAtIso, pullRequest.closedAtIso)),
                        link = pullRequest.url,
                    )
                }
        )
    }
    if (fastPullRequests.isNotEmpty()) {
        item {
            DetailEntityListCard(
                title = "Топ-5 самых быстрых PR",
                items = fastPullRequests.map { pullRequest ->
                    DetailEntityCardData(
                        title = pullRequest.title,
                        status = pullRequest.state,
                        metaLines = listOf(
                            "Автор: ${pullRequest.authorName}",
                            "Время: ${formatDurationLabel(durationMinutes(pullRequest.createdAtIso, pullRequest.closedAtIso))}",
                        ),
                        link = pullRequest.url,
                    )
                }
            )
        }
    }
    if (slowPullRequests.isNotEmpty()) {
        item {
            DetailEntityListCard(
                title = "Топ-5 самых медленных PR",
                items = slowPullRequests.map { pullRequest ->
                    DetailEntityCardData(
                        title = pullRequest.title,
                        status = pullRequest.state,
                        metaLines = listOf(
                            "Автор: ${pullRequest.authorName}",
                            "Время: ${formatDurationLabel(durationMinutes(pullRequest.createdAtIso, pullRequest.closedAtIso))}",
                        ),
                        link = pullRequest.url,
                    )
                }
            )
        }
    }
    item {
        ScoreCard(
            score = score,
            title = "оценка Pull Request",
        )
    }
}

private fun LazyListScope.rapidPullRequestItems(
    snapshots: Map<String, DetailParticipantSnapshot>,
    selectedSnapshot: DetailParticipantSnapshot?,
    peerSnapshots: List<DetailParticipantSnapshot>,
    effectiveParticipantId: String?,
    overallRank: Int?,
    overallScore: Double?,
    details: StatsDetailDataUi,
    rapidThreshold: ProjectStatsThresholdUi,
    onRapidThresholdChanged: (Int, Int, Int) -> Unit,
) {
    val filteredPullRequests = filterPullRequests(details, effectiveParticipantId)
    val rapidPullRequests = filteredPullRequests.filter { isRapidPullRequest(it, rapidThreshold.totalMinutes) }
    val current = selectedSnapshot
    val contributorRows = snapshots.values
        .sortedByDescending { it.rapidPullRequestCount }
        .map {
            DetailTableRow(
                title = appendYouSuffix(it.participant.name, it.participant.id == effectiveParticipantId),
                values = listOf(it.rapidPullRequestCount.toString()),
                highlight = it.participant.id == effectiveParticipantId,
            )
        }
    val leader = snapshots.values.maxByOrNull { it.rapidPullRequestCount }
    val score = resolveScore(
        overallScore = overallScore,
        overallScope = effectiveParticipantId == null,
        specificScore = current?.rapidPullScore,
    )
    val rank = resolveRank(
        overallRank = overallRank,
        overallScope = effectiveParticipantId == null,
        specificScore = current?.rapidPullScore,
        peerScores = peerSnapshots.map { it.rapidPullScore },
    )

    item {
        DetailThresholdSelector(
            threshold = rapidThreshold,
            onThresholdChanged = onRapidThresholdChanged,
        )
    }
    item {
        ChartCard(
            title = "График быстрых PR",
            chartType = ProjectStatsChartType.Line,
            points = buildChartPoints(rapidPullRequests.mapNotNull { it.closedAtIso }) { "$it быстрых PR" },
            tooltipTitle = "Быстрые PR",
        )
    }
    item {
        DoubleMetricRow(
            leftValue = rapidPullRequests.size.toString(),
            leftCaption = "быстрых PR",
            rightValue = rank?.toString() ?: "—",
            rightCaption = if (effectiveParticipantId == null) "место в рейтинге" else "место в команде",
        )
    }
    if (contributorRows.isNotEmpty()) {
        item {
            DetailTableCard(
                title = "Количество быстрых Pull Request",
                headers = listOf("Кол-во"),
                rows = contributorRows,
            )
        }
    }
    item {
        DetailEntityListCard(
            title = "Список быстрых Pull Requests",
            items = rapidPullRequests
                .sortedByDescending { parseInstant(it.closedAtIso)?.toEpochMilliseconds() ?: Long.MIN_VALUE }
                .take(20)
                .map { pullRequest ->
                    DetailEntityCardData(
                        title = pullRequest.title,
                        status = pullRequest.state,
                        metaLines = listOf(
                            "Автор: ${pullRequest.authorName}",
                            "Создано: ${pullRequest.createdAtLabel}",
                            "Закрыто: ${pullRequest.closedAtLabel ?: "—"}",
                        ),
                        trailing = formatDurationLabel(durationMinutes(pullRequest.createdAtIso, pullRequest.closedAtIso)),
                        link = pullRequest.url,
                    )
                }
        )
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SingleMetricCard(
                modifier = Modifier.weight(1f),
                value = percentLabel(rapidPullRequests.size, filteredPullRequests.size),
                caption = "доля быстрых PR",
            )
            SingleMetricCard(
                modifier = Modifier.weight(1f),
                value = leader?.participant?.name ?: "—",
                caption = "лидер по быстрым PR",
            )
        }
    }
    item {
        ScoreCard(
            score = score,
            title = "оценка быстрых PR",
        )
    }
}

private fun LazyListScope.codeChurnItems(
    snapshots: Map<String, DetailParticipantSnapshot>,
    selectedSnapshot: DetailParticipantSnapshot?,
    peerSnapshots: List<DetailParticipantSnapshot>,
    effectiveParticipantId: String?,
    overallRank: Int?,
    overallScore: Double?,
    details: StatsDetailDataUi,
) {
    val filteredCommits = filterCommits(details, effectiveParticipantId)
    val fileStats = buildFileAggregates(filteredCommits)
    val current = selectedSnapshot
    val participantRows = snapshots.values
        .sortedByDescending { it.fileStats.size }
        .map {
            DetailTableRow(
                title = appendYouSuffix(it.participant.name, it.participant.id == effectiveParticipantId),
                values = listOf(it.fileStats.size.toString()),
                highlight = it.participant.id == effectiveParticipantId,
            )
        }
    val churnSlices = buildFileChurnSlices(fileStats)
    val score = resolveScore(
        overallScore = overallScore,
        overallScope = effectiveParticipantId == null,
        specificScore = current?.codeChurnScore,
    )
    val rank = resolveRank(
        overallRank = overallRank,
        overallScope = effectiveParticipantId == null,
        specificScore = current?.codeChurnScore,
        peerScores = peerSnapshots.map { it.codeChurnScore },
    )

    item {
        DetailFilesCard(
            title = "Статистика по файлам",
            rows = fileStats.take(20),
        )
    }
    item {
        DoubleMetricRow(
            leftValue = fileStats.size.toString(),
            leftCaption = "изменено файлов",
            rightValue = rank?.toString() ?: "—",
            rightCaption = if (effectiveParticipantId == null) "место в рейтинге" else "место в команде",
        )
    }
    if (participantRows.isNotEmpty()) {
        item {
            DetailTableCard(
                title = "Количество измененных файлов",
                headers = listOf("Кол-во"),
                rows = participantRows,
            )
        }
    }
    if (churnSlices.isNotEmpty()) {
        item {
            DetailDonutCard(
                title = "Распределение изменений файлов",
                slices = churnSlices,
                highlightId = churnSlices.maxByOrNull { it.value }?.label,
            )
        }
    }
    item {
        DetailCard {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Самый часто изменяемый файл",
                    fontFamily = AppFonts.OpenSansSemiBold,
                    fontSize = 16.sp,
                    color = AppColors.Color2,
                )
                Text(
                    text = fileStats.firstOrNull()?.fileName ?: "Нет данных",
                    fontFamily = AppFonts.OpenSansBold,
                    fontSize = 16.sp,
                    color = AppColors.Color3,
                )
            }
        }
    }
    item {
        ScoreCard(
            score = score,
            title = "оценка изменчивости кода",
        )
    }
}

private fun LazyListScope.ownershipItems(
    snapshots: Map<String, DetailParticipantSnapshot>,
    selectedSnapshot: DetailParticipantSnapshot?,
    peerSnapshots: List<DetailParticipantSnapshot>,
    effectiveParticipantId: String?,
    overallRank: Int?,
    overallScore: Double?,
    details: StatsDetailDataUi,
) {
    val ownershipRows = buildOwnershipRows(details, snapshots)
    val totalLines = ownershipRows.sumOf { it.lines }
    val slices = ownershipRows.mapIndexed { index, row ->
        ProjectStatsDonutSliceUi(
            label = row.name,
            secondaryLabel = "${row.lines} строк",
            percentLabel = percentLabel(row.lines, totalLines),
            value = row.lines.toFloat(),
            colorHex = ownershipPalette[index % ownershipPalette.size],
            highlight = row.id == (effectiveParticipantId ?: details.defaultParticipantId),
        )
    }
    val score = resolveScore(
        overallScore = overallScore,
        overallScope = effectiveParticipantId == null,
        specificScore = selectedSnapshot?.codeOwnershipScore,
    )
    val rank = resolveRank(
        overallRank = overallRank,
        overallScope = effectiveParticipantId == null,
        specificScore = selectedSnapshot?.codeOwnershipScore,
        peerScores = peerSnapshots.map { it.codeOwnershipScore },
    )

    if (slices.isNotEmpty()) {
        item {
            DetailDonutCard(
                title = "Распределение владения кодом",
                slices = slices,
                highlightId = null,
            )
        }
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SingleMetricCard(
                modifier = Modifier.weight(1f),
                value = totalLines.toString(),
                caption = "всего строк",
            )
            SingleMetricCard(
                modifier = Modifier.weight(1f),
                value = rank?.toString() ?: "—",
                caption = if (effectiveParticipantId == null) "место в рейтинге" else "место в команде",
            )
        }
    }
    if (ownershipRows.isNotEmpty()) {
        item {
            DetailTableCard(
                title = "Участники",
                headers = listOf("Коммиты", "Строки"),
                rows = ownershipRows.map {
                    DetailTableRow(
                        title = appendYouSuffix(it.name, it.id == effectiveParticipantId),
                        values = listOf(it.commits.toString(), it.lines.toString()),
                        highlight = it.id == effectiveParticipantId,
                    )
                }
            )
        }
    }
    item {
        ScoreCard(
            score = score,
            title = "оценка владения кодом",
        )
    }
}

private fun LazyListScope.dominantWeekDayItems(
    snapshots: Map<String, DetailParticipantSnapshot>,
    selectedSnapshot: DetailParticipantSnapshot?,
    peerSnapshots: List<DetailParticipantSnapshot>,
    effectiveParticipantId: String?,
    overallRank: Int?,
    overallScore: Double?,
    details: StatsDetailDataUi,
) {
    val weekDays = buildWeekDayStats(
        commits = filterCommits(details, effectiveParticipantId),
        issues = filterIssues(details, effectiveParticipantId),
        pullRequests = filterPullRequests(details, effectiveParticipantId),
    )
    val total = weekDays.sumOf { it.count }
    val slices = weekDays.mapIndexed { index, item ->
        ProjectStatsDonutSliceUi(
            label = item.label,
            secondaryLabel = "${item.count} действий",
            percentLabel = percentLabel(item.count, total),
            value = item.count.toFloat(),
            colorHex = weekdayPalette[index % weekdayPalette.size],
            highlight = item.isDominant,
        )
    }
    val score = resolveScore(
        overallScore = overallScore,
        overallScope = effectiveParticipantId == null,
        specificScore = selectedSnapshot?.weekDayScore,
    )
    val rank = resolveRank(
        overallRank = overallRank,
        overallScope = effectiveParticipantId == null,
        specificScore = selectedSnapshot?.weekDayScore,
        peerScores = peerSnapshots.map { it.weekDayScore },
    )
    val dominant = weekDays.maxByOrNull { it.count }
    val leastActive = weekDays.minByOrNull { it.count }

    if (slices.isNotEmpty()) {
        item {
            DetailDonutCard(
                title = "Распределение активности по дням",
                slices = slices,
                highlightId = dominant?.label,
            )
        }
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DetailHighlightCard(
                modifier = Modifier.weight(1f),
                title = "Самый активный день",
                value = dominant?.label?.uppercase() ?: "НЕТ ДАННЫХ",
            )
            DetailHighlightCard(
                modifier = Modifier.weight(1f),
                title = "Самый неактивный день",
                value = leastActive?.label?.uppercase() ?: "НЕТ ДАННЫХ",
            )
        }
    }
    item {
        SingleMetricCard(
            value = rank?.toString() ?: "—",
            caption = if (effectiveParticipantId == null) "место в рейтинге" else "место в команде",
        )
    }
    item {
        ScoreCard(
            score = score,
            title = "оценка доминирующего дня недели",
        )
    }
}

@Composable
private fun DetailHeader(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Назад",
            tint = Color.Black,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(24.dp)
                .clickable(onClick = onBackClick)
        )
        Text(
            text = title,
            fontFamily = AppFonts.OpenSansBold,
            fontSize = 28.sp,
            lineHeight = 28.sp,
            color = AppColors.Color3,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun DetailDropdownSelector(
    title: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectedKey: String? = null,
) {
    DropdownSelector(
        title = title,
        value = value,
        options = options,
        onSelected = onSelected,
        modifier = modifier,
        selectedKey = selectedKey,
    )
}

@Composable
private fun DetailThresholdSelector(
    threshold: ProjectStatsThresholdUi,
    onThresholdChanged: (Int, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Период для быстроты",
                fontFamily = AppFonts.OpenSansSemiBold,
                fontSize = 16.sp,
                color = AppColors.Color2,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailValueSelector(
                    label = "д",
                    value = threshold.days,
                    values = (0..30).toList(),
                    onSelected = { onThresholdChanged(it, threshold.hours, threshold.minutes) },
                    modifier = Modifier.weight(1f),
                )
                DetailValueSelector(
                    label = "ч",
                    value = threshold.hours,
                    values = (0..23).toList(),
                    onSelected = { onThresholdChanged(threshold.days, it, threshold.minutes) },
                    modifier = Modifier.weight(1f),
                )
                DetailValueSelector(
                    label = "мин",
                    value = threshold.minutes,
                    values = (0..59).toList(),
                    onSelected = { onThresholdChanged(threshold.days, threshold.hours, it) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DetailValueSelector(
    label: String,
    value: Int,
    values: List<Int>,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailDropdownSelector(
        title = "",
        value = "$value $label",
        options = values.map { it.toString() to "$it $label" },
        onSelected = { onSelected(it.toInt()) },
        modifier = modifier,
        selectedKey = value.toString(),
    )
}

@Composable
private fun DualLineChartCard(
    title: String,
    points: List<DetailLinePoint>,
    modifier: Modifier = Modifier,
) {
    val maxValue = remember(points) {
        points.maxOfOrNull { maxOf(it.firstValue, it.secondValue) }?.coerceAtLeast(1) ?: 1
    }
    DetailCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = title,
                fontFamily = AppFonts.OpenSansSemiBold,
                fontSize = 16.sp,
                color = AppColors.Color2,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp, bottom = 24.dp)
                ) {
                    if (points.isEmpty()) return@Canvas
                    val widthStep = size.width / (points.size.coerceAtLeast(2) - 1).toFloat()
                    fun y(value: Int): Float {
                        val fraction = value.toFloat() / maxValue.toFloat()
                        return size.height - 32.dp.toPx() - (size.height - 40.dp.toPx()) * fraction
                    }

                    val addPath = Path()
                    val deletePath = Path()
                    points.forEachIndexed { index, point ->
                        val x = widthStep * index
                        val addY = y(point.firstValue)
                        val delY = y(point.secondValue)
                        if (index == 0) {
                            addPath.moveTo(x, addY)
                            deletePath.moveTo(x, delY)
                        } else {
                            addPath.lineTo(x, addY)
                            deletePath.lineTo(x, delY)
                        }
                        drawCircle(Color(0xFF11B78B), 4.dp.toPx(), Offset(x, addY))
                        drawCircle(Color(0xFFC21807), 4.dp.toPx(), Offset(x, delY))
                    }
                    drawPath(
                        path = addPath,
                        color = Color(0xFF11B78B),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    )
                    drawPath(
                        path = deletePath,
                        color = Color(0xFFC21807),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    points.forEach { point ->
                        Text(
                            text = point.label,
                            fontFamily = AppFonts.OpenSansMedium,
                            fontSize = 12.sp,
                            color = AppColors.Color2,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendBadge("Добавлено", Color(0xFF11B78B))
                LegendBadge("Удалено", Color(0xFFC21807))
            }
        }
    }
}

@Composable
private fun LegendBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Text(
            text = text,
            fontFamily = AppFonts.OpenSansMedium,
            fontSize = 12.sp,
            color = AppColors.Color2,
        )
    }
}

@Composable
private fun DetailFilesCard(
    title: String,
    rows: List<DetailFileAggregate>,
    modifier: Modifier = Modifier,
) {
    DetailCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = title,
                fontFamily = AppFonts.OpenSansSemiBold,
                fontSize = 16.sp,
                color = AppColors.Color2,
            )
            if (rows.isEmpty()) {
                Text(
                    text = "Нет данных",
                    fontFamily = AppFonts.OpenSansMedium,
                    fontSize = 13.sp,
                    color = AppColors.Color2,
                )
            } else {
                rows.forEachIndexed { index, row ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = row.fileName,
                                fontFamily = AppFonts.OpenSansMedium,
                                fontSize = 13.sp,
                                color = AppColors.Color2,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = row.changes.toString(),
                                fontFamily = AppFonts.OpenSansBold,
                                fontSize = 13.sp,
                                color = AppColors.Color3,
                            )
                        }
                        Text(
                            text = buildString {
                                append("+${row.additions} / -${row.deletions}")
                                row.status?.takeIf { it.isNotBlank() }?.let {
                                    append(" • $it")
                                }
                            },
                            fontFamily = AppFonts.OpenSansRegular,
                            fontSize = 12.sp,
                            color = AppColors.Color2,
                        )
                        if (index < rows.lastIndex) {
                            DetailDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailTableCard(
    title: String,
    headers: List<String>,
    rows: List<DetailTableRow>,
    modifier: Modifier = Modifier,
) {
    DetailCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = title,
                fontFamily = AppFonts.OpenSansSemiBold,
                fontSize = 16.sp,
                color = AppColors.Color2,
            )
            if (rows.isEmpty()) {
                Text(
                    text = "Нет данных",
                    fontFamily = AppFonts.OpenSansMedium,
                    fontSize = 13.sp,
                    color = AppColors.Color2,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Название",
                        fontFamily = AppFonts.OpenSansBold,
                        fontSize = 12.sp,
                        color = AppColors.Color2,
                        modifier = Modifier.weight(1f),
                    )
                    headers.forEach { header ->
                        Text(
                            text = header,
                            fontFamily = AppFonts.OpenSansBold,
                            fontSize = 12.sp,
                            color = AppColors.Color2,
                            textAlign = TextAlign.End,
                            modifier = Modifier.widthIn(min = 72.dp),
                        )
                    }
                }
                rows.forEachIndexed { index, row ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = row.title,
                                fontFamily = AppFonts.OpenSansRegular,
                                fontWeight = if (row.highlight) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (row.highlight) AppColors.Color3 else AppColors.Color2,
                                modifier = Modifier.weight(1f),
                            )
                            row.values.forEach { value ->
                                Text(
                                    text = value,
                                    fontFamily = AppFonts.OpenSansSemiBold,
                                    fontSize = 13.sp,
                                    color = AppColors.Color2,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.widthIn(min = 72.dp),
                                )
                            }
                        }
                        if (index < rows.lastIndex) {
                            DetailDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailEntityListCard(
    title: String,
    items: List<DetailEntityCardData>,
    modifier: Modifier = Modifier,
) {
    DetailCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = title,
                fontFamily = AppFonts.OpenSansSemiBold,
                fontSize = 16.sp,
                color = AppColors.Color2,
            )
            if (items.isEmpty()) {
                Text(
                    text = "Нет данных",
                    fontFamily = AppFonts.OpenSansMedium,
                    fontSize = 13.sp,
                    color = AppColors.Color2,
                )
            } else {
                items.forEachIndexed { index, item ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = item.title,
                                    fontFamily = AppFonts.OpenSansBold,
                                    fontSize = 14.sp,
                                    color = AppColors.Color2,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item.badge?.let { badge ->
                                        DetailChip(text = badge)
                                    }
                                    item.status?.let { status ->
                                        DetailChip(text = status.uppercase())
                                    }
                                    item.secondary?.let { secondary ->
                                        Text(
                                            text = secondary,
                                            fontFamily = AppFonts.OpenSansMedium,
                                            fontSize = 12.sp,
                                            color = AppColors.Color2,
                                        )
                                    }
                                }
                            }
                            item.trailing?.let { trailing ->
                                Text(
                                    text = trailing,
                                    fontFamily = AppFonts.OpenSansBold,
                                    fontSize = 12.sp,
                                    color = AppColors.Color3,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.padding(start = 12.dp),
                                )
                            }
                        }
                        item.metaLines.forEach { line ->
                            Text(
                                text = line,
                                fontFamily = AppFonts.OpenSansRegular,
                                fontSize = 12.sp,
                                color = AppColors.Color2,
                            )
                        }
                        item.link?.let { link ->
                            Text(
                                text = link,
                                fontFamily = AppFonts.OpenSansBold,
                                fontSize = 12.sp,
                                color = AppColors.Color3,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (index < items.lastIndex) {
                            DetailDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailChipSection(
    title: String,
    chips: List<String>,
    modifier: Modifier = Modifier,
) {
    DetailCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = title,
                fontFamily = AppFonts.OpenSansSemiBold,
                fontSize = 16.sp,
                color = AppColors.Color2,
            )
            val rows = chips.chunked(3)
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { chip ->
                        DetailChip(text = chip)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailDonutCard(
    title: String,
    slices: List<ProjectStatsDonutSliceUi>,
    highlightId: String?,
    modifier: Modifier = Modifier,
) {
    DetailCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = title,
                fontFamily = AppFonts.OpenSansSemiBold,
                fontSize = 16.sp,
                color = AppColors.Color2,
            )
            DonutChart(slices = slices)
            slices.forEach { slice ->
                val labelFontFamily = if (slice.label == highlightId || slice.highlight) {
                    AppFonts.OpenSansBold
                } else {
                    AppFonts.OpenSansRegular
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                            withStyle(SpanStyle(color = AppColors.Color3, fontFamily = AppFonts.OpenSansBold)) {
                                append(slice.secondaryLabel)
                            }
                        },
                        fontFamily = labelFontFamily,
                        fontSize = 13.sp,
                        color = AppColors.Color2,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailHighlightCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    DetailCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = value,
                fontFamily = AppFonts.OpenSansBold,
                fontSize = 32.sp,
                lineHeight = 32.sp,
                color = AppColors.Color3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = title,
                fontFamily = AppFonts.OpenSansMedium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = AppColors.Color2,
            )
        }
    }
}

@Composable
private fun DetailExportActions(
    onExportPdfClick: () -> Unit,
    onExportExcelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            DetailActionRow(
                text = "Экспорт в PDF",
                icon = {
                    Image(
                        painter = painterResource(Res.drawable.stats_footer_pdf),
                        contentDescription = null,
                        modifier = Modifier
                            .width(16.dp)
                            .height(22.4.dp),
                    )
                },
                onClick = onExportPdfClick,
            )
            DetailActionRow(
                text = "Экспорт в Excel",
                icon = {
                    Image(
                        painter = painterResource(Res.drawable.stats_footer_excel),
                        contentDescription = null,
                        modifier = Modifier
                            .width(16.dp)
                            .height(22.4.dp),
                    )
                },
                onClick = onExportExcelClick,
            )
        }
    }
}

@Composable
private fun DetailActionRow(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon()
        Text(
            text = text,
            fontFamily = AppFonts.OpenSansMedium,
            fontSize = 14.sp,
            color = AppColors.Color2,
        )
    }
}

@Composable
private fun DetailChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        color = Color(0xFFF2F2F4),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE4E4E7)),
    ) {
        Text(
            text = text,
            fontFamily = AppFonts.OpenSansMedium,
            fontSize = 11.sp,
            color = AppColors.Color2,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun DetailCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = DetailCardShape,
        color = Color.White,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            content = content,
        )
    }
}

@Composable
private fun DetailDivider(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFE4E4E6))
    )
}

private fun detailTitle(section: StatsScreenSection): String {
    return when (section) {
        StatsScreenSection.Commits -> "Коммиты"
        StatsScreenSection.Issues -> "Issue"
        StatsScreenSection.PullRequests -> "Pull Request"
        StatsScreenSection.RapidPullRequests -> "Быстрые PR"
        StatsScreenSection.CodeChurn -> "Рефакторинг"
        StatsScreenSection.CodeOwnership -> "Владение"
        StatsScreenSection.DominantWeekDay -> "Доминирующий день недели"
    }
}

private fun filterCommits(
    details: StatsDetailDataUi,
    participantId: String?,
): List<StatsDetailCommitUi> {
    if (participantId == null) return details.commits
    return details.commits.filter { it.authorId == participantId }
}

private fun filterIssues(
    details: StatsDetailDataUi,
    participantId: String?,
): List<StatsDetailIssueUi> {
    if (participantId == null) return details.issues
    return details.issues.filter { issue ->
        issue.creatorId == participantId || issue.assigneeIds.contains(participantId)
    }
}

private fun filterPullRequests(
    details: StatsDetailDataUi,
    participantId: String?,
): List<StatsDetailPullRequestUi> {
    if (participantId == null) return details.pullRequests
    return details.pullRequests.filter { it.authorId == participantId }
}

private fun buildParticipantSnapshots(
    details: StatsDetailDataUi,
    rapidThresholdMinutes: Int,
): Map<String, DetailParticipantSnapshot> {
    val participants = linkedMapOf<String, StatsDetailParticipantUi>()
    details.participants.forEach { participants[it.id] = it }
    details.commits.forEach { commit ->
        commit.authorId?.let { id ->
            if (!participants.containsKey(id)) {
                participants[id] = StatsDetailParticipantUi(
                    id = id,
                    name = commit.authorName,
                    subtitle = "Участник",
                )
            }
        }
    }
    details.issues.forEach { issue ->
        issue.creatorId?.let { id ->
            if (!participants.containsKey(id)) {
                participants[id] = StatsDetailParticipantUi(
                    id = id,
                    name = issue.creatorName,
                    subtitle = "Участник",
                )
            }
        }
        issue.assigneeIds.forEachIndexed { index, id ->
            if (!participants.containsKey(id)) {
                participants[id] = StatsDetailParticipantUi(
                    id = id,
                    name = issue.assigneeNames.getOrNull(index) ?: id,
                    subtitle = "Участник",
                )
            }
        }
    }
    details.pullRequests.forEach { pullRequest ->
        pullRequest.authorId?.let { id ->
            if (!participants.containsKey(id)) {
                participants[id] = StatsDetailParticipantUi(
                    id = id,
                    name = pullRequest.authorName,
                    subtitle = "Участник",
                )
            }
        }
    }

    val totalProjectLines = details.commits.sumOf { it.additions + it.deletions + it.changes }

    return participants.mapValues { (_, participant) ->
        val userCommits = details.commits.filter { it.authorId == participant.id }
        val userIssues = details.issues.filter {
            it.creatorId == participant.id || it.assigneeIds.contains(participant.id)
        }
        val userPullRequests = details.pullRequests.filter { it.authorId == participant.id }
        val rapidPullRequests = userPullRequests.filter {
            isRapidPullRequest(it, rapidThresholdMinutes)
        }
        val fileStats = buildFileAggregates(userCommits)
        val weekDays = buildWeekDayStats(userCommits, userIssues, userPullRequests)
        val linesOwned = userCommits.sumOf { it.additions + it.deletions + it.changes }
        DetailParticipantSnapshot(
            participant = participant,
            commits = userCommits,
            issues = userIssues,
            pullRequests = userPullRequests,
            rapidPullRequests = rapidPullRequests,
            fileStats = fileStats,
            weekDays = weekDays,
            linesOwned = linesOwned,
            commitCount = userCommits.size,
            issueCount = userIssues.size,
            openIssueCount = userIssues.count { it.closedAtIso == null },
            closedIssueCount = userIssues.count { it.closedAtIso != null },
            pullRequestCount = userPullRequests.size,
            rapidPullRequestCount = rapidPullRequests.size,
            commitsScore = totalCommitsScore(userCommits),
            issueScore = issueCompletenessScore(userIssues),
            pullRequestScore = pullRequestHangScore(userPullRequests),
            rapidPullScore = rapidPullScore(userPullRequests, rapidThresholdMinutes),
            codeChurnScore = codeChurnScore(fileStats, userCommits.size),
            codeOwnershipScore = if (totalProjectLines > 0 && linesOwned > 0) {
                round2((2 + 3 * (linesOwned.toDouble() / totalProjectLines.toDouble())).coerceIn(0.0, 5.0))
            } else {
                null
            },
            weekDayScore = weekDayScore(weekDays),
        )
    }
}

private fun buildFileAggregates(
    commits: List<StatsDetailCommitUi>,
): List<DetailFileAggregate> {
    val stats = linkedMapOf<String, DetailFileAggregateBuilder>()
    commits.forEach { commit ->
        commit.files.forEach { file ->
            if (file.fileName.isBlank()) return@forEach
            val builder = stats.getOrPut(file.fileName) { DetailFileAggregateBuilder() }
            builder.changes += file.changes.takeIf { it > 0 } ?: (file.additions + file.deletions)
            builder.additions += file.additions
            builder.deletions += file.deletions
            file.status?.let { status ->
                builder.statusCounts[status] = (builder.statusCounts[status] ?: 0) + 1
            }
        }
    }
    return stats.entries
        .map { (fileName, builder) ->
            DetailFileAggregate(
                fileName = fileName,
                changes = builder.changes,
                additions = builder.additions,
                deletions = builder.deletions,
                status = builder.statusCounts.maxByOrNull { it.value }?.key,
            )
        }
        .sortedByDescending { it.changes }
}

private fun buildWeekDayStats(
    commits: List<StatsDetailCommitUi>,
    issues: List<StatsDetailIssueUi>,
    pullRequests: List<StatsDetailPullRequestUi>,
): List<DetailWeekDayStat> {
    val labels = listOf(
        "Понедельник",
        "Вторник",
        "Среда",
        "Четверг",
        "Пятница",
        "Суббота",
        "Воскресенье",
    )
    val counts = linkedMapOf<String, Int>().apply {
        labels.forEach { put(it, 0) }
    }

    fun bump(value: String?) {
        val label = weekdayLabel(value) ?: return
        counts[label] = (counts[label] ?: 0) + 1
    }

    commits.forEach { bump(it.committedAtIso) }
    issues.forEach {
        bump(it.createdAtIso)
        bump(it.closedAtIso)
    }
    pullRequests.forEach {
        bump(it.createdAtIso)
        bump(it.closedAtIso)
    }

    val dominant = counts.maxByOrNull { it.value }?.key
    return counts.entries.map { (label, count) ->
        DetailWeekDayStat(
            label = label,
            count = count,
            isDominant = label == dominant,
        )
    }
}

private fun buildChartPoints(
    dates: List<String>,
    hintFormatter: (Int) -> String,
): List<ProjectStatsChartPointUi> {
    if (dates.isEmpty()) return emptyList()
    return dates
        .mapNotNull(::parseInstant)
        .groupingBy { instant ->
            val date = instant.toLocalDateTime(TimeZone.UTC).date
            "${date.dayOfMonth.toString().padStart(2, '0')}.${date.monthNumber.toString().padStart(2, '0')}.${(date.year % 100).toString().padStart(2, '0')}"
        }
        .eachCount()
        .entries
        .map { entry ->
            ProjectStatsChartPointUi(
                label = entry.key,
                value = entry.value.toFloat(),
                valueLabel = entry.value.toString(),
                hint = hintFormatter(entry.value),
            )
        }
}

private fun buildDailyCounts(dates: List<String>): List<DetailCountPoint> {
    return dates
        .mapNotNull(::parseInstant)
        .groupingBy { it.toLocalDateTime(TimeZone.UTC).date }
        .eachCount()
        .entries
        .sortedBy { it.key }
        .map { (date, count) ->
            DetailCountPoint(
                label = formatDate(date),
                count = count,
            )
        }
}

private fun buildLineDeltaPoints(commits: List<StatsDetailCommitUi>): List<DetailLinePoint> {
    val grouped = linkedMapOf<String, Pair<Int, Int>>()
    commits.forEach { commit ->
        val date = parseInstant(commit.committedAtIso)?.toLocalDateTime(TimeZone.UTC)?.date ?: return@forEach
        val key = formatDate(date)
        val previous = grouped[key] ?: (0 to 0)
        grouped[key] = (previous.first + commit.additions) to (previous.second + commit.deletions)
    }
    return grouped.entries.map { (label, values) ->
        DetailLinePoint(
            label = label,
            firstValue = values.first,
            secondValue = values.second,
        )
    }
}

private fun buildAverageLifetimeMinutes(values: List<Int>): Int? {
    if (values.isEmpty()) return null
    return (values.average()).roundToInt()
}

private fun buildPullRequestLifetimeSlices(
    durations: List<Int>,
): List<ProjectStatsDonutSliceUi> {
    if (durations.isEmpty()) return emptyList()
    val buckets = listOf(
        "< 1 часа" to durations.count { it < 60 },
        "1-6 часов" to durations.count { it in 60 until 360 },
        "6-24 часа" to durations.count { it in 360 until 1440 },
        "1-3 дня" to durations.count { it in 1440 until 4320 },
        "> 3 дней" to durations.count { it >= 4320 },
    ).filter { it.second > 0 }
    val total = durations.size
    return buckets.mapIndexed { index, (label, value) ->
        ProjectStatsDonutSliceUi(
            label = label,
            secondaryLabel = "$value PR",
            percentLabel = percentLabel(value, total),
            value = value.toFloat(),
            colorHex = ownershipPalette[index % ownershipPalette.size],
            highlight = value == buckets.maxOf { it.second },
        )
    }
}

private fun buildFileChurnSlices(
    fileStats: List<DetailFileAggregate>,
): List<ProjectStatsDonutSliceUi> {
    if (fileStats.isEmpty()) return emptyList()
    val buckets = listOf(
        "1 изменение" to fileStats.count { it.changes == 1 },
        "2-3" to fileStats.count { it.changes in 2..3 },
        "4-5" to fileStats.count { it.changes in 4..5 },
        "6-10" to fileStats.count { it.changes in 6..10 },
        "11+" to fileStats.count { it.changes >= 11 },
    ).filter { it.second > 0 }
    val total = fileStats.size
    return buckets.mapIndexed { index, (label, value) ->
        ProjectStatsDonutSliceUi(
            label = label,
            secondaryLabel = "$value файлов",
            percentLabel = percentLabel(value, total),
            value = value.toFloat(),
            colorHex = weekdayPalette[index % weekdayPalette.size],
            highlight = value == buckets.maxOf { it.second },
        )
    }
}

private fun buildIssueCreatorRows(
    issues: List<StatsDetailIssueUi>,
    effectiveParticipantId: String?,
): List<DetailTableRow> {
    return issues.groupingBy { it.creatorName }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .map { entry ->
            DetailTableRow(
                title = appendYouSuffix(entry.key, issues.any { it.creatorId == effectiveParticipantId && it.creatorName == entry.key }),
                values = listOf(entry.value.toString()),
                highlight = issues.any { it.creatorId == effectiveParticipantId && it.creatorName == entry.key },
            )
        }
}

private fun buildIssueAssigneeRows(
    issues: List<StatsDetailIssueUi>,
    effectiveParticipantId: String?,
): List<DetailTableRow> {
    val counts = linkedMapOf<String, Int>()
    issues.forEach { issue ->
        issue.assigneeNames.forEach { assignee ->
            counts[assignee] = (counts[assignee] ?: 0) + 1
        }
    }
    return counts.entries.sortedByDescending { it.value }.map { entry ->
        DetailTableRow(
            title = appendYouSuffix(entry.key, issues.any { issue ->
                issue.assigneeIds.contains(effectiveParticipantId) && issue.assigneeNames.contains(entry.key)
            }),
            values = listOf(entry.value.toString()),
            highlight = issues.any { issue ->
                issue.assigneeIds.contains(effectiveParticipantId) && issue.assigneeNames.contains(entry.key)
            },
        )
    }
}

private fun buildLabelChips(issues: List<StatsDetailIssueUi>): List<String> {
    val counts = linkedMapOf<String, Int>()
    issues.flatMap { it.labels }.forEach { label ->
        counts[label] = (counts[label] ?: 0) + 1
    }
    return counts.entries.sortedByDescending { it.value }.take(10).map { "${it.key} ${it.value}" }
}

private fun buildOwnershipRows(
    details: StatsDetailDataUi,
    snapshots: Map<String, DetailParticipantSnapshot>,
): List<DetailOwnershipRow> {
    return snapshots.values
        .sortedByDescending { it.linesOwned }
        .map { snapshot ->
            DetailOwnershipRow(
                id = snapshot.participant.id,
                name = snapshot.participant.name,
                commits = snapshot.commitCount,
                lines = snapshot.linesOwned,
            )
        }
}

private fun resolveScore(
    overallScore: Double?,
    overallScope: Boolean,
    specificScore: Double?,
): Double? {
    return if (overallScope) overallScore else specificScore
}

private fun resolveRank(
    overallRank: Int?,
    overallScope: Boolean,
    specificScore: Double?,
    peerScores: List<Double?>,
): Int? {
    if (overallScope) return overallRank
    if (specificScore == null) return null
    val ranked = (peerScores + specificScore).filterNotNull().sortedDescending()
    return ranked.indexOfFirst { it == specificScore }.takeIf { it >= 0 }?.plus(1)
}

private fun totalCommitsScore(commits: List<StatsDetailCommitUi>): Double? {
    if (commits.isEmpty()) return null
    val dates = commits.mapNotNull { parseInstant(it.committedAtIso)?.toEpochMilliseconds() }
    val start = dates.minOrNull() ?: return null
    val end = dates.maxOrNull() ?: return null
    val dayCount = (end - start).toDouble() / MILLIS_PER_DAY
    if (dayCount <= 0) return 5.0
    val commitsPerDay = commits.size / dayCount
    return round2((commitsPerDay * 3 + 2).coerceIn(0.0, 5.0))
}

private fun issueCompletenessScore(issues: List<StatsDetailIssueUi>): Double? {
    if (issues.isEmpty()) return null
    val closed = issues.count { it.closedAtIso != null }
    return round2((closed.toDouble() / issues.size) * 3 + 2)
}

private fun pullRequestHangScore(pullRequests: List<StatsDetailPullRequestUi>): Double? {
    val durations = pullRequests.mapNotNull { durationMinutes(it.createdAtIso, it.closedAtIso) }
    if (durations.isEmpty()) return null
    val averageMinutes = durations.average()
    val averageMillis = averageMinutes * MILLIS_PER_MINUTE
    return if (averageMillis < 5 * MILLIS_PER_MINUTE) {
        round2(5.0)
    } else {
        round2(((1 - averageMillis / (7 * MILLIS_PER_DAY)) * 3 + 2).coerceAtLeast(0.0))
    }
}

private fun rapidPullScore(
    pullRequests: List<StatsDetailPullRequestUi>,
    thresholdMinutes: Int,
): Double? {
    if (pullRequests.isEmpty()) return null
    val rapidCount = pullRequests.count { isRapidPullRequest(it, thresholdMinutes) }
    return round2((1 - rapidCount.toDouble() / pullRequests.size) * 3 + 2)
}

private fun codeChurnScore(
    fileStats: List<DetailFileAggregate>,
    commitCount: Int,
): Double? {
    if (fileStats.isEmpty() || commitCount <= 0) return null
    val churnPerCommit = fileStats.sumOf { it.changes }.toDouble() / commitCount
    return round2((5.0 - ln(1.0 + churnPerCommit).coerceAtMost(4.0) * 1.2).coerceIn(0.0, 5.0))
}

private fun weekDayScore(weekdays: List<DetailWeekDayStat>): Double? {
    if (weekdays.isEmpty()) return null
    val dominant = weekdays.maxByOrNull { it.count } ?: return null
    val total = weekdays.sumOf { it.count }
    if (total <= 0) return null
    val dominantShare = dominant.count.toDouble() / total.toDouble()
    return round2((2 + 3 * dominantShare).coerceIn(0.0, 5.0))
}

private fun isRapidPullRequest(
    pullRequest: StatsDetailPullRequestUi,
    thresholdMinutes: Int,
): Boolean {
    val duration = durationMinutes(pullRequest.createdAtIso, pullRequest.closedAtIso) ?: return false
    return duration < thresholdMinutes
}

private fun durationMinutes(
    startIso: String?,
    endIso: String?,
): Int? {
    val start = parseInstant(startIso)?.toEpochMilliseconds() ?: return null
    val end = parseInstant(endIso)?.toEpochMilliseconds() ?: return null
    if (end <= start) return null
    return ((end - start) / MILLIS_PER_MINUTE).toInt()
}

private fun weekdayLabel(value: String?): String? {
    val instant = parseInstant(value) ?: return null
    return when (instant.toLocalDateTime(TimeZone.UTC).dayOfWeek.name.lowercase()) {
        "monday" -> "Понедельник"
        "tuesday" -> "Вторник"
        "wednesday" -> "Среда"
        "thursday" -> "Четверг"
        "friday" -> "Пятница"
        "saturday" -> "Суббота"
        "sunday" -> "Воскресенье"
        else -> null
    }
}

private fun parseInstant(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return runCatching { Instant.parse(value) }.getOrNull()
}

private fun formatDate(date: kotlinx.datetime.LocalDate): String {
    val day = date.dayOfMonth.toString().padStart(2, '0')
    val month = date.monthNumber.toString().padStart(2, '0')
    val year = (date.year % 100).toString().padStart(2, '0')
    return "$day.$month.$year"
}

private fun formatDurationLabel(minutes: Int?): String {
    if (minutes == null) return "—"
    if (minutes < 60) return "$minutes мин"
    if (minutes < 1440) {
        val hours = minutes / 60
        val rest = minutes % 60
        return if (rest == 0) "$hours ч" else "$hours ч $rest мин"
    }
    val days = minutes / 1440
    val restHours = (minutes % 1440) / 60
    val dayWord = pluralize(days, "день", "дня", "дней")
    return if (restHours == 0) "$days $dayWord" else "$days $dayWord $restHours ч"
}

private fun formatCompactNumber(value: Double): String {
    val rounded = (value * 10.0).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString().replace('.', ',')
    }
}

private fun percentLabel(value: Int, total: Int): String {
    if (total <= 0) return "0%"
    return "${((value.toDouble() / total.toDouble()) * 100.0).roundToInt()}%"
}

private fun pluralize(
    count: Int,
    one: String,
    few: String,
    many: String,
): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
}

private fun appendYouSuffix(
    name: String,
    highlight: Boolean,
): String {
    return if (highlight) "$name (Вы)" else name
}

private fun round2(value: Double): Double = ((value * 100.0).roundToInt() / 100.0)

private val ownershipPalette = listOf(
    0xFFC21807L,
    0xFF0B73D9L,
    0xFF11B78BL,
    0xFF6821D8L,
    0xFFC69207L,
    0xFF0BA7D9L,
    0xFF2A18D8L,
)

private val weekdayPalette = listOf(
    0xFFC21807L,
    0xFFC69207L,
    0xFF0BA7D9L,
    0xFF6821D8L,
    0xFFC2C807L,
    0xFF11B78BL,
    0xFF2A18D8L,
)

private const val MILLIS_PER_MINUTE = 60_000.0
private const val MILLIS_PER_DAY = 86_400_000.0

private enum class DetailDatePickerTarget {
    Start,
    End,
}

private data class DetailLinePoint(
    val label: String,
    val firstValue: Int,
    val secondValue: Int,
)

private data class DetailCountPoint(
    val label: String,
    val count: Int,
)

private data class DetailFileAggregate(
    val fileName: String,
    val changes: Int,
    val additions: Int,
    val deletions: Int,
    val status: String?,
)

private data class DetailFileAggregateBuilder(
    var changes: Int = 0,
    var additions: Int = 0,
    var deletions: Int = 0,
    val statusCounts: MutableMap<String, Int> = linkedMapOf(),
)

private data class DetailWeekDayStat(
    val label: String,
    val count: Int,
    val isDominant: Boolean,
)

private data class DetailParticipantSnapshot(
    val participant: StatsDetailParticipantUi,
    val commits: List<StatsDetailCommitUi>,
    val issues: List<StatsDetailIssueUi>,
    val pullRequests: List<StatsDetailPullRequestUi>,
    val rapidPullRequests: List<StatsDetailPullRequestUi>,
    val fileStats: List<DetailFileAggregate>,
    val weekDays: List<DetailWeekDayStat>,
    val linesOwned: Int,
    val commitCount: Int,
    val issueCount: Int,
    val openIssueCount: Int,
    val closedIssueCount: Int,
    val pullRequestCount: Int,
    val rapidPullRequestCount: Int,
    val commitsScore: Double?,
    val issueScore: Double?,
    val pullRequestScore: Double?,
    val rapidPullScore: Double?,
    val codeChurnScore: Double?,
    val codeOwnershipScore: Double?,
    val weekDayScore: Double?,
)

private data class DetailEntityCardData(
    val title: String,
    val badge: String? = null,
    val status: String? = null,
    val metaLines: List<String>,
    val secondary: String? = null,
    val trailing: String? = null,
    val link: String? = null,
)

private data class DetailTableRow(
    val title: String,
    val values: List<String>,
    val highlight: Boolean = false,
)

private data class DetailOwnershipRow(
    val id: String,
    val name: String,
    val commits: Int,
    val lines: Int,
)
