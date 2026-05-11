package com.spbu.projecttrack.rating.common

import com.spbu.projecttrack.core.settings.localizePluralRuntime
import com.spbu.projecttrack.core.settings.localizeRuntime

object StatsDetailCopy {
    fun commitsCount(count: Int): String =
        localizePluralRuntime(count, "коммит", "коммита", "коммитов", "commit", "commits")

    fun rapidPrInChart(count: Int): String =
        localizePluralRuntime(count, "быстрый PR", "быстрых PR", "быстрых PR", "rapid PR", "rapid PRs")

    fun commitsChartTitle(): String = localizeRuntime("График коммитов", "Commits chart")
    fun avgCommitsPerDayCaption(): String =
        localizeRuntime("средн. количество коммитов в день", "avg. commits per day")
    fun linesAddedCaption(): String = localizeRuntime("добавлено строк", "lines added")
    fun linesRemovedCaption(): String = localizeRuntime("удалено строк", "lines removed")
    fun lineChangesChartTitle(): String = localizeRuntime("График изменения строк", "Line changes chart")
    fun maxCommitsPerDayCaption(): String = localizeRuntime("макс. коммитов / день", "max commits / day")
    fun minCommitsPerDayCaption(): String = localizeRuntime("мин. коммитов / день", "min commits / day")
    fun top10FilesTitle(): String = localizeRuntime("Топ-10 изменяемых файлов", "Top 10 changed files")
    fun commitCountTableTitle(): String = localizeRuntime("Количество коммитов", "Commit count")
    fun valueColumnHeader(): String = localizeRuntime("Значение", "Value")
    fun commitScoreTitle(): String = localizeRuntime("оценка коммитов", "Commits score")

    fun issuesTotalCaption(): String = localizeRuntime("всего Issue", "total issues")
    fun issueAvgLifetimeCaption(): String = localizeRuntime("ср. время жизни Issue", "avg. issue lifetime")
    fun openIssuesCaption(): String = localizeRuntime("открытых Issue", "open issues")
    fun closedIssuesCaption(): String = localizeRuntime("закрытых Issue", "closed issues")
    fun assignedIssueCountTitle(): String =
        localizeRuntime("Количество назначенных Issue", "Assigned issues count")
    fun openClosedSubtitle(): String = localizeRuntime("(открытые/закрытые)", "(open/closed)")
    fun issuesListTitle(): String = localizeRuntime("Список Issues", "Issue list")
    fun viewAll(): String = localizeRuntime("Смотреть все", "View all")
    fun issueScoreTitle(): String = localizeRuntime("оценка Issue", "Issue score")

    fun issueNoDetails(): String =
        localizeRuntime("Подробной информации по Issue нет", "No issue details available")
    fun issueNoActive(): String = localizeRuntime("Нет активных Issue", "No active issues")
    fun issueCloseMore(count: Int): String = localizeRuntime(
        "Закройте еще $count Issue",
        "Close $count more ${if (count == 1) "issue" else "issues"}",
    )
    fun issuesAllClosed(): String = localizeRuntime("Все Issue закрыты", "All issues are closed")

    fun pullRequestsChartTitle(): String = localizeRuntime("График Pull Requests", "Pull requests chart")
    fun pullRequestsChartTooltip(): String = localizeRuntime("Pull Requests", "Pull requests")
    fun prChartHintCount(count: Int): String = localizeRuntime(
        "$count PR",
        "$count ${if (count == 1) "PR" else "PRs"}",
    )
    fun prAvgLifetimeCaption(): String =
        localizeRuntime("среднее время жизни Pull Request", "avg. pull request lifetime")
    fun totalPullRequestsCaption(): String = localizeRuntime("всего Pull Request", "total pull requests")
    fun pullRequestCountTitle(): String = localizeRuntime("Количество Pull Request", "Pull request count")
    fun prMinLifetimeCaption(): String =
        localizeRuntime("минимальное время жизни Pull Request", "min. pull request lifetime")
    fun prMaxLifetimeCaption(): String =
        localizeRuntime("максимальное время жизни Pull Request", "max. pull request lifetime")
    fun prLifetimeDistributionTitle(): String =
        localizeRuntime("Распределение времени жизни PR", "PR lifetime distribution")
    fun prMostRelevantLifetimeCaption(): String =
        localizeRuntime("самое актуальное время жизни PR", "most common PR lifetime")
    fun pullRequestsListTitle(): String = localizeRuntime("Список Pull Requests", "Pull request list")
    fun top5FastestPr(): String = localizeRuntime("Топ-5 самых быстрых PR", "Top 5 fastest PRs")
    fun top5SlowestPr(): String = localizeRuntime("Топ-5 самых медленных PR", "Top 5 slowest PRs")
    fun pullRequestScoreTitle(): String = localizeRuntime("оценка Pull Request", "Pull request score")

