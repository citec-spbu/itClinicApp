package com.spbu.projecttrack.core.search

private const val DefaultSearchMatchThreshold = 0.72

fun searchScore(query: String, vararg texts: String): Double = searchScore(query, texts.asList())

fun searchScore(query: String, texts: List<String>): Double {
    val normalizedQueryRaw = normalizeSearchRaw(query)
    if (normalizedQueryRaw.isBlank()) return 1.0

    val preparedFields = texts
        .asSequence()
        .map(::prepareSearchField)
        .filter { it.raw.isNotBlank() }
        .toList()
    if (preparedFields.isEmpty()) return 0.0

    val queryTokens = tokenizeForSearch(normalizedQueryRaw)
    val queryTokenText = queryTokens.joinToString(separator = " ")
    val directScore = preparedFields.maxOf { field ->
        directFieldScore(
            queryRaw = normalizedQueryRaw,
            queryTokenText = queryTokenText,
            field = field,
        )
    }

    if (queryTokens.isEmpty()) return directScore

    val candidateTokens = preparedFields.flatMap { it.tokens }.distinct()
    val tokenScores = queryTokens.map { token -> bestTokenScore(token, candidateTokens) }
    val tokenScore = if (tokenScores.all { it > 0.0 }) tokenScores.average() else 0.0

    return maxOf(directScore, tokenScore)
}

fun matchesSearchQuery(
    query: String,
    vararg texts: String,
    threshold: Double = DefaultSearchMatchThreshold,
): Boolean = searchScore(query, texts.asList()) >= threshold

private data class PreparedSearchField(
    val raw: String,
    val tokenText: String,
    val tokens: List<String>,
)

private fun prepareSearchField(text: String): PreparedSearchField {
    val raw = normalizeSearchRaw(text)
    val tokens = tokenizeForSearch(raw)
    return PreparedSearchField(
        raw = raw,
        tokenText = tokens.joinToString(separator = " "),
        tokens = tokens,
    )
}

private fun directFieldScore(
    queryRaw: String,
    queryTokenText: String,
    field: PreparedSearchField,
): Double {
    val raw = field.raw
    val tokenText = field.tokenText
    return when {
        raw == queryRaw || tokenText == queryTokenText -> 1.0
        raw.startsWith(queryRaw) || tokenText.startsWith(queryTokenText) -> 0.98
        raw.contains(" $queryRaw") || tokenText.contains(" $queryTokenText") -> 0.95
        queryRaw.length >= 3 && (raw.contains(queryRaw) || tokenText.contains(queryTokenText)) -> 0.92
        else -> 0.0
    }
}

private fun bestTokenScore(queryToken: String, candidateTokens: List<String>): Double {
    var bestScore = 0.0
    for (candidateToken in candidateTokens) {
        val score = when {
            candidateToken == queryToken -> 1.0
            candidateToken.startsWith(queryToken) -> 0.97
            queryToken.length >= 3 && candidateToken.contains(queryToken) -> 0.9
            else -> 0.0
        }
        if (score > bestScore) bestScore = score
        if (bestScore >= 1.0) return bestScore
    }
    return bestScore
}

private fun tokenizeForSearch(text: String): List<String> {
    if (text.isBlank()) return emptyList()

    val tokenized = buildString(text.length) {
        text.forEach { character ->
            append(
                when {
                    character.isLetterOrDigit() || character == '#' -> character
                    else -> ' '
                }
            )
        }
    }

    return tokenized
        .split(' ')
        .map(String::trim)
        .filter(String::isNotEmpty)
}

private fun normalizeSearchRaw(text: String): String {
    return buildString(text.length) {
        var previousWasWhitespace = false
        text.lowercase().forEach { character ->
            val normalizedCharacter = if (character == 'ё') 'е' else character
            if (normalizedCharacter.isWhitespace()) {
                if (!previousWasWhitespace) {
                    append(' ')
                    previousWasWhitespace = true
                }
            } else {
                append(normalizedCharacter)
                previousWasWhitespace = false
            }
        }
    }.trim()
}
