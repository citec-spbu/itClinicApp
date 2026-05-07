package com.spbu.projecttrack.main.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.window.Dialog
import com.spbu.projecttrack.core.auth.AuthManager
import com.spbu.projecttrack.core.di.DependencyContainer
import com.spbu.projecttrack.core.email.FeedbackMailPayload
import com.spbu.projecttrack.core.email.FeedbackMailSender
import com.spbu.projecttrack.core.platform.openExternalUrl
import com.spbu.projecttrack.core.settings.AppLanguage
import com.spbu.projecttrack.core.settings.AppThemeMode
import com.spbu.projecttrack.core.settings.LocalAppStrings
import com.spbu.projecttrack.core.settings.LocalAppUiSettingsController
import com.spbu.projecttrack.core.settings.LocalSettingsPalette
import com.spbu.projecttrack.core.storage.createAppPreferences
import com.spbu.projecttrack.core.theme.AppFonts
import com.spbu.projecttrack.user.data.model.UserProfileResponse
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.backhandler.BackHandler
import projecttrack.composeapp.generated.resources.Res
import projecttrack.composeapp.generated.resources.github_logo
import projecttrack.composeapp.generated.resources.spbu_logo

private enum class SettingsDestination {
    Home,
    Settings,
    PrivacyPolicy,
    Feedback,
    Profile,
}

private enum class SelectionDialog {
    None,
    Language,
    Theme,
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
    val palette = LocalSettingsPalette.current
    val settingsController = LocalAppUiSettingsController.current
    val preferences = remember { createAppPreferences() }
    val userProfileApi = remember { DependencyContainer.provideUserProfileApi() }
    val mobileAuthApi = remember { DependencyContainer.provideMobileAuthApi() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isAuthorized by AuthManager.isAuthorized.collectAsState(initial = false)

    var destination by rememberSaveable { mutableStateOf(SettingsDestination.Home) }
    var selectionDialog by rememberSaveable { mutableStateOf(SelectionDialog.None) }
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

    fun loadProfile() {
        if (!isAuthorized) {
            profile = null
            profileError = null
            resetToHomeIfNeeded()
            return
        }

        scope.launch {
            isProfileLoading = true
            profileError = null
            val result = userProfileApi.getProfile()
            if (result.isSuccess) {
                profile = result.getOrNull()
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

    LaunchedEffect(destination) {
        onRootDestinationChange(destination == SettingsDestination.Home)
    }

    BackHandler(enabled = destination != SettingsDestination.Home) {
        destination = SettingsDestination.Home
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        SettingsWatermarkBackground()

        when (destination) {
            SettingsDestination.Home -> SettingsHomeScreen(
                isAuthorized = isAuthorized,
                isProfileLoading = isProfileLoading,
                profile = profile,
                onSettingsClick = { destination = SettingsDestination.Settings },
                onPolicyClick = { destination = SettingsDestination.PrivacyPolicy },
                onFeedbackClick = { destination = SettingsDestination.Feedback },
                onProfileClick = {
                    if (isAuthorized) {
                        destination = SettingsDestination.Profile
                    }
                },
                onLoginClick = {
                    openExternalUrl(mobileAuthApi.loginUrl)
                },
            )

            SettingsDestination.Settings -> SettingsPreferencesScreen(
                onBackClick = { destination = SettingsDestination.Home },
                onLanguageClick = { selectionDialog = SelectionDialog.Language },
                onThemeClick = { selectionDialog = SelectionDialog.Theme },
            )

            SettingsDestination.PrivacyPolicy -> SettingsPolicyScreen(
                onBackClick = { destination = SettingsDestination.Home },
            )

            SettingsDestination.Feedback -> SettingsFeedbackScreen(
                value = feedbackText,
                onValueChange = { feedbackText = it },
                isSending = isFeedbackSending,
                onBackClick = { destination = SettingsDestination.Home },
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
                onBackClick = { destination = SettingsDestination.Home },
                onEditClick = { openEditProfile() },
                onLogoutClick = { showLogoutDialog = true },
            )
        }

        if (selectionDialog != SelectionDialog.None) {
            SettingsSelectionDialog(
                title = when (selectionDialog) {
                    SelectionDialog.Language -> strings.selectLanguageTitle
                    SelectionDialog.Theme -> strings.selectThemeTitle
                    SelectionDialog.None -> ""
                },
                options = when (selectionDialog) {
                    SelectionDialog.Language -> listOf(
                        SelectionOption(
                            label = strings.languageRuLong,
                            selected = settingsController.language == AppLanguage.Russian,
                            onClick = {
                                settingsController.onLanguageChange(AppLanguage.Russian)
                                selectionDialog = SelectionDialog.None
                            },
                        ),
                        SelectionOption(
                            label = strings.languageEnLong,
                            selected = settingsController.language == AppLanguage.English,
                            onClick = {
                                settingsController.onLanguageChange(AppLanguage.English)
                                selectionDialog = SelectionDialog.None
                            },
                        ),
                    )

                    SelectionDialog.Theme -> listOf(
                        SelectionOption(
                            label = strings.themeLight,
                            selected = settingsController.themeMode == AppThemeMode.Light,
                            onClick = {
                                settingsController.onThemeModeChange(AppThemeMode.Light)
                                selectionDialog = SelectionDialog.None
                            },
                        ),
                        SelectionOption(
                            label = strings.themeDark,
                            selected = settingsController.themeMode == AppThemeMode.Dark,
                            onClick = {
                                settingsController.onThemeModeChange(AppThemeMode.Dark)
                                selectionDialog = SelectionDialog.None
                            },
                        ),
                        SelectionOption(
                            label = strings.themeSystem,
                            selected = settingsController.themeMode == AppThemeMode.System,
                            onClick = {
                                settingsController.onThemeModeChange(AppThemeMode.System)
                                selectionDialog = SelectionDialog.None
                            },
                        ),
                    )

                    SelectionDialog.None -> emptyList()
                },
                onDismiss = { selectionDialog = SelectionDialog.None },
            )
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
                            loadProfile()
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
                        destination = SettingsDestination.Home
                    }
                },
            )
        }

        if (showThanksDialog) {
            ThanksDialog(
                onDismiss = { showThanksDialog = false },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 104.dp)
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
    val palette = LocalSettingsPalette.current
    val displayName = profile?.user?.fullName?.displayName()
        ?.takeIf { it.isNotBlank() }
        ?: strings.profileCardFallbackName
    val email = profile?.user?.email?.takeIf { it.isNotBlank() } ?: strings.notSpecified
    val currentProject = profile?.projects?.firstOrNull()?.name ?: strings.noCurrentProject

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
                isLoading = isProfileLoading,
                onClick = onProfileClick,
            )
            Spacer(modifier = Modifier.height(22.dp))
        } else {
            LoginChip(onClick = onLoginClick)
            Spacer(modifier = Modifier.height(28.dp))
        }

        SettingsMenuRow(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = palette.secondaryText,
                    modifier = Modifier.size(20.dp)
                )
            },
            label = strings.settingsTitle,
            onClick = onSettingsClick,
        )
        Spacer(modifier = Modifier.height(18.dp))
        SettingsMenuRow(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = palette.secondaryText,
                    modifier = Modifier.size(20.dp)
                )
            },
            label = strings.privacyPolicyTitle.replace("\n", " "),
            onClick = onPolicyClick,
        )
        Spacer(modifier = Modifier.height(18.dp))
        SettingsMenuRow(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    tint = palette.secondaryText,
                    modifier = Modifier.size(20.dp)
                )
            },
            label = strings.feedbackTitle.replace("\n", " "),
            onClick = onFeedbackClick,
        )
    }
}

