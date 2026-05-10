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
                member.value?.takeIf { it.isNotBlank() }?.let { add(it) }
                member.marker?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
            lines += "• " + parts.joinToString(" — ")
        }
    }

    payload.sections.forEach { section ->
        lines += ""
        lines += section.title
        section.subtitle?.takeIf { it.isNotBlank() }?.let { lines += "  $it" }
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
        section.chart?.let { chart ->
            lines += "График: ${chart.title}"
            when (chart) {
                is ProjectStatsChart.Bar -> chart.points.forEach { point ->
                    lines += buildString {
                        append("  • ")
                        append(point.label)
                        append(": ")
                        append(point.value)
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
                        append(point.value)
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
                        append(segment.value)
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
    val rows = mutableListOf(
        listOf("Группа", "Блок", "Метрика", "Значение", "Комментарий")
    )

    fun addRow(group: String, block: String, metric: String, value: String, note: String? = null) {
        rows += listOf(group, block, metric, value, note.orEmpty())
    }

    addRow("Контекст", "Проект", "Название", payload.projectName)
    payload.periodLabel?.takeIf { it.isNotBlank() }?.let {
        addRow("Контекст", "Проект", "Период", it)
    }
    payload.customerName?.takeIf { it.isNotBlank() }?.let {
        addRow("Контекст", "Проект", "Заказчик", it)
    }
    payload.repositoryUrl?.takeIf { it.isNotBlank() }?.let {
        addRow("Контекст", "Проект", "Репозиторий", it)
    }

    payload.summaryCards.forEach { card ->
        addRow("Сводка", "Ключевые показатели", card.title, card.value, card.subtitle)
    }

    payload.members.forEach { member ->
        addRow(
            group = "Команда",
            block = member.role.orEmpty().ifBlank { "Участник" },
            metric = member.name,
            value = member.value.orEmpty(),
            note = member.marker
        )
    }

    payload.sections.forEach { section ->
        section.rows.forEach { row ->
            addRow(section.title, "Показатели", row.label, row.value, row.note)
        }
        when (val chart = section.chart) {
            is ProjectStatsChart.Bar -> chart.points.forEach { point ->
                addRow(section.title, chart.title, point.label, point.value.toString(), point.note)
            }
            is ProjectStatsChart.Line -> chart.points.forEach { point ->
                addRow(section.title, chart.title, point.label, point.value.toString(), point.note)
            }
            is ProjectStatsChart.Donut -> chart.segments.forEach { segment ->
                addRow(section.title, chart.title, segment.label, segment.value.toString(), segment.colorHint)
            }
            null -> Unit
        }
        section.notes.forEach { note ->
            addRow(section.title, "Примечания", "Комментарий", note, null)
        }
    }

    return rows.joinToString(separator = "\n") { row ->
        row.joinToString(";") { cell -> csvCell(cell) }
    }
}

private fun csvCell(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return "\"$escaped\""
}