    fun rapidPrChartTitle(): String = localizeRuntime("График быстрых PR", "Rapid PR chart")
    fun rapidPrTooltip(): String = localizeRuntime("Быстрые PR", "Rapid PRs")
    fun rapidPrCaption(): String = localizeRuntime("быстрых PR", "rapid PRs")
    fun rapidPullRequestCountTitle(): String =
        localizeRuntime("Количество быстрых Pull Request", "Rapid pull request count")
    fun rapidPullRequestsListTitle(): String =
        localizeRuntime("Список быстрых Pull Requests", "Rapid pull request list")
    fun rapidPrShareCaption(): String = localizeRuntime("доля быстрых PR", "rapid PR share")
    fun rapidPrLeaderCaption(): String = localizeRuntime("лидер по быстрым PR", "rapid PR leader")
    fun rapidPrScoreTitle(): String = localizeRuntime("оценка быстрых PR", "Rapid PR score")

    fun fileStatsTitle(): String = localizeRuntime("Статистика по файлам", "File statistics")
    fun filesChangedCaption(): String = localizeRuntime("изменено файлов", "files changed")
    fun changedFilesCountTitle(): String =
        localizeRuntime("Количество измененных файлов", "Changed files count")
    fun countAbbrevHeader(): String = localizeRuntime("Кол-во", "Count")
    fun fileChangesDistributionText(): String =
        localizeRuntime("Распределение изменений файлов", "File change distribution")
    fun churnScoreTitle(): String = localizeRuntime("оценка изменчивости кода", "Code churn score")
    fun noDataFallbackFile(): String = localizeRuntime("Нет данных", "No data")
    fun linesCountLabel(lines: Int): String = localizeRuntime(
        "$lines строк",
        "$lines ${if (lines == 1) "line" else "lines"}",
    )
    fun totalLinesCaption(): String = localizeRuntime("всего строк", "total lines")
    fun ownershipScoreTitle(): String = localizeRuntime("оценка владения кодом", "Code ownership score")

    fun actionsCount(count: Int): String =
        localizePluralRuntime(count, "действие", "действия", "действий", "action", "actions")

    fun activityByWeekdayTitle(): String =
        localizeRuntime("Распределение активности по дням", "Activity by weekday")
    fun noActivityYet(): String = localizeRuntime("Действий ещё не было", "No activity yet")
    fun mostActiveWeekdayTitle(): String =
        localizeRuntime("самый активный день недели", "most active weekday")
    fun leastActiveWeekdayTitle(): String =
        localizeRuntime("самый неактивный день недели", "least active weekday")
    fun dominantWeekdayScoreTitle(): String =
        localizeRuntime("оценка доминирующего дня недели", "Dominant weekday score")

    fun backContentDescription(): String = localizeRuntime("Назад", "Back")
    fun rapidThresholdHeading(): String = localizeRuntime("Период для быстроты", "Rapid merge threshold")
    fun dayShort(): String = localizeRuntime("д", "d")
    fun hourShort(): String = localizeRuntime("ч", "h")
    fun minShort(): String = localizeRuntime("мин", "min")

