package com.spbu.projecttrack.projects.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.spbu.projecttrack.core.theme.AppColors
import com.spbu.projecttrack.core.theme.AppFonts
import com.spbu.projecttrack.projects.data.model.Tag
import com.spbu.projecttrack.projects.presentation.models.ProjectFilters
import org.jetbrains.compose.resources.painterResource
import projecttrack.composeapp.generated.resources.*

@Composable
fun FiltersAlert(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    tags: List<Tag>,
    filters: ProjectFilters,
    onFiltersChange: (ProjectFilters) -> Unit,
    modifier: Modifier = Modifier
) {
    // Кроссфейд анимация
    androidx.compose.animation.AnimatedVisibility(
        visible = isVisible,
        enter = androidx.compose.animation.fadeIn(
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 300)
        ),
        exit = androidx.compose.animation.fadeOut(
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 300)
        )
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = onDismiss)
            ) {
            Box(
                modifier = Modifier
                    .width(350.dp)
                    .height(313.dp)
                    .align(Alignment.Center)
                    .background(
                        color = AppColors.White,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = AppColors.Color1,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable(enabled = false) { },
                contentAlignment = Alignment.TopCenter
            ) {
                // Фон логотип СПбГУ по ширине
                Image(
                    painter = painterResource(Res.drawable.spbu_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(0.1f), // Видимость 10%
                    contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    // Заголовок по центру с кнопкой закрытия
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Фильтры",
                            fontFamily = AppFonts.OpenSansBold,
                            fontSize = 24.sp,
                            color = AppColors.Color2,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        
                        Image(
                            painter = painterResource(Res.drawable.close_icon),
                            contentDescription = "Закрыть",
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.CenterEnd)
                                .clickable(onClick = onDismiss)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Блок тегов
                    TagFiltersSection(
                        tags = tags,
                        selectedTags = filters.selectedTags,
                        onTagsChange = { newTags ->
                            onFiltersChange(filters.copy(selectedTags = newTags))
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Блок дат (упрощенная версия)
                    DateFiltersSection(
                        filters = filters,
                        onFiltersChange = onFiltersChange
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Кнопка "Очистить все" без затемнения
                    Box(
                        modifier = Modifier
                            .width(125.dp)
                            .height(30.dp)
                            .background(
                                color = AppColors.Color3,
                                shape = RoundedCornerShape(7.dp)
                            )
                            .border(
                                width = 2.dp,
                                color = AppColors.BorderColor,
                                shape = RoundedCornerShape(7.dp)
                            )
                            .clickable(
                                indication = null, // Убираем затемнение
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = { onFiltersChange(ProjectFilters()) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Очистить все",
                            fontFamily = AppFonts.OpenSansSemiBold,
                            fontSize = 10.sp,
                            color = AppColors.White
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun TagFiltersSection(
    tags: List<Tag>,
    selectedTags: Set<String>,
    onTagsChange: (Set<String>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Теги",
                fontFamily = AppFonts.OpenSansSemiBold,
                fontSize = 15.sp,
                color = AppColors.Black
            )
            
            if (selectedTags.isNotEmpty()) {
                // Кнопка очистить без текста
                Box(
                    modifier = Modifier
                        .width(58.dp)
                        .height(16.dp)
                        .background(
                            color = AppColors.Color3,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 2.dp,
                            color = AppColors.BorderColor,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            onClick = { onTagsChange(emptySet<String>()) }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Выпадающее меню для тегов
        Box {
            // Кнопка открытия меню
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(
                        color = AppColors.White,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = AppColors.Color1,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = if (selectedTags.isEmpty()) {
                        "Выберите теги"
                    } else {
                        "${selectedTags.size} выбрано"
                    },
                    fontFamily = AppFonts.OpenSansRegular,
                    fontSize = 15.sp,
                    color = if (selectedTags.isEmpty()) AppColors.Color1 else AppColors.Color2
                )
            }

            // Выпадающий список с прокруткой
            if (expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp) // Максимальная высота
                        .offset(y = 45.dp)
                        .background(
                            color = AppColors.White,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = AppColors.Color1,
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tags.size) { index ->
                            val tag = tags[index]
                            val tagId = tag.id.toString()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newTags = if (selectedTags.contains(tagId)) {
                                            selectedTags - tagId
                                        } else {
                                            selectedTags + tagId
                                        }
                                        onTagsChange(newTags)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = selectedTags.contains(tagId),
                                    onCheckedChange = null, // Обрабатывается через Row clickable
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = AppColors.Color2,
                                        uncheckedColor = AppColors.Color2
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )
                                
                                Text(
                                    text = tag.name,
                                    fontFamily = AppFonts.OpenSansRegular,
                                    fontSize = 15.sp,
                                    color = AppColors.Color1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateFiltersSection(
    filters: ProjectFilters,
    onFiltersChange: (ProjectFilters) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Срок записи на проект
        DateInputField(
            label = "Срок записи на проект",
            startDate = filters.enrollmentStartDate,
            endDate = filters.enrollmentEndDate,
            onDatesChange = { start, end ->
                onFiltersChange(filters.copy(
                    enrollmentStartDate = start,
                    enrollmentEndDate = end
                ))
            }
        )
        
        // Срок реализации
        DateInputField(
            label = "Срок реализации",
            startDate = filters.projectStartDate,
            endDate = filters.projectEndDate,
            onDatesChange = { start, end ->
                onFiltersChange(filters.copy(
                    projectStartDate = start,
                    projectEndDate = end
                ))
            }
        )
    }
}

@Composable
private fun DateInputField(
    label: String,
    startDate: String?,
    endDate: String?,
    onDatesChange: (String?, String?) -> Unit
) {
    var showCalendar by remember { mutableStateOf(false) }
    
    Column {
        Text(
            text = label,
            fontFamily = AppFonts.OpenSansSemiBold,
            fontSize = 15.sp,
            color = AppColors.Black
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(30.dp)
                .background(
                    color = AppColors.White,
                    shape = RoundedCornerShape(10.dp)
                )
                .border(
                    width = 1.dp,
                    color = AppColors.Color1,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { showCalendar = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (startDate != null || endDate != null) {
                        "${startDate ?: ""} - ${endDate ?: ""}"
                    } else {
                        "00.00.0000 по 31.12.3000"
                    },
                    fontFamily = AppFonts.OpenSansMedium,
                    fontSize = 12.sp,
                    color = if (startDate != null || endDate != null) AppColors.Color2 else AppColors.Color1
                )
                
                Image(
                    painter = painterResource(Res.drawable.calendar_icon),
                    contentDescription = "Календарь",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // TODO: Добавить календарь (потребуется платформо-специфичная реализация)
    }
}
