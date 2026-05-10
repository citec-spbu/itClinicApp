package com.spbu.projecttrack.rating.data.model

import com.spbu.projecttrack.core.settings.localizeRuntime

enum class RankingMetricKey(
    private val titleRu: String,
    private val titleEn: String,
    val supportsPeriod: Boolean = false,
    val supportsThreshold: Boolean = false,
    val supportsWeekDay: Boolean = false,
) {
    Commits(
        titleRu = "Commits",
        titleEn = "Commits",
        supportsPeriod = true,
    ),
    Issues(
        titleRu = "Issues",
        titleEn = "Issues",
        supportsPeriod = true,
    ),
    PullRequests(
        titleRu = "Pull Requests",
        titleEn = "Pull Requests",
        supportsPeriod = true,
    ),
    PerformanceGrade(
        titleRu = "Оценка производительности",
        titleEn = "Performance Grade",
        supportsPeriod = true,
    ),
    TotalCommits(
        titleRu = "Общее количество коммитов",
        titleEn = "Total Commits",
    ),
    IssueCompleteness(
        titleRu = "Завершенность задач",
        titleEn = "Issue Completeness",
    ),
    PullRequestHangTime(
        titleRu = "Время жизни Pull Request",
        titleEn = "PR Hang Time",
    ),
    RapidPullRequests(
        titleRu = "Быстрые Pull Requests",
        titleEn = "Rapid Pull Requests",
        supportsThreshold = true,
    ),
    CodeChurn(
        titleRu = "Изменчивость кода",
        titleEn = "Code Churn",
    ),
    CodeOwnership(
        titleRu = "Владение кодом",
        titleEn = "Code Ownership",
    ),
    DominantWeekDay(
        titleRu = "Доминирующий день недели",
        titleEn = "Dominant Weekday",
        supportsWeekDay = true,
    );

    val title: String get() = localizeRuntime(titleRu, titleEn)
    val chipLabel: String get() = title
}

enum class RankingPeriodPreset(
    private val labelRu: String,
    private val labelEn: String,
    val days: Int,
) {
    ThreeDays("3 дня", "3 days", 3),
    FiveDays("5 дней", "5 days", 5),
    OneWeek("1 неделя", "1 week", 7),
    TwoWeeks("2 недели", "2 weeks", 14),
    OneMonth("1 месяц", "1 month", 30),
    TwoMonths("2 месяца", "2 months", 60),
    ThreeMonths("3 месяца", "3 months", 90),
    SixMonths("6 месяцев", "6 months", 180),
    ;

    val label: String get() = localizeRuntime(labelRu, labelEn)
}

enum class RankingThresholdPreset(
    private val labelRu: String,
    private val labelEn: String,
    val minutes: Int,
) {
    TenMinutes("10 минут", "10 minutes", 10),
    FifteenMinutes("15 минут", "15 minutes", 15),
    ThirtyMinutes("30 минут", "30 minutes", 30),
    OneHour("1 час", "1 hour", 60),
    TwoAndHalfHours("2,5 часа", "2.5 hours", 150),
    FourHours("4 часа", "4 hours", 240),
    SixHours("6 часов", "6 hours", 360),
    TwelveHours("12 часов", "12 hours", 720),
    TwentyFourHours("24 часа", "24 hours", 1440),
    ;

    val label: String get() = localizeRuntime(labelRu, labelEn)
}

enum class RankingWeekDay(
    private val labelRu: String,
    val backendValue: String,
) {
    Monday("Понедельник", "Monday"),
    Tuesday("Вторник", "Tuesday"),
    Wednesday("Среда", "Wednesday"),
    Thursday("Четверг", "Thursday"),
    Friday("Пятница", "Friday"),
    Saturday("Суббота", "Saturday"),
    Sunday("Воскресенье", "Sunday");

    val label: String get() = localizeRuntime(labelRu, backendValue)
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
    return RankingFilters(metrics = rankingDefaultMetricFilters())
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
            title = localizeRuntime("Нет", "None"),
            filters = defaultFilters,
            isBuiltIn = true,
        ),
        RankingFilterTemplate(
            id = "commits",
            title = localizeRuntime("Коммиты", "Commits"),
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
            title = localizeRuntime("Пулл Реквесты", "Pull Requests"),
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
            title = localizeRuntime("Работа с кодом", "Code Work"),
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
