package com.spbu.projecttrack.main.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.composed
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.spbu.projecttrack.core.auth.AuthManager
import com.spbu.projecttrack.core.di.DependencyContainer
import com.spbu.projecttrack.projects.data.api.MemberApi
import com.spbu.projecttrack.core.email.FeedbackMailPayload
import com.spbu.projecttrack.core.email.FeedbackMailSender
import com.spbu.projecttrack.core.platform.openExternalUrl
import com.spbu.projecttrack.core.settings.AppLanguage
import com.spbu.projecttrack.core.settings.AppThemeMode
import com.spbu.projecttrack.core.settings.LocalAppStrings
import com.spbu.projecttrack.core.settings.LocalAppUiSettingsController
import com.spbu.projecttrack.core.settings.LocalSettingsPalette
import com.spbu.projecttrack.core.settings.localizeRuntime
import com.spbu.projecttrack.core.settings.localizedString
import com.spbu.projecttrack.core.storage.createAppPreferences
import com.spbu.projecttrack.core.theme.AppFonts
import com.spbu.projecttrack.core.ui.AppSnackbarHost
import com.spbu.projecttrack.projects.data.model.Member
import com.spbu.projecttrack.projects.data.model.User
import com.spbu.projecttrack.user.data.model.UserProfileCache
import com.spbu.projecttrack.user.data.model.UserProfileResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import projecttrack.composeapp.generated.resources.Res
import projecttrack.composeapp.generated.resources.arrow_back
import projecttrack.composeapp.generated.resources.github_logo
import projecttrack.composeapp.generated.resources.feedback_logo
import projecttrack.composeapp.generated.resources.lang_logo
import projecttrack.composeapp.generated.resources.logout
import projecttrack.composeapp.generated.resources.pencil
import projecttrack.composeapp.generated.resources.privacy_logo
import projecttrack.composeapp.generated.resources.spbu_logo
import projecttrack.composeapp.generated.resources.stats_dropdown_chevron
import projecttrack.composeapp.generated.resources.stats_footer_settings
import projecttrack.composeapp.generated.resources.theme_logo

private enum class SettingsDestination {
    Home,
    Settings,
    PrivacyPolicy,
    Feedback,
    Profile,
}

private val SettingsScrollFadeHeight = 32.dp
private val SettingsLoginChipWidth = 180.dp
private val SettingsLoginChipHeight = 30.dp
private val SettingsLoginChipShape = RoundedCornerShape(25.dp)
private val SettingsLoginChipHorizontalPadding = 8.dp
private val SettingsFallbackRoleOptions = listOf(
    "DevOps",
    "Designer",
    "Architect",
    "QA",
    "Android Developer",
    "UX Researcher",
    "Frontend Developer",
    "Backend Developer",
    "iOS Developer",
    "Fullstack Developer",
    "CV Engineer",
    "PM",
    "Data Engineer",
    "IoT Engineer",
    "Product Analyst",
    "Analyst",
    "Data Analyst",
    "ML Engineer",
    "NLP Engineer",
    "Team Lead",
    "Content Manager",
    "Project Manager",
    "Business Analyst",
)

private fun localizedRoleLabel(role: String): String {
    val normalized = role.trim().lowercase()
    return when (normalized) {
        "backend-разработчик", "backend developer" ->
            localizeRuntime("Backend-разработчик", "Backend Developer")
        "frontend-разработчик", "frontend developer" ->
            localizeRuntime("Frontend-разработчик", "Frontend Developer")
        "fullstack-разработчик", "fullstack developer" ->
            localizeRuntime("Fullstack-разработчик", "Fullstack Developer")
        "аналитик", "analyst", "business analyst", "product analyst", "data analyst" ->
            when (normalized) {
                "business analyst" -> localizeRuntime("Бизнес-аналитик", "Business Analyst")
                "product analyst" -> localizeRuntime("Продуктовый аналитик", "Product Analyst")
                "data analyst" -> localizeRuntime("Дата-аналитик", "Data Analyst")
                else -> localizeRuntime("Аналитик", "Analyst")
            }
        "тестировщик", "qa" ->
            localizeRuntime("Тестировщик", "QA")
        "designer" ->
            localizeRuntime("Designer", "Designer")
        "project manager", "pm" ->
            if (normalized == "pm") "PM" else localizeRuntime("Project Manager", "Project Manager")
        "devops" -> "DevOps"
        "architect" -> localizeRuntime("Архитектор", "Architect")
        "android developer" -> localizeRuntime("Android-разработчик", "Android Developer")
        "ios developer" -> localizeRuntime("iOS-разработчик", "iOS Developer")
        "ux researcher" -> localizeRuntime("UX-исследователь", "UX Researcher")
        "cv engineer" -> localizeRuntime("CV-инженер", "CV Engineer")
        "data engineer" -> localizeRuntime("Дата-инженер", "Data Engineer")
        "iot engineer" -> localizeRuntime("IoT-инженер", "IoT Engineer")
        "ml engineer" -> localizeRuntime("ML-инженер", "ML Engineer")
        "nlp engineer" -> localizeRuntime("NLP-инженер", "NLP Engineer")
        "team lead" -> localizeRuntime("Тимлид", "Team Lead")
        "content manager" -> localizeRuntime("Контент-менеджер", "Content Manager")
        else -> role
    }
}

