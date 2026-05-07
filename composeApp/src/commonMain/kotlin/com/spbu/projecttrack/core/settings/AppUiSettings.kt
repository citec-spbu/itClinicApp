package com.spbu.projecttrack.core.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppLanguage(val storageValue: String) {
    Russian("ru"),
    English("en");

    companion object {
        fun fromStorage(value: String?): AppLanguage {
            return entries.firstOrNull { it.storageValue == value } ?: Russian
        }
    }
}

enum class AppThemeMode(val storageValue: String) {
    Light("light"),
    Dark("dark"),
    System("system");

    companion object {
        fun fromStorage(value: String?): AppThemeMode {
            return entries.firstOrNull { it.storageValue == value } ?: System
        }
    }
}

@Stable
data class AppUiSettingsController(
    val language: AppLanguage,
    val themeMode: AppThemeMode,
    val onLanguageChange: (AppLanguage) -> Unit,
    val onThemeModeChange: (AppThemeMode) -> Unit,
)

@Immutable
data class AppStrings(
    val loginWithGithub: String,
    val continueWithoutAuth: String,
    val infoTitle: String,
    val profileTitle: String,
    val settingsTitle: String,
    val privacyPolicyTitle: String,
    val feedbackTitle: String,
    val projectLabel: String,
    val noCurrentProject: String,
    val notSpecified: String,
    val languageLabel: String,
    val themeLabel: String,
    val languageRuShort: String,
    val languageEnShort: String,
    val languageRuLong: String,
    val languageEnLong: String,
    val themeLight: String,
    val themeDark: String,
    val themeSystem: String,
    val helpUsTitle: String,
    val feedbackDescription: String,
    val feedbackPlaceholder: String,
    val sendLabel: String,
    val editLabel: String,
    val saveLabel: String,
    val logoutLabel: String,
    val yesLabel: String,
    val noLabel: String,
    val firstNameLabel: String,
    val surnameLabel: String,
    val patronymicLabel: String,
    val emailLabel: String,
    val phoneLabel: String,
    val notificationsLabel: String,
    val thanksTitle: String,
    val thanksMessage: String,
    val logoutQuestion: String,
    val selectLanguageTitle: String,
    val selectThemeTitle: String,
    val profileCardFallbackName: String,
    val loginSuccessMessage: String,
    val loginErrorMessage: String,
    val feedbackSendError: String,
    val profileSaveError: String,
    val profileLoadError: String,
    val updateAvailableTitle: String,
    val updateAvailableMessage: String,
    val currentVersionLabel: String,
    val availableVersionLabel: String,
    val installUpdateLabel: String,
    val remindMeLaterLabel: String,
    val policyBody: String,
)

@Immutable
data class SettingsPalette(
    val background: Color,
    val surface: Color,
    val title: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val border: Color,
    val dialogBorder: Color,
    val accent: Color,
    val accentBorder: Color,
    val disabledButton: Color,
    val buttonText: Color,
    val watermarkAlpha: Float,
)

private val LightPalette = SettingsPalette(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    title = Color(0xFF9F2D20),
    primaryText = Color(0xFF000000),
    secondaryText = Color(0xFF76767C),
    border = Color(0xFF76767C),
    dialogBorder = Color(0xFF76767C),
    accent = Color(0xFF9F2D20),
    accentBorder = Color(0xFFCF3F2F),
    disabledButton = Color(0xFF76767C),
    buttonText = Color(0xFFFFFFFF),
    watermarkAlpha = 0.06f,
)

private val DarkPalette = SettingsPalette(
    background = Color(0xFF131314),
    surface = Color(0xFF1A1B1E),
    title = Color(0xFFE36E60),
    primaryText = Color(0xFFF5F3F1),
    secondaryText = Color(0xFFC8C3BF),
    border = Color(0xFF8F8B88),
    dialogBorder = Color(0xFF8F8B88),
    accent = Color(0xFFB84234),
    accentBorder = Color(0xFFCF6A5B),
    disabledButton = Color(0xFF6E6C72),
    buttonText = Color(0xFFFFFFFF),
    watermarkAlpha = 0.045f,
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF9F2D20),
    onPrimary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE36E60),
    onPrimary = Color.White,
    background = Color(0xFF131314),
    onBackground = Color(0xFFF5F3F1),
    surface = Color(0xFF1A1B1E),
    onSurface = Color(0xFFF5F3F1),
)

