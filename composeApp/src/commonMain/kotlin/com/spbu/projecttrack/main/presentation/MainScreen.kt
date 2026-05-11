package com.spbu.projecttrack.main.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spbu.projecttrack.core.auth.AuthManager
import com.spbu.projecttrack.core.di.DependencyContainer
import com.spbu.projecttrack.core.logging.AppLog
import com.spbu.projecttrack.core.settings.localizedString
import com.spbu.projecttrack.core.theme.AppFonts
import com.spbu.projecttrack.core.theme.appPalette
import com.spbu.projecttrack.projects.data.model.Project
import com.spbu.projecttrack.projects.data.model.ProjectDetail
import com.spbu.projecttrack.projects.data.model.Tag
import com.spbu.projecttrack.projects.presentation.ProjectsScreen
import com.spbu.projecttrack.projects.presentation.components.FiltersAlert
import com.spbu.projecttrack.projects.presentation.components.SuggestProjectButton
import com.spbu.projecttrack.projects.presentation.components.SuggestProjectAlert
import com.spbu.projecttrack.projects.presentation.components.SuggestProjectResultAlert
import com.spbu.projecttrack.projects.presentation.detail.ProjectDetailScreen
import com.spbu.projecttrack.projects.presentation.detail.ProjectDetailScreenContent
import com.spbu.projecttrack.projects.presentation.detail.ProjectDetailUiState
import com.spbu.projecttrack.projects.presentation.models.ProjectFilters
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.backhandler.BackHandler
import projecttrack.composeapp.generated.resources.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private val TabBarBackground = Color(0xFF9F2D20)
private val TabBarBorder = Color(0xFFCF3F2F)
private val UnselectedTabColor = Color(0xFFA6A6A6)
private val SelectedTabColor = Color.White
private val SelectionGradientTop = Color(0xFFCF3F2F)
private val SelectionGradientBottom = Color(0xFFB13123)
private val FloatingActionInset = 16.dp
private val FloatingButtonToTabBarGap = 16.dp
private val TabBarVisibleLift = 30.dp
private val TabBarHeight = 60.dp
private val TabBarWidth = 380.dp
private val TabBarIndicatorWidth = 123.dp
private val TabBarIndicatorHeight = 50.dp
private val TabBarIndicatorTopInset = 5.dp
private val TabBarIndicatorOffsets = listOf(5.dp, 128.5.dp, 252.dp)

private fun tabPositionToIndicatorOffset(
    position: Float,
    indicatorOffsetsPx: List<Float>,
): Float {
    if (indicatorOffsetsPx.isEmpty()) return 0f
    val clampedPosition = position.coerceIn(0f, indicatorOffsetsPx.lastIndex.toFloat())
    val startIndex = floor(clampedPosition).toInt()
    val endIndex = ceil(clampedPosition).toInt()
    if (startIndex == endIndex) return indicatorOffsetsPx[startIndex]
    val progress = clampedPosition - startIndex
    val startOffset = indicatorOffsetsPx[startIndex]
    val endOffset = indicatorOffsetsPx[endIndex]
    return startOffset + (endOffset - startOffset) * progress
}

