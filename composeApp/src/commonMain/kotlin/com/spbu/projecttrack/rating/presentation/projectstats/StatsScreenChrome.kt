package com.spbu.projecttrack.rating.presentation.projectstats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spbu.projecttrack.core.logging.AppLog
import com.spbu.projecttrack.core.settings.localizedString
import com.spbu.projecttrack.core.theme.AppFonts
import com.spbu.projecttrack.core.theme.appPalette
import com.spbu.projecttrack.core.time.PlatformTime
import org.jetbrains.compose.resources.painterResource
import projecttrack.composeapp.generated.resources.Res
import projecttrack.composeapp.generated.resources.arrow_back
import androidx.compose.ui.text.font.FontWeight

internal val ScreenHorizontalPadding = 21.dp
internal val StatsTopBarTotalHeight = 60.dp
private val StatsTopBarHorizontalPadding = 30.dp

private const val StatsNavigationLogTag = "StatsNavigation"
private const val StatsBackThrottleMs = 420L

@Composable
internal fun rememberStatsBackDispatcher(
    screenName: String,
    stateDescription: () -> String = { "" },
): (String, () -> Unit) -> Unit {
    var lastBackActionTs by rememberSaveable(screenName) { mutableStateOf(0L) }
    val latestStateDescription by rememberUpdatedState(stateDescription)

    return fun(source: String, action: () -> Unit) {
        val now = PlatformTime.currentTimeMillis()
        val deltaMs = now - lastBackActionTs
        val state = latestStateDescription().trim()
        val stateSuffix = if (state.isEmpty()) "" else " state={$state}"

        AppLog.d(
            StatsNavigationLogTag,
            "screen=$screenName source=$source deltaMs=$deltaMs$stateSuffix",
        )
        if (deltaMs in 0 until StatsBackThrottleMs) {
            AppLog.d(
                StatsNavigationLogTag,
                "screen=$screenName source=$source result=throttled deltaMs=$deltaMs",
            )
            return
        }

        lastBackActionTs = now
        AppLog.d(
            StatsNavigationLogTag,
            "screen=$screenName source=$source result=accepted",
        )
        action()
    }
}

@Composable
internal fun StatsTopBar(
    title: String,
    onBackClick: () -> Unit,
    titleFontSize: TextUnit = 40.sp,
    titleLineHeight: TextUnit = 40.sp,
    titleMaxLines: Int = 1,
    modifier: Modifier = Modifier,
) {
    val backInteractionSource = remember { MutableInteractionSource() }
    val backPressed by backInteractionSource.collectIsPressedAsState()
    val backScale by animateFloatAsState(
        targetValue = if (backPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "stats_topbar_back_scale",
    )
    val titleSizeState = rememberStatsTitleFontSize(
        title = title,
        maxFontSize = titleFontSize,
        maxLineHeight = titleLineHeight,
        maxLines = titleMaxLines,
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(horizontal = StatsTopBarHorizontalPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        val palette = appPalette()
        Image(
            painter = painterResource(Res.drawable.arrow_back),
            contentDescription = localizedString("Назад", "Back"),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-9).dp)
                .size(26.dp)
                .graphicsLayer {
                    scaleX = backScale
                    scaleY = backScale
                }
                .clickable(
                    interactionSource = backInteractionSource,
                    indication = null,
                    onClick = onBackClick,
                ),
            colorFilter = ColorFilter.tint(palette.secondaryText),
        )
        Text(
            text = title,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.Bold,
            fontSize = titleSizeState.fontSize,
            lineHeight = titleSizeState.lineHeight,
            letterSpacing = if (titleSizeState.fontSize >= 40.sp) 0.4.sp else 0.16.sp,
            color = palette.title,
            textAlign = TextAlign.Center,
            maxLines = titleMaxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result -> titleSizeState.onTextLayout(result.hasVisualOverflow) },
            modifier = Modifier.padding(horizontal = 40.dp),
        )
    }
}

@Composable
private fun rememberStatsTitleFontSize(
    title: String,
    maxFontSize: TextUnit,
    maxLineHeight: TextUnit,
    maxLines: Int,
): StatsTitleSizeState {
    val fontSizeCandidates = remember(maxFontSize) {
        buildList {
            add(maxFontSize)
            addAll(
                listOf(40.sp, 36.sp, 32.sp, 28.sp, 24.sp, 20.sp)
                    .filter { it < maxFontSize }
            )
        }.distinct().sortedByDescending { it.value }
    }
    val lineHeightRatio = remember(maxFontSize, maxLineHeight) {
        if (maxFontSize.value == 0f) 1f else maxLineHeight.value / maxFontSize.value
    }
    var currentIndex by remember(title, maxFontSize, maxLineHeight, maxLines) { mutableStateOf(0) }
    val fontSize = fontSizeCandidates[currentIndex]
    val lineHeight = (fontSize.value * lineHeightRatio).sp

    return StatsTitleSizeState(
        fontSize = fontSize,
        lineHeight = lineHeight,
        onTextLayout = { hasOverflow ->
            if (hasOverflow && currentIndex < fontSizeCandidates.lastIndex) {
                currentIndex += 1
            }
        },
    )
}

private data class StatsTitleSizeState(
    val fontSize: TextUnit,
    val lineHeight: TextUnit,
    val onTextLayout: (Boolean) -> Unit,
)
