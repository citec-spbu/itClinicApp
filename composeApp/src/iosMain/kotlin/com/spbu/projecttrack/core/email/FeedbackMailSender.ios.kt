@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.spbu.projecttrack.core.email

import com.spbu.projecttrack.core.logging.AppLog
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreFoundation.CFAbsoluteTimeGetCurrent
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFReadStreamClose
import platform.CoreFoundation.CFReadStreamGetStatus
import platform.CoreFoundation.CFReadStreamHasBytesAvailable
import platform.CoreFoundation.CFReadStreamOpen
import platform.CoreFoundation.CFReadStreamRead
import platform.CoreFoundation.CFReadStreamRef
import platform.CoreFoundation.CFReadStreamRefVar
import platform.CoreFoundation.CFReadStreamSetProperty
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStreamCreatePairWithSocketToHost
import platform.CoreFoundation.CFWriteStreamCanAcceptBytes
import platform.CoreFoundation.CFWriteStreamClose
import platform.CoreFoundation.CFWriteStreamGetStatus
import platform.CoreFoundation.CFWriteStreamOpen
import platform.CoreFoundation.CFWriteStreamRef
import platform.CoreFoundation.CFWriteStreamRefVar
import platform.CoreFoundation.CFWriteStreamSetProperty
import platform.CoreFoundation.CFWriteStreamWrite
import platform.CoreFoundation.kCFStreamPropertySocketSecurityLevel
import platform.CoreFoundation.kCFStreamSocketSecurityLevelNegotiatedSSL
import platform.CoreFoundation.kCFStreamStatusAtEnd
import platform.CoreFoundation.kCFStreamStatusClosed
import platform.CoreFoundation.kCFStreamStatusError
import platform.CoreFoundation.kCFStreamStatusOpen
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Foundation.NSThread
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual object FeedbackMailSender {

    private const val logTag = "FeedbackMailSender"
    private const val pollIntervalSeconds = 0.05
    private const val streamTimeoutSeconds = 30.0

    actual suspend fun send(payload: FeedbackMailPayload): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val config = requireSmtpMailConfig()
            val connection = openConnection(config)

            try {
                expectCode(connection.readResponse(), 220)
                connection.sendCommand("EHLO citec.app")
                expectCode(connection.readResponse(), 250)

                connection.sendCommand("AUTH LOGIN")
                expectCode(connection.readResponse(), 334)
                connection.sendCommand(base64(config.username))
                expectCode(connection.readResponse(), 334)
                connection.sendCommand(base64(config.password))
                expectCode(connection.readResponse(), 235)

                connection.sendCommand("MAIL FROM:<${config.fromEmail}>")
                expectCode(connection.readResponse(), 250)
                connection.sendCommand("RCPT TO:<${config.toEmail}>")
                expectCode(connection.readResponse(), 250, 251)
                connection.sendCommand("DATA")
                expectCode(connection.readResponse(), 354)

                connection.writeRaw(dotStuff(buildFeedbackMimeMessage(config, payload)))
                connection.writeRaw("\r\n.\r\n")
                expectCode(connection.readResponse(), 250)

                connection.sendCommand("QUIT")
                runCatching { connection.readResponse() }
                Unit
            } finally {
                connection.close()
            }
        }.onFailure { error ->
            AppLog.e(logTag, "SMTP feedback send failed on iOS", error)
        }
    }

    private fun openConnection(config: SmtpMailConfig): SmtpConnection = memScoped {
        val inputRef = allocArray<CFReadStreamRefVar>(1)
        val outputRef = allocArray<CFWriteStreamRefVar>(1)
        val hostRef = CFStringCreateWithCString(
            alloc = null,
            cStr = config.host,
            encoding = kCFStringEncodingUTF8,
        ) ?: error("Не удалось создать CFString для SMTP host")

        try {
            CFStreamCreatePairWithSocketToHost(
                alloc = null,
                host = hostRef,
                port = config.port.toUInt(),
                readStream = inputRef,
                writeStream = outputRef,
            )
        } finally {
            CFRelease(hostRef)
        }

        val input = inputRef[0] ?: error("Не удалось создать CFReadStream для SMTP")
        val output = outputRef[0] ?: error("Не удалось создать CFWriteStream для SMTP")

        check(CFReadStreamSetProperty(input, kCFStreamPropertySocketSecurityLevel, kCFStreamSocketSecurityLevelNegotiatedSSL)) {
            "Не удалось включить SSL для SMTP input stream"
        }
        check(CFWriteStreamSetProperty(output, kCFStreamPropertySocketSecurityLevel, kCFStreamSocketSecurityLevelNegotiatedSSL)) {
            "Не удалось включить SSL для SMTP output stream"
        }
        check(CFReadStreamOpen(input)) { "Не удалось открыть SMTP input stream" }
        check(CFWriteStreamOpen(output)) { "Не удалось открыть SMTP output stream" }

        waitForOpen(input, output)
        SmtpConnection(input = input, output = output)
    }

    private fun waitForOpen(input: CFReadStreamRef, output: CFWriteStreamRef) {
        waitUntil("SMTP open timeout") {
            when {
                CFReadStreamGetStatus(input) == kCFStreamStatusError.toLong() -> error("SMTP input stream error")
                CFWriteStreamGetStatus(output) == kCFStreamStatusError.toLong() -> error("SMTP output stream error")
                CFReadStreamGetStatus(input) == kCFStreamStatusOpen.toLong() &&
                    CFWriteStreamGetStatus(output) == kCFStreamStatusOpen.toLong() -> true
                else -> false
            }
        }
    }

    private fun waitForReadable(input: CFReadStreamRef) {
        waitUntil("SMTP read timeout") {
            when {
                CFReadStreamHasBytesAvailable(input) -> true
                CFReadStreamGetStatus(input) == kCFStreamStatusAtEnd.toLong() -> true
                CFReadStreamGetStatus(input) == kCFStreamStatusError.toLong() -> error("SMTP input stream error")
                CFReadStreamGetStatus(input) == kCFStreamStatusClosed.toLong() -> error("SMTP input stream closed")
                else -> false
            }
        }
    }

    private fun waitForWritable(output: CFWriteStreamRef) {
        waitUntil("SMTP write timeout") {
            when {
                CFWriteStreamCanAcceptBytes(output) -> true
                CFWriteStreamGetStatus(output) == kCFStreamStatusError.toLong() -> error("SMTP output stream error")
                CFWriteStreamGetStatus(output) == kCFStreamStatusClosed.toLong() -> error("SMTP output stream closed")
                CFWriteStreamGetStatus(output) == kCFStreamStatusAtEnd.toLong() -> error("SMTP output stream ended")
                else -> false
            }
        }
    }

    private fun waitUntil(timeoutMessage: String, predicate: () -> Boolean) {
        val deadline = CFAbsoluteTimeGetCurrent() + streamTimeoutSeconds
        while (CFAbsoluteTimeGetCurrent() < deadline) {
            if (predicate()) return
            NSThread.sleepForTimeInterval(pollIntervalSeconds)
        }
        error(timeoutMessage)
    }

    private fun expectCode(response: SmtpResponse, vararg expectedCodes: Int) {
        if (expectedCodes.none { it == response.statusCode }) {
            error("SMTP ${response.statusCode}: ${response.text}")
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun base64(value: String): String = Base64.encode(value.encodeToByteArray())

    private data class SmtpConnection(
        val input: CFReadStreamRef,
        val output: CFWriteStreamRef,
    ) {
        fun close() {
            runCatching { CFReadStreamClose(input) }
            runCatching { CFWriteStreamClose(output) }
        }

        fun sendCommand(command: String) {
            writeRaw(command)
            writeRaw("\r\n")
        }

        fun writeRaw(value: String) {
            val data = value.encodeToByteArray()
            var offset = 0

            while (offset < data.size) {
                FeedbackMailSender.waitForWritable(output)
                val written = data.usePinned { pinned ->
                    CFWriteStreamWrite(
                        output,
                        pinned.addressOf(offset).reinterpret(),
                        (data.size - offset).toLong(),
                    )
                }

                if (written <= 0) {
                    error("SMTP write failed")
                }

                offset += written.toInt()
            }
        }

        fun readResponse(): SmtpResponse {
            val lines = mutableListOf<String>()

            while (true) {
                val line = readLine() ?: error("SMTP connection closed unexpectedly")
                lines += line
                if (line.length < 4 || line[3] != '-') break
            }

            val statusCode = lines.last().take(3).toIntOrNull()
                ?: error("Invalid SMTP response: ${lines.lastOrNull().orEmpty()}")

            return SmtpResponse(
                statusCode = statusCode,
                text = lines.joinToString("\n"),
            )
        }

        private fun readLine(): String? {
            val bytes = mutableListOf<Byte>()
            val buffer = ByteArray(1)

            while (true) {
                FeedbackMailSender.waitForReadable(input)
                val read = buffer.usePinned { pinned ->
                    CFReadStreamRead(
                        input,
                        pinned.addressOf(0).reinterpret(),
                        1L,
                    )
                }

                if (read < 0) {
                    error("SMTP read failed")
                }
                if (read == 0L) {
                    return if (bytes.isEmpty()) {
                        null
                    } else {
                        bytes.toByteArray().decodeToString()
                    }
                }

                val byte = buffer[0]
                if (byte == '\n'.code.toByte()) break
                if (byte != '\r'.code.toByte()) {
                    bytes += byte
                }
            }

            return bytes.toByteArray().decodeToString()
        }
    }

    private data class SmtpResponse(
        val statusCode: Int,
        val text: String,
    )
}
