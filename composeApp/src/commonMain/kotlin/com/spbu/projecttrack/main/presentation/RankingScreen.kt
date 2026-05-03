@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.spbu.projecttrack.main.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.spbu.projecttrack.core.auth.AuthManager
import com.spbu.projecttrack.core.di.DependencyContainer
import com.spbu.projecttrack.core.time.PlatformTime
import com.spbu.projecttrack.core.theme.AppFonts
import com.spbu.projecttrack.rating.data.model.RankingData
import com.spbu.projecttrack.rating.data.model.RankingDateRangeFilter
import com.spbu.projecttrack.rating.data.model.RankingFilterTemplate
import com.spbu.projecttrack.rating.data.model.RankingFilters
import com.spbu.projecttrack.rating.data.model.RankingItem
import com.spbu.projecttrack.rating.data.model.RankingMetricFilter
import com.spbu.projecttrack.rating.data.model.RankingMetricKey
import com.spbu.projecttrack.rating.data.model.RankingPeriodPreset
import com.spbu.projecttrack.rating.data.model.RankingThresholdPreset
import com.spbu.projecttrack.rating.data.model.RankingWeekDay
import com.spbu.projecttrack.rating.data.model.rankingBuiltInTemplates
import com.spbu.projecttrack.rating.data.model.rankingDefaultFilters
import com.spbu.projecttrack.rating.presentation.RankingUiState
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import projecttrack.composeapp.generated.resources.Res
import projecttrack.composeapp.generated.resources.arrow_back
import projecttrack.composeapp.generated.resources.calendar_icon
import projecttrack.composeapp.generated.resources.close_icon
import projecttrack.composeapp.generated.resources.filter_icon
import projecttrack.composeapp.generated.resources.position_down
import projecttrack.composeapp.generated.resources.position_up
import projecttrack.composeapp.generated.resources.search_icon
import projecttrack.composeapp.generated.resources.spbu_logo
import projecttrack.composeapp.generated.resources.stats_sort_asc

private enum class RankingTab {
    Projects,
    Students,
}

private enum class RankingSortOrder {
    Descending,
    Ascending,
}

private enum class RankingMenuTarget {
    DateRange,
}

private data class RankingMetricInfo(
    val title: String,
    val whatShows: String,
    val whyNeeded: List<String>,
    val howToInterpret: List<String>,
    val note: String? = null,
)

private data class RankingDateRangePreset(
    val title: String,
    val filter: RankingDateRangeFilter,
)

private val RankingGray = Color(0xFF76767C)
private val RankingLightGray = Color(0xFFBDBDBD)
private val RankingDivider = Color(0xFFBDBDBD)
private val RankingRed = Color(0xFF9F2D20)
private val RankingRedLight = Color(0xFFCF3F2F)
private val RankingRedGradientBottom = Color(0xFF842318)
private val RankingGreen = Color(0xFF209F31)
private val RankingYellow = Color(0xFF9F9220)
private const val RankingPageSize = 10

/** Max vertical scale at full pull / refresh (stretch from top, same on all platforms). */
private const val RankingPullContentStretchMax = 0.022f

private val RankingPullIndicatorRestOffset = (-20).dp

private val RankingPullIndicatorTravel = 42.dp

/** Align filter/sort glyphs and chip LazyRow vertically (tap size matches icon raster). */
private val RankingFiltersToolbarHeight = 36.dp

private val RankingFilterLaneIconTap = 32.dp

/** Horizontal fade strip — same dp as LazyColumn top edge on project rows. */
private val RankingFiltersScrollFadeWidth = 32.dp

private fun rankingTabForPage(page: Int): RankingTab {
    return RankingTab.entries.getOrElse(page) { RankingTab.Projects }
}