private data class EditableProfileForm(
    val name: String,
    val surname: String,
    val patronymic: String,
    val email: String,
    val phone: String,
    val notificationsEnabled: Boolean,
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SettingsTabScreen(
    modifier: Modifier = Modifier,
    onRootDestinationChange: (Boolean) -> Unit = {},
) {
    val strings = LocalAppStrings.current
    val themeComingSoonMessage = localizedString(
        "Тёмная тема будет добавлена в следующих версиях.",
        "Dark theme will be added in future versions.",
    )
    val palette = LocalSettingsPalette.current
    val settingsController = LocalAppUiSettingsController.current
    val preferences = remember { createAppPreferences() }
    val userProfileApi = remember { DependencyContainer.provideUserProfileApi() }
    val mobileAuthApi = remember { DependencyContainer.provideMobileAuthApi() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Используем .value как initial — избегаем один ложный кадр "незалогинен"
    val isAuthorized by AuthManager.isAuthorized.collectAsState()

    var destination by rememberSaveable { mutableStateOf(SettingsDestination.Home) }
    var feedbackText by rememberSaveable { mutableStateOf("") }
    var isFeedbackSending by rememberSaveable { mutableStateOf(false) }
    var isProfileLoading by rememberSaveable { mutableStateOf(false) }
    var isProfileSaving by rememberSaveable { mutableStateOf(false) }
    var profile by remember { mutableStateOf<UserProfileResponse?>(null) }
    var profileError by rememberSaveable { mutableStateOf<String?>(null) }
    var editProfileForm by remember { mutableStateOf<EditableProfileForm?>(null) }
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var showThanksDialog by rememberSaveable { mutableStateOf(false) }

    fun openEditProfile() {
        val currentProfile = profile ?: return
        editProfileForm = EditableProfileForm(
            name = currentProfile.user.fullName.name,
            surname = currentProfile.user.fullName.surname,
            patronymic = currentProfile.user.fullName.patronymic,
            email = currentProfile.user.email,
            phone = currentProfile.user.phone,
            notificationsEnabled = preferences.isProjectStatusNotificationsEnabled(),
        )
    }

    fun resetToHomeIfNeeded() {
        if (!isAuthorized && destination == SettingsDestination.Profile) {
            destination = SettingsDestination.Home
        }
    }

    fun loadProfile(forceRefresh: Boolean = false) {
        if (!isAuthorized) {
            profile = null
            profileError = null
            resetToHomeIfNeeded()
            return
        }

        // Отдаём кэш сразу, чтобы не было пустого экрана
        if (!forceRefresh) {
            val cached = UserProfileCache.get()
            if (cached != null) {
                profile = cached
                return
            }
        }

        scope.launch {
            isProfileLoading = true
            profileError = null
            val result = userProfileApi.getProfile()
            if (result.isSuccess) {
                val loaded = result.getOrNull()
                profile = loaded
                loaded?.let { UserProfileCache.set(it) }
            } else {
                profile = null
                profileError = strings.profileLoadError
                snackbarHostState.showSnackbar(strings.profileLoadError)
            }
            isProfileLoading = false
        }
    }

    LaunchedEffect(isAuthorized) {
        if (isAuthorized) {
            loadProfile()
        } else {
            profile = null
            profileError = null
            editProfileForm = null
            showLogoutDialog = false
            resetToHomeIfNeeded()
        }
    }

    // Синхронный хелпер: устанавливает destination И сообщает родителю об isRoot
    // в одном handler-е, чтобы Compose применил оба изменения за один кадр.
    // Это устраняет визуальный прыжок верстки при переходе субэкран → главная.
    fun navigate(dest: SettingsDestination) {
        destination = dest
        onRootDestinationChange(dest == SettingsDestination.Home)
    }

    BackHandler(enabled = destination != SettingsDestination.Home) {
        navigate(SettingsDestination.Home)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        SettingsWatermarkBackground()

        AnimatedContent(
            targetState = destination,
            transitionSpec = {
                val animatedSubScreens = setOf(
                    SettingsDestination.Settings,
                    SettingsDestination.PrivacyPolicy,
                    SettingsDestination.Feedback,
                    SettingsDestination.Profile,
                )
                val involvesSlidingSubScreen =
                    initialState in animatedSubScreens || targetState in animatedSubScreens

                if (involvesSlidingSubScreen) {
                    if (targetState in animatedSubScreens) {
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth / 6 },
                            animationSpec = tween(durationMillis = 260),
                        ) + fadeIn(animationSpec = tween(durationMillis = 220))) togetherWith
                            (slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -fullWidth / 10 },
                                animationSpec = tween(durationMillis = 220),
                            ) + fadeOut(animationSpec = tween(durationMillis = 180)))
                    } else {
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth / 10 },
                            animationSpec = tween(durationMillis = 220),
                        ) + fadeIn(animationSpec = tween(durationMillis = 180))) togetherWith
                            (slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth / 6 },
                                animationSpec = tween(durationMillis = 260),
                            ) + fadeOut(animationSpec = tween(durationMillis = 180)))
                    }
                } else {
                    fadeIn(animationSpec = tween(durationMillis = 120)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 90))
                }
            },
            label = "settings_destination_transition",
        ) { currentDestination ->
            when (currentDestination) {
                SettingsDestination.Home -> SettingsHomeScreen(
                    isAuthorized = isAuthorized,
                    isProfileLoading = isProfileLoading,
                    profile = profile,
                    onSettingsClick = { navigate(SettingsDestination.Settings) },
                    onPolicyClick = { navigate(SettingsDestination.PrivacyPolicy) },
                    onFeedbackClick = { navigate(SettingsDestination.Feedback) },
                    onProfileClick = {
                        if (isAuthorized) {
                            navigate(SettingsDestination.Profile)
                        }
                    },
                    onLoginClick = {
                        openExternalUrl(mobileAuthApi.loginUrl)
                    },
                )

                SettingsDestination.Settings -> SettingsPreferencesScreen(
                    onBackClick = { navigate(SettingsDestination.Home) },
                    onThemeClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(themeComingSoonMessage)
                        }
                    },
                )

                SettingsDestination.PrivacyPolicy -> SettingsPolicyScreen(
                    onBackClick = { navigate(SettingsDestination.Home) },
                )

                SettingsDestination.Feedback -> SettingsFeedbackScreen(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    isSending = isFeedbackSending,
                    onBackClick = { navigate(SettingsDestination.Home) },
                    onSendClick = {
                        val trimmedFeedback = feedbackText.trim()
                        if (trimmedFeedback.isBlank() || isFeedbackSending) return@SettingsFeedbackScreen

                        scope.launch {
                            try {
                                isFeedbackSending = true
                                val currentProfile = profile
                                val senderName = currentProfile?.user?.fullName?.displayName()
                                    ?.takeIf { it.isNotBlank() }
                                    ?: strings.profileCardFallbackName
                                val senderEmail = currentProfile?.user?.email.orEmpty()

                                val result = FeedbackMailSender.send(
                                    FeedbackMailPayload(
                                        senderName = senderName,
                                        senderEmail = senderEmail,
                                        message = trimmedFeedback,
                                    )
                                )

                                if (result.isSuccess) {
                                    feedbackText = ""
                                    showThanksDialog = true
                                } else {
                                    snackbarHostState.showSnackbar(
                                        result.exceptionOrNull()?.message ?: strings.feedbackSendError
                                    )
                                }
                            } finally {
                                isFeedbackSending = false
                            }
                        }
                    },
                )

                SettingsDestination.Profile -> SettingsProfileScreen(
                    isAuthorized = isAuthorized,
                    isLoading = isProfileLoading,
                    profile = profile,
                    errorMessage = profileError,
                    onBackClick = { navigate(SettingsDestination.Home) },
                    onEditClick = { openEditProfile() },
                    onLogoutClick = { showLogoutDialog = true },
                )
            }
        }

        if (editProfileForm != null) {
            EditProfileDialog(
                form = editProfileForm!!,
                isSaving = isProfileSaving,
                onDismiss = {
                    if (!isProfileSaving) editProfileForm = null
                },
                onFormChange = { editProfileForm = it },
                onSaveClick = {
                    val form = editProfileForm ?: return@EditProfileDialog
                    scope.launch {
                        isProfileSaving = true

                        val personalResult = userProfileApi.editPersonalData(
                            name = form.name,
                            surname = form.surname,
                            patronymic = form.patronymic,
                        )

                        val accountResult = userProfileApi.editAccountData(
                            email = form.email,
                            phone = form.phone,
                        )

                        if (personalResult.isSuccess && accountResult.isSuccess) {
                            preferences.setProjectStatusNotificationsEnabled(form.notificationsEnabled)
                            editProfileForm = null
                            UserProfileCache.invalidate()
                            loadProfile(forceRefresh = true)
                        } else {
                            snackbarHostState.showSnackbar(strings.profileSaveError)
                        }

                        isProfileSaving = false
                    }
                },
            )
        }

        if (showLogoutDialog) {
            LogoutDialog(
                onDismiss = { showLogoutDialog = false },
                onConfirm = {
                    showLogoutDialog = false
                    scope.launch {
                        DependencyContainer.provideMobileAuthApi().logout()
                        preferences.clearTokens()
                        AuthManager.clearToken()
                        UserProfileCache.invalidate()
                        navigate(SettingsDestination.Home)
                    }
                },
            )
        }

        if (showThanksDialog) {
            ThanksDialog(
                onDismiss = { showThanksDialog = false },
            )
        }

        AppSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            bottomSpacing = 24.dp,
        )
    }
}