    fun noData(): String = localizeRuntime("Нет данных", "No data")
    fun nothingFound(): String = localizeRuntime("Ничего не найдено", "Nothing found")
    fun linesAddedLegend(): String = localizeRuntime("Добавлено строк", "Lines added")
    fun linesRemovedLegend(): String = localizeRuntime("Удалено строк", "Lines removed")
    fun tooltipAdded(value: Int): String = localizeRuntime("+$value добавлено", "+$value added")
    fun tooltipRemoved(value: Int, approx: Boolean): String {
        val suffix = if (approx) localizeRuntime(" (≈)", " (≈)") else ""
        return localizeRuntime("−$value удалено", "−$value removed") + suffix
    }
    fun closeContentDescription(): String = localizeRuntime("Закрыть", "Close")
    fun tableNameColumn(): String = localizeRuntime("Название", "Name")
    fun expandContentDescription(): String = localizeRuntime("Развернуть", "Expand")
    fun collapseContentDescription(): String = localizeRuntime("Свернуть", "Collapse")

    fun filesCount(count: Int): String =
        localizePluralRuntime(count, "файл", "файла", "файлов", "file", "files")
    fun linkLabel(): String = localizeRuntime("ссылка", "link")
    fun changedFilesHeading(): String = localizeRuntime("Изменённые файлы", "Changed files")
    fun noFileData(): String = localizeRuntime("Нет данных о файлах", "No file data")

    fun allFilesInternalFilter(): String = "__all_file_statuses__"
    fun filePathSearchPlaceholder(): String = localizeRuntime("Путь к файлу", "File path")
    fun sortAscendingCd(): String = localizeRuntime("По возрастанию", "Sort ascending")
    fun sortDescendingCd(): String = localizeRuntime("По убыванию", "Sort descending")
    fun allFilesWithCount(count: Int): String = localizeRuntime(
        "Все файлы ($count)",
        "All files ($count)",
    )

    fun allIssuesTitle(): String = localizeRuntime("Все Issues", "All issues")
    fun allPullRequestsDefaultTitle(): String =
        localizeRuntime("Все Pull Requests", "All pull requests")
    fun searchPlaceholder(): String = localizeRuntime("Поиск", "Search")

    fun creatorTitle(): String = localizeRuntime("Создатель", "Author")
    fun assigneesTitle(): String = localizeRuntime("Назначенные", "Assignees")
    fun notAssigned(): String = localizeRuntime("Не назначено", "Not assigned")
    fun createdAtTitle(): String = localizeRuntime("Дата создания", "Created")
    fun closedAtTitle(): String = localizeRuntime("Дата закрытия", "Closed")
    fun commentsCd(): String = localizeRuntime("Комментарии", "Comments")
    fun commitsCd(): String = localizeRuntime("Коммиты", "Commits")
    fun changedFilesCd(): String = localizeRuntime("Измененные файлы", "Changed files")
    fun linkWithArrow(): String = localizeRuntime("ссылка ›", "link ›")
    fun reactionsPositiveCd(): String = localizeRuntime("Положительные реакции", "Positive reactions")
    fun reactionsNegativeCd(): String = localizeRuntime("Отрицательные реакции", "Negative reactions")

    fun closedInDuration(duration: String): String =
        localizeRuntime("Закрыт за $duration", "Closed in $duration")

    fun youInParens(): String = localizeRuntime("(Вы)", "(You)")
    fun issueStateOpen(): String = localizeRuntime("open", "open")
    fun issueStateClosed(): String = localizeRuntime("closed", "closed")
    fun issueStateMerged(): String = localizeRuntime("merged", "merged")
    fun issueStateDraft(): String = localizeRuntime("черновик", "draft")

    fun issueLabelCount(count: Int): String = localizeRuntime(
        "- $count issue",
        "- $count ${if (count == 1) "issue" else "issues"}",
    )

    fun participantsTableTitle(): String = localizeRuntime("Таблица участников", "Participants table")
    fun changesWord(count: Int): String =
        localizePluralRuntime(count, "изменение", "изменения", "изменений", "change", "changes")
    fun linesWord(count: Int): String =
        localizePluralRuntime(count, "строка", "строки", "строк", "line", "lines")