@Composable
fun RankingScreen(
    onProjectClick: (String) -> Unit = {},
    onStudentClick: (String, String, String?) -> Unit = { _, _, _ -> },
    onRootDestinationChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isAuthorized by AuthManager.isAuthorized.collectAsState()
    val viewModel = remember { DependencyContainer.provideRankingViewModel() }
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val focusManager = LocalFocusManager.current
    val pagerState = rememberPagerState(
        initialPage = RankingTab.Projects.ordinal,
        pageCount = { RankingTab.entries.size },
    )
    val coroutineScope = rememberCoroutineScope()

    var searchText by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(RankingSortOrder.Descending) }
    var appliedFilters by remember { mutableStateOf(rankingDefaultFilters()) }
    var draftFilters by remember { mutableStateOf(appliedFilters) }
    var showFiltersScreen by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var selectedMetricInfo by remember { mutableStateOf<RankingMetricKey?>(null) }
    var customTemplates by remember { mutableStateOf<List<RankingFilterTemplate>>(emptyList()) }
    var selectedTemplateId by remember { mutableStateOf("none") }
    var menuMetricKey by remember { mutableStateOf<RankingMetricKey?>(null) }
    var menuTarget by remember { mutableStateOf<RankingMenuTarget?>(null) }

    val templates = remember(customTemplates) {
        rankingBuiltInTemplates() + customTemplates
    }
    val selectedTab = rankingTabForPage(pagerState.currentPage)

    // Note: we intentionally do NOT call onRootDestinationChange(false) when opening filters.
    // Hiding the bottom tab bar resizes the Scaffold content area, causing the ranking layout
    // to visibly shift before the filter overlay finishes animating in.

    LaunchedEffect(isAuthorized) {
        if (isAuthorized) {
            viewModel.load(filters = appliedFilters)
        } else {
            viewModel.reset()
        }
    }

    val handleProjectClick: (String) -> Unit = { projectId ->
        if (isSearchFocused) {
            focusManager.clearFocus()
            isSearchFocused = false
        } else {
            onProjectClick(projectId)
        }
    }
    val handleStudentClick: (String, String, String?) -> Unit = { userId, userName, projectName ->
        if (isSearchFocused) {
            focusManager.clearFocus()
            isSearchFocused = false
        } else {
            onStudentClick(userId, userName, projectName)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Logo — first child, z=0, behind everything.
        // Same pattern as OnboardingScreen: Image as first child of the same Box as all content.
        RankingBackgroundLogo()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        focusManager.clearFocus()
                        isSearchFocused = false
                    },
                ),
        )
        when {
            !isAuthorized -> {
                RankingUnauthorizedState()
            }

            else -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    RankingTitle()
                    RankingTabs(
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(tab.ordinal)
                            }
                        },
                    )
                    RankingSearchField(
                        searchText = searchText,
                        onSearchTextChange = { searchText = it },
                        onFocusChange = { isSearchFocused = it },
                        modifier = Modifier.padding(horizontal = 17.dp, vertical = 12.dp),
                    )
                    RankingControlsRow(
                        activeFilters = appliedFilters,
                        sortOrder = sortOrder,
                        onFilterClick = {
                            focusManager.clearFocus()
                            isSearchFocused = false
                            draftFilters = appliedFilters
                            menuMetricKey = null
                            menuTarget = null
                            showFiltersScreen = true
                        },
                        onSortToggle = {
                            sortOrder = if (sortOrder == RankingSortOrder.Descending) {
                                RankingSortOrder.Ascending
                            } else {
                                RankingSortOrder.Descending
                            }
                        },
                        modifier = Modifier.padding(horizontal = 17.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        when (val state = uiState) {
                            RankingUiState.Idle,
                            RankingUiState.Loading,
                            -> LoadingContent()

                            is RankingUiState.Error -> ErrorContent(
                                message = state.message,
                                onRetry = { viewModel.retry() },
                            )

                            is RankingUiState.Success -> {
                                val pullRefreshState = rememberPullToRefreshState()
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pullToRefresh(
                                            state = pullRefreshState,
                                            isRefreshing = isRefreshing,
                                            onRefresh = { viewModel.refresh() },
                                        ),
                                ) {
                                    val pullFraction = pullRefreshState.distanceFraction.coerceAtLeast(0f)
                                    val stretchEase = if (isRefreshing) {
                                        1f
                                    } else {
                                        LinearOutSlowInEasing.transform(pullFraction.coerceIn(0f, 1f))
                                    }
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                transformOrigin = TransformOrigin(0.5f, 0f)
                                                clip = false
                                                scaleY = 1f + stretchEase * RankingPullContentStretchMax
                                            },
                                    ) { page ->
                                        val tab = rankingTabForPage(page)
                                        val sourceItems = if (tab == RankingTab.Projects) {
                                            state.data.projects
                                        } else {
                                            state.data.students
                                        }
                                        val searchedItems = searchRankingItems(sourceItems, searchText)
                                        val sortedItems = sortRankingItems(searchedItems, sortOrder)
                                        val markerLabel = if (tab == RankingTab.Projects) "Текущий" else "Вы"
                                        val fullSortedItems = sortRankingItems(sourceItems, sortOrder)
                                        val pinnedItem = fullSortedItems.firstOrNull { it.markerLabel == markerLabel }
                                        val pinnedRank = if (pinnedItem != null) fullSortedItems.indexOf(pinnedItem) + 1 else -1

                                        RankingList(
                                            items = sortedItems,
                                            tab = tab,
                                            pinnedItem = pinnedItem,
                                            pinnedRank = pinnedRank,
                                            resetKey = "${tab.name}|${searchText.trim()}|${sortOrder.name}|${appliedFilters.hashCode()}",
                                            emptyText = if (tab == RankingTab.Projects) {
                                                "Нет данных по проектам"
                                            } else {
                                                "Нет данных по студентам"
                                            },
                                            onProjectClick = handleProjectClick,
                                            onStudentClick = handleStudentClick,
                                        )
                                    }

                                    RankingPullRefreshIndicator(
                                        state = pullRefreshState,
                                        isRefreshing = isRefreshing,
                                        modifier = Modifier.align(Alignment.TopCenter),
                                    )
                                }

                            }
                        }

                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showFiltersScreen,
            enter = fadeIn(animationSpec = tween(220)) + scaleIn(
                initialScale = 0.985f,
                animationSpec = tween(220),
            ),
            exit = fadeOut(animationSpec = tween(160)) + scaleOut(
                targetScale = 0.99f,
                animationSpec = tween(160),
            ),
        ) {
            RankingFiltersScreen(
                filters = draftFilters,
                templates = templates,
                selectedTemplateId = selectedTemplateId,
                expandedMetricMenu = menuMetricKey,
                expandedGlobalMenu = menuTarget,
                onBack = {
                    menuMetricKey = null
                    menuTarget = null
                    showFiltersScreen = false
                },
                onTemplateSelected = { template ->
                    selectedTemplateId = template.id
                    draftFilters = template.filters ?: draftFilters
                },
                onMetricToggle = { key ->
                    selectedTemplateId = "none"
                    draftFilters = draftFilters.withMetric(
                        key = key,
                        transform = { it.copy(enabled = !it.enabled) },
                    )
                },
                onClearMetrics = {
                    selectedTemplateId = "none"
                    draftFilters = draftFilters.copy(
                        metrics = draftFilters.metrics.mapValues { (_, metric) -> metric.copy(enabled = false) },
                    )
                },
                onMetricInfoClick = { key ->
                    selectedMetricInfo = key
                },
                onMetricMenuOpen = { key ->
                    menuMetricKey = if (menuMetricKey == key) null else key
                },
                onMetricMenuDismiss = {
                    menuMetricKey = null
                },
                onMetricPeriodSelected = { key, preset ->
                    selectedTemplateId = "none"
                    draftFilters = draftFilters.withMetric(
                        key = key,
                        transform = { it.copy(periodPreset = preset) },
                    )
                    menuMetricKey = null
                },
                onMetricThresholdSelected = { key, preset ->
                    selectedTemplateId = "none"
                    draftFilters = draftFilters.withMetric(
                        key = key,
                        transform = { it.copy(thresholdPreset = preset) },
                    )
                    menuMetricKey = null
                },
                onMetricWeekDaySelected = { key, weekDay ->
                    selectedTemplateId = "none"
                    draftFilters = draftFilters.withMetric(
                        key = key,
                        transform = { it.copy(weekDay = weekDay) },
                    )
                    menuMetricKey = null
                },
                onDateRangeMenuOpen = {
                    menuTarget = if (menuTarget == RankingMenuTarget.DateRange) null else RankingMenuTarget.DateRange
                },
                onDateRangeMenuDismiss = {
                    menuTarget = null
                },
                onDateRangeSelected = { range ->
                    selectedTemplateId = "none"
                    draftFilters = draftFilters.copy(dateRange = range.filter)
                    menuTarget = null
                },
                onClearDateRange = {
                    selectedTemplateId = "none"
                    draftFilters = draftFilters.copy(dateRange = RankingDateRangeFilter())
                },
                onApply = {
                    appliedFilters = draftFilters
                    showFiltersScreen = false
                    menuMetricKey = null
                    menuTarget = null
                    viewModel.applyFilters(appliedFilters)
                },
                onSaveTemplate = {
                    showSaveTemplateDialog = true
                },
            )
        }

        if (showSaveTemplateDialog) {
            SaveTemplateDialog(
                onDismiss = { showSaveTemplateDialog = false },
                onSave = { templateName ->
                    val cleanName = templateName.trim()
                    if (cleanName.isNotEmpty()) {
                        val templateId = "custom_${customTemplates.size}_${cleanName.lowercase()}"
                        customTemplates = customTemplates + RankingFilterTemplate(
                            id = templateId,
                            title = cleanName,
                            filters = draftFilters,
                            isBuiltIn = false,
                        )
                        selectedTemplateId = templateId
                    }
                    showSaveTemplateDialog = false
                },
            )
        }

        selectedMetricInfo?.let { metricKey ->
            MetricInfoDialog(
                info = rankingMetricInfo(metricKey),
                onDismiss = { selectedMetricInfo = null },
            )
        }
    }
}

@Composable
private fun BoxScope.RankingBackgroundLogo(
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(Res.drawable.spbu_logo),
        contentDescription = null,
        modifier = modifier
            .fillMaxSize()
            .align(Alignment.Center),
        contentScale = ContentScale.FillWidth,
    )
}

@Composable
private fun RankingTitle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Рейтинг",
            fontFamily = AppFonts.OpenSansBold,
            fontSize = 40.sp,
            color = RankingRed,
            letterSpacing = 0.4.sp,
        )
    }
}

@Composable
private fun RankingTabs(
    selectedTab: RankingTab,
    onTabSelected: (RankingTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                RankingTabText(
                    text = "Проекты",
                    selected = selectedTab == RankingTab.Projects,
                    onClick = { onTabSelected(RankingTab.Projects) },
                )
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                RankingTabText(
                    text = "Студенты",
                    selected = selectedTab == RankingTab.Students,
                    onClick = { onTabSelected(RankingTab.Students) },
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(1.dp)
                .height(20.dp)
                .background(RankingGray),
        )
    }
}

@Composable
private fun RankingTabText(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "ranking_tab_scale",
    )

    Text(
        text = text,
        fontFamily = AppFonts.OpenSansRegular,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        fontSize = 20.sp,
        color = if (selected) RankingGray else RankingLightGray,
        modifier = Modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    )
}