val LocalAppStrings = staticCompositionLocalOf { russianStrings() }
val LocalSettingsPalette = staticCompositionLocalOf { LightPalette }
val LocalAppUiSettingsController = staticCompositionLocalOf<AppUiSettingsController> {
    error("AppUiSettingsController is not provided")
}

fun appStrings(language: AppLanguage): AppStrings {
    return when (language) {
        AppLanguage.Russian -> russianStrings()
        AppLanguage.English -> englishStrings()
    }
}

@Composable
fun ITClinicTheme(
    language: AppLanguage,
    themeMode: AppThemeMode,
    settingsController: AppUiSettingsController,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        AppThemeMode.Light -> false
        AppThemeMode.Dark -> true
        AppThemeMode.System -> isSystemInDarkTheme()
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalAppStrings provides appStrings(language),
        LocalSettingsPalette provides if (isDark) DarkPalette else LightPalette,
        LocalAppUiSettingsController provides settingsController,
    ) {
        MaterialTheme(
            colorScheme = if (isDark) DarkColorScheme else LightColorScheme,
            content = content,
        )
    }
}

private fun russianStrings() = AppStrings(
    loginWithGithub = "Login With GitHub",
    continueWithoutAuth = "Продолжить без авторизации",
    infoTitle = "Информация",
    profileTitle = "Профиль",
    settingsTitle = "Настройки",
    privacyPolicyTitle = "Политика\nконфиденциальности",
    feedbackTitle = "Обратная\nсвязь",
    projectLabel = "Проект",
    noCurrentProject = "Нет текущего проекта",
    notSpecified = "Не указано",
    languageLabel = "Язык",
    themeLabel = "Тема",
    languageRuShort = "Ру",
    languageEnShort = "En",
    languageRuLong = "Русский",
    languageEnLong = "English",
    themeLight = "Светлая",
    themeDark = "Тёмная",
    themeSystem = "Как на устройстве",
    helpUsTitle = "Помогите нам стать лучше",
    feedbackDescription = "Напишите, если что-то работает не так, чего не хватает или что было бы удобно добавить.\nМы читаем каждое сообщение.",
    feedbackPlaceholder = "Например: «не открывается экран проекта», «хочу видеть дедлайны на главной»…",
    sendLabel = "Отправить",
    editLabel = "Редактировать",
    saveLabel = "Сохранить",
    logoutLabel = "Выйти",
    yesLabel = "Да",
    noLabel = "Нет",
    firstNameLabel = "Имя",
    surnameLabel = "Фамилия",
    patronymicLabel = "Отчество",
    emailLabel = "Почта",
    phoneLabel = "Номер телефона",
    notificationsLabel = "Получать оповещения об изменении статуса проектов, в которых Вы участвуете",
    thanksTitle = "Спасибо!",
    thanksMessage = "Спасибо, что поделились своим мнением. Такие отзывы помогают нам делать приложение удобнее. Мы обязательно посмотрим ваше сообщение и учтём его в дальнейших улучшениях.",
    logoutQuestion = "Выйти?",
    selectLanguageTitle = "Выберите язык",
    selectThemeTitle = "Выберите тему",
    profileCardFallbackName = "Пользователь",
    loginSuccessMessage = "Авторизация выполнена",
    loginErrorMessage = "Не удалось завершить авторизацию",
    feedbackSendError = "Не удалось отправить сообщение",
    profileSaveError = "Не удалось сохранить изменения",
    profileLoadError = "Не удалось загрузить профиль",
    updateAvailableTitle = "Доступно обновление",
    updateAvailableMessage = "Для Android опубликована более новая сборка из main. Установите обновление, чтобы получить последние изменения.",
    currentVersionLabel = "Текущая версия",
    availableVersionLabel = "Доступная версия",
    installUpdateLabel = "Обновить",
    remindMeLaterLabel = "Позже",
    policyBody = """
        Приложение CITEC обрабатывает только те данные, которые нужны для авторизации, отображения профиля, участия в проектах и расчёта проектной статистики.

        Когда вы входите в приложение, мы можем хранить данные вашей учётной записи, включая имя, университетскую почту, телефон, сведения о командах, проектах и действиях внутри интеграций, необходимых для расчёта метрик.

        Эти данные используются для отображения вашего профиля, навигации по проектам, построения рейтингов и аналитики, а также для обратной связи и технической поддержки.

        Мы не публикуем ваши персональные данные в открытом доступе и не передаём их третьим лицам вне случаев, когда это требуется для работы сервисов университета, инфраструктуры проекта или по закону.

        Сообщения, отправленные через форму обратной связи, могут содержать ваш текст, имя и электронную почту, если они доступны в профиле. Они используются только для обработки обращения и улучшения продукта.

        Данные хранятся столько, сколько это необходимо для работы приложения, сопровождения проектов и соблюдения внутренних требований университета и команды разработки.

        Вы можете изменить личные данные в профиле. Если вы хотите уточнить, какие данные хранятся, или попросить удалить неактуальную информацию, свяжитесь с командой CITEC через раздел обратной связи.
    """.trimIndent(),
)

