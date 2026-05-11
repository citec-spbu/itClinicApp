@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.spbu.projecttrack.rating.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.spbu.projecttrack.rating.common.StatsExportCopy
import kotlinx.cinterop.CValue
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import projecttrack.composeapp.generated.resources.Res
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.max

@Composable
actual fun rememberProjectStatsExporter(): ProjectStatsExporter {
    return remember { IosProjectStatsExporter() }
}

private class IosProjectStatsExporter : ProjectStatsExporter {

    override suspend fun exportPdf(
        payload: ProjectStatsExportPayload,
    ): Result<ProjectStatsExportResult> = runCatching {
        val fileName = "${sanitizeProjectStatsFileName(payload.projectName)}.pdf"
        val path = NSTemporaryDirectory() + fileName
        val watermarkImage = runCatching {
            Res.readBytes("drawable/spbu_pdf_logo.png")
                .toNSData()
                .let(UIImage::imageWithData)
        }.getOrNull()
        val pdfData = withContext(Dispatchers.Default) { StyledPdfRenderer(watermarkImage).render(payload) }
        pdfData.writeToFile(path = path, atomically = true)
        withContext(Dispatchers.Main) { share(path) }
        ProjectStatsExportResult(
            fileName = fileName,
            absolutePath = path,
            mimeType = "application/pdf",
        )
    }

    override suspend fun exportExcelCsv(
        payload: ProjectStatsExportPayload,
    ): Result<ProjectStatsExportResult> = runCatching {
        val fileName = "${sanitizeProjectStatsFileName(payload.projectName)}.xlsx"
        val path = NSTemporaryDirectory() + fileName
        withContext(Dispatchers.Default) {
            writeBinaryFile(path, buildProjectStatsXlsx(payload))
        }
        withContext(Dispatchers.Main) { share(path) }
        ProjectStatsExportResult(
            fileName = fileName,
            absolutePath = path,
            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        )
    }

    private fun share(path: String) {
        val url = NSURL.fileURLWithPath(path)
        val activityVc = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
        val rootVc = UIApplication.sharedApplication.keyWindow?.rootViewController
        var topVc: UIViewController? = rootVc
        while (topVc?.presentedViewController != null) {
            topVc = topVc!!.presentedViewController
        }
        if (topVc == null) return
        val popover = activityVc.popoverPresentationController
        popover?.sourceView = topVc.view
        popover?.sourceRect = CGRectMake(
            topVc.view.bounds.useContents { size.width / 2.0 },
            topVc.view.bounds.useContents { size.height / 2.0 },
            0.0,
            0.0,
        )
        topVc.presentViewController(activityVc, animated = true, completion = null)
    }

    private fun writeBinaryFile(path: String, bytes: ByteArray) {
        val file = fopen(path, "wb") ?: error("Cannot open for writing: $path")
        try {
            bytes.usePinned { pinned ->
                fwrite(pinned.addressOf(0), 1uL, bytes.size.toULong(), file)
            }
        } finally {
            fclose(file)
        }
    }
}

private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
}

