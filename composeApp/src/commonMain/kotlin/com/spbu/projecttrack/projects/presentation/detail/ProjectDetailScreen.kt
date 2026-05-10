package com.spbu.projecttrack.projects.presentation.detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.platform.LocalUriHandler
import com.spbu.projecttrack.core.settings.localizedString
import com.spbu.projecttrack.core.settings.localizeRuntime
import com.spbu.projecttrack.core.theme.AppColors
import com.spbu.projecttrack.core.theme.appPalette
import com.spbu.projecttrack.core.theme.subtleBorder
import com.spbu.projecttrack.projects.data.model.*
import com.spbu.projecttrack.projects.presentation.components.ProjectTeamCard
import com.spbu.projecttrack.projects.presentation.components.SuggestProjectButton
import com.spbu.projecttrack.projects.presentation.util.extractGithubUrl
import com.spbu.projecttrack.projects.presentation.util.normalizeUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import projecttrack.composeapp.generated.resources.*

// ==================== Font Helper ====================

@Composable
private fun openSansFamily(): FontFamily {
    return FontFamily(
        Font(Res.font.opensans_regular, weight = FontWeight.Normal),
        Font(Res.font.opensans_medium, weight = FontWeight.Medium),
        Font(Res.font.opensans_semibold, weight = FontWeight.SemiBold),
        Font(Res.font.opensans_bold, weight = FontWeight.Bold)
    )
}

// ==================== Back Arrow Icon ====================

@Composable
private fun BackArrowIcon(
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(Res.drawable.arrow_back),
        contentDescription = localizedString("Назад", "Back"),
        modifier = modifier.size(24.dp)
    )
}

// ==================== Requirement Tag Chip ====================

@Composable
private fun RequirementTagChip(
    text: String,
    modifier: Modifier = Modifier
) {
    val fontFamily = openSansFamily()
    val palette = appPalette()

    Surface(
        modifier = modifier
            .dropShadow(
                shape = RoundedCornerShape(10.dp),
                shadow = Shadow(
                    color = AppColors.CardShadow,
                    offset = DpOffset(x = 0.dp, y = 4.dp),
                    radius = 4.dp
                )
            ),
        shape = RoundedCornerShape(10.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, AppColors.CardBorder)
    ) {
        Text(
            text = text,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            color = palette.primaryText,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// ==================== Project Tag Chip ====================

@Composable
private fun ProjectTagChip(
    tag: Tag,
    modifier: Modifier = Modifier
) {
    val fontFamily = openSansFamily()
    val palette = appPalette()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(40.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, palette.subtleBorder),

    ) {
        Text(
            text = tag.name,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            color = palette.primaryText,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 0.dp)
        )
    }
}

// ==================== Info Card Component ====================

@Composable
private fun InfoCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val fontFamily = openSansFamily()
    val palette = appPalette()

    Surface(
        modifier = modifier
            .dropShadow(
                shape = RoundedCornerShape(10.dp),
                shadow = Shadow(
                    color = AppColors.CardShadow,
                    offset = DpOffset(x = 0.dp, y = 4.dp),
                    radius = 4.dp
                )
            ),
        shape = RoundedCornerShape(10.dp),
        color = palette.surface,
        border = BorderStroke(0.5.dp, AppColors.CardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                fontFamily = fontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                color = palette.primaryText
            )
            Text(
                text = value,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                color = palette.primaryText
            )
        }
    }
}

// ==================== Contact + Client Block ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContactClientBlock(
    contact: String?,
    client: String?,
    modifier: Modifier = Modifier
) {
    val contactLabel = localizedString("Контакты", "Contacts")
    val clientLabel = localizedString("Заказчик", "Client")
    val hasContact = !contact.isNullOrBlank()
    val hasClient = !client.isNullOrBlank()
    if (!hasContact && !hasClient) return

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (hasContact) {
            InfoCard(
                label = contactLabel,
                value = contact!!
            )
        }
        if (hasClient) {
            InfoCard(
                label = clientLabel,
                value = client!!
            )
        }
    }
}

