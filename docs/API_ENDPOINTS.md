# API Эндпоинты

## Базовые URL

- **Локальный**: `http://localhost:8000`
- **Продакшн**: `https://citec.spb.ru/api`

## Конфигурация подключения

### Автоматическое определение устройства

Приложение автоматически определяет тип устройства и выбирает правильный URL для подключения к локальному бэкенду.

**Android эмулятор:**
- Автоматически определяется по параметрам `Build.FINGERPRINT`, `Build.MODEL` и другим
- Использует URL: `http://10.0.2.2:8000`

**Android реальное устройство:**
- Автоматически определяется как НЕ эмулятор
- Использует динамически определенный IP адрес компьютера из `LocalDevConfig`
- IP определяется через `NetworkInterface` при каждом запуске

**iOS симулятор:**
- Определяется по `UIDevice.currentDevice.model` (содержит "Simulator")
- Использует URL: `http://localhost:8000`

**iOS реальное устройство:**
- Определяется как НЕ симулятор
- Использует IP адрес из `LocalDevConfig.LOCAL_MACHINE_IP`

### Переключение между локальным и продакшн API

В файле `ApiConfig.kt`:

```kotlin
object ApiConfig {
    // Для локального бэкенда
    private const val USE_LOCAL_API = true
    
    // Для продакшн
    // private const val USE_LOCAL_API = false
}
```

### Логирование конфигурации

При запуске приложения в логах выводится:

```
=== API Configuration ===
Use Local API: true
Is Emulator: true
Local Host: 10.0.2.2
Base URL: http://10.0.2.2:8000
=======================
```

## Публичные эндпоинты (без авторизации)

### Проекты

#### GET `/project/active`
Получить список активных проектов.

**Response:**
```json
{
  "projects": [...],
  "tags": [...]
}
```

#### GET `/project/new`
Получить список новых проектов.

#### POST `/project/findmany`
Поиск проектов с фильтрами и пагинацией.

**Request Body:**
```json
{
  "filters": {},
  "page": 1,
  "pageSize": 10
}
```

#### GET `/project/{slug}`
Получить детальную информацию о проекте.

**Parameters:**
- `slug` - уникальный идентификатор проекта

### Теги

#### GET `/tag`
Получить список всех тегов.

### Email

#### POST `/email/send-request`
Отправить заявку на сотрудничество.

**Request Body:**
```json
{
  "name": "Имя",
  "email": "email@example.com"
}
```

---

## Эндпоинты с авторизацией (требуют токен)

> **Важно:** Все эндпоинты ниже требуют заголовок `Authorization: Bearer {token}`

### Пользователь

#### GET `/user/project-status`
Получить статус проектов пользователя.

**Headers:**
```
Authorization: Bearer {token}
```

#### GET `/user/profile`
Получить профиль пользователя.

#### GET `/user/me`
Получить данные текущего пользователя.

### Заявки (Requests)

#### POST `/request`
Создать новую заявку на проект.

**Request Body (multipart/form-data):**
- `project` (string) - slug проекта
- `team` (number) - ID команды
- `files` (array) - файлы заявки

#### PUT `/request`
Редактировать заявку.

**Request Body (multipart/form-data):**
- `request` (number) - ID заявки
- `files` (array) - новые файлы

#### GET `/request/available`
Получить информацию о доступных заявках для пользователя.

**Response:**
```json
{
  "teams": [
    {
      "id": 25,
      "name": "Иванов И.И., Петров П.П.",
      "projects": ["project-slug-1", "project-slug-2"]
    }
  ],
  "projectReferences": [
    {
      "id": "project-slug",
      "name": "Project Name"
    }
  ]
}
```

#### DELETE `/request/{id}`
Удалить заявку.

**Parameters:**
- `id` (number) - ID заявки

### Результаты проектов

#### POST `/project/results/change-file`
Изменить файл результатов проекта.

#### DELETE `/project/results/delete-file`
Удалить файл результатов проекта.

#### POST `/project/results/upload-file`
Загрузить файл результатов проекта.

### Ссылки проектов

#### POST `/project/links`
Добавить ссылку к проекту.

#### DELETE `/project/links/{id}`
Удалить ссылку проекта.

**Parameters:**
- `id` - ID ссылки

### Профиль

#### PUT `/profile/account`
Редактировать данные аккаунта.

#### PUT `/profile/personal`
Редактировать персональные данные.

### Участники

#### PUT `/member`
Редактировать данные участника.

---

## Коды ответов

- `200` - Успешный запрос
- `401` - Не авторизован (невалидный или отсутствующий токен)
- `403` - Доступ запрещен (нет прав)
- `404` - Ресурс не найден
- `500` - Внутренняя ошибка сервера

---

## Примеры использования

### Публичный запрос (без токена)

```bash
curl https://citec.spb.ru/api/project/active
```

### Запрос с авторизацией (с токеном)

```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8000/user/me
```

### В Kotlin (автоматически через AuthInterceptor)

```kotlin
// Установить токен
AuthManager.setToken("your_token_here")

// Все последующие запросы автоматически включают токен
val response = projectsApi.getUserProjects()
```



