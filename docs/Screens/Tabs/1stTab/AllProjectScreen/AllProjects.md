# Экран «Проекты»

## Когда показывается
Экран открывается при повторном запуске приложения. Это первая вкладка таббара.

## Назначение
Показывает все проекты IT-клиники и даёт быстрые действия:
- **Предложить проект** — для заказчиков.
- **Мой проект** — доступно только авторизованным пользователям (если у пользователя есть проект).

## Функции
- Список проектов в скролле.
- Поиск по проектам.
- Фильтры для уточнения выдачи.

## Структура экрана
1. Заголовок **«Проекты»** (вверху).
2. Поле поиска + кнопка/панель фильтров.
3. Список проектов (прокручиваемый).

## Дополнительное меню

Справа над таббаром находится кнопка действий.

- **Если пользователь авторизован**, это выпадающее меню. По нажатию раскрываются действия:
  - **Мой проект**
  - **Предложить проект**

- **Если пользователь не авторизован**, выпадающего меню нет — отображается одна кнопка:
  - **Предложить проект**

## Техническая реализация

### API запросы

#### Получение списка проектов

**Эндпоинт:** `POST /project/findmany`

**Запрос:**
```json
{
  "filters": {},
  "page": 1
}
```

**Ответ:**
```json
{
  "projects": [
    {
      "id": "string",
      "name": "string",
      "description": "string",
      "shortDescription": "string",
      "dateStart": "string",
      "dateEnd": "string",
      "slug": "string",
      "tags": ["string"]
    }
  ],
  "tags": [
    {
      "id": "string",
      "name": "string",
      "description": "string"
    }
  ]
}
```

**Реализация:**
- Файл: `composeApp/src/commonMain/kotlin/com/spbu/projecttrack/projects/data/api/ProjectsApi.kt`
- Метод: `getProjects(page: Int = 1)`
- Репозиторий: `ProjectsRepository.getProjects(page)`

### Пагинация

- Размер страницы: 5 проектов
- Автоматическая загрузка следующей страницы при прокрутке (когда остаётся 3 элемента до конца)
- Определение последней страницы: если количество проектов меньше 5

**Логика загрузки:**
1. При инициализации загружается первая страница (`page = 1`)
2. При прокрутке вниз автоматически загружается следующая страница
3. Проекты накапливаются в списке (`currentProjects + newProjects`)
4. Теги объединяются и дедуплицируются по `id`

**Реализация:**
- ViewModel: `ProjectsViewModel.loadMoreProjects()`
- Состояние: `ProjectsUiState.Success` содержит `hasMorePages: Boolean`

### Поиск

Поиск выполняется локально на клиенте после загрузки данных.

**Поля поиска:**
- `project.name` (название проекта)
- `project.shortDescription` (краткое описание)
- `project.description` (полное описание)

**Реализация:**
- Файл: `ProjectsScreen.kt`
- Фильтрация: `state.projects.filter { ... }`
- Поиск регистронезависимый (`ignoreCase = true`)

### Фильтрация

Фильтры применяются через модальное окно `FiltersAlert`. Подробнее см. `ProjectsFiltersAlert.md`.

**Типы фильтров:**
- Теги (множественный выбор)
- Срок записи на проект (диапазон дат)
- Срок реализации проекта (диапазон дат)

**Модель фильтров:**
```kotlin
data class ProjectFilters(
    val selectedTags: Set<String> = emptySet(),
    val enrollmentStartDate: String? = null,
    val enrollmentEndDate: String? = null,
    val projectStartDate: String? = null,
    val projectEndDate: String? = null
)
```

### Состояния экрана

**ProjectsUiState:**
- `Loading` — загрузка данных
- `Success(projects, tags, hasMorePages, isLoadingMore)` — успешная загрузка
- `Error(message)` — ошибка загрузки

### Навигация

При клике на карточку проекта:
- Переход на экран детального просмотра
- Передача `project.slug` или `project.id` как идентификатора
- Файл: `ProjectViewScreen.kt`

### Архитектура

**Слои:**
1. **UI:** `ProjectsScreen.kt` — Compose UI
2. **ViewModel:** `ProjectsViewModel.kt` — управление состоянием
3. **Repository:** `ProjectsRepository.kt` — абстракция данных
4. **API:** `ProjectsApi.kt` — HTTP запросы

**Поток данных:**
```
ProjectsScreen → ProjectsViewModel → ProjectsRepository → ProjectsApi → HTTP
```