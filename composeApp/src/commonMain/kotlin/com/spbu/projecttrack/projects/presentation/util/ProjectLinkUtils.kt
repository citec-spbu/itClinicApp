package com.spbu.projecttrack.projects.presentation.util

import com.spbu.projecttrack.projects.data.model.Project
import com.spbu.projecttrack.projects.data.model.ProjectDetail

fun extractGithubUrl(project: Project?): String? {
    if (project == null) return null
    return extractGithubUrlFromLinks(project.links)
        ?: extractGithubUrlFromText(project.description, project.shortDescription)
}

fun extractGithubUrl(project: ProjectDetail?): String? {
    if (project == null) return null
    return extractGithubUrlFromLinks(project.links)
        ?: extractGithubUrlFromText(project.description, project.shortDescription)
}

private fun extractGithubUrlFromLinks(links: List<com.spbu.projecttrack.projects.data.model.ProjectLink>): String? {
    return links.firstNotNullOfOrNull { link ->
        val value = link.link?.trim().orEmpty()
        if (value.isBlank()) return@firstNotNullOfOrNull null

        val platform = link.platform?.trim().orEmpty()
        when {
            platform.equals("GitHub", ignoreCase = true) -> normalizeUrl(value)
            value.contains("github.com/", ignoreCase = true) -> normalizeUrl(value)
            else -> null
        }
    }
}

private fun extractGithubUrlFromText(description: String?, shortDescription: String?): String? {
    val text = listOfNotNull(description, shortDescription).joinToString(" ").trim()
    if (text.isBlank()) return null

    val regex = Regex(
        pattern = "(https?://github\\.com/[^\\s)]+|github\\.com/[^\\s)]+)",
        option = RegexOption.IGNORE_CASE
    )
    val match = regex.find(text) ?: return null
    val raw = match.value.trimEnd('.', ',', ')', ']', '>')
    return normalizeUrl(raw)
}

fun normalizeUrl(url: String): String {
    return if (url.startsWith("http://") || url.startsWith("https://")) {
        url
    } else {
        "https://$url"
    }
}
