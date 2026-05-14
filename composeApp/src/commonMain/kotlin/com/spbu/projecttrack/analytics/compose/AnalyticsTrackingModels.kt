package com.spbu.projecttrack.analytics.compose

import com.spbu.projecttrack.analytics.model.BlockType

data class BlockVisibilitySnapshot(
    val blockId: String,
    val blockType: BlockType,
    val screenName: String,
    val durationMs: Long,
    val maxVisibleRatio: Float,
)

data class BlockTapSnapshot(
    val blockId: String,
    val blockType: BlockType,
    val screenName: String,
    val action: String,
)
