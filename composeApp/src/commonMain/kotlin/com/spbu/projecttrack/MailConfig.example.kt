package com.spbu.projecttrack

/**
 * ШАБЛОН локальной SMTP-конфигурации для отправки обратной связи из приложения.
 *
 * 1. Скопируйте этот файл как MailConfig.kt
 * 2. Заполните своими значениями
 * 3. MailConfig.kt уже добавлен в .gitignore и не должен попадать в Git
 *
 * Важно:
 * - Для Яндекса нужен SMTP, а не IMAP
 * - Разрешение IMAP/почтовых клиентов в кабинете Яндекса все равно должно быть включено,
 *   иначе пароль приложения для внешних клиентов не сработает
 */
object MailConfigExample {
    const val SMTP_HOST = "smtp.yandex.ru"
    const val SMTP_PORT = 465
    const val SMTP_USERNAME = "citec@yandex.ru"
    const val SMTP_PASSWORD = "your_app_password_here"
    const val SMTP_FROM_EMAIL = "citec@yandex.ru"
    const val SMTP_FROM_NAME = "CITEC"
    const val FEEDBACK_TO_EMAIL = "sasha-uhnov@yandex.ru"
    const val FEEDBACK_SUBJECT = "ОБРАТНАЯ СВЯЗЬ CITEC"
}