@Composable
private fun SettingsHomeScreen(
    isAuthorized: Boolean,
    isProfileLoading: Boolean,
    profile: UserProfileResponse?,
    onSettingsClick: () -> Unit,
    onPolicyClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLoginClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val displayName = profile?.user?.fullName?.displayName()
        ?.takeIf { it.isNotBlank() }
        ?: strings.profileCardFallbackName
    val email = profile?.user?.email?.takeIf { it.isNotBlank() } ?: strings.notSpecified
    val currentProject = profile?.projects?.firstOrNull()?.name ?: strings.noCurrentProject
    val avatarUrl = profile?.githubAvatarUrl()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp)
            .padding(top = 0.dp, bottom = 92.dp),
    ) {
        SettingsTitle(strings.infoTitle)
        Spacer(modifier = Modifier.height(30.dp))

        if (isAuthorized) {
            ProfileSummaryCard(
                displayName = displayName,
                email = email,
                projectName = currentProject,
                avatarUrl = avatarUrl,
                isLoading = isProfileLoading,
                onClick = onProfileClick,
            )
            Spacer(modifier = Modifier.height(22.dp))
        } else {
            LoginChip(onClick = onLoginClick)
            Spacer(modifier = Modifier.height(28.dp))
        }

        SettingsMenuRow(
            iconRes = Res.drawable.stats_footer_settings,
            label = strings.settingsTitle,
            onClick = onSettingsClick,
        )
        Spacer(modifier = Modifier.height(18.dp))
        SettingsMenuRow(
            iconRes = Res.drawable.privacy_logo,
            label = strings.privacyPolicyTitle.replace("\n", " "),
            onClick = onPolicyClick,
        )
        Spacer(modifier = Modifier.height(18.dp))
        SettingsMenuRow(
            iconRes = Res.drawable.feedback_logo,
            label = strings.feedbackTitle.replace("\n", " "),
            onClick = onFeedbackClick,
        )
    }
}

@Composable
private fun SettingsPreferencesScreen(
    onBackClick: () -> Unit,
    onThemeClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val settingsController = LocalAppUiSettingsController.current

    SettingsSubScreen(
        title = strings.settingsTitle,
        onBackClick = onBackClick,
    ) {
        SettingsDropdownValueRow(
            icon = {
                Image(
                    painter = painterResource(Res.drawable.lang_logo),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(LocalSettingsPalette.current.secondaryText),
                )
            },
            label = strings.languageLabel,
            value = when (settingsController.language) {
                AppLanguage.Russian -> strings.languageRuShort
                AppLanguage.English -> strings.languageEnShort
            },
            options = listOf(
                DropdownOption(
                    key = AppLanguage.Russian.storageValue,
                    label = strings.languageRuLong,
                ),
                DropdownOption(
                    key = AppLanguage.English.storageValue,
                    label = strings.languageEnLong,
                ),
            ),
            selectedKey = settingsController.language.storageValue,
            onOptionSelected = { selected ->
                when (selected) {
                    AppLanguage.Russian.storageValue -> {
                        settingsController.onLanguageChange(AppLanguage.Russian)
                    }

                    AppLanguage.English.storageValue -> {
                        settingsController.onLanguageChange(AppLanguage.English)
                    }
                }
            },
        )
        Spacer(modifier = Modifier.height(14.dp))
        SettingsDropdownValueRow(
            icon = {
                Image(
                    painter = painterResource(Res.drawable.theme_logo),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(LocalSettingsPalette.current.secondaryText),
                )
            },
            label = strings.themeLabel,
            value = strings.themeLight,
            options = emptyList(),
            selectedKey = AppThemeMode.Light.storageValue,
            onOptionSelected = { },
            canExpand = false,
            showChevron = false,
            onRowClick = onThemeClick,
        )
    }
}

@Composable
private fun SettingsPolicyScreen(
    onBackClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val palette = LocalSettingsPalette.current
    val scrollState = rememberScrollState()
    val canScrollUp by remember { derivedStateOf { scrollState.value > 0 } }
    val canScrollDown by remember { derivedStateOf { scrollState.value < scrollState.maxValue } }
    val topFadeAlpha by animateFloatAsState(
        targetValue = if (canScrollUp) 1f else 0f,
        animationSpec = tween(200),
        label = "policyScrollTopFade",
    )
    val bottomFadeAlpha by animateFloatAsState(
        targetValue = if (canScrollDown) 1f else 0f,
        animationSpec = tween(200),
        label = "policyScrollBottomFade",
    )

    SettingsSubScreen(
        title = strings.privacyPolicyTitle,
        onBackClick = onBackClick,
        titleFontSize = 24.sp,
        titleLineHeight = 21.sp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val fadeHeight = SettingsScrollFadeHeight.toPx()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 1f - topFadeAlpha),
                                Color.Black,
                            ),
                            startY = 0f,
                            endY = fadeHeight.coerceAtMost(size.height),
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black,
                                Color.Black.copy(alpha = 1f - bottomFadeAlpha),
                            ),
                            startY = (size.height - fadeHeight).coerceAtLeast(0f),
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
            ) {
                Text(
                    text = strings.policyBody,
                    color = palette.secondaryText,
                    fontFamily = AppFonts.OpenSansRegular,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                )
                Spacer(modifier = Modifier.height(96.dp))
            }
        }
    }
}

