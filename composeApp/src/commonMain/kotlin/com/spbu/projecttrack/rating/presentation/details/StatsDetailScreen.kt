package com.spbu.projecttrack.rating.presentation.details

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spbu.projecttrack.core.search.matchesSearchQuery
import com.spbu.projecttrack.core.logging.AppLog
import com.spbu.projecttrack.core.settings.localizedString
import com.spbu.projecttrack.core.theme.AppColors
import com.spbu.projecttrack.core.theme.AppFonts
import com.spbu.projecttrack.core.theme.appPalette
import com.spbu.projecttrack.core.theme.subtleBorder
import com.spbu.projecttrack.core.ui.lazyListEdgeFadeMask
import com.spbu.projecttrack.rating.common.StatsDetailCopy
import com.spbu.projecttrack.rating.common.StatsExportCopy
import com.spbu.projecttrack.rating.common.formatDurationMinutesLabel
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
import com.spbu.projecttrack.rating.data.model.filterByParticipant
import com.spbu.projecttrack.rating.data.repository.displayPersonName
import com.spbu.projecttrack.rating.presentation.projectstats.ChartCard
import com.spbu.projecttrack.rating.presentation.projectstats.DonutChart
import com.spbu.projecttrack.rating.presentation.projectstats.DoubleMetricRow
import com.spbu.projecttrack.rating.presentation.projectstats.DropdownSelector
import com.spbu.projecttrack.rating.presentation.projectstats.EmptyDetailedInfoCard
import com.spbu.projecttrack.rating.presentation.projectstats.EqualVerticalMetricLayout
import com.spbu.projecttrack.rating.presentation.projectstats.IssueProgressCard
import com.spbu.projecttrack.rating.presentation.projectstats.CompactStatsCardHeight
import com.spbu.projecttrack.rating.presentation.projectstats.MetricCardHorizontalPadding
import com.spbu.projecttrack.rating.presentation.projectstats.RepositorySelectorCard
import com.spbu.projecttrack.rating.presentation.projectstats.ScoreCard
import com.spbu.projecttrack.rating.presentation.projectstats.SingleMetricCard
import com.spbu.projecttrack.rating.presentation.projectstats.formatScoreValue
import com.spbu.projecttrack.rating.presentation.projectstats.projectScoreColor
import com.spbu.projecttrack.rating.presentation.projectstats.StatsBackgroundLogo
import com.spbu.projecttrack.rating.presentation.projectstats.StatsDateRangePickerDialog
import com.spbu.projecttrack.rating.presentation.projectstats.rememberStatsBackDispatcher
import com.spbu.projecttrack.rating.presentation.projectstats.StatsTopBar
import com.spbu.projecttrack.rating.presentation.projectstats.StatsTopBarTotalHeight
import com.spbu.projecttrack.rating.presentation.projectstats.WeekDayDistributionCard
import com.spbu.projecttrack.rating.presentation.settings.StatsScreenSection
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import projecttrack.composeapp.generated.resources.Res
import projecttrack.composeapp.generated.resources.search_icon
import projecttrack.composeapp.generated.resources.stats_footer_excel
import projecttrack.composeapp.generated.resources.stats_footer_pdf
import projecttrack.composeapp.generated.resources.stats_issue_comments_logo
import projecttrack.composeapp.generated.resources.stats_issue_dislike_logo
import projecttrack.composeapp.generated.resources.stats_issue_like_logo
import projecttrack.composeapp.generated.resources.spbu_logo
import projecttrack.composeapp.generated.resources.stats_tooltip_close
import projecttrack.composeapp.generated.resources.stats_sort_asc
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.clip
import coil3.compose.AsyncImage
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.IntSize

private val DetailCardShape = RoundedCornerShape(10.dp)
private val DetailControlShape = RoundedCornerShape(5.dp)
private val DetailAccentGradient = Brush.verticalGradient(
    colors = listOf(AppColors.GradientStart, AppColors.GradientEndAlt)
)
private const val AllParticipantsId = "__all__"

private enum class IssueStateFilter(
    val key: String,
) {
    All("all"),
    Open("open"),
    Closed("closed");

    companion object {
        fun fromKey(key: String?): IssueStateFilter {
            return entries.firstOrNull { it.key == key } ?: All
        }
    }
}

@Composable
private fun issueStateFilterLabel(filter: IssueStateFilter): String = when (filter) {
    IssueStateFilter.All -> localizedString("Все", "All")
    IssueStateFilter.Open -> localizedString("Открытые", "Open")
    IssueStateFilter.Closed -> localizedString("Закрытые", "Closed")
}

