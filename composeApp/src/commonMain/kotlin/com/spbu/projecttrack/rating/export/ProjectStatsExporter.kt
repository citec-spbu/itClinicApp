package com.spbu.projecttrack.rating.export

import androidx.compose.runtime.Composable
import com.spbu.projecttrack.rating.common.StatsExportCopy

data class ProjectStatsExportPayload(
    val projectId: String,
    val projectName: String,
    val description: String? = null,
    val customerName: String? = null,
    val repositoryUrl: String? = null,
    val periodLabel: String? = null,
    val generatedAtLabel: String? = null,
    val summaryCards: List<ProjectStatsSummaryCard> = emptyList(),
    val members: List<ProjectStatsMemberRow> = emptyList(),
    val sections: List<ProjectStatsSection> = emptyList()
)

data class ProjectStatsSummaryCard(
    val title: String,
    val value: String,
    val subtitle: String? = null
)

data class ProjectStatsMemberRow(
    val name: String,
    val role: String? = null,
)

data class ProjectStatsSection(
    val title: String,
    val subtitle: String? = null,
    val score: Double? = null,
    val rows: List<ProjectStatsTableRow> = emptyList(),
    val table: ProjectStatsTable? = null,
    val chart: ProjectStatsChart? = null,
    val chartFirst: Boolean = false,
    val notes: List<String> = emptyList()
)

data class ProjectStatsTableRow(
    val label: String,
    val value: String,
    val note: String? = null
)

data class ProjectStatsTable(
    val title: String? = null,
    val headers: List<String>,
    val rows: List<List<String>>,
    val columnFractions: List<Float>? = null,
    val pdfStyle: ProjectStatsTablePdfStyle = ProjectStatsTablePdfStyle.Default,
)

enum class ProjectStatsTablePdfStyle {
    Default,
    RapidPullRequestList,
}

sealed interface ProjectStatsChart {
    val title: String

    data class Bar(
        override val title: String,
        val points: List<ProjectStatsChartPoint>,
        val yAxisLabel: String? = null
    ) : ProjectStatsChart

    data class Line(
        override val title: String,
        val points: List<ProjectStatsChartPoint>,
        val yAxisLabel: String? = null
    ) : ProjectStatsChart

    data class Donut(
        override val title: String,
        val segments: List<ProjectStatsChartSegment>
    ) : ProjectStatsChart
}

data class ProjectStatsChartPoint(
    val label: String,
    val value: Double,
    val note: String? = null
)

data class ProjectStatsChartSegment(
    val label: String,
    val value: Double,
    val colorHint: String? = null
)

data class ProjectStatsExportResult(
    val fileName: String,
    val absolutePath: String,
    val mimeType: String
)

interface ProjectStatsExporter {
    suspend fun exportPdf(payload: ProjectStatsExportPayload): Result<ProjectStatsExportResult>
    suspend fun exportExcelCsv(payload: ProjectStatsExportPayload): Result<ProjectStatsExportResult>
}

@Composable
expect fun rememberProjectStatsExporter(): ProjectStatsExporter

internal fun sanitizeProjectStatsFileName(value: String): String {
    val cleaned = value
        .lowercase()
        .replace(Regex("[^a-z0-9а-яё]+"), "_")
        .replace(Regex("_+"), "_")
        .trim('_')

    return if (cleaned.isBlank()) "project_stats" else cleaned
}

