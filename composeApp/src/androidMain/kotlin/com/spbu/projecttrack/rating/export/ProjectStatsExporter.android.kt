package com.spbu.projecttrack.rating.export

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.spbu.projecttrack.rating.common.StatsExportCopy
import java.io.File
import projecttrack.composeapp.generated.resources.Res
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
actual fun rememberProjectStatsExporter(): ProjectStatsExporter {
    val context = LocalContext.current
    return remember(context) {
        AndroidProjectStatsExporter(context.applicationContext)
    }
}

private class AndroidProjectStatsExporter(
    private val context: Context,
) : ProjectStatsExporter {

    override suspend fun exportPdf(
        payload: ProjectStatsExportPayload,
    ): Result<ProjectStatsExportResult> = runCatching {
        val file = createOutputFile(payload, "pdf")
        val watermarkBitmap = runCatching {
            Res.readBytes("drawable/spbu_pdf_logo.png")
                .let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
        }.getOrNull()
        StyledPdfRenderer(watermarkBitmap).write(file, payload)
        tryShare(file, "application/pdf")
        ProjectStatsExportResult(
            fileName = file.name,
            absolutePath = file.absolutePath,
            mimeType = "application/pdf",
        )
    }

    override suspend fun exportExcelCsv(
        payload: ProjectStatsExportPayload,
    ): Result<ProjectStatsExportResult> = runCatching {
        val file = createOutputFile(payload, "xlsx")
        file.writeBytes(buildProjectStatsXlsx(payload))
        tryShare(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        ProjectStatsExportResult(
            fileName = file.name,
            absolutePath = file.absolutePath,
            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        )
    }

    private fun createOutputFile(payload: ProjectStatsExportPayload, extension: String): File {
        val baseName = sanitizeProjectStatsFileName(payload.projectName)
        val dir = File(context.cacheDir, "project_stats").apply { mkdirs() }
        return File(dir, "$baseName.$extension")
    }

    private fun tryShare(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
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
        }.onFailure { if (it is ActivityNotFoundException) return }
    }
}

