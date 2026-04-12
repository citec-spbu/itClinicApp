package com.spbu.projecttrack.projects.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.onFocusChanged
import com.spbu.projecttrack.core.theme.AppColors
import com.spbu.projecttrack.core.theme.AppFonts
import org.jetbrains.compose.resources.painterResource
import projecttrack.composeapp.generated.resources.*

@Composable
fun SearchBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onFilterClick: () -> Unit = {},
    hasActiveFilters: Boolean = false,
    modifier: Modifier = Modifier,
    onFocusChange: (Boolean) -> Unit = {},
    showFilters: Boolean = true
) {
    val focusManager = LocalFocusManager.current
    
    Box(
        modifier = modifier
            .width(375.dp)
            .height(40.dp)
            .background(
                color = AppColors.White,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = AppColors.Color1,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Лого поиска
            Image(
                painter = painterResource(Res.drawable.search_icon),
                contentDescription = "Поиск",
                modifier = Modifier.size(24.dp)
            )
            
            // Поле ввода - одна строка
            BasicTextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focusState ->
                        onFocusChange(focusState.isFocused)
                    },
                singleLine = true, // Одна строка
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = AppFonts.OpenSansRegular,
                    fontWeight = if (searchText.isEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 16.sp,
                    color = if (searchText.isEmpty()) AppColors.Color1 else AppColors.Color2
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                    }
                ),
                decorationBox = { innerTextField ->
                    if (searchText.isEmpty()) {
                        Text(
                            text = "Поиск",
                            fontFamily = AppFonts.OpenSansSemiBold,
                            fontSize = 16.sp,
                            color = AppColors.Color1
                        )
                    }
                    innerTextField()
                }
            )
            
            if (showFilters) {
                // Лого фильтров с индикатором (цвет 2)
                Box(modifier = Modifier.size(24.dp)) {
                    Image(
                        painter = painterResource(Res.drawable.filter_icon),
                        contentDescription = "Фильтры",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(onClick = onFilterClick),
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(AppColors.Color2)
                    )

                    // Индикатор активных фильтров
                    if (hasActiveFilters) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    color = AppColors.Color3,
                                    shape = CircleShape
                                )
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                        )
                    }
                }
            }
        }
    }
}
