package com.spbu.projecttrack.rating.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.spbu.projecttrack.core.settings.localizedString
import com.spbu.projecttrack.core.settings.localizeRuntime
import com.spbu.projecttrack.core.theme.AppFonts
import com.spbu.projecttrack.core.theme.appPalette
import com.spbu.projecttrack.rating.presentation.projectstats.StatsTopBar
import com.spbu.projecttrack.rating.presentation.projectstats.StatsTopBarTotalHeight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import projecttrack.composeapp.generated.resources.Res
import projecttrack.composeapp.generated.resources.arrow_back
import projecttrack.composeapp.generated.resources.settings_plus
import projecttrack.composeapp.generated.resources.settings_remove
import projecttrack.composeapp.generated.resources.settings_trash
import projecttrack.composeapp.generated.resources.spbu_logo
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontWeight

enum class StatsScreenSettingsTarget {
    Project,
    User;

    fun descriptionSuffix(): String = when (this) {
        Project -> localizeRuntime("проектной статистики", "project statistics")
        User -> localizeRuntime("личной статистики", "personal statistics")
    }
}

enum class StatsScreenSection(
    val id: String,
) {
    Commits("commits"),
    Issues("issues"),
    PullRequests("pull_requests"),
    RapidPullRequests("rapid_pull_requests"),
    CodeChurn("code_churn"),
    CodeOwnership("code_ownership"),
    DominantWeekDay("dominant_week_day");

    fun title(): String = when (this) {
        Commits -> localizeRuntime("Коммиты", "Commits")
        Issues -> "Issue"
        PullRequests -> "Pull Request"
        RapidPullRequests -> localizeRuntime("Быстрые Pull Request", "Rapid Pull Requests")
        CodeChurn -> localizeRuntime("Изменчивость кода", "Code churn")
        CodeOwnership -> localizeRuntime("Владение кодом", "Code ownership")
        DominantWeekDay -> localizeRuntime("Доминирующий день недели", "Dominant weekday")
    }

    companion object {
        fun fromId(id: String): StatsScreenSection? = entries.firstOrNull { it.id == id }
    }
}

fun defaultStatsScreenSectionIds(): List<String> {
    return StatsScreenSection.entries.map(StatsScreenSection::id)
}

fun statsScreenSectionsFromIds(ids: List<String>): List<StatsScreenSection> {
    return ids.mapNotNull(StatsScreenSection::fromId).distinct()
}