// ==================== Status Card Component ====================

@Composable
private fun StatusCard(
    status: String,
    modifier: Modifier = Modifier
) {
    val fontFamily = openSansFamily()
    val palette = appPalette()

    Surface(
        modifier = modifier
            .dropShadow(
                shape = RoundedCornerShape(10.dp),
                shadow = Shadow(
                    color = AppColors.CardShadow,
                    offset = DpOffset(x = 0.dp, y = 4.dp),
                    radius = 4.dp
                )
            ),
        shape = RoundedCornerShape(10.dp),
        color = palette.surface,
        border = BorderStroke(0.5.dp, AppColors.CardBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 70.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = status,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = palette.accent,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// ==================== Header Date Block ====================

@Composable
private fun HeaderDateBlock(
    title: String,
    date: String,
    modifier: Modifier = Modifier
) {
    val fontFamily = openSansFamily()

    Surface(
        modifier = modifier
            .dropShadow(
                shape = RoundedCornerShape(10.dp),
                shadow = Shadow(
                    color = AppColors.CardShadow,
                    offset = DpOffset(x = 0.dp, y = 4.dp),
                    radius = 4.dp
                )
            ),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, AppColors.BorderColor)
    ) {
        Box(
            modifier = modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(AppColors.GradientStart, AppColors.GradientEndAlt)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    color = AppColors.White
                )
                Text(
                    text = date,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = AppColors.White
                )
            }
        }
    }
}

// ==================== Header Card with Gradient ====================

