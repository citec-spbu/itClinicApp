package com.spbu.projecttrack.core.email

import com.spbu.projecttrack.core.logging.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual object FeedbackMailSender {

    private const val logTag = "FeedbackMailSender"

    actual suspend fun send(payload: FeedbackMailPayload): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val config = requireSmtpMailConfig()
            val socket = (SSLSocketFactory.getDefault().createSocket(
                config.host,
                config.port,
            ) as SSLSocket).apply {
                soTimeout = 30_000
                startHandshake()
            }

            socket.use { secureSocket ->
                val reader = BufferedReader(InputStreamReader(secureSocket.inputStream, Charsets.UTF_8))
                val writer = BufferedWriter(OutputStreamWriter(secureSocket.outputStream, Charsets.UTF_8))

                expectCode(readResponse(reader), 220)
                sendCommand(writer, "EHLO citec.app")
                expectCode(readResponse(reader), 250)

                sendCommand(writer, "AUTH LOGIN")
                expectCode(readResponse(reader), 334)
                sendCommand(writer, base64(config.username))
                expectCode(readResponse(reader), 334)
                sendCommand(writer, base64(config.password))
                expectCode(readResponse(reader), 235)

                sendCommand(writer, "MAIL FROM:<${config.fromEmail}>")
                expectCode(readResponse(reader), 250)
                sendCommand(writer, "RCPT TO:<${config.toEmail}>")
                expectCode(readResponse(reader), 250, 251)
                sendCommand(writer, "DATA")
                expectCode(readResponse(reader), 354)

                writer.write(dotStuff(buildFeedbackMimeMessage(config, payload)))
                writer.write("\r\n.\r\n")
                writer.flush()
                expectCode(readResponse(reader), 250)

                sendCommand(writer, "QUIT")
                readResponse(reader)
                Unit
            }
        }.onFailure { error ->
            AppLog.e(logTag, "SMTP feedback send failed", error)
        }
    }

    private fun sendCommand(writer: BufferedWriter, command: String) {
        writer.write(command)
        writer.write("\r\n")
        writer.flush()
    }

    private fun readResponse(reader: BufferedReader): SmtpResponse {
        val lines = mutableListOf<String>()
        while (true) {
            val line = reader.readLine() ?: error("SMTP connection closed unexpectedly")
            lines += line
            if (line.length < 4 || line[3] != '-') break
        }

        val statusCode = lines.last().take(3).toIntOrNull()
            ?: error("Invalid SMTP response: ${lines.lastOrNull().orEmpty()}")

        return SmtpResponse(statusCode = statusCode, text = lines.joinToString("\n"))
    }

    private fun expectCode(response: SmtpResponse, vararg expectedCodes: Int) {
        if (expectedCodes.none { it == response.statusCode }) {
            error("SMTP ${response.statusCode}: ${response.text}")
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun base64(value: String): String = Base64.encode(value.encodeToByteArray())

    private data class SmtpResponse(
        val statusCode: Int,
        val text: String,
    )
}