private fun indicatorOffsetToTabPosition(
    indicatorOffsetPx: Float,
    indicatorOffsetsPx: List<Float>,
): Float {
    if (indicatorOffsetsPx.isEmpty()) return 0f
    val clampedOffset = indicatorOffsetPx.coerceIn(indicatorOffsetsPx.first(), indicatorOffsetsPx.last())
    for (index in 0 until indicatorOffsetsPx.lastIndex) {
        val startOffset = indicatorOffsetsPx[index]
        val endOffset = indicatorOffsetsPx[index + 1]
        if (clampedOffset <= endOffset) {
            val distance = endOffset - startOffset
            val progress = if (distance == 0f) 0f else (clampedOffset - startOffset) / distance
            return index + progress.coerceIn(0f, 1f)
        }
    }
    return indicatorOffsetsPx.lastIndex.toFloat()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MainScreen(
    onProjectDetailClick: (String) -> Unit,
    onProjectStatsClick: (String) -> Unit,
    onUserStatsClick: (String, String, String?) -> Unit,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSuggestProject by remember { mutableStateOf(false) }
    var isRankingRoot by remember { mutableStateOf(true) }
    var isSettingsRoot by remember { mutableStateOf(true) }
    var isMainTabDragging by remember { mutableStateOf(false) }
    var draggedMainTabPosition by remember { mutableStateOf<Float?>(null) }
    var settlingMainTab by remember { mutableStateOf<Int?>(null) }
    val isAuthorized by AuthManager.isAuthorized.collectAsState()
    val projectsTitle = localizedString("Проекты", "Projects")
    val suggestProjectLabel = localizedString("Предложить проект", "Suggest a project")
    val allProjectsLabel = localizedString("Все проекты", "All projects")
    val myProjectLabel = localizedString("Мой проект", "My project")
    val loginToSeeProjectLabel = localizedString(
        "Войдите, чтобы увидеть свой проект",
        "Sign in to see your project",
    )
    val noPersonalProjectLabel = localizedString(
        "У вас еще нет личного проекта",
        "You do not have a personal project yet",
    )
    val requestSentTitle = localizedString("Заявка отправлена", "Request sent")
    val requestSentMessage = localizedString(
        "Мы свяжемся с вами в ближайшее время.",
        "We will contact you soon.",
    )
    val requestFailedTitle = localizedString(
        "Не удалось отправить заявку",
        "Failed to send request",
    )
    val requestFailedMessage = localizedString("Попробуйте позже.", "Please try again later.")

    // Reset root flags when leaving nested tabs so the first frame on return uses the correct insets.
    LaunchedEffect(selectedTab) {
        if (selectedTab != 2) isSettingsRoot = true
        if (selectedTab != 1) isRankingRoot = true
    }

    val showTabBar = when (selectedTab) {
        0 -> true
        1 -> isRankingRoot
        2 -> isSettingsRoot
        else -> true
    }
    val mainTabTargetPosition = when {
        isMainTabDragging && draggedMainTabPosition != null -> draggedMainTabPosition!!
        settlingMainTab != null -> settlingMainTab!!.toFloat()
        else -> selectedTab.toFloat()
    }
    val mainTabVisualPosition by animateFloatAsState(
        targetValue = mainTabTargetPosition,
        animationSpec = if (isMainTabDragging) snap() else tween(durationMillis = 320),
        label = "main_tab_visual_position",
    )

    LaunchedEffect(selectedTab, isMainTabDragging, settlingMainTab, mainTabVisualPosition) {
        if (!isMainTabDragging && settlingMainTab == selectedTab && abs(mainTabVisualPosition - selectedTab.toFloat()) < 0.001f) {
            settlingMainTab = null
        }
    }

    val handleTabSelection = remember(onTabSelected) {
        { tab: Int ->
            isMainTabDragging = false
            draggedMainTabPosition = null
            settlingMainTab = tab
            onTabSelected(tab)
        }
    }

    MainScreenContent(
        onProjectDetailClick = onProjectDetailClick,
        onProjectStatsClick = onProjectStatsClick,
        onUserStatsClick = onUserStatsClick,
        modifier = modifier,
        isAuthorized = isAuthorized,
        selectedTab = selectedTab,
        onTabSelected = handleTabSelection,
        mainTabVisualPosition = mainTabVisualPosition,
        onMainTabDragStart = {
            isMainTabDragging = true
            settlingMainTab = null
        },
        onMainTabDrag = { position ->
            draggedMainTabPosition = position
        },
        onMainTabDragEnd = { tab ->
            handleTabSelection(tab)
        },
        showSuggestProject = showSuggestProject,
        onShowSuggestProjectChange = { showSuggestProject = it },
        onRankingRootChange = { isRankingRoot = it },
        isSettingsRoot = isSettingsRoot,
        onSettingsRootChange = { isSettingsRoot = it },
        showTabBar = showTabBar,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun MainScreenContent(
    onProjectDetailClick: (String) -> Unit,
    onProjectStatsClick: (String) -> Unit,
    onUserStatsClick: (String, String, String?) -> Unit,
    modifier: Modifier = Modifier,
    isAuthorized: Boolean,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    mainTabVisualPosition: Float,
    onMainTabDragStart: () -> Unit,
    onMainTabDrag: (Float) -> Unit,
    onMainTabDragEnd: (Int) -> Unit,
    showSuggestProject: Boolean,
    onShowSuggestProjectChange: (Boolean) -> Unit,
    onRankingRootChange: (Boolean) -> Unit,
    isSettingsRoot: Boolean,
    onSettingsRootChange: (Boolean) -> Unit,
    showTabBar: Boolean,
    initialMyProject: Project? = null
) {
    val contactRequestApi = remember { DependencyContainer.provideContactRequestApi() }
    val userProfileApi = remember { DependencyContainer.provideUserProfileApi() }
    val projectsTitle = localizedString("Проекты", "Projects")
    val suggestProjectLabel = localizedString("Предложить проект", "Suggest a project")
    val requestSentTitle = localizedString("Заявка отправлена", "Request sent")
    val requestSentMessage = localizedString(
        "Мы свяжемся с вами в ближайшее время.",
        "We will contact you soon.",
    )
    val requestFailedTitle = localizedString(
        "Не удалось отправить заявку",
        "Failed to send request",
    )
    val requestFailedMessage = localizedString("Попробуйте позже.", "Please try again later.")
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var submitAlert by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var projectFilters by remember { mutableStateOf(ProjectFilters()) }
    var availableProjectTags by remember { mutableStateOf<List<Tag>>(emptyList()) }
    val logTag = "SuggestProject"
    val isPreview = LocalInspectionMode.current
    val dismissKeyboard = remember(focusManager, keyboardController) {
        {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            Unit
        }
    }

    // Refresh "My project" after auth changes and whenever the user comes back to the first tab.
    var cachedMyProject by remember { mutableStateOf<Project?>(null) }
    var myProjectRefreshKey by remember { mutableStateOf(0) }
    LaunchedEffect(isAuthorized, isPreview, myProjectRefreshKey) {
        if (isPreview || !isAuthorized) { cachedMyProject = null; return@LaunchedEffect }
        val result = userProfileApi.getProfile()
        if (result.isSuccess) {
            cachedMyProject = result.getOrNull()?.projects?.firstOrNull()
            AppLog.d("MyProject", "Cached project: ${cachedMyProject?.name}")
        } else {
            AppLog.e("MyProject", "Profile load failed")
            cachedMyProject = null
        }
    }
    var prevTab by remember { mutableStateOf(selectedTab) }
    LaunchedEffect(selectedTab) {
        if (selectedTab == 0 && prevTab != 0) {
            myProjectRefreshKey++
        }
        prevTab = selectedTab
    }

    val projectsPagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    BackHandler(enabled = selectedTab == 0 && projectsPagerState.currentPage == 1) {
        coroutineScope.launch { projectsPagerState.animateScrollToPage(0) }
    }

    val rootTopInsetModifier = if (showTabBar || selectedTab == 2) {
        Modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showTabBar) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CustomTabBar(
                            selectedTab = selectedTab,
                            tabVisualPosition = mainTabVisualPosition,
                            onTabSelected = { onTabSelected(it) },
                            onTabDragStart = onMainTabDragStart,
                            onTabDrag = onMainTabDrag,
                            onTabDragEnd = onMainTabDragEnd,
                        )
                    }
                }
            }
        ) {
            val contentModifier = Modifier
                .fillMaxSize()
                .then(rootTopInsetModifier)
            Box(
                modifier = contentModifier
            ) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val density = LocalDensity.current
                    val pageWidthPx = with(density) { maxWidth.toPx() }
                    val clampedTabPosition = mainTabVisualPosition.coerceIn(0f, 2f)
                    val startPage = floor(clampedTabPosition).toInt().coerceIn(0, 2)
                    val endPage = ceil(clampedTabPosition).toInt().coerceIn(0, 2)
                    val visiblePages = buildList {
                        add(startPage)
                        if (endPage != startPage) add(endPage)
                    }

                    visiblePages.forEach { currentTab ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationX = (currentTab - clampedTabPosition) * pageWidthPx
                                }
                        ) {
                            MainTabPage(
                                tab = currentTab,
                                isAuthorized = isAuthorized,
                                isPreview = isPreview,
                                projectsTitle = projectsTitle,
                                suggestProjectLabel = suggestProjectLabel,
                                projectFilters = projectFilters,
                                cachedMyProject = cachedMyProject,
                                myProjectRefreshKey = myProjectRefreshKey,
                                projectsPagerState = projectsPagerState,
                                onProjectDetailClick = onProjectDetailClick,
                                onProjectStatsClick = onProjectStatsClick,
                                onUserStatsClick = onUserStatsClick,
                                onShowFilters = { showFilters = true },
                                onAvailableTagsChange = { availableProjectTags = it },
                                onDismissKeyboard = dismissKeyboard,
                                onShowSuggestProject = { onShowSuggestProjectChange(true) },
                                onRankingRootChange = onRankingRootChange,
                                onSettingsRootChange = onSettingsRootChange,
                            )
                        }
                    }
                }
            }
        }

        FiltersAlert(
            isVisible = selectedTab == 0 && showFilters,
            onDismiss = { showFilters = false },
            tags = availableProjectTags,
            filters = projectFilters,
            onFiltersChange = { projectFilters = it }
        )

        SuggestProjectAlert(
            isVisible = showSuggestProject,
            onDismiss = { onShowSuggestProjectChange(false) },
            onSubmit = { name, email ->
                AppLog.d(logTag, "onSubmit: nameLen=${name.length}, emailLen=${email.length}")
                coroutineScope.launch {
                    val result = contactRequestApi.sendRequest(name, email)
                    if (result.isSuccess) {
                        AppLog.d(logTag, "Request success")
                        submitAlert = requestSentTitle to requestSentMessage
                    } else {
                        AppLog.e(logTag, "Request failed")
                        submitAlert = requestFailedTitle to requestFailedMessage
                    }
                }
            }
        )

        SuggestProjectResultAlert(
            isVisible = submitAlert != null,
            title = submitAlert?.first ?: "",
            message = submitAlert?.second ?: "",
            onDismiss = { submitAlert = null }
        )
    }
}