@Composable
private fun RankingSearchField(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

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
                color = RankingDivider,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.search_icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        BasicTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    onFocusChange(focusState.isFocused)
                },
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = AppFonts.OpenSansSemiBold,
                fontSize = 16.sp,
                color = RankingGray,
                letterSpacing = 0.16.sp,
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                    onFocusChange(false)
                },
            ),
            decorationBox = { innerTextField ->
                if (searchText.isBlank()) {
                    Text(
                        text = "Пример поиска",
                        fontFamily = AppFonts.OpenSansSemiBold,
                        fontSize = 16.sp,
                        color = RankingGray,
                        letterSpacing = 0.16.sp,
                    )
                }
                innerTextField()
            },
        )
    }
}

@Composable
private fun RankingControlsRow(
    activeFilters: RankingFilters,
    sortOrder: RankingSortOrder,
    onFilterClick: () -> Unit,
    onSortToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val filtersListState = rememberLazyListState()
    val canScrollBackward by remember {
        derivedStateOf { filtersListState.canScrollBackward }
    }
    val canScrollForward by remember {
        derivedStateOf { filtersListState.canScrollForward }
    }

    val leftFadeAlpha by animateFloatAsState(
        targetValue = if (canScrollBackward) 1f else 0f,
        animationSpec = tween(200),
        label = "filtersLaneLeftFade",
    )
    val rightFadeAlpha by animateFloatAsState(
        targetValue = if (canScrollForward) 1f else 0f,
        animationSpec = tween(200),
        label = "filtersLaneRightFade",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(RankingFiltersToolbarHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RankingIconButton(
            painter = painterResource(Res.drawable.filter_icon),
            contentDescription = "Открыть фильтры",
            onClick = onFilterClick,
            showBadge = activeFilters.hasActiveSelections(),
            iconModifier = Modifier.size(RankingFilterLaneIconTap),
            modifier = Modifier.size(RankingFilterLaneIconTap),
        )
        Spacer(modifier = Modifier.width(4.dp))
        RankingSortButton(
            sortOrder = sortOrder,
            onClick = onSortToggle,
            modifier = Modifier.size(RankingFilterLaneIconTap),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            LazyRow(
                state = filtersListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        val fadeW = RankingFiltersScrollFadeWidth.toPx()
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 1f - leftFadeAlpha),
                                    Color.Black,
                                ),
                                startX = 0f,
                                endX = fadeW.coerceAtMost(size.width),
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black,
                                    Color.Black.copy(alpha = 1f - rightFadeAlpha),
                                ),
                                startX = (size.width - fadeW).coerceAtLeast(0f),
                                endX = size.width,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                items(activeFilters.activeChipLabels()) { chipLabel ->
                    RankingAppliedFilterChip(chipLabel)
                }
            }
        }
    }
}

@Composable
private fun RankingIconButton(
    painter: androidx.compose.ui.graphics.painter.Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBadge: Boolean = false,
    iconModifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "ranking_icon_scale",
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = iconModifier,
        )
        if (showBadge) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-1).dp, y = 1.dp)
                    .background(RankingRed, CircleShape),
            )
        }
    }
}

@Composable
private fun RankingSortButton(
    sortOrder: RankingSortOrder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "ranking_sort_scale",
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.stats_sort_asc),
            contentDescription = if (sortOrder == RankingSortOrder.Descending) {
                "Сортировка по убыванию"
            } else {
                "Сортировка по возрастанию"
            },
            modifier = Modifier
                .size(24.dp)
                .rotate(if (sortOrder == RankingSortOrder.Descending) 180f else 0f),
        )
    }
}

@Composable
private fun RankingAppliedFilterChip(
    label: String,
) {
    Box(
        modifier = Modifier
            .background(RankingGray, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            fontFamily = AppFonts.OpenSansSemiBold,
            fontSize = 15.sp,
            color = Color.White,
            lineHeight = 10.sp,
            letterSpacing = 0.15.sp,
            maxLines = 1,
        )
    }
}


@Composable
private fun RankingUnauthorizedState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Рейтинг недоступен",
            fontFamily = AppFonts.OpenSansBold,
            fontSize = 22.sp,
            color = RankingRed,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Авторизуйтесь, чтобы увидеть рейтинг проектов и студентов.",
            fontFamily = AppFonts.OpenSansMedium,
            fontSize = 16.sp,
            color = RankingGray,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RankingPullRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val visibilityEased = LinearOutSlowInEasing.transform(
        state.distanceFraction.coerceIn(0f, 1.2f),
    ).coerceIn(0f, 1f)
    val alpha = if (isRefreshing) 1f else visibilityEased
    Box(
        modifier = modifier
            .offset(
                y = RankingPullIndicatorRestOffset + RankingPullIndicatorTravel * state.distanceFraction,
            )
            .alpha(alpha),
        contentAlignment = Alignment.Center,
    ) {
        if (alpha < 0.02f && !isRefreshing) return@Box
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = RankingRed,
                strokeWidth = 2.5.dp,
            )
        } else {
            CircularProgressIndicator(
                progress = { minOf(1f, state.distanceFraction) },
                modifier = Modifier.size(28.dp),
                color = RankingRed,
                strokeWidth = 2.5.dp,
                trackColor = RankingLightGray.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun LoadingContent(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(color = RankingRed)
            Text(
                text = "Загрузка рейтинга...",
                fontFamily = AppFonts.OpenSansMedium,
                fontSize = 14.sp,
                color = RankingGray,
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 28.dp),
        ) {
            Text(
                text = "Ошибка загрузки",
                fontFamily = AppFonts.OpenSansBold,
                fontSize = 18.sp,
                color = RankingRed,
            )
            Text(
                text = message,
                fontFamily = AppFonts.OpenSansRegular,
                fontSize = 14.sp,
                color = RankingGray,
            )
            RankingActionButton(
                text = "Повторить",
                backgroundColor = RankingRed,
                borderColor = RankingRedLight,
                onClick = onRetry,
                modifier = Modifier.width(150.dp),
            )
        }
    }
}