@Composable
private fun ProjectHeaderCard(
    project: ProjectDetail,
    modifier: Modifier = Modifier
) {
    val enrollmentLabel = localizedString("Срок записи", "Enrollment")
    val durationLabel = localizedString("Срок реализации", "Duration")
    val fontFamily = openSansFamily()
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .dropShadow(
                shape = RoundedCornerShape(10.dp),
                shadow = Shadow(
                    color = AppColors.CardShadow,
                    offset = DpOffset(x = 0.dp, y = 4.dp),
                    radius = 4.dp
                )
            ),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.5.dp, AppColors.BorderColor)

    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(AppColors.GradientStart, AppColors.GradientEndAlt)
                    )
                )
                .padding(13.dp)
        ) {
            Column {
                // Верхняя часть: дата + название
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp) // Увеличенное расстояние между датой и титулом
                ) {
                    // Дата слева
                    Column(
                        modifier = Modifier.width(50.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        val dateParts = formatDateForCard(project.dateStart ?: "")
                        if (dateParts.isNotEmpty()) {
                            Text(
                                text = dateParts.first(),
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                lineHeight = 12.sp,
                                color = AppColors.White
                            )
                            Text(
                                text = dateParts.drop(1).joinToString(" "),
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                color = AppColors.White
                            )
                        }
                    }
                    
                    // Название проекта (динамическая высота)
                    Text(
                        text = project.name,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        color = AppColors.White,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Нижняя часть: 2 блока с датами
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeaderDateBlock(
                        title = enrollmentLabel,
                        date = formatDateDots(project.dateStart),
                        modifier = Modifier.weight(1f)
                    )
                    HeaderDateBlock(
                        title = durationLabel,
                        date = formatDateDots(project.dateEnd),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ==================== Section Title ====================

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    val fontFamily = openSansFamily()
    val palette = appPalette()

    Text(
        text = text,
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        color = palette.primaryText,
        modifier = modifier
    )
}

// ==================== Requirement Item ====================

@Composable
private fun RequirementItem(
    number: Int,
    text: String,
    modifier: Modifier = Modifier
) {
    val fontFamily = openSansFamily()
    val palette = appPalette()

    Column(modifier = modifier.fillMaxWidth()) {
        // Разделитель сверху
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.subtleBorder)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "$number",
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = palette.primaryText,
                modifier = Modifier.width(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = text,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = palette.primaryText
            )
        }
    }
}

// ==================== Date Formatting ====================

private fun formatDateForCard(dateString: String): List<String> {
    if (dateString.isEmpty()) return emptyList()
    val parts = dateString.take(10).split("-")
    if (parts.size == 3) {
        val day = parts[2]
        val month = when(parts[1]) {
            "01" -> localizeRuntime("янв", "Jan")
            "02" -> localizeRuntime("фев", "Feb")
            "03" -> localizeRuntime("мар", "Mar")
            "04" -> localizeRuntime("апр", "Apr")
            "05" -> localizeRuntime("май", "May")
            "06" -> localizeRuntime("июн", "Jun")
            "07" -> localizeRuntime("июл", "Jul")
            "08" -> localizeRuntime("авг", "Aug")
            "09" -> localizeRuntime("сен", "Sep")
            "10" -> localizeRuntime("окт", "Oct")
            "11" -> localizeRuntime("ноя", "Nov")
            "12" -> localizeRuntime("дек", "Dec")
            else -> parts[1]
        }
        val year = parts[0]
        return listOf(day, month, year)
    }
    return emptyList()
}

private fun formatDateDots(dateString: String?): String {
    if (dateString.isNullOrBlank()) return localizeRuntime("Не указано", "Not specified")
    val s = dateString.take(10)
    val parts = s.split("-")
    return if (parts.size == 3) {
        "${parts[2]}.${parts[1]}.${parts[0]}"
    } else s
}

// ==================== Main Screen ====================

@Composable
fun ProjectDetailScreen(
    viewModel: ProjectDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    showTitle: Boolean = true,
    showBackButton: Boolean = true,
    showMyProjectActions: Boolean = false,
    onMyProjectOpenStats: () -> Unit = {},
    onTeamMemberClick: ((String, String, String?) -> Unit)? = null,
    onMemberRoleEdit: ((memberId: Int, newRole: String) -> Unit)? = null,
    showBackgroundLogo: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAuthorized by com.spbu.projecttrack.core.auth.AuthManager.isAuthorized.collectAsState(initial = false)
    val currentUserId by com.spbu.projecttrack.core.auth.AuthManager.currentUserId.collectAsState(initial = null)
    val resolvedTitle = title ?: localizedString("Проекты", "Projects")

    ProjectDetailScreenContent(
        uiState = uiState,
        isAuthorized = isAuthorized,
        currentUserId = currentUserId,
        onBackClick = onBackClick,
        onRetry = { viewModel.retry() },
        title = resolvedTitle,
        showTitle = showTitle,
        showBackButton = showBackButton,
        showMyProjectActions = showMyProjectActions,
        onMyProjectOpenStats = onMyProjectOpenStats,
        onTeamMemberClick = onTeamMemberClick,
        onMemberRoleEdit = onMemberRoleEdit ?: { memberId, newRole ->
            viewModel.updateMemberRole(memberId, newRole)
        },
        showBackgroundLogo = showBackgroundLogo,
        modifier = modifier
    )
}

@Composable
internal fun ProjectDetailScreenContent(
    uiState: ProjectDetailUiState,
    isAuthorized: Boolean,
    currentUserId: Int? = null,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    title: String? = null,
    showTitle: Boolean = true,
    showBackButton: Boolean = true,
    showMyProjectActions: Boolean = false,
    onMyProjectOpenStats: () -> Unit = {},
    onTeamMemberClick: ((String, String, String?) -> Unit)? = null,
    onMemberRoleEdit: ((memberId: Int, newRole: String) -> Unit)? = null,
    showBackgroundLogo: Boolean = true,
    modifier: Modifier = Modifier
) {
    val loadingErrorLabel = localizedString("Ошибка загрузки", "Loading error")
    val retryLabel = localizedString("Повторить", "Retry")
    val resolvedTitle = title ?: localizedString("Проекты", "Projects")
    val fontFamily = openSansFamily()
    val uriHandler = LocalUriHandler.current
    val projectDetail = (uiState as? ProjectDetailUiState.Success)?.project
    val githubUrl = remember(projectDetail) { extractGithubUrl(projectDetail) }
    val palette = appPalette()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (showBackgroundLogo) palette.background else Color.Transparent)
    ) {
        if (showBackgroundLogo) {
            Image(
                painter = painterResource(Res.drawable.spbu_logo),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .alpha(palette.spbuBackdropLogoAlpha),
                contentScale = ContentScale.FillWidth
            )
        }

        Scaffold (
            containerColor = Color.Transparent,
            contentColor = palette.primaryText
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                    )
            ) {
                // Хедер с кнопкой назад и титулом
                if (showTitle || showBackButton) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.background)
                            .padding(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        // Кнопка назад
                        if (showBackButton) {
                            val backInteractionSource = remember {
                                androidx.compose.foundation.interaction.MutableInteractionSource()
                            }
                            val backPressed by backInteractionSource.collectIsPressedAsState()
                            val backScale by animateFloatAsState(
                                targetValue = if (backPressed) 0.9f else 1f,
                                animationSpec = spring(dampingRatio = 0.76f, stiffness = 780f),
                                label = "project_detail_back_scale"
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .size(24.dp)
                                    .scale(backScale)
                                    .clickable(
                                        interactionSource = backInteractionSource,
                                        indication = null,
                                        onClick = onBackClick
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                BackArrowIcon()
                            }
                        }

                        // Титул
                        if (showTitle) {
                            Text(
                                text = resolvedTitle,
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 40.sp,
                                color = palette.accent,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }

                // Контент
                Box(modifier = Modifier.weight(1f)) {
                    when (uiState) {
                        is ProjectDetailUiState.Loading -> {
                            LoadingContent()
                        }

                        is ProjectDetailUiState.Success -> {
                            ProjectDetailContent(
                                project = uiState.project,
                                tags = uiState.tags,
                                members = uiState.members,
                                users = uiState.users,
                                statusText = uiState.statusText,
                                isAuthorized = isAuthorized,
                                currentUserId = currentUserId,
                                bottomPadding = 80.dp,
                                topPadding = if (showMyProjectActions) 48.dp else 32.dp,
                                showMyProjectActions = showMyProjectActions,
                                githubUrl = githubUrl,
                                onOpenGithub = { url -> uriHandler.openUri(normalizeUrl(url)) },
                                onOpenStats = onMyProjectOpenStats,
                                onMemberClick = onTeamMemberClick,
                                onMemberRoleEdit = onMemberRoleEdit
                            )
                        }

                        is ProjectDetailUiState.Error -> {
                            ErrorContent(
                                message = uiState.message,
                                onRetry = onRetry
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    val palette = appPalette()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = palette.accent)
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val loadingErrorLabel = localizedString("Ошибка загрузки", "Loading error")
    val retryLabel = localizedString("Повторить", "Retry")
    val fontFamily = openSansFamily()
    val palette = appPalette()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = loadingErrorLabel,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = palette.accent
            )
            Text(
                text = message,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = palette.primaryText
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent
                )
            ) {
                Text(
                    text = retryLabel,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun MyProjectMenu(
    githubUrl: String?,
    onBackToProjects: () -> Unit,
    onOpenGithub: (String) -> Unit,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allProjectsLabel = localizedString("Все проекты", "All projects")
    val githubLabel = localizedString("ГитХаб", "GitHub")
    val projectStatsLabel = localizedString("Статистика проекта", "Project stats")
    val menuBaseOffset = (-95).dp

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(y = menuBaseOffset)
                .padding(end = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SuggestProjectButton(
                onClick = onBackToProjects,
                text = allProjectsLabel
            )

            if (!githubUrl.isNullOrBlank()) {
                SuggestProjectButton(
                    onClick = { onOpenGithub(githubUrl) },
                    text = githubLabel
                )
            }

            SuggestProjectButton(
                onClick = onOpenStats,
                text = projectStatsLabel
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectDetailContent(
    project: ProjectDetail,
    tags: List<Tag>,
    members: List<Member>,
    users: List<User>,
    statusText: String,
    isAuthorized: Boolean,
    currentUserId: Int? = null,
    bottomPadding: Dp = 80.dp,
    topPadding: Dp = 32.dp,
    showMyProjectActions: Boolean = false,
    githubUrl: String? = null,
    onOpenGithub: ((String) -> Unit)? = null,
    onOpenStats: (() -> Unit)? = null,
    onMemberClick: ((String, String, String?) -> Unit)? = null,
    onMemberRoleEdit: ((memberId: Int, newRole: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val descriptionTitle = localizedString("Описание проекта", "Project description")
    val requirementsTitle = localizedString("Требования проекта", "Project requirements")
    val performerRequirementsTitle = localizedString(
        "Требования для исполнителей",
        "Requirements for contributors",
    )
    val githubLabel = localizedString("GitHub", "GitHub")
    val projectStatsLabel = localizedString("Статистика проекта", "Project stats")
    val fontFamily = openSansFamily()
    val palette = appPalette()
    val scrollState = rememberScrollState()
    val projectTags = tags.filter { tag -> project.tags?.contains(tag.id) == true }
    val hasContactClient = !project.contact.isNullOrBlank() || !project.client.isNullOrBlank()
    val showTopFade by remember { derivedStateOf { scrollState.value > 0 } }
    val showBottomFade by remember { derivedStateOf { scrollState.value < scrollState.maxValue } }
    val topFadeAlpha by animateFloatAsState(
        targetValue = if (showTopFade) 1f else 0f,
        animationSpec = tween(200),
        label = "detail_top_fade"
    )
    val bottomFadeAlpha by animateFloatAsState(
        targetValue = if (showBottomFade) 1f else 0f,
        animationSpec = tween(200),
        label = "detail_bottom_fade"
    )
    
    // Требования проекта: из поля requirements или парсим из описания
    val requirements = project.requirements ?: emptyList()
    
    // Требования для исполнителей
    val executorRequirements = project.executorRequirements ?: emptyList()
    
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 21.dp)
                .padding(top = topPadding, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Главная карточка с градиентом
            ProjectHeaderCard(project = project)
            
            // Статус команды (во всю ширину)
            if (statusText.isNotBlank()) {
                StatusCard(
                    status = statusText,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Контакты + заказчик
            ContactClientBlock(
                contact = project.contact,
                client = project.client
            )
            
            // Теги проекта
            if (projectTags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (hasContactClient) 8.dp else 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    projectTags.forEach { tag ->
                        ProjectTagChip(tag = tag)
                    }
                }
            }
            
            // Описание проекта
            if (!project.description.isNullOrBlank()) {
                SectionTitle(text = descriptionTitle)
                Text(
                    text = project.description,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = palette.primaryText
                )
            }
            
            // Требования проекта (если есть)
            if (requirements.isNotEmpty()) {
                SectionTitle(text = requirementsTitle)
                
                requirements.forEachIndexed { index, requirement ->
                    RequirementItem(
                        number = index + 1,
                        text = requirement
                    )
                }
            }
            
            // Только для авторизованных пользователей
            if (isAuthorized) {
                // Требования для исполнителей (теги)
                if (executorRequirements.isNotEmpty()) {
                    SectionTitle(text = performerRequirementsTitle)
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        executorRequirements.forEach { req ->
                            RequirementTagChip(text = req)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Команда
                if (members.isNotEmpty()) {
                    ProjectTeamCard(
                        members = members,
                        users = users,
                        preferredProjectName = project.name,
                        onMemberClick = onMemberClick,
                        currentUserId = currentUserId,
                        onMemberRoleEdit = onMemberRoleEdit
                    )
                }
            }

            // Кнопки "Мой проект": GitHub и Статистика в скролле
            if (showMyProjectActions) {
                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(palette.subtleBorder)
                )

                if (!githubUrl.isNullOrBlank() && onOpenGithub != null) {
                    MyProjectActionButton(
                        iconRes = Res.drawable.github_logo,
                        iconSize = 20.dp,
                        text = githubLabel,
                        onClick = { onOpenGithub(githubUrl) }
                    )
                }

                MyProjectActionButton(
                    iconRes = Res.drawable.stats_tab_logo,
                    iconSize = 22.dp,
                    text = projectStatsLabel,
                    onClick = { onOpenStats?.invoke() },
                    showBottomDivider = false
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Верхний градиент — плавно появляется при скролле вниз
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .align(Alignment.TopCenter)
                .alpha(topFadeAlpha)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            palette.background,
                            palette.background.copy(alpha = 0f)
                        )
                    )
                )
        )

        // Нижний градиент — плавно исчезает когда дошли до конца
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.BottomCenter)
                .alpha(bottomFadeAlpha)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            palette.background.copy(alpha = 0f),
                            palette.background
                        )
                    )
                )
        )
    }
}

@Composable
private fun MyProjectActionButton(
    iconRes: org.jetbrains.compose.resources.DrawableResource,
    iconSize: Dp,
    text: String,
    onClick: () -> Unit,
    showBottomDivider: Boolean = true
) {
    val fontFamily = openSansFamily()
    val palette = appPalette()
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "action_btn_scale"
    )

    Column(modifier = Modifier.fillMaxWidth().scale(scale)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                colorFilter = ColorFilter.tint(palette.accent)
            )
            Text(
                text = text,
                fontFamily = fontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = palette.accent
            )
        }
        if (showBottomDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(palette.subtleBorder)
            )
        }
    }
}

// ==================== Preview Functions ====================

@Suppress("UNCHECKED_CAST")
private fun createPreviewViewModel(state: ProjectDetailUiState): ProjectDetailViewModel {
    return object {
        val uiState: StateFlow<ProjectDetailUiState> = MutableStateFlow(state)
        fun retry() {}
    } as ProjectDetailViewModel
}

private fun getSampleProjectDetail() = ProjectDetail(
    id = "1",
    name = "Анализ и прогнозирование манёвра космического аппарата (КА)",
    description = "В современном мире сложно переоценить важность актуальной информации. Каждая компания стремится показать клиенту свои достижения и скрыть недостатки. Предположим компания А решила заказать себе спутник ретранслятор для обеспечения связи с удаленными буровыми платформами.",
    shortDescription = null,
    dateStart = "2025-09-08",
    dateEnd = "2025-12-26",
    slug = "cosmic-analysis",
    tags = listOf(1, 2, 3),
    status = "team_assigned",
    client = "СТЦ",
    contact = "И.С.Блеканов",
    requirements = listOf(
        "Реализовать сервис анализа деградации орбит с rest-api и модуль отображения полученной информации.",
        "Входная информация набор TLE для одного или группы КА. Выходная дата предыдущих коррекций и следующей ближайшей",
        "Сервис реализуется на языках C# или C++. Отображение на усмотрение производителя",
        "Используем набор сохранённых ранее описателей орбит. Проводим анализ их деградаций. На выходе получаем для конкретного КА интервал коррекции."
    ),
    executorRequirements = listOf("Интерес к OSINT", "С++", "С#", "Rest API")
)

private fun getSampleTags() = listOf(
    Tag(id = 1, name = "Веб-разработка"),
    Tag(id = 2, name = "C++"),
    Tag(id = 3, name = "C#")
)

private fun getSampleMembers() = listOf(
    Member(id = 1, name = "Студент Студентов Студентович", roles = listOf("Backend-разработчик"), user = 1),
    Member(id = 2, name = "Студент Студентов Студентович", roles = listOf("Frontend-разработчик")),
    Member(id = 3, name = "Студент Студентов Студентович", roles = listOf("Designer")),
    Member(id = 4, name = "Студент Студентов Студентович", roles = listOf("Project Manager"))
)