@Composable
private fun issueStateFilterOptions(): List<Pair<String, String>> = listOf(
    IssueStateFilter.All.key to issueStateFilterLabel(IssueStateFilter.All),
    IssueStateFilter.Open.key to issueStateFilterLabel(IssueStateFilter.Open),
    IssueStateFilter.Closed.key to issueStateFilterLabel(IssueStateFilter.Closed),
)

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
    onExportPdfClick: (String?) -> Unit,
    onExportExcelClick: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val logTag = "StatsDetailScreen"
    val palette = appPalette()
    var showDateRangePicker by remember { mutableStateOf(false) }
    var showAllIssues by rememberSaveable(section.id) { mutableStateOf(false) }
    var showAllPullRequests by rememberSaveable(section.id) { mutableStateOf(false) }
    var showAllFiles by rememberSaveable(section.id) { mutableStateOf(false) }
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
    val issueListForOverlay = remember(section, details, effectiveParticipantId) {
        if (section == StatsScreenSection.Issues) filterIssues(details, effectiveParticipantId)
        else emptyList()
    }
    val fileListForOverlay = remember(section, details, effectiveParticipantId) {
        if (section == StatsScreenSection.CodeChurn) {
            val commits = filterCommits(details, effectiveParticipantId)
            buildFileAggregates(commits)
        } else emptyList()
    }
    val pullRequestListForOverlay = remember(section, details, effectiveParticipantId, rapidThreshold.totalMinutes) {
        when (section) {
            StatsScreenSection.PullRequests -> filterPullRequests(details, effectiveParticipantId)
            StatsScreenSection.RapidPullRequests -> filterPullRequests(details, effectiveParticipantId)
                .filter { isRapidPullRequest(it, rapidThreshold.totalMinutes) }
            else -> emptyList()
        }
    }
    val pullRequestOverlayTitle = when (section) {
        StatsScreenSection.RapidPullRequests -> localizedString(
            "Все быстрые Pull Requests",
            "All rapid pull requests",
        )
        else -> StatsDetailCopy.allPullRequestsDefaultTitle()
    }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var topBarHeight by remember { mutableStateOf(0.dp) }
    val topContentPadding = maxOf(topBarHeight, StatsTopBarTotalHeight) + 8.dp
    val dispatchBack = rememberStatsBackDispatcher("$logTag:${section.id}") {
        "issuesOverlay=$showAllIssues pullRequestsOverlay=$showAllPullRequests filesOverlay=$showAllFiles participant=${effectiveParticipantId ?: "all"}"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
    ) {
        Image(
            painter = painterResource(Res.drawable.spbu_logo),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            contentScale = ContentScale.Fit,
            alpha = palette.spbuBackdropLogoAlpha,
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .lazyListEdgeFadeMask(
                    listState = listState,
                    topInset = topContentPadding,
                ),
            contentPadding = PaddingValues(
                start = 21.dp,
                top = topContentPadding,
                end = 21.dp,
                bottom = 20.dp,
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
                        title = localizedString("Выбор участника", "Choose participant"),
                        value = details.participants.firstOrNull { it.id == selectedParticipantId }?.name
                            ?: localizedString("Вся команда", "Whole team"),
                        options = buildList {
                            val wholeTeam = localizedString("Вся команда", "Whole team")
                            add(AllParticipantsId to wholeTeam)
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
                    onShowAllClick = {
                        AppLog.d(logTag, "openIssuesOverlay section=${section.id}")
                        showAllIssues = true
                    },
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
                    onShowAllClick = {
                        AppLog.d(logTag, "openPullRequestsOverlay section=${section.id}")
                        showAllPullRequests = true
                    },
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
                    onShowAllClick = {
                        AppLog.d(logTag, "openPullRequestsOverlay section=${section.id}")
                        showAllPullRequests = true
                    },
                )

                StatsScreenSection.CodeChurn -> codeChurnItems(
                    snapshots = participantSnapshots,
                    selectedSnapshot = selectedSnapshot,
                    peerSnapshots = peerSnapshots,
                    effectiveParticipantId = effectiveParticipantId,
                    overallRank = overallRank,
                    overallScore = overallScore,
                    details = details,
                    onShowAllFilesClick = {
                        AppLog.d(logTag, "openFilesOverlay section=${section.id}")
                        showAllFiles = true
                    },
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
                    onExportPdfClick = { onExportPdfClick(effectiveParticipantId) },
                    onExportExcelClick = { onExportExcelClick(effectiveParticipantId) },
                )
            }
        }

        StatsTopBar(
            title = detailTitle(section),
            onBackClick = { dispatchBack("detail_top_bar") { onBackClick() } },
            titleMaxLines = 2,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { topBarHeight = with(density) { it.height.toDp() } },
        )

        AnimatedVisibility(
            visible = showAllIssues && section == StatsScreenSection.Issues,
            enter = slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(tween(300)),
            exit = slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize(),
        ) {
            AllIssuesOverlay(
                issues = issueListForOverlay,
                onBackClick = {
                    dispatchBack("issues_overlay_top_bar") {
                        showAllIssues = false
                    }
                },
            )
        }

        AnimatedVisibility(
            visible = showAllPullRequests &&
                (section == StatsScreenSection.PullRequests || section == StatsScreenSection.RapidPullRequests),
            enter = slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(tween(300)),
            exit = slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize(),
        ) {
            AllPullRequestsOverlay(
                pullRequests = pullRequestListForOverlay,
                title = pullRequestOverlayTitle,
                onBackClick = {
                    dispatchBack("pull_requests_overlay_top_bar") {
                        showAllPullRequests = false
                    }
                },
            )
        }

        AnimatedVisibility(
            visible = showAllFiles && section == StatsScreenSection.CodeChurn,
            enter = slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(tween(300)),
            exit = slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize(),
        ) {
            AllFilesOverlay(
                files = fileListForOverlay,
                onBackClick = {
                    dispatchBack("files_overlay_top_bar") {
                        showAllFiles = false
                    }
                },
            )
        }
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
    // Divide by the full first-to-last-commit span, not just by active days, to avoid
    // reporting an "average per active day" as the overall daily average.
    val commitInstants = filteredCommits.mapNotNull { parseInstant(it.committedAtIso) }
    val spanDays = if (commitInstants.size < 2) {
        dailyCounts.size.coerceAtLeast(1)
    } else {
        val minMs = commitInstants.minOf { it.toEpochMilliseconds() }
        val maxMs = commitInstants.maxOf { it.toEpochMilliseconds() }
        ((maxMs - minMs) / (1000L * 60 * 60 * 24)).toInt() + 1
    }
    val averagePerDay = if (filteredCommits.isEmpty()) 0.0
    else filteredCommits.size.toDouble() / spanDays.toDouble()
    val maxPerDay = dailyCounts.maxOfOrNull { it.count } ?: 0
    // Missing days inside the span imply at least one zero-commit day.
    val minPerDay = if (dailyCounts.size < spanDays) 0
                   else dailyCounts.minOfOrNull { it.count } ?: 0
    val linePoints = buildLineDeltaPoints(filteredCommits)
    val contributorRows = snapshots.values
        .sortedByDescending { it.commitCount }
        .map {
            DetailTableRow(
                title = appendYouSuffix(it.participant.name, it.participant.isCurrentUser),
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
    val teamRank = resolveRank(
        overallRank = null,
        overallScope = false,
        specificScore = current?.commitsScore,
        peerScores = peerSnapshots.map { it.commitsScore },
    )

    item {
        DoubleMetricRow(
            leftValue = filteredCommits.size.toString(),
            leftCaption = StatsDetailCopy.commitsCount(filteredCommits.size),
            rightValue = rank?.toString() ?: "—",
            rightCaption = if (effectiveParticipantId == null) {
                StatsExportCopy.rankInRating()
            } else {
                StatsExportCopy.rankInTeam()
            },
        )
    }
    item {
        ChartCard(
            title = StatsDetailCopy.commitsChartTitle(),
            chartType = ProjectStatsChartType.Bars,
            points = buildChartPoints(filteredCommits.mapNotNull { it.committedAtIso }) {
                "$it ${StatsDetailCopy.commitsCount(it)}"
            },
            tooltipTitle = StatsExportCopy.commits(),
        )
    }
    item {
        SingleMetricCard(
            value = formatCompactNumber(averagePerDay),
            caption = StatsDetailCopy.avgCommitsPerDayCaption(),
        )
    }
    item {
        DoubleMetricRow(
            leftValue = "+$additions",
            leftCaption = StatsDetailCopy.linesAddedCaption(),
            rightValue = "-$deletions",
            rightCaption = StatsDetailCopy.linesRemovedCaption(),
        )
    }
    if (linePoints.isNotEmpty()) {
        item {
            DualLineChartCard(
                title = StatsDetailCopy.lineChangesChartTitle(),
                points = linePoints,
            )
        }
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SingleMetricCard(
                modifier = Modifier.weight(1f),
                value = maxPerDay.toString(),
                caption = StatsDetailCopy.maxCommitsPerDayCaption(),
            )
            SingleMetricCard(
                modifier = Modifier.weight(1f),
                value = minPerDay.toString(),
                caption = StatsDetailCopy.minCommitsPerDayCaption(),
            )
        }
    }
    item {
        DetailFilesCard(
            title = StatsDetailCopy.top10FilesTitle(),
            rows = topFiles,
        )
    }
    if (contributorRows.isNotEmpty()) {
        item {
            DetailTableCard(
                title = StatsDetailCopy.commitCountTableTitle(),
                headers = listOf(StatsDetailCopy.valueColumnHeader()),
                rows = contributorRows,
            )
        }
    }
    item {
        CommitEntityListCard(
            title = StatsExportCopy.commitListTitle(),
            commits = filteredCommits
                .sortedByDescending { parseInstant(it.committedAtIso)?.toEpochMilliseconds() ?: Long.MIN_VALUE }
                .take(20),
        )
    }
    item {
        DoubleMetricRow(
            leftValue = overallRank?.toString() ?: "—",
            leftCaption = StatsExportCopy.rankInRating(),
            rightValue = teamRank?.toString() ?: "—",
            rightCaption = StatsExportCopy.rankInTeam(),
        )
    }
    item {
        ScoreCard(
            score = score,
            title = StatsDetailCopy.commitScoreTitle(),
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
    onShowAllClick: () -> Unit,
) {
    val filteredIssues = filterIssues(details, effectiveParticipantId)
    val sortedIssues = sortIssuesByNewest(filteredIssues)
    val current = selectedSnapshot
    val openIssues = filteredIssues.count { it.closedAtIso.isNullOrBlank() }
    val closedIssues = filteredIssues.size - openIssues
    val issueProgress = if (filteredIssues.isEmpty()) 0f else closedIssues.toFloat() / filteredIssues.size.toFloat()
    val averageLifetime = formatDurationMinutesLabel(buildAverageLifetimeMinutes(filteredIssues.mapNotNull {
        durationMinutes(it.createdAtIso, it.closedAtIso)
    }))
    val issueRemainingText = when {
        filteredIssues.isEmpty() && effectiveParticipantId == null -> StatsDetailCopy.issueNoDetails()
        filteredIssues.isEmpty() -> StatsDetailCopy.issueNoActive()
        openIssues > 0 -> StatsDetailCopy.issueCloseMore(openIssues)
        else -> StatsDetailCopy.issuesAllClosed()
    }
    val currentUserParticipantId = details.participants.firstOrNull { it.isCurrentUser }?.id
    val creatorRows = buildIssueCreatorRows(filteredIssues, effectiveParticipantId, currentUserParticipantId)
    val assigneeRows = buildIssueAssigneeRows(filteredIssues, effectiveParticipantId, currentUserParticipantId)
    val participantRows = snapshots.values
        .sortedByDescending { it.issueCount }
        .map {
            val issueValue = if (it.issueCount == 0) "0"
                             else "${it.openIssueCount}/${it.closedIssueCount}"
            DetailTableRow(
                title = appendYouSuffix(it.participant.name, it.participant.isCurrentUser),
                values = listOf(issueValue),
                highlight = it.participant.id == effectiveParticipantId,
            )
        }
    val labelChips = buildLabelChips(filteredIssues)
    val score = resolveScore(
        overallScore = overallScore,
        overallScope = effectiveParticipantId == null,
        specificScore = current?.issueScore,
    )
    val teamRank = resolveRank(
        overallRank = null,
        overallScope = false,
        specificScore = current?.issueScore,
        peerScores = peerSnapshots.map { it.issueScore },
    )

    item {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SingleMetricCard(
                modifier = Modifier.weight(1f),
                value = filteredIssues.size.toString(),
                caption = StatsDetailCopy.issuesTotalCaption(),
            )
            SingleMetricCard(
                modifier = Modifier.weight(1f),
                value = averageLifetime,
                caption = StatsDetailCopy.issueAvgLifetimeCaption(),
            )
        }
    }
    item {
        DoubleMetricRow(
            leftValue = openIssues.toString(),
            leftCaption = StatsDetailCopy.openIssuesCaption(),
            rightValue = closedIssues.toString(),
            rightCaption = StatsDetailCopy.closedIssuesCaption(),
        )
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IssueProgressCard(
                progress = issueProgress,
                remainingText = issueRemainingText,
                modifier = Modifier.weight(1f),
            )
            SingleMetricCard(
                modifier = Modifier.weight(1f),
                value = overallRank?.toString() ?: "—",
                caption = StatsExportCopy.rankInRating(),
            )
        }
    }
    if (participantRows.isNotEmpty()) {
        item {
            DetailTableCard(
                title = StatsDetailCopy.assignedIssueCountTitle(),
                subtitle = StatsDetailCopy.openClosedSubtitle(),
                headers = emptyList(),
                rows = participantRows,
            )
        }
    }
    item {
        DetailIssueSection(
            title = StatsDetailCopy.issuesListTitle(),
            issues = sortedIssues.take(1),
            actionLabel = if (sortedIssues.isNotEmpty()) StatsDetailCopy.viewAll() else null,
            onActionClick = onShowAllClick,
        )
    }
    if (creatorRows.isNotEmpty()) {
        item {
            DetailPersonSection(
                title = StatsExportCopy.issueCreatorsTitle(),
                rows = creatorRows,
            )
        }
    }
    if (assigneeRows.isNotEmpty()) {
        item {
            DetailPersonSection(
                title = StatsExportCopy.issueAssigneesTitle(),
                rows = assigneeRows,
            )
        }
    }
    if (labelChips.isNotEmpty()) {
        item {
            DetailLabelSection(
                title = StatsExportCopy.labelsTitle(),
                chips = labelChips,
            )
        }
    }
    if (effectiveParticipantId != null) {
        item {
            SingleMetricCard(
                value = teamRank?.toString() ?: "—",
                caption = StatsExportCopy.rankInTeam(),
            )
        }
    }
    item {
        DetailCard {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val scoreText = score?.let(::formatScoreValue) ?: "—"
                Text(
                    text = scoreText,
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    lineHeight = 32.sp,
                    color = projectScoreColor(score),
                )
                Text(
                    text = StatsDetailCopy.issueScoreTitle(),
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    color = appPalette().primaryText,
                )
            }
        }
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
    onShowAllClick: () -> Unit,
) {
    val filteredPullRequests = filterPullRequests(details, effectiveParticipantId)
    val sortedPullRequests = sortPullRequestsByNewest(filteredPullRequests)
    val current = selectedSnapshot
    val lifetimeMinutes = filteredPullRequests.mapNotNull {
        durationMinutes(it.createdAtIso, it.effectiveEndAtIso)
    }
    val distributionSlices = buildPullRequestLifetimeSlices(lifetimeMinutes)
    val contributorRows = snapshots.values
        .sortedByDescending { it.pullRequestCount }
        .map {
            DetailTableRow(
                title = appendYouSuffix(it.participant.name, it.participant.isCurrentUser),
                values = listOf(it.pullRequestCount.toString()),
                highlight = it.participant.id == effectiveParticipantId,
            )
        }
    val score = resolveScore(
        overallScore = overallScore,
        overallScope = effectiveParticipantId == null,
        specificScore = current?.pullRequestScore,
    )
    val teamRank = resolveRank(
        overallRank = null,
        overallScope = false,
        specificScore = current?.pullRequestScore,
        peerScores = peerSnapshots.map { it.pullRequestScore },
    )
    val fastPullRequests = sortedPullRequests
        .filter { isRapidPullRequest(it, rapidThreshold.totalMinutes) }
        .sortedBy { durationMinutes(it.createdAtIso, it.effectiveEndAtIso) ?: Int.MAX_VALUE }
        .take(5)
    val slowPullRequests = sortedPullRequests
        .filter { durationMinutes(it.createdAtIso, it.effectiveEndAtIso) != null }
        .sortedByDescending { durationMinutes(it.createdAtIso, it.effectiveEndAtIso) ?: Int.MIN_VALUE }
        .take(5)
    val dominantLifetimeLabel = distributionSlices.maxByOrNull { it.value }?.label

    item {
        ChartCard(
            title = StatsDetailCopy.pullRequestsChartTitle(),
            chartType = ProjectStatsChartType.Line,
            points = buildChartPoints(filteredPullRequests.mapNotNull { it.createdAtIso }) {
                StatsDetailCopy.prChartHintCount(it)
            },
            tooltipTitle = StatsDetailCopy.pullRequestsChartTooltip(),
        )
    }
    item {
        DetailMetricStatementCard(
            value = formatDurationMinutesLabel(buildAverageLifetimeMinutes(lifetimeMinutes)),
            caption = StatsDetailCopy.prAvgLifetimeCaption(),
        )
    }
    item {
        DoubleMetricRow(
            leftValue = filteredPullRequests.size.toString(),
            leftCaption = StatsDetailCopy.totalPullRequestsCaption(),
            rightValue = overallRank?.toString() ?: "—",
            rightCaption = StatsExportCopy.rankInRating(),
        )
    }
    if (contributorRows.isNotEmpty()) {
        item {
            DetailTableCard(
                title = StatsDetailCopy.pullRequestCountTitle(),
                headers = emptyList(),
                rows = contributorRows,
            )
        }
    }
    item {
        DetailMetricStatementCard(
            value = formatDurationMinutesLabel(lifetimeMinutes.minOrNull()),
            caption = StatsDetailCopy.prMinLifetimeCaption(),
        )
    }
    item {
        DetailMetricStatementCard(
            value = formatDurationMinutesLabel(lifetimeMinutes.maxOrNull()),
            caption = StatsDetailCopy.prMaxLifetimeCaption(),
        )
    }
    if (distributionSlices.isNotEmpty()) {
        item {
            DetailPullRequestLifetimeSection(
                title = StatsDetailCopy.prLifetimeDistributionTitle(),
                slices = distributionSlices,
            )
        }
        dominantLifetimeLabel?.let { label ->
            item {
                DetailMetricStatementCard(
                    value = formatPullRequestLifetimeHighlightLabel(label),
                    caption = StatsDetailCopy.prMostRelevantLifetimeCaption(),
                )
            }
        }
    }
    item {
        DetailPullRequestSection(
            title = StatsDetailCopy.pullRequestsListTitle(),
            pullRequests = sortedPullRequests.take(1),
            actionLabel = if (sortedPullRequests.isNotEmpty()) StatsDetailCopy.viewAll() else null,
            onActionClick = onShowAllClick,
        )
    }
    if (fastPullRequests.isNotEmpty()) {
        item {
            DetailPullRequestCompactSection(
                title = StatsDetailCopy.top5FastestPr(),
                pullRequests = fastPullRequests,
            )
        }
    }
    if (slowPullRequests.isNotEmpty()) {
        item {
            DetailPullRequestCompactSection(
                title = StatsDetailCopy.top5SlowestPr(),
                pullRequests = slowPullRequests,
            )
        }
    }
    if (effectiveParticipantId != null) {
        item {
            DetailMetricStatementCard(
                value = teamRank?.toString() ?: "—",
                caption = StatsExportCopy.rankInTeam(),
            )
        }
    }
    item {
        ScoreCard(
            score = score,
            title = StatsDetailCopy.pullRequestScoreTitle(),
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
    onShowAllClick: () -> Unit,
) {
    val filteredPullRequests = filterPullRequests(details, effectiveParticipantId)
    val rapidPullRequests = filteredPullRequests.filter { isRapidPullRequest(it, rapidThreshold.totalMinutes) }
    val sortedRapidPullRequests = sortPullRequestsByNewest(rapidPullRequests)
    val current = selectedSnapshot
    val contributorRows = snapshots.values
        .sortedByDescending { it.rapidPullRequestCount }
        .map {
            DetailTableRow(
                title = appendYouSuffix(it.participant.name, it.participant.isCurrentUser),
                values = listOf(it.rapidPullRequestCount.toString()),
                highlight = it.participant.id == effectiveParticipantId,
            )
        }
    val leader = snapshots.values
        .filter { it.rapidPullRequestCount > 0 }
        .maxByOrNull { it.rapidPullRequestCount }
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
            title = StatsDetailCopy.rapidPrChartTitle(),
            chartType = ProjectStatsChartType.Bars,
            points = buildChartPoints(rapidPullRequests.mapNotNull { it.closedAtIso }) {
                StatsDetailCopy.rapidPrInChart(it)
            },
            tooltipTitle = StatsDetailCopy.rapidPrTooltip(),
        )
    }
    item {
        DoubleMetricRow(
            leftValue = rapidPullRequests.size.toString(),
            leftCaption = StatsDetailCopy.rapidPrCaption(),
            rightValue = rank?.toString() ?: "—",
            rightCaption = if (effectiveParticipantId == null) {
                StatsExportCopy.rankInRating()
            } else {
                StatsExportCopy.rankInTeam()
            },
        )
    }
    if (contributorRows.isNotEmpty()) {
        item {
            DetailTableCard(
                title = StatsDetailCopy.rapidPullRequestCountTitle(),
                headers = emptyList(),
                rows = contributorRows,
            )
        }
    }
    item {
        DetailPullRequestSection(
            title = StatsDetailCopy.rapidPullRequestsListTitle(),
            pullRequests = sortedRapidPullRequests.take(1),
            actionLabel = if (sortedRapidPullRequests.isNotEmpty()) StatsDetailCopy.viewAll() else null,
            onActionClick = onShowAllClick,
        )
    }
    item {
        DetailMetricStatementCard(
            value = percentLabel(rapidPullRequests.size, filteredPullRequests.size),
            caption = StatsDetailCopy.rapidPrShareCaption(),
        )
    }
    item {
        DetailMetricStatementCard(
            value = displayPersonName(leader?.participant?.name).ifBlank { "—" },
            caption = StatsDetailCopy.rapidPrLeaderCaption(),
        )
    }
    item {
        ScoreCard(
            score = score,
            title = StatsDetailCopy.rapidPrScoreTitle(),
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
    onShowAllFilesClick: () -> Unit = {},
) {
    val filteredCommits = filterCommits(details, effectiveParticipantId)
    val fileStats = buildFileAggregates(filteredCommits)
    val current = selectedSnapshot
    val participantRows = snapshots.values
        .sortedByDescending { it.fileStats.size }
        .map {
            DetailTableRow(
                title = appendYouSuffix(it.participant.name, it.participant.isCurrentUser),
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
            title = StatsDetailCopy.fileStatsTitle(),
            rows = fileStats.take(5),
            onShowAllClick = if (fileStats.size > 5) onShowAllFilesClick else null,
        )
    }
    item {
        DoubleMetricRow(
            leftValue = fileStats.size.toString(),
            leftCaption = StatsDetailCopy.filesChangedCaption(),
            rightValue = rank?.toString() ?: "—",
            rightCaption = if (effectiveParticipantId == null) {
                StatsExportCopy.rankInRating()
            } else {
                StatsExportCopy.rankInTeam()
            },
        )
    }
    if (participantRows.isNotEmpty()) {
        item {
            DetailTableCard(
                title = StatsDetailCopy.changedFilesCountTitle(),
                headers = listOf(StatsDetailCopy.countAbbrevHeader()),
                rows = participantRows,
                showHeaders = false,
            )
        }
    }
    if (churnSlices.isNotEmpty()) {
        item {
            Text(
                text = StatsDetailCopy.fileChangesDistributionText(),
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                color = appPalette().primaryText,
                modifier = Modifier.padding(horizontal = 0.dp),
            )
        }
        item {
            CodeChurnDonutCard(
                slices = churnSlices,
                highlightId = churnSlices.maxByOrNull { it.value }?.label,
            )
        }
    }
    item {
        FileNameMetricCard(
            fileName = fileStats.firstOrNull()?.fileName ?: StatsDetailCopy.noDataFallbackFile(),
        )
    }
    item {
        ScoreCard(
            score = score,
            title = StatsDetailCopy.churnScoreTitle(),
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
    val ownershipRows = buildOwnershipRows(snapshots)
    val totalLines = ownershipRows.sumOf { it.lines }
    val slices = ownershipRows.mapIndexed { index, row ->
        ProjectStatsDonutSliceUi(
            label = row.name,
            secondaryLabel = StatsDetailCopy.linesCountLabel(row.lines),
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
            DetailOwnershipDonutCard(
                slices = slices,
                rows = ownershipRows,
            )
        }
    }
    item {
        DetailMetricStatementCard(
            value = totalLines.toString(),
            caption = StatsDetailCopy.totalLinesCaption(),
        )
    }
    if (ownershipRows.isNotEmpty()) {
        item {
            DetailOwnershipParticipantsTableCard(
                rows = ownershipRows,
            )
        }
    }
    item {
        DetailMetricStatementCard(
            value = rank?.toString() ?: "—",
            caption = if (effectiveParticipantId == null) {
                StatsExportCopy.rankInRating()
            } else {
                StatsExportCopy.rankInTeam()
            },
        )
    }
    item {
        ScoreCard(
            score = score,
            title = StatsDetailCopy.ownershipScoreTitle(),
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
            secondaryLabel = "${item.count} ${StatsDetailCopy.actionsCount(item.count)}",
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
    val hasActivity = total > 0
    val dominantLabel = dominant?.label?.takeIf { hasActivity }?.uppercase()
        ?: StatsDetailCopy.noDataUpper()
    val leastActiveLabel = leastActive?.label?.takeIf { hasActivity }?.uppercase()
        ?: StatsDetailCopy.noDataUpper()

    if (slices.isNotEmpty()) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = StatsDetailCopy.activityByWeekdayTitle(),
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    color = appPalette().primaryText,
                )
                WeekDayDistributionCard(
                    slices = slices,
                    emptyText = StatsDetailCopy.noActivityYet(),
                )
            }
        }
    }
    item {
        DetailHighlightCard(
            modifier = Modifier.fillMaxWidth(),
            title = StatsDetailCopy.mostActiveWeekdayTitle(),
            value = dominantLabel,
        )
    }
    item {
        DetailHighlightCard(
            modifier = Modifier.fillMaxWidth(),
            title = StatsDetailCopy.leastActiveWeekdayTitle(),
            value = leastActiveLabel,
        )
    }
    item {
        SingleMetricCard(
            value = rank?.toString() ?: "—",
            caption = if (effectiveParticipantId == null) {
                StatsExportCopy.rankInRating()
            } else {
                StatsExportCopy.rankInTeam()
            },
        )
    }
    item {
        ScoreCard(
            score = score,
            title = StatsDetailCopy.dominantWeekdayScoreTitle(),
        )
    }
}

@Composable
private fun DetailHeader(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = appPalette()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = StatsDetailCopy.backContentDescription(),
            tint = palette.primaryText,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(24.dp)
                .clickable(onClick = onBackClick)
        )
        Text(
            text = title,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 28.sp,
            color = palette.accent,
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
    val palette = appPalette()
    DetailCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = StatsDetailCopy.rapidThresholdHeading(),
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = palette.primaryText,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailValueSelector(
                    label = StatsDetailCopy.dayShort(),
                    value = threshold.days,
                    values = (0..30).toList(),
                    onSelected = { onThresholdChanged(it, threshold.hours, threshold.minutes) },
                    modifier = Modifier.weight(1f),
                )
                DetailValueSelector(
                    label = StatsDetailCopy.hourShort(),
                    value = threshold.hours,
                    values = (0..23).toList(),
                    onSelected = { onThresholdChanged(threshold.days, it, threshold.minutes) },
                    modifier = Modifier.weight(1f),
                )
                DetailValueSelector(
                    label = StatsDetailCopy.minShort(),
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

private val DualLineAddColor = Color(0xFF27AE60)
private val DualLineDelColor = Color(0xFF9F2D20)
private val DualChartGridColor = Color(0xFFE3E3E6)
// Merge add/remove tooltip rows when both values are visually indistinguishable on the chart scale.
private const val DualLineMergeThreshold = 0.12f

@Composable
private fun DualLineChartCard(
    title: String,
    points: List<DetailLinePoint>,
    modifier: Modifier = Modifier,
) {
    val palette = appPalette()
    DetailCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = title,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = palette.primaryText,
            )
            if (points.isEmpty()) {
                Text(
                    text = StatsDetailCopy.noData(),
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = palette.primaryText,
                )
            } else {
                DualLineChart(points = points)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DualLineLegendItem(label = StatsDetailCopy.linesAddedLegend(), color = DualLineAddColor)
                Spacer(modifier = Modifier.width(20.dp))
                DualLineLegendItem(label = StatsDetailCopy.linesRemovedLegend(), color = DualLineDelColor)
            }
        }
    }
}

@Composable
private fun DualLineChart(points: List<DetailLinePoint>) {
    val density = LocalDensity.current

    val maxRawValue = remember(points) {
        (points.maxOfOrNull { maxOf(it.firstValue, it.secondValue) } ?: 1).coerceAtLeast(1)
    }
    val axisData = remember(maxRawValue) {
        val step = ceil(maxRawValue.toFloat() / 3f).toInt().coerceAtLeast(1)
        val axisMax = step * 3
        Pair(axisMax.toFloat(), listOf(axisMax, axisMax - step, axisMax - step * 2, 0))
    }
    val axisScaleMax = axisData.first
    val axisLabels = axisData.second

    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }
    var tooltipSize by remember { mutableStateOf(IntSize.Zero) }

    val chartHeight = 152.dp
    val axisLabelWidth = 24.dp
    val axisLabelGap = 4.dp
    val plotTopPadding = 10.dp
    val plotBottomPadding = 6.dp
    val hPadding = 0.dp
    val plotEndPadding = 6.dp
    val xAxisGap = 4.dp

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val totalWidth = maxWidth
        val plotStart = hPadding + axisLabelWidth + axisLabelGap
        val plotWidth = totalWidth - plotStart - plotEndPadding
        val plotHeight = chartHeight - plotTopPadding - plotBottomPadding
        val slotWidth = plotWidth / points.size.coerceAtLeast(1)

        val maxVisibleLabels = ((plotWidth / 64.dp).toInt()).coerceAtLeast(2)
        val labelStep = if (points.size <= maxVisibleLabels) 1
        else (points.size.toFloat() / maxVisibleLabels.toFloat()).roundToInt().coerceAtLeast(1)
        val visibleLabelIndices = remember(points.size, labelStep) {
            buildSet {
                var i = 0
                while (i < points.size) { add(i); i += labelStep }
                add(points.lastIndex)
            }
        }

        val addPositions = remember(points, plotStart, slotWidth, plotTopPadding, plotHeight, axisScaleMax) {
            points.mapIndexed { i, pt ->
                val x = (plotStart + slotWidth * i + slotWidth / 2).value
                val frac = (pt.firstValue.toFloat() / axisScaleMax).coerceIn(0f, 1f)
                val y = (plotTopPadding + plotHeight * (1f - frac)).value
                Offset(x, y)
            }
        }
        val delPositions = remember(points, plotStart, slotWidth, plotTopPadding, plotHeight, axisScaleMax) {
            points.mapIndexed { i, pt ->
                val x = (plotStart + slotWidth * i + slotWidth / 2).value
                val frac = (pt.secondValue.toFloat() / axisScaleMax).coerceIn(0f, 1f)
                val y = (plotTopPadding + plotHeight * (1f - frac)).value
                Offset(x, y)
            }
        }

        val tooltipWidth = with(density) { tooltipSize.width.toDp() }
        val tooltipHeight = with(density) { tooltipSize.height.toDp() }

        Column {
            Box(modifier = Modifier.fillMaxWidth().height(chartHeight)) {

                DualLineChartGrid(
                    axisLabels = axisLabels,
                    axisLabelWidth = axisLabelWidth,
                    axisLabelGap = axisLabelGap,
                    hPadding = hPadding,
                    plotStart = plotStart,
                    plotTopPadding = plotTopPadding,
                    plotBottomPadding = plotBottomPadding,
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(addPositions) {
                            detectTapGestures { tapOffset ->
                                val tapXDp = with(density) { tapOffset.x.toDp() }
                                val closest = addPositions.indices.minByOrNull { i ->
                                    abs((addPositions[i].x - tapXDp.value))
                                }
                                selectedIndex = if (selectedIndex == closest) null else closest
                            }
                        }
                ) {
                    if (addPositions.isEmpty()) return@Canvas

                    val addPath = buildCatmullRomPath(
                        addPositions.map { Offset(it.x.dp.toPx(), it.y.dp.toPx()) }
                    )
                    val delPath = buildCatmullRomPath(
                        delPositions.map { Offset(it.x.dp.toPx(), it.y.dp.toPx()) }
                    )

                    drawPath(addPath, DualLineAddColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                    drawPath(delPath, DualLineDelColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

                    addPositions.indices.forEach { i ->
                        val isSelected = i == selectedIndex
                        val r = if (isSelected) 5.5.dp.toPx() else 4.dp.toPx()
                        val addPx = Offset(addPositions[i].x.dp.toPx(), addPositions[i].y.dp.toPx())
                        val delPx = Offset(delPositions[i].x.dp.toPx(), delPositions[i].y.dp.toPx())
                        drawCircle(Color.White, r + 2.dp.toPx(), addPx)
                        drawCircle(DualLineAddColor, r, addPx)
                        drawCircle(Color.White, r + 2.dp.toPx(), delPx)
                        drawCircle(DualLineDelColor, r, delPx)
                    }
                }

                selectedIndex?.let { idx ->
                    val point = points[idx]
                    val px = addPositions[idx].x.dp
                    val addYDp = addPositions[idx].y.dp
                    val delYDp = delPositions[idx].y.dp

                    val isMerged = abs(point.firstValue - point.secondValue).toFloat() / axisScaleMax < DualLineMergeThreshold

                    val upperY = minOf(addYDp, delYDp)
                    val tooltipX = (px - tooltipWidth / 2)
                        .coerceIn(plotStart, totalWidth - tooltipWidth - hPadding)
                    val tooltipY = (upperY - tooltipHeight - 8.dp).coerceAtLeast(plotTopPadding)

                    Row(
                        modifier = Modifier
                            .offset(x = tooltipX, y = tooltipY)
                            .onSizeChanged { tooltipSize = it }
                            .background(appPalette().primaryText.copy(alpha = 0.92f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = point.label,
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = appPalette().buttonText,
                            )
                            Text(
                                text = StatsDetailCopy.tooltipAdded(point.firstValue),
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = DualLineAddColor,
                            )
                            Text(
                                text = StatsDetailCopy.tooltipRemoved(point.secondValue, isMerged),
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = Color(0xFFFF8A80),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null,
                                    onClick = { selectedIndex = null },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.stats_tooltip_close),
                                contentDescription = StatsDetailCopy.closeContentDescription(),
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(appPalette().buttonText),
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
            }

            val labelFixedWidth = 60.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = xAxisGap)
                    .height(26.dp),
            ) {
                visibleLabelIndices.forEach { i ->
                    val centerX = plotStart + slotWidth * i + slotWidth / 2
                    val labelX = (centerX - labelFixedWidth / 2)
                        .coerceAtLeast(0.dp)
                        .coerceAtMost((totalWidth - labelFixedWidth).coerceAtLeast(0.dp))
                    Text(
                        text = points[i].label,
                        fontFamily = AppFonts.OpenSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                        letterSpacing = 0.11.sp,
                        color = appPalette().primaryText,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .width(labelFixedWidth)
                            .offset(x = labelX),
                    )
                }
            }
        }
    }
}

@Composable
private fun DualLineChartGrid(
    axisLabels: List<Int>,
    axisLabelWidth: Dp,
    axisLabelGap: Dp,
    hPadding: Dp,
    plotStart: Dp,
    plotTopPadding: Dp,
    plotBottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val palette = appPalette()
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val plotHeight = maxHeight - plotTopPadding - plotBottomPadding
        val rowHeight = 16.dp
        val lastIndex = axisLabels.lastIndex.coerceAtLeast(1)

        axisLabels.forEachIndexed { index, label ->
            val lineY = plotTopPadding + plotHeight * (index / lastIndex.toFloat())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .padding(start = hPadding, end = hPadding + axisLabelWidth + axisLabelGap)
                    .offset(y = lineY - rowHeight / 2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatDualAxisLabel(label),
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    letterSpacing = 0.12.sp,
                    color = palette.primaryText,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.width(axisLabelWidth),
                )
                Spacer(modifier = Modifier.width(axisLabelGap))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (label == 0) 2.dp else 1.dp)
                        .background(
                            if (label == 0) palette.primaryText.copy(alpha = 0.4f)
                            else DualChartGridColor
                        )
                )
            }
        }

        Box(
            modifier = Modifier
                .offset(x = plotStart, y = plotTopPadding)
                .width(1.dp)
                .height(plotHeight)
                .background(DualChartGridColor)
        )
    }
}

