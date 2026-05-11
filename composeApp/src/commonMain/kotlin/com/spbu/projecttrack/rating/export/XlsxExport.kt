package com.spbu.projecttrack.rating.export

import com.spbu.projecttrack.rating.common.StatsExportCopy
import kotlin.math.roundToInt

private val CRC_TABLE: IntArray = IntArray(256) { n ->
    var c = n
    repeat(8) { c = if (c and 1 != 0) (0xEDB88320.toInt() xor (c ushr 1)) else (c ushr 1) }
    c
}

private fun crc32(data: ByteArray): Int {
    var crc = -1
    for (b in data) crc = (crc ushr 8) xor CRC_TABLE[(crc xor b.toInt()) and 0xFF]
    return crc xor -1
}

private class ZipBuilder {
    private val buf = mutableListOf<Byte>()
    private val cdir = mutableListOf<Byte>()
    private var count = 0

    fun add(name: String, data: ByteArray) {
        val nb = name.encodeToByteArray()
        val crc = crc32(data)
        val localOff = buf.size

        buf.leInt(0x04034b50)
        buf.leShort(20)
        buf.leShort(0)
        buf.leShort(0)
        buf.leShort(0)
        buf.leShort(0)
        buf.leInt(crc)
        buf.leInt(data.size)
        buf.leInt(data.size)
        buf.leShort(nb.size)
        buf.leShort(0)
        buf += nb.asList()
        buf += data.asList()

        cdir.leInt(0x02014b50)
        cdir.leShort(20)
        cdir.leShort(20)
        cdir.leShort(0)
        cdir.leShort(0)
        cdir.leShort(0)
        cdir.leShort(0)
        cdir.leInt(crc)
        cdir.leInt(data.size)
        cdir.leInt(data.size)
        cdir.leShort(nb.size)
        cdir.leShort(0)
        cdir.leShort(0)
        cdir.leShort(0)
        cdir.leShort(0)
        cdir.leInt(0)
        cdir.leInt(localOff)
        cdir += nb.asList()

        count++
    }

    fun build(): ByteArray {
        val cdOff = buf.size
        val cdSize = cdir.size
        cdir.leInt(0x06054b50)
        cdir.leShort(0)
        cdir.leShort(0)
        cdir.leShort(count)
        cdir.leShort(count)
        cdir.leInt(cdSize)
        cdir.leInt(cdOff)
        cdir.leShort(0)
        return (buf + cdir).toByteArray()
    }

    private fun MutableList<Byte>.leInt(v: Int) {
        add((v and 0xFF).toByte())
        add(((v shr 8) and 0xFF).toByte())
        add(((v shr 16) and 0xFF).toByte())
        add(((v shr 24) and 0xFF).toByte())
    }

    private fun MutableList<Byte>.leShort(v: Int) {
        add((v and 0xFF).toByte())
        add(((v shr 8) and 0xFF).toByte())
    }
}

private class SharedStrings {
    private val list = mutableListOf<String>()
    private val index = mutableMapOf<String, Int>()

    fun idx(s: String): Int = index.getOrPut(s) {
        list.add(s)
        list.size - 1
    }

    fun xml(): ByteArray {
        val sb = StringBuilder()
        sb.append(XML_DECL)
        sb.append(
            """<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" """ +
                """count="${list.size}" uniqueCount="${list.size}">""",
        )
        list.forEach { sb.append("""<si><t xml:space="preserve">${xmlEsc(it)}</t></si>""") }
        sb.append("</sst>")
        return sb.toString().encodeToByteArray()
    }
}

private const val STY_NORMAL = 0
private const val STY_HEADER = 1
private const val STY_TITLE = 2
private const val STY_MUTED = 3
private const val STY_SECTION = 4
private const val STY_SCORE_LABEL = 5
private const val STY_SCORE_LOW = 6
private const val STY_SCORE_MID = 7
private const val STY_SCORE_HIGH = 8

private data class XCell(
    val col: Int,
    val text: String,
    val num: Double? = null,
    val style: Int = STY_NORMAL,
)