@Composable
private fun MainTabPage(
    tab: Int,
    isAuthorized: Boolean,
    isPreview: Boolean,
    projectsTitle: String,
    suggestProjectLabel: String,
    projectFilters: ProjectFilters,
    cachedMyProject: Project?,
    myProjectRefreshKey: Int,
    projectsPagerState: androidx.compose.foundation.pager.PagerState,
    onProjectDetailClick: (String) -> Unit,
    onProjectStatsClick: (String) -> Unit,
    onUserStatsClick: (String, String, String?) -> Unit,
    onShowFilters: () -> Unit,
    onAvailableTagsChange: (List<Tag>) -> Unit,
    onDismissKeyboard: () -> Unit,
    onShowSuggestProject: () -> Unit,
    onRankingRootChange: (Boolean) -> Unit,
    onSettingsRootChange: (Boolean) -> Unit,
) {
    val pagerScope = rememberCoroutineScope()

    when (tab) {
        0 -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                ProjectsBackgroundLogo()

                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(top = 0.dp, bottom = 0.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = projectsTitle,
                                fontFamily = AppFonts.OpenSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 40.sp,
                                color = appPalette().title
                            )
                        }
                    }
                    ProjectsSegmentedControl(
                        selectedPage = projectsPagerState.currentPage,
                        onPageSelected = { page ->
                            onDismissKeyboard()
                            pagerScope.launch {
                                projectsPagerState.animateScrollToPage(page)
                            }
                        }
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        HorizontalPager(
                            state = projectsPagerState,
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = true
                        ) { page ->
                            when (page) {
                                0 -> {
                                    if (isPreview) {
                                        Box(modifier = Modifier.fillMaxSize())
                                    } else {
                                        val projectsViewModel = remember { DependencyContainer.provideProjectsViewModel() }
                                        ProjectsScreen(
                                            viewModel = projectsViewModel,
                                            onProjectClick = onProjectDetailClick,
                                            showTitle = false,
                                            showLogo = false,
                                            filters = projectFilters,
                                            onFilterClick = onShowFilters,
                                            onDismissKeyboard = onDismissKeyboard,
                                            onAvailableTagsChange = onAvailableTagsChange
                                        )
                                    }
                                }
                                1 -> {
                                    MyProjectPage(
                                        isAuthorized = isAuthorized,
                                        isPreview = isPreview,
                                        myProject = cachedMyProject,
                                        refreshKey = myProjectRefreshKey,
                                        onProjectStatsClick = onProjectStatsClick,
                                        onUserStatsClick = onUserStatsClick
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(
                                    end = FloatingActionInset,
                                    bottom = TabBarVisibleLift + TabBarHeight + FloatingButtonToTabBarGap
                                )
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = projectsPagerState.currentPage == 0,
                                enter = fadeIn(tween(200)) + scaleIn(tween(200)),
                                exit = fadeOut(tween(150)) + scaleOut(tween(150)),
                            ) {
                                SuggestProjectButton(
                                    onClick = {
                                        onDismissKeyboard()
                                        onShowSuggestProject()
                                    },
                                    text = suggestProjectLabel
                                )
                            }
                        }
                    }
                }
            }
        }

        1 -> RankingScreen(
            onProjectClick = onProjectStatsClick,
            onStudentClick = onUserStatsClick,
            onRootDestinationChange = onRankingRootChange,
        )
        2 -> SettingsTabScreen(
            onRootDestinationChange = onSettingsRootChange
        )
    }
}