@Composable
private fun DualLineLegendItem(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val palette = appPalette()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(2.dp)
                .background(color, RoundedCornerShape(50))
        )
        Text(
            text = label,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = palette.primaryText,
        )
    }
}

@Composable
private fun DetailFilesCard(
    title: String,
    rows: List<DetailFileAggregate>,
    modifier: Modifier = Modifier,
    onShowAllClick: (() -> Unit)? = null,
) {
    val palette = appPalette()
    DetailCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = palette.primaryText,
                )
                if (onShowAllClick != null) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val alpha by animateFloatAsState(
                        targetValue = if (isPressed) 0.5f else 1f,
                        animationSpec = tween(100),
                        label = "show_all_alpha"
                    )
                    Text(
                        text = StatsDetailCopy.viewAll(),
                        fontFamily = AppFonts.OpenSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = palette.primaryText.copy(alpha = alpha),
                        modifier = Modifier
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = onShowAllClick,
                            ),
                    )
                }
            }
            if (rows.isEmpty()) {
                Text(
                    text = StatsDetailCopy.noData(),
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = palette.primaryText,
                )
            } else {
                rows.forEachIndexed { index, row ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(
                                    text = breakableFileName(row.fileName),
                                    fontFamily = AppFonts.OpenSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = palette.primaryText,
                                )
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(color = DualLineAddColor)) {
                                            append("+${row.additions}")
                                        }
                                        withStyle(SpanStyle(color = DualLineDelColor)) {
                                            append("/-${row.deletions}")
                                        }
                                    },
                                    fontFamily = AppFonts.OpenSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(
                                            fontFamily = AppFonts.OpenSans,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.accent,
                                        )) {
                                            append("${row.changes}")
                                        }
                                        withStyle(SpanStyle(
                                            fontFamily = AppFonts.OpenSans,
                                            fontWeight = FontWeight.Normal,
                                            color = palette.primaryText,
                                        )) {
                                            append(" ${StatsDetailCopy.changesWord(row.changes)}")
                                        }
                                    },
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.End,
                                )
                                row.status?.takeIf { it.isNotBlank() }?.let { status ->
                                    Text(
                                        text = status,
                                        fontFamily = AppFonts.OpenSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = fileStatusColor(status),
                                        textAlign = TextAlign.End,
                                    )
                                }
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
private fun fileStatusColor(status: String): Color = when (status.lowercase()) {
    "added" -> DualLineAddColor
    "removed" -> DualLineDelColor
    "modified", "changed" -> Color(0xFFE59500)
    "renamed", "copied" -> Color(0xFF2980B9)
    else -> appPalette().primaryText
}

