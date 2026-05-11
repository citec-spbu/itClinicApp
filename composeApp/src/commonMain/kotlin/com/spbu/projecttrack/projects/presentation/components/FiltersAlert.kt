package com.spbu.projecttrack.projects.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spbu.projecttrack.core.settings.localizedString
import com.spbu.projecttrack.core.settings.localizeRuntime
import com.spbu.projecttrack.core.theme.AppColors
import com.spbu.projecttrack.core.theme.AppFonts
import com.spbu.projecttrack.core.theme.appPalette
import com.spbu.projecttrack.core.time.PlatformTime
import com.spbu.projecttrack.projects.data.model.Tag
import com.spbu.projecttrack.projects.presentation.models.ProjectFilters
import com.spbu.projecttrack.rating.presentation.projectstats.StatsDateRangePickerDialog
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import projecttrack.composeapp.generated.resources.Res
import projecttrack.composeapp.generated.resources.calendar_icon
import projecttrack.composeapp.generated.resources.close_icon
import projecttrack.composeapp.generated.resources.spbu_logo
import projecttrack.composeapp.generated.resources.stats_dropdown_chevron
import androidx.compose.ui.text.font.FontWeight

private val FiltersAlertShape = RoundedCornerShape(20.dp)
private val FiltersFieldShape = RoundedCornerShape(10.dp)
private val FiltersActionChipShape = RoundedCornerShape(15.dp)
private val FiltersClearAllShape = RoundedCornerShape(7.dp)
private val FiltersDropdownShape = RoundedCornerShape(10.dp)
private val FiltersDropdownItemShape = RoundedCornerShape(7.dp)
private val FiltersDropdownBorder = Color(0xFFE4E4E7)
private val FiltersDropdownSelectedRow = AppColors.Color3.copy(alpha = 0.08f)

@Composable
fun FiltersAlert(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    tags: List<Tag>,
    filters: ProjectFilters,
    onFiltersChange: (ProjectFilters) -> Unit,
    modifier: Modifier = Modifier
) {
    val enrollmentTitle = localizedString("Срок записи на проект", "Enrollment period")
    val projectDurationTitle = localizedString("Срок реализации", "Project duration")
    var showTagsMenu by remember { mutableStateOf(false) }
    var showEnrollmentCalendar by remember { mutableStateOf(false) }
    var showProjectCalendar by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            delay(ProjectOverlayDialogAnimationDurationMs)
            showTagsMenu = false
        }
    }

    ProjectOverlayDialog(
        isVisible = isVisible,
        onDismiss = {
            showTagsMenu = false
            onDismiss()
        },
        modifier = modifier,
        maxWidth = 350.dp,
        shape = FiltersAlertShape,
        borderColor = AppColors.Color1,
        containerColor = AppColors.White,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        backgroundContent = {
            Image(
                painter = painterResource(Res.drawable.spbu_logo),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(appPalette().spbuBackdropLogoAlpha),
                alignment = Alignment.Center,
                contentScale = ContentScale.FillWidth
            )
        }
    ) { dismiss ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FiltersAlertHeader(onClose = dismiss)

            TagsFilterSection(
                tags = tags,
                selectedTags = filters.selectedTags,
                expanded = showTagsMenu,
                onExpandedChange = { showTagsMenu = it },
                onTagsChange = { newTags ->
                    onFiltersChange(filters.copy(selectedTags = newTags))
                }
            )

            FilterDateSection(
                title = enrollmentTitle,
                startDate = filters.enrollmentStartDate,
                endDate = filters.enrollmentEndDate,
                onClear = {
                    onFiltersChange(
                        filters.copy(
                            enrollmentStartDate = null,
                            enrollmentEndDate = null
                        )
                    )
                },
                onClick = { showEnrollmentCalendar = true }
            )

            FilterDateSection(
                title = projectDurationTitle,
                startDate = filters.projectStartDate,
                endDate = filters.projectEndDate,
                onClear = {
                    onFiltersChange(
                        filters.copy(
                            projectStartDate = null,
                            projectEndDate = null
                        )
                    )
                },
                onClick = { showProjectCalendar = true }
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ClearAllButton(onClick = {
                    showTagsMenu = false
                    onFiltersChange(filters.clear())
                })
            }
        }
    }

    if (showEnrollmentCalendar) {
        StatsDateRangePickerDialog(
            initialStartIsoDate = filters.enrollmentStartDate ?: defaultCalendarStartIso(filters.enrollmentEndDate),
            initialEndIsoDate = filters.enrollmentEndDate ?: defaultCalendarEndIso(filters.enrollmentStartDate),
            onDismiss = { showEnrollmentCalendar = false },
            onConfirm = { startIsoDate, endIsoDate ->
                showEnrollmentCalendar = false
                onFiltersChange(
                    filters.copy(
                        enrollmentStartDate = startIsoDate,
                        enrollmentEndDate = endIsoDate
                    )
                )
            }
        )
    }

    if (showProjectCalendar) {
        StatsDateRangePickerDialog(
            initialStartIsoDate = filters.projectStartDate ?: defaultCalendarStartIso(filters.projectEndDate),
            initialEndIsoDate = filters.projectEndDate ?: defaultCalendarEndIso(filters.projectStartDate),
            onDismiss = { showProjectCalendar = false },
            onConfirm = { startIsoDate, endIsoDate ->
                showProjectCalendar = false
                onFiltersChange(
                    filters.copy(
                        projectStartDate = startIsoDate,
                        projectEndDate = endIsoDate
                    )
                )
            }
        )
    }
}

