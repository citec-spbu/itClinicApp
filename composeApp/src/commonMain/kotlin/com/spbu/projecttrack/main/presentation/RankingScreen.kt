package com.spbu.projecttrack.main.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spbu.projecttrack.core.auth.AuthManager
import com.spbu.projecttrack.core.di.DependencyContainer
import com.spbu.projecttrack.core.theme.AppColors
import com.spbu.projecttrack.core.theme.AppFonts
import com.spbu.projecttrack.projects.presentation.components.SearchBar
import com.spbu.projecttrack.rating.data.model.RankingData
import com.spbu.projecttrack.rating.data.model.RankingItem
import com.spbu.projecttrack.rating.presentation.RankingUiState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import projecttrack.composeapp.generated.resources.Res
import projecttrack.composeapp.generated.resources.filter_icon
import projecttrack.composeapp.generated.resources.spbu_logo

private enum class RankingTab {
    Projects,
    Students
}

private enum class RankingSortOrder {
    Descending,
    Ascending
}

private val RankingRiseColor = Color(0xFF209F31)
private val RankingFallColor = Color(0xFF9F2D20)

private fun rankingTabForPage(page: Int): RankingTab {
    return RankingTab.values().getOrElse(page) { RankingTab.Projects }
}

@Composable
fun RankingScreen(
    onProjectClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isAuthorized by AuthManager.isAuthorized.collectAsState(initial = false)
    val viewModel = remember { DependencyContainer.provideRankingViewModel() }
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pagerState = rememberPagerState(
        initialPage = RankingTab.Projects.ordinal,
        pageCount = { RankingTab.values().size }
    )
    val coroutineScope = rememberCoroutineScope()

    var searchText by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(RankingSortOrder.Descending) }
    val hasActiveFilters = false
    val selectedTab = rankingTabForPage(pagerState.currentPage)

    LaunchedEffect(isAuthorized) {
        if (isAuthorized) {
            viewModel.load()
        } else {
            viewModel.reset()
        }
    }

    RankingScreenLayout(
        modifier = modifier,
        isAuthorized = isAuthorized,
        uiState = uiState,
        isRefreshing = isRefreshing,
        pagerState = pagerState,
        selectedTab = selectedTab,
        onTabSelected = { tab ->
            coroutineScope.launch {
                pagerState.animateScrollToPage(tab.ordinal)
            }
        },
        searchText = searchText,
        onSearchTextChange = { searchText = it },
        hasActiveFilters = hasActiveFilters,
        onFilterClick = {},
        sortOrder = sortOrder,
        onSortToggle = {
            sortOrder = if (sortOrder == RankingSortOrder.Descending) {
                RankingSortOrder.Ascending
            } else {
                RankingSortOrder.Descending
            }
        },
        onRefresh = { viewModel.refresh() },
        onRetry = { viewModel.retry() },
        onProjectClick = onProjectClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RankingScreenLayout(
    isAuthorized: Boolean,
    uiState: RankingUiState,
    isRefreshing: Boolean,
    pagerState: PagerState,
    selectedTab: RankingTab,
    onTabSelected: (RankingTab) -> Unit,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    hasActiveFilters: Boolean,
    onFilterClick: () -> Unit,
    sortOrder: RankingSortOrder,
    onSortToggle: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onProjectClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Image(
            painter = painterResource(Res.drawable.spbu_logo),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .alpha(1.0f),
            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(top = 0.dp, bottom = 0.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = "Рейтинг",
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    color = AppColors.Color3
                )
            }

            if (!isAuthorized) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Авторизуйтесь, чтобы видеть рейтинг\nстудентов и пользователей",
                        fontFamily = AppFonts.OpenSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = AppColors.Color2,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RankingTabButton(
                            text = "Проекты",
                            selected = selectedTab == RankingTab.Projects,
                            onClick = { onTabSelected(RankingTab.Projects) }
                        )
                        RankingTabButton(
                            text = "Студенты",
                            selected = selectedTab == RankingTab.Students,
                            onClick = { onTabSelected(RankingTab.Students) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(1.dp)
                            .height(20.dp)
                            .background(AppColors.Color2)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchBar(
                        searchText = searchText,
                        onSearchTextChange = onSearchTextChange,
                        onFilterClick = {},
                        hasActiveFilters = false,
                        showFilters = false,
                        onFocusChange = {},
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RankingFilterButton(
                            hasActiveFilters = hasActiveFilters,
                            onClick = onFilterClick
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        RankingSortButton(
                            sortOrder = sortOrder,
                            onClick = onSortToggle
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (val state = uiState) {
                        RankingUiState.Idle,
                        RankingUiState.Loading -> {
                            LoadingContent()
                        }

                        is RankingUiState.Error -> {
                            ErrorContent(
                                message = state.message,
                                onRetry = onRetry
                            )
                        }

                        is RankingUiState.Success -> {
                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = onRefresh,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { page ->
                                    val tab = rankingTabForPage(page)
                                    val items = if (tab == RankingTab.Projects) {
                                        state.data.projects
                                    } else {
                                        state.data.students
                                    }
                                    val searchedItems = searchRankingItems(items, searchText)
                                    RankingList(
                                        items = sortRankingItems(
                                            items = searchedItems,
                                            sortOrder = sortOrder
                                        ),
                                        tab = tab,
                                        emptyText = if (tab == RankingTab.Projects) {
                                            "Нет данных по проектам"
                                        } else {
                                            "Нет данных по студентам"
                                        },
                                        onProjectClick = onProjectClick
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 1f),
                                        Color.White.copy(alpha = 0f)
                                    )
                                )
                            )
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0f),
                            Color.White.copy(alpha = 1f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun RankingFilterButton(
    hasActiveFilters: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 700f),
        label = "ranking_filter_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.7f else 1f,
        label = "ranking_filter_alpha"
    )

    Box(
        modifier = modifier
            .size(28.dp)
            .scale(scale)
            .alpha(alpha)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(Res.drawable.filter_icon),
            contentDescription = "Фильтр",
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(AppColors.Color2)
        )

        if (hasActiveFilters) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = AppColors.Color3,
                        shape = CircleShape
                    )
                    .align(Alignment.TopEnd)
                    .offset(x = (-1).dp, y = 1.dp)
            )
        }
    }
}

@Composable
private fun RankingSortButton(
    sortOrder: RankingSortOrder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 650f),
        label = "ranking_sort_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.75f else 1f,
        label = "ranking_sort_alpha"
    )

    Box(
        modifier = modifier
            .size(28.dp)
            .scale(scale)
            .alpha(alpha)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (sortOrder == RankingSortOrder.Ascending) "↓" else "↑",
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = AppColors.Color2
        )
    }
}

