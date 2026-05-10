# Экран «Проекты» — модальное окно «Фильтры»

## Когда показывается
Открывается поверх экрана **«Проекты»** при нажатии на иконку фильтров рядом с поиском. Фон затемняется, список проектов на фоне неактивен.

## Назначение
Позволяет настроить фильтрацию списка проектов по тегам и срокам (датам).

## Содержимое модального окна

### Заголовок
- Заголовок: **«Фильтры»**
- Кнопка закрытия (**×**) в правом верхнем углу.

### Блок «Теги»
- Набор тегов с чекбоксами (**Медицина**, **IT**, **История**, **Общество**, **Социология** ).
- Кнопка **«Очистить»** — сбрасывает выбранные теги.

### Блок «Срок записи на проект»
- Поле выбора периода (интервал дат) с иконкой календаря.
- Кнопка **«Очистить»** — сбрасывает выбранный период.

### Блок «Срок реализации на проект»
- Поле выбора периода (интервал дат) с иконкой календаря.
- Кнопка **«Очистить»** — сбрасывает выбранный период.

### Общий сброс
- Кнопка **«Очистить все»** — сбрасывает все выбранные фильтры сразу.

## Техническая реализация

### Компонент

**Файл:** `composeApp/src/commonMain/kotlin/com/spbu/projecttrack/projects/presentation/components/FiltersAlert.kt`

**Composable функция:**
```kotlin
@Composable
fun FiltersAlert(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    tags: List<Tag>,
    filters: ProjectFilters,
    onFiltersChange: (ProjectFilters) -> Unit
)
```

### Модель фильтров

**Файл:** `composeApp/src/commonMain/kotlin/com/spbu/projecttrack/projects/presentation/models/ProjectFilters.kt`

```kotlin
data class ProjectFilters(
    val selectedTags: Set<String> = emptySet(),
    val enrollmentStartDate: String? = null,
    val enrollmentEndDate: String? = null,
    val projectStartDate: String? = null,
    val projectEndDate: String? = null
) {
    fun hasActiveFilters(): Boolean {
        return selectedTags.isNotEmpty() || 
               enrollmentStartDate != null || 
               enrollmentEndDate != null ||
               projectStartDate != null ||
               projectEndDate != null
    }
    
    fun clear(): ProjectFilters {
        return ProjectFilters()
    }
}
```

### Блок тегов

**Реализация:**
- Выпадающее меню с прокруткой (`LazyColumn`)
- Максимальная высота: 150dp
- Чекбоксы для каждого тега
- Выбор/снятие выбора по клику на строку

**Логика выбора:**
```kotlin
val newTags = if (selectedTags.contains(tag.id)) {
    selectedTags - tag.id  // Удалить из выбранных
} else {
    selectedTags + tag.id  // Добавить в выбранные
}
onTagsChange(newTags)
```

**Источник данных:**
- Теги загружаются из ответа API `/project/findmany` (поле `tags`)
- Передаются в алерт из состояния `ProjectsUiState.Success`

### Блок дат

**Реализация:**
- Два поля для выбора диапазона дат:
  - Срок записи на проект (`enrollmentStartDate`, `enrollmentEndDate`)
  - Срок реализации (`projectStartDate`, `projectEndDate`)

**Формат дат:**
- Строка в формате ISO 8601 или другом формате, поддерживаемом бэкендом

**Компонент:**
- `DateInputField` — поле ввода с лейблом и кнопкой очистки

### Анимация

**Кроссфейд анимация:**
- Вход: `fadeIn` (300ms)
- Выход: `fadeOut` (300ms)
- Реализация: `AnimatedVisibility`

### Применение фильтров

**Текущая реализация:**
- Фильтры сохраняются в состоянии `ProjectFilters`
- Применение фильтров к API запросам реализуется на стороне бэкенда
- В текущей версии фильтры передаются в запрос `POST /project/findmany` в поле `filters`

**Будущая реализация:**
- Фильтры будут преобразовываться в формат, ожидаемый бэкендом
- Структура `filters` в запросе будет соответствовать API контракту

### Индикация активных фильтров

- Кнопка фильтров показывает количество выбранных фильтров
- Визуальная индикация активных фильтров в UI

### Очистка фильтров

**Методы очистки:**
1. Очистить теги — `filters.copy(selectedTags = emptySet())`
2. Очистить даты записи — `filters.copy(enrollmentStartDate = null, enrollmentEndDate = null)`
3. Очистить даты реализации — `filters.copy(projectStartDate = null, projectEndDate = null)`
4. Очистить все — `ProjectFilters()` (создание нового пустого объекта)