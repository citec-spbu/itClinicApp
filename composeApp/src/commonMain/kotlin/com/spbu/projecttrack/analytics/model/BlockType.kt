package com.spbu.projecttrack.analytics.model

enum class BlockType(val key: String) {
    METRIC_CARD("metric_card"),
    CHART("chart"),
    KPI("kpi"),
    STUDENT_STATS("student_stats"),
    PROJECT_SECTION("project_section"),
    SUMMARY_HEADER("summary_header"),
    FILTER_BAR("filter_bar"),
    RANKING_ROW("ranking_row"),
    UNKNOWN("unknown"),
}