internal fun buildProjectStatsReportLines(payload: ProjectStatsExportPayload): List<String> {
    val lines = mutableListOf<String>()
    lines += payload.projectName
    payload.periodLabel?.takeIf { it.isNotBlank() }?.let { lines += "Период: $it" }
    payload.customerName?.takeIf { it.isNotBlank() }?.let { lines += "Заказчик: $it" }
    payload.repositoryUrl?.takeIf { it.isNotBlank() }?.let { lines += "Репозиторий: $it" }
    payload.description?.takeIf { it.isNotBlank() }?.let { lines += "Описание: $it" }
    payload.generatedAtLabel?.takeIf { it.isNotBlank() }?.let { lines += "Сформировано: $it" }

    if (payload.summaryCards.isNotEmpty()) {
        lines += ""
        lines += "Сводка"
        payload.summaryCards.forEach { card ->
            val subtitle = card.subtitle?.takeIf { it.isNotBlank() }
            lines += buildString {
                append("• ")
                append(card.title)
                append(": ")
                append(card.value)
                subtitle?.let {
                    append(" — ")
                    append(it)
                }
            }
        }
    }

    if (payload.members.isNotEmpty()) {
        lines += ""
        lines += "Команда"
        payload.members.forEach { member ->
            val parts = buildList {
                add(member.name)
                member.role?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
            lines += "• " + parts.joinToString(" — ")
        }
    }

    payload.sections.forEach { section ->
        lines += ""
        lines += section.title
        section.subtitle?.takeIf { it.isNotBlank() }?.let { lines += "  $it" }
        section.score?.let { lines += "  ${StatsExportCopy.scoreLabel()}: ${formatChartValue(it)}" }
        section.rows.forEach { row ->
            lines += buildString {
                append("• ")
                append(row.label)
                append(": ")
                append(row.value)
                row.note?.takeIf { it.isNotBlank() }?.let {
                    append(" — ")
                    append(it)
                }
            }
        }
        section.table?.let { table ->
            table.title?.takeIf { it.isNotBlank() }?.let(lines::add)
            lines += "  " + table.headers.joinToString(" | ")
            table.rows.forEach { row ->
                lines += "  " + row.joinToString(" | ")
            }
        }
        section.chart?.let { chart ->
            lines += chart.title
            when (chart) {
                is ProjectStatsChart.Bar -> chart.points.forEach { point ->
                    lines += buildString {
                        append("  • ")
                        append(point.label)
                        append(": ")
                        append(formatChartValue(point.value))
                        point.note?.takeIf { it.isNotBlank() }?.let {
                            append(" — ")
                            append(it)
                        }
                    }
                }
                is ProjectStatsChart.Line -> chart.points.forEach { point ->
                    lines += buildString {
                        append("  • ")
                        append(point.label)
                        append(": ")
                        append(formatChartValue(point.value))
                        point.note?.takeIf { it.isNotBlank() }?.let {
                            append(" — ")
                            append(it)
                        }
                    }
                }
                is ProjectStatsChart.Donut -> chart.segments.forEach { segment ->
                    lines += buildString {
                        append("  • ")
                        append(segment.label)
                        append(": ")
                        append(formatChartValue(segment.value))
                        segment.colorHint?.takeIf { it.isNotBlank() }?.let {
                            append(" — ")
                            append(it)
                        }
                    }
                }
            }
        }
        section.notes.forEach { note ->
            lines += "• $note"
        }
    }

    return lines
}

internal fun buildProjectStatsCsv(payload: ProjectStatsExportPayload): String {
    val lines = mutableListOf<List<String>>()

    fun row(vararg cells: String) = lines.add(cells.toList())
    fun blank()                   = lines.add(emptyList())
    fun header(title: String)     = lines.add(listOf("=== $title ==="))

    // ── Project info ──────────────────────────────────────────────────────────
    header(payload.projectName)
    payload.periodLabel?.takeIf      { it.isNotBlank() }?.let { row("Период", it) }
    payload.customerName?.takeIf     { it.isNotBlank() }?.let { row("Заказчик", it) }
    payload.repositoryUrl?.takeIf    { it.isNotBlank() }?.let { row("Репозиторий", it) }
    payload.description?.takeIf      { it.isNotBlank() }?.let { row("Описание", it) }
    payload.generatedAtLabel?.takeIf { it.isNotBlank() }?.let { row("Сформировано", it) }
    blank()

    // ── Summary cards ─────────────────────────────────────────────────────────
    if (payload.summaryCards.isNotEmpty()) {
        header("СВОДКА")
        row("Показатель", "Значение")
        payload.summaryCards.forEach { card ->
            row(card.title, card.value)
        }
        blank()
    }

    // ── Team ──────────────────────────────────────────────────────────────────
    if (payload.members.isNotEmpty()) {
        header("КОМАНДА")
        row("Участник", "Роль")
        payload.members.forEach { m ->
            row(m.name, m.role.orEmpty())
        }
        blank()
    }

    // ── Data sections ─────────────────────────────────────────────────────────
    payload.sections.forEach { section ->
        header(section.title.uppercase())

        section.score?.let { row(StatsExportCopy.scoreLabel(), formatChartValue(it)) }

        section.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
            row("Подзаголовок", subtitle)
            blank()
        }

        // Metric rows
        if (section.rows.isNotEmpty()) {
            row("Показатель", "Значение", "Комментарий")
            section.rows.forEach { r ->
                row(r.label, r.value, r.note.orEmpty())
            }
        }

        // Chart sub-table
        section.chart?.let { chart ->
            blank()
            when (chart) {
                is ProjectStatsChart.Bar, is ProjectStatsChart.Line -> {
                    val pts = if (chart is ProjectStatsChart.Bar) chart.points else (chart as ProjectStatsChart.Line).points
                    lines.add(listOf("— ${chart.title} —"))
                    row("Дата / Метка", "Значение")
                    pts.forEach { pt ->
                        row(pt.label, formatChartValue(pt.value))
                    }
                }
                is ProjectStatsChart.Donut -> {
                    lines.add(listOf("— ${chart.title} —"))
                    row("Категория", "Значение")
                    chart.segments.forEach { seg ->
                row(seg.label, formatChartValue(seg.value))
                    }
                }
            }
        }

        section.table?.let { table ->
            blank()
            table.title?.takeIf { it.isNotBlank() }?.let { lines.add(listOf("— $it —")) }
            lines.add(table.headers)
            table.rows.forEach { tableRow -> lines.add(tableRow) }
        }

        // Notes
        if (section.notes.isNotEmpty()) {
            blank()
            lines.add(listOf("— Примечания —"))
            section.notes.forEach { note -> row(note) }
        }

        blank()
    }

    return lines.joinToString(separator = "\n") { cells ->
        cells.joinToString(";") { csvCell(it) }
    }
}

private fun csvCell(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return "\"$escaped\""
}

internal fun formatChartValue(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString().replace('.', ',')