    fun mostChangedFileSubtitle(): String =
        localizeRuntime("самый часто изменяемый файл", "most frequently changed file")
    fun donutPrSuffix(): String = localizeRuntime(" PR", " PR")
    fun exportPdf(): String = localizeRuntime("Экспорт в PDF", "Export to PDF")
    fun exportExcel(): String = localizeRuntime("Экспорт в Excel", "Export to Excel")

    fun detailSectionTitleCommits(): String = localizeRuntime("Коммиты", "Commits")
    fun detailSectionTitleIssues(): String = localizeRuntime("Задачи", "Issues")
    fun detailSectionTitlePullRequests(): String = localizeRuntime("Pull Request", "Pull Request")
    fun detailSectionTitleRapidPr(): String = localizeRuntime("Быстрые PR", "Rapid PRs")
    fun detailSectionTitleRefactoring(): String = localizeRuntime("Рефакторинг", "Refactoring")
    fun detailSectionTitleOwnership(): String = localizeRuntime("Владение", "Ownership")
    fun detailSectionTitleDominantDay(): String =
        localizeRuntime("Доминирующий день недели", "Dominant weekday")

    fun fileStatusLabelAll(): String = localizeRuntime("Все", "All")
    fun fileStatusLabelAdded(): String = localizeRuntime("Добавлено", "Added")
    fun fileStatusLabelRemoved(): String = localizeRuntime("Удалено", "Removed")
    fun fileStatusLabelModified(): String = localizeRuntime("Изменено", "Modified")
    fun fileStatusLabelRenamed(): String = localizeRuntime("Переименовано", "Renamed")
    fun fileStatusLabelCopied(): String = localizeRuntime("Скопировано", "Copied")

    fun prLifetimeBucket1(): String = localizeRuntime("< 1 часа", "< 1 hour")
    fun prLifetimeBucket2(): String = localizeRuntime("1-6 часов", "1–6 hours")
    fun prLifetimeBucket3(): String = localizeRuntime("6-24 часа", "6–24 hours")
    fun prLifetimeBucket4(): String = localizeRuntime("1-3 дня", "1–3 days")
    fun prLifetimeBucket5(): String = localizeRuntime("3-7 дней", "3–7 days")
    fun prLifetimeBucket6(): String = localizeRuntime("7-14 дней", "7–14 days")
    fun prLifetimeBucket7(): String = localizeRuntime(">14 дней", ">14 days")
    fun prCountInDonut(count: Int): String = localizeRuntime(
        "$count PR",
        "$count ${if (count == 1) "PR" else "PRs"}",
    )

    fun fileChurnBucket1(): String = localizeRuntime("1 изменение", "1 change")
    fun fileChurnBucket2(): String = localizeRuntime("2-3 изменения", "2–3 changes")
    fun fileChurnBucket3(): String = localizeRuntime("4-5 изменений", "4–5 changes")
    fun fileChurnBucket4(): String = localizeRuntime("6-7 изменений", "6–7 changes")
    fun fileChurnBucket5(): String = localizeRuntime("8-10 изменений", "8–10 changes")
    fun fileChurnBucket6(): String = localizeRuntime(">10 изменений", ">10 changes")
    fun filesCountInDonut(count: Int): String = localizeRuntime(
        "$count файлов",
        "$count ${if (count == 1) "file" else "files"}",
    )

    fun noDataUpper(): String = localizeRuntime("НЕТ ДАННЫХ", "NO DATA")

    fun monday(): String = localizeRuntime("Понедельник", "Monday")
    fun tuesday(): String = localizeRuntime("Вторник", "Tuesday")
    fun wednesday(): String = localizeRuntime("Среда", "Wednesday")
    fun thursday(): String = localizeRuntime("Четверг", "Thursday")
    fun friday(): String = localizeRuntime("Пятница", "Friday")
    fun saturday(): String = localizeRuntime("Суббота", "Saturday")
    fun sunday(): String = localizeRuntime("Воскресенье", "Sunday")
}