private fun buildFileStatusFilters(
    files: List<DetailFileAggregate>,
): List<String> {
    val knownOrder = listOf("added", "removed", "modified", "changed", "renamed", "copied")
    val knownStatuses = knownOrder.filter { status ->
        files.any { normalizeFileStatus(it.status) == status }
    }
    val extraStatuses = files
        .mapNotNull { normalizeFileStatus(it.status) }
        .filterNot { it in knownOrder }
        .distinct()
        .sorted()
    return buildList {
        add(StatsDetailCopy.allFilesInternalFilter())
        addAll(knownStatuses)
        addAll(extraStatuses)
    }
}

private fun fileStatusFilterLabel(key: String): String = when (key) {
    StatsDetailCopy.allFilesInternalFilter() -> StatsDetailCopy.fileStatusLabelAll()
    "added" -> StatsDetailCopy.fileStatusLabelAdded()
    "removed" -> StatsDetailCopy.fileStatusLabelRemoved()
    "modified", "changed" -> StatsDetailCopy.fileStatusLabelModified()
    "renamed" -> StatsDetailCopy.fileStatusLabelRenamed()
    "copied" -> StatsDetailCopy.fileStatusLabelCopied()
    else -> key
}

private fun normalizeFileStatus(status: String?): String? =
    status?.trim()?.lowercase()?.takeIf { it.isNotBlank() }

@Composable
private fun DetailTableCard(
    title: String,
    headers: List<String>,
    rows: List<DetailTableRow>,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showHeaders: Boolean = true,
) {
    val palette = appPalette()
    DetailCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = palette.primaryText,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontFamily = AppFonts.OpenSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = palette.primaryText,
                    )
                }
            }
            if (rows.isEmpty()) {
                Text(
                    text = StatsDetailCopy.noData(),
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = palette.primaryText,
                )
            } else {
                if (showHeaders && subtitle == null && headers.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = StatsDetailCopy.tableNameColumn(),
                            fontFamily = AppFonts.OpenSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = palette.primaryText,
                            modifier = Modifier.weight(1f),
                        )
                        headers.forEach { header ->
                            Text(
                                text = header,
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = palette.primaryText,
                                textAlign = TextAlign.End,
                                modifier = Modifier.widthIn(min = 72.dp),
                            )
                        }
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
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = if (row.highlight) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (row.highlight) palette.accent else palette.primaryText,
                                modifier = Modifier.weight(1f),
                            )
                            row.values.forEach { value ->
                                Text(
                                    text = value,
                                    fontFamily = AppFonts.OpenSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = palette.primaryText,
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

private fun authorInitials(name: String): String {
    val parts = name.trim().split(" ")
    return when {
        parts.size >= 2 -> "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}".uppercase()
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "?"
    }
}

private fun authorAvatarColor(name: String): Color {
    val palette = listOf(
        Color(0xFF4A90D9), Color(0xFF7B68EE), Color(0xFF2ECC71),
        Color(0xFFE67E22), Color(0xFF1ABC9C), Color(0xFFE74C3C),
        Color(0xFF9B59B6), Color(0xFF3498DB),
    )
    return palette[kotlin.math.abs(name.hashCode()) % palette.size]
}

@Composable
private fun CommitEntityListCard(
    title: String,
    commits: List<StatsDetailCommitUi>,
    modifier: Modifier = Modifier,
) {
    val palette = appPalette()
    var expandedIndices by remember { mutableStateOf(emptySet<Int>()) }

    DetailCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = title,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = palette.primaryText,
            )
            if (commits.isEmpty()) {
                Text(
                    text = StatsDetailCopy.noData(),
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = palette.primaryText,
                )
            } else {
                val uriHandler = LocalUriHandler.current
                commits.forEachIndexed { index, commit ->
                    val isExpanded = index in expandedIndices
                    val chevronAngle by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = tween(durationMillis = 250),
                        label = "chevron_$index",
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        val rowInteraction = remember { MutableInteractionSource() }
                        val rowPressed by rowInteraction.collectIsPressedAsState()
                        val rowScale by animateFloatAsState(
                            targetValue = if (rowPressed) 0.95f else 1f,
                            animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
                            label = "commit_press_$index",
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { scaleX = rowScale; scaleY = rowScale }
                                .clickable(
                                    interactionSource = rowInteraction,
                                    indication = null,
                                ) { expandedIndices = if (isExpanded) expandedIndices - index else expandedIndices + index },
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(authorAvatarColor(commit.authorName), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = authorInitials(commit.authorName),
                                    fontFamily = AppFonts.OpenSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = appPalette().buttonText,
                                )
                                commit.authorAvatarUrl?.let { url ->
                                    AsyncImage(
                                        model = url,
                                        contentDescription = commit.authorName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape),
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = commit.authorName,
                                        fontFamily = AppFonts.OpenSans,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = palette.primaryText,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            text = commit.committedAtLabel,
                                            fontFamily = AppFonts.OpenSans,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 12.sp,
                                            color = palette.primaryText,
                                        )
                                        Icon(
                                            imageVector = Icons.Outlined.KeyboardArrowDown,
                                            contentDescription = if (isExpanded) {
                                                StatsDetailCopy.collapseContentDescription()
                                            } else {
                                                StatsDetailCopy.expandContentDescription()
                                            },
                                            tint = palette.primaryText,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .graphicsLayer { rotationZ = chevronAngle },
                                        )
                                    }
                                }
                                Text(
                                    text = commit.message,
                                    fontFamily = AppFonts.OpenSans,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = palette.primaryText,
                                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = buildAnnotatedString {
                                            withStyle(SpanStyle(color = DualLineAddColor, fontFamily = AppFonts.OpenSans, fontWeight = FontWeight.Medium)) {
                                                append("+${commit.additions}")
                                            }
                                            withStyle(SpanStyle(color = palette.primaryText, fontFamily = AppFonts.OpenSans, fontWeight = FontWeight.Normal)) {
                                                append(" / ")
                                            }
                                            withStyle(SpanStyle(color = DualLineDelColor, fontFamily = AppFonts.OpenSans, fontWeight = FontWeight.Medium)) {
                                                append("-${commit.deletions}")
                                            }
                                            if (commit.files.isNotEmpty()) {
                                                withStyle(SpanStyle(color = palette.primaryText, fontFamily = AppFonts.OpenSans, fontWeight = FontWeight.Normal)) {
                                                    append("  ${commit.files.size} ${StatsDetailCopy.filesCount(commit.files.size)}")
                                                }
                                            }
                                        },
                                        fontSize = 12.sp,
                                    )
                                    commit.url?.let { url ->
                                        Text(
                                            text = StatsDetailCopy.linkLabel(),
                                            fontFamily = AppFonts.OpenSans,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 12.sp,
                                            color = palette.accent,
                                            style = androidx.compose.ui.text.TextStyle(
                                                textDecoration = TextDecoration.Underline,
                                            ),
                                            modifier = Modifier.clickable { uriHandler.openUri(url) },
                                        )
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                            exit = fadeOut(tween(150)) + shrinkVertically(tween(200)),
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                            ) {
                                if (commit.files.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF8F8FA), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = StatsDetailCopy.changedFilesHeading(),
                                            fontFamily = AppFonts.OpenSans,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = palette.primaryText,
                                        )
                                        commit.files.forEach { file ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top,
                                            ) {
                                                Column(
                                                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(1.dp),
                                                ) {
                                                    Text(
                                                        text = file.fileName.substringAfterLast('/'),
                                                        fontFamily = AppFonts.OpenSans,
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = 11.sp,
                                                        color = palette.primaryText,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    Text(
                                                        text = buildAnnotatedString {
                                                            withStyle(SpanStyle(color = DualLineAddColor)) { append("+${file.additions}") }
                                                            withStyle(SpanStyle(color = DualLineDelColor)) { append(" -${file.deletions}") }
                                                        },
                                                        fontSize = 10.sp,
                                                        fontFamily = AppFonts.OpenSans,
                                                        fontWeight = FontWeight.Normal,
                                                    )
                                                }
                                                file.status?.takeIf { it.isNotBlank() }?.let { status ->
                                                    Text(
                                                        text = status,
                                                        fontFamily = AppFonts.OpenSans,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        color = fileStatusColor(status),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = StatsDetailCopy.noFileData(),
                                        fontFamily = AppFonts.OpenSans,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = palette.primaryText,
                                    )
                                }
                            }
                        }

                        if (index < commits.lastIndex) {
                            Spacer(Modifier.height(8.dp))
                            DetailDivider()
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AllFilesOverlay(
    files: List<DetailFileAggregate>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = appPalette()
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    var searchText by remember { mutableStateOf("") }
    var sortAscending by remember { mutableStateOf(false) }
    var selectedStatusFilter by rememberSaveable { mutableStateOf(StatsDetailCopy.allFilesInternalFilter()) }
    var topBarHeight by remember { mutableStateOf(0.dp) }
    val topContentPadding = maxOf(topBarHeight, StatsTopBarTotalHeight) + 8.dp
    val statusFilters = remember(files) { buildFileStatusFilters(files) }

    val displayedFiles = remember(files, searchText, sortAscending, selectedStatusFilter) {
        files
            .let { list ->
                if (searchText.isBlank()) list
                else list.filter { matchesSearchQuery(searchText, it.fileName) }
            }
            .let { list ->
                if (selectedStatusFilter == StatsDetailCopy.allFilesInternalFilter()) list
                else list.filter { normalizeFileStatus(it.status) == selectedStatusFilter }
            }
            .let { list -> sortFileAggregates(list, ascending = sortAscending) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        val listState = rememberLazyListState()
        Image(
            painter = painterResource(Res.drawable.spbu_logo),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            contentScale = ContentScale.Fit,
            alpha = palette.spbuBackdropLogoAlpha,
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .lazyListEdgeFadeMask(
                    listState = listState,
                    topInset = topContentPadding,
                )
                .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) },
            contentPadding = PaddingValues(
                start = 21.dp,
                top = topContentPadding,
                end = 21.dp,
                bottom = 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "files_controls") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            DetailIssueSearchBar(
                                searchText = searchText,
                                onSearchTextChange = { searchText = it },
                                placeholder = StatsDetailCopy.filePathSearchPlaceholder(),
                            )
                        }
                        val sortInteraction = remember { MutableInteractionSource() }
                        val sortPressed by sortInteraction.collectIsPressedAsState()
                        val sortAlpha by animateFloatAsState(
                            targetValue = if (sortPressed) 0.5f else 1f,
                            animationSpec = tween(100),
                            label = "sort_alpha",
                        )
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = Color(0xFFFEFEFE),
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFE3E3E6),
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clickable(
                                    interactionSource = sortInteraction,
                                    indication = null,
                                ) { sortAscending = !sortAscending },
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.stats_sort_asc),
                                contentDescription = if (sortAscending) {
                                    StatsDetailCopy.sortAscendingCd()
                                } else {
                                    StatsDetailCopy.sortDescendingCd()
                                },
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer {
                                        scaleY = if (sortAscending) 1f else -1f
                                        alpha = sortAlpha
                                    },
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        statusFilters.forEach { filter ->
                            FileStatusFilterChip(
                                text = fileStatusFilterLabel(filter),
                                selected = selectedStatusFilter == filter,
                                onClick = { selectedStatusFilter = filter },
                            )
                        }
                    }
                }
            }

            if (displayedFiles.isEmpty()) {
                item {
                    DetailCard {
                        Text(
                            text = if (searchText.isBlank()) {
                                StatsDetailCopy.noData()
                            } else {
                                StatsDetailCopy.nothingFound()
                            },
                            fontFamily = AppFonts.OpenSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = palette.primaryText,
                        )
                    }
                }
            } else {
                item {
                    DetailFilesCard(
                        title = StatsDetailCopy.allFilesWithCount(displayedFiles.size),
                        rows = displayedFiles,
                    )
                }
            }
        }
        StatsTopBar(
            title = StatsDetailCopy.fileStatsTitle(),
            onBackClick = {
                focusManager.clearFocus()
                onBackClick()
            },
            titleMaxLines = 2,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { topBarHeight = with(density) { it.height.toDp() } },
        )
    }
}

