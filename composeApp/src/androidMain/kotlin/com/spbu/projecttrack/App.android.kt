package com.spbu.projecttrack

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.spbu.projecttrack.core.auth.AuthManager
import com.spbu.projecttrack.core.di.DependencyContainer
import com.spbu.projecttrack.core.network.NetworkSettings
import com.spbu.projecttrack.core.platform.isDebugBuild
import com.spbu.projecttrack.core.settings.AppLanguage
import com.spbu.projecttrack.core.settings.AppThemeMode
import com.spbu.projecttrack.core.settings.AppUiSettingsController
import com.spbu.projecttrack.core.settings.ITClinicTheme
import com.spbu.projecttrack.core.settings.appStrings
import com.spbu.projecttrack.core.storage.createAppPreferences
import com.spbu.projecttrack.core.update.AndroidAppUpdateChecker
import com.spbu.projecttrack.core.update.AndroidAppUpdateDialog
import com.spbu.projecttrack.core.update.AndroidAppUpdate
import com.spbu.projecttrack.main.presentation.MainScreen
import com.spbu.projecttrack.onboarding.presentation.OnboardingScreen
import com.spbu.projecttrack.projects.presentation.detail.ProjectDetailScreen
import com.spbu.projecttrack.rating.presentation.projectstats.ProjectStatsScreen
import com.spbu.projecttrack.rating.presentation.userstats.UserStatsScreen
import kotlinx.coroutines.launch

sealed class Screen {
    data object Onboarding : Screen()
    data object Main : Screen()
    data class ProjectDetail(val projectId: String) : Screen()
    data class ProjectStats(val projectId: String) : Screen()
    data class UserStats(
        val userId: String,
        val userName: String,
        val preferredProjectName: String?,
    ) : Screen()
}