private class StyledPdfRenderer(
    private val watermarkBitmap: Bitmap?,
) {
    private data class PdfPageSpec(
        val width: Int = 595,
        val height: Int = 842,
        val leftMargin: Float = 34f,
        val rightMargin: Float = 34f,
        val topMargin: Float = 88f,
        val bottomMargin: Float = 48f,
    )

    private data class TableRenderConfig(
        val x: Float,
        val width: Float,
        val columnWidths: List<Float>,
        val cellPaint: Paint,
        val cellPadding: Float,
        val cellLineHeight: Float,
        val headerLineHeight: Float,
        val minRowHeight: Float,
        val minHeaderHeight: Float,
    )

    private val defaultPageSpec = PdfPageSpec()
    private var currentPageSpec = defaultPageSpec

    private val pageWidth: Int get() = currentPageSpec.width
    private val pageHeight: Int get() = currentPageSpec.height
    private val leftMargin: Float get() = currentPageSpec.leftMargin
    private val rightMargin: Float get() = currentPageSpec.rightMargin
    private val topMargin: Float get() = currentPageSpec.topMargin
    private val bottomMargin: Float get() = currentPageSpec.bottomMargin
    private val contentWidth: Float get() = pageWidth - leftMargin - rightMargin

    private val paperPaint = fillPaint(ProjectStatsPdfTheme.paper)
    private val railPaint = fillPaint(ProjectStatsPdfTheme.rail)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = color(ProjectStatsPdfTheme.border)
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = color(ProjectStatsPdfTheme.border)
        strokeWidth = 1f
    }
    private val pageNoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = color(ProjectStatsPdfTheme.rail)
        textSize = 11f
        isFakeBoldText = true
    }
    private val pageTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = color(ProjectStatsPdfTheme.muted)
        textSize = 10f
        isFakeBoldText = true
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = color(ProjectStatsPdfTheme.rail)
        textSize = 34f
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        isFakeBoldText = true
    }
    private val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = color(ProjectStatsPdfTheme.ink)
        textSize = 24f
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        isFakeBoldText = true
    }
    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = color(ProjectStatsPdfTheme.muted)
        textSize = 11f
    }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = color(ProjectStatsPdfTheme.ink)
        textSize = 11f
    }
    private val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = color(ProjectStatsPdfTheme.muted)
        textSize = 9.5f
    }
    private val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textSize = 10f
        isFakeBoldText = true
    }
    private val cardTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = color(ProjectStatsPdfTheme.muted)
        textSize = 9f
        isFakeBoldText = true
    }
    private val cardValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = color(ProjectStatsPdfTheme.rail)
        textSize = 22f
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        isFakeBoldText = true
    }
    private val cardSubtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = color(ProjectStatsPdfTheme.muted)
        textSize = 9f
    }
    private val scoreLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = color(ProjectStatsPdfTheme.muted)
        textSize = 9f
        isFakeBoldText = true
    }
    private val scoreValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 18f
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        isFakeBoldText = true
    }
    private val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = color(ProjectStatsPdfTheme.muted)
        textSize = 10f
    }
    private val chartLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = color(ProjectStatsPdfTheme.ink)
        textSize = 9.5f
    }
    private val chartMutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = color(ProjectStatsPdfTheme.muted)
        textSize = 8.5f
    }

    private lateinit var document: PdfDocument
    private lateinit var page: PdfDocument.Page
    private lateinit var canvas: Canvas
    private var pageNumber = 0
    private var y = topMargin
    private var currentTag = "OVERVIEW"
    fun write(file: File, payload: ProjectStatsExportPayload) {
        document = PdfDocument()
        startPage("OVERVIEW")
        drawCover(payload)
        payload.sections.forEach { drawSection(it) }
        document.finishPage(page)
        file.outputStream().use { document.writeTo(it) }
        document.close()
    }

    private fun drawCover(payload: ProjectStatsExportPayload) {
        ensureSpace(140f, "OVERVIEW")
        drawTextBlock(payload.projectName, titlePaint, 42f, leftMargin + 48f, contentWidth - 48f)
        y += 8f
        payload.description?.takeIf { it.isNotBlank() }?.let {
            drawTextBlock(it, subtitlePaint, 15f, leftMargin + 48f, contentWidth - 48f)
            y += 10f
        }

        val meta = buildList {
            payload.periodLabel?.takeIf { it.isNotBlank() }?.let { add("Период: $it") }
            payload.customerName?.takeIf { it.isNotBlank() }?.let { add("Заказчик: $it") }
            payload.repositoryUrl?.takeIf { it.isNotBlank() }?.let { add("Репозиторий: $it") }
        }.joinToString("  •  ")
        if (meta.isNotBlank()) {
            drawTextBlock(meta, bodyPaint, 16f, leftMargin + 48f, contentWidth - 48f)
            y += 4f
        }
        payload.generatedAtLabel?.takeIf { it.isNotBlank() }?.let {
            drawTextBlock("Сформировано: $it", subtitlePaint, 14f, leftMargin + 48f, contentWidth - 48f)
        }
        y += 12f
        canvas.drawLine(leftMargin + 48f, y, pageWidth - rightMargin, y, dividerPaint)
        y += 28f

        if (payload.summaryCards.isNotEmpty()) {
            drawSectionTitle("Ключевые показатели")
            y += 8f
            drawSummaryCards(payload.summaryCards)
            y += 22f
        }

        if (payload.members.isNotEmpty()) {
            drawSectionTitle("Команда")
            y += 10f
            drawTable(
                title = null,
                headers = listOf("Участник", "Роль"),
                rows = payload.members.map { listOf(it.name, it.role.orEmpty()) },
                columnFractions = listOf(0.5f, 0.5f),
                pageTag = "OVERVIEW",
            )
            y += 14f
        }
    }

    private fun drawSection(section: ProjectStatsSection) {
        currentTag = pdfSectionTag(section.title)
        startPage(currentTag, pageSpecForSection(section))
        drawSectionHeader(section)

        val metricRowsAsCards = section.rows.isNotEmpty() && section.rows.size <= 4
        if (metricRowsAsCards) {
            drawMetricCards(section.rows)
            y += 18f
        } else if (section.rows.isNotEmpty()) {
            drawKeyValueTable(section.rows, currentTag)
            y += 16f
        }

        if (section.chartFirst) {
            section.chart?.let { drawChart(it, currentTag) }
            section.table?.let { table ->
                drawTable(
                    title = table.title,
                    headers = table.headers,
                    rows = table.rows,
                    columnFractions = table.columnFractions ?: List(table.headers.size) { 1f / table.headers.size },
                    pageTag = currentTag,
                    compact = table.headers.size >= 6,
                    pdfStyle = table.pdfStyle,
                )
                y += 16f
            }
        } else {
            val tableCompact = section.table?.headers?.size?.let { it >= 6 } ?: false
            section.table?.let { table ->
                drawTable(
                    title = table.title,
                    headers = table.headers,
                    rows = table.rows,
                    columnFractions = table.columnFractions ?: List(table.headers.size) { 1f / table.headers.size },
                    pageTag = currentTag,
                    compact = tableCompact,
                    pdfStyle = table.pdfStyle,
                )
                y += 16f
            }
            section.chart?.let { drawChart(it, currentTag, indented = !tableCompact) }
        }

        section.notes.forEach { note ->
            ensureSpace(24f, currentTag)
            drawTextBlock(note, notePaint, 14f, leftMargin + 48f, contentWidth - 48f)
            y += 6f
        }
        y += 10f
    }

    private fun drawSectionHeader(section: ProjectStatsSection) {
        val headerX = leftMargin + 48f
        val scoreWidth = if (section.score != null) 96f else 0f
        val textWidth = contentWidth - 48f - if (section.score != null) scoreWidth + 12f else 0f
        val titleLines = wrapText(section.title, sectionPaint, textWidth)
        val titleHeight = titleLines.size * 28f
        val subtitleHeight = section.subtitle?.takeIf { it.isNotBlank() }?.let {
            wrapText(it, subtitlePaint, textWidth).size * 15f + 6f
        } ?: 0f
        val textPaddingTop = 24f
        val blockHeight = max(titleHeight + subtitleHeight + textPaddingTop + 8f, if (section.score != null) 76f else 0f)
        ensureSpace(blockHeight + 12f, currentTag)

        titleLines.forEachIndexed { index, line ->
            canvas.drawText(line, headerX, y + textPaddingTop + index * 28f, sectionPaint)
        }
        var localY = y + textPaddingTop + titleHeight
        section.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
            localY += 8f
            wrapText(subtitle, subtitlePaint, textWidth).forEachIndexed { index, line ->
                canvas.drawText(line, headerX, localY + index * 15f, subtitlePaint)
            }
        }

        section.score?.let { score ->
            drawScoreBadge(score, pageWidth - rightMargin - scoreWidth, y + 10f, scoreWidth, 58f)
        }

        y += blockHeight + 16f
    }

    private fun drawSectionTitle(title: String) {
        drawTextBlock(title, sectionPaint, 28f, leftMargin + 48f, contentWidth - 48f)
        y += 6f
    }

    private fun drawSummaryCards(cards: List<ProjectStatsSummaryCard>) {
        val displayCards = cards.take(4)
        if (displayCards.isEmpty()) return
        val gap = 12f
        val startX = leftMargin + 48f
        val availableWidth = contentWidth - 48f
        val cardWidth = (availableWidth - gap * (displayCards.size - 1)) / displayCards.size
        val cardHeight = 94f
        ensureSpace(cardHeight + 8f, "OVERVIEW")

        displayCards.forEachIndexed { index, card ->
            val x = startX + index * (cardWidth + gap)
            val valueColor = summaryCardColor(card.title)
            drawMetricCard(x, y, cardWidth, cardHeight, card.title, card.value, card.subtitle, valueColor)
        }
        y += cardHeight
    }

    private fun drawMetricCards(rows: List<ProjectStatsTableRow>) {
        val gap = 12f
        val startX = leftMargin + 48f
        val availableWidth = contentWidth - 48f
        val cardWidth = (availableWidth - gap * (rows.size - 1)) / rows.size
        val cardHeight = 84f
        ensureSpace(cardHeight + 8f, currentTag)
        rows.forEachIndexed { index, row ->
            val x = startX + index * (cardWidth + gap)
            val toneColor = when {
                row.label.contains("быстр", ignoreCase = true) -> ProjectStatsPdfTheme.high
                row.label.contains("доля", ignoreCase = true) || row.label.contains("issue", ignoreCase = true) -> ProjectStatsPdfTheme.mid
                else -> ProjectStatsPdfTheme.rail
            }
            drawMetricCard(x, y, cardWidth, cardHeight, row.label, row.value, row.note, toneColor)
        }
        y += cardHeight
    }

    private fun drawMetricCard(
        x: Float,
        top: Float,
        width: Float,
        height: Float,
        title: String,
        value: String,
        subtitle: String?,
        valueColor: PdfRgb,
    ) {
        val fillPaint = fillPaint(ProjectStatsPdfTheme.cardFill)
        val valuePaint = Paint(cardValuePaint).apply { color = color(valueColor) }
        canvas.drawRoundRect(x, top, x + width, top + height, 10f, 10f, fillPaint)
        canvas.drawRoundRect(x, top, x + width, top + height, 10f, 10f, borderPaint)
        drawSingleLine(title.uppercase(), cardTitlePaint, x + 14f, top + 28f, width - 28f)
        drawSingleLine(value, valuePaint, x + 14f, top + 62f, width - 28f)
        subtitle?.takeIf { it.isNotBlank() }?.let {
            val lines = wrapText(it, cardSubtitlePaint, width - 28f).take(3)
            lines.forEachIndexed { index, line ->
                canvas.drawText(line, x + 14f, top + 78f + index * 11f, cardSubtitlePaint)
            }
        }
    }

    private fun drawScoreBadge(score: Double, x: Float, top: Float, width: Float, height: Float) {
        val tone = pdfScoreColor(score)
        val fillPaint = fillPaint(ProjectStatsPdfTheme.cardFillStrong)
        val valuePaint = Paint(scoreValuePaint).apply { color = color(tone) }
        canvas.drawRoundRect(x, top, x + width, top + height, 10f, 10f, fillPaint)
        canvas.drawRoundRect(x, top, x + width, top + height, 10f, 10f, borderPaint)
        canvas.drawText(StatsExportCopy.scoreLabel().uppercase(), x + 12f, top + 18f, scoreLabelPaint)
        canvas.drawText(formatChartValue(score), x + 12f, top + 40f, valuePaint)
    }

    private fun drawKeyValueTable(rows: List<ProjectStatsTableRow>, pageTag: String) {
        val hasNotes = rows.any { !it.note.isNullOrBlank() }
        if (hasNotes) {
            drawTable(
                title = null,
                headers = listOf("Показатель", "Значение", "Комментарий"),
                rows = rows.map { listOf(it.label, it.value, it.note.orEmpty()) },
                columnFractions = listOf(0.34f, 0.18f, 0.48f),
                pageTag = pageTag,
            )
        } else {
            drawTable(
                title = null,
                headers = listOf("Показатель", "Значение"),
                rows = rows.map { listOf(it.label, it.value) },
                columnFractions = listOf(0.55f, 0.45f),
                pageTag = pageTag,
            )
        }
    }

    private fun drawTable(
        title: String?,
        headers: List<String>,
        rows: List<List<String>>,
        columnFractions: List<Float>,
        pageTag: String,
        compact: Boolean = false,
        pdfStyle: ProjectStatsTablePdfStyle = ProjectStatsTablePdfStyle.Default,
    ) {
        val renderConfig = buildTableRenderConfig(headers, rows, columnFractions, compact, pdfStyle)

        title?.takeIf { it.isNotBlank() }?.let {
            ensureSpace(24f, pageTag)
            drawTextBlock(it.uppercase(), smallPaint, 14f, renderConfig.x, renderConfig.width)
            y += 6f
        }

        fun drawHeader() {
            val actualHeaderH = measureTableRowHeight(
                headers,
                renderConfig.columnWidths,
                tableHeaderPaint,
                renderConfig.headerLineHeight,
                renderConfig.cellPadding,
                renderConfig.minHeaderHeight,
            )
            ensureSpace(actualHeaderH + 4f, pageTag)
            drawTableRow(
                x = renderConfig.x,
                top = y,
                columnWidths = renderConfig.columnWidths,
                cells = headers,
                textPaint = tableHeaderPaint,
                fill = fillPaint(ProjectStatsPdfTheme.headerFill),
                textColorIsLight = true,
                border = false,
                minHeight = renderConfig.minHeaderHeight,
                lineHeight = renderConfig.headerLineHeight,
                padding = renderConfig.cellPadding,
            )
            y += actualHeaderH
        }

        if (headers.isNotEmpty()) drawHeader()
        rows.forEach { row ->
            val rowHeight = measureTableRowHeight(
                row,
                renderConfig.columnWidths,
                renderConfig.cellPaint,
                renderConfig.cellLineHeight,
                renderConfig.cellPadding,
                renderConfig.minRowHeight,
            )
            if (y + rowHeight > pageHeight - bottomMargin) {
                startPage(pageTag)
                if (headers.isNotEmpty()) drawHeader()
            }
            drawTableRow(
                x = renderConfig.x,
                top = y,
                columnWidths = renderConfig.columnWidths,
                cells = row,
                textPaint = renderConfig.cellPaint,
                fill = null,
                textColorIsLight = false,
                border = true,
                minHeight = renderConfig.minRowHeight,
                lineHeight = renderConfig.cellLineHeight,
                padding = renderConfig.cellPadding,
            )
            y += rowHeight
        }
    }

    private fun drawTableRow(
        x: Float,
        top: Float,
        columnWidths: List<Float>,
        cells: List<String>,
        textPaint: Paint,
        fill: Paint?,
        textColorIsLight: Boolean,
        border: Boolean,
        minHeight: Float,
        lineHeight: Float,
        padding: Float,
    ) {
        val rowHeight = measureTableRowHeight(cells, columnWidths, textPaint, lineHeight, padding, minHeight)
        var cursorX = x
        columnWidths.forEachIndexed { index, width ->
            fill?.let { canvas.drawRoundRect(cursorX, top, cursorX + width, top + rowHeight, 7f, 7f, it) }
            if (border) {
                canvas.drawRoundRect(cursorX, top, cursorX + width, top + rowHeight, 7f, 7f, borderPaint)
            }
            val rawCell = cells.getOrNull(index).orEmpty()
            val isLink = !textColorIsLight && rawCell.startsWith("@LINK:")
            val cellText = if (isLink) rawCell.removePrefix("@LINK:").substringAfter("|") else rawCell
            val effectivePaint = when {
                textColorIsLight -> tableHeaderPaint
                isLink -> Paint(textPaint).apply {
                    color = AndroidColor.rgb(30, 100, 200)
                    isUnderlineText = true
                }
                else -> textPaint
            }
            val lines = wrapText(cellText, effectivePaint, width - padding * 2)
            val baselineStart = top + padding + lineHeight - 2f
            lines.forEachIndexed { lineIndex, line ->
                canvas.drawText(line, cursorX + padding, baselineStart + lineIndex * lineHeight, effectivePaint)
            }
            cursorX += width
        }
    }

    private fun measureTableRowHeight(
        cells: List<String>,
        columnWidths: List<Float>,
        textPaint: Paint,
        lineHeight: Float,
        padding: Float,
        minHeight: Float,
    ): Float {
        val lineCount = columnWidths.indices.maxOfOrNull { index ->
            val rawCell = cells.getOrNull(index).orEmpty()
            val cellText = if (rawCell.startsWith("@LINK:")) rawCell.removePrefix("@LINK:").substringAfter("|") else rawCell
            max(1, wrapText(cellText, textPaint, columnWidths[index] - padding * 2).size)
        } ?: 1
        return max(minHeight, padding * 2 + lineCount * lineHeight)
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
        val xLabelH = 24f
        ensureSpace(160f + xLabelH, pageTag)
        val titleX = if (indented) leftMargin + 48f else leftMargin
        val titleW = if (indented) contentWidth - 48f else contentWidth
        drawTextBlock(title, bodyPaint, 16f, titleX, titleW)
        y += 2f
        subtitle?.takeIf { it.isNotBlank() }?.let {
            drawTextBlock(it, smallPaint, 13f, titleX, titleW)
            y += 2f
        }

        val chartX = if (indented) leftMargin + 48f else leftMargin
        val chartWidth = if (indented) contentWidth - 48f else contentWidth
        val yAxisW = 38f
        val plotWidth = chartWidth - yAxisW
        val plotHeight = 108f
        val chartTop = y + 10f
        val baselineY = chartTop + plotHeight
        val maxValue = displayPoints.maxOf { it.value }.coerceAtLeast(1.0)
        val barPaint = fillPaint(accent)
        val gridPaint = Paint(dividerPaint).apply { alpha = 55 }

        // Y-axis grid lines and labels (4 ticks)
        val roundedMax = kotlin.math.ceil(maxValue).toInt().coerceAtLeast(1)
        for (i in 0..3) {
            val fraction = i / 3.0
            val tickValue = kotlin.math.round(roundedMax * fraction).toInt()
            val tickFraction = tickValue.toFloat() / roundedMax.toFloat()
            val tickY = baselineY - tickFraction * plotHeight
            val label = tickValue.toString()
            val lw = chartMutedPaint.measureText(label)
            canvas.drawText(label, chartX + yAxisW - 4f - lw, tickY + 3.5f, chartMutedPaint)
            if (i > 0) canvas.drawLine(chartX + yAxisW, tickY, chartX + yAxisW + plotWidth, tickY, gridPaint)
        }

        // Axes
        canvas.drawLine(chartX + yAxisW, chartTop, chartX + yAxisW, baselineY, dividerPaint)
        canvas.drawLine(chartX + yAxisW, baselineY, chartX + yAxisW + plotWidth, baselineY, dividerPaint)

        // Bars and x-axis labels
        val gap = 5f
        val totalGap = gap * (displayPoints.size - 1)
        val barWidth = min(36f, (plotWidth - totalGap) / displayPoints.size)
        val totalBarsW = barWidth * displayPoints.size + totalGap
        val startX = chartX + yAxisW + (plotWidth - totalBarsW) / 2f

        displayPoints.forEachIndexed { index, point ->
            val left = startX + index * (barWidth + gap)
            val cx = left + barWidth / 2f
            val valueH = ((point.value / maxValue).toFloat() * plotHeight).coerceAtLeast(4f)
            canvas.drawRoundRect(left, baselineY - valueH, left + barWidth, baselineY, 4f, 4f, barPaint)
            val label = wrapText(point.label, chartMutedPaint, barWidth + gap + 2f).firstOrNull().orEmpty()
            val lw = chartMutedPaint.measureText(label)
            canvas.drawText(label, cx - lw / 2f, baselineY + 14f, chartMutedPaint)
        }

        y = baselineY + xLabelH + 6f
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
        val xLabelH = 24f
        ensureSpace(160f + xLabelH, pageTag)
        val titleX = if (indented) leftMargin + 48f else leftMargin
        val titleW = if (indented) contentWidth - 48f else contentWidth
        drawTextBlock(title, bodyPaint, 16f, titleX, titleW)
        y += 2f
        subtitle?.takeIf { it.isNotBlank() }?.let {
            drawTextBlock(it, smallPaint, 13f, titleX, titleW)
            y += 2f
        }

        val chartX = if (indented) leftMargin + 48f else leftMargin
        val chartWidth = if (indented) contentWidth - 48f else contentWidth
        val yAxisW = 38f
        val plotWidth = chartWidth - yAxisW
        val plotHeight = 108f
        val chartTop = y + 10f
        val baselineY = chartTop + plotHeight
        val maxValue = displayPoints.maxOf { it.value }.coerceAtLeast(1.0)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = color(accent)
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        val dotPaint = fillPaint(accent)
        val gridPaint = Paint(dividerPaint).apply { alpha = 55 }

        // Y-axis grid and labels
        val roundedMax = kotlin.math.ceil(maxValue).toInt().coerceAtLeast(1)
        for (i in 0..3) {
            val fraction = i / 3.0
            val tickValue = kotlin.math.round(roundedMax * fraction).toInt()
            val tickFraction = tickValue.toFloat() / roundedMax.toFloat()
            val tickY = baselineY - tickFraction * plotHeight
            val label = tickValue.toString()
            val lw = chartMutedPaint.measureText(label)
            canvas.drawText(label, chartX + yAxisW - 4f - lw, tickY + 3.5f, chartMutedPaint)
            if (i > 0) canvas.drawLine(chartX + yAxisW, tickY, chartX + yAxisW + plotWidth, tickY, gridPaint)
        }

        // Axes
        canvas.drawLine(chartX + yAxisW, chartTop, chartX + yAxisW, baselineY, dividerPaint)
        canvas.drawLine(chartX + yAxisW, baselineY, chartX + yAxisW + plotWidth, baselineY, dividerPaint)

        // Line and dots
        val plotStartX = chartX + yAxisW
        val stepX = if (displayPoints.size == 1) 0f else plotWidth / (displayPoints.size - 1)
        val path = Path()
        displayPoints.forEachIndexed { index, point ->
            val px = plotStartX + stepX * index
            val py = baselineY - ((point.value / maxValue).toFloat() * plotHeight)
            if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
            canvas.drawCircle(px, py, 4f, dotPaint)
        }
        canvas.drawPath(path, linePaint)

        // X-axis labels
        val labelSlotW = if (displayPoints.size == 1) plotWidth else stepX
        displayPoints.forEachIndexed { index, point ->
            val px = plotStartX + stepX * index
            val label = wrapText(point.label, chartMutedPaint, labelSlotW + 2f).firstOrNull().orEmpty()
            val lw = chartMutedPaint.measureText(label)
            canvas.drawText(label, px - lw / 2f, baselineY + 14f, chartMutedPaint)
        }

        y = baselineY + xLabelH + 6f
    }

    private fun drawDonutChart(title: String, segments: List<ProjectStatsChartSegment>, pageTag: String) {
        if (segments.isEmpty()) return
        val total = segments.sumOf { it.value }.coerceAtLeast(1.0)
        val legendH = segments.size * 18f
        ensureSpace(160f + legendH.coerceAtLeast(160f), pageTag)
        drawTextBlock(title, bodyPaint, 16f, leftMargin + 48f, contentWidth - 48f)
        y += 4f

        val chartX = leftMargin + 48f
        val chartWidth = contentWidth - 48f
        val outerRadius = 68f
        val innerRadius = 34f
        // Place donut on the left, legend on the right
        val centerX = chartX + outerRadius + 6f
        val centerY = y + outerRadius + 8f
        var startAngle = -90f

        segments.forEachIndexed { index, segment ->
            val color = donutColor(index, title)
            val sweep = ((segment.value / total) * 360.0).toFloat()
            canvas.drawArc(
                RectF(centerX - outerRadius, centerY - outerRadius, centerX + outerRadius, centerY + outerRadius),
                startAngle, sweep, true, fillPaint(color),
            )
            startAngle += sweep
        }
        canvas.drawCircle(centerX, centerY, innerRadius, paperPaint)

        // Right-side vertical legend
        val legendX = centerX + outerRadius + 16f
        val legendW = chartX + chartWidth - legendX
        val legendStartY = centerY - (segments.size * 18f) / 2f + 9f
        segments.forEachIndexed { index, segment ->
            val ly = legendStartY + index * 18f
            canvas.drawRoundRect(legendX, ly - 7f, legendX + 9f, ly + 1f, 2f, 2f, fillPaint(donutColor(index, title)))
            val pct = "${((segment.value / total) * 100).toInt()}%"
            drawSingleLine("${segment.label}  $pct", chartMutedPaint, legendX + 13f, ly, legendW - 16f)
        }

        val chartBlockH = (centerY + outerRadius + 8f) - (y - 4f)
        y += chartBlockH.coerceAtLeast(legendH) + 8f
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

    private fun drawTextBlock(text: String, paint: Paint, lineHeight: Float, x: Float, width: Float) {
        wrapText(text, paint, width).forEachIndexed { index, line ->
            canvas.drawText(line, x, y + index * lineHeight, paint)
        }
        y += wrapText(text, paint, width).size * lineHeight
    }

    private fun drawSingleLine(text: String, paint: Paint, x: Float, y: Float, width: Float) {
        val line = wrapText(text, paint, width).firstOrNull().orEmpty()
        canvas.drawText(line, x, y, paint)
    }

    private fun drawCenteredText(text: String, paint: Paint, centerX: Float, baselineY: Float, width: Float) {
        val line = wrapText(text, paint, width).firstOrNull().orEmpty()
        val measured = paint.measureText(line)
        canvas.drawText(line, centerX - measured / 2f, baselineY, paint)
    }

    private fun pageSpecForSection(section: ProjectStatsSection): PdfPageSpec {
        val table = section.table ?: return defaultPageSpec
        if (table.pdfStyle != ProjectStatsTablePdfStyle.RapidPullRequestList) return defaultPageSpec
        val renderConfig = buildRapidPullRequestTableConfig(table.headers, table.rows)
        val requiredWidth = (defaultPageSpec.leftMargin + renderConfig.columnWidths.sum() + defaultPageSpec.rightMargin).toInt()
        return defaultPageSpec.copy(width = max(595, requiredWidth))
    }

    private fun buildTableRenderConfig(
        headers: List<String>,
        rows: List<List<String>>,
        columnFractions: List<Float>,
        compact: Boolean,
        pdfStyle: ProjectStatsTablePdfStyle,
    ): TableRenderConfig = when (pdfStyle) {
        ProjectStatsTablePdfStyle.RapidPullRequestList -> buildRapidPullRequestTableConfig(headers, rows)
        ProjectStatsTablePdfStyle.Default -> {
            val tableX = if (compact) leftMargin else leftMargin + 48f
            val tableWidth = if (compact) contentWidth else contentWidth - 48f
            val fractions = if (columnFractions.size == headers.size) columnFractions else List(headers.size) { 1f / headers.size }
            TableRenderConfig(
                x = tableX,
                width = tableWidth,
                columnWidths = fractions.map { tableWidth * it },
                cellPaint = if (compact) smallPaint else bodyPaint,
                cellPadding = if (compact) 6f else 12f,
                cellLineHeight = if (compact) 12f else 15f,
                headerLineHeight = if (compact) 10f else 13f,
                minRowHeight = if (compact) 22f else 34f,
                minHeaderHeight = if (compact) 26f else 34f,
            )
        }
    }

    private fun buildRapidPullRequestTableConfig(
        headers: List<String>,
        rows: List<List<String>>,
    ): TableRenderConfig {
        val prWidth = 0.20f * (595 - defaultPageSpec.leftMargin - defaultPageSpec.rightMargin)
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
            cellPaint = smallPaint,
            cellPadding = 3.5f,
            cellLineHeight = 10.5f,
            headerLineHeight = 9.5f,
            minRowHeight = 0f,
            minHeaderHeight = 0f,
        )
    }

    private fun rapidColumnWidth(
        headers: List<String>,
        rows: List<List<String>>,
        index: Int,
        splitByNewline: Boolean,
    ): Float {
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
            parts.maxOfOrNull { smallPaint.measureText(it.ifBlank { " " }) } ?: 0f
        } ?: 0f
        return measured + 16f
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val result = mutableListOf<String>()
        text.split('\n').forEach { paragraph ->
            val rawWords = paragraph.split(Regex("\\s+")).filter { it.isNotBlank() }
            // Expand words containing '/' that are too wide to fit as-is
            val words = rawWords.flatMap { word ->
                if ('/' in word && paint.measureText(word) > maxWidth) {
                    breakWordAtSlash(word, paint, maxWidth)
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
                if (paint.measureText(candidate) <= maxWidth) {
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

    private fun breakWordAtSlash(word: String, paint: Paint, maxWidth: Float): List<String> {
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
                if (paint.measureText(candidate) <= maxWidth) {
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

    private fun ensureSpace(requiredHeight: Float, pageTag: String) {
        if (y + requiredHeight > pageHeight - bottomMargin) {
            startPage(pageTag)
        }
    }

    private fun startPage(tag: String, pageSpec: PdfPageSpec = currentPageSpec) {
        if (pageNumber > 0) {
            document.finishPage(page)
        }
        currentPageSpec = pageSpec
        currentTag = tag
        pageNumber += 1
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
        y = topMargin
        decoratePage(tag)
    }

    private fun decoratePage(tag: String) {
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), paperPaint)
        drawWatermark()
        canvas.drawRect(0f, 0f, 18f, pageHeight.toFloat(), railPaint)
        val pageNo = "%02d".format(pageNumber)
        canvas.drawText(pageNo, 28f, 42f, pageNoPaint)
        canvas.drawText(tag, 84f, 42f, pageTagPaint)
        canvas.drawLine(84f, 62f, pageWidth - rightMargin, 62f, dividerPaint)
    }

    private fun drawWatermark() {
        val bitmap = watermarkBitmap
        if (bitmap == null) return
        val targetWidth = pageWidth.toFloat()
        val scale = targetWidth / bitmap.width.toFloat()
        val targetHeight = bitmap.height * scale
        val left = (pageWidth - targetWidth) / 2f
        val top = (pageHeight - targetHeight) / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 255 }
        canvas.drawBitmap(bitmap, null, RectF(left, top, left + targetWidth, top + targetHeight), paint)
    }

    private fun fillPaint(rgb: PdfRgb): Paint = fillPaint(rgb, 1f)

    private fun fillPaint(rgb: PdfRgb, alpha: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = color(rgb, alpha)
        style = Paint.Style.FILL
    }

    private fun color(rgb: PdfRgb, alpha: Float = 1f): Int = AndroidColor.argb(
        (alpha * 255).toInt().coerceIn(0, 255),
        rgb.r,
        rgb.g,
        rgb.b,
    )
}