@Composable
private fun AllIssuesOverlay(
    issues: List<StatsDetailIssueUi>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = appPalette()
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    var searchText by remember { mutableStateOf("") }
    var stateFilterKey by remember { mutableStateOf(IssueStateFilter.All.key) }
    var topBarHeight by remember { mutableStateOf(0.dp) }
    val topContentPadding = maxOf(topBarHeight, StatsTopBarTotalHeight) + 8.dp
    val stateFilter = IssueStateFilter.fromKey(stateFilterKey)
    val filteredIssues = remember(issues, searchText, stateFilter) {
        filterAllIssues(issues, searchText, stateFilter)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        val listState = rememberLazyListState()
        Image(
            painter = painterResource(Res.drawable.spbu_logo),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            contentScale = ContentScale.Fit,
            alpha = palette.spbuBackdropLogoAlpha,
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .lazyListEdgeFadeMask(
                    listState = listState,
                    topInset = topContentPadding,
                )
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                },
            contentPadding = PaddingValues(
                top = topContentPadding,
                bottom = 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "search_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 21.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DetailIssueSearchBar(
                        searchText = searchText,
                        onSearchTextChange = { searchText = it },
                    )
                    DetailDropdownSelector(
                        title = localizedString("Состояние", "State"),
                        value = issueStateFilterLabel(stateFilter),
                        options = issueStateFilterOptions(),
                        onSelected = { stateFilterKey = it ?: IssueStateFilter.All.key },
                        selectedKey = stateFilterKey,
                    )
                }
            }

            if (filteredIssues.isEmpty()) {
                item {
                    Box(Modifier.padding(horizontal = 21.dp)) {
                        DetailCard {
                            Text(
                                text = if (searchText.isBlank() && stateFilter == IssueStateFilter.All) {
                                    localizedString("Нет данных", "No data")
                                } else {
                                    localizedString("Ничего не найдено", "Nothing found")
                                },
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = appPalette().primaryText,
                            )
                        }
                    }
                }
            } else {
                items(
                    items = filteredIssues,
                    key = { issue -> issueStableKey(issue) },
                ) { issue ->
                    Box(Modifier.padding(horizontal = 21.dp)) {
                        DetailIssueCard(issue = issue)
                    }
                }
            }
        }
        StatsTopBar(
            title = StatsDetailCopy.allIssuesTitle(),
            onBackClick = {
                focusManager.clearFocus()
                onBackClick()
            },
            titleMaxLines = 2,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { topBarHeight = with(density) { it.height.toDp() } },
        )
    }
}

@Composable
private fun AllPullRequestsOverlay(
    pullRequests: List<StatsDetailPullRequestUi>,
    onBackClick: () -> Unit,
    title: String = StatsDetailCopy.allPullRequestsDefaultTitle(),
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    var searchText by remember { mutableStateOf("") }
    var stateFilterKey by remember { mutableStateOf(IssueStateFilter.All.key) }
    var topBarHeight by remember { mutableStateOf(0.dp) }
    val topContentPadding = maxOf(topBarHeight, StatsTopBarTotalHeight) + 8.dp
    val stateFilter = IssueStateFilter.fromKey(stateFilterKey)
    val filteredPullRequests = remember(pullRequests, searchText, stateFilter) {
        filterAllPullRequests(pullRequests, searchText, stateFilter)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(appPalette().background),
    ) {
        val listState = rememberLazyListState()
        Image(
            painter = painterResource(Res.drawable.spbu_logo),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            contentScale = ContentScale.Fit,
            alpha = appPalette().spbuBackdropLogoAlpha,
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .lazyListEdgeFadeMask(
                    listState = listState,
                    topInset = topContentPadding,
                )
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                },
            contentPadding = PaddingValues(
                top = topContentPadding,
                bottom = 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "search_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 21.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DetailIssueSearchBar(
                        searchText = searchText,
                        onSearchTextChange = { searchText = it },
                    )
                    DetailDropdownSelector(
                        title = localizedString("Состояние", "State"),
                        value = issueStateFilterLabel(stateFilter),
                        options = issueStateFilterOptions(),
                        onSelected = { stateFilterKey = it ?: IssueStateFilter.All.key },
                        selectedKey = stateFilterKey,
                    )
                }
            }

            if (filteredPullRequests.isEmpty()) {
                item {
                    Box(Modifier.padding(horizontal = 21.dp)) {
                        DetailCard {
                            Text(
                                text = if (searchText.isBlank() && stateFilter == IssueStateFilter.All) {
                                    localizedString("Нет данных", "No data")
                                } else {
                                    localizedString("Ничего не найдено", "Nothing found")
                                },
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = appPalette().primaryText,
                            )
                        }
                    }
                }
            } else {
                items(
                    items = filteredPullRequests,
                    key = { pullRequest -> pullRequestStableKey(pullRequest) },
                ) { pullRequest ->
                    Box(Modifier.padding(horizontal = 21.dp)) {
                        DetailPullRequestCard(pullRequest = pullRequest)
                    }
                }
            }
        }
        StatsTopBar(
            title = title,
            onBackClick = {
                focusManager.clearFocus()
                onBackClick()
            },
            titleMaxLines = 2,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { topBarHeight = with(density) { it.height.toDp() } },
        )
    }
}

@Composable
private fun DetailIssueSearchBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    placeholder: String = StatsDetailCopy.searchPlaceholder(),
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(
                color = Color(0xFFFEFEFE),
                shape = RoundedCornerShape(20.dp),
            )
            .border(
                width = 1.dp,
                color = Color(0xFFE3E3E6),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.search_icon),
            contentDescription = placeholder,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        BasicTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = appPalette().primaryText,
                letterSpacing = 0.16.sp,
            ),
            decorationBox = { innerTextField ->
                if (searchText.isBlank()) {
                    Text(
                        text = placeholder,
                        fontFamily = AppFonts.OpenSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = appPalette().subtleBorder,
                        letterSpacing = 0.16.sp,
                    )
                }
                innerTextField()
            },
        )
    }
}

@Composable
private fun DetailIssueSection(
    title: String,
    issues: List<StatsDetailIssueUi>,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = appPalette().primaryText,
            )
            if (actionLabel != null && onActionClick != null) {
                AnimatedClickableText(
                    text = actionLabel,
                    onClick = onActionClick,
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = appPalette().primaryText,
                )
            }
        }
        if (issues.isEmpty()) {
            DetailCard {
                Text(
                    text = StatsDetailCopy.noData(),
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = appPalette().primaryText,
                )
            }
        } else {
            issues.forEach { issue ->
                DetailIssueCard(issue = issue)
            }
        }
    }
}

@Composable
private fun DetailPullRequestSection(
    title: String,
    pullRequests: List<StatsDetailPullRequestUi>,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                color = appPalette().primaryText,
            )
            if (actionLabel != null && onActionClick != null) {
                AnimatedClickableText(
                    text = actionLabel,
                    onClick = onActionClick,
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 20.sp,
                    color = appPalette().primaryText,
                )
            }
        }

        if (pullRequests.isEmpty()) {
            DetailCard {
                Text(
                    text = StatsDetailCopy.noData(),
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = appPalette().primaryText,
                )
            }
        } else {
            pullRequests.forEach { pullRequest ->
                DetailPullRequestCard(pullRequest = pullRequest)
            }
        }
    }
}