@Composable
private fun SettingsPreferencesScreen(
    onBackClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onThemeClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val settingsController = LocalAppUiSettingsController.current

    SettingsSubScreen(
        title = strings.settingsTitle,
        onBackClick = onBackClick,
    ) {
        SettingValueRow(
            icon = Icons.Outlined.Language,
            label = strings.languageLabel,
            value = when (settingsController.language) {
                AppLanguage.Russian -> strings.languageRuShort
                AppLanguage.English -> strings.languageEnShort
            },
            showArrow = true,
            onClick = onLanguageClick,
        )
        Spacer(modifier = Modifier.height(14.dp))
        SettingValueRow(
            icon = Icons.Outlined.Palette,
            label = strings.themeLabel,
            value = when (settingsController.themeMode) {
                AppThemeMode.Light -> strings.themeLight
                AppThemeMode.Dark -> strings.themeDark
                AppThemeMode.System -> strings.themeSystem
            },
            onClick = onThemeClick,
        )
    }
}

@Composable
private fun SettingsPolicyScreen(
    onBackClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val palette = LocalSettingsPalette.current

    SettingsSubScreen(
        title = strings.privacyPolicyTitle,
        onBackClick = onBackClick,
        titleFontSize = 24.sp,
        titleLineHeight = 21.sp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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

    SettingsSubScreen(
        title = strings.feedbackTitle,
        onBackClick = onBackClick,
        titleLineHeight = 35.sp,
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
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
            )

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
                    background = palette.disabledButton,
                    onClick = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        onSendClick()
                    },
                    enabled = value.trim().isNotBlank() && !isSending,
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
    val fullName = profile?.user?.fullName?.displayName()
        ?.takeIf { it.isNotBlank() }
        ?: strings.profileCardFallbackName
    val email = profile?.user?.email?.takeIf { it.isNotBlank() } ?: strings.notSpecified
    val phone = profile?.user?.phone?.takeIf { it.isNotBlank() } ?: strings.notSpecified
    val projectName = profile?.projects?.firstOrNull()?.name ?: strings.noCurrentProject

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

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = palette.surface,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AvatarBadge(label = fullName)
                ProfileInfoRow(strings.surnameLabel, fullName)
                ProfileInfoRow(strings.emailLabel, email)
                ProfileInfoRow(strings.phoneLabel, phone)
                ProfileInfoRow(strings.projectLabel, projectName)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        ActionButton(
            text = strings.editLabel,
            background = palette.disabledButton,
            onClick = onEditClick,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(14.dp))

        ActionButton(
            text = strings.logoutLabel,
            background = palette.accent,
            borderColor = palette.accentBorder,
            onClick = onLogoutClick,
            modifier = Modifier.fillMaxWidth(),
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
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
            )
            .padding(horizontal = 30.dp)
            .padding(top = 0.dp, bottom = 24.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = palette.secondaryText,
                    modifier = Modifier.size(28.dp),
                )
            }

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
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val palette = LocalSettingsPalette.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(5.dp),
        color = palette.surface,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarBadge(label = displayName)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    color = palette.primaryText,
                    fontFamily = AppFonts.OpenSansSemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (isLoading) "..." else email,
                    color = palette.secondaryText,
                    fontFamily = AppFonts.OpenSansRegular,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${strings.projectLabel}: $projectName",
                    color = palette.primaryText,
                    fontFamily = AppFonts.OpenSansBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "→",
                color = palette.border,
                fontFamily = AppFonts.OpenSansBold,
                fontSize = 28.sp,
            )
        }
    }
}

