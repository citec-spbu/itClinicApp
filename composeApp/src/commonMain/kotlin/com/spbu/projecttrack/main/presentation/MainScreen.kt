package com.spbu.projecttrack.main.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
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
import com.spbu.projecttrack.core.theme.AppColors
import com.spbu.projecttrack.core.theme.AppFonts
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

// Custom TabBar colors
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
    val isAuthorized by AuthManager.isAuthorized.collectAsState()
    val showTabBar = when (selectedTab) {
        0 -> true
        1 -> isRankingRoot
        2 -> isSettingsRoot
        else -> true
    }

    MainScreenContent(
        onProjectDetailClick = onProjectDetailClick,
        onProjectStatsClick = onProjectStatsClick,
        onUserStatsClick = onUserStatsClick,
        modifier = modifier,
        isAuthorized = isAuthorized,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
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

    // Кэш "Мой проект" — загружается один раз при смене авторизации
    var cachedMyProject by remember { mutableStateOf<Project?>(null) }
    LaunchedEffect(isAuthorized, isPreview) {
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

    // Pager для первой вкладки: 0 = Все проекты, 1 = Мой проект
    val projectsPagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    BackHandler(enabled = selectedTab == 0 && projectsPagerState.currentPage == 1) {
        coroutineScope.launch { projectsPagerState.animateScrollToPage(0) }
    }

    val rootTopInsetModifier = if (showTabBar) {
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
            containerColor = Color.White,
            bottomBar = {
                if (showTabBar) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CustomTabBar(
                            selectedTab = selectedTab,
                            onTabSelected = { onTabSelected(it) }
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
                when (selectedTab) {
                    0 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White)
                        ) {
                            ProjectsBackgroundLogo()

                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White)
                                        .padding(top = 0.dp, bottom = 0.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Проекты",
                                            fontFamily = AppFonts.OpenSansBold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 40.sp,
                                            color = AppColors.Color3
                                        )
                                    }
                                }
                                ProjectsSegmentedControl(
                                    selectedPage = projectsPagerState.currentPage,
                                    onPageSelected = { page ->
                                        dismissKeyboard()
                                        coroutineScope.launch {
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
                                                        onFilterClick = { showFilters = true },
                                                        onDismissKeyboard = dismissKeyboard,
                                                        onAvailableTagsChange = { availableProjectTags = it }
                                                    )
                                                }
                                            }
                                            1 -> {
                                                MyProjectPage(
                                                    isAuthorized = isAuthorized,
                                                    isPreview = isPreview,
                                                    myProject = cachedMyProject,
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
                                                    dismissKeyboard()
                                                    onShowSuggestProjectChange(true)
                                                },
                                                text = "Предложить проект"
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
                        submitAlert = "Заявка отправлена" to
                            "Мы свяжемся с вами в ближайшее время."
                    } else {
                        AppLog.e(logTag, "Request failed")
                        submitAlert = "Не удалось отправить заявку" to
                            "Попробуйте позже."
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

// ==================== Сегментный контрол "Все проекты / Мой проект" ====================

private val ProjectsSegmentGray = Color(0xFF76767C)
private val ProjectsSegmentLightGray = Color(0xFFBDBDBD)

@Composable
private fun ProjectsSegmentedControl(
    selectedPage: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
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
                ProjectsSegmentText(
                    text = "Все проекты",
                    selected = selectedPage == 0,
                    onClick = { onPageSelected(0) },
                )
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                ProjectsSegmentText(
                    text = "Мой проект",
                    selected = selectedPage == 1,
                    onClick = { onPageSelected(1) },
                )
            }
        }
        // Вертикальный разделитель по центру
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(1.dp)
                .height(20.dp)
                .background(ProjectsSegmentGray),
        )
    }
}

@Composable
private fun ProjectsSegmentText(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "projects_segment_scale",
    )

    Text(
        text = text,
        fontFamily = AppFonts.OpenSansRegular,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        fontSize = 20.sp,
        color = if (selected) ProjectsSegmentGray else ProjectsSegmentLightGray,
        modifier = Modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    )
}

