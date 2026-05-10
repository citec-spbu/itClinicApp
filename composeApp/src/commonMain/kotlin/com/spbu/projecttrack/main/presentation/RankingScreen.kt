@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.spbu.projecttrack.main.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
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
import com.spbu.projecttrack.core.settings.LocalAppUiSettingsController
import com.spbu.projecttrack.core.settings.localizedString
import com.spbu.projecttrack.core.settings.localizeRuntime
import com.spbu.projecttrack.core.storage.createAppPreferences
import com.spbu.projecttrack.core.time.PlatformTime
import com.spbu.projecttrack.core.theme.AppFonts
import com.spbu.projecttrack.core.theme.appPalette
import com.spbu.projecttrack.core.theme.dimText
import com.spbu.projecttrack.core.theme.subtleBorder
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
import com.spbu.projecttrack.rating.data.RankingFilterPersistence
import com.spbu.projecttrack.rating.data.model.rankingDefaultFilters
import com.spbu.projecttrack.rating.presentation.RankingUiState
import com.spbu.projecttrack.rating.presentation.projectstats.StatsDateRangePickerDialog
import com.spbu.projecttrack.rating.presentation.projectstats.StatsDropdownMenu
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

private data class RankingMetricInfo(
    val title: String,
    val whatShows: String,
    val whyNeeded: List<String>,
    val howToInterpret: List<String>,
    val note: String? = null,
)

private val RankingGreen = Color(0xFF209F31)
private val RankingYellow = Color(0xFF9F9220)
private const val RankingPageSize = 10

