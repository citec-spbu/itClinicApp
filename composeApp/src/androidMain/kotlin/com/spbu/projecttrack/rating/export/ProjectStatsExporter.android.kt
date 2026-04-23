package com.spbu.projecttrack.rating.export

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberProjectStatsExporter(): ProjectStatsExporter {
    val context = LocalContext.current
    return remember(context) {
        AndroidProjectStatsExporter(context.applicationContext)
    }
}

private class AndroidProjectStatsExporter(
    private val context: Context
) : ProjectStatsExporter {
    private enum class PdfTextStyle {
        Title,
        Heading,
        Body,
        Muted,
    }

    private data class PdfParagraph(
        val text: String,
        val style: PdfTextStyle,
        val indent: Float = 0f,
        val spacingAfter: Float = 0f,
    )

    override suspend fun exportPdf(
        payload: ProjectStatsExportPayload
    ): Result<ProjectStatsExportResult> = runCatching {
        val file = createOutputFile(payload, "pdf")
        writePdf(file, payload)
        tryShare(file, "application/pdf")
        ProjectStatsExportResult(
            fileName = file.name,
            absolutePath = file.absolutePath,
            mimeType = "application/pdf"
        )
    }

    override suspend fun exportExcelCsv(
        payload: ProjectStatsExportPayload
    ): Result<ProjectStatsExportResult> = runCatching {
        val file = createOutputFile(payload, "csv")
        // UTF-8 BOM so Excel opens without an encoding dialog
        file.writeText("\uFEFF" + buildProjectStatsCsv(payload))
        tryShare(file, "text/csv")
        ProjectStatsExportResult(
            fileName = file.name,
            absolutePath = file.absolutePath,
            mimeType = "text/csv"
        )
    }

    private fun createOutputFile(payload: ProjectStatsExportPayload, extension: String): File {
        val baseName = sanitizeProjectStatsFileName(payload.projectName)
        val dir = File(context.cacheDir, "project_stats").apply { mkdirs() }
        return File(dir, "$baseName.$extension")
    }

    private fun writePdf(file: File, payload: ProjectStatsExportPayload) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val leftMargin = 32f
        val rightMargin = 32f
        val topMargin = 40f
        val bottomMargin = 40f

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(159, 45, 32)
            textSize = 24f
            isFakeBoldText = true
        }
        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(159, 45, 32)
            textSize = 16f
            isFakeBoldText = true
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(48, 48, 52)
            textSize = 11f
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(118, 118, 124)
            textSize = 10f
        }

        val paragraphs = buildPdfParagraphs(payload)

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = topMargin

        fun paintFor(style: PdfTextStyle): Paint = when (style) {
            PdfTextStyle.Title -> titlePaint
            PdfTextStyle.Heading -> headingPaint
            PdfTextStyle.Body -> bodyPaint
            PdfTextStyle.Muted -> mutedPaint
        }

        fun lineHeightFor(style: PdfTextStyle): Float = when (style) {
            PdfTextStyle.Title -> 30f
            PdfTextStyle.Heading -> 22f
            PdfTextStyle.Body -> 17f
            PdfTextStyle.Muted -> 15f
        }

        fun startNewPage() {
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = topMargin
        }

        fun ensureSpace(requiredHeight: Float) {
            if (y + requiredHeight > pageHeight - bottomMargin) {
                startNewPage()
            }
        }

        paragraphs.forEach { paragraph ->
            val paint = paintFor(paragraph.style)
            val lineHeight = lineHeightFor(paragraph.style)
            val wrappedLines = wrapPdfText(
                text = paragraph.text,
                paint = paint,
                maxWidth = pageWidth - leftMargin - rightMargin - paragraph.indent,
            )

            wrappedLines.forEach { line ->
                ensureSpace(lineHeight)
                canvas.drawText(line, leftMargin + paragraph.indent, y, paint)
                y += lineHeight
            }

            y += paragraph.spacingAfter
        }

        document.finishPage(page)
        file.outputStream().use { output ->
            document.writeTo(output)
        }
        document.close()
    }

    private fun buildPdfParagraphs(payload: ProjectStatsExportPayload): List<PdfParagraph> {
        val paragraphs = mutableListOf<PdfParagraph>()
        paragraphs += PdfParagraph(payload.projectName, PdfTextStyle.Title, spacingAfter = 8f)

        payload.periodLabel?.takeIf { it.isNotBlank() }?.let {
            paragraphs += PdfParagraph("Период: $it", PdfTextStyle.Body)
        }
        payload.customerName?.takeIf { it.isNotBlank() }?.let {
            paragraphs += PdfParagraph("Заказчик: $it", PdfTextStyle.Body)
        }
        payload.repositoryUrl?.takeIf { it.isNotBlank() }?.let {
            paragraphs += PdfParagraph("Репозиторий: $it", PdfTextStyle.Body)
        }
        payload.description?.takeIf { it.isNotBlank() }?.let {
            paragraphs += PdfParagraph("Описание: $it", PdfTextStyle.Muted, spacingAfter = 4f)
        }

        if (payload.summaryCards.isNotEmpty()) {
            paragraphs += PdfParagraph("Сводка", PdfTextStyle.Heading, spacingAfter = 2f)
            payload.summaryCards.forEach { card ->
                val text = buildString {
                    append(card.title)
                    append(": ")
                    append(card.value)
                    card.subtitle?.takeIf { it.isNotBlank() }?.let {
                        append(" — ")
                        append(it)
                    }
                }
                paragraphs += PdfParagraph(text, PdfTextStyle.Body, indent = 10f)
            }
        }

        if (payload.members.isNotEmpty()) {
            paragraphs += PdfParagraph("Команда", PdfTextStyle.Heading, spacingAfter = 2f)
            payload.members.forEach { member ->
                val text = buildString {
                    append(member.name)
                    member.role?.takeIf { it.isNotBlank() }?.let { append(" — ").append(it) }
                    member.value?.takeIf { it.isNotBlank() }?.let { append(" — ").append(it) }
                    member.marker?.takeIf { it.isNotBlank() }?.let { append(" — ").append(it) }
                }
                paragraphs += PdfParagraph(text, PdfTextStyle.Body, indent = 10f)
            }
        }

        payload.sections.forEach { section ->
            paragraphs += PdfParagraph(section.title, PdfTextStyle.Heading, spacingAfter = 2f)
            section.subtitle?.takeIf { it.isNotBlank() }?.let {
                paragraphs += PdfParagraph(it, PdfTextStyle.Muted)
            }
            section.rows.forEach { row ->
                val text = buildString {
                    append(row.label)
                    append(": ")
                    append(row.value)
                    row.note?.takeIf { it.isNotBlank() }?.let {
                        append(" — ")
                        append(it)
                    }
                }
                paragraphs += PdfParagraph(text, PdfTextStyle.Body, indent = 10f)
            }
            when (val chart = section.chart) {
                is ProjectStatsChart.Bar -> {
                    paragraphs += PdfParagraph("График: ${chart.title}", PdfTextStyle.Muted)
                    chart.points.forEach { point ->
                        paragraphs += PdfParagraph(
                            text = buildString {
                                append(point.label)
                                append(": ")
                                append(point.value)
                                point.note?.takeIf { it.isNotBlank() }?.let {
                                    append(" — ")
                                    append(it)
                                }
                            },
                            style = PdfTextStyle.Body,
                            indent = 18f,
                        )
                    }
                }
                is ProjectStatsChart.Line -> {
                    paragraphs += PdfParagraph("График: ${chart.title}", PdfTextStyle.Muted)
                    chart.points.forEach { point ->
                        paragraphs += PdfParagraph(
                            text = buildString {
                                append(point.label)
                                append(": ")
                                append(point.value)
                                point.note?.takeIf { it.isNotBlank() }?.let {
                                    append(" — ")
                                    append(it)
                                }
                            },
                            style = PdfTextStyle.Body,
                            indent = 18f,
                        )
                    }
                }
                is ProjectStatsChart.Donut -> {
                    paragraphs += PdfParagraph("График: ${chart.title}", PdfTextStyle.Muted)
                    chart.segments.forEach { segment ->
                        paragraphs += PdfParagraph(
                            text = buildString {
                                append(segment.label)
                                append(": ")
                                append(segment.value)
                                segment.colorHint?.takeIf { it.isNotBlank() }?.let {
                                    append(" — ")
                                    append(it)
                                }
                            },
                            style = PdfTextStyle.Body,
                            indent = 18f,
                        )
                    }
                }
                null -> Unit
            }
            section.notes.forEach { note ->
                paragraphs += PdfParagraph(note, PdfTextStyle.Muted, indent = 10f)
            }
        }

        return paragraphs
    }

    private fun wrapPdfText(
        text: String,
        paint: Paint,
        maxWidth: Float,
    ): List<String> {
        if (text.isBlank()) return listOf("")

        val lines = mutableListOf<String>()
        val words = text.split(Regex("\\s+"))
        var currentLine = ""

        words.forEach { word ->
            val candidate = if (currentLine.isBlank()) word else "$currentLine $word"
            if (paint.measureText(candidate) <= maxWidth) {
                currentLine = candidate
            } else {
                if (currentLine.isNotBlank()) {
                    lines += currentLine
                }
                currentLine = word
            }
        }

        if (currentLine.isNotBlank()) {
            lines += currentLine
        }

        return lines.ifEmpty { listOf(text) }
    }

    private fun tryShare(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        runCatching {
            val chooser = Intent.createChooser(intent, file.name)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }.onFailure {
            if (it is ActivityNotFoundException) return
        }
    }
}