@Composable
private fun SettingsFeedbackScreen(
    value: String,
    onValueChange: (String) -> Unit,
    isSending: Boolean,
    onBackClick: () -> Unit,
    onSendClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val palette = LocalSettingsPalette.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val sendEnabled = value.trim().isNotBlank() && !isSending
    val sendButtonColor by animateColorAsState(
        targetValue = if (sendEnabled) palette.accent else palette.disabledButton,
        animationSpec = tween(durationMillis = 300),
        label = "send_button_color",
    )

    SettingsSubScreen(
        title = strings.feedbackTitle,
        onBackClick = onBackClick,
        titleLineHeight = 35.sp,
    ) {
        // Весь Box — кликабельный: тап в любом пустом месте скрывает клавиатуру.
        // BasicTextField поглощает тапы внутри себя, поэтому фокус при вводе
        // не сбрасывается. ActionButton обрабатывает свои тапы независимо.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    }
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = strings.helpUsTitle,
                    color = palette.primaryText,
                    fontFamily = AppFonts.OpenSansBold,
                    fontSize = 16.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.feedbackDescription,
                    color = palette.primaryText,
                    fontFamily = AppFonts.OpenSansRegular,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                Spacer(modifier = Modifier.height(16.dp))

                MultiLineField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = strings.feedbackPlaceholder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                )

                Spacer(modifier = Modifier.height(22.dp))

                ActionButton(
                    text = strings.sendLabel,
                    background = sendButtonColor,
                    onClick = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        onSendClick()
                    },
                    enabled = sendEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (isSending) {
                    Spacer(modifier = Modifier.height(14.dp))
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(24.dp),
                        color = palette.accent,
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsProfileScreen(
    isAuthorized: Boolean,
    isLoading: Boolean,
    profile: UserProfileResponse?,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val palette = LocalSettingsPalette.current
    val projectsApi = remember { DependencyContainer.provideProjectsApi() }
    val memberApi = remember { DependencyContainer.provideMemberApi() }
    val scope = rememberCoroutineScope()
    val currentUserId by AuthManager.currentUserId.collectAsState()
    val fullName = profile?.user?.fullName?.displayName()
        ?.takeIf { it.isNotBlank() }
        ?: strings.profileCardFallbackName
    val email = profile?.user?.email?.takeIf { it.isNotBlank() } ?: strings.notSpecified
    val phone = profile?.user?.phone?.takeIf { it.isNotBlank() } ?: strings.notSpecified
    val avatarUrl = profile?.githubAvatarUrl()
    val projectName = profile?.projects?.firstOrNull()?.name?.takeIf { it.isNotBlank() }
        ?: localizedString("Проект не указан", "Project not specified")
    val currentProjectKey = profile?.projects?.firstOrNull()?.slug?.takeIf { it.isNotBlank() }
        ?: profile?.projects?.firstOrNull()?.id
    var teamMembers by remember(currentProjectKey) { mutableStateOf<List<Member>>(emptyList()) }
    var teamUsers by remember(currentProjectKey) { mutableStateOf<List<User>>(emptyList()) }
    var isTeamLoading by remember(currentProjectKey) { mutableStateOf(false) }
    var editedOwnRoles by rememberSaveable(currentProjectKey) { mutableStateOf<List<String>?>(null) }
    var isRoleSaving by rememberSaveable { mutableStateOf(false) }
    var roleSaveError by rememberSaveable { mutableStateOf<String?>(null) }
    var isRoleMenuExpanded by rememberSaveable(currentProjectKey) { mutableStateOf(false) }

    suspend fun loadTeamData() {
        if (!isAuthorized || currentProjectKey.isNullOrBlank()) {
            teamMembers = emptyList()
            teamUsers = emptyList()
            isTeamLoading = false
            return
        }

        isTeamLoading = true
        val result = projectsApi.getProjectById(currentProjectKey)
        val response = result.getOrNull()
        teamMembers = response?.members.orEmpty()
        teamUsers = response?.users.orEmpty()
        isTeamLoading = false
    }

    LaunchedEffect(isAuthorized, currentProjectKey) {
        loadTeamData()
    }

    val orderedTeamMembers = remember(teamMembers, teamUsers, fullName, currentUserId) {
        teamMembers.sortedWith(
            compareByDescending<Member> { member ->
                member.belongsToProfile(fullName, teamUsers, currentUserId)
            }.thenBy { it.name.lowercase() }
        )
    }
    val currentMember = remember(orderedTeamMembers, fullName, teamUsers, currentUserId) {
        orderedTeamMembers.firstOrNull { it.belongsToProfile(fullName, teamUsers, currentUserId) }
    }
    val currentRoles = remember(currentMember, editedOwnRoles) {
        editedOwnRoles ?: currentMember?.roles
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?: currentMember?.role
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.distinct()
                .orEmpty()
    }
    val roleSaveErrorText = localizedString(
        "Не удалось сохранить. Попробуйте ещё раз.",
        "Failed to save. Please try again.",
    )
    val availableRoleOptions = remember(orderedTeamMembers, currentRoles) {
        buildList {
            currentRoles.forEach { role ->
                if (!contains(role)) {
                    add(role)
                }
            }
            orderedTeamMembers.forEach { member ->
                val memberRoles = if (member.roles.isNotEmpty()) {
                    member.roles
                } else {
                    member.role
                        ?.split(",")
                        ?.map { it.trim() }
                        ?.filter { it.isNotBlank() }
                        .orEmpty()
                }

                memberRoles.forEach { role ->
                    val trimmedRole = role.trim()
                    if (trimmedRole.isNotBlank() && !contains(trimmedRole)) {
                        add(trimmedRole)
                    }
                }
            }
            SettingsFallbackRoleOptions.forEach { role ->
                if (!contains(role)) {
                    add(role)
                }
            }
        }
    }

    SettingsSubScreen(
        title = strings.profileTitle,
        onBackClick = onBackClick,
    ) {
        if (!isAuthorized) {
            Text(
                text = strings.continueWithoutAuth,
                color = palette.secondaryText,
                fontFamily = AppFonts.OpenSansRegular,
                fontSize = 16.sp,
            )
            return@SettingsSubScreen
        }

        if (isLoading && profile == null) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = palette.accent, strokeWidth = 2.dp)
            }
            return@SettingsSubScreen
        }

        errorMessage?.let {
            Text(
                text = it,
                color = palette.secondaryText,
                fontFamily = AppFonts.OpenSansRegular,
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        ProfilePrimaryCard(
            fullName = fullName,
            email = if (isLoading) "..." else email,
            phone = if (isLoading) "..." else phone,
            avatarUrl = avatarUrl,
            onEditClick = onEditClick,
        )

        Spacer(modifier = Modifier.height(18.dp))

        ProfileTeamSection(
            members = orderedTeamMembers,
            teamUsers = teamUsers,
            fullName = fullName,
            currentUserId = currentUserId,
            editedOwnRoles = editedOwnRoles,
            isLoading = isTeamLoading,
            isRoleSaving = isRoleSaving,
            isRoleMenuExpanded = isRoleMenuExpanded,
            roleOptions = availableRoleOptions,
            roleError = roleSaveError,
            onEditOwnRole = {
                roleSaveError = null
                isRoleMenuExpanded = true
            },
            onDismissRoleMenu = { isRoleMenuExpanded = false },
            onToggleOwnRole = { selectedRole ->
                val member = currentMember
                if (member == null) return@ProfileTeamSection
                val trimmed = selectedRole.trim()
                if (trimmed.isBlank()) return@ProfileTeamSection
                val updatedRoles = currentRoles.toMutableList().apply {
                    if (contains(trimmed)) {
                        remove(trimmed)
                    } else {
                        add(trimmed)
                    }
                }.distinct()
                roleSaveError = null
                scope.launch {
                    isRoleSaving = true
                    val result = memberApi.editMemberRoles(member, updatedRoles)
                    isRoleSaving = false
                    if (result.isSuccess) {
                        editedOwnRoles = updatedRoles
                        teamMembers = teamMembers.map { existing ->
                            if (existing.id == member.id) existing.copy(roles = updatedRoles) else existing
                        }
                    } else {
                        roleSaveError = roleSaveErrorText
                    }
                }
            },
            onClearOwnRoles = {
                val member = currentMember
                if (member == null || currentRoles.isEmpty()) return@ProfileTeamSection
                roleSaveError = null
                scope.launch {
                    isRoleSaving = true
                    val result = memberApi.editMemberRoles(member, emptyList())
                    isRoleSaving = false
                    if (result.isSuccess) {
                        editedOwnRoles = emptyList()
                        teamMembers = teamMembers.map { existing ->
                            if (existing.id == member.id) existing.copy(roles = emptyList()) else existing
                        }
                    } else {
                        roleSaveError = roleSaveErrorText
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(18.dp))

        ProfileProjectCard(
            projectName = if (isLoading) "..." else projectName,
        )

        Spacer(modifier = Modifier.height(26.dp))

        LogoutActionButton(
            text = strings.logoutLabel,
            onClick = onLogoutClick,
        )
    }
}

@Composable
private fun ProfilePrimaryCard(
    fullName: String,
    email: String,
    phone: String,
    avatarUrl: String?,
    onEditClick: () -> Unit,
) {
    val palette = LocalSettingsPalette.current
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = palette.surface,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ProfileAvatar(
                label = fullName,
                avatarUrl = avatarUrl,
                modifier = Modifier.size(72.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = fullName,
                    color = palette.primaryText,
                    fontFamily = AppFonts.OpenSansBold,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                )
                Text(
                    text = email,
                    color = palette.secondaryText,
                    fontFamily = AppFonts.OpenSansRegular,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = phone,
                    color = palette.secondaryText,
                    fontFamily = AppFonts.OpenSansRegular,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.accent.copy(alpha = 0.08f))
                    .pressableClickable(
                        interactionSource = interactionSource,
                        pressedScale = 0.9f,
                        onClick = onEditClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.pencil),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    colorFilter = ColorFilter.tint(palette.accent),
                )
            }
        }
    }
}

@Composable
private fun ProfileTeamSection(
    members: List<Member>,
    teamUsers: List<User>,
    fullName: String,
    currentUserId: Int?,
    editedOwnRoles: List<String>?,
    isLoading: Boolean,
    isRoleSaving: Boolean,
    isRoleMenuExpanded: Boolean,
    roleOptions: List<String>,
    roleError: String?,
    onEditOwnRole: () -> Unit,
    onDismissRoleMenu: () -> Unit,
    onToggleOwnRole: (String) -> Unit,
    onClearOwnRoles: () -> Unit,
) {
    val palette = LocalSettingsPalette.current
    val teamTitle = localizedString("Команда", "Team")
    val noTeamLabel = localizedString("Нет данных о команде", "No team data")
    val clearRoleLabel = localizedString("Убрать все роли", "Clear all roles")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = palette.surface,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
        ) {
            Text(
                text = teamTitle,
                color = palette.primaryText,
                fontFamily = AppFonts.OpenSansSemiBold,
                fontSize = 20.sp,
                lineHeight = 20.sp,
            )
            Spacer(modifier = Modifier.height(9.dp))
            HorizontalDivider(color = palette.border.copy(alpha = 0.75f))
            Spacer(modifier = Modifier.height(6.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = palette.accent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                members.isEmpty() -> {
                    Text(
                        text = noTeamLabel,
                        color = palette.secondaryText,
                        fontFamily = AppFonts.OpenSansRegular,
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                    )
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        members.forEach { member ->
                            val isCurrentUser = member.belongsToProfile(fullName, teamUsers, currentUserId)
                            val resolvedRoles = if (isCurrentUser) {
                                editedOwnRoles ?: member.roles
                            } else {
                                member.roles
                            }.map { it.trim() }
                                .filter { it.isNotBlank() }
                                .distinct()
                            val fallbackRoles = member.role
                                ?.split(",")
                                ?.map { it.trim() }
                                ?.filter { it.isNotBlank() }
                                ?.distinct()
                                .orEmpty()
                            val resolvedRoleLabel = (if (editedOwnRoles != null && isCurrentUser) {
                                resolvedRoles
                            } else {
                                resolvedRoles.ifEmpty { fallbackRoles }
                            }).joinToString(", ") { localizedRoleLabel(it) }
                            val selectedRoles = (if (editedOwnRoles != null && isCurrentUser) {
                                resolvedRoles
                            } else {
                                resolvedRoles.ifEmpty { fallbackRoles }
                            }).toSet()
                            val resolvedName = if (isCurrentUser) {
                                fullName
                            } else {
                                teamUsers.firstOrNull { user ->
                                    user.id == member.user?.toString()
                                }?.name?.takeIf { !it.isNullOrBlank() } ?: member.name
                            }

                            ProfileTeamMemberCard(
                                name = resolvedName,
                                role = resolvedRoleLabel.ifBlank {
                                    localizedString("роль не указана", "role not specified")
                                },
                                selectedRoleKeys = selectedRoles,
                                isCurrentUser = isCurrentUser,
                                onEditClick = if (isCurrentUser) onEditOwnRole else null,
                                isRoleSaving = isCurrentUser && isRoleSaving,
                                isRoleMenuExpanded = isCurrentUser && isRoleMenuExpanded,
                                roleOptions = if (isCurrentUser) roleOptions else emptyList(),
                                clearRoleLabel = clearRoleLabel,
                                onDismissRoleMenu = if (isCurrentUser) onDismissRoleMenu else null,
                                onRoleToggle = if (isCurrentUser) onToggleOwnRole else null,
                                onClearRoles = if (isCurrentUser) onClearOwnRoles else null,
                            )
                        }
                    }

                    roleError?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = it,
                            color = Color(0xFFD32F2F),
                            fontFamily = AppFonts.OpenSansRegular,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileTeamMemberCard(
    name: String,
    role: String,
    selectedRoleKeys: Set<String>,
    isCurrentUser: Boolean,
    onEditClick: (() -> Unit)?,
    isRoleSaving: Boolean = false,
    isRoleMenuExpanded: Boolean = false,
    roleOptions: List<String> = emptyList(),
    clearRoleLabel: String = "",
    onDismissRoleMenu: (() -> Unit)? = null,
    onRoleToggle: ((String) -> Unit)? = null,
    onClearRoles: (() -> Unit)? = null,
) {
    val palette = LocalSettingsPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val noRoleOptionsLabel = localizedString("Нет доступных ролей", "No roles available")
    val rowLabel = buildAnnotatedString {
        pushStyle(
            SpanStyle(
                color = palette.secondaryText,
                fontFamily = AppFonts.OpenSansRegular,
                fontSize = 12.sp,
            )
        )
        append(name)
        append(", ")
        pop()
        pushStyle(
            SpanStyle(
                color = palette.secondaryText,
                fontFamily = AppFonts.OpenSansRegular,
                fontSize = 10.sp,
            )
        )
        append(role)
        pop()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = rowLabel,
            color = palette.secondaryText,
            fontFamily = AppFonts.OpenSansRegular,
            fontSize = 12.sp,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f),
        )

        if (isCurrentUser && onEditClick != null) {
            Box {
                Surface(
                    modifier = Modifier.pressableClickable(
                        interactionSource = interactionSource,
                        pressedScale = 0.985f,
                        onClick = onEditClick,
                        enabled = !isRoleSaving,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.dialogBorder),
                    shadowElevation = 6.dp,
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isRoleSaving) {
                            CircularProgressIndicator(
                                color = palette.accent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp),
                            )
                        } else {
                            Image(
                                painter = painterResource(Res.drawable.pencil),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                colorFilter = ColorFilter.tint(palette.secondaryText),
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = isRoleMenuExpanded,
                    onDismissRequest = { onDismissRoleMenu?.invoke() },
                    offset = DpOffset(0.dp, 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    containerColor = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 14.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE4E4E7)),
                    modifier = Modifier
                        .widthIn(min = 210.dp, max = 260.dp)
                        .heightIn(max = 260.dp),
                ) {
                    val clearSelected = selectedRoleKeys.isEmpty()
                    DropdownMenuItem(
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                if (clearSelected) palette.accent.copy(alpha = 0.08f) else Color.Transparent
                            ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        text = {
                            Text(
                                text = clearRoleLabel,
                                fontFamily = if (clearSelected) AppFonts.OpenSansSemiBold else AppFonts.OpenSansRegular,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = if (clearSelected) palette.accent else palette.secondaryText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 9.dp),
                            )
                        },
                        onClick = { onClearRoles?.invoke() },
                        enabled = !isRoleSaving,
                    )

                    if (roleOptions.isEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = noRoleOptionsLabel,
                                    fontFamily = AppFonts.OpenSansRegular,
                                    fontSize = 13.sp,
                                )
                            },
                            onClick = {},
                            enabled = false,
                        )
                    } else {
                        roleOptions.forEach { option ->
                            val isSelected = option in selectedRoleKeys
                            DropdownMenuItem(
                                modifier = Modifier
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(
                                        if (isSelected) palette.accent.copy(alpha = 0.08f) else Color.Transparent
                                    ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                text = {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = localizedRoleLabel(option),
                                            fontFamily = if (isSelected) AppFonts.OpenSansSemiBold else AppFonts.OpenSansRegular,
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp,
                                            color = if (isSelected) palette.accent else palette.secondaryText,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(vertical = 7.dp),
                                        )
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = null,
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = palette.accent,
                                                uncheckedColor = palette.dialogBorder,
                                                checkmarkColor = Color.White,
                                            ),
                                        )
                                    }
                                },
                                onClick = { onRoleToggle?.invoke(option) },
                                enabled = !isRoleSaving,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileProjectCard(
    projectName: String,
) {
    val palette = LocalSettingsPalette.current
    val projectTitle = localizedString("Проект", "Project")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = palette.surface,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
        ) {
            Text(
                text = projectTitle,
                color = palette.primaryText,
                fontFamily = AppFonts.OpenSansSemiBold,
                fontSize = 20.sp,
                lineHeight = 20.sp,
            )
            Spacer(modifier = Modifier.height(9.dp))
            HorizontalDivider(color = palette.border.copy(alpha = 0.75f))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = projectName,
                color = palette.secondaryText,
                fontFamily = AppFonts.OpenSansRegular,
                fontSize = 12.sp,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun LogoutActionButton(
    text: String,
    onClick: () -> Unit,
) {
    val palette = LocalSettingsPalette.current
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .pressableClickable(
                interactionSource = interactionSource,
                pressedScale = 0.94f,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.logout),
            contentDescription = null,
            modifier = Modifier.size(width = 11.dp, height = 16.dp),
            colorFilter = ColorFilter.tint(palette.secondaryText),
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = text,
            color = palette.secondaryText,
            fontFamily = AppFonts.OpenSansSemiBold,
            fontSize = 16.sp,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun SettingsSubScreen(
    title: String,
    onBackClick: () -> Unit,
    titleFontSize: androidx.compose.ui.unit.TextUnit = 40.sp,
    titleLineHeight: androidx.compose.ui.unit.TextUnit = 20.sp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalSettingsPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp)
            .padding(top = 0.dp, bottom = 24.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            val backInteractionSource = remember { MutableInteractionSource() }

            Image(
                painter = painterResource(Res.drawable.arrow_back),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    // Колонка имеет padding(horizontal = 30.dp), сдвигаем на −9dp
                    // чтобы кнопка встала на стандартные 21dp от края экрана
                    .offset(x = (-9).dp)
                    .size(26.dp)
                    .pressableClickable(
                        interactionSource = backInteractionSource,
                        onClick = onBackClick,
                    ),
                colorFilter = ColorFilter.tint(palette.secondaryText),
            )

            Text(
                text = title,
                color = palette.title,
                fontFamily = AppFonts.OpenSansBold,
                fontSize = titleFontSize,
                lineHeight = titleLineHeight,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
        content()
    }
}

@Composable
private fun SettingsTitle(text: String) {
    val palette = LocalSettingsPalette.current

    Text(
        text = text,
        color = palette.title,
        fontFamily = AppFonts.OpenSansBold,
        fontSize = 40.sp,
        lineHeight = 40.sp,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ProfileSummaryCard(
    displayName: String,
    email: String,
    projectName: String,
    avatarUrl: String?,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val palette = LocalSettingsPalette.current
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pressableClickable(
                interactionSource = interactionSource,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(12.dp),
        color = palette.surface,
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                ProfileAvatar(
                    label = displayName,
                    avatarUrl = avatarUrl,
                )

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                ) {
                    Text(
                        text = displayName,
                        color = palette.primaryText,
                        fontFamily = AppFonts.OpenSansSemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.TopStart),
                    )
                    Text(
                        text = if (isLoading) "..." else email,
                        color = palette.secondaryText,
                        fontFamily = AppFonts.OpenSansRegular,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.BottomStart),
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))
                Image(
                    painter = painterResource(Res.drawable.arrow_back),
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            scaleX = -1f
                        },
                    colorFilter = ColorFilter.tint(palette.border),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Text(
                    text = "${strings.projectLabel}: ",
                    color = palette.primaryText,
                    fontFamily = AppFonts.OpenSansBold,
                    fontSize = 13.sp,
                )
                Text(
                    text = projectName,
                    color = palette.secondaryText,
                    fontFamily = AppFonts.OpenSansRegular,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LoginChip(
    onClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonBackgroundColor = Color(0xFFD7D7D7)
    val contentColor = Color(0xFF76767C)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.992f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 780f),
        label = "settings_github_button_scale",
    )
    val shadowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.16f else 0.22f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 700f),
        label = "settings_github_button_shadow_alpha",
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) Color(0xFFCFCFD1) else buttonBackgroundColor,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 620f),
        label = "settings_github_button_background",
    )

    Box(
        modifier = Modifier
            .width(SettingsLoginChipWidth)
            .height(SettingsLoginChipHeight)
            .scale(scale)
            .shadow(
                elevation = 8.dp,
                shape = SettingsLoginChipShape,
                spotColor = Color.Black.copy(alpha = shadowAlpha),
                ambientColor = Color.Black.copy(alpha = shadowAlpha),
            )
            .background(
                color = backgroundColor,
                shape = SettingsLoginChipShape,
            )
            .pressableClickable(
                interactionSource = interactionSource,
                pressedScale = 1f,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SettingsLoginChipHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Image(
                painter = painterResource(Res.drawable.github_logo),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(contentColor),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = strings.loginWithGithub,
                color = contentColor,
                fontFamily = AppFonts.OpenSansSemiBold,
                fontSize = 12.sp,
                lineHeight = 14.sp,
            )
        }
    } 
}