private class StyledPdfRenderer(
    private val watermarkImage: UIImage?,
) {
    private data class PdfPageSpec(
        val width: Double = 595.0,
        val height: Double = 842.0,
        val leftMargin: Double = 34.0,
        val rightMargin: Double = 34.0,
        val topMargin: Double = 88.0,
        val bottomMargin: Double = 48.0,
    )

    private data class TableRenderConfig(
        val x: Double,
        val width: Double,
        val columnWidths: List<Double>,
        val cellFont: UIFont,
        val cellPadding: Double,
        val cellLineHeight: Double,
        val headerLineHeight: Double,
        val minRowHeight: Double,
        val minHeaderHeight: Double,
    )

    private val defaultPageSpec = PdfPageSpec()
    private var currentPageSpec = defaultPageSpec

    private val pageWidth: Double get() = currentPageSpec.width
    private val pageHeight: Double get() = currentPageSpec.height
    private val leftMargin: Double get() = currentPageSpec.leftMargin
    private val rightMargin: Double get() = currentPageSpec.rightMargin
    private val topMargin: Double get() = currentPageSpec.topMargin
    private val bottomMargin: Double get() = currentPageSpec.bottomMargin
    private val contentWidth: Double get() = pageWidth - leftMargin - rightMargin

    private val pageNoFont = UIFont.boldSystemFontOfSize(11.0)
    private val tagFont = UIFont.boldSystemFontOfSize(10.0)
    private val titleFont = UIFont.boldSystemFontOfSize(34.0)
    private val sectionFont = UIFont.boldSystemFontOfSize(24.0)
    private val subtitleFont = UIFont.systemFontOfSize(11.0)
    private val bodyFont = UIFont.systemFontOfSize(11.0)
    private val smallFont = UIFont.systemFontOfSize(9.5)
    private val headerFont = UIFont.boldSystemFontOfSize(10.0)
    private val cardTitleFont = UIFont.boldSystemFontOfSize(9.0)
    private val cardValueFont = UIFont.boldSystemFontOfSize(22.0)
    private val scoreLabelFont = UIFont.boldSystemFontOfSize(9.0)
    private val scoreValueFont = UIFont.boldSystemFontOfSize(18.0)
    private val chartLabelFont = UIFont.systemFontOfSize(9.5)
    private val chartMutedFont = UIFont.systemFontOfSize(8.5)

    private var pdfData = NSMutableData()
    private var y = topMargin
    private var pageNumber = 0
    private var currentTag = "OVERVIEW"

    fun render(payload: ProjectStatsExportPayload): NSMutableData {
        val pageRect = CGRectMake(0.0, 0.0, pageWidth, pageHeight)
        pdfData = NSMutableData()
        UIGraphicsBeginPDFContextToData(pdfData, pageRect, null)
        startPage("OVERVIEW")
        drawCover(payload)
        payload.sections.forEach { drawSection(it) }
        UIGraphicsEndPDFContext()
        return pdfData
    }

    private fun startPage(tag: String, pageSpec: PdfPageSpec = currentPageSpec) {
        currentPageSpec = pageSpec
        currentTag = tag
        pageNumber += 1
        UIGraphicsBeginPDFPageWithInfo(CGRectMake(0.0, 0.0, pageWidth, pageHeight), null)
        y = topMargin
        decoratePage(tag)
    }

    private fun decoratePage(tag: String) {
        val ctx = UIGraphicsGetCurrentContext() ?: return
        fillRect(ctx, CGRectMake(0.0, 0.0, pageWidth, pageHeight), ProjectStatsPdfTheme.paper)
        drawWatermark(ctx)
        fillRect(ctx, CGRectMake(0.0, 0.0, 18.0, pageHeight), ProjectStatsPdfTheme.rail)
        drawText(pageNumber.toString().padStart(2, '0'), pageNoFont, ProjectStatsPdfTheme.rail, 28.0, 42.0)
        drawText(tag, tagFont, ProjectStatsPdfTheme.muted, 84.0, 42.0)
        strokeLine(ctx, 84.0, 62.0, pageWidth - rightMargin, 62.0, ProjectStatsPdfTheme.border, 1.0)
    }

    private fun drawCover(payload: ProjectStatsExportPayload) {
        ensureSpace(160.0, "OVERVIEW")
        drawParagraph(payload.projectName, titleFont, ProjectStatsPdfTheme.rail, 42.0, leftMargin + 48.0, contentWidth - 48.0)
        y += 8.0
        payload.description?.takeIf { it.isNotBlank() }?.let {
            drawParagraph(it, subtitleFont, ProjectStatsPdfTheme.muted, 15.0, leftMargin + 48.0, contentWidth - 48.0)
            y += 10.0
        }

        val meta = buildList {
            payload.periodLabel?.takeIf { it.isNotBlank() }?.let { add("Период: $it") }
            payload.customerName?.takeIf { it.isNotBlank() }?.let { add("Заказчик: $it") }
            payload.repositoryUrl?.takeIf { it.isNotBlank() }?.let { add("Репозиторий: $it") }
        }.joinToString("  •  ")
        if (meta.isNotBlank()) {
            drawParagraph(meta, bodyFont, ProjectStatsPdfTheme.ink, 16.0, leftMargin + 48.0, contentWidth - 48.0)
            y += 4.0
        }
        payload.generatedAtLabel?.takeIf { it.isNotBlank() }?.let {
            drawParagraph("Сформировано: $it", subtitleFont, ProjectStatsPdfTheme.muted, 14.0, leftMargin + 48.0, contentWidth - 48.0)
        }
        y += 12.0
        val ctx = UIGraphicsGetCurrentContext() ?: return
        strokeLine(ctx, leftMargin + 48.0, y, pageWidth - rightMargin, y, ProjectStatsPdfTheme.border, 1.0)
        y += 28.0

        if (payload.summaryCards.isNotEmpty()) {
            drawSectionTitle("Ключевые показатели")
            y += 8.0
            drawSummaryCards(payload.summaryCards)
            y += 22.0
        }

        if (payload.members.isNotEmpty()) {
            drawSectionTitle("Команда")
            y += 10.0
            drawTable(
                headers = listOf("Участник", "Роль"),
                rows = payload.members.map { listOf(it.name, it.role.orEmpty()) },
                columnFractions = listOf(0.5, 0.5),
                title = null,
                pageTag = "OVERVIEW",
            )
            y += 14.0
        }
    }

    private fun drawSection(section: ProjectStatsSection) {
        currentTag = pdfSectionTag(section.title)
        startPage(currentTag, pageSpecForSection(section))
        drawSectionHeader(section)

        val metricRowsAsCards = section.rows.isNotEmpty() && section.rows.size <= 4
        if (metricRowsAsCards) {
            drawMetricCards(section.rows)
            y += 18.0
        } else if (section.rows.isNotEmpty()) {
            drawKeyValueTable(section.rows, currentTag)
            y += 16.0
        }

        if (section.chartFirst) {
            section.chart?.let { drawChart(it, currentTag) }
            section.table?.let { table ->
                drawTable(
                    headers = table.headers,
                    rows = table.rows,
                    columnFractions = table.columnFractions?.map { it.toDouble() } ?: List(table.headers.size) { 1.0 / table.headers.size },
                    title = table.title,
                    pageTag = currentTag,
                    compact = table.headers.size >= 6,
                    pdfStyle = table.pdfStyle,
                )
                y += 16.0
            }
        } else {
            val tableCompact = section.table?.headers?.size?.let { it >= 6 } ?: false
            section.table?.let { table ->
                drawTable(
                    headers = table.headers,
                    rows = table.rows,
                    columnFractions = table.columnFractions?.map { it.toDouble() } ?: List(table.headers.size) { 1.0 / table.headers.size },
                    title = table.title,
                    pageTag = currentTag,
                    compact = tableCompact,
                    pdfStyle = table.pdfStyle,
                )
                y += 16.0
            }
            section.chart?.let { drawChart(it, currentTag, indented = !tableCompact) }
        }

        section.notes.forEach { note ->
            ensureSpace(24.0, currentTag)
            drawParagraph(note, smallFont, ProjectStatsPdfTheme.muted, 14.0, leftMargin + 48.0, contentWidth - 48.0)
            y += 6.0
        }
        y += 10.0
    }

    private fun drawSectionTitle(title: String) {
        drawParagraph(title, sectionFont, ProjectStatsPdfTheme.ink, 28.0, leftMargin + 48.0, contentWidth - 48.0)
        y += 6.0
    }

    private fun drawSectionHeader(section: ProjectStatsSection) {
        val headerX = leftMargin + 48.0
        val scoreWidth = if (section.score != null) 96.0 else 0.0
        val textWidth = contentWidth - 48.0 - if (section.score != null) scoreWidth + 12.0 else 0.0
        val titleHeight = measureParagraphHeight(section.title, sectionFont, textWidth)
        val subtitleHeight = section.subtitle?.takeIf { it.isNotBlank() }?.let {
            measureParagraphHeight(it, subtitleFont, textWidth) + 6.0
        } ?: 0.0
        val textPaddingTop = 24.0
        val blockHeight = max(titleHeight + subtitleHeight + textPaddingTop + 8.0, if (section.score != null) 76.0 else 0.0)
        ensureSpace(blockHeight + 12.0, currentTag)

        val originY = y
        y = originY + textPaddingTop
        drawParagraph(section.title, sectionFont, ProjectStatsPdfTheme.ink, 28.0, headerX, textWidth)
        val titleBottom = y
        section.subtitle?.takeIf { it.isNotBlank() }?.let {
            y = titleBottom + 8.0
            drawParagraph(it, subtitleFont, ProjectStatsPdfTheme.muted, 15.0, headerX, textWidth)
        }
        val sectionBottom = max(y, titleBottom)
        section.score?.let { score ->
            drawScoreBadge(score, pageWidth - rightMargin - scoreWidth, originY + 10.0, scoreWidth, 58.0)
        }
        y = max(originY + blockHeight, sectionBottom) + 16.0
    }

    private fun drawSummaryCards(cards: List<ProjectStatsSummaryCard>) {
        val displayCards = cards.take(4)
        if (displayCards.isEmpty()) return
        val gap = 12.0
        val startX = leftMargin + 48.0
        val availableWidth = contentWidth - 48.0
        val cardWidth = (availableWidth - gap * (displayCards.size - 1)) / displayCards.size
        val cardHeight = 94.0
        ensureSpace(cardHeight + 8.0, "OVERVIEW")
        val cardTop = y
        displayCards.forEachIndexed { index, card ->
            val x = startX + index * (cardWidth + gap)
            val valueColor = summaryCardColor(card.title)
            drawMetricCard(x, cardTop, cardWidth, cardHeight, card.title, card.value, card.subtitle, valueColor)
        }
        y = cardTop + cardHeight
    }

    private fun drawMetricCards(rows: List<ProjectStatsTableRow>) {
        val gap = 12.0
        val startX = leftMargin + 48.0
        val availableWidth = contentWidth - 48.0
        val cardWidth = (availableWidth - gap * (rows.size - 1)) / rows.size
        val cardHeight = 84.0
        ensureSpace(cardHeight + 8.0, currentTag)
        val cardTop = y
        rows.forEachIndexed { index, row ->
            val x = startX + index * (cardWidth + gap)
            val toneColor = when {
                row.label.contains("быстр", ignoreCase = true) -> ProjectStatsPdfTheme.high
                row.label.contains("доля", ignoreCase = true) || row.label.contains("issue", ignoreCase = true) -> ProjectStatsPdfTheme.mid
                else -> ProjectStatsPdfTheme.rail
            }
            drawMetricCard(x, cardTop, cardWidth, cardHeight, row.label, row.value, row.note, toneColor)
        }
        y = cardTop + cardHeight
    }

    private fun drawMetricCard(
        x: Double,
        top: Double,
        width: Double,
        height: Double,
        title: String,
        value: String,
        subtitle: String?,
        valueColor: PdfRgb,
    ) {
        val ctx = UIGraphicsGetCurrentContext() ?: return
        fillRoundedRect(ctx, CGRectMake(x, top, width, height), ProjectStatsPdfTheme.cardFill, 10.0)
        strokeRoundedRect(ctx, CGRectMake(x, top, width, height), ProjectStatsPdfTheme.border, 10.0)
        y = top + 16.0
        drawParagraph(title.uppercase(), cardTitleFont, ProjectStatsPdfTheme.muted, 11.0, x + 14.0, width - 28.0, false)
        val oldY = y
        y = top + 44.0
        drawParagraph(value, cardValueFont, valueColor, 24.0, x + 14.0, width - 28.0, false)
        subtitle?.takeIf { it.isNotBlank() }?.let {
            y = top + 68.0
            drawParagraph(it, smallFont, ProjectStatsPdfTheme.muted, 11.0, x + 14.0, width - 28.0, false)
        }
        y = oldY
    }

    private fun drawScoreBadge(score: Double, x: Double, top: Double, width: Double, height: Double) {
        val ctx = UIGraphicsGetCurrentContext() ?: return
        val tone = pdfScoreColor(score)
        fillRoundedRect(ctx, CGRectMake(x, top, width, height), ProjectStatsPdfTheme.cardFillStrong, 10.0)
        strokeRoundedRect(ctx, CGRectMake(x, top, width, height), ProjectStatsPdfTheme.border, 10.0)
        drawText(StatsExportCopy.scoreLabel().uppercase(), scoreLabelFont, ProjectStatsPdfTheme.muted, x + 12.0, top + 18.0)
        drawText(formatChartValue(score), scoreValueFont, tone, x + 12.0, top + 40.0)
    }

    private fun drawKeyValueTable(rows: List<ProjectStatsTableRow>, pageTag: String) {
        val hasNotes = rows.any { !it.note.isNullOrBlank() }
        if (hasNotes) {
            drawTable(
                headers = listOf("Показатель", "Значение", "Комментарий"),
                rows = rows.map { listOf(it.label, it.value, it.note.orEmpty()) },
                columnFractions = listOf(0.34, 0.18, 0.48),
                title = null,
                pageTag = pageTag,
            )
        } else {
            drawTable(
                headers = listOf("Показатель", "Значение"),
                rows = rows.map { listOf(it.label, it.value) },
                columnFractions = listOf(0.55, 0.45),
                title = null,
                pageTag = pageTag,
            )
        }
    }

    private fun drawTable(
        headers: List<String>,
        rows: List<List<String>>,
        columnFractions: List<Double>,
        title: String?,
        pageTag: String,
        compact: Boolean = false,
        pdfStyle: ProjectStatsTablePdfStyle = ProjectStatsTablePdfStyle.Default,
    ) {
        val renderConfig = buildTableRenderConfig(headers, rows, columnFractions, compact, pdfStyle)

        title?.takeIf { it.isNotBlank() }?.let {
            ensureSpace(24.0, pageTag)
            drawParagraph(it.uppercase(), smallFont, ProjectStatsPdfTheme.muted, 14.0, renderConfig.x, renderConfig.width)
            y += 6.0
        }

        fun drawHeader() {
            val ctx = UIGraphicsGetCurrentContext() ?: return
            val actualHeaderH = measureTableRowHeight(
                headers,
                renderConfig.columnWidths,
                headerFont,
                renderConfig.headerLineHeight,
                renderConfig.cellPadding,
                renderConfig.minHeaderHeight,
            )
            ensureSpace(actualHeaderH + 4.0, pageTag)
            drawTableRow(
                ctx,
                renderConfig.x,
                y,
                renderConfig.columnWidths,
                headers,
                headerFont,
                ProjectStatsPdfTheme.headerFill,
                true,
                renderConfig.minHeaderHeight,
                renderConfig.cellPadding,
                renderConfig.headerLineHeight,
            )
            y += actualHeaderH
        }

        if (headers.isNotEmpty()) drawHeader()
        rows.forEach { row ->
            val ctx = UIGraphicsGetCurrentContext() ?: return@forEach
            val rowHeight = measureTableRowHeight(
                row,
                renderConfig.columnWidths,
                renderConfig.cellFont,
                renderConfig.cellLineHeight,
                renderConfig.cellPadding,
                renderConfig.minRowHeight,
            )
            if (y + rowHeight > pageHeight - bottomMargin) {
                startPage(pageTag)
                if (headers.isNotEmpty()) drawHeader()
            }
            val currentCtx = UIGraphicsGetCurrentContext() ?: return@forEach
            drawTableRow(
                currentCtx,
                renderConfig.x,
                y,
                renderConfig.columnWidths,
                row,
                renderConfig.cellFont,
                null,
                false,
                renderConfig.minRowHeight,
                renderConfig.cellPadding,
                renderConfig.cellLineHeight,
            )
            y += rowHeight
        }
    }

    private fun drawTableRow(
        ctx: platform.CoreGraphics.CGContextRef,
        x: Double,
        top: Double,
        widths: List<Double>,
        cells: List<String>,
        font: UIFont,
        fill: PdfRgb?,
        lightText: Boolean,
        minHeight: Double,
        padding: Double,
        lineHeight: Double = if (lightText) 13.0 else 15.0,
    ) {
        val rowHeight = measureTableRowHeight(cells, widths, font, lineHeight, padding, minHeight)
        var cursorX = x
        widths.forEachIndexed { index, width ->
            fill?.let { fillRoundedRect(ctx, CGRectMake(cursorX, top, width, rowHeight), it, 7.0) }
            if (!lightText) {
                strokeRoundedRect(ctx, CGRectMake(cursorX, top, width, rowHeight), ProjectStatsPdfTheme.border, 7.0)
            }
            val raw = cells.getOrNull(index).orEmpty()
            val isLink = !lightText && raw.startsWith("@LINK:")
            val displayText = if (isLink) raw.removePrefix("@LINK:").substringAfter("|") else raw
            val color = when {
                lightText -> PdfRgb(255, 255, 255)
                isLink -> PdfRgb(30, 100, 200)
                else -> ProjectStatsPdfTheme.ink
            }
            if (isLink) {
                val url = raw.removePrefix("@LINK:").substringBefore("|")
                NSURL.URLWithString(url)?.let { linkUrl ->
                    UIGraphicsSetPDFContextURLForRect(
                        linkUrl,
                        CGRectMake(cursorX + padding, top + padding - 1.0, width - padding * 2, rowHeight - padding * 2),
                    )
                }
            }
            wrapText(displayText, font, width - padding * 2).forEachIndexed { lineIndex, line ->
                attr(line, font, color).drawAtPoint(
                    CGPointMake(cursorX + padding, top + padding + lineIndex * lineHeight - 1.0),
                )
            }
            cursorX += width
        }
    }

    private fun measureTableRowHeight(
        cells: List<String>,
        widths: List<Double>,
        font: UIFont,
        lineHeight: Double,
        padding: Double,
        minHeight: Double,
    ): Double {
        val maxLines = widths.indices.maxOfOrNull { index ->
            val cell = cells.getOrNull(index).orEmpty()
            max(1, wrapText(cell, font, widths[index] - padding * 2).size)
        } ?: 1
        return max(minHeight, padding * 2 + maxLines * lineHeight)
    }

    private fun drawChart(chart: ProjectStatsChart, pageTag: String, indented: Boolean = true) {
        when (chart) {
            is ProjectStatsChart.Bar -> drawColumnChart(chart.title, chart.yAxisLabel, chart.points, pdfChartAccent(chart.title), pageTag, indented)
            is ProjectStatsChart.Line -> drawLineChart(chart.title, chart.yAxisLabel, chart.points, pdfChartAccent(chart.title), pageTag, indented)
            is ProjectStatsChart.Donut -> drawDonutChart(chart.title, chart.segments, pageTag)
        }
    }

    private fun drawColumnChart(
        title: String,
        subtitle: String?,
        points: List<ProjectStatsChartPoint>,
        accent: PdfRgb,
        pageTag: String,
        indented: Boolean = true,
    ) {
        val displayPoints = if (points.size > 10) points.takeLast(10) else points
        if (displayPoints.isEmpty()) return
        val xLabelH = 24.0
        ensureSpace(160.0 + xLabelH, pageTag)
        val titleX = if (indented) leftMargin + 48.0 else leftMargin
        val titleW = if (indented) contentWidth - 48.0 else contentWidth
        drawParagraph(title, bodyFont, ProjectStatsPdfTheme.ink, 16.0, titleX, titleW)
        y += 2.0
        subtitle?.takeIf { it.isNotBlank() }?.let {
            drawParagraph(it, chartMutedFont, ProjectStatsPdfTheme.muted, 13.0, titleX, titleW)
            y += 2.0
        }
        val ctx = UIGraphicsGetCurrentContext() ?: return
        val chartX = if (indented) leftMargin + 48.0 else leftMargin
        val chartWidth = if (indented) contentWidth - 48.0 else contentWidth
        val yAxisW = 38.0
        val plotWidth = chartWidth - yAxisW
        val plotHeight = 108.0
        val chartTop = y + 10.0
        val baselineY = chartTop + plotHeight
        val maxValue = displayPoints.maxOf { it.value }.coerceAtLeast(1.0)

        // Y-axis grid and labels
        val roundedMax = kotlin.math.ceil(maxValue).toInt().coerceAtLeast(1)
        for (i in 0..3) {
            val fraction = i / 3.0
            val tickValue = kotlin.math.round(roundedMax * fraction).toInt()
            val tickFraction = tickValue.toDouble() / roundedMax.toDouble()
            val tickY = baselineY - tickFraction * plotHeight
            val label = tickValue.toString()
            val lw = measureTextWidth(label, chartMutedFont)
            drawText(label, chartMutedFont, ProjectStatsPdfTheme.muted, chartX + yAxisW - 4.0 - lw, tickY + 4.0)
            if (i > 0) strokeLine(ctx, chartX + yAxisW, tickY, chartX + yAxisW + plotWidth, tickY, ProjectStatsPdfTheme.border, 0.5)
        }

        // Axes
        strokeLine(ctx, chartX + yAxisW, chartTop, chartX + yAxisW, baselineY, ProjectStatsPdfTheme.border, 1.0)
        strokeLine(ctx, chartX + yAxisW, baselineY, chartX + yAxisW + plotWidth, baselineY, ProjectStatsPdfTheme.border, 1.0)

        // Bars and x-axis labels
        val gap = 5.0
        val totalGap = gap * (displayPoints.size - 1)
        val barWidth = minOf(36.0, (plotWidth - totalGap) / displayPoints.size)
        val totalBarsW = barWidth * displayPoints.size + totalGap
        val startX = chartX + yAxisW + (plotWidth - totalBarsW) / 2.0

        displayPoints.forEachIndexed { index, point ->
            val left = startX + index * (barWidth + gap)
            val cx = left + barWidth / 2.0
            val valueH = max(4.0, point.value / maxValue * plotHeight)
            fillRoundedRect(ctx, CGRectMake(left, baselineY - valueH, barWidth, valueH), accent, 4.0)
            val shortLabel = point.label.take(8)
            val lw = measureTextWidth(shortLabel, chartMutedFont)
            drawText(shortLabel, chartMutedFont, ProjectStatsPdfTheme.muted, cx - lw / 2.0, baselineY + 14.0)
        }

        y = baselineY + xLabelH + 6.0
    }

    private fun drawLineChart(
        title: String,
        subtitle: String?,
        points: List<ProjectStatsChartPoint>,
        accent: PdfRgb,
        pageTag: String,
        indented: Boolean = true,
    ) {
        val displayPoints = if (points.size > 10) points.takeLast(10) else points
        if (displayPoints.isEmpty()) return
        val xLabelH = 24.0
        ensureSpace(160.0 + xLabelH, pageTag)
        val titleX = if (indented) leftMargin + 48.0 else leftMargin
        val titleW = if (indented) contentWidth - 48.0 else contentWidth
        drawParagraph(title, bodyFont, ProjectStatsPdfTheme.ink, 16.0, titleX, titleW)
        y += 2.0
        subtitle?.takeIf { it.isNotBlank() }?.let {
            drawParagraph(it, chartMutedFont, ProjectStatsPdfTheme.muted, 13.0, titleX, titleW)
            y += 2.0
        }
        val ctx = UIGraphicsGetCurrentContext() ?: return
        val chartX = if (indented) leftMargin + 48.0 else leftMargin
        val chartWidth = if (indented) contentWidth - 48.0 else contentWidth
        val yAxisW = 38.0
        val plotWidth = chartWidth - yAxisW
        val plotHeight = 108.0
        val chartTop = y + 10.0
        val baselineY = chartTop + plotHeight
        val maxValue = displayPoints.maxOf { it.value }.coerceAtLeast(1.0)
        val plotStartX = chartX + yAxisW

        // Y-axis grid and labels
        val roundedMax = kotlin.math.ceil(maxValue).toInt().coerceAtLeast(1)
        for (i in 0..3) {
            val fraction = i / 3.0
            val tickValue = kotlin.math.round(roundedMax * fraction).toInt()
            val tickFraction = tickValue.toDouble() / roundedMax.toDouble()
            val tickY = baselineY - tickFraction * plotHeight
            val label = tickValue.toString()
            val lw = measureTextWidth(label, chartMutedFont)
            drawText(label, chartMutedFont, ProjectStatsPdfTheme.muted, chartX + yAxisW - 4.0 - lw, tickY + 4.0)
            if (i > 0) strokeLine(ctx, plotStartX, tickY, plotStartX + plotWidth, tickY, ProjectStatsPdfTheme.border, 0.5)
        }

        // Axes
        strokeLine(ctx, plotStartX, chartTop, plotStartX, baselineY, ProjectStatsPdfTheme.border, 1.0)
        strokeLine(ctx, plotStartX, baselineY, plotStartX + plotWidth, baselineY, ProjectStatsPdfTheme.border, 1.0)

        // Line path
        val stepX = if (displayPoints.size == 1) 0.0 else plotWidth / (displayPoints.size - 1)
        CGContextSetStrokeColorWithColor(ctx, uiColor(accent).CGColor)
        CGContextSetLineWidth(ctx, 2.5)
        CGContextBeginPath(ctx)
        displayPoints.forEachIndexed { index, point ->
            val px = plotStartX + stepX * index
            val py = baselineY - point.value / maxValue * plotHeight
            if (index == 0) CGContextMoveToPoint(ctx, px, py) else CGContextAddLineToPoint(ctx, px, py)
        }
        CGContextStrokePath(ctx)

        // Dots and x-axis labels
        displayPoints.forEachIndexed { index, point ->
            val px = plotStartX + stepX * index
            val py = baselineY - point.value / maxValue * plotHeight
            fillEllipse(ctx, CGRectMake(px - 4.0, py - 4.0, 8.0, 8.0), accent)
            val shortLabel = point.label.take(8)
            val lw = measureTextWidth(shortLabel, chartMutedFont)
            drawText(shortLabel, chartMutedFont, ProjectStatsPdfTheme.muted, px - lw / 2.0, baselineY + 14.0)
        }

        y = baselineY + xLabelH + 6.0
    }

    private fun drawDonutChart(title: String, segments: List<ProjectStatsChartSegment>, pageTag: String) {
        if (segments.isEmpty()) return
        val total = segments.sumOf { it.value }.coerceAtLeast(1.0)
        val legendH = segments.size * 18.0
        ensureSpace(180.0 + legendH.coerceAtLeast(160.0), pageTag)
        drawParagraph(title, bodyFont, ProjectStatsPdfTheme.ink, 16.0, leftMargin + 48.0, contentWidth - 48.0)
        y += 4.0
        val ctx = UIGraphicsGetCurrentContext() ?: return
        val chartX = leftMargin + 48.0
        val centerX = chartX + 74.0
        val centerY = y + 78.0
        val outerRadius = 68.0
        val innerRadius = 34.0
        var startAngle = -PI / 2.0

        segments.forEachIndexed { index, segment ->
            val color = donutColor(index, title)
            val sweep = segment.value / total * 2.0 * PI
            drawPieSlice(ctx, centerX, centerY, outerRadius, startAngle, startAngle + sweep, color)
            startAngle += sweep
        }
        fillEllipse(ctx, CGRectMake(centerX - innerRadius, centerY - innerRadius, innerRadius * 2.0, innerRadius * 2.0), ProjectStatsPdfTheme.paper)

        // Right-side vertical legend
        val legendTop = centerY - (segments.size * 18.0) / 2.0 + 9.0
        val legendX = centerX + outerRadius + 16.0
        segments.forEachIndexed { index, segment ->
            val ly = legendTop + index * 18.0
            fillRoundedRect(ctx, CGRectMake(legendX, ly - 7.0, 9.0, 8.0), donutColor(index, title), 2.0)
            val pct = "${((segment.value / total) * 100).toInt()}%"
            drawText("${segment.label}  $pct", chartMutedFont, ProjectStatsPdfTheme.muted, legendX + 13.0, ly + 4.0)
        }

        val chartBottom = centerY + outerRadius + 8.0
        y = max(chartBottom, legendTop + legendH) + 8.0
    }

    private fun drawPieSlice(
        ctx: platform.CoreGraphics.CGContextRef,
        centerX: Double,
        centerY: Double,
        radius: Double,
        startAngle: Double,
        endAngle: Double,
        color: PdfRgb,
    ) {
        val path = PathBuilder()
        path.moveTo(centerX, centerY)
        val segments = 24
        for (step in 0..segments) {
            val t = startAngle + (endAngle - startAngle) * step / segments
            val x = centerX + kotlin.math.cos(t) * radius
            val y = centerY + kotlin.math.sin(t) * radius
            path.lineTo(x, y)
        }
        path.close()
        CGContextSetFillColorWithColor(ctx, uiColor(color).CGColor)
        CGContextBeginPath(ctx)
        path.points.forEachIndexed { index, point ->
            if (index == 0) CGContextMoveToPoint(ctx, point.first, point.second) else CGContextAddLineToPoint(ctx, point.first, point.second)
        }
        CGContextClosePath(ctx)
        CGContextFillPath(ctx)
    }

    private fun ensureSpace(requiredHeight: Double, pageTag: String) {
        if (y + requiredHeight > pageHeight - bottomMargin) {
            startPage(pageTag)
        }
    }

    private fun pageSpecForSection(section: ProjectStatsSection): PdfPageSpec {
        val table = section.table ?: return defaultPageSpec
        if (table.pdfStyle != ProjectStatsTablePdfStyle.RapidPullRequestList) return defaultPageSpec
        val renderConfig = buildRapidPullRequestTableConfig(table.headers, table.rows)
        val requiredWidth = defaultPageSpec.leftMargin + renderConfig.columnWidths.sum() + defaultPageSpec.rightMargin
        return defaultPageSpec.copy(width = max(595.0, requiredWidth))
    }

    private fun buildTableRenderConfig(
        headers: List<String>,
        rows: List<List<String>>,
        columnFractions: List<Double>,
        compact: Boolean,
        pdfStyle: ProjectStatsTablePdfStyle,
    ): TableRenderConfig = when (pdfStyle) {
        ProjectStatsTablePdfStyle.RapidPullRequestList -> buildRapidPullRequestTableConfig(headers, rows)
        ProjectStatsTablePdfStyle.Default -> {
            val tableX = if (compact) leftMargin else leftMargin + 48.0
            val tableWidth = if (compact) contentWidth else contentWidth - 48.0
            val fractions = if (columnFractions.size == headers.size) columnFractions else List(headers.size) { 1.0 / headers.size }
            TableRenderConfig(
                x = tableX,
                width = tableWidth,
                columnWidths = fractions.map { tableWidth * it },
                cellFont = if (compact) smallFont else bodyFont,
                cellPadding = if (compact) 6.0 else 12.0,
                cellLineHeight = if (compact) 12.0 else 15.0,
                headerLineHeight = if (compact) 10.0 else 13.0,
                minRowHeight = if (compact) 22.0 else 34.0,
                minHeaderHeight = if (compact) 26.0 else 34.0,
            )
        }
    }

    private fun buildRapidPullRequestTableConfig(
        headers: List<String>,
        rows: List<List<String>>,
    ): TableRenderConfig {
        val prWidth = 0.20 * (595.0 - defaultPageSpec.leftMargin - defaultPageSpec.rightMargin)
        val widths = listOf(
            prWidth,
            rapidColumnWidth(headers, rows, 1, splitByNewline = false),
            rapidColumnWidth(headers, rows, 2, splitByNewline = false),
            rapidColumnWidth(headers, rows, 3, splitByNewline = false),
            rapidColumnWidth(headers, rows, 4, splitByNewline = true),
            rapidColumnWidth(headers, rows, 5, splitByNewline = true),
            rapidColumnWidth(headers, rows, 6, splitByNewline = false),
            rapidColumnWidth(headers, rows, 7, splitByNewline = false),
        )
        return TableRenderConfig(
            x = defaultPageSpec.leftMargin,
            width = widths.sum(),
            columnWidths = widths,
            cellFont = smallFont,
            cellPadding = 3.5,
            cellLineHeight = 10.5,
            headerLineHeight = 9.5,
            minRowHeight = 0.0,
            minHeaderHeight = 0.0,
        )
    }

    private fun rapidColumnWidth(
        headers: List<String>,
        rows: List<List<String>>,
        index: Int,
        splitByNewline: Boolean,
    ): Double {
        val values = buildList {
            headers.getOrNull(index)?.let(::add)
            rows.mapNotNull { it.getOrNull(index) }
                .map { cell ->
                    if (cell.startsWith("@LINK:")) cell.removePrefix("@LINK:").substringAfter("|")
                    else cell
                }
                .forEach(::add)
        }
        val measured = values.maxOfOrNull { text ->
            val parts = if (splitByNewline) text.split('\n') else listOf(text)
            parts.maxOfOrNull { measureTextWidth(it.ifBlank { " " }, smallFont) } ?: 0.0
        } ?: 0.0
        return measured + 16.0
    }

    private fun drawParagraph(
        text: String,
        font: UIFont,
        rgb: PdfRgb,
        lineHeight: Double,
        x: Double,
        width: Double,
        advanceY: Boolean = true,
        alpha: Double = 1.0,
    ) {
        val attr = attr(text, font, rgb, alpha)
        val height = attr.boundingRectWithSize(
            CGSizeMake(width, 8000.0),
            NSStringDrawingUsesLineFragmentOrigin,
            null,
        ).useContents { size.height }
        attr.drawWithRect(
            CGRectMake(x, y - lineHeight + font.lineHeight, width, height + 2.0),
            NSStringDrawingUsesLineFragmentOrigin,
            null,
        )
        if (advanceY) {
            y += max(lineHeight, height)
        }
    }

    private fun measureParagraphHeight(text: String, font: UIFont, width: Double): Double =
        attr(text, font, ProjectStatsPdfTheme.ink).boundingRectWithSize(
            CGSizeMake(width, 8000.0),
            NSStringDrawingUsesLineFragmentOrigin,
            null,
        ).useContents { size.height }

    private fun wrapText(text: String, font: UIFont, maxWidth: Double): List<String> {
        if (text.isBlank()) return listOf("")
        val result = mutableListOf<String>()
        text.split('\n').forEach { paragraph ->
            val rawWords = paragraph.split(Regex("\\s+")).filter { it.isNotBlank() }
            // Expand words containing '/' that are too wide to fit as-is
            val words = rawWords.flatMap { word ->
                if ('/' in word && measureTextWidth(word, font) > maxWidth) {
                    breakWordAtSlash(word, font, maxWidth)
                } else {
                    listOf(word)
                }
            }
            if (words.isEmpty()) {
                result += ""
                return@forEach
            }
            var current = words.first()
            for (word in words.drop(1)) {
                val candidate = "$current $word"
                if (measureTextWidth(candidate, font) <= maxWidth) {
                    current = candidate
                } else {
                    result += current
                    current = word
                }
            }
            result += current
        }
        return result
    }

    private fun breakWordAtSlash(word: String, font: UIFont, maxWidth: Double): List<String> {
        val parts = word.split('/')
        if (parts.size <= 1) return listOf(word)
        val segments = mutableListOf<String>()
        var current = ""
        parts.forEachIndexed { index, part ->
            val piece = if (index < parts.lastIndex) "$part/" else part
            if (current.isEmpty()) {
                current = piece
            } else {
                val candidate = current + piece
                if (measureTextWidth(candidate, font) <= maxWidth) {
                    current = candidate
                } else {
                    segments += current
                    current = piece
                }
            }
        }
        if (current.isNotEmpty()) segments += current
        return segments.ifEmpty { listOf(word) }
    }

    private fun drawText(
        text: String,
        font: UIFont,
        rgb: PdfRgb,
        x: Double,
        baselineY: Double,
        alpha: Double = 1.0,
    ) {
        attr(text, font, rgb, alpha).drawAtPoint(platform.CoreGraphics.CGPointMake(x, baselineY - font.lineHeight))
    }

    private fun measureTextWidth(text: String, font: UIFont): Double =
        attr(text, font, ProjectStatsPdfTheme.ink)
            .boundingRectWithSize(CGSizeMake(10000.0, 100.0), NSStringDrawingUsesLineFragmentOrigin, null)
            .useContents { size.width }

    private fun drawCenteredText(text: String, font: UIFont, rgb: PdfRgb, centerX: Double, baselineY: Double, width: Double) {
        val attributed = attr(text, font, rgb)
        val rect = attributed.boundingRectWithSize(CGSizeMake(width, 100.0), NSStringDrawingUsesLineFragmentOrigin, null)
        drawText(text, font, rgb, centerX - rect.useContents { size.width / 2.0 }, baselineY)
    }

    private fun fillRect(ctx: CGContextRef, rect: CValue<CGRect>, rgb: PdfRgb) {
        CGContextSetFillColorWithColor(ctx, uiColor(rgb).CGColor)
        CGContextFillRect(ctx, rect)
    }

    private fun fillRoundedRect(ctx: CGContextRef, rect: CValue<CGRect>, rgb: PdfRgb, radius: Double) {
        rect.useContents {
            addRoundedRectPath(ctx, origin.x, origin.y, size.width, size.height, radius)
        }
        CGContextSetFillColorWithColor(ctx, uiColor(rgb).CGColor)
        CGContextFillPath(ctx)
    }

    private fun strokeRect(ctx: CGContextRef, rect: CValue<CGRect>, rgb: PdfRgb) {
        CGContextSetRGBStrokeColor(ctx, rgb.r / 255.0, rgb.g / 255.0, rgb.b / 255.0, 1.0)
        CGContextSetLineWidth(ctx, 1.0)
        CGContextStrokeRect(ctx, rect)
    }

    private fun strokeRoundedRect(ctx: CGContextRef, rect: CValue<CGRect>, rgb: PdfRgb, radius: Double) {
        rect.useContents {
            addRoundedRectPath(ctx, origin.x, origin.y, size.width, size.height, radius)
        }
        CGContextSetRGBStrokeColor(ctx, rgb.r / 255.0, rgb.g / 255.0, rgb.b / 255.0, 1.0)
        CGContextSetLineWidth(ctx, 1.0)
        CGContextStrokePath(ctx)
    }

    private fun fillEllipse(ctx: CGContextRef, rect: CValue<CGRect>, rgb: PdfRgb) {
        CGContextSetFillColorWithColor(ctx, uiColor(rgb).CGColor)
        CGContextFillEllipseInRect(ctx, rect)
    }

    private fun strokeLine(
        ctx: platform.CoreGraphics.CGContextRef,
        x1: Double,
        y1: Double,
        x2: Double,
        y2: Double,
        rgb: PdfRgb,
        width: Double,
    ) {
        CGContextSetRGBStrokeColor(ctx, rgb.r / 255.0, rgb.g / 255.0, rgb.b / 255.0, 1.0)
        CGContextSetLineWidth(ctx, width)
        CGContextBeginPath(ctx)
        CGContextMoveToPoint(ctx, x1, y1)
        CGContextAddLineToPoint(ctx, x2, y2)
        CGContextStrokePath(ctx)
    }

    private fun addRoundedRectPath(
        ctx: CGContextRef,
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        radius: Double,
    ) {
        val safeRadius = minOf(radius, width / 2.0, height / 2.0)
        CGContextBeginPath(ctx)
        CGContextMoveToPoint(ctx, x + safeRadius, y)
        CGContextAddLineToPoint(ctx, x + width - safeRadius, y)
        CGContextAddArcToPoint(ctx, x + width, y, x + width, y + safeRadius, safeRadius)
        CGContextAddLineToPoint(ctx, x + width, y + height - safeRadius)
        CGContextAddArcToPoint(ctx, x + width, y + height, x + width - safeRadius, y + height, safeRadius)
        CGContextAddLineToPoint(ctx, x + safeRadius, y + height)
        CGContextAddArcToPoint(ctx, x, y + height, x, y + height - safeRadius, safeRadius)
        CGContextAddLineToPoint(ctx, x, y + safeRadius)
        CGContextAddArcToPoint(ctx, x, y, x + safeRadius, y, safeRadius)
        CGContextClosePath(ctx)
    }

    private fun drawWatermark(ctx: CGContextRef) {
        val image = watermarkImage
        if (image == null) return
        val targetWidth = pageWidth
        val scale = targetWidth / image.size.useContents { width }
        val targetHeight = image.size.useContents { height } * scale
        val left = (pageWidth - targetWidth) / 2.0
        val top = (pageHeight - targetHeight) / 2.0
        CGContextSaveGState(ctx)
        CGContextSetAlpha(ctx, 1.0)
        image.drawInRect(CGRectMake(left, top, targetWidth, targetHeight))
        CGContextRestoreGState(ctx)
    }

    private fun donutColor(index: Int, title: String): PdfRgb {
        val palette = if (title.contains("ддн", ignoreCase = true) || title.contains("день", ignoreCase = true)) {
            ProjectStatsPdfTheme.weekdayPalette
        } else {
            ProjectStatsPdfTheme.churnPalette
        }
        return palette[index % palette.size]
    }

    private fun summaryCardColor(title: String): PdfRgb {
        val t = title.lowercase()
        return when {
            "быстр" in t || "rapid" in t -> ProjectStatsPdfTheme.rapidPrChart
            "pull" in t || " pr" in t || t == "pr" || t.startsWith("pr ") -> ProjectStatsPdfTheme.prChart
            else -> ProjectStatsPdfTheme.rail
        }
    }

    private fun attr(text: String, font: UIFont, rgb: PdfRgb, alpha: Double = 1.0): NSAttributedString =
        NSAttributedString.create(
            string = text,
            attributes = mapOf(
                NSFontAttributeName to font,
                NSForegroundColorAttributeName to uiColor(rgb, alpha),
            ),
        )

    private fun uiColor(rgb: PdfRgb, alpha: Double = 1.0): UIColor =
        UIColor.colorWithRed(rgb.r / 255.0, green = rgb.g / 255.0, blue = rgb.b / 255.0, alpha = alpha)!!
}

private class PathBuilder {
    val points = mutableListOf<Pair<Double, Double>>()
    fun moveTo(x: Double, y: Double) {
        points += x to y
    }
    fun lineTo(x: Double, y: Double) {
        points += x to y
    }
    fun close() = Unit
}