@Composable
private fun DetailPullRequestCard(
    pullRequest: StatsDetailPullRequestUi,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val author = remember(pullRequest.authorId, pullRequest.authorName) {
        DetailIssueParticipantUi(
            id = pullRequest.authorId,
            name = pullRequest.authorName,
            avatarUrl = resolveGithubAvatarUrl(pullRequest.authorId, null),
        )
    }
    val assignees = remember(pullRequest.assigneeIds, pullRequest.assigneeNames) {
        pullRequest.assigneeNames.mapIndexed { index, name ->
            DetailIssueParticipantUi(
                id = pullRequest.assigneeIds.getOrNull(index),
                name = name,
                avatarUrl = resolveGithubAvatarUrl(pullRequest.assigneeIds.getOrNull(index), null),
            )
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = DetailCardShape,
        color = appPalette().buttonText,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = pullRequest.title,
                        fontFamily = AppFonts.OpenSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        color = appPalette().primaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    pullRequest.number?.let { number ->
                        Text(
                            text = "#$number",
                            fontFamily = AppFonts.OpenSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            color = appPalette().primaryText,
                        )
                    }
                }
                pullRequest.state?.takeIf { it.isNotBlank() }?.let { state ->
                    IssueBadge(
                        text = localizedIssueStateLabel(state),
                        backgroundColor = issueStatusBackgroundColor(state),
                        textColor = Color.White,
                    )
                }
            }

            DetailDivider()

            IssueParticipantsSection(
                title = StatsDetailCopy.creatorTitle(),
                participants = listOf(author),
            )
            IssueParticipantsSection(
                title = StatsDetailCopy.assigneesTitle(),
                participants = assignees,
                emptyText = StatsDetailCopy.notAssigned(),
            )

            DetailDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                IssueDateColumn(
                    title = StatsDetailCopy.createdAtTitle(),
                    value = pullRequest.createdAtLabel,
                    modifier = Modifier.weight(1f),
                )
                IssueDateColumn(
                    title = StatsDetailCopy.closedAtTitle(),
                    value = pullRequest.closedAtLabel ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }

            DetailDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // PR payloads have no reactions, so reuse the compact footer slots for the fields we do have.
                IssueFooterMetric(
                    icon = {
                        Image(
                            painter = painterResource(Res.drawable.stats_issue_comments_logo),
                            contentDescription = StatsDetailCopy.commentsCd(),
                            modifier = Modifier.size(15.dp),
                            contentScale = ContentScale.Fit,
                        )
                    },
                    value = formatIssueMetricValue(pullRequest.comments),
                )
                Spacer(modifier = Modifier.width(10.dp))
                IssueFooterMetric(
                    icon = {
                        Image(
                            painter = painterResource(Res.drawable.stats_issue_like_logo),
                            contentDescription = StatsDetailCopy.commitsCd(),
                            modifier = Modifier
                                .width(15.dp)
                                .height(17.dp),
                            contentScale = ContentScale.Fit,
                        )
                    },
                    value = formatIssueMetricValue(pullRequest.commitsCount),
                )
                Spacer(modifier = Modifier.width(10.dp))
                IssueFooterMetric(
                    icon = {
                        Image(
                            painter = painterResource(Res.drawable.stats_issue_dislike_logo),
                            contentDescription = StatsDetailCopy.changedFilesCd(),
                            modifier = Modifier
                                .width(15.dp)
                                .height(17.dp),
                            contentScale = ContentScale.Fit,
                        )
                    },
                    value = formatIssueMetricValue(pullRequest.changedFiles),
                )
                Spacer(modifier = Modifier.weight(1f))
                pullRequest.url?.let { url ->
                    AnimatedClickableText(
                        text = StatsDetailCopy.linkWithArrow(),
                        onClick = { uriHandler.openUri(url) },
                        fontFamily = AppFonts.OpenSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        color = appPalette().subtleBorder,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailIssueCard(
    issue: StatsDetailIssueUi,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val creator = remember(issue.creatorId, issue.creatorName, issue.creatorAvatarUrl) {
        DetailIssueParticipantUi(
            id = issue.creatorId,
            name = issue.creatorName,
            avatarUrl = resolveGithubAvatarUrl(issue.creatorId, issue.creatorAvatarUrl),
        )
    }
    val assignees = remember(issue.assigneeIds, issue.assigneeNames, issue.assigneeAvatarUrls) {
        issue.assigneeNames.mapIndexed { index, name ->
            DetailIssueParticipantUi(
                id = issue.assigneeIds.getOrNull(index),
                name = name,
                avatarUrl = resolveGithubAvatarUrl(
                    issue.assigneeIds.getOrNull(index),
                    issue.assigneeAvatarUrls.getOrNull(index),
                ),
            )
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = DetailCardShape,
        color = appPalette().buttonText,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = issue.title,
                        fontFamily = AppFonts.OpenSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        color = appPalette().primaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        issue.number?.let { number ->
                            Text(
                                text = "#$number",
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                color = appPalette().primaryText,
                            )
                        }
                    }
                    if (issue.labels.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            issue.labels.forEach { label ->
                                IssueBadge(
                                    text = label,
                                    backgroundColor = Color(0xFF209F31),
                                    textColor = Color.White,
                                )
                            }
                        }
                    }
                }
                issue.state?.takeIf { it.isNotBlank() }?.let { state ->
                    IssueBadge(
                        text = localizedIssueStateLabel(state),
                        backgroundColor = issueStatusBackgroundColor(state),
                        textColor = Color.White,
                    )
                }
            }

            DetailDivider()

            IssueParticipantsSection(
                title = StatsDetailCopy.creatorTitle(),
                participants = listOf(creator),
            )
            IssueParticipantsSection(
                title = StatsDetailCopy.assigneesTitle(),
                participants = assignees,
                emptyText = StatsDetailCopy.notAssigned(),
            )

            DetailDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                IssueDateColumn(
                    title = StatsDetailCopy.createdAtTitle(),
                    value = issue.createdAtLabel,
                    modifier = Modifier.weight(1f),
                )
                IssueDateColumn(
                    title = StatsDetailCopy.closedAtTitle(),
                    value = issue.closedAtLabel ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }

            DetailDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IssueFooterMetric(
                    icon = {
                        Image(
                            painter = painterResource(Res.drawable.stats_issue_comments_logo),
                            contentDescription = StatsDetailCopy.commentsCd(),
                            modifier = Modifier.size(15.dp),
                            contentScale = ContentScale.Fit,
                        )
                    },
                    value = formatIssueMetricValue(issue.comments),
                )
                Spacer(modifier = Modifier.width(10.dp))
                IssueFooterMetric(
                    icon = {
                        Image(
                            painter = painterResource(Res.drawable.stats_issue_like_logo),
                            contentDescription = StatsDetailCopy.reactionsPositiveCd(),
                            modifier = Modifier
                                .width(15.dp)
                                .height(17.dp),
                            contentScale = ContentScale.Fit,
                        )
                    },
                    value = formatIssueMetricValue(issue.thumbsUpCount),
                )
                Spacer(modifier = Modifier.width(10.dp))
                IssueFooterMetric(
                    icon = {
                        Image(
                            painter = painterResource(Res.drawable.stats_issue_dislike_logo),
                            contentDescription = StatsDetailCopy.reactionsNegativeCd(),
                            modifier = Modifier
                                .width(15.dp)
                                .height(17.dp),
                            contentScale = ContentScale.Fit,
                        )
                    },
                    value = formatIssueMetricValue(issue.thumbsDownCount),
                )
                Spacer(modifier = Modifier.weight(1f))
                issue.url?.let { url ->
                    AnimatedClickableText(
                        text = StatsDetailCopy.linkWithArrow(),
                        onClick = { uriHandler.openUri(url) },
                        fontFamily = AppFonts.OpenSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        color = appPalette().subtleBorder,
                    )
                }
            }
        }
    }
}

@Composable
private fun IssueBadge(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(5.dp),
        color = backgroundColor,
    ) {
        Text(
            text = text,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun IssueParticipantsSection(
    title: String,
    participants: List<DetailIssueParticipantUi>,
    modifier: Modifier = Modifier,
    emptyText: String = StatsDetailCopy.noData(),
) {
    val visibleParticipants = participants.filter { it.name.isNotBlank() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = appPalette().primaryText,
        )
        if (visibleParticipants.isEmpty()) {
            Text(
                text = emptyText,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = appPalette().subtleBorder,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 2,
            ) {
                visibleParticipants.forEach { participant ->
                    IssueParticipantItem(
                        participant = participant,
                        modifier = if (visibleParticipants.size == 1) {
                            Modifier.fillMaxWidth()
                        } else {
                            Modifier.widthIn(min = 156.dp, max = 168.dp)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun IssueParticipantItem(
    participant: DetailIssueParticipantUi,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IssueParticipantAvatar(
            name = participant.name,
            avatarUrl = participant.avatarUrl,
        )
        Text(
            text = participant.name,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            color = appPalette().primaryText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun IssueDateColumn(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = appPalette().primaryText,
        )
        Text(
            text = value,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 20.sp,
            color = appPalette().primaryText,
        )
    }
}

@Composable
private fun IssueFooterMetric(
    icon: @Composable () -> Unit,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        icon()
        Text(
            text = value,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = appPalette().primaryText,
        )
    }
}

@Composable
private fun IssueParticipantAvatar(
    name: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(authorAvatarColor(name), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = authorInitials(name),
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = appPalette().buttonText,
        )
        avatarUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
            )
        }
    }
}

@Composable
private fun DetailPullRequestCompactSection(
    title: String,
    pullRequests: List<StatsDetailPullRequestUi>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            color = appPalette().primaryText,
        )
        pullRequests.forEach { pullRequest ->
            DetailPullRequestCompactCard(pullRequest = pullRequest)
        }
    }
}

@Composable
private fun DetailPullRequestCompactCard(
    pullRequest: StatsDetailPullRequestUi,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 620f),
        label = "pull_request_compact_press",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                enabled = pullRequest.url != null,
                interactionSource = interactionSource,
                indication = null,
                onClick = { pullRequest.url?.let(uriHandler::openUri) },
            ),
        shape = RoundedCornerShape(5.dp),
        color = appPalette().buttonText,
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            pullRequest.number?.let { number ->
                Text(
                    text = "#$number",
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = appPalette().primaryText,
                )
            }
            Text(
                text = pullRequest.title,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                color = appPalette().primaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = StatsDetailCopy.closedInDuration(
                    formatDurationMinutesLabel(
                        durationMinutes(pullRequest.createdAtIso, pullRequest.effectiveEndAtIso),
                    ),
                ),
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = appPalette().primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AnimatedClickableText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontFamily: androidx.compose.ui.text.font.FontFamily = AppFonts.OpenSans,
    fontWeight: FontWeight = FontWeight.Normal,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    lineHeight: androidx.compose.ui.unit.TextUnit = fontSize,
    color: Color = AppColors.Color2,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 500f),
        label = "issue_click_scale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.74f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "issue_click_alpha",
    )

    Text(
        text = text,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        fontSize = fontSize,
        lineHeight = lineHeight,
        color = color,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    )
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
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = appPalette().primaryText,
            )
            if (items.isEmpty()) {
                Text(
                    text = StatsDetailCopy.noData(),
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = appPalette().primaryText,
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
                                    fontFamily = AppFonts.OpenSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = appPalette().primaryText,
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
                                            fontFamily = AppFonts.OpenSans,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = appPalette().primaryText,
                                        )
                                    }
                                }
                            }
                            item.trailing?.let { trailing ->
                                Text(
                                    text = trailing,
                                    fontFamily = AppFonts.OpenSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = appPalette().accent,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.padding(start = 12.dp),
                                )
                            }
                        }
                        item.metaLines.forEach { line ->
                            Text(
                                text = line,
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = appPalette().primaryText,
                            )
                        }
                        item.link?.let { link ->
                            Text(
                                text = item.linkLabel ?: link,
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = appPalette().accent,
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
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = appPalette().primaryText,
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
private fun DetailPersonSection(
    title: String,
    rows: List<DetailPersonRow>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = appPalette().primaryText,
        )
        rows.forEach { row ->
            DetailPersonCard(row = row)
        }
    }
}

@Composable
private fun DetailPersonCard(
    row: DetailPersonRow,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(5.dp),
        color = appPalette().buttonText,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(authorAvatarColor(row.name), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = authorInitials(row.name),
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = appPalette().buttonText,
                )
                row.avatarUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = row.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                    )
                }
            }
            Text(
                text = row.name,
                fontFamily = AppFonts.OpenSans,
                fontWeight = if (row.isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                color = appPalette().primaryText,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = row.value,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = appPalette().primaryText,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun DetailLabelSection(
    title: String,
    chips: List<DetailLabelChipUi>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = appPalette().primaryText,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            chips.forEach { chip ->
                DetailLabelBadge(chip = chip)
            }
        }
    }
}

@Composable
private fun DetailLabelBadge(
    chip: DetailLabelChipUi,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(5.dp),
            color = Color(0xFF209F31),
        ) {
            Text(
                text = chip.label,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = appPalette().buttonText,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
        Text(
            text = StatsDetailCopy.issueLabelCount(chip.count),
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = appPalette().primaryText,
        )
    }
}