private class SheetBuilder(private val ss: SharedStrings) {
    private val rows = mutableListOf<List<XCell>?>()
    private val columnWidths = linkedMapOf<Int, Double>()
    private val autoColumnWidths = linkedMapOf<Int, Double>()
    private var maxCol = 0

    fun row(vararg cells: XCell) {
        cells.forEach { cell ->
            maxCol = maxOf(maxCol, cell.col)
            autoColumnWidths[cell.col] = maxOf(
                autoColumnWidths[cell.col] ?: 0.0,
                estimateCellWidth(cell),
            )
        }
        rows.add(cells.toList())
    }

    fun blank() {
        rows.add(null)
    }

    fun strRow(style: Int, vararg texts: String) {
        row(*texts.mapIndexed { index, text -> XCell(index + 1, text, style = style) }.toTypedArray())
    }

    fun setColumnWidth(col: Int, width: Double) {
        columnWidths[col] = width
    }

    fun setColumnWidths(widths: List<Double>) {
        widths.forEachIndexed { index, width -> setColumnWidth(index + 1, width) }
    }

    fun nextRowNumber(): Int = rows.size + 1

    fun currentRowNumber(): Int = rows.size

    fun maxUsedCol(): Int = maxCol

    fun xml(hasDrawing: Boolean): ByteArray {
        val sb = StringBuilder()
        sb.append(XML_DECL)
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"")
        if (hasDrawing) {
            sb.append(" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"")
        }
        sb.append(">")

        val totalCols = maxOf(maxCol, columnWidths.keys.maxOrNull() ?: 0)
        if (totalCols > 0) {
            sb.append("<cols>")
            for (col in 1..totalCols) {
                val width = resolveColumnWidth(col)
                sb.append("""<col min="$col" max="$col" width="$width" customWidth="1"/>""")
            }
            sb.append("</cols>")
        }

        sb.append("<sheetData>")
        rows.forEachIndexed { index, cells ->
            if (cells.isNullOrEmpty()) return@forEachIndexed
            val rowNum = index + 1
            sb.append("""<row r="$rowNum">""")
            cells.forEach { cell ->
                val ref = "${colLetter(cell.col)}$rowNum"
                val styleAttr = if (cell.style != STY_NORMAL) """ s="${cell.style}"""" else ""
                when {
                    cell.num != null -> {
                        val value = if (cell.num % 1.0 == 0.0) cell.num.toLong().toString() else cell.num.toString()
                        sb.append("""<c r="$ref"$styleAttr><v>$value</v></c>""")
                    }

                    cell.text.isNotBlank() -> {
                        sb.append("""<c r="$ref" t="s"$styleAttr><v>${ss.idx(cell.text)}</v></c>""")
                    }
                }
            }
            sb.append("</row>")
        }
        sb.append("</sheetData>")
        if (hasDrawing) {
            sb.append("""<drawing r:id="rId1"/>""")
        }
        sb.append("</worksheet>")
        return sb.toString().encodeToByteArray()
    }

    private fun resolveColumnWidth(col: Int): Double {
        val manual = columnWidths[col]
        val auto = autoColumnWidths[col] ?: 10.0
        return manual?.coerceAtLeast(auto) ?: auto
    }

    private fun estimateCellWidth(cell: XCell): Double {
        val source = when {
            cell.text.isNotBlank() -> cell.text
            cell.num != null -> formatChartValue(cell.num)
            else -> ""
        }
        val longestLine = source
            .split('\n')
            .maxOfOrNull { it.trim().length }
            ?: 0
        return (longestLine + 2)
            .coerceAtLeast(10)
            .toDouble()
    }
}

private const val XML_DECL = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>"""

private val RELS_XML: ByteArray = (
    XML_DECL +
        """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
        """<Relationship Id="rId1" """ +
        """Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" """ +
        """Target="xl/workbook.xml"/>""" +
        """</Relationships>"""
    ).encodeToByteArray()