@Composable
private fun FiltersAlertHeader(
    onClose: () -> Unit
) {
    val filtersTitle = localizedString("Фильтры", "Filters")
    val closeLabel = localizedString("Закрыть", "Close")

    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = filtersTitle,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = AppColors.Color2,
            modifier = Modifier.align(Alignment.Center)
        )

        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.88f else 1f,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 760f),
            label = "filters_close_scale"
        )

        Image(
            painter = painterResource(Res.drawable.close_icon),
            contentDescription = closeLabel,
            modifier = Modifier
                .size(24.dp)
                .scale(scale)
                .align(Alignment.CenterEnd)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClose
                )
        )
    }
}

@Composable
private fun TagsFilterSection(
    tags: List<Tag>,
    selectedTags: Set<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onTagsChange: (Set<String>) -> Unit
) {
    val tagsTitle = localizedString("Теги", "Tags")
    val tagNameById = remember(tags) { tags.associate { it.id.toString() to it.name } }
    val selectedNames = remember(selectedTags, tagNameById) {
        selectedTags.mapNotNull(tagNameById::get)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterSectionHeader(
            title = tagsTitle,
            showClear = selectedTags.isNotEmpty(),
            onClear = { onTagsChange(emptySet()) }
        )

        Box {
            TagSelectionField(
                selectedNames = selectedNames,
                expanded = expanded,
                onClick = { onExpandedChange(!expanded) }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                offset = DpOffset(0.dp, 6.dp),
                shape = FiltersDropdownShape,
                containerColor = Color.White,
                tonalElevation = 0.dp,
                shadowElevation = 14.dp,
                border = BorderStroke(1.dp, FiltersDropdownBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 248.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    tags.forEach { tag ->
                        val tagId = tag.id.toString()
                        val isSelected = selectedTags.contains(tagId)
                        DropdownMenuItem(
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .clip(FiltersDropdownItemShape)
                                .background(
                                    if (isSelected) {
                                        FiltersDropdownSelectedRow
                                    } else {
                                        Color.Transparent
                                    }
                                ),
                            text = {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = AppColors.Color2,
                                            uncheckedColor = AppColors.Color2,
                                            checkmarkColor = AppColors.White
                                        )
                                    )
                                    Text(
                                        text = tag.name,
                                        fontFamily = AppFonts.OpenSans,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 14.sp,
                                        color = if (isSelected) AppColors.Color3 else AppColors.Color2,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            onClick = {
                                val newTags = if (isSelected) {
                                    selectedTags - tagId
                                } else {
                                    selectedTags + tagId
                                }
                                onTagsChange(newTags)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagSelectionField(
    selectedNames: List<String>,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val selectTagsLabel = localizedString("Выберите теги", "Choose tags")
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 780f),
        label = "filters_tag_field_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .scale(scale)
            .background(
                color = AppColors.White,
                shape = FiltersFieldShape
            )
            .border(
                width = 1.dp,
                color = AppColors.Color1,
                shape = FiltersFieldShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = when {
                selectedNames.isEmpty() -> selectTagsLabel
                selectedNames.size == 1 -> selectedNames.first()
                else -> selectedNames.joinToString(", ")
            },
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = if (selectedNames.isEmpty()) AppColors.Color1 else AppColors.Color2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Image(
            painter = painterResource(Res.drawable.stats_dropdown_chevron),
            contentDescription = null,
            modifier = Modifier
                .size(16.dp)
                .rotate(if (expanded) 180f else 0f)
                .alpha(0.9f)
        )
    }
}

@Composable
private fun FilterDateSection(
    title: String,
    startDate: String?,
    endDate: String?,
    onClear: () -> Unit,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterSectionHeader(
            title = title,
            showClear = startDate != null || endDate != null,
            onClear = onClear
        )

        DateRangeField(
            startDate = startDate,
            endDate = endDate,
            onClick = onClick
        )
    }
}

@Composable
private fun FilterSectionHeader(
    title: String,
    showClear: Boolean,
    onClear: () -> Unit
) {
    val clearLabel = localizedString("Очистить", "Clear")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = AppColors.Black
        )

        if (showClear) {
            FilterActionChip(
                modifier = Modifier.align(Alignment.CenterVertically),
                text = clearLabel,
                shape = FiltersActionChipShape,
                textSize = 10.sp,
                horizontalPadding = 6.dp,
                verticalPadding = 4.dp,
                onClick = onClear
            )
        }
    }
}

@Composable
private fun DateRangeField(
    startDate: String?,
    endDate: String?,
    onClick: () -> Unit
) {
    val calendarLabel = localizedString("Календарь", "Calendar")
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 780f),
        label = "filters_date_field_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .scale(scale)
            .background(
                color = AppColors.White,
                shape = FiltersFieldShape
            )
            .border(
                width = 1.dp,
                color = AppColors.Color1,
                shape = FiltersFieldShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = formatFilterDateRangeDisplay(startDate, endDate),
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = if (startDate != null || endDate != null) AppColors.Color2 else AppColors.Color1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Image(
            painter = painterResource(Res.drawable.calendar_icon),
            contentDescription = calendarLabel,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ClearAllButton(
    onClick: () -> Unit
) {
    FilterActionChip(
        modifier = Modifier.height(30.dp),
        text = localizedString("Очистить все", "Clear all"),
        shape = FiltersClearAllShape,
        textSize = 15.sp,
        horizontalPadding = 15.dp,
        verticalPadding = 0.dp,
        onClick = onClick
    )
}

@Composable
private fun FilterActionChip(
    modifier: Modifier = Modifier,
    text: String,
    shape: RoundedCornerShape,
    textSize: androidx.compose.ui.unit.TextUnit,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    verticalPadding: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 760f),
        label = "filters_action_chip_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .dropShadow(
                shape = shape,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.07f),
                    offset = DpOffset(0.dp, 2.dp),
                    radius = 4.dp
                )
            )
            .background(
                color = Color(0xFF9F2D20),
                shape = shape
            )
            .border(
                width = 1.dp,
                color = Color(0xFFCF3F2F),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = textSize,
                lineHeight = textSize,
                letterSpacing = 0.1.sp,
                color = AppColors.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatFilterDateRangeDisplay(
    startDate: String?,
    endDate: String?
): String {
    val formattedStart = formatIsoDateForDisplay(startDate)
    val formattedEnd = formatIsoDateForDisplay(endDate)
    return when {
        formattedStart != null && formattedEnd != null -> localizeRuntime(
            "с $formattedStart по $formattedEnd",
            "from $formattedStart to $formattedEnd",
        )
        formattedStart != null -> localizeRuntime("с $formattedStart", "from $formattedStart")
        formattedEnd != null -> localizeRuntime("до $formattedEnd", "until $formattedEnd")
        else -> localizeRuntime("с 00.00.0000 по 31.12.3000", "from 00.00.0000 to 31.12.3000")
    }
}

private fun formatIsoDateForDisplay(value: String?): String? {
    val normalized = value?.take(10)?.takeIf { it.length == 10 } ?: return null
    val parts = normalized.split("-")
    if (parts.size != 3) return normalized
    return "${parts[2]}.${parts[1]}.${parts[0]}"
}

private fun defaultCalendarStartIso(otherBound: String?): String {
    return otherBound?.take(10)?.takeIf { it.length == 10 } ?: todayIsoDate()
}

private fun defaultCalendarEndIso(otherBound: String?): String {
    return otherBound?.take(10)?.takeIf { it.length == 10 } ?: todayIsoDate()
}

private fun todayIsoDate(): String {
    return Instant
        .fromEpochMilliseconds(PlatformTime.currentTimeMillis())
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()
}