@Composable
private fun RankingTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = Color.Transparent
    val contentColor = if (selected) AppColors.Color2 else AppColors.Color1
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "ranking_tab_scale"
    )

    Surface(
        color = background,
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontFamily = AppFonts.OpenSans,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 20.sp,
                color = contentColor
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(color = AppColors.Color3)
            Text(
                text = "Загрузка рейтинга...",
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = AppColors.Color2
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Ошибка загрузки",
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = AppColors.Color3
            )
            Text(
                text = message,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = AppColors.Color2
            )
            RankingTabButton(
                text = "Повторить",
                selected = true,
                onClick = onRetry
            )
        }
    }
}

@Composable
private fun RankingList(
    items: List<RankingItem>,
    tab: RankingTab,
    emptyText: String,
    onProjectClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyText,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = AppColors.Color2
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 180.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(items) { index, item ->
                RankingRow(
                    index = index + 1,
                    item = item,
                    tab = tab,
                    onProjectClick = onProjectClick
                )
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
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && tab == RankingTab.Projects) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 700f),
        label = "ranking_row_scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                enabled = tab == RankingTab.Projects,
                interactionSource = interactionSource,
                indication = null
            ) {
                if (tab == RankingTab.Projects) {
                    onProjectClick(item.key)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppColors.Color1)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .width(50.dp)
                    .padding(start = 0.dp),
                horizontalAlignment = Alignment.Start
            ) {
                RankingMovementArrow(
                    item = item,
                    showMovement = tab == RankingTab.Projects
                )
                Text(
                    text = index.toString(),
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 20.sp,
                    color = AppColors.Color2
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    RankingTitle(
                        item = item,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = item.scoreText,
                        fontFamily = AppFonts.OpenSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = scoreGradientColor(item.score)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (tab) {
                    RankingTab.Projects -> {
                        ProjectRankingBody(item = item)
                    }

                    RankingTab.Students -> {
                        StudentRankingBody(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingTitle(
    item: RankingItem,
    modifier: Modifier = Modifier
) {
    val titleText = remember(item.title, item.markerLabel) {
        buildAnnotatedString {
            append(item.title)
            if (!item.markerLabel.isNullOrBlank()) {
                append(" ")
                withStyle(
                    style = SpanStyle(color = Color(0xFF9F2D20))
                ) {
                    append("(${item.markerLabel})")
                }
            }
        }
    }

    Text(
        text = titleText,
        fontFamily = AppFonts.OpenSans,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = AppColors.Color2,
        modifier = modifier,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectRankingBody(
    item: RankingItem,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = item.description?.takeIf { it.isNotBlank() } ?: "Описание проекта пока недоступно",
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            color = AppColors.Color2,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis
        )

        if (item.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item.tags.forEach { tag ->
                    RankingTagChip(tag = tag)
                }
            }
        }
    }
}

@Composable
private fun StudentRankingBody(
    item: RankingItem,
    modifier: Modifier = Modifier
) {
    Text(
        text = "Проект: ${item.projectName?.takeIf { it.isNotBlank() } ?: "Не определен"}",
        fontFamily = AppFonts.OpenSans,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        color = Color(0xFF75757D),
        modifier = modifier.fillMaxWidth(),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun RankingTagChip(
    tag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, AppColors.Color1)
    ) {
        Text(
            text = tag,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            color = AppColors.Color2,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 0.dp)
        )
    }
}

@Composable
private fun RankingMovementArrow(
    item: RankingItem,
    showMovement: Boolean,
    modifier: Modifier = Modifier
) {
    val delta = item.positionDelta
    val arrow = when {
        !showMovement -> null
        item.previousPosition == null -> null
        delta == null || delta == 0 -> null
        delta > 0 -> "↑"
        else -> "↓"
    }
    val color = when {
        delta == null || delta == 0 -> Color.Transparent
        delta > 0 -> RankingRiseColor
        else -> RankingFallColor
    }

    Box(
        modifier = modifier
            .height(14.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.TopStart
    ) {
        if (arrow != null) {
            Text(
                text = arrow,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 14.sp,
                color = color
            )
        }
    }
}

private fun fuzzyMatchScore(text: String, query: String): Double {
    if (query.isBlank()) return 1.0
    if (text.isBlank()) return 0.0

    val normalizedText = text.lowercase()
    val normalizedQuery = query.lowercase()

    if (normalizedText.contains(normalizedQuery)) {
        return 1.0
    }

    val queryBigrams = mutableSetOf<String>()
    for (i in 0 until normalizedQuery.length - 1) {
        queryBigrams.add(normalizedQuery.substring(i, i + 2))
    }

    if (queryBigrams.isEmpty()) {
        return if (normalizedText.contains(normalizedQuery[0])) 0.5 else 0.0
    }

    val textBigrams = mutableSetOf<String>()
    for (i in 0 until normalizedText.length - 1) {
        textBigrams.add(normalizedText.substring(i, i + 2))
    }

    val matches = queryBigrams.intersect(textBigrams).size

    return matches.toDouble() / queryBigrams.size
}

private fun searchRankingItems(
    items: List<RankingItem>,
    query: String,
    threshold: Double = 0.3
): List<RankingItem> {
    if (query.isBlank()) return items

    return items
        .map { item -> item to fuzzyMatchScore(rankingSearchText(item), query) }
        .filter { it.second >= threshold }
        .sortedByDescending { it.second }
        .map { it.first }
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
    sortOrder: RankingSortOrder
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
    if (score == null) return AppColors.Color2

    val low = RankingFallColor
    val mid = Color(0xFF9F9220)
    val high = RankingRiseColor

    val clamped = score.coerceIn(1.0, 5.0).toFloat()
    return if (clamped <= 3f) {
        val t = (clamped - 1f) / 2f
        lerp(low, mid, t)
    } else {
        val t = (clamped - 3f) / 2f
        lerp(mid, high, t)
    }
}

@Preview(showBackground = true, name = "Ranking Screen")
@Composable
private fun RankingScreenPreview() {
    val pagerState = rememberPagerState(
        initialPage = RankingTab.Projects.ordinal,
        pageCount = { RankingTab.values().size }
    )
    val coroutineScope = rememberCoroutineScope()
    var searchText by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(RankingSortOrder.Descending) }

    RankingScreenLayout(
        isAuthorized = true,
        uiState = RankingUiState.Success(previewRankingData()),
        isRefreshing = false,
        pagerState = pagerState,
        selectedTab = rankingTabForPage(pagerState.currentPage),
        onTabSelected = { tab ->
            coroutineScope.launch {
                pagerState.animateScrollToPage(tab.ordinal)
            }
        },
        searchText = searchText,
        onSearchTextChange = { searchText = it },
        hasActiveFilters = true,
        onFilterClick = {},
        sortOrder = sortOrder,
        onSortToggle = {
            sortOrder = if (sortOrder == RankingSortOrder.Descending) {
                RankingSortOrder.Ascending
            } else {
                RankingSortOrder.Descending
            }
        },
        onRefresh = {},
        onRetry = {},
        onProjectClick = {}
    )
}

private fun previewRankingData(): RankingData = RankingData(
    projects = listOf(
        RankingItem(
            key = "project-1",
            title = "AI-ассистент для клинических маршрутов",
            score = 4.92,
            scoreText = "4.92",
            description = "Сервис для анализа маршрутов пациентов и рекомендаций по оптимальному пути лечения.",
            tags = listOf("Backend", "ML", "Android"),
            markerLabel = "Текущий",
            previousPosition = 3,
            positionDelta = 2,
            historyAvailable = true
        ),
        RankingItem(
            key = "project-2",
            title = "Сервис телемедицины для малых городов",
            score = 3.14,
            scoreText = "3.14",
            description = "Платформа дистанционных консультаций для небольших медицинских центров.",
            tags = listOf("iOS", "Backend"),
            previousPosition = 1,
            positionDelta = -1,
            historyAvailable = true
        ),
        RankingItem(
            key = "project-3",
            title = "Модуль оценки рисков госпитализации",
            score = 1.67,
            scoreText = "1.67",
            description = "Аналитический модуль, который прогнозирует вероятность госпитализации по истории пациента.",
            tags = listOf("Data", "ML"),
            previousPosition = 3,
            positionDelta = 0,
            historyAvailable = true
        )
    ),
    students = listOf(
        RankingItem(
            key = "student-1",
            title = "Иван Петров",
            score = 4.75,
            scoreText = "4.75",
            projectName = "AI-ассистент для клинических маршрутов",
            markerLabel = "Вы"
        ),
        RankingItem(
            key = "student-2",
            title = "Мария Смирнова",
            score = 2.95,
            scoreText = "2.95",
            projectName = "Сервис телемедицины для малых городов"
        ),
        RankingItem(
            key = "student-3",
            title = "Алексей Кузнецов",
            score = 1.23,
            scoreText = "1.23",
            projectName = "Модуль оценки рисков госпитализации"
        )
    )
)