private fun Project.toProjectDetail(): ProjectDetail {
    return ProjectDetail(
        id = id,
        name = name,
        description = description,
        shortDescription = shortDescription,
        dateStart = dateStart,
        dateEnd = dateEnd,
        slug = slug,
        tags = tags,
        client = client
    )
}

@Composable
private fun ProjectsSegmentedControl(
    selectedPage: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val allProjectsLabel = localizedString("Все проекты", "All projects")
    val myProjectLabel = localizedString("Мой проект", "My project")

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
                ProjectsSegmentText(
                    text = allProjectsLabel,
                    selected = selectedPage == 0,
                    onClick = { onPageSelected(0) },
                )
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                ProjectsSegmentText(
                    text = myProjectLabel,
                    selected = selectedPage == 1,
                    onClick = { onPageSelected(1) },
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
private fun ProjectsSegmentText(
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
        label = "projects_segment_scale",
    )

    Text(
        text = text,
        fontFamily = AppFonts.OpenSans,
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
private fun MyProjectPage(
    isAuthorized: Boolean,
    isPreview: Boolean,
    myProject: Project?,
    refreshKey: Int = 0,
    onProjectStatsClick: (String) -> Unit,
    onUserStatsClick: (String, String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isAuthorized) {
        MyProjectUnauthorizedState(modifier = modifier)
        return
    }

    val project = myProject
    if (project == null) {
        MyProjectEmptyState(modifier = modifier)
        return
    }

    if (isPreview) {
        val projectDetail = project.toProjectDetail()
        ProjectDetailScreenContent(
            uiState = ProjectDetailUiState.Success(
                project = projectDetail,
                tags = emptyList(),
                teams = emptyList(),
                members = emptyList(),
                users = emptyList(),
                statusText = ""
            ),
            isAuthorized = isAuthorized,
            onBackClick = {},
            onRetry = {},
            showTitle = false,
            showBackButton = false,
            showMyProjectActions = true,
            onMyProjectOpenStats = { onProjectStatsClick(project.slug ?: project.id) },
            showBackgroundLogo = false,
            modifier = modifier
        )
    } else {
        val projectId = project.slug ?: project.id
        val detailViewModel = remember(projectId) {
            DependencyContainer.provideProjectDetailViewModel(projectId)
        }
        LaunchedEffect(refreshKey) {
            if (refreshKey > 0) detailViewModel.loadProjectDetail()
        }
        ProjectDetailScreen(
            viewModel = detailViewModel,
            onBackClick = {},
            showTitle = false,
            showBackButton = false,
            showMyProjectActions = true,
            onMyProjectOpenStats = { onProjectStatsClick(projectId) },
            onTeamMemberClick = onUserStatsClick,
            showBackgroundLogo = false,
            modifier = modifier
        )
    }
}

@Composable
private fun BoxScope.ProjectsBackgroundLogo() {
    Image(
        painter = painterResource(Res.drawable.spbu_logo),
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .align(Alignment.Center)
            .alpha(appPalette().spbuBackdropLogoAlpha),
        contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
    )
}

@Composable
private fun MyProjectUnauthorizedState(modifier: Modifier = Modifier) {
    val loginToSeeProjectLabel = localizedString(
        "Войдите, чтобы увидеть свой проект",
        "Sign in to see your project",
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = loginToSeeProjectLabel,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            color = appPalette().primaryText,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyProjectEmptyState(
    modifier: Modifier = Modifier
) {
    val noPersonalProjectLabel = localizedString(
        "У вас еще нет личного проекта",
        "You do not have a personal project yet",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = noPersonalProjectLabel,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            color = appPalette().primaryText,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
    }
}

@Composable
internal fun CustomTabBar(
    selectedTab: Int,
    tabVisualPosition: Float,
    onTabSelected: (Int) -> Unit,
    onTabDragStart: () -> Unit,
    onTabDrag: (Float) -> Unit,
    onTabDragEnd: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val indicatorOffsetsPx = remember(density) {
        TabBarIndicatorOffsets.map { with(density) { it.toPx() } }
    }
    val indicatorWidthPx = with(density) { TabBarIndicatorWidth.toPx() }
    val indicatorHeightPx = with(density) { TabBarIndicatorHeight.toPx() }
    val indicatorTopInsetPx = with(density) { TabBarIndicatorTopInset.toPx() }
    val minIndicatorOffsetPx = indicatorOffsetsPx.first()
    val maxIndicatorOffsetPx = indicatorOffsetsPx.last()
    val latestSelectedTab by rememberUpdatedState(selectedTab)
    val latestOnTabDragStart by rememberUpdatedState(onTabDragStart)
    val latestOnTabDrag by rememberUpdatedState(onTabDrag)
    val latestOnTabDragEnd by rememberUpdatedState(onTabDragEnd)
    var indicatorDragOffsetPx by remember {
        mutableStateOf(
            tabPositionToIndicatorOffset(
                position = tabVisualPosition,
                indicatorOffsetsPx = indicatorOffsetsPx,
            )
        )
    }
    val indicatorOffsetPx = tabPositionToIndicatorOffset(
        position = tabVisualPosition,
        indicatorOffsetsPx = indicatorOffsetsPx,
    )
    val latestIndicatorOffsetPx by rememberUpdatedState(indicatorOffsetPx)
    val indicatorOffsetX = with(density) { indicatorOffsetPx.toDp() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .offset(y = (-30).dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(TabBarWidth)
                .height(60.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(30.dp),
                    ambientColor = Color.Black.copy(alpha = 0.55f),
                    spotColor = Color.Black.copy(alpha = 0.55f),
                    clip = false
                )
                .border(
                    width = 2.dp,
                    color = TabBarBorder,
                    shape = RoundedCornerShape(30.dp)
                )
                .background(
                    color = TabBarBackground,
                    shape = RoundedCornerShape(30.dp)
                )
                .pointerInput(indicatorOffsetsPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val currentIndicatorOffsetPx = latestIndicatorOffsetPx
                        val startedInsideIndicator = down.position.x in
                            currentIndicatorOffsetPx..(currentIndicatorOffsetPx + indicatorWidthPx) &&
                            down.position.y in indicatorTopInsetPx..(indicatorTopInsetPx + indicatorHeightPx)

                        if (!startedInsideIndicator) {
                            return@awaitEachGesture
                        }

                        latestOnTabDragStart()
                        indicatorDragOffsetPx = currentIndicatorOffsetPx

                        val dragChange = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                            val nextOffset = (indicatorDragOffsetPx + over)
                                .coerceIn(minIndicatorOffsetPx, maxIndicatorOffsetPx)
                            indicatorDragOffsetPx = nextOffset
                            latestOnTabDrag(
                                indicatorOffsetToTabPosition(
                                    indicatorOffsetPx = nextOffset,
                                    indicatorOffsetsPx = indicatorOffsetsPx,
                                )
                            )
                            change.consume()
                        }

                        if (dragChange == null) {
                            latestOnTabDragEnd(latestSelectedTab)
                            return@awaitEachGesture
                        }

                        val activePointerId = dragChange.id
                        while (true) {
                            val nextEvent = awaitPointerEvent()
                            val pointer = nextEvent.changes.firstOrNull { it.id == activePointerId } ?: break

                            if (!pointer.pressed) {
                                break
                            }

                            val pointerDeltaX = pointer.positionChange().x
                            if (pointerDeltaX != 0f) {
                                indicatorDragOffsetPx = (indicatorDragOffsetPx + pointerDeltaX)
                                    .coerceIn(minIndicatorOffsetPx, maxIndicatorOffsetPx)
                                latestOnTabDrag(
                                    indicatorOffsetToTabPosition(
                                        indicatorOffsetPx = indicatorDragOffsetPx,
                                        indicatorOffsetsPx = indicatorOffsetsPx,
                                    )
                                )
                                pointer.consume()
                            }
                        }

                        val nearestTab = indicatorOffsetToTabPosition(
                            indicatorOffsetPx = indicatorDragOffsetPx,
                            indicatorOffsetsPx = indicatorOffsetsPx,
                        ).roundToInt()
                            .coerceIn(0, indicatorOffsetsPx.lastIndex)
                        latestOnTabDragEnd(nearestTab)
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffsetX, y = TabBarIndicatorTopInset)
                    .width(TabBarIndicatorWidth)
                    .height(TabBarIndicatorHeight)
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(25.dp),
                        ambientColor = Color.Black.copy(alpha = 0.4f),
                        spotColor = Color.Black.copy(alpha = 0.4f),
                        clip = false
                    )
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                SelectionGradientTop.copy(alpha = 0.7f),
                                SelectionGradientBottom.copy(alpha = 0.7f)
                            )
                        ),
                        shape = RoundedCornerShape(25.dp)
                    )
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabItem(
                    icon = Res.drawable.spbu_tab_logo,
                    iconSize = Pair(33.dp, 41.dp),
                    isSelected = selectedTab == 0,
                    onClick = { onTabSelected(0) }
                )

                TabItem(
                    icon = Res.drawable.stats_tab_logo,
                    iconSize = Pair(30.dp, 27.5.dp),
                    isSelected = selectedTab == 1,
                    onClick = { onTabSelected(1) }
                )

                TabItem(
                    icon = Res.drawable.settings_tab_logo,
                    iconSize = Pair(30.dp, 30.dp),
                    isSelected = selectedTab == 2,
                    onClick = { onTabSelected(2) }
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    icon: org.jetbrains.compose.resources.DrawableResource,
    iconSize: Pair<androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp>,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.15f else 1f, // Увеличение на 15% при нажатии
        animationSpec = tween(durationMillis = 150)
    )

    Box(
        modifier = modifier
            .width(TabBarIndicatorWidth)
            .height(TabBarIndicatorHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Убираем стандартное затемнение
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier
                .size(width = iconSize.first, height = iconSize.second)
                .scale(scale),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                if (isSelected) SelectedTabColor else UnselectedTabColor
            )
        )
    }
}