@Composable
private fun RankingList(
    items: List<RankingItem>,
    tab: RankingTab,
    resetKey: String,
    emptyText: String,
    pinnedItem: RankingItem? = null,
    pinnedRank: Int = -1,
    onProjectClick: (String) -> Unit,
    onStudentClick: (String, String, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty() && pinnedItem == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = emptyText,
                fontFamily = AppFonts.OpenSansMedium,
                fontSize = 14.sp,
                color = RankingGray,
            )
        }
        return
    }

    val listState = rememberLazyListState()
    var visibleCount by remember(resetKey) {
        mutableStateOf(minOf(RankingPageSize, items.size))
    }

    LaunchedEffect(resetKey, items.size) {
        visibleCount = minOf(RankingPageSize, items.size)
        listState.scrollToItem(0)
    }

    LaunchedEffect(listState, items.size, visibleCount) {
        snapshotFlow {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisibleIndex >= visibleCount - 3 && visibleCount < items.size
        }.collect { shouldLoadMore ->
            if (shouldLoadMore) {
                visibleCount = minOf(items.size, visibleCount + RankingPageSize)
            }
        }
    }

    // Position of the pinned item within the displayed (possibly search-filtered) list.
    // -1 means the item is not in the current filtered results.
    val pinnedItemIndexInList = remember(items, pinnedItem) {
        if (pinnedItem == null) -1
        else items.indexOfFirst { it.key == pinnedItem.key }
    }

    // Measured height of the sticky header (px). Updated once on first layout via onSizeChanged.
    var stickyHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    // stickyFraction: 0..1, driven by the scroll progress of the item that precedes the pinned one.
    //
    // WHY NOT pinnedItem.offset:
    //   contentPadding.top affects item offsets → reading offset → setting padding → circular shaking.
    //
    // CORRECT APPROACH:
    //   - The LazyColumn is shifted+clipped via Modifier.padding(top = stickyHeight) — FIXED, never
    //     changes during scroll, so no feedback loop.
    //   - stickyFraction is derived only from FVI / FVO (logical scroll state), which is completely
    //     independent of layout padding.
    //   - Transition fires as the item just before the pinned one scrolls off the top of the list:
    //       FVO = 0            → item before pinned at top  → fraction = 1 (sticky fully visible)
    //       FVO = itemHeight   → item before pinned gone    → fraction = 0 (sticky gone)
    //   - Special case: pinned item IS item[0] → fade as the pinned item itself scrolls off.
    val stickyFraction by remember {
        derivedStateOf {
            if (pinnedItem == null) return@derivedStateOf 0f
            if (pinnedItemIndexInList < 0) return@derivedStateOf 1f // filtered out — always show
            val sh = stickyHeightPx
            if (sh <= 0) return@derivedStateOf 1f // not yet measured

            val fvi = listState.firstVisibleItemIndex
            val fvo = listState.firstVisibleItemScrollOffset.toFloat()

            when {
                // Special case: pinned item is item[0] — fade as it scrolls off the top.
                pinnedItemIndexInList == 0 -> {
                    if (fvi == 0) (1f - fvo / sh.toFloat()).coerceIn(0f, 1f) else 0f
                }

                // Pinned item is at the top of the viewport (or scrolled above) — sticky gone.
                // NOTE: >= (not >) to prevent a jump-to-1 when FVI transitions from N-1 to N.
                fvi >= pinnedItemIndexInList -> 0f

                // The item just before pinned is at the top and scrolling off — fade proportionally.
                fvi == pinnedItemIndexInList - 1 -> {
                    val itemInfo = listState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.index == fvi }
                        ?: return@derivedStateOf 1f
                    val itemHeight = itemInfo.size.toFloat()
                    if (itemHeight <= 0f) 1f else (1f - fvo / itemHeight).coerceIn(0f, 1f)
                }

                // Pinned item is still well below the viewport — always show sticky.
                else -> 1f
            }
        }
    }

    // Padding is driven by stickyFraction (which is driven by user scroll), so the top boundary
    // and the scroll position always move at the same rate — no independent post-fade animation
    // that would cause items to drift upward on their own.
    val stickyTopPaddingDp = with(density) { (stickyHeightPx * stickyFraction).toDp() }

    val canScrollUp by remember { derivedStateOf { listState.canScrollBackward } }
    val canScrollDown by remember { derivedStateOf { listState.canScrollForward } }
    val topFadeAlpha by animateFloatAsState(
        targetValue = if (canScrollUp) 1f else 0f,
        animationSpec = tween(200),
        label = "listTopFade",
    )
    val bottomFadeAlpha by animateFloatAsState(
        targetValue = if (canScrollDown) 1f else 0f,
        animationSpec = tween(200),
        label = "listBottomFade",
    )

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = stickyTopPaddingDp)
                // Offscreen layer so DstIn works against a transparent canvas —
                // the mask only touches drawn pixels, transparent areas stay transparent.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()

                    // Top edge: gradient alpha mask — items fade out pixel-by-pixel
                    // over 32dp so there's no hard clip. The gradient top color animates
                    // 0→1 opacity via topFadeAlpha so the fade only appears while scrolling.
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 1f - topFadeAlpha),
                                Color.Black,
                            ),
                            startY = 0f,
                            endY = 32.dp.toPx(),
                        ),
                        blendMode = BlendMode.DstIn,
                    )

                    // Bottom edge: same idea, opposite direction.
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black,
                                Color.Black.copy(alpha = 1f - bottomFadeAlpha),
                            ),
                            startY = size.height - 60.dp.toPx(),
                            endY = size.height,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
            contentPadding = PaddingValues(
                start = 17.dp,
                end = 20.dp,
                bottom = 188.dp,
            ),
        ) {
            // The pinned item appears only at its natural rank position — no duplicate at the top.
            // Display its real rank (pinnedRank from the full sorted list) when shown in the list.
            itemsIndexed(
                items = items.take(visibleCount),
                key = { _, item -> item.key },
            ) { index, item ->
                RankingRow(
                    index = if (item.key == pinnedItem?.key) pinnedRank else index + 1,
                    item = item,
                    tab = tab,
                    onProjectClick = onProjectClick,
                    onStudentClick = onStudentClick,
                )
            }
        }

        // Sticky pinned header overlay — sits above the LazyColumn.
        // The DstIn fade on the LazyColumn does not affect this overlay because
        // it is a sibling composable drawn after the LazyColumn, not a child of it.
        if (pinnedItem != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .onSizeChanged { size -> if (size.height > 0) stickyHeightPx = size.height }
                    .alpha(stickyFraction)
                    .padding(start = 17.dp, end = 20.dp),
            ) {
                Text(
                    text = if (tab == RankingTab.Projects) "Ваш проект" else "Ваша позиция",
                    fontFamily = AppFonts.OpenSansSemiBold,
                    fontSize = 11.sp,
                    color = RankingRed,
                    letterSpacing = 0.4.sp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                )
                RankingRow(
                    index = pinnedRank,
                    item = pinnedItem,
                    tab = tab,
                    onProjectClick = onProjectClick,
                    onStudentClick = onStudentClick,
                    showBody = false,
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun RankingRow(
    index: Int,
    item: RankingItem,
    tab: RankingTab,
    onProjectClick: (String) -> Unit,
    onStudentClick: (String, String, String?) -> Unit,
    modifier: Modifier = Modifier,
    showBody: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.992f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 700f),
        label = "ranking_row_scale",
    )

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        when (tab) {
                            RankingTab.Projects -> onProjectClick(item.key)
                            RankingTab.Students -> onStudentClick(item.key, item.title, item.projectName)
                        }
                    },
                ),
        ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(RankingDivider),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 7.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            RankingPositionIndicator(
                position = index,
                item = item,
                modifier = Modifier
                    .width(52.dp)
                    .padding(top = 2.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RankingTitleText(
                        item = item,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.scoreText,
                        fontFamily = AppFonts.OpenSansBold,
                        fontSize = 20.sp,
                        color = scoreGradientColor(item.score),
                        lineHeight = 12.sp,
                    )
                }
                if (showBody) {
                    Spacer(modifier = Modifier.height(if (tab == RankingTab.Projects) 8.dp else 10.dp))
                    when (tab) {
                        RankingTab.Projects -> ProjectRankingBody(item)
                        RankingTab.Students -> StudentRankingBody(item)
                    }
                }
            }
        }
        } // closes Column
    } // closes outer Box
}