private val STYLES_XML: ByteArray = (
    XML_DECL + """
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="9">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="15"/><color rgb="FF9F2D20"/><name val="Calibri"/></font>
    <font><i/><sz val="10"/><color rgb="FF767678"/><name val="Calibri"/></font>
    <font><b/><sz val="12"/><color rgb="FF9F2D20"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FF9F2D20"/><name val="Calibri"/></font>
    <font><b/><sz val="12"/><color rgb="FF9F2D20"/><name val="Calibri"/></font>
    <font><b/><sz val="12"/><color rgb="FF9F9220"/><name val="Calibri"/></font>
    <font><b/><sz val="12"/><color rgb="FF209F31"/><name val="Calibri"/></font>
  </fonts>
  <fills count="5">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFF5EEED"/><bgColor indexed="64"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFF8F2F1"/><bgColor indexed="64"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFF9F4E0"/><bgColor indexed="64"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFE4E4E6"/></left>
      <right style="thin"><color rgb="FFE4E4E6"/></right>
      <top style="thin"><color rgb="FFE4E4E6"/></top>
      <bottom style="thin"><color rgb="FFE4E4E6"/></bottom>
      <diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="9">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
    <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>
    <xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1"/>
    <xf numFmtId="0" fontId="4" fillId="3" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
    <xf numFmtId="0" fontId="5" fillId="4" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
    <xf numFmtId="0" fontId="6" fillId="0" borderId="0" xfId="0" applyFont="1"/>
    <xf numFmtId="0" fontId="7" fillId="0" borderId="0" xfId="0" applyFont="1"/>
    <xf numFmtId="0" fontId="8" fillId="0" borderId="0" xfId="0" applyFont="1"/>
  </cellXfs>
</styleSheet>
""".trimIndent()
    ).encodeToByteArray()

private enum class ChartKind {
    BAR,
    LINE,
    PIE,
}

private data class SheetChartSpec(
    val kind: ChartKind,
    val title: String,
    val labels: List<String>,
    val values: List<Double>,
    val categoryColumn: Int,
    val valueColumn: Int,
    val firstDataRow: Int,
    val lastDataRow: Int,
    val anchorStartCol: Int,
    val anchorEndCol: Int,
    val anchorStartRow: Int = 1,
    val anchorEndRow: Int = 19,
)

private data class RenderedSheet(
    val name: String,
    val xml: ByteArray,
    val chartSpec: SheetChartSpec? = null,
)

private data class ChartPart(
    val sheetIndex: Int,
    val drawingIndex: Int,
    val chartIndex: Int,
    val drawingXml: ByteArray,
    val drawingRelXml: ByteArray,
    val chartXml: ByteArray,
    val sheetRelXml: ByteArray,
)

private fun contentTypesXml(
    sheetCount: Int,
    drawingCount: Int,
    chartCount: Int,
): ByteArray {
    val sb = StringBuilder(XML_DECL)
    sb.append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
    sb.append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
    sb.append("""<Default Extension="xml" ContentType="application/xml"/>""")
    sb.append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
    for (sheet in 1..sheetCount) {
        sb.append("""<Override PartName="/xl/worksheets/sheet$sheet.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
    }
    for (drawing in 1..drawingCount) {
        sb.append("""<Override PartName="/xl/drawings/drawing$drawing.xml" ContentType="application/vnd.openxmlformats-officedocument.drawing+xml"/>""")
    }
    for (chart in 1..chartCount) {
        sb.append("""<Override PartName="/xl/charts/chart$chart.xml" ContentType="application/vnd.openxmlformats-officedocument.drawingml.chart+xml"/>""")
    }
    sb.append("""<Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>""")
    sb.append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
    sb.append("</Types>")
    return sb.toString().encodeToByteArray()
}

private fun workbookXml(sheets: List<String>): ByteArray {
    val sb = StringBuilder(XML_DECL)
    sb.append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" """)
    sb.append("""xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
    sb.append("<sheets>")
    sheets.forEachIndexed { index, name ->
        sb.append("""<sheet name="${xmlEsc(name)}" sheetId="${index + 1}" r:id="rId${index + 1}"/>""")
    }
    sb.append("</sheets></workbook>")
    return sb.toString().encodeToByteArray()
}

private fun workbookRelsXml(sheetCount: Int): ByteArray {
    val sb = StringBuilder(XML_DECL)
    sb.append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
    for (sheet in 1..sheetCount) {
        sb.append("""<Relationship Id="rId$sheet" """)
        sb.append("""Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" """)
        sb.append("""Target="worksheets/sheet$sheet.xml"/>""")
    }
    sb.append("""<Relationship Id="rId${sheetCount + 1}" """)
    sb.append("""Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" """)
    sb.append("""Target="sharedStrings.xml"/>""")
    sb.append("""<Relationship Id="rId${sheetCount + 2}" """)
    sb.append("""Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" """)
    sb.append("""Target="styles.xml"/>""")
    sb.append("</Relationships>")
    return sb.toString().encodeToByteArray()
}

