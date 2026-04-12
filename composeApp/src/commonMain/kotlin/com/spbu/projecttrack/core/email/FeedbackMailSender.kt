package com.spbu.projecttrack.core.email

import com.spbu.projecttrack.MailConfig
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class FeedbackMailPayload(
    val senderName: String,
    val senderEmail: String,
    val message: String,
)

internal data class SmtpMailConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val fromEmail: String,
    val fromName: String,
    val toEmail: String,
    val subject: String,
)

expect object FeedbackMailSender {
    suspend fun send(payload: FeedbackMailPayload): Result<Unit>
}

internal fun requireSmtpMailConfig(): SmtpMailConfig {
    val username = MailConfig.SMTP_USERNAME.trim()
    val password = MailConfig.SMTP_PASSWORD.trim()
    val fromEmail = MailConfig.SMTP_FROM_EMAIL.trim().ifBlank { username }
    val toEmail = MailConfig.FEEDBACK_TO_EMAIL.trim()

    require(username.isNotBlank()) { "MailConfig.kt: заполните SMTP_USERNAME" }
    require(password.isNotBlank()) { "MailConfig.kt: заполните SMTP_PASSWORD" }
    require(fromEmail.isNotBlank()) { "MailConfig.kt: заполните SMTP_FROM_EMAIL" }
    require(toEmail.isNotBlank()) { "MailConfig.kt: заполните FEEDBACK_TO_EMAIL" }

    return SmtpMailConfig(
        host = MailConfig.SMTP_HOST.trim().ifBlank { "smtp.yandex.ru" },
        port = MailConfig.SMTP_PORT,
        username = username,
        password = password,
        fromEmail = fromEmail,
        fromName = MailConfig.SMTP_FROM_NAME.trim().ifBlank { "CITEC" },
        toEmail = toEmail,
        subject = MailConfig.FEEDBACK_SUBJECT.trim().ifBlank { "ОБРАТНАЯ СВЯЗЬ CITEC" },
    )
}

@OptIn(ExperimentalEncodingApi::class)
internal fun buildFeedbackMimeMessage(
    config: SmtpMailConfig,
    payload: FeedbackMailPayload,
): String {
    val body = buildString {
        appendLine("Сообщение из раздела обратной связи приложения CITEC.")
        appendLine()
        appendLine("Пользователь: ${payload.senderName.ifBlank { "Не указан" }}")
        appendLine("Email пользователя: ${payload.senderEmail.ifBlank { "Не указан" }}")
        appendLine()
        appendLine("Текст сообщения:")
        appendLine(payload.message.trim())
    }.trimEnd()

    val encodedSubject = mimeHeader(config.subject)
    val encodedFromName = mimeHeader(config.fromName)
    val encodedBody = base64WithCrlf(body)

    val replyToHeader = payload.senderEmail.trim()
        .takeIf { it.isNotBlank() }
        ?.let { "Reply-To: <$it>\r\n" }
        .orEmpty()

    return buildString {
        append("From: $encodedFromName <${config.fromEmail}>\r\n")
        append("To: <${config.toEmail}>\r\n")
        append("Subject: $encodedSubject\r\n")
        append(replyToHeader)
        append("MIME-Version: 1.0\r\n")
        append("Content-Type: text/plain; charset=UTF-8\r\n")
        append("Content-Transfer-Encoding: base64\r\n")
        append("\r\n")
        append(encodedBody)
    }
}

internal fun dotStuff(data: String): String {
    return data
        .replace("\r\n", "\n")
        .split("\n")
        .joinToString("\r\n") { line ->
            if (line.startsWith(".")) ".$line" else line
        }
}

@OptIn(ExperimentalEncodingApi::class)
private fun mimeHeader(value: String): String {
    val trimmed = value.trim()
    return "=?UTF-8?B?${Base64.encode(trimmed.encodeToByteArray())}?="
}

@OptIn(ExperimentalEncodingApi::class)
private fun base64WithCrlf(value: String): String {
    val encoded = Base64.encode(value.encodeToByteArray())
    return encoded.chunked(76).joinToString("\r\n")
}