@Composable
private fun SettingsMenuRow(
    iconRes: DrawableResource,
    label: String,
    onClick: () -> Unit,
    iconSize: Dp = 20.dp,
) {
    val palette = LocalSettingsPalette.current
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressableClickable(
                interactionSource = interactionSource,
                pressedScale = 0.98f,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            colorFilter = ColorFilter.tint(palette.secondaryText),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = palette.secondaryText,
            fontFamily = AppFonts.OpenSansSemiBold,
            fontSize = 16.sp,
        )
    }
}

private data class DropdownOption(
    val key: String,
    val label: String,
)

@Composable
private fun SettingsDropdownValueRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    options: List<DropdownOption>,
    selectedKey: String,
    onOptionSelected: (String) -> Unit,
    canExpand: Boolean = true,
    showChevron: Boolean = true,
    onRowClick: (() -> Unit)? = null,
) {
    val palette = LocalSettingsPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "settings_dropdown_chevron_rotation",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressableClickable(
                interactionSource = interactionSource,
                pressedScale = 0.985f,
                onClick = {
                    if (canExpand) {
                        expanded = !expanded
                    } else {
                        onRowClick?.invoke()
                    }
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = palette.secondaryText,
            fontFamily = AppFonts.OpenSansSemiBold,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
        )
        Box(
            contentAlignment = Alignment.TopEnd,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value,
                    color = palette.secondaryText,
                    fontFamily = AppFonts.OpenSansBold,
                    fontSize = 16.sp,
                )
                if (showChevron) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Image(
                        painter = painterResource(Res.drawable.stats_dropdown_chevron),
                        contentDescription = null,
                        modifier = Modifier
                            .width(8.5.dp)
                            .height(7.dp)
                            .rotate(chevronRotation),
                        colorFilter = ColorFilter.tint(palette.secondaryText),
                    )
                }
            }

            if (canExpand) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    offset = DpOffset(0.dp, 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    containerColor = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 14.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE4E4E7)),
                    modifier = Modifier
                        .widthIn(min = 180.dp)
                        .heightIn(max = 260.dp),
                ) {
                    options.forEach { option ->
                        val isSelected = option.key == selectedKey
                        DropdownMenuItem(
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    if (isSelected) palette.accent.copy(alpha = 0.08f) else Color.Transparent
                                ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            text = {
                                Text(
                                    text = option.label,
                                    fontFamily = if (isSelected) {
                                        AppFonts.OpenSansSemiBold
                                    } else {
                                        AppFonts.OpenSansRegular
                                    },
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = if (isSelected) palette.accent else palette.secondaryText,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 9.dp),
                                )
                            },
                            onClick = {
                                expanded = false
                                onOptionSelected(option.key)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String,
) {
    val palette = LocalSettingsPalette.current

    Column {
        Text(
            text = label,
            color = palette.secondaryText,
            fontFamily = AppFonts.OpenSansRegular,
            fontSize = 12.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = palette.primaryText,
            fontFamily = AppFonts.OpenSansRegular,
            fontSize = 16.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = palette.border.copy(alpha = 0.6f))
    }
}

@Composable
private fun AvatarBadge(
    label: String,
) {
    val palette = LocalSettingsPalette.current
    val initial = profileInitials(label)

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        palette.accent.copy(alpha = 0.92f),
                        palette.accentBorder.copy(alpha = 0.92f),
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = palette.buttonText,
            fontFamily = AppFonts.OpenSansBold,
            fontSize = 18.sp,
        )
    }
}

