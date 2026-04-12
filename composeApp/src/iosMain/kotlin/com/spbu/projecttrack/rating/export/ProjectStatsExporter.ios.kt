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
    val titleLineHeight = 22
    val linesPerPage = 44
    val wrappedLines = lines
        .flatMap { wrapPdfLine(it, 82) }
        .ifEmpty { listOf("Статистика проекта") }
    val pages = wrappedLines.chunked(linesPerPage)
    val objects = mutableListOf<String>()
    objects += "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj"
    val firstPageObjectId = 3
    val fontObjectId = firstPageObjectId + pages.size * 2
    val pageObjectIds = pages.indices.map { index -> firstPageObjectId + index * 2 }
    objects += "2 0 obj << /Type /Pages /Kids [${pageObjectIds.joinToString(" ") { "$it 0 R" }}] /Count ${pages.size} >> endobj"

    pages.forEachIndexed { index, pageLines ->
        val pageObjectId = firstPageObjectId + index * 2
        val contentObjectId = pageObjectId + 1
        val streamContent = buildPageStream(
            lines = pageLines,
            leftMargin = leftMargin,
            pageHeight = pageHeight,
            topMargin = topMargin,
            lineHeight = lineHeight,
            titleLineHeight = titleLineHeight,
            isFirstPage = index == 0,
        )

        objects += buildString {
            append("$pageObjectId 0 obj << /Type /Page /Parent 2 0 R ")
            append("/MediaBox [0 0 $pageWidth $pageHeight] ")
            append("/Contents $contentObjectId 0 R ")
            append("/Resources << /Font << /F1 $fontObjectId 0 R >> >> >> endobj")
        }
        objects += "$contentObjectId 0 obj << /Length ${streamContent.encodeToByteArray().size} >> stream\n$streamContent\nendstream endobj"
    }

    objects += "$fontObjectId 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj"

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

private fun buildPageStream(
    lines: List<String>,
    leftMargin: Int,
    pageHeight: Int,
    topMargin: Int,
    lineHeight: Int,
    titleLineHeight: Int,
    isFirstPage: Boolean,
): String {
    val contentLines = mutableListOf<String>()
    contentLines += "BT"

    if (isFirstPage && lines.isNotEmpty()) {
        contentLines += "/F1 18 Tf"
        contentLines += "$leftMargin ${pageHeight - topMargin} Td"
        contentLines += "(${escapePdfText(lines.first())}) Tj"
        if (lines.size > 1) {
            contentLines += "0 -$titleLineHeight Td"
            contentLines += "/F1 12 Tf"
            lines.drop(1).forEachIndexed { index, rawLine ->
                if (index > 0) {
                    contentLines += "0 -$lineHeight Td"
                }
                rawLine.takeIf { it.isNotBlank() }?.let {
                    contentLines += "(${escapePdfText(it)}) Tj"
                }
            }
        }
    } else {
        contentLines += "/F1 12 Tf"
        contentLines += "$leftMargin ${pageHeight - topMargin} Td"
        lines.forEachIndexed { index, rawLine ->
            if (index > 0) {
                contentLines += "0 -$lineHeight Td"
            }
            rawLine.takeIf { it.isNotBlank() }?.let {
                contentLines += "(${escapePdfText(it)}) Tj"
            }
        }
    }

    contentLines += "ET"
    return contentLines.joinToString("\n")
}

private fun wrapPdfLine(value: String, maxChars: Int): List<String> {
    if (value.length <= maxChars) return listOf(value)
    if (value.isBlank()) return listOf("")

    val lines = mutableListOf<String>()
    var current = ""

    value.split(Regex("\\s+")).forEach { word ->
        val candidate = if (current.isBlank()) word else "$current $word"
        if (candidate.length <= maxChars) {
            current = candidate
        } else {
            if (current.isNotBlank()) {
                lines += current
            }
            current = word
        }
    }

    if (current.isNotBlank()) {
        lines += current
    }

    return lines.ifEmpty { listOf(value) }
}

private fun escapePdfText(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("(", "\\(")
        .replace(")", "\\)")
        .replace("\r", " ")
        .replace("\n", " ")
}