private fun buildSummarySheet(payload: ProjectStatsExportPayload, ss: SharedStrings): RenderedSheet {
    val s = SheetBuilder(ss)

    s.row(XCell(1, payload.projectName, style = STY_TITLE))
    val meta = buildList {
        payload.periodLabel?.takeIf { it.isNotBlank() }?.let { add("Период: $it") }
        payload.customerName?.takeIf { it.isNotBlank() }?.let { add("Заказчик: $it") }
        payload.generatedAtLabel?.takeIf { it.isNotBlank() }?.let { add("Сформировано: $it") }
    }
    if (meta.isNotEmpty()) {
        s.row(XCell(1, meta.joinToString("   ·   "), style = STY_MUTED))
    }
    payload.repositoryUrl?.takeIf { it.isNotBlank() }?.let {
        s.row(XCell(1, "Репозиторий: $it", style = STY_MUTED))
    }
    payload.description?.takeIf { it.isNotBlank() }?.let {
        s.row(XCell(1, it, style = STY_MUTED))
    }
    s.blank()

    if (payload.summaryCards.isNotEmpty()) {
        s.row(XCell(1, "Сводка", style = STY_SECTION))
        s.strRow(STY_HEADER, "Показатель", "Значение")
        payload.summaryCards.forEach { card ->
            s.row(
                XCell(1, card.title),
                XCell(2, card.value),
            )
        }
        s.blank()
    }

    if (payload.members.isNotEmpty()) {
        s.row(XCell(1, "Команда", style = STY_SECTION))
        s.strRow(STY_HEADER, "Участник", "Роль")
        payload.members.forEach { member ->
            s.row(
                XCell(1, member.name),
                XCell(2, member.role.orEmpty()),
            )
        }
    }

    return RenderedSheet(
        name = "Сводка",
        xml = s.xml(hasDrawing = false),
    )
}