// ==================== Страница "Мой проект" ====================

@Composable
private fun MyProjectPage(
    isAuthorized: Boolean,
    isPreview: Boolean,
    myProject: Project?,
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
            .align(Alignment.Center),
        contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
    )
}

@Composable
private fun MyProjectUnauthorizedState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Войдите, чтобы увидеть свой проект",
            fontFamily = AppFonts.OpenSansRegular,
            fontSize = 18.sp,
            color = AppColors.Color2,
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "У вас еще нет личного проекта",
            fontFamily = AppFonts.OpenSansRegular,
            fontSize = 20.sp,
            color = AppColors.Color2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
    }
}

@Composable
internal fun CustomTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var dragOffset by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Calculate animated offset for selection indicator
    val baseOffset = when (selectedTab) {
        0 -> 5.dp
        1 -> 128.5.dp
        2 -> 252.dp
        else -> 5.dp
    }

    val offsetX by animateDpAsState(
        targetValue = baseOffset + with(density) { dragOffset.toDp() },
        animationSpec = if (isDragging) tween(durationMillis = 0) else tween(durationMillis = 300)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .offset(y = (-30).dp), // Поднимаем таббар на 30dp вверх
        contentAlignment = Alignment.Center
    ) {
        // Main TabBar container
        Box(
            modifier = Modifier
                .width(380.dp)
                .height(60.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(30.dp), // Скругление 30dp
                    ambientColor = Color.Black.copy(alpha = 0.55f),
                    spotColor = Color.Black.copy(alpha = 0.55f),
                    clip = false
                )
                .border(
                    width = 2.dp,
                    color = TabBarBorder,
                    shape = RoundedCornerShape(30.dp) // Скругление 30dp
                )
                .background(
                    color = TabBarBackground,
                    shape = RoundedCornerShape(30.dp) // Скругление 30dp
                )
        ) {
            // Animated selection indicator with drag support
            Box(
                modifier = Modifier
                    .offset(x = offsetX, y = 5.dp)
                    .width(123.dp)
                    .height(50.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { _: Offset -> isDragging = true },
                            onDragEnd = {
                                isDragging = false
                                // Определяем ближайшую вкладку на основе позиции
                                val tabWidth = with(density) { 123.dp.toPx() }
                                val currentPosition = with(density) { baseOffset.toPx() } + dragOffset
                                val newTab = when {
                                    currentPosition < tabWidth * 0.5f -> 0
                                    currentPosition < tabWidth * 1.5f -> 1
                                    else -> 2
                                }
                                if (newTab != selectedTab) {
                                    onTabSelected(newTab)
                                }
                                dragOffset = 0f
                            },
                            onDragCancel = {
                                isDragging = false
                                dragOffset = 0f
                            }
                        ) { _, dragAmount ->
                            dragOffset += dragAmount
                            // Ограничиваем перетаскивание в пределах таббара
                            val minOffset = with(density) { -baseOffset.toPx() }
                            val maxOffset = with(density) {
                                (380.dp.toPx() - baseOffset.toPx() - 123.dp.toPx())
                            }
                            dragOffset = dragOffset.coerceIn(minOffset, maxOffset)
                        }
                    }
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

            // Tab items
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 1: Projects (SPbU logo)
                TabItem(
                    icon = Res.drawable.spbu_tab_logo,
                    iconSize = Pair(33.dp, 41.dp),
                    isSelected = selectedTab == 0,
                    onClick = { onTabSelected(0) }
                )

                // Tab 2: Statistics
                TabItem(
                    icon = Res.drawable.stats_tab_logo,
                    iconSize = Pair(30.dp, 27.5.dp),
                    isSelected = selectedTab == 1,
                    onClick = { onTabSelected(1) }
                )

                // Tab 3: Settings
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
            .width(123.dp)
            .height(50.dp)
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
