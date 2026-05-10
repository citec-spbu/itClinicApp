package com.spbu.projecttrack.rating.data.model

internal fun StatsDetailIssueUi.involvesParticipant(participantId: String?): Boolean {
    if (participantId == null) return true
    return creatorId == participantId || assigneeIds.contains(participantId)
}

internal fun List<StatsDetailIssueUi>.filterByParticipant(
    participantId: String?,
): List<StatsDetailIssueUi> {
    if (participantId == null) return this
    return filter { it.involvesParticipant(participantId) }
}