private fun buildSectionSheet(
    sheetName: String,
    section: ProjectStatsSection,
    ss: SharedStrings,
): RenderedSheet {
    val s = SheetBuilder(ss)

    s.row(XCell(1, section.title, style = STY_TITLE))
    section.subtitle?.takeIf { it.isNotBlank() }?.let {
        s.row(XCell(1, it, style = STY_MUTED))
    }
    section.score?.let { score ->
        s.row(
            XCell(1, StatsExportCopy.scoreLabel(), style = STY_SCORE_LABEL),
            XCell(2, formatScoreValueForSheet(score), style = scoreStyle(score)),
        )
    }

    fun appendRowsBlock() {
        s.blank()
        s.strRow(STY_HEADER, "Показатель", "Значение", "Комментарий")
        section.rows.forEach { row ->
            s.row(
                XCell(1, row.label),
                XCell(2, row.value),
                XCell(3, row.note.orEmpty()),
            )
        }
    }

    fun appendTableBlock() {
        val table = section.table ?: return
        s.blank()
        table.title?.takeIf { it.isNotBlank() }?.let {
            s.row(XCell(1, it, style = STY_SECTION))
        }
        if (table.headers.isNotEmpty()) {
            s.strRow(STY_HEADER, *table.headers.toTypedArray())
            table.rows.forEach { tableRow ->
                s.row(
                    *tableRow.mapIndexed { index, value ->
                        XCell(index + 1, resolveLinkCell(value))
                    }.toTypedArray(),
                )
            }
        }
    }

    fun appendChartBlock(): SheetChartSpec? {
        val chart = section.chart ?: return null

        val labels: List<String>
        val values: List<Double>
        val chartKind: ChartKind
        val header1: String
        val header2: String
        val header3: String?

        when (chart) {
            is ProjectStatsChart.Bar -> {
                labels = chart.points.map { it.label }
                values = chart.points.map { it.value }
                chartKind = ChartKind.BAR
                header1 = "Дата / Метка"
                header2 = "Количество"
                header3 = null
            }

            is ProjectStatsChart.Line -> {
                labels = chart.points.map { it.label }
                values = chart.points.map { it.value }
                chartKind = ChartKind.LINE
                header1 = "Дата / Метка"
                header2 = "Количество"
                header3 = null
            }

            is ProjectStatsChart.Donut -> {
                labels = chart.segments.map { it.label }
                values = chart.segments.map { it.value }
                chartKind = ChartKind.PIE
                header1 = "Категория"
                header2 = "Значение"
                header3 = "Подпись"
            }
        }

        if (labels.isEmpty() || values.isEmpty()) return null

        s.blank()
        s.row(XCell(1, chart.title, style = STY_SECTION))
        if (header3 == null) {
            s.strRow(STY_HEADER, header1, header2)
        } else {
            s.strRow(STY_HEADER, header1, header2, header3)
        }
        val firstDataRow = s.nextRowNumber()
        when (chart) {
            is ProjectStatsChart.Bar -> {
                chart.points.forEach { point ->
                    s.row(
                        XCell(1, point.label),
                        XCell(2, formatChartValue(point.value), num = point.value),
                    )
                }
            }

            is ProjectStatsChart.Line -> {
                chart.points.forEach { point ->
                    s.row(
                        XCell(1, point.label),
                        XCell(2, formatChartValue(point.value), num = point.value),
                    )
                }
            }

            is ProjectStatsChart.Donut -> {
                chart.segments.forEach { segment ->
                    s.row(
                        XCell(1, segment.label),
                        XCell(2, formatChartValue(segment.value), num = segment.value),
                        XCell(3, segment.colorHint.orEmpty()),
                    )
                }
            }
        }

        val startCol = (s.maxUsedCol() + 2).coerceAtLeast(5)
        return SheetChartSpec(
            kind = chartKind,
            title = chart.title,
            labels = labels,
            values = values,
            categoryColumn = 1,
            valueColumn = 2,
            firstDataRow = firstDataRow,
            lastDataRow = s.currentRowNumber(),
            anchorStartCol = startCol,
            anchorEndCol = startCol + 7,
        )
    }

    val chartSpec = if (section.chartFirst) {
        val chart = appendChartBlock()
        if (section.rows.isNotEmpty()) appendRowsBlock()
        if (section.table != null) appendTableBlock()
        chart
    } else {
        if (section.rows.isNotEmpty()) appendRowsBlock()
        if (section.table != null) appendTableBlock()
        appendChartBlock()
    }

    if (section.notes.isNotEmpty()) {
        s.blank()
        s.row(XCell(1, "Примечания", style = STY_SECTION))
        section.notes.forEach { note ->
            s.row(XCell(1, note))
        }
    }

    return RenderedSheet(
        name = sheetName,
        xml = s.xml(hasDrawing = chartSpec != null),
        chartSpec = chartSpec,
    )
}

