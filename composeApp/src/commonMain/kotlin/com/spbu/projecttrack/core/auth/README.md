# Модуль авторизации

## Структура

```
core/auth/
├── AuthManager.kt          # Менеджер токенов авторизации
└── README.md              # Эта документация

core/network/
├── AuthInterceptor.kt     # Автоматическое добавление токена к запросам
├── ApiConfig.kt           # Конфигурация API (локальный/продакшн)
└── HttpClientFactory.kt   # Фабрика HTTP клиента
```

## AuthManager

Синглтон для управления токенами авторизации.

### Основные методы

```kotlin
// Установить токен после успешной авторизации
AuthManager.setToken(token: String)

// Очистить токен (выход из системы)
AuthManager.clearToken()

// Получить текущий токен
val token = AuthManager.getToken()

// Проверить статус авторизации
val isAuthorized = AuthManager.isAuthorized.collectAsState()
```

### Тестовый токен (только для разработки)

```kotlin
// TODO: Удалить перед релизом!
AuthManager.setTestToken()
```

## AuthInterceptor

Ktor плагин, который автоматически добавляет токен авторизации ко всем HTTP запросам.

### Как работает

1. Перед каждым запросом проверяет наличие токена в `AuthManager`
2. Если токен есть, добавляет заголовок `Authorization: Bearer {token}`
3. Если токена нет, запрос отправляется без заголовка

### Установка

```kotlin
// В HttpClientFactory
HttpClient(Engine) {
    // ... другие плагины
    install(AuthInterceptor)
}
```

## ApiConfig

Конфигурация для переключения между локальным и продакшн API.

### Использование

```kotlin
// Получить текущий базовый URL
val baseUrl = ApiConfig.baseUrl

// Проверить, используется ли локальный API
if (ApiConfig.isLocalApi) {
    // Локальный бэкенд
}

// Публичные эндпоинты
val endpoint = ApiConfig.Public.PROJECT_ACTIVE

// Эндпоинты с авторизацией
val endpoint = ApiConfig.AuthRequired.USER_ME
```

### Переключение между локальным и продакшн

В `ApiConfig.kt`:

```kotlin
// Для локального бэкенда
private const val USE_LOCAL_API = true

// Для продакшн бэкенда
private const val USE_LOCAL_API = false
```

## Пример использования

### 1. Авторизация пользователя

```kotlin
// В OnboardingScreen
Button(
    onClick = {
        // TODO: Реализовать GitHub OAuth
        // После успешной авторизации:
        AuthManager.setToken(receivedToken)
        onGitHubAuth()
    }
)
```

### 2. Проверка статуса авторизации

```kotlin
@Composable
fun SomeScreen() {
    val isAuthorized by AuthManager.isAuthorized.collectAsState()
    
    if (isAuthorized) {
        // Показать контент для авторизованных пользователей
        AuthorizedContent()
    } else {
        // Показать контент для гостей
        GuestContent()
    }
}
```

### 3. Выход из системы

```kotlin
Button(
    onClick = {
        AuthManager.clearToken()
        // Очистить сохраненный токен
        appPreferences.clearAuthToken()
        // Перейти на экран входа
        navigateToOnboarding()
    }
) {
    Text("Выйти")
}
```

### 4. Сохранение токена

```kotlin
// После авторизации
AuthManager.setToken(token)
appPreferences.saveAuthToken(token)

// При запуске приложения
val savedToken = appPreferences.getAuthToken()
if (savedToken != null) {
    AuthManager.setToken(savedToken)
}
```

## Безопасность

### ⚠️ Важно

1. **НЕ храните токены в коде** - используйте `AppPreferences` или `Keychain`/`KeyStore`
2. **НЕ логируйте токены** - они могут попасть в логи
3. **Проверяйте срок действия токена** - обновляйте через refresh token
4. **Используйте HTTPS** - для продакшн API
5. **Очищайте токен при выходе** - не оставляйте в памяти

### Хранение токенов

#### Android (рекомендуется)
```kotlin
// Использовать EncryptedSharedPreferences
val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

#### iOS (рекомендуется)
```kotlin
// Использовать Keychain
// TODO: Реализовать через expect/actual
```

## TODO

- [ ] Реализовать GitHub OAuth
- [ ] Добавить refresh token logic
- [ ] Реализовать безопасное хранение токенов (Keychain/KeyStore)
- [ ] Добавить автоматическое обновление токена
- [ ] Добавить обработку ошибок 401 (токен истек)
- [ ] Удалить `setTestToken()` перед релизом
- [ ] Добавить логирование (без токенов!)








