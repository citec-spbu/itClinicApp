package com.spbu.projecttrack.projects.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spbu.projecttrack.core.settings.localizedString
import com.spbu.projecttrack.core.settings.localizeRuntime
import com.spbu.projecttrack.core.theme.AppColors
import com.spbu.projecttrack.core.theme.AppFonts
import com.spbu.projecttrack.projects.data.model.Member
import com.spbu.projecttrack.projects.data.model.User

private val TeamRoleOptions = listOf(
    "Backend-разработчик",
    "Frontend-разработчик",
    "Fullstack-разработчик",
    "Designer",
    "Project Manager",
    "Аналитик",
    "Тестировщик",
)

private data class TeamMemberNavigationTarget(
    val userId: String,
    val userName: String,
    val preferredProjectName: String?,
)

private val TeamMembersSpacing = 16.dp

private fun localizedRoleLabel(role: String): String {
    val normalized = role.trim().lowercase()
    return when (normalized) {
        "backend-разработчик", "backend developer" ->
            localizeRuntime("Backend-разработчик", "Backend Developer")
        "frontend-разработчик", "frontend developer" ->
            localizeRuntime("Frontend-разработчик", "Frontend Developer")
        "fullstack-разработчик", "fullstack developer" ->
            localizeRuntime("Fullstack-разработчик", "Fullstack Developer")
        "аналитик", "analyst" ->
            localizeRuntime("Аналитик", "Analyst")
        "тестировщик", "qa" ->
            localizeRuntime("Тестировщик", "QA")
        "project manager" ->
            localizeRuntime("Project Manager", "Project Manager")
        "designer" ->
            localizeRuntime("Designer", "Designer")
        else -> role
    }
}

@Composable
fun ProjectTeamCard(
    members: List<Member>,
    users: List<User>,
    preferredProjectName: String?,
    onMemberClick: ((String, String, String?) -> Unit)?,
    currentUserId: Int? = null,
    onMemberRoleEdit: ((memberId: Int, newRole: String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val fontFamily = AppFonts.OpenSans
    val teamLabel = localizedString("Команда", "Team")
    val editRoleDescription = localizedString("Изменить роль", "Edit role")

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
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.BorderColor)
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
                Text(
                    text = teamLabel,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = AppColors.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 5.dp),
                    textAlign = TextAlign.Center
                )

                members.forEachIndexed { index, member ->
                    ProjectTeamMemberRow(
                        index = index + 1,
                        member = member,
                        users = users,
                        preferredProjectName = preferredProjectName,
                        currentUserId = currentUserId,
                        onClick = onMemberClick,
                        editRoleDescription = editRoleDescription,
                        onRoleEdit = if (onMemberRoleEdit != null && member.user == currentUserId) {
                            { newRole -> onMemberRoleEdit(member.id, newRole) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectTeamMemberRow(
    index: Int,
    member: Member,
    users: List<User>,
    preferredProjectName: String?,
    currentUserId: Int? = null,
    onClick: ((String, String, String?) -> Unit)? = null,
    editRoleDescription: String,
    onRoleEdit: ((newRole: String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val fontFamily = AppFonts.OpenSans
    val usersById = remember(users) { users.associateBy(User::id) }

    val nameParts = member.name.split(" - ", " | ", "\n")
    val baseName = nameParts.firstOrNull() ?: member.name
    val isCurrentUser = currentUserId != null && member.user == currentUserId
    val currentUserSuffix = localizeRuntime("(Вы)", "(You)")
    val name = if (isCurrentUser && !baseName.contains(currentUserSuffix)) {
        "$baseName $currentUserSuffix"
    } else {
        baseName
    }
    var displayRole by remember(member.role) {
        mutableStateOf((member.role ?: nameParts.getOrNull(1).orEmpty()).trim())
    }
    val navigationTarget = remember(member, usersById, preferredProjectName) {
        resolveTeamMemberNavigationTarget(
            member = member,
            usersById = usersById,
            fallbackName = baseName,
            preferredProjectName = preferredProjectName
        )
    }
    val isClickable = onClick != null && navigationTarget != null
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isClickable && isPressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = 760f),
        label = "project_team_member_scale"
    )
    val rowShape = RoundedCornerShape(12.dp)
    var showRoleDropdown by remember { mutableStateOf(false) }

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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$index. $name",
                fontFamily = fontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                color = AppColors.White
            )
            Text(
                text = localizedRoleLabel(displayRole).ifBlank { "—" },
                fontFamily = fontFamily,
                fontWeight = FontWeight.Light,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = AppColors.White,
                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
            )
        }

        if (onRoleEdit != null) {
            Box {
                IconButton(
                    onClick = { showRoleDropdown = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = editRoleDescription,
                        tint = AppColors.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(
                    expanded = showRoleDropdown,
                    onDismissRequest = { showRoleDropdown = false },
                    offset = DpOffset(x = (-80).dp, y = 4.dp),
                ) {
                    TeamRoleOptions.forEach { roleOption ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = localizedRoleLabel(roleOption),
                                    fontFamily = fontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = if (roleOption == displayRole) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (roleOption == displayRole) AppColors.Color3 else AppColors.Color2,
                                )
                            },
                            onClick = {
                                displayRole = roleOption
                                showRoleDropdown = false
                                onRoleEdit(roleOption)
                            }
                        )
                    }
                }
            }
        } else {
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