internal fun buildProjectStatsXlsx(payload: ProjectStatsExportPayload): ByteArray {
    val ss = SharedStrings()
    val zip = ZipBuilder()

    val renderedSheets = mutableListOf<RenderedSheet>()
    renderedSheets += buildSummarySheet(payload, ss)

    val usedNames = mutableSetOf("Сводка")
    payload.sections.forEach { section ->
        val baseName = xlsxSheetName(section.title)
        var name = baseName
        var suffix = 2
        while (name in usedNames) {
            val prefix = baseName.take(28).trimEnd()
            name = "$prefix $suffix".take(31)
            suffix++
        }
        usedNames += name
        renderedSheets += buildSectionSheet(name, section, ss)
    }

    val chartParts = buildList {
        var nextChartIndex = 1
        renderedSheets.forEachIndexed { index, sheet ->
            val spec = sheet.chartSpec ?: return@forEachIndexed
            add(
                buildChartPart(
                    sheetIndex = index + 1,
                    drawingIndex = nextChartIndex,
                    chartIndex = nextChartIndex,
                    sheetName = sheet.name,
                    spec = spec,
                ),
            )
            nextChartIndex += 1
        }
    }

    zip.add(
        "[Content_Types].xml",
        contentTypesXml(
            sheetCount = renderedSheets.size,
            drawingCount = chartParts.size,
            chartCount = chartParts.size,
        ),
    )
    zip.add("_rels/.rels", RELS_XML)
    zip.add("xl/workbook.xml", workbookXml(renderedSheets.map { it.name }))
    zip.add("xl/_rels/workbook.xml.rels", workbookRelsXml(renderedSheets.size))
    zip.add("xl/styles.xml", STYLES_XML)

    renderedSheets.forEachIndexed { index, sheet ->
        zip.add("xl/worksheets/sheet${index + 1}.xml", sheet.xml)
    }

    chartParts.forEach { part ->
        zip.add("xl/worksheets/_rels/sheet${part.sheetIndex}.xml.rels", part.sheetRelXml)
        zip.add("xl/drawings/drawing${part.drawingIndex}.xml", part.drawingXml)
        zip.add("xl/drawings/_rels/drawing${part.drawingIndex}.xml.rels", part.drawingRelXml)
        zip.add("xl/charts/chart${part.chartIndex}.xml", part.chartXml)
    }

    zip.add("xl/sharedStrings.xml", ss.xml())
    return zip.build()
}

private fun buildChartPart(
    sheetIndex: Int,
    drawingIndex: Int,
    chartIndex: Int,
    sheetName: String,
    spec: SheetChartSpec,
): ChartPart {
    return ChartPart(
        sheetIndex = sheetIndex,
        drawingIndex = drawingIndex,
        chartIndex = chartIndex,
        drawingXml = drawingXml(drawingIndex, spec),
        drawingRelXml = drawingRelXml(chartIndex),
        chartXml = chartXml(chartIndex, sheetName, spec),
        sheetRelXml = sheetRelXml(drawingIndex),
    )
}

private fun sheetRelXml(drawingIndex: Int): ByteArray = (
    XML_DECL +
        """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
        """<Relationship Id="rId1" """ +
        """Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing" """ +
        """Target="../drawings/drawing$drawingIndex.xml"/>""" +
        """</Relationships>"""
    ).encodeToByteArray()

private fun drawingRelXml(chartIndex: Int): ByteArray = (
    XML_DECL +
        """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
        """<Relationship Id="rId1" """ +
        """Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/chart" """ +
        """Target="../charts/chart$chartIndex.xml"/>""" +
        """</Relationships>"""
    ).encodeToByteArray()

private fun drawingXml(
    drawingIndex: Int,
    spec: SheetChartSpec,
): ByteArray {
    val fromCol = spec.anchorStartCol - 1
    val toCol = spec.anchorEndCol - 1
    val fromRow = spec.anchorStartRow
    val toRow = spec.anchorEndRow
    return (
        XML_DECL + """
<xdr:wsDr xmlns:xdr="http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing"
          xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
          xmlns:c="http://schemas.openxmlformats.org/drawingml/2006/chart"
          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <xdr:twoCellAnchor editAs="oneCell">
    <xdr:from>
      <xdr:col>$fromCol</xdr:col>
      <xdr:colOff>0</xdr:colOff>
      <xdr:row>$fromRow</xdr:row>
      <xdr:rowOff>0</xdr:rowOff>
    </xdr:from>
    <xdr:to>
      <xdr:col>$toCol</xdr:col>
      <xdr:colOff>0</xdr:colOff>
      <xdr:row>$toRow</xdr:row>
      <xdr:rowOff>0</xdr:rowOff>
    </xdr:to>
    <xdr:graphicFrame macro="">
      <xdr:nvGraphicFramePr>
        <xdr:cNvPr id="${drawingIndex + 1}" name="Chart ${drawingIndex + 1}"/>
        <xdr:cNvGraphicFramePr/>
      </xdr:nvGraphicFramePr>
      <xdr:xfrm>
        <a:off x="0" y="0"/>
        <a:ext cx="0" cy="0"/>
      </xdr:xfrm>
      <a:graphic>
        <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/chart">
          <c:chart r:id="rId1"/>
        </a:graphicData>
      </a:graphic>
    </xdr:graphicFrame>
    <xdr:clientData/>
  </xdr:twoCellAnchor>
</xdr:wsDr>
""".trimIndent()
        ).encodeToByteArray()
}

