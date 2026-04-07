package com.spbu.projecttrack.rating.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite

@Composable
actual fun rememberProjectStatsExporter(): ProjectStatsExporter {
    return remember {
        IosProjectStatsExporter()
    }
}

@OptIn(ExperimentalForeignApi::class)
private class IosProjectStatsExporter : ProjectStatsExporter {
    override suspend fun exportPdf(
        payload: ProjectStatsExportPayload
    ): Result<ProjectStatsExportResult> = runCatching {
        val fileName = "${sanitizeProjectStatsFileName(payload.projectName)}.pdf"
        val path = NSTemporaryDirectory() + fileName
        val pdf = buildMinimalPdf(buildProjectStatsReportLines(payload))
        writeTextFile(path, pdf)
        presentShareSheet(path, fileName, "application/pdf")
        ProjectStatsExportResult(
            fileName = fileName,
            absolutePath = path,
            mimeType = "application/pdf"
        )
    }

    override suspend fun exportExcelCsv(
        payload: ProjectStatsExportPayload
    ): Result<ProjectStatsExportResult> = runCatching {
        val fileName = "${sanitizeProjectStatsFileName(payload.projectName)}.csv"
        val path = NSTemporaryDirectory() + fileName
        writeTextFile(path, buildProjectStatsCsv(payload))
        presentShareSheet(path, fileName, "text/csv")
        ProjectStatsExportResult(
            fileName = fileName,
            absolutePath = path,
            mimeType = "text/csv"
        )
    }

    private fun writeTextFile(path: String, content: String) {
        val bytes = content.encodeToByteArray()
        val file = fopen(path, "wb") ?: error("Не удалось открыть файл для записи: $path")
        try {
            bytes.usePinned { pinned ->
                val written = fwrite(
                    pinned.addressOf(0),
                    1.convert(),
                    bytes.size.convert(),
                    file
                ).toInt()
                if (written != bytes.size) {
                    error("Не удалось полностью записать файл: $path")
                }
            }
        } finally {
            fclose(file)
        }
    }

    private fun presentShareSheet(path: String, fileName: String, mimeType: String) {
        val url = NSURL.fileURLWithPath(path)
        val controller = UIActivityViewController(
            activityItems = listOf(url),
            applicationActivities = null
        )

        val root = UIApplication.sharedApplication.keyWindow?.rootViewController
        root?.presentViewController(controller, animated = true, completion = null)
    }
}

private fun buildMinimalPdf(lines: List<String>): String {
    val pageWidth = 595
    val pageHeight = 842
    val topMargin = 72
    val leftMargin = 48
    val lineHeight = 16

    val contentLines = buildList {
        add("BT")
        add("/F1 12 Tf")
        add("${leftMargin} ${pageHeight - topMargin} Td")
        lines.forEachIndexed { index, rawLine ->
            val line = escapePdfText(rawLine)
            if (index == 0) {
                add("($line) Tj")
            } else {
                add("0 -$lineHeight Td")
                add("($line) Tj")
            }
        }
        add("ET")
    }

    val streamContent = contentLines.joinToString("\n")
    val objects = mutableListOf<String>()
    objects += "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj"
    objects += "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj"
    objects += buildString {
        append("3 0 obj << /Type /Page /Parent 2 0 R ")
        append("/MediaBox [0 0 $pageWidth $pageHeight] ")
        append("/Contents 4 0 R ")
        append("/Resources << /Font << /F1 5 0 R >> >> >> endobj")
    }
    objects += "4 0 obj << /Length ${streamContent.encodeToByteArray().size} >> stream\n$streamContent\nendstream endobj"
    objects += "5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj"

    val header = "%PDF-1.4\n"
    val builder = StringBuilder(header)
    val offsets = mutableListOf<Int>()
    offsets += 0
    var currentOffset = header.encodeToByteArray().size

    objects.forEach { objectText ->
        offsets += currentOffset
        builder.append(objectText).append('\n')
        currentOffset = builder.toString().encodeToByteArray().size
    }

    val xrefOffset = builder.toString().encodeToByteArray().size
    val xref = buildString {
        append("xref\n")
        append("0 ${objects.size + 1}\n")
        append("0000000000 65535 f \n")
        offsets.drop(1).forEach { offset ->
            append(offset.toString().padStart(10, '0')).append(" 00000 n \n")
        }
        append("trailer\n")
        append("<< /Size ${objects.size + 1} /Root 1 0 R >>\n")
        append("startxref\n")
        append("$xrefOffset\n")
        append("%%EOF")
    }

    return builder.append(xref).toString()
}

private fun escapePdfText(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("(", "\\(")
        .replace(")", "\\)")
        .replace("\r", " ")
        .replace("\n", " ")
}
