package com.spbu.projecttrack.projects.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.alpha
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import projecttrack.composeapp.generated.resources.*
import com.spbu.projecttrack.projects.data.model.Project
import com.spbu.projecttrack.projects.data.model.Tag
import com.spbu.projecttrack.projects.presentation.components.SearchBar
import com.spbu.projecttrack.projects.presentation.components.FiltersAlert
import com.spbu.projecttrack.projects.presentation.models.ProjectFilters
import com.spbu.projecttrack.core.theme.AppColors
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput


@Composable
private fun openSansFamily(): FontFamily {
    // OpenSans шрифты с разными весами
    return FontFamily(
        Font(Res.font.opensans_regular, weight = FontWeight.Normal),
        Font(Res.font.opensans_medium, weight = FontWeight.Medium),
        Font(Res.font.opensans_semibold, weight = FontWeight.SemiBold),
        Font(Res.font.opensans_bold, weight = FontWeight.Bold)
    )
}

/**
 * Fuzzy search с использованием биграмм (пары букв)
 * Возвращает коэффициент совпадения от 0.0 до 1.0
 */
private fun fuzzyMatchScore(text: String, query: String): Double {
    if (query.isBlank()) return 1.0
    if (text.isBlank()) return 0.0
    
    val normalizedText = text.lowercase()
    val normalizedQuery = query.lowercase()
    
    // Точное совпадение - максимальный приоритет
    if (normalizedText.contains(normalizedQuery)) {
        return 1.0
    }
    
    // Биграммы (пары букв) для запроса
    val queryBigrams = mutableSetOf<String>()
    for (i in 0 until normalizedQuery.length - 1) {
        queryBigrams.add(normalizedQuery.substring(i, i + 2))
    }
    
    if (queryBigrams.isEmpty()) {
        // Если запрос из 1 символа
        return if (normalizedText.contains(normalizedQuery[0])) 0.5 else 0.0
    }
    
    // Биграммы для текста
    val textBigrams = mutableSetOf<String>()
    for (i in 0 until normalizedText.length - 1) {
        textBigrams.add(normalizedText.substring(i, i + 2))
    }
    
    // Количество совпадающих биграмм
    val matches = queryBigrams.intersect(textBigrams).size
    
    // Коэффициент совпадения
    return matches.toDouble() / queryBigrams.size
}

/**
 * Поиск проектов по названию с fuzzy matching
 */
private fun searchProjects(projects: List<Project>, query: String, threshold: Double = 0.3): List<Project> {
    if (query.isBlank()) return projects
    
    return projects
        .map { project -> 
            Pair(project, fuzzyMatchScore(project.name, query))
        }
        .filter { it.second >= threshold }
        .sortedByDescending { it.second }
        .map { it.first }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: ProjectsViewModel,
    onProjectClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchText by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var filters by remember { mutableStateOf(ProjectFilters()) }
    val hasActiveFilters = filters.hasActiveFilters()
    val isAuthorized by com.spbu.projecttrack.core.auth.AuthManager.isAuthorized.collectAsState(initial = false)
    val focusManager = LocalFocusManager.current
    
    // Флаг: открыта ли клавиатура (есть ли фокус на поле ввода)
    var isSearchFocused by remember { mutableStateOf(false) }
    
    // Обработчик клика по проекту: если клава открыта - только закрываем, если закрыта - открываем проект
    val handleProjectClick: (String) -> Unit = { projectId ->
        if (isSearchFocused) {
            // Клавиатура открыта - только закрываем
            focusManager.clearFocus()
            isSearchFocused = false
        } else {
            // Клавиатура закрыта - открываем проект
            onProjectClick(projectId)
        }
    }

    val fontFamily = openSansFamily()
    val titleColor = AppColors.Color3

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Невидимый фоновый слой для закрытия клавиатуры - самый нижний слой
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    onClick = { 
                        focusManager.clearFocus()
                        isSearchFocused = false
                    },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
        )
        
        // Лого СПбГУ на весь экран по ширине
        Image(
            painter = painterResource(Res.drawable.spbu_logo),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .alpha(1.0f), 
            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White) // Белый фон навбара
                    .padding(top = 0.dp, bottom = 0.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Проекты",
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 40.sp,
                        color = titleColor
                    )

                }
            }

            // Поиск
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                SearchBar(
                    searchText = searchText,
                    onSearchTextChange = { searchText = it },
                    onFilterClick = { showFilters = true },
                    hasActiveFilters = hasActiveFilters,
                    onFocusChange = { focused -> isSearchFocused = focused },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Контент проектов
            Box(modifier = Modifier.weight(1f)) {
                when (val state = uiState) {
                    is ProjectsUiState.Loading -> {
                        LoadingContent()
                    }
                    is ProjectsUiState.Success -> {
                        // Фильтруем проекты по названию с fuzzy search
                        val filteredProjects = searchProjects(state.projects, searchText)

                        ProjectsContent(
                            projects = filteredProjects,
                            tags = state.tags,
                            isLoadingMore = state.isLoadingMore,
                            onProjectClick = handleProjectClick,
                            onLoadMore = { viewModel.loadMoreProjects() }
                        )
                    }
                    is ProjectsUiState.Error -> {
                        ErrorContent(
                            message = state.message,
                            onRetry = { viewModel.retry() }
                        )
                    }
                }
                
                // Верхний градиент (fade effect) - сразу под поиском
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 1f),
                                    Color.White.copy(alpha = 0f)
                                )
                            )
                        )
                )
            }
        }
        
        // Нижний градиент (fade effect) - в самом низу устройства, поверх всего
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0f),
                            Color.White.copy(alpha = 1f)
                        )
                    )
                )
        )
        
        // FiltersAlert
        if (uiState is ProjectsUiState.Success) {
            FiltersAlert(
                isVisible = showFilters,
                onDismiss = { showFilters = false },
                tags = (uiState as ProjectsUiState.Success).tags,
                filters = filters,
                onFiltersChange = { filters = it }
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Загрузка проектов...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRetry) {
                Text("Повторить")
            }
        }
    }
}