@Composable
private fun LoginChip(
    onClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val palette = LocalSettingsPalette.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.disabledButton.copy(alpha = 0.35f),
        shadowElevation = 2.dp,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(Res.drawable.github_logo),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = strings.loginWithGithub,
                color = palette.secondaryText,
                fontFamily = AppFonts.OpenSansSemiBold,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun SettingsMenuRow(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
) {
    val palette = LocalSettingsPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = palette.secondaryText,
            fontFamily = AppFonts.OpenSansSemiBold,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun SettingValueRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    showArrow: Boolean = false,
) {
    val palette = LocalSettingsPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.secondaryText,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = palette.secondaryText,
            fontFamily = AppFonts.OpenSansSemiBold,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = palette.secondaryText,
            fontFamily = AppFonts.OpenSansBold,
            fontSize = 16.sp,
        )
        if (showArrow) {
            Text(
                text = "▼",
                color = palette.secondaryText,
                fontFamily = AppFonts.OpenSansRegular,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 6.dp),
            )
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
    val initial = label.trim().firstOrNull()?.uppercase() ?: "C"

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
                ) {
                    if (value.isBlank()) {
                        Text(
                            text = placeholder,
                            color = palette.border.copy(alpha = 0.55f),
                            fontFamily = AppFonts.OpenSansRegular,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    innerTextField()
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

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .then(
                if (borderColor != null) {
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(10.dp))
                } else {
                    Modifier
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
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

private data class SelectionOption(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
)

@Composable
private fun SettingsSelectionDialog(
    title: String,
    options: List<SelectionOption>,
    onDismiss: () -> Unit,
) {
    val palette = LocalSettingsPalette.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = palette.surface,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            ) {
                Text(
                    text = title,
                    color = palette.secondaryText,
                    fontFamily = AppFonts.OpenSansBold,
                    fontSize = 24.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(14.dp))

                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                option.onClick()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = option.label,
                            color = palette.secondaryText,
                            fontFamily = AppFonts.OpenSansRegular,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Checkbox(
                            checked = option.selected,
                            onCheckedChange = {
                                option.onClick()
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = palette.disabledButton,
                                uncheckedColor = palette.border,
                                checkmarkColor = palette.buttonText,
                            )
                        )
                    }
                    if (index != options.lastIndex) {
                        HorizontalDivider(color = palette.border.copy(alpha = 0.35f))
                    }
                }
            }
        }
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

    Dialog(onDismissRequest = onDismiss) {
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
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = strings.editLabel,
                        color = palette.secondaryText,
                        fontFamily = AppFonts.OpenSansBold,
                        fontSize = 24.sp,
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd),
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
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = form.notificationsEnabled,
                        onCheckedChange = {
                            onFormChange(form.copy(notificationsEnabled = it))
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = palette.disabledButton,
                            uncheckedColor = palette.border,
                            checkmarkColor = palette.buttonText,
                        )
                    )
                    Text(
                        text = strings.notificationsLabel,
                        color = palette.secondaryText,
                        fontFamily = AppFonts.OpenSansRegular,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
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

@Composable
private fun LogoutDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val palette = LocalSettingsPalette.current

    Dialog(onDismissRequest = onDismiss) {
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
                        onClick = onDismiss,
                        modifier = Modifier.width(100.dp),
                    )
                    ActionButton(
                        text = strings.yesLabel,
                        background = palette.disabledButton,
                        onClick = onConfirm,
                        modifier = Modifier.width(100.dp),
                    )
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

    Dialog(onDismissRequest = onDismiss) {
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
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = strings.thanksTitle,
                        color = palette.secondaryText,
                        fontFamily = AppFonts.OpenSansBold,
                        fontSize = 24.sp,
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd),
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

@Composable
private fun SettingsWatermarkBackground() {
    val palette = LocalSettingsPalette.current

    Image(
        painter = painterResource(Res.drawable.spbu_logo),
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .alpha(palette.watermarkAlpha),
        contentScale = ContentScale.Crop,
    )
}