private fun chartXml(
    chartIndex: Int,
    sheetName: String,
    spec: SheetChartSpec,
): ByteArray {
    val categoryFormula = sheetRangeRef(sheetName, spec.categoryColumn, spec.firstDataRow, spec.lastDataRow)
    val valueFormula = sheetRangeRef(sheetName, spec.valueColumn, spec.firstDataRow, spec.lastDataRow)
    val series = chartSeriesXml(
        index = 0,
        labels = spec.labels,
        values = spec.values,
        categoryFormula = categoryFormula,
        valueFormula = valueFormula,
    )
    val plotArea = when (spec.kind) {
        ChartKind.BAR -> barPlotAreaXml(chartIndex, series)
        ChartKind.LINE -> linePlotAreaXml(chartIndex, series)
        ChartKind.PIE -> piePlotAreaXml(series)
    }
    return (
        XML_DECL + """
<c:chartSpace xmlns:c="http://schemas.openxmlformats.org/drawingml/2006/chart"
              xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
              xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <c:lang val="ru-RU"/>
  <c:style val="10"/>
  <c:chart>
    <c:title>
      <c:tx>
        <c:rich>
          <a:bodyPr/>
          <a:lstStyle/>
          <a:p>
            <a:r>
              <a:rPr lang="ru-RU" sz="1200" b="1"/>
              <a:t>${xmlEsc(spec.title)}</a:t>
            </a:r>
          </a:p>
        </c:rich>
      </c:tx>
      <c:layout/>
      <c:overlay val="0"/>
    </c:title>
    $plotArea
    ${chartLegendXml(spec.kind)}
    <c:plotVisOnly val="1"/>
  </c:chart>
</c:chartSpace>
""".trimIndent()
        ).encodeToByteArray()
}

private fun barPlotAreaXml(chartIndex: Int, series: String): String {
    val catAxisId = 40_000 + chartIndex * 2
    val valueAxisId = catAxisId + 1
    return """
<c:plotArea>
  <c:layout/>
  <c:barChart>
    <c:barDir val="col"/>
    <c:grouping val="clustered"/>
    <c:varyColors val="0"/>
    $series
    <c:axId val="$catAxisId"/>
    <c:axId val="$valueAxisId"/>
  </c:barChart>
  <c:catAx>
    <c:axId val="$catAxisId"/>
    <c:scaling><c:orientation val="minMax"/></c:scaling>
    <c:delete val="0"/>
    <c:axPos val="b"/>
    <c:tickLblPos val="nextTo"/>
    <c:crossAx val="$valueAxisId"/>
    <c:crosses val="autoZero"/>
    <c:auto val="1"/>
    <c:lblAlgn val="ctr"/>
    <c:lblOffset val="100"/>
  </c:catAx>
  <c:valAx>
    <c:axId val="$valueAxisId"/>
    <c:scaling><c:orientation val="minMax"/></c:scaling>
    <c:delete val="0"/>
    <c:axPos val="l"/>
    <c:majorGridlines/>
    <c:numFmt formatCode="General" sourceLinked="1"/>
    <c:tickLblPos val="nextTo"/>
    <c:crossAx val="$catAxisId"/>
    <c:crosses val="autoZero"/>
    <c:crossBetween val="between"/>
  </c:valAx>
</c:plotArea>
""".trimIndent()
}

