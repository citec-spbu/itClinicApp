package com.spbu.projecttrack

/**
 * ШАБЛОН конфигурации сборки
 * 
 * 📋 ИНСТРУКЦИЯ:
 * 1. Скопируйте этот файл как BuildConfig.kt (в ту же папку)
 * 2. Заполните своими значениями
 * 3. BuildConfig.kt уже добавлен в .gitignore и не будет коммититься
 * 
 * Для других разработчиков: используйте этот файл как шаблон!
 */
object BuildConfigExample {
    /**
     * Тестовый токен для локальной разработки
     * Получите токен, запустив: node generate-test-token.js в папке Registry/
     */
    const val TEST_TOKEN = "your_test_token_here"
    
    /**
     * Использовать локальный API (true) или продакшн (false)
     */
    const val USE_LOCAL_API = true
    
    /**
     * URL продакшн API
     */
    const val PRODUCTION_BASE_URL = "https://citec.spb.ru/api"

    /**
     * URL веб-хоста для auth proxy.
     * Пример: https://citec.spb.ru/auth
     */
    const val AUTH_PRODUCTION_BASE_URL = "https://citec.spb.ru/auth"
    
    /**
     * Порт локального API
     */
    const val LOCAL_PORT = 8000

    /**
     * Порт локального web-хоста, через который проксируется /auth.
     */
    const val AUTH_LOCAL_PORT = 3000

    /**
     * IP адрес машины разработчика в локальной сети для реальных устройств.
     * Для GitHub OAuth локальный auth host должен совпадать с callback host,
     * поэтому для auth лучше указывать реальный LAN IP, а не 10.0.2.2.
     * Оставьте пустым, если хотите полагаться только на ручную настройку через NetworkDebugScreen.
     */
    const val LOCAL_HOST_IP = ""

    /**
     * URL продакшн API для метрик
     */
    const val METRIC_PRODUCTION_BASE_URL = "https://metrics.example.com"

    /**
     * Порт локального API для метрик
     */
    const val METRIC_LOCAL_PORT = 4173

    /**
     * Deep link, в который auth backend возвращает мобильное приложение.
     */
    const val MOBILE_AUTH_REDIRECT_URL = "itclinicapp://auth/callback"
    
    /**
     * GitHub OAuth Client ID (если используется)
     */
    const val GITHUB_CLIENT_ID = "your_github_client_id"
    
    /**
     * GitHub OAuth Client Secret (если используется)
     */
    const val GITHUB_CLIENT_SECRET = "your_github_client_secret"
}