@Composable
actual fun App() {
    val context = LocalContext.current
    val preferences = remember { createAppPreferences() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val debugBuild = isDebugBuild()
    val customHostIp by NetworkSettings.customHostIP.collectAsState()
    val authToken by AuthManager.authToken.collectAsState()
    val storedCustomHostIp = remember { preferences.getCustomHostIP() }
    var appLanguage by remember {
        mutableStateOf(AppLanguage.fromStorage(preferences.getAppLanguage()))
    }
    var appThemeMode by remember {
        mutableStateOf(AppThemeMode.fromStorage(preferences.getAppThemeMode()))
    }
    val strings = appStrings(appLanguage)

    LaunchedEffect(Unit) {
        println(com.spbu.projecttrack.core.network.ApiConfig.getDebugInfo())
        if (debugBuild) {
            AuthManager.clearToken()
            preferences.clearTokens()
        } else {
            preferences.getAccessToken()
                ?.takeIf { it.isNotBlank() }
                ?.let(AuthManager::setToken)
        }
    }

    LaunchedEffect(storedCustomHostIp) {
        NetworkSettings.setCustomHostIP(storedCustomHostIp)
    }

    LaunchedEffect(customHostIp) {
        if (customHostIp.isNullOrBlank()) {
            preferences.clearCustomHostIP()
        } else {
            preferences.saveCustomHostIP(customHostIp!!)
        }
    }

    LaunchedEffect(authToken) {
        if (authToken.isNullOrBlank()) {
            preferences.clearTokens()
        } else {
            preferences.saveAccessToken(authToken!!)
        }
    }

    LaunchedEffect(appLanguage) {
        preferences.saveAppLanguage(appLanguage.storageValue)
    }

    LaunchedEffect(appThemeMode) {
        preferences.saveAppThemeMode(appThemeMode.storageValue)
    }

    var isOnboardingVisible by remember {
        mutableStateOf(debugBuild || !preferences.isOnboardingCompleted())
    }
    var availableAndroidUpdate by remember { mutableStateOf<AndroidAppUpdate?>(null) }
    var mainSelectedTab by rememberSaveable { mutableStateOf(0) }
    val screenStack = remember { mutableStateListOf<Screen>() }

    LaunchedEffect(isOnboardingVisible) {
        if (!isOnboardingVisible) {
            availableAndroidUpdate = AndroidAppUpdateChecker.checkForAvailableUpdate(context)
        }
    }

    fun openScreen(screen: Screen) {
        screenStack.add(screen)
    }

    fun popScreen() {
        if (screenStack.isNotEmpty()) {
            screenStack.removeAt(screenStack.lastIndex)
        }
    }

    BackHandler(
        enabled = screenStack.isNotEmpty()
    ) {
        popScreen()
    }

    ITClinicTheme(
        language = appLanguage,
        themeMode = appThemeMode,
        settingsController = AppUiSettingsController(
            language = appLanguage,
            themeMode = appThemeMode,
            onLanguageChange = { appLanguage = it },
            onThemeModeChange = { appThemeMode = it },
        ),
    ) {
        if (isOnboardingVisible) {
            OnboardingScreen(
                onGitHubAuth = {
                    AuthManager.setTestToken()
                    preferences.setOnboardingCompleted()
                    isOnboardingVisible = false
                    scope.launch {
                        snackbarHostState.showSnackbar(strings.loginSuccessMessage)
                    }
                },
                onContinueWithoutAuth = {
                    preferences.setOnboardingCompleted()
                    isOnboardingVisible = false
                }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                MainScreen(
                    onProjectDetailClick = { projectId ->
                        openScreen(Screen.ProjectDetail(projectId))
                    },
                    onProjectStatsClick = { projectId ->
                        openScreen(Screen.ProjectStats(projectId))
                    },
                    onUserStatsClick = { userId, userName, preferredProjectName ->
                        openScreen(
                            Screen.UserStats(
                                userId = userId,
                                userName = userName,
                                preferredProjectName = preferredProjectName
                            )
                        )
                    },
                    selectedTab = mainSelectedTab,
                    onTabSelected = { mainSelectedTab = it },
                )

                when (val screen = screenStack.lastOrNull()) {
                    is Screen.ProjectDetail -> {
                        val detailViewModel = remember(screen.projectId) {
                            DependencyContainer.provideProjectDetailViewModel(screen.projectId)
                        }
                        ProjectDetailScreen(
                            viewModel = detailViewModel,
                            onBackClick = ::popScreen
                        )
                    }

                    is Screen.ProjectStats -> {
                        val statsViewModel = remember(screen.projectId) {
                            DependencyContainer.provideProjectStatsViewModel(screen.projectId)
                        }
                        ProjectStatsScreen(
                            viewModel = statsViewModel,
                            onBackClick = ::popScreen,
                            onOverallRatingClick = {
                                mainSelectedTab = 1
                                screenStack.clear()
                            },
                            onMemberStatsClick = { member ->
                                openScreen(
                                    Screen.UserStats(
                                        userId = member.userId ?: member.login ?: member.name,
                                        userName = member.name,
                                        preferredProjectName = screen.projectId,
                                    )
                                )
                            },
                        )
                    }

                    is Screen.UserStats -> {
                        val statsViewModel = remember(screen.userId, screen.userName, screen.preferredProjectName) {
                            DependencyContainer.provideUserStatsViewModel(
                                userId = screen.userId,
                                userName = screen.userName,
                                preferredProjectName = screen.preferredProjectName
                            )
                        }
                        UserStatsScreen(
                            viewModel = statsViewModel,
                            onBackClick = ::popScreen,
                            onProjectClick = { projectId ->
                                openScreen(Screen.ProjectStats(projectId))
                            },
                            onOverallRatingClick = {
                                mainSelectedTab = 1
                                screenStack.clear()
                            }
                        )
                    }

                    else -> Unit
                }
            }
        }

        availableAndroidUpdate?.let { update ->
            AndroidAppUpdateDialog(
                update = update,
                onDismiss = {
                    AndroidAppUpdateChecker.dismissUpdate(context, update.versionCode)
                    availableAndroidUpdate = null
                },
                onUpdateClick = {
                    AndroidAppUpdateChecker.openUpdatePage(context, update.apkUrl)
                    availableAndroidUpdate = null
                },
            )
        }

        SnackbarHost(hostState = snackbarHostState)
    }
}