private fun rankingAccentGradientBottom(accent: Color): Color = lerp(accent, Color.Black, 0.25f)

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
        initialPage = viewModel.savedPage,
        pageCount = { RankingTab.entries.size },
    )

    // Список проектов и список студентов — создаём здесь, чтобы контролировать
    // начальную позицию скролла и сохранять её при уходе со вкладки.
    val projectsListState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.savedProjectsScrollIndex,
        initialFirstVisibleItemScrollOffset = viewModel.savedProjectsScrollOffset,
    )
    val studentsListState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.savedStudentsScrollIndex,
        initialFirstVisibleItemScrollOffset = viewModel.savedStudentsScrollOffset,
    )

    // Сохраняем позицию при уходе с экрана рейтинга
    DisposableEffect(Unit) {
        onDispose {
            viewModel.savedPage = pagerState.currentPage
            viewModel.savedProjectsScrollIndex = projectsListState.firstVisibleItemIndex
            viewModel.savedProjectsScrollOffset = projectsListState.firstVisibleItemScrollOffset
            viewModel.savedStudentsScrollIndex = studentsListState.firstVisibleItemIndex
            viewModel.savedStudentsScrollOffset = studentsListState.firstVisibleItemScrollOffset
        }
    }
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
    var rankingFilterPrefsLoaded by remember { mutableStateOf(false) }
    val appPreferences = remember { createAppPreferences() }

    val language = LocalAppUiSettingsController.current.language
    val noneTemplate = remember(language) {
        RankingFilterTemplate(
            id = "none",
            title = localizeRuntime("Нет", "None"),
            filters = rankingDefaultFilters(),
            isBuiltIn = true,
        )
    }
    val templates = remember(customTemplates) { listOf(noneTemplate) + customTemplates }
    val selectedTab = rankingTabForPage(pagerState.currentPage)

    // Hide/show tab bar in sync with the filter overlay.
    LaunchedEffect(showFiltersScreen) {
        onRootDestinationChange(!showFiltersScreen)
    }

    LaunchedEffect(Unit) {
        val (storedTemplates, storedSelectedId) =
            RankingFilterPersistence.decode(appPreferences.getRankingFilterTemplatesJson())
        customTemplates = storedTemplates
        val validSelected =
            if (storedSelectedId != "none" && storedTemplates.any { it.id == storedSelectedId }) {
                storedSelectedId
            } else {
                "none"
            }
        selectedTemplateId = validSelected
        if (validSelected != "none") {
            storedTemplates.firstOrNull { it.id == validSelected }?.filters?.let { restored ->
                appliedFilters = restored
                draftFilters = restored
            }
        }
        rankingFilterPrefsLoaded = true
    }

    LaunchedEffect(customTemplates, selectedTemplateId, rankingFilterPrefsLoaded) {
        if (!rankingFilterPrefsLoaded) return@LaunchedEffect
        appPreferences.saveRankingFilterTemplatesJson(
            RankingFilterPersistence.encode(customTemplates, selectedTemplateId),
        )
    }

    LaunchedEffect(isAuthorized, rankingFilterPrefsLoaded) {
        if (!rankingFilterPrefsLoaded) return@LaunchedEffect
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
            .background(MaterialTheme.colorScheme.background)
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
                            // Always start fresh – a template selected in the previous
                            // session but not applied should not appear highlighted.
                            selectedTemplateId = "none"
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
                                        val fullSortedItems = sortRankingItems(sourceItems, sortOrder)
                                        val pinnedItem = fullSortedItems.firstOrNull {
                                            !it.markerLabel.isNullOrBlank()
                                        }
                                        val pinnedRank = if (pinnedItem != null) fullSortedItems.indexOf(pinnedItem) + 1 else -1

                                        RankingList(
                                            items = sortedItems,
                                            tab = tab,
                                            pinnedItem = pinnedItem,
                                            pinnedRank = pinnedRank,
                                            resetKey = "${tab.name}|${searchText.trim()}|${sortOrder.name}|${appliedFilters.hashCode()}",
                                            listState = if (tab == RankingTab.Projects) projectsListState else studentsListState,
                                            emptyText = if (tab == RankingTab.Projects) {
                                                localizedString("Нет данных по проектам", "No project data")
                                            } else {
                                                localizedString("Нет данных по студентам", "No student data")
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
                onBack = {
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
                onMetricPeriodSelected = { key, preset ->
                    selectedTemplateId = "none"
                    draftFilters = draftFilters.withMetric(
                        key = key,
                        transform = { it.copy(periodPreset = preset) },
                    )
                },
                onMetricThresholdSelected = { key, preset ->
                    selectedTemplateId = "none"
                    draftFilters = draftFilters.withMetric(
                        key = key,
                        transform = { it.copy(thresholdPreset = preset) },
                    )
                },
                onMetricWeekDaySelected = { key, weekDay ->
                    selectedTemplateId = "none"
                    draftFilters = draftFilters.withMetric(
                        key = key,
                        transform = { it.copy(weekDay = weekDay) },
                    )
                },
                onDateRangeSelected = { startMillis, endMillis ->
                    selectedTemplateId = "none"
                    draftFilters = draftFilters.copy(
                        dateRange = RankingDateRangeFilter(startMillis = startMillis, endMillis = endMillis)
                    )
                },
                onClearDateRange = {
                    selectedTemplateId = "none"
                    draftFilters = draftFilters.copy(dateRange = RankingDateRangeFilter())
                },
                onApply = {
                    appliedFilters = draftFilters
                    showFiltersScreen = false
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
    asWatermark: Boolean = false,
) {
    val alpha =
        if (asWatermark) appPalette().spbuWatermarkLogoAlpha
        else appPalette().spbuBackdropLogoAlpha
    Image(
        painter = painterResource(Res.drawable.spbu_logo),
        contentDescription = null,
        modifier = modifier
            .alpha(alpha)
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
            text = localizedString("Рейтинг", "Rating"),
            fontFamily = AppFonts.OpenSansBold,
            fontSize = 40.sp,
            color = appPalette().title,
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
    val palette = appPalette()
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
                    text = localizedString("Проекты", "Projects"),
                    selected = selectedTab == RankingTab.Projects,
                    onClick = { onTabSelected(RankingTab.Projects) },
                )
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                RankingTabText(
                    text = localizedString("Студенты", "Students"),
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
                .background(palette.border),
        )
    }
}

@Composable
private fun RankingTabText(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = appPalette()
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
        color = if (selected) palette.primaryText else palette.secondaryText,
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
    val palette = appPalette()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
            )
            .border(
                width = 1.dp,
                color = palette.subtleBorder,
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
                color = palette.primaryText,
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
                        text = localizedString("Поиск", "Search"),
                        fontFamily = AppFonts.OpenSansSemiBold,
                        fontSize = 16.sp,
                        color = palette.dimText,
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
            contentDescription = localizedString("Открыть фильтры", "Open filters"),
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
                    .background(appPalette().accent, CircleShape),
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
                localizedString("Сортировка по убыванию", "Sort descending")
            } else {
                localizedString("Сортировка по возрастанию", "Sort ascending")
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
    val palette = appPalette()
    Box(
        modifier = Modifier
            .background(palette.disabledButton, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            fontFamily = AppFonts.OpenSansSemiBold,
            fontSize = 15.sp,
            color = palette.buttonText,
            lineHeight = 10.sp,
            letterSpacing = 0.15.sp,
            maxLines = 1,
        )
    }
}


@Composable
private fun RankingUnauthorizedState() {
    val palette = appPalette()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = localizedString("Рейтинг недоступен", "Rating unavailable"),
            fontFamily = AppFonts.OpenSansBold,
            fontSize = 22.sp,
            color = palette.title,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = localizedString("Авторизуйтесь, чтобы увидеть рейтинг проектов и студентов.", "Sign in to view project and student rankings."),
            fontFamily = AppFonts.OpenSansMedium,
            fontSize = 16.sp,
            color = palette.secondaryText,
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
    val palette = appPalette()
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
                color = palette.accent,
                strokeWidth = 2.5.dp,
            )
        } else {
            CircularProgressIndicator(
                progress = { minOf(1f, state.distanceFraction) },
                modifier = Modifier.size(28.dp),
                color = palette.accent,
                strokeWidth = 2.5.dp,
                trackColor = palette.dimText.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun LoadingContent(
    modifier: Modifier = Modifier,
) {
    val palette = appPalette()
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(color = palette.accent)
            Text(
                text = localizedString("Загрузка рейтинга...", "Loading..."),
                fontFamily = AppFonts.OpenSansMedium,
                fontSize = 14.sp,
                color = palette.secondaryText,
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
    val palette = appPalette()
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = localizedString("Ошибка загрузки", "Load error"),
                fontFamily = AppFonts.OpenSansBold,
                fontSize = 18.sp,
                color = palette.accent,
            )
            Text(
                text = message,
                fontFamily = AppFonts.OpenSansRegular,
                fontSize = 14.sp,
                color = palette.secondaryText,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Button(onClick = onRetry) {
                Text(
                    text = localizedString("Повторить", "Retry"),
                    fontFamily = AppFonts.OpenSansMedium,
                )
            }
        }
    }
}

@Composable
private fun RankingList(
    items: List<RankingItem>,
    tab: RankingTab,
    resetKey: String,
    listState: LazyListState,
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
                color = appPalette().secondaryText,
            )
        }
        return
    }

    var visibleCount by remember(resetKey) {
        mutableStateOf(minOf(RankingPageSize, items.size))
    }

    // Флаг: пропускаем скролл при первой композиции (восстановление позиции),
    // но скроллим в начало когда resetKey реально меняется (поиск/фильтр/сортировка).
    var isFirstComposition by remember { mutableStateOf(true) }

    LaunchedEffect(resetKey) {
        if (isFirstComposition) {
            isFirstComposition = false
        } else {
            listState.scrollToItem(0)
        }
        visibleCount = minOf(RankingPageSize, items.size)
    }

    LaunchedEffect(items.size) {
        visibleCount = minOf(RankingPageSize, items.size)
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
                    text = if (tab == RankingTab.Projects) localizedString("Ваш проект", "Your project") else localizedString("Ваша позиция", "Your position"),
                    fontFamily = AppFonts.OpenSansSemiBold,
                    fontSize = 11.sp,
                    color = appPalette().accent,
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
    val palette = appPalette()
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
                .background(palette.subtleBorder),
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
                        color = scoreGradientColor(
                            item.score,
                            palette.accent,
                            palette.secondaryText,
                        ),
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
    val palette = appPalette()
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
            color = palette.secondaryText,
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
                        localizedString("Поднялся в рейтинге", "Moved up in ranking")
                    } else {
                        localizedString("Опустился в рейтинге", "Moved down in ranking")
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
    val accent = appPalette().accent
    val titleText = remember(item.title, item.markerLabel, accent) {
        buildAnnotatedString {
            append(item.title)
            item.markerLabel?.takeIf { it.isNotBlank() }?.let { marker ->
                append(" ")
                withStyle(SpanStyle(color = accent)) {
                    append("($marker)")
                }
            }
        }
    }

    Text(
        text = titleText,
        fontFamily = AppFonts.OpenSansBold,
        fontSize = 16.sp,
        color = appPalette().primaryText,
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
    val palette = appPalette()
    Column {
        Text(
            text = item.description?.takeIf { it.isNotBlank() } ?: localizedString("Описание проекта пока недоступно", "Project description not yet available"),
            fontFamily = AppFonts.OpenSansRegular,
            fontSize = 10.sp,
            color = palette.secondaryText,
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
    val palette = appPalette()
    val projectText = buildAnnotatedString {
        withStyle(
            SpanStyle(
                fontFamily = AppFonts.OpenSansBold,
                color = palette.primaryText,
            ),
        ) {
            append(localizeRuntime("Проект:", "Project:"))
        }
        append(" ")
        append(item.projectName?.takeIf { it.isNotBlank() } ?: localizeRuntime("Не определен", "Not defined"))
    }

    Text(
        text = projectText,
        fontFamily = AppFonts.OpenSansRegular,
        fontSize = 10.sp,
        color = palette.secondaryText,
        lineHeight = 10.sp,
    )
}

@Composable
private fun RankingTagChip(
    text: String,
) {
    val palette = appPalette()
    Box(
        modifier = Modifier
            .border(1.dp, palette.subtleBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 5.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            fontFamily = AppFonts.OpenSansSemiBold,
            fontSize = 10.sp,
            color = palette.secondaryText,
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
    useAccentGradient: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "ranking_action_scale",
    )
    val gradientColors = if (useAccentGradient) {
        listOf(backgroundColor, rankingAccentGradientBottom(backgroundColor))
    } else {
        listOf(backgroundColor, backgroundColor)
    }

    Box(
        modifier = modifier
            .height(50.dp)
            .scale(scale)
            .alpha(if (enabled) 1f else 0.5f)
            .background(
                brush = Brush.verticalGradient(colors = gradientColors),
                shape = RoundedCornerShape(10.dp),
            )
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
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
            color = appPalette().buttonText,
            letterSpacing = 0.16.sp,
        )
    }
}

// Save button used exclusively in SaveTemplateDialog — matches Figma node 632:1827.
@Composable
private fun SaveDialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = appPalette()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "save_dialog_btn_scale",
    )

    Box(
        modifier = modifier
            .scale(scale)
            .alpha(if (enabled) 1f else 0.45f)
            .dropShadow(
                shape = RoundedCornerShape(15.dp),
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.07f),
                    offset = DpOffset(x = 0.dp, y = 2.dp),
                    radius = 4.dp,
                ),
            )
            .background(
                Brush.verticalGradient(
                    listOf(palette.accent, rankingAccentGradientBottom(palette.accent)),
                ),
                RoundedCornerShape(15.dp),
            )
            .border(1.dp, palette.accentBorder, RoundedCornerShape(15.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 10.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontFamily = AppFonts.OpenSansSemiBold,
            fontSize = 15.sp,
            color = palette.buttonText,
            letterSpacing = 0.15.sp,
        )
    }
}

@Composable
private fun RankingFiltersScreen(
    filters: RankingFilters,
    templates: List<RankingFilterTemplate>,
    selectedTemplateId: String,
    onBack: () -> Unit,
    onTemplateSelected: (RankingFilterTemplate) -> Unit,
    onMetricToggle: (RankingMetricKey) -> Unit,
    onClearMetrics: () -> Unit,
    onMetricInfoClick: (RankingMetricKey) -> Unit,
    onMetricPeriodSelected: (RankingMetricKey, RankingPeriodPreset) -> Unit,
    onMetricThresholdSelected: (RankingMetricKey, RankingThresholdPreset) -> Unit,
    onMetricWeekDaySelected: (RankingMetricKey, RankingWeekDay) -> Unit,
    onDateRangeSelected: (Long?, Long?) -> Unit,
    onClearDateRange: () -> Unit,
    onApply: () -> Unit,
    onSaveTemplate: () -> Unit,
) {
    val scrollState = rememberScrollState()
    var showDateRangeCalendar by remember { mutableStateOf(false) }
    val screenBg = MaterialTheme.colorScheme.background
    val onBg = MaterialTheme.colorScheme.onBackground
    val palette = appPalette()

    // Scroll fade helpers
    val canScrollUp by remember { derivedStateOf { scrollState.value > 0 } }
    val canScrollDown by remember { derivedStateOf { scrollState.value < scrollState.maxValue } }
    val topFadeAlpha by animateFloatAsState(
        targetValue = if (canScrollUp) 1f else 0f,
        animationSpec = tween(200),
        label = "filtersScrollTopFade",
    )
    val bottomFadeAlpha by animateFloatAsState(
        targetValue = if (canScrollDown) 1f else 0f,
        animationSpec = tween(200),
        label = "filtersScrollBottomFade",
    )

    // Back button animation
    val backInteraction = remember { MutableInteractionSource() }
    val isBackPressed by backInteraction.collectIsPressedAsState()
    val backScale by animateFloatAsState(
        targetValue = if (isBackPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "filtersBackScale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .imePadding()
            // Consume all taps so they don't fall through to the screen behind.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        RankingBackgroundLogo()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Bottom
                    )
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 0.dp),
            ) {
                Image(
                    painter = painterResource(Res.drawable.arrow_back),
                    contentDescription = localizedString("Назад", "Back"),
                    modifier = Modifier
                        .padding(start = 9.dp, top = 14.dp)
                        .size(24.dp)
                        .scale(backScale)
                        .clickable(
                            interactionSource = backInteraction,
                            indication = null,
                            onClick = onBack,
                        ),
                )
                Text(
                    text = localizedString("Фильтры", "Filters"),
                    fontFamily = AppFonts.OpenSansBold,
                    fontSize = 40.sp,
                    color = palette.title,
                    letterSpacing = 0.4.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        val fadeH = RankingFiltersScrollFadeWidth.toPx()
                        // top fade
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 1f - topFadeAlpha),
                                    Color.Black,
                                ),
                                startY = 0f,
                                endY = fadeH.coerceAtMost(size.height),
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                        // bottom fade
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black,
                                    Color.Black.copy(alpha = 1f - bottomFadeAlpha),
                                ),
                                startY = (size.height - fadeH).coerceAtLeast(0f),
                                endY = size.height,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 15.dp),
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Templates section – only shown when user has saved templates
                    if (templates.isNotEmpty()) {
                        Text(
                            text = localizedString("Шаблоны", "Templates"),
                            fontFamily = AppFonts.OpenSansSemiBold,
                            fontSize = 15.sp,
                            color = onBg,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val templateListState = rememberLazyListState()
                        val tmplCanBack by remember { derivedStateOf { templateListState.canScrollBackward } }
                        val tmplCanFwd by remember { derivedStateOf { templateListState.canScrollForward } }
                        val tmplLeftAlpha by animateFloatAsState(
                            targetValue = if (tmplCanBack) 1f else 0f,
                            animationSpec = tween(200),
                            label = "tmplLeftFade",
                        )
                        val tmplRightAlpha by animateFloatAsState(
                            targetValue = if (tmplCanFwd) 1f else 0f,
                            animationSpec = tween(200),
                            label = "tmplRightFade",
                        )
                        // No Offscreen compositing so the dropShadow on each chip can
                        // draw outside its own bounds without being clipped by the bitmap.
                        // Scroll fades are white-gradient overlays instead of DstIn.
                        Box(modifier = Modifier.fillMaxWidth()) {
                            LazyRow(
                                state = templateListState,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                // 8 dp start/end so the first/last chip's shadow is never
                                // clipped by the LazyRow's left/right edge.
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                items(templates) { template ->
                                    RankingTemplateChip(
                                        title = template.title,
                                        selected = selectedTemplateId == template.id,
                                        onClick = { onTemplateSelected(template) },
                                    )
                                }
                            }
                            // Left fade overlay – matches filter-screen background
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .width(RankingFiltersScrollFadeWidth)
                                    .fillMaxHeight()
                                    .alpha(tmplLeftAlpha)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(screenBg, Color.Transparent),
                                        )
                                    ),
                            )
                            // Right fade overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .width(RankingFiltersScrollFadeWidth)
                                    .fillMaxHeight()
                                    .alpha(tmplRightAlpha)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color.Transparent, screenBg),
                                        )
                                    ),
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = localizedString("Метрики", "Metrics"),
                            fontFamily = AppFonts.OpenSansSemiBold,
                            fontSize = 15.sp,
                            color = onBg,
                        )
                        SmallPillButton(
                            text = localizedString("Очистить", "Clear"),
                            onClick = onClearMetrics,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = localizedString("Выберите метрики, по которым будет составляться общая оценка:", "Select the metrics to use for the overall score:"),
                        fontFamily = AppFonts.OpenSansMedium,
                        fontSize = 14.sp,
                        color = palette.secondaryText,
                        lineHeight = 16.sp,
                        modifier = Modifier.widthIn(max = 375.dp),
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    RankingMetricKey.entries.forEach { metricKey ->
                        val metricFilter = filters.metric(metricKey)
                        RankingMetricRow(
                            metricKey = metricKey,
                            metricFilter = metricFilter,
                            onToggle = { onMetricToggle(metricKey) },
                            onInfoClick = { onMetricInfoClick(metricKey) },
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
                            text = localizedString("Диапазон дат", "Date range"),
                            fontFamily = AppFonts.OpenSansSemiBold,
                            fontSize = 15.sp,
                            color = onBg,
                        )
                        SmallPillButton(
                            text = localizedString("Очистить", "Clear"),
                            onClick = onClearDateRange,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    RankingDateRangeField(
                        dateRange = filters.dateRange,
                        onClick = { showDateRangeCalendar = true },
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 19.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                RankingActionButton(
                    text = localizedString("Применить", "Apply"),
                    backgroundColor = palette.disabledButton,
                    borderColor = palette.subtleBorder,
                    onClick = onApply,
                    modifier = Modifier.weight(1f),
                )
                RankingActionButton(
                    text = localizedString("Сохранить шаблон", "Save template"),
                    backgroundColor = palette.accent,
                    borderColor = palette.accentBorder,
                    useAccentGradient = true,
                    onClick = onSaveTemplate,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (showDateRangeCalendar) {
        StatsDateRangePickerDialog(
            initialStartIsoDate = filters.dateRange.startMillis?.toRankingIsoDate() ?: rankingTodayIsoDate(),
            initialEndIsoDate = filters.dateRange.endMillis?.toRankingIsoDate() ?: rankingTodayIsoDate(),
            onDismiss = { showDateRangeCalendar = false },
            onConfirm = { startIso, endIso ->
                onDateRangeSelected(
                    rankingIsoDateToMillis(startIso),
                    rankingIsoDateToMillis(endIso),
                )
            },
        )
    }
}

@Composable
private fun RankingTemplateChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = appPalette()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "templateChipScale",
    )

    Box(
        modifier = Modifier
            .height(30.dp)
            .scale(scale)
            .dropShadow(
                shape = RoundedCornerShape(10.dp),
                shadow = Shadow(
                    color = if (selected) palette.accent.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.15f),
                    offset = DpOffset(x = 0.dp, y = if (selected) 3.dp else 2.dp),
                    radius = if (selected) 6.dp else 3.dp,
                )
            )
            .background(
                color = if (selected) palette.accent else palette.disabledButton,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            fontFamily = AppFonts.OpenSansSemiBold,
            fontSize = 13.sp,
            color = palette.buttonText,
            letterSpacing = 0.15.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun RankingMetricRow(
    metricKey: RankingMetricKey,
    metricFilter: RankingMetricFilter,
    onToggle: () -> Unit,
    onInfoClick: () -> Unit,
    onPeriodSelected: (RankingPeriodPreset) -> Unit,
    onThresholdSelected: (RankingThresholdPreset) -> Unit,
    onWeekDaySelected: (RankingWeekDay) -> Unit,
) {
    var showPeriodPicker by remember { mutableStateOf(false) }
    var showThresholdPicker by remember { mutableStateOf(false) }
    var showWeekDayMenu by remember { mutableStateOf(false) }

    // Checkbox (24dp) + Spacer (2dp) = 26dp → sub-chip start offset
    val subFieldStartPadding = 26.dp
    val palette = appPalette()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                color = palette.primaryText,
                lineHeight = 10.sp,
                modifier = Modifier.weight(1f),
            )
            RankingInfoBadge(onClick = onInfoClick)
        }

        // Period picker chip (Commits, Issues, Pull Requests, PerformanceGrade)
        AnimatedVisibility(
            visible = metricKey.supportsPeriod && metricFilter.enabled,
            enter = expandVertically(animationSpec = spring(stiffness = 600f)) + fadeIn(tween(180)),
            exit = shrinkVertically(animationSpec = spring(stiffness = 600f)) + fadeOut(tween(120)),
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                RankingSubFieldChip(
                    text = metricFilter.periodPreset.label,
                    onClick = { showPeriodPicker = true },
                    modifier = Modifier.padding(start = subFieldStartPadding),
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        // Threshold picker chip (RapidPullRequests)
        AnimatedVisibility(
            visible = metricKey.supportsThreshold && metricFilter.enabled,
            enter = expandVertically(animationSpec = spring(stiffness = 600f)) + fadeIn(tween(180)),
            exit = shrinkVertically(animationSpec = spring(stiffness = 600f)) + fadeOut(tween(120)),
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                RankingSubFieldChip(
                    text = metricFilter.thresholdPreset.label,
                    onClick = { showThresholdPicker = true },
                    modifier = Modifier.padding(start = subFieldStartPadding),
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        // Week day dropdown chip (DominantWeekDay)
        AnimatedVisibility(
            visible = metricKey.supportsWeekDay && metricFilter.enabled,
            enter = expandVertically(animationSpec = spring(stiffness = 600f)) + fadeIn(tween(180)),
            exit = shrinkVertically(animationSpec = spring(stiffness = 600f)) + fadeOut(tween(120)),
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.padding(start = subFieldStartPadding)) {
                    RankingSubFieldChip(
                        text = metricFilter.weekDay.label,
                        onClick = { showWeekDayMenu = true },
                    )
                    StatsDropdownMenu(
                        expanded = showWeekDayMenu,
                        onDismissRequest = { showWeekDayMenu = false },
                        options = RankingWeekDay.entries.map { it.backendValue to it.label },
                        onSelected = { key ->
                            val day = RankingWeekDay.entries.firstOrNull { it.backendValue == key }
                            if (day != null) onWeekDaySelected(day)
                        },
                        selectedKey = metricFilter.weekDay.backendValue,
                        width = 200.dp,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }

    if (showPeriodPicker) {
        RankingWheelPickerDialog(
            title = localizedString("Период", "Period"),
            options = RankingPeriodPreset.entries.map { it.label },
            selectedIndex = RankingPeriodPreset.entries.indexOf(metricFilter.periodPreset).coerceAtLeast(0),
            onDismiss = { showPeriodPicker = false },
            onConfirm = { idx ->
                val preset = RankingPeriodPreset.entries.getOrNull(idx) ?: metricFilter.periodPreset
                onPeriodSelected(preset)
            },
        )
    }

    if (showThresholdPicker) {
        RankingWheelPickerDialog(
            title = localizedString("Продолжительность", "Duration"),
            options = RankingThresholdPreset.entries.map { it.label },
            selectedIndex = RankingThresholdPreset.entries.indexOf(metricFilter.thresholdPreset).coerceAtLeast(0),
            onDismiss = { showThresholdPicker = false },
            onConfirm = { idx ->
                val preset = RankingThresholdPreset.entries.getOrNull(idx) ?: metricFilter.thresholdPreset
                onThresholdSelected(preset)
            },
        )
    }
}

/** Simple chip button used for sub-fields (period, threshold, weekday). */
@Composable
private fun RankingSubFieldChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = appPalette()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "subFieldChipScale",
    )
    Box(
        modifier = modifier
            .height(30.dp)
            .scale(scale)
            .border(1.dp, palette.subtleBorder, RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontFamily = AppFonts.OpenSansBold,
            fontSize = 14.sp,
            color = palette.secondaryText,
            lineHeight = 10.sp,
        )
    }
}

@Composable
private fun RankingWheelPickerDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val itemHeightDp = 48.dp
    val visibleCount = 5
    val paddingCount = visibleCount / 2  // 2 blank rows top + bottom so every option can reach centre

    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeightDp.toPx() }

    val clampedSelected = selectedIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))
    // scroll = optionIndex * itemHeightPx keeps the chosen option centred in the viewport
    val scrollState = rememberScrollState(initial = (clampedSelected * itemHeightPx).toInt())

    // Derive selected index from scroll position (nearest item)
    val pickedIndex by remember {
        derivedStateOf {
            ((scrollState.value.toFloat() / itemHeightPx) + 0.5f)
                .toInt()
                .coerceIn(0, (options.size - 1).coerceAtLeast(0))
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val palette = appPalette()
    val dialogSurface = MaterialTheme.colorScheme.surface

    // After every scroll-stop, animate to the exact snap position
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.isScrollInProgress }
            .collect { scrolling ->
                if (!scrolling) {
                    val target = (pickedIndex * itemHeightPx).toInt()
                    if (scrollState.value != target) {
                        scrollState.animateScrollTo(target)
                    }
                }
            }
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    fun dismiss() {
        visible = false
        coroutineScope.launch {
            kotlinx.coroutines.delay(200)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = ::dismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.92f, animationSpec = tween(200)),
            exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.96f, animationSpec = tween(180)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .widthIn(max = 320.dp)
                    .background(dialogSurface, RoundedCornerShape(20.dp))
                    .border(1.dp, palette.subtleBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                RankingBackgroundLogo(
                    modifier = Modifier.matchParentSize(),
                    asWatermark = true,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        fontFamily = AppFonts.OpenSansBold,
                        fontSize = 18.sp,
                        color = palette.primaryText,
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Wheel viewport
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeightDp * visibleCount),
                    ) {
                        // Scrollable column: paddingCount blank rows + real options + paddingCount blank rows
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState),
                        ) {
                            repeat(paddingCount) {
                                Spacer(modifier = Modifier.height(itemHeightDp))
                            }
                            options.forEachIndexed { index, option ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(itemHeightDp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val isCenter = index == pickedIndex
                                    Text(
                                        text = option,
                                        fontFamily = if (isCenter) AppFonts.OpenSansBold else AppFonts.OpenSansRegular,
                                        fontSize = if (isCenter) 17.sp else 14.sp,
                                        color = if (isCenter) palette.accent else palette.secondaryText,
                                        maxLines = 1,
                                    )
                                }
                            }
                            repeat(paddingCount) {
                                Spacer(modifier = Modifier.height(itemHeightDp))
                            }
                        }
                        // Top fade (surface gradient masks items scrolling away at the top)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeightDp * paddingCount)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(dialogSurface, Color.Transparent),
                                    )
                                ),
                        )
                        // Bottom fade
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeightDp * paddingCount)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, dialogSurface),
                                    )
                                ),
                        )
                        // Selection indicator frame at the vertical centre
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeightDp)
                                .align(Alignment.Center)
                                .border(1.dp, palette.subtleBorder, RoundedCornerShape(8.dp)),
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(13.dp),
                    ) {
                        RankingActionButton(
                            text = localizedString("Отмена", "Cancel"),
                            backgroundColor = palette.disabledButton,
                            borderColor = palette.subtleBorder,
                            onClick = ::dismiss,
                            modifier = Modifier.weight(1f),
                        )
                        RankingActionButton(
                            text = localizedString("Выбрать", "Select"),
                            backgroundColor = palette.accent,
                            borderColor = palette.accentBorder,
                            useAccentGradient = true,
                            onClick = {
                                onConfirm(pickedIndex)
                                dismiss()
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingCheckbox(
    checked: Boolean,
    onClick: () -> Unit,
) {
    val palette = appPalette()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "checkboxScale",
    )
    Box(
        modifier = Modifier
            .size(24.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(
                    color = if (checked) palette.disabledButton else Color.Transparent,
                    shape = RoundedCornerShape(2.dp),
                )
                .border(
                    width = if (checked) 0.dp else 2.dp,
                    color = palette.border,
                    shape = RoundedCornerShape(2.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Text(
                    text = "✓",
                    fontFamily = AppFonts.OpenSansBold,
                    fontSize = 13.sp,
                    color = palette.buttonText,
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
    val palette = appPalette()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.82f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "infoBadgeScale",
    )
    Box(
        modifier = Modifier
            .size(15.dp)
            .scale(scale)
            .border(1.dp, palette.border, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "i",
            fontFamily = AppFonts.OpenSansRegular,
            fontSize = 12.sp,
            color = palette.secondaryText,
            lineHeight = 10.sp,
        )
    }
}

@Composable
private fun RankingDateRangeField(
    dateRange: RankingDateRangeFilter,
    onClick: () -> Unit,
) {
    val palette = appPalette()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "dateRangeFieldScale",
    )
    Row(
        modifier = Modifier
            .width(200.dp)
            .height(30.dp)
            .scale(scale)
            .border(1.dp, palette.subtleBorder, RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatDateRangeField(dateRange),
            fontFamily = AppFonts.OpenSansMedium,
            fontSize = 12.sp,
            color = palette.secondaryText,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        Image(
            painter = painterResource(Res.drawable.calendar_icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SmallPillButton(
    text: String,
    onClick: () -> Unit,
) {
    val palette = appPalette()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "pillButtonScale",
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .background(
                Brush.verticalGradient(
                    listOf(palette.accent, rankingAccentGradientBottom(palette.accent)),
                ),
                RoundedCornerShape(15.dp),
            )
            .border(1.dp, palette.accentBorder, RoundedCornerShape(15.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 4.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            fontFamily = AppFonts.OpenSansSemiBold,
            fontSize = 10.sp,
            color = palette.buttonText,
            lineHeight = 10.sp,
        )
    }
}

@Composable
private fun SaveTemplateDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val maxNameLength = 50
    var templateName by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val palette = appPalette()
    val dialogSurface = MaterialTheme.colorScheme.surface
    LaunchedEffect(Unit) { visible = true }

    // Close-button press animation
    val closeInteraction = remember { MutableInteractionSource() }
    val closePressed by closeInteraction.collectIsPressedAsState()
    val closeScale by animateFloatAsState(
        targetValue = if (closePressed) 0.78f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "save_dlg_close_scale",
    )

    fun dismiss() {
        visible = false
        coroutineScope.launch {
            kotlinx.coroutines.delay(160)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = ::dismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(160)) + scaleIn(initialScale = 0.93f, animationSpec = tween(160)),
            exit  = fadeOut(tween(140)) + scaleOut(targetScale = 0.96f, animationSpec = tween(140)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .widthIn(max = 350.dp)
                    .background(dialogSurface, RoundedCornerShape(20.dp))
                    .border(1.dp, palette.subtleBorder, RoundedCornerShape(20.dp))
                    // No padding here — logo fills the full Box, clipped by rounded corners
                    .clip(RoundedCornerShape(20.dp)),
            ) {
                // Logo fills the entire dialog background independently of content padding
                Image(
                    painter = painterResource(Res.drawable.spbu_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(appPalette().spbuBackdropLogoAlpha),
                    contentScale = ContentScale.FillWidth,
                )
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Title row with animated close button
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = localizedString("Сохранить шаблон", "Save template"),
                            fontFamily = AppFonts.OpenSansBold,
                            fontSize = 24.sp,
                            color = palette.primaryText,
                            modifier = Modifier.align(Alignment.Center),
                        )
                        Image(
                            painter = painterResource(Res.drawable.close_icon),
                            contentDescription = localizedString("Закрыть", "Close"),
                            modifier = Modifier
                                .size(24.dp)
                                .scale(closeScale)
                                .align(Alignment.CenterEnd)
                                .clickable(
                                    interactionSource = closeInteraction,
                                    indication = null,
                                    onClick = ::dismiss,
                                ),
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    // Text field with 50-char limit, text centered
                    BasicTextField(
                        value = templateName,
                        onValueChange = { if (it.length <= maxNameLength) templateName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                        textStyle = TextStyle(
                            fontFamily = AppFonts.OpenSansRegular,
                            fontSize = 12.sp,
                            color = palette.primaryText,
                            textAlign = TextAlign.Center,
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
                                        text = localizedString("Введите название шаблона", "Enter template name"),
                                        fontFamily = AppFonts.OpenSansRegular,
                                        fontSize = 12.sp,
                                        color = palette.dimText,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(palette.subtleBorder),
                        )
                        // Character counter — appears when user starts typing
                        if (templateName.isNotEmpty()) {
                            Text(
                                text = "${templateName.length}/$maxNameLength",
                                fontFamily = AppFonts.OpenSansRegular,
                                fontSize = 10.sp,
                                color = if (templateName.length >= maxNameLength) palette.accent else palette.dimText,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(top = 4.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                    SaveDialogButton(
                        text = localizedString("Сохранить", "Save"),
                        enabled = templateName.isNotBlank(),
                        onClick = { onSave(templateName) },
                        modifier = Modifier.fillMaxWidth(),
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
    val coroutineScope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val palette = appPalette()
    val dialogSurface = MaterialTheme.colorScheme.surface
    val onBg = MaterialTheme.colorScheme.onSurface

    fun dismiss() {
        visible = false
        coroutineScope.launch {
            kotlinx.coroutines.delay(160)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = ::dismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(160)) + scaleIn(initialScale = 0.93f, animationSpec = tween(160)),
            exit  = fadeOut(tween(140)) + scaleOut(targetScale = 0.96f, animationSpec = tween(140)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 350.dp)
                    .background(dialogSurface, RoundedCornerShape(20.dp))
                    .border(1.dp, palette.subtleBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 12.dp),
            ) {
                RankingBackgroundLogo(
                    modifier = Modifier.matchParentSize(),
                    asWatermark = true,
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
                            color = onBg,
                            modifier = Modifier.align(Alignment.CenterStart),
                        )
                        Image(
                            painter = painterResource(Res.drawable.close_icon),
                            contentDescription = localizedString("Закрыть", "Close"),
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.CenterEnd)
                                .clickable(onClick = ::dismiss),
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    MetricInfoSection(
                        title = localizedString("Что показывает:", "What it shows:"),
                        lines = listOf(info.whatShows),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MetricInfoSection(
                        title = localizedString("Зачем нужна:", "Why it matters:"),
                        lines = info.whyNeeded,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MetricInfoSection(
                        title = localizedString("Как интерпретировать:", "How to interpret:"),
                        lines = info.howToInterpret,
                    )
                    info.note?.let { note ->
                        Spacer(modifier = Modifier.height(12.dp))
                        val noteLabel = localizedString("Примечание:", "Note:")
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        fontFamily = AppFonts.OpenSansBold,
                                        color = palette.secondaryText,
                                    )
                                ) {
                                    append(noteLabel)
                                }
                                append(" ")
                                append(note)
                            },
                            fontFamily = AppFonts.OpenSansRegular,
                            fontSize = 12.sp,
                            color = palette.secondaryText,
                            lineHeight = 12.sp,
                        )
                    }
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
    val palette = appPalette()
    Text(
        text = title,
        fontFamily = AppFonts.OpenSansBold,
        fontSize = 12.sp,
        color = palette.secondaryText,
        lineHeight = 12.sp,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { line ->
            Text(
                text = line,
                fontFamily = AppFonts.OpenSansRegular,
                fontSize = 12.sp,
                color = palette.secondaryText,
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
            whatShows = localizeRuntime(
                "Сырой список всех коммитов за период: автор, дата, затронутые файлы. Это источник данных, а не самостоятельная оценка.",
                "Raw list of all commits for the period: author, date, affected files. This is source data, not a standalone metric.",
            ),
            whyNeeded = listOf(
                localizeRuntime("Служит основой для метрик Total Commits, Code Churn и Code Ownership.", "Serves as the foundation for Total Commits, Code Churn, and Code Ownership metrics."),
                localizeRuntime("Позволяет видеть регулярность и ритм разработки без агрегации.", "Lets you see development regularity and rhythm without aggregation."),
            ),
            howToInterpret = listOf(
                localizeRuntime("Регулярные коммиты небольшого размера — признак здоровой разработки.", "Regular small commits are a sign of healthy development."),
                localizeRuntime("Длинные паузы между коммитами могут говорить о блокерах или завершении фазы.", "Long gaps between commits may indicate blockers or phase completion."),
            ),
            note = localizeRuntime(
                "Метрика не оценивается напрямую. Оценка строится через производные: Total Commits, Code Churn, Code Ownership.",
                "This metric is not scored directly. Scores are derived through: Total Commits, Code Churn, Code Ownership.",
            ),
        )

        RankingMetricKey.Issues -> RankingMetricInfo(
            title = "Issues",
            whatShows = localizeRuntime(
                "Сырой список задач за период: созданные, открытые, закрытые, исполнители. Это источник данных, а не самостоятельная оценка.",
                "Raw list of issues for the period: created, open, closed, assignees. This is source data, not a standalone metric.",
            ),
            whyNeeded = listOf(
                localizeRuntime("Показывает, как команда управляет бэклогом задач.", "Shows how the team manages the issue backlog."),
                localizeRuntime("Используется для расчёта Issue Completeness и Dominant Week Day.", "Used to calculate Issue Completeness and Dominant Week Day."),
            ),
            howToInterpret = listOf(
                localizeRuntime("Большое число незакрытых задач при малом числе новых — признак накопленного долга.", "Many unclosed issues with few new ones indicates accumulated debt."),
                localizeRuntime("Баланс открытых и закрытых задач говорит о стабильном процессе.", "A balance of open and closed issues indicates a stable process."),
            ),
            note = localizeRuntime(
                "Метрика не оценивается напрямую. Оценка строится через производные: Issue Completeness, Dominant Week Day.",
                "This metric is not scored directly. Scores are derived through: Issue Completeness, Dominant Week Day.",
            ),
        )

        RankingMetricKey.PullRequests -> RankingMetricInfo(
            title = "Pull Requests",
            whatShows = localizeRuntime(
                "Сырой список pull request за период: автор, время создания и закрытия. Это источник данных, а не самостоятельная оценка.",
                "Raw list of pull requests for the period: author, creation and close time. This is source data, not a standalone metric.",
            ),
            whyNeeded = listOf(
                localizeRuntime("Показывает, как команда проводит code review.", "Shows how the team conducts code reviews."),
                localizeRuntime("Используется для расчёта PR Hang Time и Rapid Pull Requests.", "Used to calculate PR Hang Time and Rapid Pull Requests."),
            ),
            howToInterpret = listOf(
                localizeRuntime("Быстро закрывающиеся PR без пометки «rapid» — признак хорошего ревью.", "Quickly closed PRs without the \"rapid\" flag are a sign of good reviews."),
                localizeRuntime("Слишком быстрые закрытия могут означать формальный или пропущенный ревью.", "Very fast closures may indicate a perfunctory or skipped review."),
            ),
            note = localizeRuntime(
                "Метрика не оценивается напрямую. Оценка строится через производные: PR Hang Time, Rapid Pull Requests.",
                "This metric is not scored directly. Scores are derived through: PR Hang Time, Rapid Pull Requests.",
            ),
        )

        RankingMetricKey.PerformanceGrade -> RankingMetricInfo(
            title = localizeRuntime("Оценка производительности", "Performance Grade"),
            whatShows = localizeRuntime(
                "Итоговую оценку ресурса по проекту, рассчитанную на основе всех включённых метрик с учётом их весов.",
                "The final resource score for the project, calculated from all enabled metrics weighted accordingly.",
            ),
            whyNeeded = listOf(
                localizeRuntime("Даёт единое число для быстрого сравнения ресурсов.", "Provides a single number for quick resource comparison."),
                localizeRuntime("Агрегирует все включённые метрики с их весами в одну шкалу от 2 до 5.", "Aggregates all enabled metrics with their weights into a single 2–5 scale."),
            ),
            howToInterpret = listOf(
                localizeRuntime("5.0 — все включённые метрики на максимуме.", "5.0 — all enabled metrics are at maximum."),
                localizeRuntime("4.0–4.9 — высокий уровень, незначительные отклонения.", "4.0–4.9 — high level, minor deviations."),
                localizeRuntime("3.0–3.9 — средний уровень, есть зоны роста.", "3.0–3.9 — mid level, room for improvement."),
                localizeRuntime("2.0–2.9 — низкий уровень, требуется анализ и улучшение процессов.", "2.0–2.9 — low level, process analysis and improvement required."),
            ),
            note = localizeRuntime(
                "Оценка берётся из панели администратора как взвешенное среднее по всем активным метрикам.",
                "Score is taken from the admin panel as a weighted average across all active metrics.",
            ),
        )

        RankingMetricKey.TotalCommits -> RankingMetricInfo(
            title = localizeRuntime("Общее количество коммитов", "Total Commits"),
            whatShows = localizeRuntime(
                "Среднюю частоту коммитов в день на одного участника команды за выбранный период.",
                "Average daily commit frequency per team member over the selected period.",
            ),
            whyNeeded = listOf(
                localizeRuntime("Показывает темп разработки с поправкой на размер команды.", "Shows development pace adjusted for team size."),
                localizeRuntime("Позволяет сравнивать активность между периодами и ресурсами.", "Allows comparing activity between periods and resources."),
            ),
            howToInterpret = listOf(
                localizeRuntime("≥ 0.33 коммита в день на человека → оценка 5 (максимум).", "≥ 0.33 commits per person per day → score 5 (maximum)."),
                localizeRuntime("0 коммитов → оценка 2 (минимум).", "0 commits → score 2 (minimum)."),
                localizeRuntime("Линейный рост: каждые ~0.033 коммита в день на человека добавляют ~0.3 балла.", "Linear growth: every ~0.033 commits per person per day adds ~0.3 points."),
            ),
            note = localizeRuntime(
                "Формула: min(коммитов_в_день_на_участника × 9 + 2, 5). Шкала от 2 до 5.",
                "Formula: min(commits_per_day_per_member × 9 + 2, 5). Scale: 2 to 5.",
            ),
        )

        RankingMetricKey.IssueCompleteness -> RankingMetricInfo(
            title = localizeRuntime("Завершённость задач", "Issue Completeness"),
            whatShows = localizeRuntime(
                "Долю закрытых задач от общего числа задач за выбранный период.",
                "The share of closed issues out of total issues for the selected period.",
            ),
            whyNeeded = listOf(
                localizeRuntime("Оценивает способность команды доводить задачи до конца.", "Evaluates the team's ability to complete tasks."),
                localizeRuntime("Сигнализирует о накапливающемся хвосте незакрытых задач.", "Signals an accumulating backlog of unclosed issues."),
            ),
            howToInterpret = listOf(
                localizeRuntime("100% закрыто → оценка 5.", "100% closed → score 5."),
                localizeRuntime("0% закрыто → оценка 2.", "0% closed → score 2."),
                localizeRuntime("Каждые 33% закрытых задач добавляют ~1 балл.", "Every 33% of closed issues adds ~1 point."),
            ),
            note = localizeRuntime(
                "Формула: (закрытые / все) × 3 + 2. Шкала от 2 до 5.",
                "Formula: (closed / total) × 3 + 2. Scale: 2 to 5.",
            ),
        )

        RankingMetricKey.PullRequestHangTime -> RankingMetricInfo(
            title = localizeRuntime("Время жизни Pull Request", "PR Hang Time"),
            whatShows = localizeRuntime(
                "Среднее время от открытия PR до его закрытия за выбранный период.",
                "Average time from PR opening to closure over the selected period.",
            ),
            whyNeeded = listOf(
                localizeRuntime("Выявляет задержки на этапе ревью и слияния.", "Identifies delays at the review and merge stage."),
                localizeRuntime("Помогает оценить скорость обратной связи внутри команды.", "Helps assess the speed of feedback within the team."),
            ),
            howToInterpret = listOf(
                localizeRuntime("Среднее время < 5 минут — отображается как сырое значение в минутах (возможно, автоматические PR).", "Average time < 5 minutes — shown as raw value in minutes (possibly automated PRs)."),
                localizeRuntime("0 дней (мгновенное закрытие) → оценка 5.", "0 days (instant closure) → score 5."),
                localizeRuntime("7+ дней среднего времени жизни → оценка ≤ 2.", "7+ days average lifetime → score ≤ 2."),
            ),
            note = localizeRuntime(
                "Формула: (1 − среднее_время / 7_дней) × 3 + 2. При среднем < 5 мин отображается время, а не балл.",
                "Formula: (1 − avg_time / 7_days) × 3 + 2. When average < 5 min, time is shown instead of score.",
            ),
        )

        RankingMetricKey.RapidPullRequests -> RankingMetricInfo(
            title = localizeRuntime("Быстрые Pull Requests", "Rapid Pull Requests"),
            whatShows = localizeRuntime(
                "Долю PR, закрытых быстрее заданного порога (возможно без полноценного ревью).",
                "The share of PRs closed faster than the set threshold (possibly without a proper review).",
            ),
            whyNeeded = listOf(
                localizeRuntime("Выявляет PR, которые могли пройти без полноценного ревью.", "Identifies PRs that may have been merged without a thorough review."),
                localizeRuntime("Помогает контролировать качество code review в команде.", "Helps monitor code review quality in the team."),
            ),
            howToInterpret = listOf(
                localizeRuntime("0 быстрых PR из всех → оценка 5.", "0 rapid PRs out of all → score 5."),
                localizeRuntime("Все PR закрыты быстро → оценка 2.", "All PRs closed rapidly → score 2."),
                localizeRuntime("Чем меньше доля быстрых PR, тем выше балл.", "The lower the share of rapid PRs, the higher the score."),
            ),
            note = localizeRuntime(
                "Формула: (1 − быстрые / все) × 3 + 2. Порог скорости задаётся в настройках фильтра.",
                "Formula: (1 − rapid / total) × 3 + 2. Speed threshold is set in filter settings.",
            ),
        )

        RankingMetricKey.CodeChurn -> RankingMetricInfo(
            title = localizeRuntime("Изменчивость кода", "Code Churn"),
            whatShows = localizeRuntime(
                "Насколько интенсивно правится кодовая база: среднее число правок на коммит по всем файлам.",
                "How intensively the codebase is edited: average number of edits per commit across all files.",
            ),
            whyNeeded = listOf(
                localizeRuntime("Высокий churn может говорить о нестабильной архитектуре или частых переработках.", "High churn may indicate an unstable architecture or frequent rewrites."),
                localizeRuntime("Помогает найти «горячие» модули, которые постоянно меняются.", "Helps identify \"hot\" modules that change frequently."),
            ),
            howToInterpret = listOf(
                localizeRuntime("~1 правка файла на коммит → оценка близка к 5 (стабильный код).", "~1 file edit per commit → score close to 5 (stable code)."),
                localizeRuntime("Рост churn снижает оценку по логарифмической шкале.", "Increasing churn reduces the score on a logarithmic scale."),
                localizeRuntime("Очень высокий churn (десятки правок на коммит) → оценка стремится к 2.", "Very high churn (tens of edits per commit) → score approaches 2."),
            ),
            note = localizeRuntime(
                "Клиент использует формулу 5 − ln(1 + churn_на_коммит) × 1.2. Backend считает частоту изменения файлов, но отдельной оценки не выставляет — клиент применяет собственную логарифмическую шкалу.",
                "Client uses formula 5 − ln(1 + churn_per_commit) × 1.2. Backend tracks file change frequency but does not assign its own score — the client applies its own logarithmic scale.",
            ),
        )

        RankingMetricKey.CodeOwnership -> RankingMetricInfo(
            title = localizeRuntime("Владение кодом", "Code Ownership"),
            whatShows = localizeRuntime(
                "Равномерность распределения коммитов между участниками команды.",
                "How evenly commits are distributed among team members.",
            ),
            whyNeeded = listOf(
                localizeRuntime("Выявляет зависимость от одного-двух разработчиков (bus factor).", "Identifies dependency on one or two developers (bus factor)."),
                localizeRuntime("Показывает, насколько знания о кодовой базе распределены по команде.", "Shows how well codebase knowledge is distributed across the team."),
            ),
            howToInterpret = listOf(
                localizeRuntime("Полностью равномерное распределение → оценка 5.", "Fully even distribution → score 5."),
                localizeRuntime("Один человек делает все коммиты → оценка 2.", "One person makes all commits → score 2."),
                localizeRuntime("Требуется минимум 2 участника: при одном авторе оценка не рассчитывается.", "Requires at least 2 participants: score is not calculated for a single author."),
            ),
            note = localizeRuntime(
                "Формула: (1 − √(дисперсия / худший_случай)) × 3 + 2. Оценивает отклонение от идеально равного распределения.",
                "Formula: (1 − √(variance / worst_case)) × 3 + 2. Measures deviation from a perfectly equal distribution.",
            ),
        )

        RankingMetricKey.DominantWeekDay -> RankingMetricInfo(
            title = localizeRuntime("Доминирующий день недели", "Dominant Weekday"),
            whatShows = localizeRuntime(
                "Долю активности (коммиты + issues + PR) в выбранный нежелательный день относительно среднего по остальным дням.",
                "The share of activity (commits + issues + PRs) on the selected undesired day relative to the average on other days.",
            ),
            whyNeeded = listOf(
                localizeRuntime("Позволяет выявить нежелательные паттерны работы: переработки в выходные или неравномерную нагрузку.", "Helps identify undesired work patterns: weekend overtime or uneven load."),
                localizeRuntime("Поощряет равномерное распределение активности по рабочей неделе.", "Encourages even distribution of activity across the working week."),
            ),
            howToInterpret = listOf(
                localizeRuntime("Активность в нежелательный день ниже среднего → оценка 5.", "Activity on the undesired day is below average → score 5."),
                localizeRuntime("Активность в ~3 раза выше среднего → оценка 2.", "Activity ~3× above average → score 2."),
                localizeRuntime("Нет активности в нежелательный день → максимальный балл.", "No activity on the undesired day → maximum score."),
            ),
            note = localizeRuntime(
                "Формула: max(0, −1.5 × ratio + 6.5), где ratio = активность_в_день / средняя_активность. День выбирается в настройках фильтра.",
                "Formula: max(0, −1.5 × ratio + 6.5), where ratio = activity_on_day / average_activity. Day is selected in filter settings.",
            ),
        )
    }
}

private fun formatDateRangeField(dateRange: RankingDateRangeFilter): String {
    val from = localizeRuntime("с", "from")
    val to = localizeRuntime("по", "to")
    val defaultStart = localizeRuntime("00.00.0000", "01/01/0000")
    val defaultEnd = localizeRuntime("31.12.3000", "12/31/3000")
    if (!dateRange.isActive) return "$from $defaultStart $to $defaultEnd"

    val start = dateRange.startMillis?.let(::formatDate)
    val end = dateRange.endMillis?.let(::formatDate)
    return "$from ${start ?: defaultStart} $to ${end ?: defaultEnd}"
}

/** Converts epoch-millis to an ISO-8601 date string (YYYY-MM-DD, UTC). */
private fun Long.toRankingIsoDate(): String {
    val date = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date
    val m = date.monthNumber.toString().padStart(2, '0')
    val d = date.dayOfMonth.toString().padStart(2, '0')
    return "${date.year}-$m-$d"
}

/** Returns today's date as an ISO-8601 string (UTC). */
private fun rankingTodayIsoDate(): String = PlatformTime.currentTimeMillis().toRankingIsoDate()

/**
 * Converts an ISO-8601 date string (YYYY-MM-DD) to epoch-millis (UTC midnight).
 * Uses a pure Julian Day Number calculation with no extra imports.
 */
private fun rankingIsoDateToMillis(iso: String): Long? {
    val parts = iso.split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    if (month < 1 || month > 12 || day < 1 || day > 31) return null
    // Julian Day Number
    val a = (14 - month) / 12
    val y = year + 4800 - a
    val m2 = month + 12 * a - 3
    val jdn = day + (153 * m2 + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
    val epochJdn = 2440588 // JDN of 1970-01-01
    return (jdn - epochJdn).toLong() * 86_400_000L
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

private fun scoreGradientColor(score: Double?, accentRed: Color, neutralGray: Color): Color {
    if (score == null) return neutralGray
    val clamped = score.coerceIn(1.0, 5.0).toFloat()
    return if (clamped <= 3f) {
        val t = (clamped - 1f) / 2f
        lerp(accentRed, RankingYellow, t)
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