@Composable
private fun RankingPositionIndicator(
    position: Int,
    item: RankingItem,
    modifier: Modifier = Modifier,
) {
    val movedUp = item.positionDelta?.let { it > 0 } == true
    val icon = when {
        item.positionDelta == null || item.positionDelta == 0 -> null
        movedUp -> Res.drawable.position_up
        else -> Res.drawable.position_down
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = position.toString(),
            fontFamily = AppFonts.OpenSansBold,
            fontSize = 24.sp,
            color = RankingGray,
            lineHeight = 20.sp,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier.size(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Image(
                    painter = painterResource(icon),
                    contentDescription = if (movedUp) {
                        "Поднялся в рейтинге"
                    } else {
                        "Опустился в рейтинге"
                    },
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun RankingTitleText(
    item: RankingItem,
    modifier: Modifier = Modifier,
) {
    val titleText = remember(item.title, item.markerLabel) {
        buildAnnotatedString {
            append(item.title)
            item.markerLabel?.takeIf { it.isNotBlank() }?.let { marker ->
                append(" ")
                withStyle(SpanStyle(color = RankingRed)) {
                    append("($marker)")
                }
            }
        }
    }

    Text(
        text = titleText,
        fontFamily = AppFonts.OpenSansBold,
        fontSize = 16.sp,
        color = RankingGray,
        lineHeight = 20.sp,
        letterSpacing = 0.16.sp,
        modifier = modifier,
        maxLines = if (item.projectName == null) 3 else 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectRankingBody(
    item: RankingItem,
) {
    Column {
        Text(
            text = item.description?.takeIf { it.isNotBlank() } ?: "Описание проекта пока недоступно",
            fontFamily = AppFonts.OpenSansRegular,
            fontSize = 10.sp,
            color = RankingGray,
            lineHeight = 10.sp,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
        )

        if (item.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item.tags.forEach { tag ->
                    RankingTagChip(tag)
                }
            }
        }
    }
}

@Composable
private fun StudentRankingBody(
    item: RankingItem,
) {
    val projectText = buildAnnotatedString {
        withStyle(
            SpanStyle(
                fontFamily = AppFonts.OpenSansBold,
                color = RankingGray,
            ),
        ) {
            append("Проект:")
        }
        append(" ")
        append(item.projectName?.takeIf { it.isNotBlank() } ?: "Не определен")
    }

    Text(
        text = projectText,
        fontFamily = AppFonts.OpenSansRegular,
        fontSize = 10.sp,
        color = RankingGray,
        lineHeight = 10.sp,
    )
}

@Composable
private fun RankingTagChip(
    text: String,
) {
    Box(
        modifier = Modifier
            .border(1.dp, RankingDivider, RoundedCornerShape(10.dp))
            .padding(horizontal = 5.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            fontFamily = AppFonts.OpenSansSemiBold,
            fontSize = 10.sp,
            color = RankingGray,
            lineHeight = 10.sp,
        )
    }
}

@Composable
private fun RankingActionButton(
    text: String,
    backgroundColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "ranking_action_scale",
    )

    Box(
        modifier = modifier
            .height(50.dp)
            .scale(scale)
            .alpha(if (enabled) 1f else 0.5f)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (backgroundColor == RankingRed) {
                        listOf(RankingRed, RankingRedGradientBottom)
                    } else {
                        listOf(backgroundColor, backgroundColor)
                    }
                ),
                shape = RoundedCornerShape(10.dp),
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontFamily = AppFonts.OpenSansBold,
            fontSize = 16.sp,
            color = Color.White,
            letterSpacing = 0.16.sp,
        )
    }
}

@Composable
private fun RankingFiltersScreen(
    filters: RankingFilters,
    templates: List<RankingFilterTemplate>,
    selectedTemplateId: String,
    expandedMetricMenu: RankingMetricKey?,
    expandedGlobalMenu: RankingMenuTarget?,
    onBack: () -> Unit,
    onTemplateSelected: (RankingFilterTemplate) -> Unit,
    onMetricToggle: (RankingMetricKey) -> Unit,
    onClearMetrics: () -> Unit,
    onMetricInfoClick: (RankingMetricKey) -> Unit,
    onMetricMenuOpen: (RankingMetricKey) -> Unit,
    onMetricMenuDismiss: () -> Unit,
    onMetricPeriodSelected: (RankingMetricKey, RankingPeriodPreset) -> Unit,
    onMetricThresholdSelected: (RankingMetricKey, RankingThresholdPreset) -> Unit,
    onMetricWeekDaySelected: (RankingMetricKey, RankingWeekDay) -> Unit,
    onDateRangeMenuOpen: () -> Unit,
    onDateRangeMenuDismiss: () -> Unit,
    onDateRangeSelected: (RankingDateRangePreset) -> Unit,
    onClearDateRange: () -> Unit,
    onApply: () -> Unit,
    onSaveTemplate: () -> Unit,
) {
    val dateRangePresets = remember { rankingDateRangePresets() }
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .imePadding(),
    ) {
        RankingBackgroundLogo()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 0.dp),
            ) {
                Image(
                    painter = painterResource(Res.drawable.arrow_back),
                    contentDescription = "Назад",
                    modifier = Modifier
                        .padding(start = 9.dp, top = 14.dp)
                        .size(24.dp)
                        .clickable(onClick = onBack),
                )
                Text(
                    text = "Фильтры",
                    fontFamily = AppFonts.OpenSansBold,
                    fontSize = 40.sp,
                    color = RankingRed,
                    letterSpacing = 0.4.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 15.dp),
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Шаблоны",
                    fontFamily = AppFonts.OpenSansSemiBold,
                    fontSize = 15.sp,
                    color = Color.Black,
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(templates) { template ->
                        RankingTemplateChip(
                            title = template.title,
                            selected = selectedTemplateId == template.id,
                            onClick = { onTemplateSelected(template) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Метрики",
                        fontFamily = AppFonts.OpenSansSemiBold,
                        fontSize = 15.sp,
                        color = Color.Black,
                    )
                    SmallPillButton(
                        text = "Очистить",
                        onClick = onClearMetrics,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Выберите метрики, по которым будет составляться общая оценка:",
                    fontFamily = AppFonts.OpenSansMedium,
                    fontSize = 14.sp,
                    color = RankingGray,
                    lineHeight = 16.sp,
                    modifier = Modifier.widthIn(max = 375.dp),
                )
                Spacer(modifier = Modifier.height(10.dp))

                RankingMetricKey.entries.forEach { metricKey ->
                    val metricFilter = filters.metric(metricKey)
                    RankingMetricRow(
                        metricKey = metricKey,
                        metricFilter = metricFilter,
                        expanded = expandedMetricMenu == metricKey,
                        onToggle = { onMetricToggle(metricKey) },
                        onInfoClick = { onMetricInfoClick(metricKey) },
                        onMenuClick = { onMetricMenuOpen(metricKey) },
                        onMenuDismiss = onMetricMenuDismiss,
                        onPeriodSelected = { preset -> onMetricPeriodSelected(metricKey, preset) },
                        onThresholdSelected = { preset -> onMetricThresholdSelected(metricKey, preset) },
                        onWeekDaySelected = { weekDay -> onMetricWeekDaySelected(metricKey, weekDay) },
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Диапазон дат",
                        fontFamily = AppFonts.OpenSansSemiBold,
                        fontSize = 15.sp,
                        color = Color.Black,
                    )
                    SmallPillButton(
                        text = "Очистить",
                        onClick = onClearDateRange,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                RankingDateRangeField(
                    dateRange = filters.dateRange,
                    expanded = expandedGlobalMenu == RankingMenuTarget.DateRange,
                    onClick = onDateRangeMenuOpen,
                    onDismiss = onDateRangeMenuDismiss,
                    onPresetSelected = onDateRangeSelected,
                    presets = dateRangePresets,
                )
                Spacer(modifier = Modifier.height(28.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 19.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                RankingActionButton(
                    text = "Применить",
                    backgroundColor = RankingGray,
                    borderColor = RankingDivider,
                    onClick = onApply,
                    modifier = Modifier.weight(1f),
                )
                RankingActionButton(
                    text = "Сохранить шаблон",
                    backgroundColor = RankingRed,
                    borderColor = RankingRedLight,
                    onClick = onSaveTemplate,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RankingTemplateChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(
                color = if (selected) RankingRed else RankingGray,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Text(
            text = title,
            fontFamily = AppFonts.OpenSansSemiBold,
            fontSize = 15.sp,
            color = Color.White,
            letterSpacing = 0.15.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun RankingMetricRow(
    metricKey: RankingMetricKey,
    metricFilter: RankingMetricFilter,
    expanded: Boolean,
    onToggle: () -> Unit,
    onInfoClick: () -> Unit,
    onMenuClick: () -> Unit,
    onMenuDismiss: () -> Unit,
    onPeriodSelected: (RankingPeriodPreset) -> Unit,
    onThresholdSelected: (RankingThresholdPreset) -> Unit,
    onWeekDaySelected: (RankingWeekDay) -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RankingCheckbox(
                checked = metricFilter.enabled,
                onClick = onToggle,
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = metricKey.title,
                fontFamily = AppFonts.OpenSansRegular,
                fontSize = 15.sp,
                color = RankingLightGray,
                lineHeight = 10.sp,
                modifier = Modifier.weight(1f),
            )
            RankingInfoBadge(onClick = onInfoClick)
        }

        if (metricKey.supportsPeriod && metricFilter.enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            RankingMetricMenuChip(
                text = metricFilter.periodPreset.label,
                expanded = expanded,
                onClick = onMenuClick,
                onDismiss = onMenuDismiss,
            ) {
                RankingPeriodPreset.entries.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(preset.label) },
                        onClick = { onPeriodSelected(preset) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (metricKey.supportsThreshold && metricFilter.enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            RankingMetricMenuChip(
                text = metricFilter.thresholdPreset.label,
                expanded = expanded,
                onClick = onMenuClick,
                onDismiss = onMenuDismiss,
            ) {
                RankingThresholdPreset.entries.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(preset.label) },
                        onClick = { onThresholdSelected(preset) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (metricKey.supportsWeekDay && metricFilter.enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            RankingMetricMenuChip(
                text = metricFilter.weekDay.label,
                expanded = expanded,
                onClick = onMenuClick,
                onDismiss = onMenuDismiss,
            ) {
                RankingWeekDay.entries.forEach { weekDay ->
                    DropdownMenuItem(
                        text = { Text(weekDay.label) },
                        onClick = { onWeekDaySelected(weekDay) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun RankingCheckbox(
    checked: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(
                    color = if (checked) RankingGray else Color.Transparent,
                    shape = RoundedCornerShape(2.dp),
                )
                .border(
                    width = if (checked) 0.dp else 2.dp,
                    color = RankingGray,
                    shape = RoundedCornerShape(2.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Text(
                    text = "✓",
                    fontFamily = AppFonts.OpenSansBold,
                    fontSize = 13.sp,
                    color = Color.White,
                    lineHeight = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun RankingInfoBadge(
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(15.dp)
            .border(1.dp, RankingGray, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "i",
            fontFamily = AppFonts.OpenSansRegular,
            fontSize = 12.sp,
            color = RankingGray,
            lineHeight = 10.sp,
        )
    }
}

@Composable
private fun RankingMetricMenuChip(
    text: String,
    expanded: Boolean,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box {
        Box(
            modifier = Modifier
                .height(30.dp)
                .border(1.dp, RankingDivider, RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontFamily = AppFonts.OpenSansBold,
                fontSize = 14.sp,
                color = RankingGray,
                lineHeight = 10.sp,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
        ) {
            content()
        }
    }
}

@Composable
private fun RankingDateRangeField(
    dateRange: RankingDateRangeFilter,
    expanded: Boolean,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    onPresetSelected: (RankingDateRangePreset) -> Unit,
    presets: List<RankingDateRangePreset>,
) {
    Box {
        Row(
            modifier = Modifier
                .width(200.dp)
                .height(30.dp)
                .border(1.dp, RankingDivider, RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatDateRangeField(dateRange),
                fontFamily = AppFonts.OpenSansMedium,
                fontSize = 12.sp,
                color = RankingGray,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Image(
                painter = painterResource(Res.drawable.calendar_icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
        ) {
            presets.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(preset.title) },
                    onClick = { onPresetSelected(preset) },
                )
            }
        }
    }
}

@Composable
private fun SmallPillButton(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(RankingRed, RoundedCornerShape(15.dp))
            .border(1.dp, RankingRedLight, RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            fontFamily = AppFonts.OpenSansSemiBold,
            fontSize = 10.sp,
            color = Color.White,
            lineHeight = 10.sp,
        )
    }
}

@Composable
private fun SaveTemplateDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var templateName by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 350.dp)
                .background(Color.White, RoundedCornerShape(20.dp))
                .border(1.dp, RankingGray, RoundedCornerShape(20.dp))
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            RankingBackgroundLogo(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.06f),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Сохранить шаблон",
                        fontFamily = AppFonts.OpenSansBold,
                        fontSize = 24.sp,
                        color = RankingGray,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    Image(
                        painter = painterResource(Res.drawable.close_icon),
                        contentDescription = "Закрыть",
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.CenterEnd)
                            .clickable(onClick = onDismiss),
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                BasicTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = AppFonts.OpenSansRegular,
                        fontSize = 12.sp,
                        color = RankingGray,
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (templateName.isBlank()) {
                                Text(
                                    text = "Введите название шаблона",
                                    fontFamily = AppFonts.OpenSansRegular,
                                    fontSize = 12.sp,
                                    color = RankingLightGray,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(RankingGray),
                )
                Spacer(modifier = Modifier.height(30.dp))
                Box(
                    modifier = Modifier.width(110.dp),
                ) {
                    RankingActionButton(
                        text = "Сохранить",
                        backgroundColor = RankingRed,
                        borderColor = RankingRedLight,
                        onClick = { onSave(templateName) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricInfoDialog(
    info: RankingMetricInfo,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 350.dp)
                .background(Color.White, RoundedCornerShape(20.dp))
                .border(1.dp, RankingGray, RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 12.dp),
        ) {
            RankingBackgroundLogo(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.05f),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = info.title,
                        fontFamily = AppFonts.OpenSansBold,
                        fontSize = 16.sp,
                        color = Color.Black,
                        modifier = Modifier.align(Alignment.CenterStart),
                    )
                    Image(
                        painter = painterResource(Res.drawable.close_icon),
                        contentDescription = "Закрыть",
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.CenterEnd)
                            .clickable(onClick = onDismiss),
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                MetricInfoSection(
                    title = "Что показывает:",
                    lines = listOf(info.whatShows),
                )
                Spacer(modifier = Modifier.height(12.dp))
                MetricInfoSection(
                    title = "Зачем нужна:",
                    lines = info.whyNeeded,
                )
                Spacer(modifier = Modifier.height(12.dp))
                MetricInfoSection(
                    title = "Как интерпретировать:",
                    lines = info.howToInterpret,
                )
                info.note?.let { note ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    fontFamily = AppFonts.OpenSansBold,
                                    color = RankingGray,
                                )
                            ) {
                                append("Примечание:")
                            }
                            append(" ")
                            append(note)
                        },
                        fontFamily = AppFonts.OpenSansRegular,
                        fontSize = 12.sp,
                        color = RankingGray,
                        lineHeight = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricInfoSection(
    title: String,
    lines: List<String>,
) {
    Text(
        text = title,
        fontFamily = AppFonts.OpenSansBold,
        fontSize = 12.sp,
        color = RankingGray,
        lineHeight = 12.sp,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { line ->
            Text(
                text = line,
                fontFamily = AppFonts.OpenSansRegular,
                fontSize = 12.sp,
                color = RankingGray,
                lineHeight = 12.sp,
            )
        }
    }
}

private fun RankingFilters.withMetric(
    key: RankingMetricKey,
    transform: (RankingMetricFilter) -> RankingMetricFilter,
): RankingFilters {
    val updatedMetrics = metrics.toMutableMap()
    updatedMetrics[key] = transform(metric(key))
    return copy(metrics = updatedMetrics)
}

private fun rankingMetricInfo(metricKey: RankingMetricKey): RankingMetricInfo {
    return when (metricKey) {
        RankingMetricKey.Commits -> RankingMetricInfo(
            title = "Commits",
            whatShows = "Список всех коммитов в репозитории: автор, дата, измененные файлы и объем изменений.",
            whyNeeded = listOf(
                "Отслеживает активность разработки.",
                "Служит базой для метрик Total Commits, Code Churn и Code Ownership.",
                "Помогает увидеть регулярность работы команды.",
            ),
            howToInterpret = listOf(
                "Много регулярных коммитов обычно означает стабильную разработку.",
                "Длинные паузы между коммитами могут говорить о простоях или узких местах.",
            ),
            note = "Это базовая метрика. В рейтинге она сейчас транслируется в клиентскую оценку активности по коммитам.",
        )

        RankingMetricKey.Issues -> RankingMetricInfo(
            title = "Issues",
            whatShows = "Список всех задач: созданные, открытые, закрытые и назначенные исполнители.",
            whyNeeded = listOf(
                "Показывает, как команда управляет задачами.",
                "Служит основой для оценки завершенности задач.",
            ),
            howToInterpret = listOf(
                "Большое количество незакрытых задач может означать накопленный долг.",
                "Баланс созданных и закрытых задач говорит о здоровье процесса.",
            ),
            note = "В клиентском рейтинге эта метрика сейчас маппится на оценку завершенности задач.",
        )

        RankingMetricKey.PullRequests -> RankingMetricInfo(
            title = "Pull Requests",
            whatShows = "Список pull request: автор, время создания, закрытия и фактическая скорость прохождения ревью.",
            whyNeeded = listOf(
                "Показывает, как команда проводит code review.",
                "Используется для производных метрик скорости и качества работы с PR.",
            ),
            howToInterpret = listOf(
                "Быстро и стабильно закрывающиеся PR обычно говорят о хорошем процессе ревью.",
                "Слишком быстрые закрытия могут означать формальное или пропущенное ревью.",
            ),
            note = "В клиентском рейтинге эта метрика сейчас маппится на оценку времени жизни PR.",
        )

        RankingMetricKey.PerformanceGrade -> RankingMetricInfo(
            title = "Оценка производительности",
            whatShows = "Итоговую оценку производительности ресурса, рассчитанную на основе всех метрик с учетом их весов.",
            whyNeeded = listOf(
                "Общая оценка качества работы над проектом.",
                "Быстрая оценка состояния проекта.",
                "Сравнение разных ресурсов проекта.",
            ),
            howToInterpret = listOf(
                "5.0 - отличная работа, все метрики на высоком уровне.",
                "4.0-4.9 - хорошая работа, есть небольшие улучшения.",
                "3.0-3.9 - средний уровень, требуются улучшения.",
                "2.0-2.9 - низкий уровень, необходимы значительные улучшения.",
            ),
            note = "Оценка рассчитывается автоматически на основе настроенных метрик с учетом их весов.",
        )

        RankingMetricKey.TotalCommits -> RankingMetricInfo(
            title = "Общее количество коммитов",
            whatShows = "Количество коммитов за выбранный период с учетом активности команды или отдельного участника.",
            whyNeeded = listOf(
                "Показывает темп разработки.",
                "Помогает сравнивать активность между периодами и командами.",
            ),
            howToInterpret = listOf(
                "Высокое значение обычно означает активную разработку.",
                "Резкое падение может указывать на простой или завершение фазы реализации.",
            ),
            note = "Оценка считается по количеству коммитов в день на пользователя.",
        )

        RankingMetricKey.IssueCompleteness -> RankingMetricInfo(
            title = "Завершенность задач",
            whatShows = "Процент закрытых задач от общего количества задач за выбранный период.",
            whyNeeded = listOf(
                "Оценивает эффективность работы с задачами.",
                "Показывает, не накапливается ли долг по открытым issues.",
            ),
            howToInterpret = listOf(
                "80-100% - большинство задач закрывается вовремя.",
                "40-59% - виден накопленный хвост незавершенных задач.",
                "Ниже 40% - процесс явно требует внимания.",
            ),
            note = "Формула: (закрытые задачи / все задачи) * 3 + 2.",
        )

        RankingMetricKey.PullRequestHangTime -> RankingMetricInfo(
            title = "Время жизни Pull Request",
            whatShows = "Среднее время от создания PR до его закрытия.",
            whyNeeded = listOf(
                "Показывает скорость прохождения code review.",
                "Помогает увидеть задержки на этапе ревью и слияния.",
            ),
            howToInterpret = listOf(
                "Меньше 1 дня - очень быстрый и здоровый процесс ревью.",
                "3-7 дней - приемлемо, но уже есть место для ускорения.",
                "Больше 7 дней - явный сигнал о бутылочном горлышке.",
            ),
            note = "Чем быстрее закрываются PR, тем выше итоговая оценка.",
        )

        RankingMetricKey.RapidPullRequests -> RankingMetricInfo(
            title = "Быстрые Pull Requests",
            whatShows = "Количество pull request, закрытых быстрее заданного порога.",
            whyNeeded = listOf(
                "Выявляет PR, которые могли пройти без полноценного ревью.",
                "Помогает отслеживать качество процесса code review.",
            ),
            howToInterpret = listOf(
                "Низкое количество - хороший сигнал, PR проходят нормальное ревью.",
                "Высокое количество - риск формального или пропущенного ревью.",
                "0 - идеальный сценарий.",
            ),
            note = "Порог задается прямо в фильтрах. Чем меньше быстрых PR, тем выше оценка.",
        )

        RankingMetricKey.CodeChurn -> RankingMetricInfo(
            title = "Изменчивость кода",
            whatShows = "Сколько раз в среднем изменяется кодовая база и отдельные файлы за выбранный период.",
            whyNeeded = listOf(
                "Показывает стабильность архитектуры и модулей.",
                "Помогает найти файлы, которые постоянно переписываются.",
            ),
            howToInterpret = listOf(
                "1-2 изменения на файл - код стабилен.",
                "3-5 - нормальный рабочий уровень.",
                "Выше 5 - возможны проблемы с архитектурой или дизайном решения.",
            ),
            note = "На клиенте пока используется эвристическая оценка churn, потому что backend-оценка для рейтинга еще не выделена отдельным контрактом.",
        )

        RankingMetricKey.CodeOwnership -> RankingMetricInfo(
            title = "Владение кодом",
            whatShows = "Распределение владения кодом между участниками команды.",
            whyNeeded = listOf(
                "Показывает, насколько равномерно распределены знания в проекте.",
                "Помогает выявить зависимость от одного-двух разработчиков.",
            ),
            howToInterpret = listOf(
                "Равномерное распределение - хороший командный признак.",
                "Сильная концентрация у одного человека повышает проектный риск.",
            ),
            note = "Чем равномернее распределение, тем выше итоговая оценка.",
        )

        RankingMetricKey.DominantWeekDay -> RankingMetricInfo(
            title = "Доминирующий день недели",
            whatShows = "Распределение активности команды по дням недели по коммитам, issues и PR.",
            whyNeeded = listOf(
                "Позволяет увидеть устойчивые паттерны работы команды.",
                "Помогает отслеживать нежелательную активность в выходные или перегрузку в один день.",
            ),
            howToInterpret = listOf(
                "Равномерная активность в рабочие дни обычно выглядит здорово.",
                "Пики на выходных могут сигнализировать о переработках.",
                "Низкая активность в выбранный нежелательный день повышает оценку.",
            ),
            note = "Оценка зависит от выбранного нежелательного дня недели.",
        )
    }
}

private fun rankingDateRangePresets(): List<RankingDateRangePreset> {
    val now = PlatformTime.currentTimeMillis()
    return listOf(
        RankingDateRangePreset(
            title = "Весь период",
            filter = RankingDateRangeFilter(),
        ),
        RankingDateRangePreset(
            title = "Последние 2 недели",
            filter = RankingDateRangeFilter(
                startMillis = now - (14L * 24L * 60L * 60L * 1000L),
                endMillis = now,
            ),
        ),
        RankingDateRangePreset(
            title = "Последний месяц",
            filter = RankingDateRangeFilter(
                startMillis = now - (30L * 24L * 60L * 60L * 1000L),
                endMillis = now,
            ),
        ),
        RankingDateRangePreset(
            title = "Последние 3 месяца",
            filter = RankingDateRangeFilter(
                startMillis = now - (90L * 24L * 60L * 60L * 1000L),
                endMillis = now,
            ),
        ),
    )
}

private fun formatDateRangeField(dateRange: RankingDateRangeFilter): String {
    if (!dateRange.isActive) return "с 00.00.0000 по 31.12.3000"

    val start = dateRange.startMillis?.let(::formatDate)
    val end = dateRange.endMillis?.let(::formatDate)
    return "с ${start ?: "00.00.0000"} по ${end ?: "31.12.3000"}"
}

private fun formatDate(epochMillis: Long): String {
    val date = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.UTC).date
    val day = date.dayOfMonth.toString().padStart(2, '0')
    val month = date.monthNumber.toString().padStart(2, '0')
    return "$day.$month.${date.year}"
}

private fun fuzzyMatchScore(text: String, query: String): Double {
    if (query.isBlank()) return 1.0
    if (text.isBlank()) return 0.0

    val normalizedText = text.lowercase()
    val normalizedQuery = query.lowercase()
    if (normalizedText.contains(normalizedQuery)) return 1.0

    val queryBigrams = mutableSetOf<String>()
    for (index in 0 until normalizedQuery.length - 1) {
        queryBigrams += normalizedQuery.substring(index, index + 2)
    }
    if (queryBigrams.isEmpty()) {
        return if (normalizedText.contains(normalizedQuery.first())) 0.5 else 0.0
    }

    val textBigrams = mutableSetOf<String>()
    for (index in 0 until normalizedText.length - 1) {
        textBigrams += normalizedText.substring(index, index + 2)
    }

    val matches = queryBigrams.intersect(textBigrams).size
    return matches.toDouble() / queryBigrams.size.toDouble()
}

private fun searchRankingItems(
    items: List<RankingItem>,
    query: String,
    threshold: Double = 0.3,
): List<RankingItem> {
    if (query.isBlank()) return items

    return items
        .map { item -> item to fuzzyMatchScore(rankingSearchText(item), query) }
        .filter { (_, score) -> score >= threshold }
        .sortedByDescending { (_, score) -> score }
        .map { (item, _) -> item }
}

private fun rankingSearchText(item: RankingItem): String {
    return buildString {
        append(item.title)
        item.description?.takeIf { it.isNotBlank() }?.let {
            append(' ')
            append(it)
        }
        item.projectName?.takeIf { it.isNotBlank() }?.let {
            append(' ')
            append(it)
        }
        if (item.tags.isNotEmpty()) {
            append(' ')
            append(item.tags.joinToString(" "))
        }
    }
}

private fun sortRankingItems(
    items: List<RankingItem>,
    sortOrder: RankingSortOrder,
): List<RankingItem> {
    return when (sortOrder) {
        RankingSortOrder.Descending -> items.sortedWith(
            compareBy<RankingItem> { it.score == null }
                .thenByDescending { it.score ?: Double.NEGATIVE_INFINITY }
                .thenBy { it.title }
        )

        RankingSortOrder.Ascending -> items.sortedWith(
            compareBy<RankingItem> { it.score == null }
                .thenBy { it.score ?: Double.POSITIVE_INFINITY }
                .thenBy { it.title }
        )
    }
}

private fun scoreGradientColor(score: Double?): Color {
    if (score == null) return RankingGray
    val clamped = score.coerceIn(1.0, 5.0).toFloat()
    return if (clamped <= 3f) {
        val t = (clamped - 1f) / 2f
        lerp(RankingRed, RankingYellow, t)
    } else {
        val t = (clamped - 3f) / 2f
        lerp(RankingYellow, RankingGreen, t)
    }
}

private fun previewRankingData(): RankingData {
    return RankingData(
        projects = listOf(
            RankingItem(
                key = "project-1",
                title = "Анализ и прогнозирование манёвра космического аппарата (КА)",
                score = 4.91,
                scoreText = "4.91",
                description = "В современном мире сложно переоценить важность актуальной информации. Каждая компания стремится показать клиенту свои достижения и скрыть недостатки.",
                tags = listOf("Веб-разработка", "C++", "C#"),
                previousPosition = 2,
                positionDelta = 1,
            ),
            RankingItem(
                key = "project-2",
                title = "Разработка инструментов ГИС для построения цифрового двойника речной сети",
                score = 3.65,
                scoreText = "3.65",
                description = "Предполагается создание ГИС инструментов построения границ естественной экосистемы, на которой она уже способна поддерживать необходимую полноводность.",
                tags = listOf("AI", "Анализ данных", "Алгоритмы", "C++", "Python"),
                markerLabel = "Текущий",
                previousPosition = 1,
                positionDelta = -1,
            ),
            RankingItem(
                key = "project-3",
                title = "Telegram-бот для сбора статистики и продвижения",
                score = 1.82,
                scoreText = "1.82",
                description = "Разработка Telegram-бота для поиска и подбора книг, способствующего продвижению имиджа университета и увеличению числа подписчиков.",
                tags = listOf("Backend-разработка", "Анализ текстов", "Python"),
            ),
        ),
        students = listOf(
            RankingItem(
                key = "student-1",
                title = "Семенов Семен Семеныч",
                score = 4.91,
                scoreText = "4.91",
                projectName = "Анализ и прогнозирование манёвра космического аппарата (КА)",
                previousPosition = 2,
                positionDelta = 1,
            ),
            RankingItem(
                key = "student-2",
                title = "Борисов Борис Борисович",
                score = 3.65,
                scoreText = "3.65",
                projectName = "Анализ и прогнозирование манёвра космического аппарата (КА)",
                markerLabel = "Вы",
                previousPosition = 1,
                positionDelta = -1,
            ),
            RankingItem(
                key = "student-3",
                title = "Александров Александр Александрович",
                score = 1.82,
                scoreText = "1.82",
                projectName = "Анализ и прогнозирование манёвра космического аппарата (КА)",
            ),
        ),
        currentUserName = "Борисов Борис Борисович",
        currentUserProjectName = "Анализ и прогнозирование манёвра космического аппарата (КА)",
    )
}