@Composable
private fun ProfileAvatar(
    label: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
) {
    val palette = LocalSettingsPalette.current

    Box(
        modifier = modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        palette.accent.copy(alpha = 0.92f),
                        palette.accentBorder.copy(alpha = 0.92f),
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = profileInitials(label),
            color = palette.buttonText,
            fontFamily = AppFonts.OpenSansBold,
            fontSize = 20.sp,
        )
        avatarUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
private fun ProjectHighlightChip(
    projectName: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalSettingsPalette.current

    Surface(
        modifier = modifier.widthIn(max = 220.dp),
        shape = RoundedCornerShape(10.dp),
        color = palette.accent,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = palette.accentBorder,
        ),
    ) {
        Text(
            text = projectName,
            color = palette.buttonText,
            fontFamily = AppFonts.OpenSansBold,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun MultiLineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalSettingsPalette.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, palette.border, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        if (value.isBlank()) {
            Text(
                text = placeholder,
                color = palette.border,
                fontFamily = AppFonts.OpenSansRegular,
                fontSize = 12.sp,
                lineHeight = 20.sp,
            )
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxSize(),
            textStyle = TextStyle(
                color = palette.primaryText,
                fontFamily = AppFonts.OpenSansRegular,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        )
    }
}

@Composable
private fun LineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    val palette = LocalSettingsPalette.current

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(
                color = palette.primaryText,
                fontFamily = AppFonts.OpenSansRegular,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (value.isBlank()) {
                        Text(
                            text = placeholder,
                            color = palette.border.copy(alpha = 0.55f),
                            fontFamily = AppFonts.OpenSansRegular,
                            fontSize = 12.sp,
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        innerTextField()
                    }
                }
            }
        )

        HorizontalDivider(color = palette.border)
    }
}

