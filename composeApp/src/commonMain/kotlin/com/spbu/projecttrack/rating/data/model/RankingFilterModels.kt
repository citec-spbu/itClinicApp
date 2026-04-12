package com.spbu.projecttrack.rating.data.model

enum class RankingMetricKey(
    val title: String,
    val chipLabel: String = title,
    val supportsPeriod: Boolean = false,
    val supportsThreshold: Boolean = false,
    val supportsWeekDay: Boolean = false,
) {
    Commits(
        title = "Commits",
        supportsPeriod = true,
    ),
    Issues(
        title = "Issues",
        supportsPeriod = true,
    ),
    PullRequests(
        title = "Pull Requests",
        supportsPeriod = true,
    ),
    PerformanceGrade(
        title = "Оценка производительности",
        supportsPeriod = true,
    ),
    TotalCommits(
        title = "Общее количество коммитов",
    ),
    IssueCompleteness(
        title = "Завершенность задач",
    ),
    PullRequestHangTime(
        title = "Время жизни Pull Request",
    ),
    RapidPullRequests(
        title = "Быстрые Pull Requests",
        supportsThreshold = true,
    ),
    CodeChurn(
        title = "Изменчивость кода",
    ),
    CodeOwnership(
        title = "Владение кодом",
    ),
    DominantWeekDay(
        title = "Доминирующий день недели",
        supportsWeekDay = true,
    );
}

enum class RankingPeriodPreset(
    val label: String,
    val days: Int,
) {
    OneWeek("1 неделя", 7),
    TwoWeeks("2 недели", 14),
    OneMonth("1 месяц", 30);
}

enum class RankingThresholdPreset(
    val label: String,
    val minutes: Int,
) {
    ThirtyMinutes("30 минут", 30),
    OneHour("1 час", 60),
    TwoAndHalfHours("2,5 часа", 150),
}

enum class RankingWeekDay(
    val label: String,
    val backendValue: String,
) {
    Monday("Понедельник", "Monday"),
    Tuesday("Вторник", "Tuesday"),
    Wednesday("Среда", "Wednesday"),
    Thursday("Четверг", "Thursday"),
    Friday("Пятница", "Friday"),
    Saturday("Суббота", "Saturday"),
    Sunday("Воскресенье", "Sunday");
}

data class RankingMetricFilter(
    val enabled: Boolean = false,
    val periodPreset: RankingPeriodPreset = RankingPeriodPreset.TwoWeeks,
    val thresholdPreset: RankingThresholdPreset = RankingThresholdPreset.TwoAndHalfHours,
    val weekDay: RankingWeekDay = RankingWeekDay.Thursday,
)

data class RankingDateRangeFilter(
    val startMillis: Long? = null,
    val endMillis: Long? = null,
) {
    val isActive: Boolean
        get() = startMillis != null || endMillis != null
}

data class RankingFilters(
    val metrics: Map<RankingMetricKey, RankingMetricFilter> = rankingDefaultMetricFilters(),
    val dateRange: RankingDateRangeFilter = RankingDateRangeFilter(),
) {
    fun metric(key: RankingMetricKey): RankingMetricFilter {
        return metrics[key] ?: RankingMetricFilter()
    }

    fun isEnabled(key: RankingMetricKey): Boolean {
        return metric(key).enabled
    }

    fun activeMetricKeys(): List<RankingMetricKey> {
        return RankingMetricKey.entries.filter(::isEnabled)
    }

    fun activeChipLabels(): List<String> {
        return activeMetricKeys().map(RankingMetricKey::chipLabel)
    }

    fun hasActiveSelections(): Boolean {
        return activeMetricKeys().isNotEmpty() || dateRange.isActive
    }
}

data class RankingFilterTemplate(
    val id: String,
    val title: String,
    val filters: RankingFilters? = null,
    val isBuiltIn: Boolean = false,
)

fun rankingDefaultFilters(): RankingFilters {
    val metrics = rankingDefaultMetricFilters().toMutableMap()
    metrics[RankingMetricKey.Commits] = metrics.getValue(RankingMetricKey.Commits).copy(enabled = true)
    metrics[RankingMetricKey.Issues] = metrics.getValue(RankingMetricKey.Issues).copy(enabled = true)
    metrics[RankingMetricKey.PullRequests] = metrics.getValue(RankingMetricKey.PullRequests).copy(enabled = true)
    return RankingFilters(metrics = metrics)
}

fun rankingDefaultMetricFilters(): Map<RankingMetricKey, RankingMetricFilter> {
    return RankingMetricKey.entries.associateWith { RankingMetricFilter() }
}

fun rankingBuiltInTemplates(
    defaultFilters: RankingFilters = rankingDefaultFilters(),
): List<RankingFilterTemplate> {
    val metrics = rankingDefaultMetricFilters()

    return listOf(
        RankingFilterTemplate(
            id = "none",
            title = "Нет",
            filters = defaultFilters,
            isBuiltIn = true,
        ),
        RankingFilterTemplate(
            id = "commits",
            title = "Коммиты",
            filters = RankingFilters(
                metrics = metrics.toMutableMap().apply {
                    this[RankingMetricKey.Commits] = getValue(RankingMetricKey.Commits).copy(enabled = true)
                    this[RankingMetricKey.TotalCommits] = getValue(RankingMetricKey.TotalCommits).copy(enabled = true)
                }
            ),
            isBuiltIn = true,
        ),
        RankingFilterTemplate(
            id = "pull_requests",
            title = "Пулл Реквесты",
            filters = RankingFilters(
                metrics = metrics.toMutableMap().apply {
                    this[RankingMetricKey.PullRequests] = getValue(RankingMetricKey.PullRequests).copy(enabled = true)
                    this[RankingMetricKey.PullRequestHangTime] = getValue(RankingMetricKey.PullRequestHangTime).copy(enabled = true)
                    this[RankingMetricKey.RapidPullRequests] = getValue(RankingMetricKey.RapidPullRequests).copy(enabled = true)
                }
            ),
            isBuiltIn = true,
        ),
        RankingFilterTemplate(
            id = "code_work",
            title = "Работа с кодом",
            filters = RankingFilters(
                metrics = metrics.toMutableMap().apply {
                    this[RankingMetricKey.CodeChurn] = getValue(RankingMetricKey.CodeChurn).copy(enabled = true)
                    this[RankingMetricKey.CodeOwnership] = getValue(RankingMetricKey.CodeOwnership).copy(enabled = true)
                    this[RankingMetricKey.DominantWeekDay] = getValue(RankingMetricKey.DominantWeekDay).copy(enabled = true)
                }
            ),
            isBuiltIn = true,
        ),
    )
}
