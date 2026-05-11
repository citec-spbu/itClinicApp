package com.spbu.projecttrack

/**
 * Template for local SMTP settings used by the in-app feedback form.
 *
 * Copy this file to `MailConfig.kt`, fill in your credentials, and keep the
 * real file out of Git.
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
