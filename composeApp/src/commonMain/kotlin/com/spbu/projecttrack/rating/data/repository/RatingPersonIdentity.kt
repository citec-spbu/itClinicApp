package com.spbu.projecttrack.rating.data.repository

private val personTokenCleanupRegex = Regex("[^\\p{L}\\p{Nd}\\s]")
private val whitespaceRegex = Regex("\\s+")
private val roleSeparators = listOf(" — ", " - ")
private val roleKeywords = listOf(
    "developer",
    "designer",
    "analyst",
    "engineer",
    "manager",
    "lead",
    "teamlead",
    "team",
    "backend",
    "frontend",
    "fullstack",
    "product",
    "android",
    "ios",
    "qa",
    "devops",
    "data",
    "ux",
    "ui",
    "architect",
    "researcher",
    "разработчик",
    "аналитик",
    "дизайнер",
    "инженер",
    "менеджер",
    "тимлид",
    "лид",
    "тестировщик",
    "архитектор",
)

internal fun normalizeComparableText(value: String?): String {
    return value
        .orEmpty()
        .trim()
        .lowercase()
        .replace(whitespaceRegex, " ")
}

internal fun displayPersonName(value: String?): String {
    val trimmed = value.orEmpty().trim()
    if (trimmed.isBlank()) return ""

    for (separator in roleSeparators) {
        val index = trimmed.indexOf(separator)
        if (index <= 0) continue

        val head = trimmed.substring(0, index).trim()
        val tail = trimmed.substring(index + separator.length).trim()
        if (head.isBlank() || tail.isBlank()) continue

        val normalizedTail = normalizeComparableText(tail)
        if (roleKeywords.any { keyword -> keyword in normalizedTail }) {
            return head
        }
    }

    return trimmed
}

internal fun personNameKey(value: String?): String {
    val parts = tokenizePersonName(displayPersonName(value))
    if (parts.isEmpty()) return ""
    if (parts.size == 1) return parts.first()

    val surname = parts.first()
    val initials = parts.drop(1)
        .mapNotNull { token -> token.firstOrNull()?.toString() }
        .joinToString("")

    return if (initials.isBlank()) surname else "$surname|$initials"
}

internal fun personNameMatches(first: String?, second: String?): Boolean {
    val firstKey = personNameKey(first)
    val secondKey = personNameKey(second)
    if (firstKey.isNotBlank() && secondKey.isNotBlank() && firstKey == secondKey) {
        return true
    }

    val left = normalizeComparableText(displayPersonName(first))
    val right = normalizeComparableText(displayPersonName(second))
    return left.isNotBlank() && left == right
}

internal fun preferDisplayPersonName(current: String?, candidate: String?): String? {
    val currentName = displayPersonName(current).takeIf { it.isNotBlank() }
    val candidateName = displayPersonName(candidate).takeIf { it.isNotBlank() }

    return when {
        currentName == null -> candidateName
        candidateName == null -> currentName
        displayNameRank(candidateName) > displayNameRank(currentName) -> candidateName
        else -> currentName
    }
}

private fun tokenizePersonName(value: String): List<String> {
    return personTokenCleanupRegex.replace(value.lowercase(), " ")
        .split(whitespaceRegex)
        .filter { it.isNotBlank() }
}

private fun displayNameRank(value: String): Int {
    val parts = tokenizePersonName(value)
    val longTokens = parts.count { it.length > 1 }
    return (parts.size * 20) + (longTokens * 10) + value.length
}