@Composable
private fun ActionButton(
    text: String,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color? = null,
) {
    val palette = LocalSettingsPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 780f),
        label = "action_button_scale",
    )

    Box(
        modifier = modifier
            // graphicsLayer первым — масштабирует всю кнопку целиком,
            // включая фон и скруглённые углы
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .then(
                if (borderColor != null) {
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(10.dp))
                } else {
                    Modifier
                }
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = palette.buttonText,
            fontFamily = AppFonts.OpenSansBold,
            fontSize = 20.sp,
        )
    }
}

@Composable
private fun EditProfileDialog(
    form: EditableProfileForm,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onFormChange: (EditableProfileForm) -> Unit,
    onSaveClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val palette = LocalSettingsPalette.current
    val scope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }
    var isDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    fun dismissAnimated() {
        if (isDismissing || isSaving) return
        isDismissing = true
        isVisible = false
        scope.launch {
            delay(180)
            onDismiss()
        }
    }

    Dialog(onDismissRequest = ::dismissAnimated) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = 220)) +
                scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(durationMillis = 240),
                ),
            exit = fadeOut(animationSpec = tween(durationMillis = 160)) +
                scaleOut(
                    targetScale = 0.92f,
                    animationSpec = tween(durationMillis = 180),
                ),
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 350.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = palette.surface,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.dialogBorder),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        val dismissInteractionSource = remember { MutableInteractionSource() }

                        Text(
                            text = strings.editLabel,
                            color = palette.secondaryText,
                            fontFamily = AppFonts.OpenSansBold,
                            fontSize = 24.sp,
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(32.dp)
                                .pressableClickable(
                                    interactionSource = dismissInteractionSource,
                                    pressedScale = 0.9f,
                                    onClick = ::dismissAnimated,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "×",
                                color = palette.accent,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Light,
                            )
                        }
                    }

                    LineField(
                        value = form.name,
                        onValueChange = { onFormChange(form.copy(name = it)) },
                        placeholder = strings.firstNameLabel,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LineField(
                        value = form.surname,
                        onValueChange = { onFormChange(form.copy(surname = it)) },
                        placeholder = strings.surnameLabel,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LineField(
                        value = form.patronymic,
                        onValueChange = { onFormChange(form.copy(patronymic = it)) },
                        placeholder = strings.patronymicLabel,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LineField(
                        value = form.email,
                        onValueChange = { onFormChange(form.copy(email = it)) },
                        placeholder = strings.emailLabel,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LineField(
                        value = form.phone,
                        onValueChange = { onFormChange(form.copy(phone = it)) },
                        placeholder = strings.phoneLabel,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CompactCheckbox(
                            checked = form.notificationsEnabled,
                            onCheckedChange = {
                                onFormChange(form.copy(notificationsEnabled = it))
                            },
                        )
                        Text(
                            text = strings.notificationsLabel,
                            color = palette.secondaryText,
                            fontFamily = AppFonts.OpenSansRegular,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ActionButton(
                        text = strings.saveLabel,
                        background = palette.disabledButton,
                        onClick = onSaveClick,
                        enabled = !isSaving,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(200.dp),
                    )

                    if (isSaving) {
                        Spacer(modifier = Modifier.height(10.dp))
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(20.dp),
                            color = palette.accent,
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val palette = LocalSettingsPalette.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (checked) palette.disabledButton else Color.Transparent)
            .border(1.dp, if (checked) palette.disabledButton else palette.border, RoundedCornerShape(4.dp))
            .pressableClickable(
                interactionSource = interactionSource,
                pressedScale = 0.94f,
                onClick = { onCheckedChange(!checked) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Text(
                text = "✓",
                color = palette.buttonText,
                fontFamily = AppFonts.OpenSansBold,
                fontSize = 14.sp,
                lineHeight = 14.sp,
            )
        }
    }
}

@Composable
private fun LogoutDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val palette = LocalSettingsPalette.current
    val scope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }
    var isDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    fun dismissAnimated(onAfterDismiss: () -> Unit) {
        if (isDismissing) return
        isDismissing = true
        isVisible = false
        scope.launch {
            delay(180)
            onAfterDismiss()
        }
    }

    Dialog(onDismissRequest = { dismissAnimated(onDismiss) }) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = 220)) +
                scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(durationMillis = 240),
                ),
            exit = fadeOut(animationSpec = tween(durationMillis = 160)) +
                scaleOut(
                    targetScale = 0.92f,
                    animationSpec = tween(durationMillis = 180),
                ),
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 248.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = palette.surface,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.dialogBorder),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = strings.logoutQuestion,
                        color = palette.secondaryText,
                        fontFamily = AppFonts.OpenSansBold,
                        fontSize = 24.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        ActionButton(
                            text = strings.noLabel,
                            background = palette.accent,
                            borderColor = palette.accentBorder,
                            onClick = { dismissAnimated(onDismiss) },
                            modifier = Modifier.width(100.dp),
                        )
                        ActionButton(
                            text = strings.yesLabel,
                            background = palette.disabledButton,
                            onClick = { dismissAnimated(onConfirm) },
                            modifier = Modifier.width(100.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThanksDialog(
    onDismiss: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val palette = LocalSettingsPalette.current
    val scope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }
    var isDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    fun dismissAnimated() {
        if (isDismissing) return
        isDismissing = true
        isVisible = false
        scope.launch {
            delay(180)
            onDismiss()
        }
    }

    Dialog(onDismissRequest = ::dismissAnimated) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = 220)) +
                scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(durationMillis = 240),
                ),
            exit = fadeOut(animationSpec = tween(durationMillis = 160)) +
                scaleOut(
                    targetScale = 0.92f,
                    animationSpec = tween(durationMillis = 180),
                ),
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 350.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = palette.surface,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.dialogBorder),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        val dismissInteractionSource = remember { MutableInteractionSource() }

                        Text(
                            text = strings.thanksTitle,
                            color = palette.secondaryText,
                            fontFamily = AppFonts.OpenSansBold,
                            fontSize = 24.sp,
                            modifier = Modifier.padding(horizontal = 36.dp),
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-6).dp)
                                .size(32.dp)
                                .pressableClickable(
                                    interactionSource = dismissInteractionSource,
                                    pressedScale = 0.9f,
                                    onClick = ::dismissAnimated,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "×",
                                color = palette.accent,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Light,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = strings.thanksMessage,
                        color = palette.secondaryText,
                        fontFamily = AppFonts.OpenSansRegular,
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsWatermarkBackground() {
    val palette = LocalSettingsPalette.current

    Image(
        painter = painterResource(Res.drawable.spbu_logo),
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .alpha(palette.spbuBackdropLogoAlpha),
        contentScale = ContentScale.Crop,
    )
}

private fun UserProfileResponse.githubAvatarUrl(): String? {
    val login = user.githubLogin
        .trim()
        .takeIf { it.isNotBlank() }
        ?: return null

    return "https://github.com/$login.png?size=160"
}

private fun Member.belongsToProfile(
    fullName: String,
    teamUsers: List<User> = emptyList(),
    currentUserId: Int? = null,
): Boolean {
    fun normalize(value: String): String {
        return value
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
    }
    if (currentUserId != null && user == currentUserId) return true
    val normalizedFull = normalize(fullName)
    // Сравниваем по member.name
    if (normalize(name) == normalizedFull) return true
    // Запасной вариант: берём имя из списка users по member.user ID (формат совпадает с fullName)
    val teamUserName = teamUsers.firstOrNull { it.id == user?.toString() }?.name ?: return false
    return normalize(teamUserName) == normalizedFull
}

private fun profileInitials(label: String): String {
    val initials = label
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.toString() }
        .joinToString("")
        .uppercase()

    return initials.ifBlank { "C" }
}

private fun Modifier.pressableClickable(
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 780f),
        label = "settings_press_scale",
    )

    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(
        enabled = enabled,
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
    )
}
