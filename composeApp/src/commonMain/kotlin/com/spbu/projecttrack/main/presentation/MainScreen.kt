package com.spbu.projecttrack.main.presentation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
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
import com.spbu.projecttrack.projects.presentation.ProjectsScreen
import com.spbu.projecttrack.projects.presentation.components.SuggestProjectResultAlert
import com.spbu.projecttrack.projects.presentation.detail.ProjectDetailScreen
import com.spbu.projecttrack.projects.presentation.detail.ProjectDetailScreenContent
import com.spbu.projecttrack.projects.presentation.detail.ProjectDetailUiState
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
    var showMyProject by remember { mutableStateOf(false) }
    var isRankingRoot by remember { mutableStateOf(true) }
    var isSettingsRoot by remember { mutableStateOf(true) }
    val isAuthorized by AuthManager.isAuthorized.collectAsState()
    val showTabBar = when (selectedTab) {
        0 -> !showMyProject
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
        isMyProjectOpen = showMyProject,
        onMyProjectOpenChange = { showMyProject = it },
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
    isMyProjectOpen: Boolean,
    onMyProjectOpenChange: (Boolean) -> Unit,
    onRankingRootChange: (Boolean) -> Unit,
    isSettingsRoot: Boolean,
    onSettingsRootChange: (Boolean) -> Unit,
    showTabBar: Boolean,
    initialMyProject: Project? = null
) {
    val contactRequestApi = remember { DependencyContainer.provideContactRequestApi() }
    val userProfileApi = remember { DependencyContainer.provideUserProfileApi() }
    val coroutineScope = rememberCoroutineScope()
    var submitAlert by remember { mutableStateOf<Pair<String, String>?>(null) }
    val logTag = "SuggestProject"
    // Личный проект пользователя загружается из профиля
    var myProject by remember { mutableStateOf(initialMyProject) }
    val isPreview = LocalInspectionMode.current



    LaunchedEffect(isAuthorized, isPreview) {
        if (isPreview) return@LaunchedEffect
        if (!isAuthorized) {
            myProject = null
            return@LaunchedEffect
        }

        val result = userProfileApi.getProfile()
        if (result.isSuccess) {
            val projects = result.getOrNull()?.projects.orEmpty()
            myProject = projects.firstOrNull()
            AppLog.d("MyProject", "Loaded projects: ${projects.size}")
        } else {
            val error = result.exceptionOrNull()
            if (error != null) {
                AppLog.e("MyProject", "Failed to load profile", error)
            } else {
                AppLog.e("MyProject", "Failed to load profile")
            }
            myProject = null
        }
    }

    BackHandler(enabled = isMyProjectOpen) {
        onMyProjectOpenChange(false)
    }

    val rootTopInsetModifier = if (showTabBar) {
        Modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
        )
    } else {
        Modifier
    }

    Scaffold(
        containerColor = Color.White, // Белый фон для Scaffold
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

                    if (selectedTab == 0 && !isMyProjectOpen) {
                        val density = LocalDensity.current
                        var suggestBtnHeightPx by remember { mutableStateOf(0) }
                        val suggestBtnHeightDp = if (suggestBtnHeightPx == 0) {
                            46.dp
                        } else {
                            with(density) { suggestBtnHeightPx.toDp() }
                        }

                        Box(
                            modifier = Modifier
                                .width(380.dp)
                                .offset(y = (-95).dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .onSizeChanged { suggestBtnHeightPx = it.height }
                            ) {
                                com.spbu.projecttrack.projects.presentation.components.SuggestProjectButton(
                                    onClick = { onShowSuggestProjectChange(true) }
                                )
                            }

                            if (isAuthorized) {
                                com.spbu.projecttrack.projects.presentation.components.MyProjectButton(
                                    onClick = { onMyProjectOpenChange(true) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(y = -(suggestBtnHeightDp + 10.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        val contentModifier = modifier
            .fillMaxSize()
            .then(rootTopInsetModifier)
        Box(
            modifier = contentModifier
        ) {
            when (selectedTab) {
                0 -> {
                    if (isMyProjectOpen) {
                        val project = myProject
                        if (project == null) {
                            MyProjectEmptyState(modifier = Modifier.fillMaxSize())
                        } else if (isPreview) {
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
                                onBackClick = { onMyProjectOpenChange(false) },
                                onRetry = {},
                                title = "Мой проект",
                                showBackButton = false,
                                showMyProjectMenu = true,
                                onMyProjectBackToProjects = { onMyProjectOpenChange(false) },
                                onMyProjectOpenStats = {
                                    onProjectStatsClick(project.slug ?: project.id)
                                }
                            )
                        } else {
                            val projectId = project.slug ?: project.id
                            val detailViewModel = remember(projectId) {
                                DependencyContainer.provideProjectDetailViewModel(projectId)
                            }
                            ProjectDetailScreen(
                                viewModel = detailViewModel,
                                onBackClick = { onMyProjectOpenChange(false) },
                                title = "Мой проект",
                                showBackButton = false,
                                showMyProjectMenu = true,
                                onMyProjectBackToProjects = { onMyProjectOpenChange(false) },
                                onMyProjectOpenStats = {
                                    onProjectStatsClick(projectId)
                                }
                            )
                        }
                    } else {
                        if (isPreview) {
                            // В превью не дергаем DI/VM
                            Box(modifier = Modifier.fillMaxSize())
                        } else {
                            val projectsViewModel = remember { DependencyContainer.provideProjectsViewModel() }
                            ProjectsScreen(
                                viewModel = projectsViewModel,
                                onProjectClick = onProjectDetailClick
                            )
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

            // Алерт "Предложить проект"
            com.spbu.projecttrack.projects.presentation.components.SuggestProjectAlert(
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
        }
    }

    SuggestProjectResultAlert(
        isVisible = submitAlert != null,
        title = submitAlert?.first ?: "",
        message = submitAlert?.second ?: "",
        onDismiss = { submitAlert = null }
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyProjectEmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.White)
    ) {
        Image(
            painter = painterResource(Res.drawable.spbu_logo),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
        )

        Scaffold(
            containerColor = Color.Transparent,
            contentColor = AppColors.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.White)
                        .padding(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Мой проект",
                        fontFamily = AppFonts.OpenSansBold,
                        fontSize = 40.sp,
                        color = AppColors.Color3,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "У вас еще нет личного проекта",
                        fontFamily = AppFonts.OpenSansRegular,
                        fontSize = 20.sp,
                        color = AppColors.Color2,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
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
