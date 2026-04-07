package com.spbu.projecttrack.rating.export

import androidx.compose.runtime.Composable

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
    val value: String? = null,
    val marker: String? = null
)

data class ProjectStatsSection(
    val title: String,
    val subtitle: String? = null,
    val rows: List<ProjectStatsTableRow> = emptyList(),
    val chart: ProjectStatsChart? = null,
    val notes: List<String> = emptyList()
)

data class ProjectStatsTableRow(
    val label: String,
    val value: String,
    val note: String? = null
)

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
    payload.description?.takeIf { it.isNotBlank() }?.let { lines += "Описание: $it" }
    payload.customerName?.takeIf { it.isNotBlank() }?.let { lines += "Заказчик: $it" }
    payload.repositoryUrl?.takeIf { it.isNotBlank() }?.let { lines += "Репозиторий: $it" }
    payload.periodLabel?.takeIf { it.isNotBlank() }?.let { lines += "Период: $it" }
    payload.generatedAtLabel?.takeIf { it.isNotBlank() }?.let { lines += "Сформировано: $it" }

    if (payload.summaryCards.isNotEmpty()) {
        lines += ""
        lines += "Ключевые показатели"
        payload.summaryCards.forEach { card ->
            val subtitle = card.subtitle?.takeIf { it.isNotBlank() }
            lines += if (subtitle == null) {
                "${card.title}: ${card.value}"
            } else {
                "${card.title}: ${card.value} ($subtitle)"
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
                member.value?.takeIf { it.isNotBlank() }?.let { add(it) }
                member.marker?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
            lines += parts.joinToString(" - ")
        }
    }

    payload.sections.forEach { section ->
        lines += ""
        lines += section.title
        section.subtitle?.takeIf { it.isNotBlank() }?.let { lines += it }
        section.rows.forEach { row ->
            lines += "${row.label}: ${row.value}" + row.note?.let { " ($it)" }.orEmpty()
        }
        section.chart?.let { chart ->
            lines += "График: ${chart.title}"
            when (chart) {
                is ProjectStatsChart.Bar -> chart.points.forEach { point ->
                    lines += "${point.label}: ${point.value}"
                }
                is ProjectStatsChart.Line -> chart.points.forEach { point ->
                    lines += "${point.label}: ${point.value}"
                }
                is ProjectStatsChart.Donut -> chart.segments.forEach { segment ->
                    lines += "${segment.label}: ${segment.value}"
                }
            }
        }
        section.notes.forEach { note ->
            lines += note
        }
    }

    return lines
}

internal fun buildProjectStatsCsv(payload: ProjectStatsExportPayload): String {
    val rows = mutableListOf(
        listOf("section", "title", "label", "value", "note")
    )

    fun addRow(section: String, title: String, label: String, value: String, note: String? = null) {
        rows += listOf(section, title, label, value, note.orEmpty())
    }

    addRow(
        section = "meta",
        title = "project",
        label = "projectId",
        value = payload.projectId
    )
    addRow("meta", "project", "projectName", payload.projectName)
    addRow("meta", "project", "description", payload.description.orEmpty())
    addRow("meta", "project", "customerName", payload.customerName.orEmpty())
    addRow("meta", "project", "repositoryUrl", payload.repositoryUrl.orEmpty())
    addRow("meta", "project", "periodLabel", payload.periodLabel.orEmpty())
    addRow("meta", "project", "generatedAtLabel", payload.generatedAtLabel.orEmpty())

    payload.summaryCards.forEach { card ->
        addRow("summary", card.title, card.title, card.value, card.subtitle)
    }

    payload.members.forEach { member ->
        addRow(
            section = "members",
            title = member.name,
            label = member.role.orEmpty(),
            value = member.value.orEmpty(),
            note = member.marker
        )
    }

    payload.sections.forEach { section ->
        section.rows.forEach { row ->
            addRow("section", section.title, row.label, row.value, row.note)
        }
        when (val chart = section.chart) {
            is ProjectStatsChart.Bar -> chart.points.forEach { point ->
                addRow("chart_bar", chart.title, point.label, point.value.toString(), point.note)
            }
            is ProjectStatsChart.Line -> chart.points.forEach { point ->
                addRow("chart_line", chart.title, point.label, point.value.toString(), point.note)
            }
            is ProjectStatsChart.Donut -> chart.segments.forEach { segment ->
                addRow("chart_donut", chart.title, segment.label, segment.value.toString(), segment.colorHint)
            }
            null -> Unit
        }
        section.notes.forEach { note ->
            addRow("note", section.title, "", note, null)
        }
    }

    return rows.joinToString(separator = "\n") { row ->
        row.joinToString(",") { cell -> csvCell(cell) }
    }
}

private fun csvCell(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return "\"$escaped\""
}
