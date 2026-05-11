package com.spbu.projecttrack.rating.export

internal data class PdfRgb(
    val r: Int,
    val g: Int,
    val b: Int,
)

internal enum class PdfScoreTone {
    Low,
    Mid,
    High,
    Neutral,
}

internal object ProjectStatsPdfTheme {
    val paper = PdfRgb(246, 241, 234)
    val rail = PdfRgb(178, 48, 34)
    val ink = PdfRgb(42, 36, 32)
    val muted = PdfRgb(124, 115, 108)
    val border = PdfRgb(214, 202, 191)
    val headerFill = PdfRgb(34, 31, 29)
    val cardFill = PdfRgb(242, 236, 229)
    val cardFillStrong = PdfRgb(236, 228, 218)
    val low = PdfRgb(186, 55, 42)
    val mid = PdfRgb(168, 141, 63)
    val high = PdfRgb(72, 115, 95)
    val prChart = PdfRgb(151, 138, 74)
    val rapidPrChart = PdfRgb(72, 115, 95)
    val commitChart = PdfRgb(178, 48, 34)
    val weekdayPalette = listOf(
        PdfRgb(196, 73, 58),
        PdfRgb(221, 134, 61),
        PdfRgb(205, 164, 76),
        PdfRgb(177, 145, 68),
        PdfRgb(77, 114, 91),
        PdfRgb(191, 184, 176),
        PdfRgb(116, 91, 83),
    )
    val churnPalette = listOf(
        PdfRgb(178, 48, 34),
        PdfRgb(146, 124, 73),
        PdfRgb(77, 114, 91),
        PdfRgb(206, 111, 68),
        PdfRgb(191, 161, 95),
        PdfRgb(89, 64, 56),
    )
}

internal fun pdfScoreTone(score: Double): PdfScoreTone = when {
    score < 3.0 -> PdfScoreTone.Low
    score < 4.0 -> PdfScoreTone.Mid
    score.isNaN() -> PdfScoreTone.Neutral
    else -> PdfScoreTone.High
}

internal fun pdfScoreColor(score: Double): PdfRgb = when (pdfScoreTone(score)) {
    PdfScoreTone.Low -> ProjectStatsPdfTheme.low
    PdfScoreTone.Mid -> ProjectStatsPdfTheme.mid
    PdfScoreTone.High -> ProjectStatsPdfTheme.high
    PdfScoreTone.Neutral -> ProjectStatsPdfTheme.muted
}

internal fun pdfChartAccent(title: String): PdfRgb {
    val normalized = title.lowercase()
    return when {
        "быстр" in normalized || "rapid" in normalized -> ProjectStatsPdfTheme.rapidPrChart
        "pull" in normalized || "pr" in normalized -> ProjectStatsPdfTheme.prChart
        else -> ProjectStatsPdfTheme.commitChart
    }
}

internal fun pdfSectionTag(title: String): String = title
    .replace('\n', ' ')
    .trim()
    .uppercase()
    .take(28)

