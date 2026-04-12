package com.spbu.projecttrack.rating.presentation.projectstats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.spbu.projecttrack.core.theme.AppColors
import com.spbu.projecttrack.core.theme.AppFonts
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import projecttrack.composeapp.generated.resources.Res
import projecttrack.composeapp.generated.resources.stats_back
import projecttrack.composeapp.generated.resources.stats_dropdown_chevron

private val CalendarDialogShape = RoundedCornerShape(16.dp)
private val CalendarRangeFill = AppColors.Color3.copy(alpha = 0.18f)
private val CalendarOnBackground = Color(0xFF1D1B20)
private val CalendarMutedText = CalendarOnBackground.copy(alpha = 0.38f)

private data class StatsCalendarDayUi(
    val date: LocalDate,
    val inCurrentMonth: Boolean,
)

@Composable
internal fun StatsDateRangePickerDialog(
    initialStartIsoDate: String,
    initialEndIsoDate: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    val initialStart = remember(initialStartIsoDate) {
        initialStartIsoDate.toLocalDateOrNull() ?: LocalDate(2025, 1, 1)
    }
    val initialEnd = remember(initialEndIsoDate) {
        initialEndIsoDate.toLocalDateOrNull() ?: initialStart
    }
    var selectedStart by remember(initialStartIsoDate) { mutableStateOf(initialStart) }
    var selectedEnd by remember(initialEndIsoDate) { mutableStateOf(initialEnd) }
    var displayedMonth by remember(initialStartIsoDate) {
        mutableStateOf(LocalDate(initialStart.year, initialStart.monthNumber, 1))
    }
    var monthExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }

    val years = remember(initialStart, initialEnd) {
        val minYear = minOf(initialStart.year, initialEnd.year) - 3
        val maxYear = maxOf(initialStart.year, initialEnd.year) + 5
        (minYear..maxYear).toList()
    }
    val weeks = remember(displayedMonth) { buildCalendarWeeks(displayedMonth) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp)
                    .widthIn(max = 360.dp),
                shape = CalendarDialogShape,
                color = Color.White,
                shadowElevation = 6.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CalendarSelectionControl(
                            label = monthLabel(displayedMonth.monthNumber),
                            options = monthOptions(),
                            expanded = monthExpanded,
                            onExpandedChange = { monthExpanded = it },
                            onSelect = { month ->
                                displayedMonth = LocalDate(displayedMonth.year, month.toInt(), 1)
                            },
                            onPrevious = {
                                displayedMonth = displayedMonth.previousMonth()
                            },
                            onNext = {
                                displayedMonth = displayedMonth.nextMonth()
                            },
                            modifier = Modifier.width(160.dp),
                        )
                        CalendarSelectionControl(
                            label = displayedMonth.year.toString(),
                            options = years.map { it.toString() to it.toString() },
                            expanded = yearExpanded,
                            onExpandedChange = { yearExpanded = it },
                            onSelect = { year ->
                                displayedMonth = LocalDate(year.toInt(), displayedMonth.monthNumber, 1)
                            },
                            onPrevious = {
                                displayedMonth = LocalDate(displayedMonth.year - 1, displayedMonth.monthNumber, 1)
                            },
                            onNext = {
                                displayedMonth = LocalDate(displayedMonth.year + 1, displayedMonth.monthNumber, 1)
                            },
                            modifier = Modifier.width(167.dp),
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 4.dp),
                    ) {
                        CalendarWeekHeader()
                        weeks.forEach { week ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                week.forEach { day ->
                                    CalendarDayCell(
                                        day = day,
                                        selectedStart = selectedStart,
                                        selectedEnd = selectedEnd,
                                        onClick = { clickedDate ->
                                            when {
                                                selectedStart != selectedEnd -> {
                                                    selectedStart = clickedDate
                                                    selectedEnd = clickedDate
                                                }

                                                clickedDate < selectedStart -> selectedStart = clickedDate
                                                else -> selectedEnd = clickedDate
                                            }
                                        },
                                        modifier = Modifier.size(48.dp),
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Cancel",
                            fontFamily = AppFonts.OpenSansMedium,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            letterSpacing = 0.1.sp,
                            color = Color.Black,
                            modifier = Modifier
                                .clickable(onClick = onDismiss)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                        Text(
                            text = "OK",
                            fontFamily = AppFonts.OpenSansMedium,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            letterSpacing = 0.1.sp,
                            color = Color.Black,
                            modifier = Modifier
                                .clickable {
                                    val start = minOf(selectedStart, selectedEnd)
                                    val end = maxOf(selectedStart, selectedEnd)
                                    onConfirm(start.toIsoDateString(), end.toIsoDateString())
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarSelectionControl(
    label: String,
    options: List<Pair<String, String>>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var anchorSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val menuWidth = with(density) { anchorSize.width.toDp() }.coerceAtLeast(96.dp)
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 700f),
        label = "calendar_dropdown_chevron_rotation",
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        CalendarNavigationButton(
            rotation = 0f,
            onClick = onPrevious,
        )
        Box(
            modifier = Modifier
                .onSizeChanged { anchorSize = it }
                .clickable { onExpandedChange(!expanded) }
                .padding(start = 8.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = label,
                    fontFamily = AppFonts.OpenSansMedium,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = CalendarOnBackground,
                )
                Image(
                    painter = painterResource(Res.drawable.stats_dropdown_chevron),
                    contentDescription = null,
                    modifier = Modifier
                        .width(8.5.dp)
                        .height(7.dp)
                        .rotate(chevronRotation),
                    colorFilter = ColorFilter.tint(CalendarOnBackground),
                )
            }

            StatsDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                options = options,
                onSelected = onSelect,
                width = menuWidth,
                maxHeight = 240.dp,
                offset = DpOffset(0.dp, 6.dp),
                selectedLabel = label,
                itemFontFamily = AppFonts.OpenSansMedium,
                selectedItemFontFamily = AppFonts.OpenSansSemiBold,
            )
        }
        CalendarNavigationButton(
            rotation = 180f,
            onClick = onNext,
        )
    }
}

@Composable
private fun CalendarNavigationButton(
    rotation: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.stats_back),
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .rotate(rotation),
            colorFilter = ColorFilter.tint(CalendarOnBackground),
        )
    }
}

