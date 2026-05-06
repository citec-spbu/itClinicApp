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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
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
import com.spbu.projecttrack.core.theme.AppColors
import com.spbu.projecttrack.projects.data.model.*
import com.spbu.projecttrack.projects.presentation.components.SuggestProjectButton
import com.spbu.projecttrack.projects.presentation.util.extractGithubUrl
import com.spbu.projecttrack.projects.presentation.util.normalizeUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import projecttrack.composeapp.generated.resources.*

private data class TeamMemberNavigationTarget(
    val userId: String,
    val userName: String,
    val preferredProjectName: String?,
)

private val TeamMembersSpacing = 16.dp

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
        contentDescription = "Назад",
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
        color = AppColors.White,
        border = BorderStroke(1.dp, AppColors.CardBorder)
    ) {
        Text(
            text = text,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            color = AppColors.Black,
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

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(40.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, AppColors.Color1),

    ) {
        Text(
            text = tag.name,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            color = AppColors.Color2,
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
        color = AppColors.White,
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
                color = AppColors.Color2
            )
            Text(
                text = value,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                color = AppColors.Black
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
                label = "Контакты",
                value = contact!!
            )
        }
        if (hasClient) {
            InfoCard(
                label = "Заказчик",
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
        color = AppColors.White,
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
                color = AppColors.Color3,
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
                        title = "Срок записи",
                        date = formatDateDots(project.dateStart),
                        modifier = Modifier.weight(1f)
                    )
                    HeaderDateBlock(
                        title = "Срок реализации",
                        date = formatDateDots(project.dateEnd),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ==================== Team Card ====================

@Composable
private fun TeamCard(
    members: List<Member>,
    users: List<User>,
    preferredProjectName: String?,
    onMemberClick: ((String, String, String?) -> Unit)?,
    currentUserId: Int? = null,
    modifier: Modifier = Modifier
) {
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
        border = BorderStroke(1.dp, AppColors.BorderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(AppColors.TeamGradientStart, AppColors.TeamGradientEnd)
                    )
                )
                .padding(top = 5.dp, start = 16.dp, end = 16.dp, bottom = TeamMembersSpacing)

        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(TeamMembersSpacing)
            ) {
                // Заголовок
                Text(
                    text = "Команда",
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = AppColors.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 5.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                members.forEachIndexed { index, member ->
                    TeamMemberRow(
                        index = index + 1,
                        member = member,
                        users = users,
                        preferredProjectName = preferredProjectName,
                        currentUserId = currentUserId,
                        onClick = onMemberClick
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamMemberRow(
    index: Int,
    member: Member,
    users: List<User>,
    preferredProjectName: String?,
    currentUserId: Int? = null,
    onClick: ((String, String, String?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val fontFamily = openSansFamily()
    val usersById = remember(users) { users.associateBy(User::id) }

    // Разбиваем имя и роль (может приходить одной строкой с разделителем)
    val nameParts = member.name.split(" - ", " | ", "\n")
    val baseName = nameParts.firstOrNull() ?: member.name
    val isCurrentUser = currentUserId != null && member.user == currentUserId
    val name = if (isCurrentUser && !baseName.contains("(Вы)")) "$baseName (Вы)" else baseName
    val role = (member.role ?: nameParts.getOrNull(1).orEmpty()).trim()
    val navigationTarget = remember(member, usersById, preferredProjectName) {
        resolveTeamMemberNavigationTarget(
            member = member,
            usersById = usersById,
            fallbackName = baseName,
            preferredProjectName = preferredProjectName
        )
    }
    val isClickable = onClick != null && navigationTarget != null
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isClickable && isPressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = 760f),
        label = "team_member_scale"
    )
    val rowShape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(rowShape)
            .background(
                color = if (isClickable) Color.White.copy(alpha = 0.08f) else Color.Transparent,
                shape = rowShape
            )
            .then(
                if (isClickable) {
                    Modifier.border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.16f),
                        shape = rowShape
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (isClickable) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            navigationTarget?.let { target ->
                                onClick?.invoke(target.userId, target.userName, target.preferredProjectName)
                            }
                        }
                    )
                } else {
                    Modifier
                }
            )
            .padding(
                horizontal = if (isClickable) 10.dp else 0.dp,
                vertical = if (isClickable) 8.dp else 4.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Номер и данные участника
        Column(modifier = Modifier.weight(1f)) {
            // Номер + имя
            Text(
                text = "$index. $name",
                fontFamily = fontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                color = AppColors.White
            )
            // Роль с отступом
            if (role.isNotBlank()) {
                Text(
                    text = role,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Light,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = AppColors.White,
                    modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.AccountCircle,
            contentDescription = null,
            tint = AppColors.White,
            modifier = Modifier
                .size(22.dp)
                .alpha(if (isClickable) 1f else 0.82f)
        )

    }
}

private fun resolveTeamMemberNavigationTarget(
    member: Member,
    usersById: Map<String, User>,
    fallbackName: String,
    preferredProjectName: String?,
): TeamMemberNavigationTarget? {
    val userName = member.user
        ?.toString()
        ?.let(usersById::get)
        ?.name
        ?.trim()
        .takeUnless { it.isNullOrBlank() }
        ?: fallbackName.trim().takeIf { it.isNotBlank() }
        ?: return null

    val userId = member.user
        ?.toString()
        ?.takeIf { it.isNotBlank() }
        ?: userName

    return TeamMemberNavigationTarget(
        userId = userId,
        userName = userName,
        preferredProjectName = preferredProjectName?.takeIf { it.isNotBlank() }
    )
}

// ==================== Section Title ====================

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    val fontFamily = openSansFamily()
    
    Text(
        text = text,
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        color = AppColors.Black,
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
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Разделитель сверху
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppColors.Color1)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "$number",
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = AppColors.Color2,
                modifier = Modifier.width(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = text,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = AppColors.Color2
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
            "01" -> "янв"
            "02" -> "фев"
            "03" -> "мар"
            "04" -> "апр"
            "05" -> "май"
            "06" -> "июн"
            "07" -> "июл"
            "08" -> "авг"
            "09" -> "сен"
            "10" -> "окт"
            "11" -> "ноя"
            "12" -> "дек"
            else -> parts[1]
        }
        val year = parts[0]
        return listOf(day, month, year)
    }
    return emptyList()
}

private fun formatDateDots(dateString: String?): String {
    if (dateString.isNullOrBlank()) return "Не указано"
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
    title: String = "Проекты",
    showTitle: Boolean = true,
    showBackButton: Boolean = true,
    showMyProjectActions: Boolean = false,
    onMyProjectOpenStats: () -> Unit = {},
    onTeamMemberClick: ((String, String, String?) -> Unit)? = null,
    showBackgroundLogo: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAuthorized by com.spbu.projecttrack.core.auth.AuthManager.isAuthorized.collectAsState(initial = false)
    val currentUserId by com.spbu.projecttrack.core.auth.AuthManager.currentUserId.collectAsState(initial = null)

    ProjectDetailScreenContent(
        uiState = uiState,
        isAuthorized = isAuthorized,
        currentUserId = currentUserId,
        onBackClick = onBackClick,
        onRetry = { viewModel.retry() },
        title = title,
        showTitle = showTitle,
        showBackButton = showBackButton,
        showMyProjectActions = showMyProjectActions,
        onMyProjectOpenStats = onMyProjectOpenStats,
        onTeamMemberClick = onTeamMemberClick,
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
    title: String = "Проекты",
    showTitle: Boolean = true,
    showBackButton: Boolean = true,
    showMyProjectActions: Boolean = false,
    onMyProjectOpenStats: () -> Unit = {},
    onTeamMemberClick: ((String, String, String?) -> Unit)? = null,
    showBackgroundLogo: Boolean = true,
    modifier: Modifier = Modifier
) {
    val fontFamily = openSansFamily()
    val uriHandler = LocalUriHandler.current
    val projectDetail = (uiState as? ProjectDetailUiState.Success)?.project
    val githubUrl = remember(projectDetail) { extractGithubUrl(projectDetail) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (showBackgroundLogo) AppColors.White else Color.Transparent)
    ) {
        if (showBackgroundLogo) {
            Image(
                painter = painterResource(Res.drawable.spbu_logo),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                contentScale = ContentScale.FillWidth
            )
        }

        Scaffold (
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
                // Хедер с кнопкой назад и титулом
                if (showTitle || showBackButton) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
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
                                text = title,
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 40.sp,
                                color = AppColors.Color3,
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
                                onMemberClick = onTeamMemberClick
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
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = AppColors.Color3)
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fontFamily = openSansFamily()
    
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
                text = "Ошибка загрузки",
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = AppColors.Color3
            )
            Text(
                text = message,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = AppColors.Color2
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Color3
                )
            ) {
                Text(
                    text = "Повторить",
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
                text = "Все проекты"
            )

            if (!githubUrl.isNullOrBlank()) {
                SuggestProjectButton(
                    onClick = { onOpenGithub(githubUrl) },
                    text = "ГитХаб"
                )
            }

            SuggestProjectButton(
                onClick = onOpenStats,
                text = "Статистика проекта"
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
    modifier: Modifier = Modifier
) {
    val fontFamily = openSansFamily()
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
                SectionTitle(text = "Описание проекта")
                
                Text(
                    text = project.description,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = AppColors.Color2
                )
            }
            
            // Требования проекта (если есть)
            if (requirements.isNotEmpty()) {
                SectionTitle(text = "Требования проекта")
                
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
                    SectionTitle(text = "Требования для исполнителей")
                    
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
                    TeamCard(
                        members = members,
                        users = users,
                        preferredProjectName = project.name,
                        onMemberClick = onMemberClick,
                        currentUserId = currentUserId
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
                        .background(AppColors.Color1)
                )

                if (!githubUrl.isNullOrBlank() && onOpenGithub != null) {
                    MyProjectActionButton(
                        iconRes = Res.drawable.github_logo,
                        iconSize = 20.dp,
                        text = "GitHub",
                        onClick = { onOpenGithub(githubUrl) }
                    )
                }

                MyProjectActionButton(
                    iconRes = Res.drawable.stats_tab_logo,
                    iconSize = 22.dp,
                    text = "Статистика проекта",
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
                            Color.White,
                            Color.White.copy(alpha = 0f)
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
                            Color.White.copy(alpha = 0f),
                            Color.White
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
                colorFilter = ColorFilter.tint(AppColors.Color3)
            )
            Text(
                text = text,
                fontFamily = fontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = AppColors.Color3
            )
        }
        if (showBottomDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppColors.Color1)
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
    Member(id = 1, name = "Студент Студентов Студентович", role = "Backend-разработчик", user = 1),
    Member(id = 2, name = "Студент Студентов Студентович", role = "Frontend-разработчик"),
    Member(id = 3, name = "Студент Студентов Студентович", role = "Designer"),
    Member(id = 4, name = "Студент Студентов Студентович", role = "Project Manager")
)
