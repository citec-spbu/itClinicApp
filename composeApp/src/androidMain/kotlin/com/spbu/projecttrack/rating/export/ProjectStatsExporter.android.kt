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
        file.writeText(buildProjectStatsCsv(payload))
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
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        drawPdfContent(canvas, payload)
        document.finishPage(page)
        file.outputStream().use { output ->
            document.writeTo(output)
        }
        document.close()
    }

    private fun drawPdfContent(canvas: Canvas, payload: ProjectStatsExportPayload) {
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
            color = AndroidColor.DKGRAY
            textSize = 11f
        }
        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.GRAY
            textSize = 9f
        }

        var y = 48f
        canvas.drawText(payload.projectName, 32f, y, titlePaint)
        y += 28f

        listOfNotNull(
            payload.description?.takeIf { it.isNotBlank() }?.let { "Описание: $it" },
            payload.customerName?.takeIf { it.isNotBlank() }?.let { "Заказчик: $it" },
            payload.repositoryUrl?.takeIf { it.isNotBlank() }?.let { "Репозиторий: $it" },
            payload.periodLabel?.takeIf { it.isNotBlank() }?.let { "Период: $it" },
            payload.generatedAtLabel?.takeIf { it.isNotBlank() }?.let { "Сформировано: $it" }
        ).forEach { line ->
            canvas.drawText(line, 32f, y, bodyPaint)
            y += 18f
        }

        fun drawHeading(text: String) {
            y += 10f
            canvas.drawText(text, 32f, y, headingPaint)
            y += 18f
        }

        if (payload.summaryCards.isNotEmpty()) {
            drawHeading("Ключевые показатели")
            payload.summaryCards.forEach { card ->
                canvas.drawText("${card.title}: ${card.value}", 32f, y, bodyPaint)
                card.subtitle?.takeIf { it.isNotBlank() }?.let {
                    canvas.drawText(it, 300f, y, smallPaint)
                }
                y += 16f
            }
        }

        if (payload.members.isNotEmpty()) {
            drawHeading("Команда")
            payload.members.forEach { member ->
                val line = buildString {
                    append(member.name)
                    member.role?.takeIf { it.isNotBlank() }?.let { append(" - ").append(it) }
                    member.value?.takeIf { it.isNotBlank() }?.let { append(" - ").append(it) }
                    member.marker?.takeIf { it.isNotBlank() }?.let { append(" - ").append(it) }
                }
                canvas.drawText(line, 32f, y, bodyPaint)
                y += 16f
            }
        }

        payload.sections.forEach { section ->
            drawHeading(section.title)
            section.subtitle?.takeIf { it.isNotBlank() }?.let {
                canvas.drawText(it, 32f, y, smallPaint)
                y += 14f
            }
            section.rows.forEach { row ->
                canvas.drawText("${row.label}: ${row.value}", 32f, y, bodyPaint)
                row.note?.takeIf { it.isNotBlank() }?.let { note ->
                    canvas.drawText(note, 300f, y, smallPaint)
                }
                y += 15f
            }
            section.chart?.let { chart ->
                canvas.drawText("График: ${chart.title}", 32f, y, smallPaint)
                y += 14f
                when (chart) {
                    is ProjectStatsChart.Bar -> chart.points.forEach { point ->
                        canvas.drawText("${point.label}: ${point.value}", 40f, y, bodyPaint)
                        y += 14f
                    }
                    is ProjectStatsChart.Line -> chart.points.forEach { point ->
                        canvas.drawText("${point.label}: ${point.value}", 40f, y, bodyPaint)
                        y += 14f
                    }
                    is ProjectStatsChart.Donut -> chart.segments.forEach { segment ->
                        canvas.drawText("${segment.label}: ${segment.value}", 40f, y, bodyPaint)
                        y += 14f
                    }
                }
            }
            section.notes.forEach { note ->
                canvas.drawText(note, 32f, y, smallPaint)
                y += 14f
            }
        }
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
