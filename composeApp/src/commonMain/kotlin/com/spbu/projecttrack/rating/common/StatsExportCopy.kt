package com.spbu.projecttrack.rating.common

import com.spbu.projecttrack.core.settings.localizePluralRuntime
import com.spbu.projecttrack.core.settings.localizeRuntime

/**
 * Localized strings for stats PDF/CSV export and shared stats UI copy.
 * Uses [com.spbu.projecttrack.core.settings.AppRuntimeLocalization] (updated from theme).
 */
object StatsExportCopy {
    fun now(): String = localizeRuntime("Сейчас", "Now")
    fun total(): String = localizeRuntime("всего", "total")
    fun commits(): String = localizeRuntime("Коммиты", "Commits")
    fun issues(): String = localizeRuntime("Issue", "Issue")
    fun pullRequests(): String = localizeRuntime("Pull Requests", "Pull Requests")
    fun rapidPrShort(): String = localizeRuntime("Быстрые PR", "Rapid PRs")
    fun youMarker(): String = localizeRuntime("Вы", "You")
    fun unknown(): String = localizeRuntime("Неизвестно", "Unknown")
    fun noData(): String = localizeRuntime("Нет данных", "No data")
    fun rankInRating(): String = localizeRuntime("место в рейтинге", "rank")
    fun rankInTeam(): String = localizeRuntime("место в команде", "team rank")
    fun scoreLabel(): String = localizeRuntime("Оценка", "Score")
    fun scoreSubtitle(formatted: String): String = "${scoreLabel()}: $formatted"
    fun added(): String = localizeRuntime("Добавлено", "Added")
    fun removed(): String = localizeRuntime("Удалено", "Removed")
    fun lines(): String = localizeRuntime("строк", "lines")
    fun commitListTitle(): String = localizeRuntime("Список коммитов", "Commit list")
    fun openIssuesShort(): String = localizeRuntime("Открытых", "Open")
    fun closedIssuesShort(): String = localizeRuntime("Закрытых", "Closed")
    fun issueListTitle(): String = localizeRuntime("Список Issues", "Issue list")
    fun prListTitle(): String = localizeRuntime("Список Pull Requests", "Pull request list")
    fun issueCreatorsTitle(): String = localizeRuntime("Создатели Issues", "Issue creators")
    fun issueAssigneesTitle(): String = localizeRuntime("Исполнители Issues", "Issue assignees")
    fun labelsTitle(): String = localizeRuntime("Метки", "Labels")
    fun createdIssuesCount(count: Int): String = localizeRuntime(
        "создано $count Issue",
        "created $count ${if (count == 1) "issue" else "issues"}",
    )
    fun closedIssuesCount(count: Int): String = localizeRuntime(
        "закрыто $count Issue",
        "closed $count ${if (count == 1) "issue" else "issues"}",
    )
    fun participant(): String = localizeRuntime("Участник", "Participant")
    fun rankingInRating(): String = localizeRuntime("Место в рейтинге", "Rank in leaderboard")
    fun rankingInTeam(): String = localizeRuntime("Место в команде", "Rank in team")
    fun totalLines(): String = localizeRuntime("Всего строк", "Total lines")
    fun ownershipCaption(): String = localizeRuntime("владение кодом", "code ownership")
    fun ownershipScoreTitle(): String = localizeRuntime("Оценка владения кодом", "Code ownership score")
    fun ownershipSectionTitle(): String = localizeRuntime("Владение кодом", "Code ownership")
    fun ownershipDistributionTitle(): String = localizeRuntime(
        "Распределение владения кодом",
        "Code ownership distribution",
    )
    fun participantsTableTitle(): String = localizeRuntime("Таблица участников", "Participants")
    fun changesCount(count: Int): String = localizeRuntime(
        "$count изменений",
        "$count ${if (count == 1) "change" else "changes"}",
    )
    fun linesNote(additions: Int, deletions: Int, lines: Int): String = localizeRuntime(
        "+$additions/-$deletions  $lines строк",
        "+$additions/-$deletions  $lines ${if (lines == 1) "line" else "lines"}",
    )
    fun mostChangedFile(name: String): String = localizeRuntime(
        "Самый часто изменяемый файл: $name",
        "Most frequently changed file: $name",
    )
    fun fileDistributionTitle(): String = localizeRuntime(
        "Распределение изменений файлов",
        "File change distribution",
    )
    fun openIssuesRow(): String = localizeRuntime("Открытых", "Open")
    fun closedIssuesRow(): String = localizeRuntime("Закрытых", "Closed")
    fun progressRow(): String = localizeRuntime("Прогресс", "Progress")
    fun ratingRow(): String = localizeRuntime("Рейтинг", "Rank")
    fun changedFilesRow(): String = localizeRuntime("Изменено файлов", "Files changed")
    fun changedFilesHeader(): String = localizeRuntime("— Измененные файлы —", "— Changed files —")
    fun changesByParticipantHeader(): String = localizeRuntime(
        "— Кол-во изменений по участникам —",
        "— Changes by participant —",
    )
    fun mostActiveWeekday(): String = localizeRuntime(
        "Самый активный день недели",
        "Most active weekday",
    )
    fun leastActiveWeekday(): String = localizeRuntime(
        "Самый неактивный день недели",
        "Least active weekday",
    )
    fun noDataUpper(): String = localizeRuntime("НЕТ ДАННЫХ", "NO DATA")
    fun filesCount(count: Int): String =
        "$count ${localizePluralRuntime(count, "файл", "файла", "файлов", "file", "files")}"

    fun pdfSaved(fileName: String): String = localizeRuntime(
        "PDF сохранен: $fileName",
        "PDF saved: $fileName",
    )
    fun csvSaved(fileName: String): String = localizeRuntime(
        "CSV сохранен: $fileName",
        "CSV saved: $fileName",
    )
    fun exportPdfFailed(): String = localizeRuntime(
        "Не удалось экспортировать PDF",
        "Failed to export PDF",
    )
    fun exportExcelFailed(): String = localizeRuntime(
        "Не удалось экспортировать Excel",
        "Failed to export Excel",
    )

    fun createdField(): String = localizeRuntime("Создано:", "Created:")
    fun closedField(): String = localizeRuntime("Закрыто:", "Closed:")
    fun assigneesField(): String = localizeRuntime("Исполнители:", "Assignees:")
    fun labelsField(): String = localizeRuntime("Метки:", "Labels:")

    fun issueCountLabel(count: Int): String = localizeRuntime(
        "$count Issue",
        "$count ${if (count == 1) "issue" else "issues"}",
    )

    fun metricScoreTitleForSection(sectionTitle: String): String = when {
        sectionTitle.equals("Быстрые Pull Requests", ignoreCase = true) ||
            sectionTitle.equals("Rapid Pull Requests", ignoreCase = true) ->
            localizeRuntime("оценка быстрых PR", "Rapid PR score")
        else ->
            localizeRuntime(
                "оценка ${sectionTitle.lowercase()}",
                "${sectionTitle.trim()} score",
            )
    }
}