@Composable
private fun DetailOwnershipDonutCard(
    slices: List<ProjectStatsDonutSliceUi>,
    rows: List<DetailOwnershipRow>,
    modifier: Modifier = Modifier,
) {
    DetailCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            DonutChart(slices = slices)
            rows.forEachIndexed { index, row ->
                val slice = slices.getOrNull(index) ?: return@forEachIndexed
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(slice.colorHex), CircleShape)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = buildAnnotatedString {
                                append(row.name)
                                if (row.isCurrentUser) {
                                    append(" ")
                                    withStyle(
                                        SpanStyle(
                                            color = appPalette().accent,
                                            fontFamily = AppFonts.OpenSans,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    ) {
                                        append(StatsDetailCopy.youInParens())
                                    }
                                }
                            },
                            fontFamily = AppFonts.OpenSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = appPalette().primaryText,
                        )
                        Text(
                            text = StatsDetailCopy.linesCountLabel(row.lines),
                            fontFamily = AppFonts.OpenSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            lineHeight = 20.sp,
                            color = appPalette().primaryText,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeChurnDonutCard(
    slices: List<ProjectStatsDonutSliceUi>,
    highlightId: String?,
    modifier: Modifier = Modifier,
) {
    DetailCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            DonutChart(slices = slices)
            slices.forEach { slice ->
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
                            withStyle(SpanStyle(
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = FontWeight.SemiBold,
                                color = appPalette().primaryText,
                            )) {
                                append(slice.label)
                            }
                            withStyle(SpanStyle(
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = FontWeight.Normal,
                                color = appPalette().primaryText,
                            )) {
                                append(" - ")
                            }
                            withStyle(SpanStyle(
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = FontWeight.Bold,
                                color = appPalette().accent,
                            )) {
                                append(slice.secondaryLabel.substringBefore(' '))
                            }
                            withStyle(SpanStyle(
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = FontWeight.Normal,
                                color = appPalette().primaryText,
                            )) {
                                append(" " + slice.secondaryLabel.substringAfter(' '))
                            }
                        },
                        fontSize = 13.sp,
                        color = appPalette().primaryText,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailMetricStatementCard(
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    valueColor: Color = AppColors.Color3,
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

    DetailCard(
        modifier = modifier.height(CompactStatsCardHeight),
        contentPadding = PaddingValues(horizontal = MetricCardHorizontalPadding, vertical = 0.dp),
    ) {
        EqualVerticalMetricLayout(
            modifier = Modifier.fillMaxSize(),
            value = {
                Text(
                    text = value,
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = valueFontSize,
                    lineHeight = valueLineHeight,
                    color = valueColor,
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            },
        )
    }
}

@Composable
private fun DetailOwnershipParticipantsTableCard(
    rows: List<DetailOwnershipRow>,
    modifier: Modifier = Modifier,
) {
    DetailCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = StatsDetailCopy.participantsTableTitle(),
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                color = appPalette().primaryText,
            )
            if (rows.isEmpty()) {
                Text(
                    text = StatsDetailCopy.noData(),
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = appPalette().primaryText,
                )
            } else {
                DetailDivider()
                rows.forEachIndexed { index, row ->
                    DetailOwnershipParticipantRow(row = row)
                    if (index < rows.lastIndex) {
                        DetailDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailOwnershipParticipantRow(
    row: DetailOwnershipRow,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = row.name,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = appPalette().primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = DualLineAddColor,
                            fontFamily = AppFonts.OpenSans,
                            fontWeight = FontWeight.Medium,
                        )
                    ) {
                        append("+${row.additions}")
                    }
                    withStyle(
                        SpanStyle(
                            color = appPalette().primaryText,
                            fontFamily = AppFonts.OpenSans,
                            fontWeight = FontWeight.Medium,
                        )
                    ) {
                        append("/")
                    }
                    withStyle(
                        SpanStyle(
                            color = DualLineDelColor,
                            fontFamily = AppFonts.OpenSans,
                            fontWeight = FontWeight.Medium,
                        )
                    ) {
                        append("-${row.deletions}")
                    }
                },
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = appPalette().accent,
                            fontFamily = AppFonts.OpenSans,
                            fontWeight = FontWeight.Bold,
                        )
                    ) {
                        append(row.changes.toString())
                    }
                    withStyle(
                        SpanStyle(
                            color = appPalette().primaryText,
                            fontFamily = AppFonts.OpenSans,
                            fontWeight = FontWeight.Medium,
                        )
                    ) {
                        append(" ${StatsDetailCopy.changesWord(row.changes)}")
                    }
                },
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.End,
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = appPalette().accent,
                            fontFamily = AppFonts.OpenSans,
                            fontWeight = FontWeight.Bold,
                        )
                    ) {
                        append(row.lines.toString())
                    }
                    withStyle(
                        SpanStyle(
                            color = appPalette().primaryText,
                            fontFamily = AppFonts.OpenSans,
                            fontWeight = FontWeight.Medium,
                        )
                    ) {
                        append(" ${StatsDetailCopy.linesWord(row.lines)}")
                    }
                },
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun FileNameMetricCard(
    fileName: String,
    modifier: Modifier = Modifier,
) {
    DetailCard(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = MetricCardHorizontalPadding, vertical = 14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = breakableFileName(fileName.uppercase()),
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                color = appPalette().accent,
                softWrap = true,
            )
            Text(
                text = StatsDetailCopy.mostChangedFileSubtitle(),
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                color = appPalette().primaryText,
            )
        }
    }
}

@Composable
private fun DetailPullRequestLifetimeSection(
    title: String,
    slices: List<ProjectStatsDonutSliceUi>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            color = appPalette().primaryText,
        )
        DetailCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DonutChart(slices = slices)
                slices.forEach { slice ->
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
                                withStyle(
                                    SpanStyle(
                                        fontFamily = AppFonts.OpenSans,
                                        fontWeight = FontWeight.SemiBold,
                                        color = appPalette().primaryText,
                                    )
                                ) {
                                    append(slice.label)
                                }
                                withStyle(
                                    SpanStyle(
                                        fontFamily = AppFonts.OpenSans,
                                        fontWeight = FontWeight.Normal,
                                        color = appPalette().primaryText,
                                    )
                                ) {
                                    append(" - ")
                                }
                                withStyle(
                                    SpanStyle(
                                        fontFamily = AppFonts.OpenSans,
                                        fontWeight = FontWeight.Bold,
                                        color = appPalette().accent,
                                    )
                                ) {
                                    append(slice.secondaryLabel.substringBefore(' '))
                                }
                                withStyle(
                                    SpanStyle(
                                        fontFamily = AppFonts.OpenSans,
                                        fontWeight = FontWeight.Normal,
                                        color = appPalette().primaryText,
                                    )
                                ) {
                                    append(StatsDetailCopy.donutPrSuffix())
                                }
                            },
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                    }
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
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 32.sp,
                color = appPalette().accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = title,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = appPalette().primaryText,
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
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        DetailActionRow(
            text = StatsDetailCopy.exportPdf(),
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
            text = StatsDetailCopy.exportExcel(),
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

@Composable
private fun DetailActionRow(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "export_press_scale",
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon()
        Text(
            text = text,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = appPalette().primaryText,
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
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = appPalette().primaryText,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun FileStatusFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(30.dp),
        color = if (selected) appPalette().accent else appPalette().surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) appPalette().accent else Color(0xFFE4E4E7),
        ),
    ) {
        Text(
            text = text,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = if (selected) appPalette().buttonText else appPalette().primaryText,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun DetailCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = DetailCardShape,
        color = appPalette().buttonText,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
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
        StatsScreenSection.Commits -> StatsDetailCopy.detailSectionTitleCommits()
        StatsScreenSection.Issues -> StatsDetailCopy.detailSectionTitleIssues()
        StatsScreenSection.PullRequests -> StatsDetailCopy.detailSectionTitlePullRequests()
        StatsScreenSection.RapidPullRequests -> StatsDetailCopy.detailSectionTitleRapidPr()
        StatsScreenSection.CodeChurn -> StatsDetailCopy.detailSectionTitleRefactoring()
        StatsScreenSection.CodeOwnership -> StatsDetailCopy.detailSectionTitleOwnership()
        StatsScreenSection.DominantWeekDay -> StatsDetailCopy.detailSectionTitleDominantDay()
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
    return details.issues.filterByParticipant(participantId)
}

private fun sortIssuesByNewest(
    issues: List<StatsDetailIssueUi>,
): List<StatsDetailIssueUi> {
    return issues
        .distinctBy(::issueStableKey)
        .sortedByDescending { parseInstant(it.createdAtIso)?.toEpochMilliseconds() ?: Long.MIN_VALUE }
}

private fun issueStableKey(issue: StatsDetailIssueUi): String {
    val number = issue.number
    if (number != null) return "number:$number"
    val url = issue.url?.trim()?.lowercase()
    if (!url.isNullOrBlank()) return "url:$url"
    return buildString {
        append(issue.creatorId ?: issue.creatorName.lowercase())
        append(':')
        append(issue.title.trim().lowercase())
        append(':')
        append(issue.createdAtIso ?: "")
    }
}
@Composable
private fun issueStatusBackgroundColor(state: String): Color {
    return when (state.trim().lowercase()) {
        "closed" -> appPalette().accent
        "open" -> Color(0xFF209F31)
        else -> appPalette().primaryText
    }
}

private fun localizedIssueStateLabel(state: String): String = when (state.trim().lowercase()) {
    "open" -> StatsDetailCopy.issueStateOpen()
    "closed" -> StatsDetailCopy.issueStateClosed()
    "merged" -> StatsDetailCopy.issueStateMerged()
    "draft" -> StatsDetailCopy.issueStateDraft()
    else -> state.lowercase()
}

private fun resolveGithubAvatarUrl(
    userId: String?,
    avatarUrl: String?,
): String? {
    val normalizedAvatarUrl = avatarUrl?.trim()?.takeIf { it.isNotBlank() }
    if (normalizedAvatarUrl != null) return normalizedAvatarUrl
    val normalizedUserId = userId?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return "https://github.com/$normalizedUserId.png?size=80"
}

private fun filterAllIssues(
    issues: List<StatsDetailIssueUi>,
    searchText: String,
    stateFilter: IssueStateFilter,
): List<StatsDetailIssueUi> {
    return sortIssuesByNewest(issues).filter { issue ->
        issueMatchesStateFilter(issue, stateFilter) &&
            issueMatchesSearch(issue, searchText)
    }
}

private fun issueMatchesStateFilter(
    issue: StatsDetailIssueUi,
    stateFilter: IssueStateFilter,
): Boolean {
    return when (stateFilter) {
        IssueStateFilter.All -> true
        IssueStateFilter.Open -> issue.state?.trim()?.equals("open", ignoreCase = true) == true
        IssueStateFilter.Closed -> issue.state?.trim()?.equals("closed", ignoreCase = true) == true
    }
}

private fun issueMatchesSearch(
    issue: StatsDetailIssueUi,
    query: String,
): Boolean {
    val searchableTexts = buildList {
        add(issue.title)
        add(issue.creatorName)
        add(issue.state.orEmpty())
        addAll(issue.assigneeNames)
        addAll(issue.labels)
        issue.number?.let { add("#$it") }
        issue.number?.let { add(it.toString()) }
    }.toTypedArray()
    return matchesSearchQuery(query, *searchableTexts)
}

private fun formatIssueMetricValue(value: Int?): String = value?.toString() ?: "—"

private fun sortPullRequestsByNewest(
    pullRequests: List<StatsDetailPullRequestUi>,
): List<StatsDetailPullRequestUi> {
    return pullRequests
        .distinctBy(::pullRequestStableKey)
        .sortedByDescending { parseInstant(it.createdAtIso)?.toEpochMilliseconds() ?: Long.MIN_VALUE }
}

private fun pullRequestStableKey(
    pullRequest: StatsDetailPullRequestUi,
): String {
    val number = pullRequest.number
    if (number != null) return "number:$number"
    val url = pullRequest.url?.trim()?.lowercase()
    if (!url.isNullOrBlank()) return "url:$url"
    return buildString {
        append(pullRequest.authorId ?: pullRequest.authorName.lowercase())
        append(':')
        append(pullRequest.title.trim().lowercase())
        append(':')
        append(pullRequest.createdAtIso ?: "")
    }
}

private fun filterAllPullRequests(
    pullRequests: List<StatsDetailPullRequestUi>,
    searchText: String,
    stateFilter: IssueStateFilter,
): List<StatsDetailPullRequestUi> {
    return sortPullRequestsByNewest(pullRequests).filter { pullRequest ->
        pullRequestMatchesStateFilter(pullRequest, stateFilter) &&
            pullRequestMatchesSearch(pullRequest, searchText)
    }
}

private fun pullRequestMatchesStateFilter(
    pullRequest: StatsDetailPullRequestUi,
    stateFilter: IssueStateFilter,
): Boolean {
    return when (stateFilter) {
        IssueStateFilter.All -> true
        IssueStateFilter.Open -> pullRequest.state?.trim()?.equals("open", ignoreCase = true) == true
        IssueStateFilter.Closed -> pullRequest.state?.trim()?.equals("closed", ignoreCase = true) == true
    }
}

private fun pullRequestMatchesSearch(
    pullRequest: StatsDetailPullRequestUi,
    query: String,
): Boolean {
    val searchableTexts = buildList {
        add(pullRequest.title)
        add(pullRequest.authorName)
        add(pullRequest.state.orEmpty())
        addAll(pullRequest.assigneeNames)
        pullRequest.number?.let { add("#$it") }
        pullRequest.number?.let { add(it.toString()) }
    }.toTypedArray()
    return matchesSearchQuery(query, *searchableTexts)
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
                    subtitle = StatsExportCopy.participant(),
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
                    subtitle = StatsExportCopy.participant(),
                )
            }
        }
        issue.assigneeIds.forEachIndexed { index, id ->
            if (!participants.containsKey(id)) {
                participants[id] = StatsDetailParticipantUi(
                    id = id,
                    name = issue.assigneeNames.getOrNull(index) ?: id,
                    subtitle = StatsExportCopy.participant(),
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
                    subtitle = StatsExportCopy.participant(),
                )
            }
        }
    }

    // GitHub already defines `changes` as additions plus deletions, so summing them again would double count.
    val totalProjectLines = details.commits.sumOf { it.additions + it.deletions }

    return participants.mapValues { (_, participant) ->
        val userCommits = details.commits.filter { it.authorId == participant.id }
        val userIssues = details.issues.filterByParticipant(participant.id)
        val userPullRequests = details.pullRequests.filter { it.authorId == participant.id }
        val rapidPullRequests = userPullRequests.filter {
            isRapidPullRequest(it, rapidThresholdMinutes)
        }
        val fileStats = buildFileAggregates(userCommits)
        val weekDays = buildWeekDayStats(userCommits, userIssues, userPullRequests)
        val linesOwned = userCommits.sumOf { it.additions + it.deletions }
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
            openIssueCount = userIssues.count { it.closedAtIso.isNullOrBlank() },
            closedIssueCount = userIssues.count { !it.closedAtIso.isNullOrBlank() },
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
        .let { sortFileAggregates(it, ascending = false) }
}

private fun sortFileAggregates(
    files: List<DetailFileAggregate>,
    ascending: Boolean,
): List<DetailFileAggregate> {
    val comparator = if (ascending) {
        compareBy<DetailFileAggregate> { it.changes }
            .thenBy { it.fileName.lowercase() }
    } else {
        compareByDescending<DetailFileAggregate> { it.changes }
            .thenBy { it.fileName.lowercase() }
    }
    return files.sortedWith(comparator)
}