@Composable
fun StatsScreenSettingsScreen(
    target: StatsScreenSettingsTarget,
    activeSectionIds: List<String>,
    onActiveSectionIdsChange: (List<String>) -> Unit,
    onBackClick: () -> Unit,
    analyticsProjectId: String? = null,
    modifier: Modifier = Modifier,
) {
    val settingsTitle = localizedString("Настройки", "Settings")
    val removeBlockLabel = localizedString("Убрать блок", "Remove section")
    val unusedLabel = localizedString("Неиспользуемые", "Unused")
    val addBlockLabel = localizedString("Добавить блок", "Add section")
    val dragBlockLabel = localizedString("Перетащить блок", "Drag section")
    val descriptionText = localizedString(
        "Выберите вкладки, которые хотите видеть на экране ${target.descriptionSuffix()} и их порядок",
        "Choose the sections you want to see on the ${target.descriptionSuffix()} screen and their order",
    )
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val initialSectionIds = remember { activeSectionIds }
    var localActiveSectionIds by remember { mutableStateOf(activeSectionIds) }
    var draggingSectionId by remember { mutableStateOf<String?>(null) }
    var draggingOffsetPx by remember { mutableStateOf(0f) }
    var removingSectionId by remember { mutableStateOf<String?>(null) }
    var appearingSectionId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeSectionIds, draggingSectionId, removingSectionId) {
        if (draggingSectionId == null && removingSectionId == null) {
            localActiveSectionIds = activeSectionIds
        }
    }

    val activeSections = remember(localActiveSectionIds) {
        statsScreenSectionsFromIds(localActiveSectionIds)
    }
    val activeIdsSet = remember(localActiveSectionIds) { localActiveSectionIds.toSet() }
    val inactiveSections = remember(activeIdsSet) {
        StatsScreenSection.entries.filterNot { it.id in activeIdsSet }
    }
    val reorderStepPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        (SettingsCardHeight + SettingsCardSpacing).toPx()
    }
    val reorderTriggerPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        (SettingsCardSpacing + (SettingsCardHeight / 2)).toPx()
    }

    fun commitActiveSections(updatedIds: List<String>) {
        localActiveSectionIds = updatedIds
        onActiveSectionIdsChange(updatedIds)
    }

    fun addSection(sectionId: String) {
        if (sectionId in localActiveSectionIds) return
        commitActiveSections(localActiveSectionIds + sectionId)
        appearingSectionId = sectionId
        scope.launch {
            delay(SettingsAddAnimationDurationMs)
            if (appearingSectionId == sectionId) {
                appearingSectionId = null
            }
        }
    }

    fun removeSection(sectionId: String) {
        if (removingSectionId != null) return
        removingSectionId = sectionId
        if (draggingSectionId == sectionId) {
            draggingSectionId = null
            draggingOffsetPx = 0f
        }
        scope.launch {
            delay(SettingsRemoveAnimationDurationMs)
            commitActiveSections(localActiveSectionIds.filterNot { it == sectionId })
            removingSectionId = null
        }
    }

    fun reorderDraggedSection(sectionId: String, deltaY: Float) {
        if (sectionId !in localActiveSectionIds) return
        draggingOffsetPx += deltaY
        var currentIndex = localActiveSectionIds.indexOf(sectionId)
        while (draggingOffsetPx <= -reorderTriggerPx && currentIndex > 0) {
            val updated = localActiveSectionIds.moveSection(currentIndex, currentIndex - 1)
            commitActiveSections(updated)
            draggingOffsetPx += reorderStepPx
            currentIndex -= 1
        }
        while (draggingOffsetPx >= reorderTriggerPx && currentIndex < localActiveSectionIds.lastIndex) {
            val updated = localActiveSectionIds.moveSection(currentIndex, currentIndex + 1)
            commitActiveSections(updated)
            draggingOffsetPx -= reorderStepPx
            currentIndex += 1
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        TrackStatsScreenSettingsClose(
            target = target,
            initialSectionIds = initialSectionIds,
            currentSectionIds = activeSectionIds,
            analyticsProjectId = analyticsProjectId,
        )

        Image(
            painter = painterResource(Res.drawable.spbu_logo),
            contentDescription = null,
            modifier = Modifier
                .width(1800.dp)
                .align(Alignment.Center)
                .offset(y = 12.dp)
                .alpha(appPalette().spbuBackdropLogoAlpha),
            contentScale = ContentScale.FillWidth,
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                )
                .padding(horizontal = 21.dp),
            contentPadding = PaddingValues(
                top = StatsTopBarTotalHeight + 8.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item("description") {
                Text(
                    text = descriptionText,
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    color = Color.Black,
                    modifier = Modifier.widthIn(max = 370.dp),
                )
            }

            item("description_spacer") {
                Spacer(modifier = Modifier.height(14.dp))
            }

            itemsIndexed(
                items = activeSections,
                key = { _, section -> "active_${section.id}" },
            ) { index, section ->
                val isDragging = draggingSectionId == section.id

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(
                            fadeInSpec = null,
                            fadeOutSpec = null,
                            placementSpec = if (isDragging) {
                                null
                            } else {
                                spring(stiffness = Spring.StiffnessVeryLow)
                            },
                        )
                        .zIndex(if (isDragging) SettingsDraggingZIndex else 0f),
                ) {
                    AnimatedVisibility(
                        visible = removingSectionId != section.id,
                        enter = fadeIn(animationSpec = tween(SettingsAddAnimationDurationMs.toInt())) +
                            expandVertically(animationSpec = tween(SettingsAddAnimationDurationMs.toInt())),
                        exit = fadeOut(animationSpec = tween(SettingsRemoveAnimationDurationMs.toInt())) +
                            shrinkVertically(animationSpec = tween(SettingsRemoveAnimationDurationMs.toInt())),
                    ) {
                        val isAppearing = appearingSectionId == section.id
                        val (alpha, scale) = animatedSettingsCardAppearance(
                            isAppearing = isAppearing,
                            isDragging = isDragging,
                        )

                        SettingsSectionCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    translationY = if (isDragging) draggingOffsetPx else 0f
                                    this.alpha = alpha
                                    scaleX = scale
                                    scaleY = scale
                                },
                            isDragging = isDragging,
                            title = section.title(),
                            titleColor = SettingsSecondaryText,
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    SettingsDragHandle(
                                        tint = SettingsSecondaryText,
                                        onDragStart = {
                                            draggingSectionId = section.id
                                            draggingOffsetPx = 0f
                                        },
                                        onDrag = { deltaY ->
                                            reorderDraggedSection(section.id, deltaY)
                                        },
                                        onDragEnd = {
                                            draggingSectionId = null
                                            draggingOffsetPx = 0f
                                        },
                                    )
                                    SettingsActionIcon(
                                        drawable = Res.drawable.settings_trash,
                                        contentDescription = removeBlockLabel,
                                        tint = SettingsSecondaryText,
                                        iconWidth = 16.dp,
                                        iconHeight = 19.dp,
                                        onClick = { removeSection(section.id) },
                                    )
                                }
                            },
                        )
                    }
                }

                if (index < activeSections.lastIndex) {
                    Spacer(modifier = Modifier.height(SettingsCardSpacing))
                }
            }

            if (inactiveSections.isNotEmpty()) {
                item("inactive_section_spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item("inactive_section_block") {
                    Column(
                        modifier = Modifier.animateItem(
                            placementSpec = spring(stiffness = Spring.StiffnessVeryLow),
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = unusedLabel,
                            fontFamily = AppFonts.OpenSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = Color.Black,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(SettingsCardSpacing)) {
                            inactiveSections.forEach { section ->
                                val (alpha, scale) = animatedSettingsCardAppearance(
                                    isAppearing = false,
                                    isDragging = false,
                                )
                                SettingsSectionCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            this.alpha = alpha
                                            scaleX = scale
                                            scaleY = scale
                                        },
                                    title = section.title(),
                                    titleColor = SettingsDisabledText,
                                    trailingContent = {
                                        SettingsActionIcon(
                                            drawable = Res.drawable.settings_plus,
                                            contentDescription = addBlockLabel,
                                            tint = SettingsSecondaryText,
                                            iconWidth = 18.dp,
                                            iconHeight = 18.dp,
                                            onClick = { addSection(section.id) },
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        StatsTopBar(
            title = settingsTitle,
            onBackClick = onBackClick,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun animatedSettingsCardAppearance(
    isAppearing: Boolean,
    isDragging: Boolean,
): Pair<Float, Float> {
    var appeared by remember(isAppearing) { mutableStateOf(!isAppearing) }

    LaunchedEffect(isAppearing) {
        if (isAppearing) {
            appeared = true
        }
    }

    val appearProgress by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(SettingsAddAnimationDurationMs.toInt()),
        label = "settings_card_appear",
    )

    val alpha = if (isDragging) 0.98f else appearProgress
    val scale = when {
        isDragging -> 1.06f
        else -> 0.96f + (0.04f * appearProgress)
    }
    return alpha to scale
}

@Composable
private fun SettingsSectionCard(
    title: String,
    titleColor: Color,
    trailingContent: @Composable RowScope.() -> Unit,
    isDragging: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(SettingsCardHeight)
            .shadow(
                elevation = if (isDragging) SettingsDraggingCardElevation else SettingsCardElevation,
                shape = SettingsCardShape,
                clip = false,
            )
            .background(Color.White, SettingsCardShape)
            .border(1.dp, SettingsCardStroke, SettingsCardShape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 20.sp,
            color = titleColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            content = trailingContent,
        )
    }
}

@Composable
private fun SettingsActionIcon(
    drawable: org.jetbrains.compose.resources.DrawableResource,
    contentDescription: String,
    tint: Color,
    iconWidth: androidx.compose.ui.unit.Dp,
    iconHeight: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsPressableBox(
        modifier = modifier.size(24.dp),
        onClick = onClick,
    ) {
        Image(
            painter = painterResource(drawable),
            contentDescription = contentDescription,
            modifier = Modifier.size(width = iconWidth, height = iconHeight),
            colorFilter = ColorFilter.tint(tint),
        )
    }
}

@Composable
private fun SettingsPressableBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) SettingsButtonPressedScale else 1f,
        animationSpec = tween(
            durationMillis = if (isPressed) SettingsButtonPressInDurationMs else SettingsButtonPressOutDurationMs,
        ),
        label = "settings_button_press_scale",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun SettingsDragHandle(
    tint: Color,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dragBlockLabel = localizedString("Перетащить блок", "Drag section")
    Box(
        modifier = modifier
            .size(width = 18.dp, height = 24.dp)
            .pointerInput(onDragStart, onDrag, onDragEnd) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                ) { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.y)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.settings_remove),
            contentDescription = dragBlockLabel,
            modifier = Modifier.size(width = 8.dp, height = 15.dp),
            colorFilter = ColorFilter.tint(tint),
        )
    }
}

private fun List<String>.moveSection(
    fromIndex: Int,
    toIndex: Int,
): List<String> {
    if (fromIndex !in indices || toIndex !in indices || fromIndex == toIndex) {
        return this
    }

    val mutable = toMutableList()
    val item = mutable.removeAt(fromIndex)
    mutable.add(toIndex, item)
    return mutable
}

private val SettingsCardShape = RoundedCornerShape(5.dp)
private val SettingsAccent = Color(0xFF9F2D20)
private val SettingsSecondaryText = Color(0xFF76767C)
private val SettingsDisabledText = Color(0xFFBDBDBD)
private val SettingsCardStroke = Color(0x0AF4F0ED)
private val SettingsCardHeight = 60.dp
private val SettingsCardSpacing = 12.dp
private val SettingsCardElevation = 8.dp
private val SettingsDraggingCardElevation = 18.dp
private const val SettingsDraggingZIndex = 10f
private const val SettingsButtonPressedScale = 1.2f
private const val SettingsButtonPressInDurationMs = 90
private const val SettingsButtonPressOutDurationMs = 140
private const val SettingsAddAnimationDurationMs = 260L
private const val SettingsRemoveAnimationDurationMs = 220L