@Composable
private fun ProjectsContent(
    projects: List<Project>,
    tags: List<Tag>,
    isLoadingMore: Boolean,
    onProjectClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tagMap = tags.associateBy { it.id }

    if (projects.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Нет активных проектов",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 180.dp // Увеличенный padding для дополнительного пространства прокрутки
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(projects) { index, project ->
                ProjectCard(
                    project = project,
                    tags = project.tags?.mapNotNull { tagMap[it] } ?: emptyList(),
                    onClick = { onProjectClick(project.slug ?: project.id) }
                )

                // Загружаем следующую страницу когда осталось 3 элемента до конца
                if (index >= projects.size - 3 && !isLoadingMore) {
                    onLoadMore()
                }
            }

            // Индикатор загрузки внизу списка
            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    tags: List<Tag>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fontFamily = openSansFamily()
    
    // Состояние для анимации нажатия
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Card(
        modifier = modifier
            .width(375.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent // Прозрачный фон
        ),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column {
            // Полоска цвета 1 сверху
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppColors.Color1)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp, vertical = 5.dp)
            ) {
                // Дата слева (фиксированная ширина 50dp, без паддинга слева)
                Column(
                    modifier = Modifier.width(50.dp).padding(start = 0.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    val dateParts = formatDateForCard(project.dateStart ?: project.dateEnd ?: "")
                    if (dateParts.isNotEmpty()) {
                        Text(
                            text = dateParts.first(),
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            lineHeight = 20.sp,
                            color = AppColors.Color2
                        )
                        Text(
                            text = dateParts.drop(1).joinToString(" "),
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            lineHeight = 10.sp,
                            color = AppColors.Color2,
                            modifier = Modifier.offset(y = (-4).dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Контент справа
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Титул с заглушкой для статусов
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = project.name,
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = AppColors.Color2,
                            modifier = Modifier.weight(1f)
                        )
                        // Заглушка для статусов (24x24)
                        Spacer(modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Описание (5 строк)
                    Text(
                        text = project.shortDescription ?: project.description ?: "",
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.sp,
                        color = AppColors.Color2,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Блок с 3 данными
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // Вертикальная линия слева
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(AppColors.Color1)
                        )

                        // Блок 1: Срок записи на проект (первая дата)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(start = 10.dp, end = 10.dp),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Срок записи\nна проект",
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                lineHeight = 10.sp,
                                color = AppColors.Color2
                            )
                            Text(
                                text = formatDateDots(project.dateStart),
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                lineHeight = 10.sp,
                                color = AppColors.Color2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Вертикальная полоска
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(AppColors.Color1)
                        )

                        // Блок 2: Срок реализации проекта (вторая дата)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(start = 10.dp, end = 10.dp),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Срок реализации\nпроекта",
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                lineHeight = 10.sp,
                                color = AppColors.Color2
                            )
                            Text(
                                text = formatDateDots(project.dateEnd),
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                lineHeight = 10.sp,
                                color = AppColors.Color2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Вертикальная полоска
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(AppColors.Color1)
                        )

                        // Блок 3: Заказчик (во всю оставшуюся ширину)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(start = 10.dp, end = 10.dp),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Заказчик",
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                lineHeight = 10.sp,
                                color = AppColors.Color2,
                                maxLines = 1
                            )
                            Text(
                                text = project.client ?: "Не указан",
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                lineHeight = 10.sp,
                                color = AppColors.Color2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Теги проекта: сколько поместится в строку, дальше перенос, строк сколько угодно
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.forEach { tag ->
                            ProjectTagChip(tag = tag)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagChip(
    tag: Tag,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = tag.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

private fun formatDate(dateString: String): String {
    // Simple date formatting - можно улучшить используя kotlinx-datetime
    return dateString.take(10)
}

private fun formatDateDots(dateString: String?): String {
    if (dateString.isNullOrBlank()) return "Не указано"
    val s = dateString.take(10)
    val parts = s.split("-")
    return if (parts.size == 3) {
        "${parts[2]}.${parts[1]}.${parts[0]}" // yyyy-MM-dd -> dd.MM.yyyy
    } else s
}

private fun formatDateForCard(dateString: String): List<String> {
    if (dateString.isEmpty()) return emptyList()
    // Формат: "08 сен 2025" -> ["08", "сен", "2025"]
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

private fun formatDateRange(start: String?, end: String?): String {
    val startFormatted = start?.take(10) ?: ""
    val endFormatted = end?.take(10) ?: ""
    return when {
        startFormatted.isNotEmpty() && endFormatted.isNotEmpty() -> "$startFormatted - $endFormatted"
        startFormatted.isNotEmpty() -> "с $startFormatted"
        endFormatted.isNotEmpty() -> "до $endFormatted"
        else -> "Не указано"
    }
}

@Composable
private fun ProjectTagChip(
    tag: Tag,
    modifier: Modifier = Modifier
) {
    val fontFamily = openSansFamily()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = AppColors.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Color1)
    ) {
        Text(
            text = tag.name,
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            color = AppColors.Color2,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 0.dp)
        )
    }
}


// Вспомогательные функции для preview
@Suppress("UNCHECKED_CAST")
private fun createPreviewViewModel(state: ProjectsUiState): ProjectsViewModel {
    // Создаем анонимный объект, который имитирует ProjectsViewModel
    return object {
        val uiState: StateFlow<ProjectsUiState> = MutableStateFlow(state)
        fun loadMoreProjects() {}
        fun retry() {}
    } as ProjectsViewModel
}

private fun getSampleTags() = listOf(
    Tag(id = 1, name = "Android"),
    Tag(id = 2, name = "iOS"),
    Tag(id = 3, name = "ML"),
    Tag(id = 4, name = "Backend"),
    Tag(id = 5, name = "Data"),
    Tag(id = 6, name = "C++")
)

private fun getSampleProjects() = listOf(
    Project(
        id = "1",
        slug = "cosmic-analysis",
        name = "Анализ и прогнозирование манёвра космического аппарата (КА)",
        shortDescription = "В современном мире сложно переоценить важность актуальной информации. Каждая компания стремится показать клиенту свои достижения и скрыть недостатки.",
        description = null,
        dateStart = "2025-09-08",
        dateEnd = "2025-12-20",
        tags = listOf(1, 3, 4),
        client = "ООО Космические технологии"
    ),
    Project(
        id = "2",
        slug = "mobile-clinic",
        name = "Мобильное приложение для IT-клиники СПбГУ",
        shortDescription = "Разработка мобильного приложения для управления проектами IT-клиники СПбГУ с поддержкой Android и iOS.",
        description = null,
        dateStart = "2024-09-01",
        dateEnd = "2025-06-30",
        tags = listOf(1, 2, 4),
        client = "IT-клиника СПбГУ"
    ),
    Project(
        id = "3",
        slug = "data-analysis",
        name = "Система анализа больших данных",
        shortDescription = "Создание системы для анализа и визуализации больших объемов данных с использованием современных ML алгоритмов.",
        description = null,
        dateStart = "2025-02-01",
        dateEnd = "2025-08-15",
        tags = listOf(3, 5, 4),
        client = "DataCorp"
    )
)
