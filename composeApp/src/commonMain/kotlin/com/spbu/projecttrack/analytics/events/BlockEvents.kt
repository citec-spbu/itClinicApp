package com.spbu.projecttrack.analytics.events

import com.spbu.projecttrack.analytics.model.AnalyticsEvent
import com.spbu.projecttrack.analytics.model.BlockType

object BlockEvents {

    fun viewed(
        blockId: String,
        blockType: BlockType,
        visibleRatio: Float,
        durationMs: Long,
        screenName: String,
        sessionId: String,
        userId: String?,
    ) = AnalyticsEvent(
        name = "block_viewed",
        properties = mapOf(
            "block_id"      to blockId,
            "focused_block_id" to blockId,
            "block_type"    to blockType.key,
            "visible_ratio" to visibleRatio,
            "duration_ms"   to durationMs,
            "screen_name"   to screenName,
            "\$screen_name" to screenName,
            "source_screen" to screenName,
        ),
        sessionId = sessionId,
        userId = userId,
    )

    fun tapped(
        blockId: String,
        blockType: BlockType,
        screenName: String,
        sessionId: String,
        userId: String?,
    ) = AnalyticsEvent(
        name = "block_tapped",
        properties = mapOf(
            "block_id"    to blockId,
            "focused_block_id" to blockId,
            "block_type"  to blockType.key,
            "screen_name" to screenName,
            "\$screen_name" to screenName,
            "source_screen" to screenName,
        ),
        sessionId = sessionId,
        userId = userId,
    )

    fun expanded(
        blockId: String,
        blockType: BlockType,
        screenName: String,
        sessionId: String,
        userId: String?,
    ) = AnalyticsEvent(
        name = "block_expanded",
        properties = mapOf(
            "block_id"    to blockId,
            "focused_block_id" to blockId,
            "block_type"  to blockType.key,
            "screen_name" to screenName,
            "\$screen_name" to screenName,
            "source_screen" to screenName,
        ),
        sessionId = sessionId,
        userId = userId,
    )

    fun ignored(
        blockId: String,
        blockType: BlockType,
        maxVisibleRatio: Float,
        screenName: String,
        sessionId: String,
        userId: String?,
    ) = AnalyticsEvent(
        name = "block_ignored",
        properties = mapOf(
            "block_id"          to blockId,
            "focused_block_id"  to blockId,
            "block_type"        to blockType.key,
            "max_visible_ratio" to maxVisibleRatio,
            "screen_name"       to screenName,
            "\$screen_name"     to screenName,
            "source_screen"     to screenName,
        ),
        sessionId = sessionId,
        userId = userId,
    )
}