private fun buildWeekDayStats(
    commits: List<StatsDetailCommitUi>,
    issues: List<StatsDetailIssueUi>,
    pullRequests: List<StatsDetailPullRequestUi>,
): List<DetailWeekDayStat> {
    val labels = listOf(
        StatsDetailCopy.monday(),
        StatsDetailCopy.tuesday(),
        StatsDetailCopy.wednesday(),
        StatsDetailCopy.thursday(),
        StatsDetailCopy.friday(),
        StatsDetailCopy.saturday(),
        StatsDetailCopy.sunday(),
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
    // Store the epoch so grouped points can be sorted chronologically instead of by insertion order.
    data class ChartEntry(val label: String, val epochMs: Long, var count: Int)
    val grouped = linkedMapOf<String, ChartEntry>()
    dates.mapNotNull { parseInstant(it) }.forEach { instant ->
        val date = instant.toLocalDateTime(TimeZone.UTC).date
        val label = "${date.dayOfMonth.toString().padStart(2, '0')}.${date.monthNumber.toString().padStart(2, '0')}.${(date.year % 100).toString().padStart(2, '0')}"
        val prev = grouped[label]
        if (prev == null) {
            grouped[label] = ChartEntry(label, instant.toEpochMilliseconds(), 1)
        } else {
            prev.count++
        }
    }
    return grouped.values
        .sortedBy { it.epochMs }
        .map { entry ->
            ProjectStatsChartPointUi(
                label = entry.label,
                value = entry.count.toFloat(),
                valueLabel = entry.count.toString(),
                hint = hintFormatter(entry.count),
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
    // Keep the formatted label plus epoch millis so the aggregated series can still be sorted by date.
    data class Entry(val label: String, var additions: Int, var deletions: Int, val epochMs: Long)
    val grouped = linkedMapOf<String, Entry>()
    commits.forEach { commit ->
        val instant = parseInstant(commit.committedAtIso) ?: return@forEach
        val date = instant.toLocalDateTime(TimeZone.UTC).date
        val key = formatDate(date)
        val epochMs = instant.toEpochMilliseconds()
        val prev = grouped[key]
        if (prev == null) {
            grouped[key] = Entry(key, commit.additions, commit.deletions, epochMs)
        } else {
            prev.additions += commit.additions
            prev.deletions += commit.deletions
        }
    }
    return grouped.values
        .sortedBy { it.epochMs }
        .map { entry ->
            DetailLinePoint(
                label = entry.label,
                firstValue = entry.additions,
                secondValue = entry.deletions,
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
        StatsDetailCopy.prLifetimeBucket1() to durations.count { it < 60 },
        StatsDetailCopy.prLifetimeBucket2() to durations.count { it in 60 until 360 },
        StatsDetailCopy.prLifetimeBucket3() to durations.count { it in 360 until 1440 },
        StatsDetailCopy.prLifetimeBucket4() to durations.count { it in 1440 until 4320 },
        StatsDetailCopy.prLifetimeBucket5() to durations.count { it in 4320 until 10080 },
        StatsDetailCopy.prLifetimeBucket6() to durations.count { it in 10080 until 20160 },
        StatsDetailCopy.prLifetimeBucket7() to durations.count { it >= 20160 },
    ).filter { it.second > 0 }
    val total = durations.size
    return buckets.mapIndexed { index, (label, value) ->
        ProjectStatsDonutSliceUi(
            label = label,
            secondaryLabel = StatsDetailCopy.prCountInDonut(value),
            percentLabel = percentLabel(value, total),
            value = value.toFloat(),
            colorHex = weekdayPalette[index % weekdayPalette.size],
            highlight = value == buckets.maxOf { it.second },
        )
    }
}

private fun formatPullRequestLifetimeHighlightLabel(
    label: String,
): String {
    return label
        .replace("< ", "<")
        .replace("> ", ">")
        .uppercase()
}

private fun buildFileChurnSlices(
    fileStats: List<DetailFileAggregate>,
): List<ProjectStatsDonutSliceUi> {
    if (fileStats.isEmpty()) return emptyList()
    val buckets = listOf(
        StatsDetailCopy.fileChurnBucket1() to fileStats.count { it.changes == 1 },
        StatsDetailCopy.fileChurnBucket2() to fileStats.count { it.changes in 2..3 },
        StatsDetailCopy.fileChurnBucket3() to fileStats.count { it.changes in 4..5 },
        StatsDetailCopy.fileChurnBucket4() to fileStats.count { it.changes in 6..7 },
        StatsDetailCopy.fileChurnBucket5() to fileStats.count { it.changes in 8..10 },
        StatsDetailCopy.fileChurnBucket6() to fileStats.count { it.changes > 10 },
    ).filter { it.second > 0 }
    val total = buckets.sumOf { it.second }.takeIf { it > 0 } ?: 1
    return buckets.mapIndexed { index, (label, value) ->
        ProjectStatsDonutSliceUi(
            label = label,
            secondaryLabel = StatsDetailCopy.filesCountInDonut(value),
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
    currentUserParticipantId: String? = null,
): List<DetailPersonRow> {
    val counts = linkedMapOf<DetailIssueParticipantKey, Int>()
    val avatarUrls = linkedMapOf<DetailIssueParticipantKey, String?>()
    issues.forEach { issue ->
        val key = DetailIssueParticipantKey(id = issue.creatorId, name = issue.creatorName)
        counts[key] = (counts[key] ?: 0) + 1
        if (avatarUrls[key] == null) {
            avatarUrls[key] = resolveGithubAvatarUrl(issue.creatorId, issue.creatorAvatarUrl)
        }
    }
    return counts.entries
        .sortedWith(
            compareByDescending<Map.Entry<DetailIssueParticipantKey, Int>> { it.value }
                .thenBy { it.key.name.lowercase() }
        )
        .map { (key, count) ->
            DetailPersonRow(
                id = key.id,
                name = appendYouSuffix(key.name, key.id != null && key.id == currentUserParticipantId),
                avatarUrl = avatarUrls[key],
                value = StatsExportCopy.createdIssuesCount(count),
                isCurrentUser = key.id != null && key.id == currentUserParticipantId,
            )
        }
}

private fun buildIssueAssigneeRows(
    issues: List<StatsDetailIssueUi>,
    effectiveParticipantId: String?,
    currentUserParticipantId: String? = null,
): List<DetailPersonRow> {
    val closedCounts = linkedMapOf<DetailIssueParticipantKey, Int>()
    val avatarUrls = linkedMapOf<DetailIssueParticipantKey, String?>()
    issues.forEach { issue ->
        val isClosed = !issue.closedAtIso.isNullOrBlank()
        if (issue.assigneeIds.isEmpty()) {
            issue.assigneeNames.forEach { assigneeName ->
                val key = DetailIssueParticipantKey(id = null, name = assigneeName)
                if (isClosed) closedCounts[key] = (closedCounts[key] ?: 0) + 1
                else closedCounts.getOrPut(key) { 0 }
            }
        } else {
            issue.assigneeIds.forEachIndexed { index, assigneeId ->
                val assigneeName = issue.assigneeNames.getOrNull(index) ?: assigneeId
                val key = DetailIssueParticipantKey(id = assigneeId, name = assigneeName)
                if (isClosed) closedCounts[key] = (closedCounts[key] ?: 0) + 1
                else closedCounts.getOrPut(key) { 0 }
                if (avatarUrls[key] == null) {
                    avatarUrls[key] = resolveGithubAvatarUrl(
                        assigneeId,
                        issue.assigneeAvatarUrls.getOrNull(index),
                    )
                }
            }
        }
    }
    return closedCounts.entries
        .sortedWith(
            compareByDescending<Map.Entry<DetailIssueParticipantKey, Int>> { it.value }
                .thenBy { it.key.name.lowercase() }
        )
        .map { (key, closedCount) ->
            DetailPersonRow(
                id = key.id,
                name = appendYouSuffix(key.name, key.id != null && key.id == currentUserParticipantId),
                avatarUrl = avatarUrls[key],
                value = StatsExportCopy.closedIssuesCount(closedCount),
                isCurrentUser = key.id != null && key.id == currentUserParticipantId,
            )
        }
}

private fun buildLabelChips(issues: List<StatsDetailIssueUi>): List<DetailLabelChipUi> {
    val counts = linkedMapOf<String, Int>()
    issues.flatMap { it.labels }.forEach { label ->
        counts[label] = (counts[label] ?: 0) + 1
    }
    return counts.entries.sortedByDescending { it.value }.take(10)
        .map { DetailLabelChipUi(label = it.key, count = it.value) }
}

private fun buildOwnershipRows(
    snapshots: Map<String, DetailParticipantSnapshot>,
): List<DetailOwnershipRow> {
    return snapshots.values
        .sortedByDescending { it.linesOwned }
        .map { snapshot ->
            DetailOwnershipRow(
                id = snapshot.participant.id,
                name = snapshot.participant.name,
                isCurrentUser = snapshot.participant.isCurrentUser,
                additions = snapshot.commits.sumOf { it.additions },
                deletions = snapshot.commits.sumOf { it.deletions },
                changes = snapshot.commitCount,
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
    val closed = issues.count { !it.closedAtIso.isNullOrBlank() }
    return round2((closed.toDouble() / issues.size) * 3 + 2)
}

private fun pullRequestHangScore(pullRequests: List<StatsDetailPullRequestUi>): Double? {
    val durations = pullRequests.mapNotNull { durationMinutes(it.createdAtIso, it.effectiveEndAtIso) }
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
    val duration = durationMinutes(pullRequest.createdAtIso, pullRequest.effectiveEndAtIso) ?: return false
    return duration < thresholdMinutes
}

private fun durationMinutes(
    startIso: String?,
    endIso: String?,
): Int? {
    val start = parseInstant(startIso)?.toEpochMilliseconds() ?: return null
    val end = parseInstant(endIso)?.toEpochMilliseconds() ?: return null
    if (end <= start) return null
    val minutes = ((end - start) / MILLIS_PER_MINUTE).toInt()
    return maxOf(1, minutes)
}

private fun weekdayLabel(value: String?): String? {
    val instant = parseInstant(value) ?: return null
    return when (instant.toLocalDateTime(TimeZone.UTC).dayOfWeek.name.lowercase()) {
        "monday" -> StatsDetailCopy.monday()
        "tuesday" -> StatsDetailCopy.tuesday()
        "wednesday" -> StatsDetailCopy.wednesday()
        "thursday" -> StatsDetailCopy.thursday()
        "friday" -> StatsDetailCopy.friday()
        "saturday" -> StatsDetailCopy.saturday()
        "sunday" -> StatsDetailCopy.sunday()
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

/**
 * Catmull-Rom spline used for chart smoothing.
 * The control points are chosen so the curve still passes through every source point.
 */
private fun buildCatmullRomPath(pts: List<Offset>): Path {
    val path = Path()
    if (pts.isEmpty()) return path
    path.moveTo(pts[0].x, pts[0].y)
    if (pts.size < 2) return path
    if (pts.size == 2) {
        path.lineTo(pts[1].x, pts[1].y)
        return path
    }
    for (i in 0 until pts.size - 1) {
        val p0 = if (i > 0) pts[i - 1] else pts[i]
        val p1 = pts[i]
        val p2 = pts[i + 1]
        val p3 = if (i < pts.size - 2) pts[i + 2] else pts[i + 1]
        val cp1x = p1.x + (p2.x - p0.x) / 6f
        val cp1y = p1.y + (p2.y - p0.y) / 6f
        val cp2x = p2.x - (p3.x - p1.x) / 6f
        val cp2y = p2.y - (p3.y - p1.y) / 6f
        path.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
    }
    return path
}

/** Compact Y-axis label format: `1500 -> 1.5k`, `10000 -> 10k`. */
private fun formatDualAxisLabel(value: Int): String = when {
    value == 0 -> "0"
    value >= 10_000 -> "${value / 1000}k"
    value >= 1_000 -> {
        val tenths = (value % 1000) / 100
        if (tenths == 0) "${value / 1000}k" else "${value / 1000}.${tenths}k"
    }
    else -> value.toString()
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

private fun appendYouSuffix(
    name: String,
    highlight: Boolean,
): String {
    return if (highlight) "${name} (${StatsExportCopy.youMarker()})" else name
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

private fun breakableFileName(value: String): String = value.replace("/", "/\u200B")

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
    val linkLabel: String? = null,
)

private data class DetailIssueParticipantKey(
    val id: String?,
    val name: String,
)

private data class DetailIssueParticipantUi(
    val id: String?,
    val name: String,
    val avatarUrl: String?,
)

private data class DetailTableRow(
    val title: String,
    val values: List<String>,
    val highlight: Boolean = false,
)

private data class DetailOwnershipRow(
    val id: String,
    val name: String,
    val isCurrentUser: Boolean,
    val additions: Int,
    val deletions: Int,
    val changes: Int,
    val lines: Int,
)

private data class DetailPersonRow(
    val id: String?,
    val name: String,
    val avatarUrl: String? = null,
    val value: String,
    val isCurrentUser: Boolean = false,
)

private data class DetailLabelChipUi(
    val label: String,
    val count: Int,
)