@Composable
private fun CalendarWeekHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        listOf("Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб").forEach { label ->
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontFamily = AppFonts.OpenSansRegular,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    letterSpacing = 0.5.sp,
                    color = CalendarOnBackground,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: StatsCalendarDayUi,
    selectedStart: LocalDate,
    selectedEnd: LocalDate,
    onClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rangeStart = minOf(selectedStart, selectedEnd)
    val rangeEnd = maxOf(selectedStart, selectedEnd)
    val isStart = day.date == rangeStart
    val isEnd = day.date == rangeEnd
    val isSingle = rangeStart == rangeEnd && isStart
    val isBetween = day.date > rangeStart && day.date < rangeEnd
    val inRange = isSingle || isStart || isEnd || isBetween

    Box(
        modifier = modifier.clickable { onClick(day.date) },
        contentAlignment = Alignment.Center,
    ) {
        if (inRange && !isSingle) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(
                        start = if (isStart) 20.dp else 0.dp,
                        end = if (isEnd) 20.dp else 0.dp,
                    )
                    .background(
                        color = CalendarRangeFill,
                        shape = when {
                            isStart -> RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                            isEnd -> RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
                            else -> RoundedCornerShape(0.dp)
                        },
                    ),
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (isStart || isEnd) AppColors.Color3 else Color.Transparent,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                fontFamily = AppFonts.OpenSansRegular,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp,
                color = when {
                    isStart || isEnd -> Color.White
                    day.inCurrentMonth -> CalendarOnBackground
                    else -> CalendarMutedText
                },
            )
        }
    }
}

private fun buildCalendarWeeks(month: LocalDate): List<List<StatsCalendarDayUi>> {
    val firstOfMonth = LocalDate(month.year, month.monthNumber, 1)
    val gridStart = firstOfMonth.minusDays(firstOfMonth.dayOfWeek.toSundayFirstOffset())
    return List(6) { weekIndex ->
        List(7) { dayIndex ->
            val date = gridStart.plusDays(weekIndex * 7 + dayIndex)
            StatsCalendarDayUi(
                date = date,
                inCurrentMonth = date.monthNumber == month.monthNumber && date.year == month.year,
            )
        }
    }
}

private fun monthOptions(): List<Pair<String, String>> {
    return (1..12).map { month -> month.toString() to monthLabel(month) }
}

private fun monthLabel(month: Int): String = when (month) {
    1 -> "Янв"
    2 -> "Фев"
    3 -> "Мар"
    4 -> "Апр"
    5 -> "Май"
    6 -> "Июн"
    7 -> "Июл"
    8 -> "Авг"
    9 -> "Сен"
    10 -> "Окт"
    11 -> "Ноя"
    12 -> "Дек"
    else -> month.toString()
}

private fun String.toLocalDateOrNull(): LocalDate? {
    val parts = split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    return LocalDate(year, month, day)
}

private fun LocalDate.toIsoDateString(): String {
    val month = monthNumber.toString().padStart(2, '0')
    val day = dayOfMonth.toString().padStart(2, '0')
    return "$year-$month-$day"
}

private fun DayOfWeek.toSundayFirstOffset(): Int = when (this) {
    DayOfWeek.SUNDAY -> 0
    DayOfWeek.MONDAY -> 1
    DayOfWeek.TUESDAY -> 2
    DayOfWeek.WEDNESDAY -> 3
    DayOfWeek.THURSDAY -> 4
    DayOfWeek.FRIDAY -> 5
    DayOfWeek.SATURDAY -> 6
}

private fun LocalDate.previousMonth(): LocalDate {
    return if (monthNumber == 1) {
        LocalDate(year - 1, 12, 1)
    } else {
        LocalDate(year, monthNumber - 1, 1)
    }
}

private fun LocalDate.nextMonth(): LocalDate {
    return if (monthNumber == 12) {
        LocalDate(year + 1, 1, 1)
    } else {
        LocalDate(year, monthNumber + 1, 1)
    }
}

private fun LocalDate.plusDays(days: Int): LocalDate {
    if (days == 0) return this
    var result = this
    if (days > 0) {
        repeat(days) {
            result = result.nextDay()
        }
    } else {
        repeat(-days) {
            result = result.previousDay()
        }
    }
    return result
}

private fun LocalDate.minusDays(days: Int): LocalDate = plusDays(-days)

private fun LocalDate.nextDay(): LocalDate {
    val daysInMonth = daysInMonth(year, monthNumber)
    return when {
        dayOfMonth < daysInMonth -> LocalDate(year, monthNumber, dayOfMonth + 1)
        monthNumber < 12 -> LocalDate(year, monthNumber + 1, 1)
        else -> LocalDate(year + 1, 1, 1)
    }
}

private fun LocalDate.previousDay(): LocalDate {
    return when {
        dayOfMonth > 1 -> LocalDate(year, monthNumber, dayOfMonth - 1)
        monthNumber > 1 -> {
            val previousMonth = monthNumber - 1
            LocalDate(year, previousMonth, daysInMonth(year, previousMonth))
        }

        else -> LocalDate(year - 1, 12, 31)
    }
}

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (isLeapYear(year)) 29 else 28
    else -> 30
}

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
}