private fun englishStrings() = AppStrings(
    loginWithGithub = "Login With GitHub",
    continueWithoutAuth = "Continue without authorization",
    infoTitle = "Information",
    profileTitle = "Profile",
    settingsTitle = "Settings",
    privacyPolicyTitle = "Privacy\npolicy",
    feedbackTitle = "Feedback",
    projectLabel = "Project",
    noCurrentProject = "No current project",
    notSpecified = "Not specified",
    languageLabel = "Language",
    themeLabel = "Theme",
    languageRuShort = "Ru",
    languageEnShort = "En",
    languageRuLong = "Russian",
    languageEnLong = "English",
    themeLight = "Light",
    themeDark = "Dark",
    themeSystem = "Match device",
    helpUsTitle = "Help us improve",
    feedbackDescription = "Tell us if something is broken, missing, or simply inconvenient. We read every message.",
    feedbackPlaceholder = "For example: \"the project screen does not open\", \"I want to see deadlines on the home screen\"…",
    sendLabel = "Send",
    editLabel = "Edit",
    saveLabel = "Save",
    logoutLabel = "Log out",
    yesLabel = "Yes",
    noLabel = "No",
    firstNameLabel = "First name",
    surnameLabel = "Last name",
    patronymicLabel = "Middle name",
    emailLabel = "Email",
    phoneLabel = "Phone number",
    notificationsLabel = "Receive notifications when the status of projects you participate in changes",
    thanksTitle = "Thank you!",
    thanksMessage = "Thank you for sharing your feedback. Messages like this help us make the app better. We will review your note and take it into account in future improvements.",
    logoutQuestion = "Log out?",
    selectLanguageTitle = "Choose language",
    selectThemeTitle = "Choose theme",
    profileCardFallbackName = "User",
    loginSuccessMessage = "Signed in successfully",
    loginErrorMessage = "Failed to complete sign-in",
    feedbackSendError = "Failed to send feedback",
    profileSaveError = "Failed to save changes",
    profileLoadError = "Failed to load profile",
    updateAvailableTitle = "Update available",
    updateAvailableMessage = "A newer Android build from the main branch is available. Install the update to get the latest changes.",
    currentVersionLabel = "Current version",
    availableVersionLabel = "Available version",
    installUpdateLabel = "Update now",
    remindMeLaterLabel = "Later",
    policyBody = """
        The CITEC app processes only the data required for sign-in, profile rendering, project participation and project statistics.

        When you use the app, we may store account-related data such as your name, university email, phone number, team and project links, and activity data required for analytics and metrics.

        This information is used to show your profile, navigate through projects, calculate rankings and statistics, and process support and feedback requests.

        We do not publish your personal data openly and do not share it with unrelated third parties, except when it is required for university services, project infrastructure, or by law.

        Messages sent through the feedback form may include your text, name and email when those values are available in your profile. They are used only to review your request and improve the product.

        Data is retained only for as long as it is needed to operate the app, support project workflows and comply with internal university or development team requirements.

        You can update your profile data in the app. If you want to уточнить stored data or request removal of outdated information, contact the CITEC team through the feedback section.
    """.trimIndent().replace("уточнить", "clarify your stored data"),
)
