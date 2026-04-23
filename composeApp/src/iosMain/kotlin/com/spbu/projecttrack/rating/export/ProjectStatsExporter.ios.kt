package com.spbu.projecttrack.rating.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite

@Composable
actual fun rememberProjectStatsExporter(): ProjectStatsExporter {
    return remember { IosProjectStatsExporter() }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class IosProjectStatsExporter : ProjectStatsExporter {

    private enum class ParaStyle { Title, Heading, Body, Muted }

    private data class Para(
        val text: String,
        val style: ParaStyle,
        val spacingAfter: Double = 4.0,
        val indent: Double = 0.0,
    )

    // ─── Entry points ─────────────────────────────────────────────────────────

    override suspend fun exportPdf(
        payload: ProjectStatsExportPayload
    ): Result<ProjectStatsExportResult> = runCatching {
        val fileName = "${sanitizeProjectStatsFileName(payload.projectName)}.pdf"
        val path = NSTemporaryDirectory() + fileName
        renderPdf(payload).writeToFile(path = path, atomically = true)
        share(path)
        ProjectStatsExportResult(fileName = fileName, absolutePath = path, mimeType = "application/pdf")
    }

    override suspend fun exportExcelCsv(
        payload: ProjectStatsExportPayload
    ): Result<ProjectStatsExportResult> = runCatching {
        val fileName = "${sanitizeProjectStatsFileName(payload.projectName)}.csv"
        val path = NSTemporaryDirectory() + fileName
        // UTF-8 BOM ensures Excel opens without an encoding dialog
        writeTextFile(path, "\uFEFF" + buildProjectStatsCsv(payload))
        share(path)
        ProjectStatsExportResult(fileName = fileName, absolutePath = path, mimeType = "text/csv")
    }

    // ─── PDF (UIKit – full Unicode / Cyrillic support) ────────────────────────

    private fun renderPdf(payload: ProjectStatsExportPayload): NSMutableData {
        val pw = 595.0; val ph = 842.0
        val ml = 48.0; val mt = 60.0; val mb = 56.0
        val cw = pw - ml * 2

        val pdfData = NSMutableData()
        val pageRect = CGRectMake(0.0, 0.0, pw, ph)

        UIGraphicsBeginPDFContextToData(pdfData, pageRect, null)
        UIGraphicsBeginPDFPageWithInfo(pageRect, null)

        var y = mt

        buildParas(payload).forEach { para ->
            val attrStr = makeAttrStr(para)
            val textH = attrStr.boundingRectWithSize(
                CGSizeMake(cw - para.indent, 8_000.0),
                NSStringDrawingUsesLineFragmentOrigin,
                null
            ).useContents { size.height }

            if (y + textH + para.spacingAfter > ph - mb) {
                UIGraphicsBeginPDFPageWithInfo(pageRect, null)
                y = mt
            }

            attrStr.drawWithRect(
                CGRectMake(ml + para.indent, y, cw - para.indent, textH + 2.0),
                NSStringDrawingUsesLineFragmentOrigin,
                null
            )
            y += textH + para.spacingAfter
        }

        UIGraphicsEndPDFContext()
        return pdfData
    }

    private fun makeAttrStr(para: Para): NSAttributedString {
        val fontSize: Double
        val bold: Boolean
        val r: Double; val g: Double; val b: Double
        when (para.style) {
            ParaStyle.Title   -> { fontSize = 22.0; bold = true;  r = 0.624; g = 0.176; b = 0.125 }
            ParaStyle.Heading -> { fontSize = 13.0; bold = true;  r = 0.624; g = 0.176; b = 0.125 }
            ParaStyle.Body    -> { fontSize = 11.0; bold = false; r = 0.188; g = 0.188; b = 0.204 }
            ParaStyle.Muted   -> { fontSize = 10.0; bold = false; r = 0.463; g = 0.463; b = 0.486 }
        }
        val font: UIFont = if (bold) UIFont.boldSystemFontOfSize(fontSize) else UIFont.systemFontOfSize(fontSize)
        val color: UIColor = UIColor.colorWithRed(r, green = g, blue = b, alpha = 1.0)!!

        return NSAttributedString.create(
            string = para.text,
            attributes = mapOf(
                NSFontAttributeName to font,
                NSForegroundColorAttributeName to color,
            )
        )
    }

    // ─── Paragraph model ─────────────────────────────────────────────────────

    private fun buildParas(payload: ProjectStatsExportPayload): List<Para> {
        val list = mutableListOf<Para>()

        list += Para(payload.projectName, ParaStyle.Title, spacingAfter = 8.0)

        val metaParts = buildList {
            payload.periodLabel?.takeIf { it.isNotBlank() }?.let { add("Период: $it") }
            payload.customerName?.takeIf { it.isNotBlank() }?.let { add("Заказчик: $it") }
            payload.generatedAtLabel?.takeIf { it.isNotBlank() }?.let { add("Сформировано: $it") }
        }
        if (metaParts.isNotEmpty()) {
            list += Para(metaParts.joinToString("   ·   "), ParaStyle.Muted, spacingAfter = 3.0)
        }
        payload.repositoryUrl?.takeIf { it.isNotBlank() }?.let {
            list += Para("Репозиторий: $it", ParaStyle.Muted, spacingAfter = 3.0)
        }
        payload.description?.takeIf { it.isNotBlank() }?.let {
            list += Para(it, ParaStyle.Muted, spacingAfter = 6.0)
        }

        list += Para("─".repeat(68), ParaStyle.Muted, spacingAfter = 12.0)

        if (payload.summaryCards.isNotEmpty()) {
            list += Para("Ключевые показатели", ParaStyle.Heading, spacingAfter = 5.0)
            payload.summaryCards.forEach { c ->
                list += Para(
                    buildString {
                        append(c.title).append(": ").append(c.value)
                        c.subtitle?.takeIf { it.isNotBlank() }?.let { append("  –  $it") }
                    },
                    ParaStyle.Body, spacingAfter = 3.0, indent = 14.0
                )
            }
            list += Para("", ParaStyle.Body, spacingAfter = 10.0)
        }

        if (payload.members.isNotEmpty()) {
            list += Para("Команда", ParaStyle.Heading, spacingAfter = 5.0)
            payload.members.forEach { m ->
                list += Para(
                    buildString {
                        append(m.name)
                        m.role?.takeIf { it.isNotBlank() }?.let { append("  –  $it") }
                        m.value?.takeIf { it.isNotBlank() }?.let { append(":  $it") }
                        m.marker?.takeIf { it.isNotBlank() }?.let { append("  [$it]") }
                    },
                    ParaStyle.Body, spacingAfter = 3.0, indent = 14.0
                )
            }
            list += Para("", ParaStyle.Body, spacingAfter = 10.0)
        }

        payload.sections.forEach { section ->
            list += Para(section.title, ParaStyle.Heading, spacingAfter = 4.0)
            section.subtitle?.takeIf { it.isNotBlank() }?.let {
                list += Para(it, ParaStyle.Muted, spacingAfter = 4.0)
            }
            section.rows.forEach { row ->
                list += Para(
                    buildString {
                        append(row.label).append(": ").append(row.value)
                        row.note?.takeIf { it.isNotBlank() }?.let { append("  –  $it") }
                    },
                    ParaStyle.Body, spacingAfter = 3.0, indent = 14.0
                )
            }
            section.chart?.let { chart ->
                list += Para("График: ${chart.title}", ParaStyle.Muted, spacingAfter = 3.0, indent = 8.0)
                when (chart) {
                    is ProjectStatsChart.Bar -> chart.points.forEach { p ->
                        list += Para(
                            "${p.label}: ${p.value}${p.note?.let { "  –  $it" } ?: ""}",
                            ParaStyle.Body, spacingAfter = 2.0, indent = 22.0
                        )
                    }
                    is ProjectStatsChart.Line -> chart.points.forEach { p ->
                        list += Para(
                            "${p.label}: ${p.value}${p.note?.let { "  –  $it" } ?: ""}",
                            ParaStyle.Body, spacingAfter = 2.0, indent = 22.0
                        )
                    }
                    is ProjectStatsChart.Donut -> chart.segments.forEach { s ->
                        list += Para(
                            "${s.label}: ${s.value}${s.colorHint?.let { "  –  $it" } ?: ""}",
                            ParaStyle.Body, spacingAfter = 2.0, indent = 22.0
                        )
                    }
                }
            }
            section.notes.forEach { note ->
                list += Para("• $note", ParaStyle.Muted, spacingAfter = 2.0, indent = 14.0)
            }
            list += Para("", ParaStyle.Body, spacingAfter = 10.0)
        }

        return list
    }

    // ─── I/O ─────────────────────────────────────────────────────────────────

    private fun writeTextFile(path: String, content: String) {
        val bytes = content.encodeToByteArray()
        val file = fopen(path, "wb") ?: error("Cannot open for writing: $path")
        try {
            bytes.usePinned { pinned ->
                fwrite(pinned.addressOf(0), 1.convert(), bytes.size.convert(), file)
            }
        } finally {
            fclose(file)
        }
    }

    private fun share(path: String) {
        val url = NSURL.fileURLWithPath(path)
        val vc = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
        UIApplication.sharedApplication.keyWindow?.rootViewController
            ?.presentViewController(vc, animated = true, completion = null)
    }
}