private fun linePlotAreaXml(chartIndex: Int, series: String): String {
    val catAxisId = 60_000 + chartIndex * 2
    val valueAxisId = catAxisId + 1
    return """
<c:plotArea>
  <c:layout/>
  <c:lineChart>
    <c:grouping val="standard"/>
    <c:varyColors val="0"/>
    $series
    <c:smooth val="0"/>
    <c:axId val="$catAxisId"/>
    <c:axId val="$valueAxisId"/>
  </c:lineChart>
  <c:catAx>
    <c:axId val="$catAxisId"/>
    <c:scaling><c:orientation val="minMax"/></c:scaling>
    <c:delete val="0"/>
    <c:axPos val="b"/>
    <c:tickLblPos val="nextTo"/>
    <c:crossAx val="$valueAxisId"/>
    <c:crosses val="autoZero"/>
    <c:auto val="1"/>
    <c:lblAlgn val="ctr"/>
    <c:lblOffset val="100"/>
  </c:catAx>
  <c:valAx>
    <c:axId val="$valueAxisId"/>
    <c:scaling><c:orientation val="minMax"/></c:scaling>
    <c:delete val="0"/>
    <c:axPos val="l"/>
    <c:majorGridlines/>
    <c:numFmt formatCode="General" sourceLinked="1"/>
    <c:tickLblPos val="nextTo"/>
    <c:crossAx val="$catAxisId"/>
    <c:crosses val="autoZero"/>
    <c:crossBetween val="between"/>
  </c:valAx>
</c:plotArea>
""".trimIndent()
}

private fun piePlotAreaXml(series: String): String = """
<c:plotArea>
  <c:layout/>
  <c:pieChart>
    <c:varyColors val="1"/>
    $series
  </c:pieChart>
</c:plotArea>
""".trimIndent()

private fun chartLegendXml(kind: ChartKind): String = when (kind) {
    ChartKind.PIE -> """
<c:legend>
  <c:legendPos val="r"/>
  <c:layout/>
  <c:overlay val="0"/>
</c:legend>
""".trimIndent()
    ChartKind.BAR, ChartKind.LINE -> ""
}

private fun chartSeriesXml(
    index: Int,
    labels: List<String>,
    values: List<Double>,
    categoryFormula: String,
    valueFormula: String,
): String = """
<c:ser>
  <c:idx val="$index"/>
  <c:order val="$index"/>
  <c:cat>
    <c:strRef>
      <c:f>${xmlEsc(categoryFormula)}</c:f>
      <c:strCache>
        <c:ptCount val="${labels.size}"/>
        ${labels.mapIndexed { pointIndex, label -> """<c:pt idx="$pointIndex"><c:v>${xmlEsc(label)}</c:v></c:pt>""" }.joinToString("")}
      </c:strCache>
    </c:strRef>
  </c:cat>
  <c:val>
    <c:numRef>
      <c:f>${xmlEsc(valueFormula)}</c:f>
      <c:numCache>
        <c:formatCode>General</c:formatCode>
        <c:ptCount val="${values.size}"/>
        ${values.mapIndexed { pointIndex, value -> """<c:pt idx="$pointIndex"><c:v>$value</c:v></c:pt>""" }.joinToString("")}
      </c:numCache>
    </c:numRef>
  </c:val>
</c:ser>
""".trimIndent()

private fun scoreStyle(score: Double): Int = when {
    score < 3.0 -> STY_SCORE_LOW
    score < 4.0 -> STY_SCORE_MID
    else -> STY_SCORE_HIGH
}

private fun formatScoreValueForSheet(score: Double): String {
    val rounded = (score * 10.0).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString().replace('.', ',')
    }
}

private fun sheetRangeRef(sheetName: String, col: Int, firstRow: Int, lastRow: Int): String {
    val escapedName = sheetName.replace("'", "''")
    return "'$escapedName'!\$${colLetter(col)}\$$firstRow:\$${colLetter(col)}\$$lastRow"
}

private fun colLetter(col: Int): String {
    var current = col
    var result = ""
    while (current > 0) {
        val remainder = (current - 1) % 26
        result = ('A' + remainder).toString() + result
        current = (current - 1) / 26
    }
    return result
}

private fun xmlEsc(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")

private fun xlsxSheetName(raw: String): String {
    val cleaned = raw.replace(Regex("[/\\\\?*\\[\\]:]"), " ").trim()
    return cleaned.take(31).trim().ifBlank { "Лист" }
}

/** Strips "@LINK:url|display" encoding — XLSX shows the raw URL so Excel auto-hyperlinks it. */
internal fun resolveLinkCell(text: String): String {
    if (!text.startsWith("@LINK:")) return text
    return text.removePrefix("@LINK:").substringBefore("|")
}
