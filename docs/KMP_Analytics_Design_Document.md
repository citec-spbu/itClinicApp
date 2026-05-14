# KMP User Behaviour Analytics — Technical Design Document

> **Версия:** 1.2  
> **Платформы:** Android · iOS  
> **Стек:** Kotlin Multiplatform · Compose Multiplatform · Firebase Analytics / PostHog  
> **Статус:** Production implementation as of 2026-05-13

---

## Содержание

1. [Обзор архитектуры](#1-обзор-архитектуры)
2. [AnalyticsTracker — общий интерфейс](#2-analyticstracker--общий-интерфейс)
3. [Event Model](#3-event-model)
4. [Какие события логировать](#4-какие-события-логировать)
5. [Tracking в Compose: Visibility · Time · Scroll · Focus · Tap](#5-tracking-в-compose)
6. [Production-ready подход без excessive recomposition](#6-production-ready-подход)
7. [Debounce / Throttle стратегия](#7-debounce--throttle-стратегия)
8. [Session ID / User ID — хранение](#8-session-id--user-id--хранение)
9. [Naming Convention для событий](#9-naming-convention-для-событий)
10. [Интеграция с Firebase Analytics](#10-интеграция-с-firebase-analytics)
11. [Интеграция с PostHog](#11-интеграция-с-posthog)
12. [Что НЕ отправлять](#12-что-не-отправлять)
13. [Heatmap / Focus Analytics](#13-heatmap--focus-analytics)
14. [Готовая реализация MetricBlock tracking](#14-готовая-реализация-metricblock-tracking)
15. [Памятка по производительности](#15-памятка-по-производительности)

---

## Реальный статус реализации

- В production pipeline сейчас входят `LoggingAnalyticsTracker`, `PostHogAnalyticsTracker` и `FirebaseAnalyticsTracker`.
- Firebase подключён на Android напрямую через SDK и на iOS через Swift host app bridge.
- Для Compose экранов автоматический screen reporting Firebase отключён, чтобы не дублировать `screen_viewed`.
- По умолчанию `block_viewed` считается при `visibleRatio >= 0.5` и `duration >= 2000 ms`.
- `focus tracking` сейчас работает и на экране статистики проекта, и на экране личной статистики: `visibleRatio >= 0.6`, `duration >= 1500 ms`, суммирование по нескольким появлениям блока, не более одного `focus` на блок за одно открытие.
- На экране настроек статистики отправляется итоговое событие только при фактическом изменении состава или порядка блоков.
- Analytics identity привязана к auth lifecycle: после логина используется безопасный `usr_<hash>`, при logout identity сбрасывается.
- При уходе приложения в background выполняется `flush()` analytics и `refreshSession()` при возврате в foreground.

---

## 1. Обзор архитектуры

```
┌─────────────────────────────────────────────────────────────┐
│                    Compose UI Layer                          │
│  MetricCard · ChartBlock · KpiBlock · StudentStatsSection   │
│        ↓ вызывают через remember { } без recomposition      │
├─────────────────────────────────────────────────────────────┤
│                  Analytics DSL / Modifiers                   │
│  Modifier.trackVisibility() · Modifier.trackTap()           │
│  Modifier.trackScrollDepth() · Modifier.trackFocus()        │
│        ↓                                                     │
├─────────────────────────────────────────────────────────────┤
│              AnalyticsTracker (commonMain)                   │
│  interface + SessionManager + event mapping                 │
│        ↓                                                     │
├────────────────────┬────────────────────────────────────────┤
│  FirebaseAnalytics │  PostHogAnalytics  │  LoggingAnalytics │
│  (Android SDK +    │  (commonMain/HTTP) │  (debug/dev)      │
│   iOS Swift bridge)│                    │                   │
└────────────────────┴────────────────────────────────────────┘
```

### Принципы

| Принцип | Решение |
|---------|---------|
| KMP-first | Весь core в `commonMain`; платформо-зависимое — только провайдеры |
| Нет recomposition | Трекер передаётся через `remember {}` + `DisposableEffect` |
| Нет memory leaks | `DisposableEffect` + `CoroutineScope` привязан к `onDestroy` |
| Throttle/Debounce | Пороговые события и milestone tracking вместо стрима на каждый scroll tick |
| Батчинг | PostHog батчится локально; Firebase батчится самим SDK |

---

## 2. AnalyticsTracker — общий интерфейс

### 2.1 Основной интерфейс (`commonMain`)

```kotlin
// analytics/AnalyticsTracker.kt
interface AnalyticsTracker {

    /** Отправить произвольное событие */
    fun track(event: AnalyticsEvent)

    /** Установить свойства пользователя (не события) */
    fun identify(userId: String, properties: Map<String, Any> = emptyMap())

    /** Сбросить сессию (logout) */
    fun reset()

    /** Принудительно сбросить буфер событий на сервер */
    suspend fun flush()
}
```

### 2.2 Составной трекер — fan-out к нескольким системам

```kotlin
// analytics/CompositeAnalyticsTracker.kt
class CompositeAnalyticsTracker(
    private val trackers: List<AnalyticsTracker>
) : AnalyticsTracker {

    override fun track(event: AnalyticsEvent) {
        trackers.forEach { it.track(event) }
    }

    override fun identify(userId: String, properties: Map<String, Any>) {
        trackers.forEach { it.identify(userId, properties) }
    }

    override fun reset() {
        trackers.forEach { it.reset() }
    }

    override suspend fun flush() {
        trackers.forEach { it.flush() }
    }
}
```

### 2.3 NoOp-реализация для тестов

```kotlin
// analytics/NoOpAnalyticsTracker.kt
class NoOpAnalyticsTracker : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) = Unit
    override fun identify(userId: String, properties: Map<String, Any>) = Unit
    override fun reset() = Unit
    override suspend fun flush() = Unit
}
```

### 2.4 Logging-реализация для дебага

```kotlin
// analytics/LoggingAnalyticsTracker.kt
class LoggingAnalyticsTracker(
    private val logger: (String) -> Unit = ::println
) : AnalyticsTracker {

    override fun track(event: AnalyticsEvent) {
        logger("[Analytics] ${event.name} | ${event.properties}")
    }

    override fun identify(userId: String, properties: Map<String, Any>) {
        logger("[Analytics] identify $userId | $properties")
    }

    override fun reset() {
        logger("[Analytics] reset session")
    }

    override suspend fun flush() {
        logger("[Analytics] flush")
    }
}
```

---

## 3. Event Model

### 3.1 Базовая модель события

```kotlin
// analytics/model/AnalyticsEvent.kt
data class AnalyticsEvent(
    val name: String,                              // "metric_block_viewed"
    val properties: Map<String, Any> = emptyMap(), // свободные параметры
    val timestamp: Long = currentTimeMillis(),     // Unix ms
    val sessionId: String,
    val userId: String?,
)

// expect fun currentTimeMillis(): Long
// actual fun currentTimeMillis(): Long = System.currentTimeMillis()  // androidMain
// actual fun currentTimeMillis(): Long = ...                          // iosMain
```

### 3.2 Типизированные фабрики событий

```kotlin
// analytics/events/BlockEvents.kt
object BlockEvents {

    fun viewed(
        blockId: String,
        blockType: BlockType,
        visibleRatio: Float,       // 0.0–1.0
        durationMs: Long,
        screenName: String,
        sessionId: String,
        userId: String?
    ) = AnalyticsEvent(
        name = "block_viewed",
        properties = mapOf(
            "block_id"       to blockId,
            "block_type"     to blockType.key,
            "visible_ratio"  to visibleRatio,
            "duration_ms"    to durationMs,
            "screen_name"    to screenName
        ),
        sessionId = sessionId,
        userId = userId
    )

    fun tapped(
        blockId: String,
        blockType: BlockType,
        tapCount: Int = 1,
        screenName: String,
        sessionId: String,
        userId: String?
    ) = AnalyticsEvent(
        name = "block_tapped",
        properties = mapOf(
            "block_id"    to blockId,
            "block_type"  to blockType.key,
            "tap_count"   to tapCount,
            "screen_name" to screenName
        ),
        sessionId = sessionId,
        userId = userId
    )

    fun expanded(
        blockId: String,
        blockType: BlockType,
        screenName: String,
        sessionId: String,
        userId: String?
    ) = AnalyticsEvent(
        name = "block_expanded",
        properties = mapOf(
            "block_id"    to blockId,
            "block_type"  to blockType.key,
            "screen_name" to screenName
        ),
        sessionId = sessionId,
        userId = userId
    )

    fun ignored(
        blockId: String,
        blockType: BlockType,
        maxVisibleRatio: Float,
        screenName: String,
        sessionId: String,
        userId: String?
    ) = AnalyticsEvent(
        name = "block_ignored",
        properties = mapOf(
            "block_id"          to blockId,
            "block_type"        to blockType.key,
            "max_visible_ratio" to maxVisibleRatio,
            "screen_name"       to screenName
        ),
        sessionId = sessionId,
        userId = userId
    )
}
```

### 3.3 Тип блока

```kotlin
// analytics/model/BlockType.kt
enum class BlockType(val key: String) {
    METRIC_CARD("metric_card"),
    CHART("chart"),
    KPI("kpi"),
    STUDENT_STATS("student_stats"),
    PROJECT_SECTION("project_section"),
    SUMMARY_HEADER("summary_header"),
    FILTER_BAR("filter_bar"),
    UNKNOWN("unknown")
}
```

### 3.4 Scroll и Screen события

```kotlin
// analytics/events/ScrollEvents.kt
object ScrollEvents {

    fun depthReached(
        screenName: String,
        depthPercent: Int,    // 25, 50, 75, 100
        sessionId: String,
        userId: String?
    ) = AnalyticsEvent(
        name = "scroll_depth_reached",
        properties = mapOf(
            "screen_name"   to screenName,
            "depth_percent" to depthPercent
        ),
        sessionId = sessionId,
        userId = userId
    )

    fun sessionSummary(
        screenName: String,
        maxDepthPercent: Int,
        totalScrollPx: Int,
        sessionId: String,
        userId: String?
    ) = AnalyticsEvent(
        name = "scroll_session_summary",
        properties = mapOf(
            "screen_name"       to screenName,
            "max_depth_percent" to maxDepthPercent,
            "total_scroll_px"   to totalScrollPx
        ),
        sessionId = sessionId,
        userId = userId
    )
}

// analytics/events/ScreenEvents.kt
object ScreenEvents {

    fun viewed(
        screenName: String,
        referrer: String? = null,
        sessionId: String,
        userId: String?
    ) = AnalyticsEvent(
        name = "screen_viewed",
        properties = buildMap {
            put("screen_name", screenName)
            referrer?.let { put("referrer", it) }
        },
        sessionId = sessionId,
        userId = userId
    )

    fun left(
        screenName: String,
        timeOnScreenMs: Long,
        sessionId: String,
        userId: String?
    ) = AnalyticsEvent(
        name = "screen_left",
        properties = mapOf(
            "screen_name"      to screenName,
            "time_on_screen_ms" to timeOnScreenMs
        ),
        sessionId = sessionId,
        userId = userId
    )
}
```

---

## 4. Какие события логировать

| Событие | Триггер | Ключевые параметры |
|---------|---------|--------------------|
| `screen_viewed` | экран стал активным | `screen_name`, `referrer` |
| `screen_left` | экран скрылся/закрылся | `screen_name`, `time_on_screen_ms` |
| `block_viewed` | блок был виден ≥ 2000 ms при ≥ 50% | `block_id`, `block_type`, `duration_ms`, `visible_ratio`, `screen_name` |
| `block_tapped` | пользователь нажал | `block_id`, `block_type`, `screen_name` |
| `block_expanded` | раскрытие блока | `block_id`, `block_type` |
| `block_ignored` | блок проскролен быстро | `block_id`, `max_visible_ratio` |
| `scroll_depth_reached` | каждые 25% прокрутки | `screen_name`, `depth_percent` |
| `scroll_session_summary` | при выходе с экрана | `max_depth_percent` |
| `metric_block_focus` | блок статистики реально посмотрели | `screen`, `project_id_hash`, `block_id`, `block_type`, `position`, `duration_ms`, `max_visible_percent` |
| `metric_block_tap` | взаимодействие с блоком статистики | `screen`, `project_id_hash`, `block_id`, `block_type`, `position`, `action` |
| `project_stats_screen_close` | выход с экрана статистики проекта | `screen`, `project_id_hash`, `session_duration_ms`, `max_scroll_percent`, `impressed_blocks_count`, `focused_blocks_count`, `tapped_blocks_count` |
| `user_stats_screen_close` | выход с экрана личной статистики | `screen`, `project_id_hash`, `subject_user_id_hash`, `session_duration_ms`, `max_scroll_percent`, `impressed_blocks_count`, `focused_blocks_count`, `tapped_blocks_count` |
| `stats_screen_settings_changed` | выход с экрана настроек после изменений | `screen`, `stats_target`, `project_id_hash`, `initial_section_ids`, `updated_section_ids`, `order_changed`, `visibility_changed`, `changed_blocks_count` |
| `stats_repository_changed` | пользователь сменил репозиторий | `screen`, `project_id_hash`, `repository_id_hash` |
| `stats_date_range_changed` | пользователь сменил период | `screen`, `project_id_hash`, `start_iso_date`, `end_iso_date` |
| `stats_rapid_threshold_changed` | пользователь сменил порог rapid | `screen`, `project_id_hash`, `days`, `hours`, `minutes`, `total_minutes` |
| `stats_detail_opened` | пользователь открыл detail screen по секции | `screen`, `project_id_hash`, `section_id` |
| `stats_settings_opened` | пользователь открыл экран настроек статистики | `screen`, `project_id_hash` |
| `stats_export_requested` | пользователь запросил экспорт | `screen`, `project_id_hash`, `format`, `scope`, `section_id`, `participant_id_hash` |
| `project_stats_member_opened` | пользователь открыл статистику участника проекта | `screen`, `project_id_hash`, `member_id_hash` |
| `filter_applied` | смена фильтра | `filter_name`, `filter_value` |
| `chart_interacted` | зум/тап по точке | `chart_id`, `interaction_type` |

---

## 5. Tracking в Compose

### 5.1 Контекст трекера через CompositionLocal

```kotlin
// analytics/compose/LocalAnalytics.kt
val LocalAnalyticsTracker = staticCompositionLocalOf<AnalyticsTracker> {
    NoOpAnalyticsTracker()
}

val LocalAnalyticsSession = staticCompositionLocalOf<AnalyticsSession> {
    error("AnalyticsSession not provided")
}

// Обёртка — удобный доступ
@Composable
fun rememberAnalyticsContext(): AnalyticsContext {
    val tracker = LocalAnalyticsTracker.current
    val session = LocalAnalyticsSession.current
    return remember(tracker, session) { AnalyticsContext(tracker, session) }
}

data class AnalyticsContext(
    val tracker: AnalyticsTracker,
    val session: AnalyticsSession
)
```

### 5.2 Предоставление зависимостей в дереве

```kotlin
// В корневом Composable (App.kt)
@Composable
fun App(
    analyticsTracker: AnalyticsTracker,
    analyticsSession: AnalyticsSession
) {
    CompositionLocalProvider(
        LocalAnalyticsTracker provides analyticsTracker,
        LocalAnalyticsSession provides analyticsSession
    ) {
        AppNavigation()
    }
}
```

### 5.3 Modifier.trackVisibility() — отслеживание видимости

```kotlin
// analytics/compose/VisibilityTracker.kt

/**
 * Отслеживает, сколько времени блок был виден на экране.
 * Срабатывает когда visibleRatio >= threshold дольше minDurationMs.
 *
 * Не вызывает recomposition — использует side effect.
 */
fun Modifier.trackVisibility(
    blockId: String,
    blockType: BlockType,
    screenName: String,
    visibilityThreshold: Float = 0.5f,
    minDurationMs: Long = 1_000L,
    analyticsContext: AnalyticsContext,
): Modifier = this.then(
    VisibilityTrackingModifier(
        blockId = blockId,
        blockType = blockType,
        screenName = screenName,
        visibilityThreshold = visibilityThreshold,
        minDurationMs = minDurationMs,
        analyticsContext = analyticsContext,
    )
)

private class VisibilityTrackingModifier(
    private val blockId: String,
    private val blockType: BlockType,
    private val screenName: String,
    private val visibilityThreshold: Float,
    private val minDurationMs: Long,
    private val analyticsContext: AnalyticsContext,
) : ModifierNodeElement<VisibilityTrackingNode>() {

    override fun create() = VisibilityTrackingNode(
        blockId, blockType, screenName,
        visibilityThreshold, minDurationMs, analyticsContext
    )

    override fun update(node: VisibilityTrackingNode) {
        node.blockId = blockId
        node.blockType = blockType
        node.screenName = screenName
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VisibilityTrackingModifier) return false
        return blockId == other.blockId && blockType == other.blockType
    }

    override fun hashCode(): Int = blockId.hashCode() * 31 + blockType.hashCode()
}

private class VisibilityTrackingNode(
    var blockId: String,
    var blockType: BlockType,
    var screenName: String,
    private val visibilityThreshold: Float,
    private val minDurationMs: Long,
    private val analyticsContext: AnalyticsContext,
) : Modifier.Node(), LayoutAwareModifierNode, GlobalPositionAwareModifierNode {

    private var visibleStart: Long? = null
    private var totalVisibleMs: Long = 0L
    private var maxVisibleRatio: Float = 0f
    private var hasReported = false

    // Вызывается при изменении позиции/размера
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val visibleRatio = calculateVisibleRatio(coordinates)
        val now = currentTimeMillis()

        maxVisibleRatio = maxOf(maxVisibleRatio, visibleRatio)

        if (visibleRatio >= visibilityThreshold) {
            if (visibleStart == null) {
                visibleStart = now // начало видимости
            }
        } else {
            visibleStart?.let { start ->
                totalVisibleMs += now - start
                visibleStart = null
            }
        }

        // Отправляем событие если блок был виден достаточно долго
        if (!hasReported && totalVisibleMs >= minDurationMs && visibleRatio >= visibilityThreshold) {
            hasReported = true
            reportViewed()
        }
    }

    override fun onDetach() {
        // Финальный flush при уходе с экрана
        val end = currentTimeMillis()
        visibleStart?.let { start ->
            totalVisibleMs += end - start
        }

        if (!hasReported && totalVisibleMs > 0) {
            if (maxVisibleRatio < visibilityThreshold) {
                reportIgnored()
            } else {
                reportViewed()
            }
        }
        super.onDetach()
    }

    private fun reportViewed() {
        analyticsContext.tracker.track(
            BlockEvents.viewed(
                blockId = blockId,
                blockType = blockType,
                visibleRatio = maxVisibleRatio,
                durationMs = totalVisibleMs,
                screenName = screenName,
                sessionId = analyticsContext.session.sessionId,
                userId = analyticsContext.session.userId
            )
        )
    }

    private fun reportIgnored() {
        analyticsContext.tracker.track(
            BlockEvents.ignored(
                blockId = blockId,
                blockType = blockType,
                maxVisibleRatio = maxVisibleRatio,
                screenName = screenName,
                sessionId = analyticsContext.session.sessionId,
                userId = analyticsContext.session.userId
            )
        )
    }

    private fun calculateVisibleRatio(coordinates: LayoutCoordinates): Float {
        val windowBounds = coordinates.findRootCoordinates().size
        val bounds = coordinates.boundsInWindow()

        if (bounds.isEmpty) return 0f

        val visibleTop    = maxOf(bounds.top, 0f)
        val visibleBottom = minOf(bounds.bottom, windowBounds.height.toFloat())
        val visibleLeft   = maxOf(bounds.left, 0f)
        val visibleRight  = minOf(bounds.right, windowBounds.width.toFloat())

        if (visibleBottom <= visibleTop || visibleRight <= visibleLeft) return 0f

        val visibleArea = (visibleBottom - visibleTop) * (visibleRight - visibleLeft)
        val totalArea   = coordinates.size.width.toFloat() * coordinates.size.height.toFloat()

        return if (totalArea == 0f) 0f else (visibleArea / totalArea).coerceIn(0f, 1f)
    }
}
```

### 5.4 Modifier.trackTap() — отслеживание нажатий

```kotlin
// analytics/compose/TapTracker.kt

fun Modifier.trackTap(
    blockId: String,
    blockType: BlockType,
    screenName: String,
    analyticsContext: AnalyticsContext,
): Modifier = this.pointerInput(blockId) {
    // Используем detectTapGestures — не блокирует другие жесты
    detectTapGestures(
        onTap = {
            analyticsContext.tracker.track(
                BlockEvents.tapped(
                    blockId = blockId,
                    blockType = blockType,
                    screenName = screenName,
                    sessionId = analyticsContext.session.sessionId,
                    userId = analyticsContext.session.userId
                )
            )
        }
    )
}
```

### 5.5 Scroll depth tracking

```kotlin
// analytics/compose/ScrollDepthTracker.kt

/**
 * Оборачивает LazyColumn/Column.
 * Отправляет события при пересечении 25/50/75/100% глубины.
 */
@Composable
fun TrackScrollDepth(
    screenName: String,
    scrollState: LazyListState,
    analyticsContext: AnalyticsContext,
    content: @Composable () -> Unit
) {
    // Используем derivedStateOf — пересчёт только при изменении depth
    val depthPercent by remember(scrollState) {
        derivedStateOf {
            calculateScrollDepth(scrollState)
        }
    }

    // Отслеживаем достижение milestone'ов без recomposition content
    val reportedMilestones = remember { mutableSetOf<Int>() }
    val milestones = remember { listOf(25, 50, 75, 100) }

    LaunchedEffect(depthPercent) {
        milestones
            .filter { it <= depthPercent && it !in reportedMilestones }
            .forEach { milestone ->
                reportedMilestones.add(milestone)
                analyticsContext.tracker.track(
                    ScrollEvents.depthReached(
                        screenName = screenName,
                        depthPercent = milestone,
                        sessionId = analyticsContext.session.sessionId,
                        userId = analyticsContext.session.userId
                    )
                )
            }
    }

    // Финальный отчёт при уходе с экрана
    DisposableEffect(scrollState) {
        onDispose {
            val maxDepth = reportedMilestones.maxOrNull() ?: 0
            analyticsContext.tracker.track(
                ScrollEvents.sessionSummary(
                    screenName = screenName,
                    maxDepthPercent = maxDepth,
                    totalScrollPx = scrollState.firstVisibleItemScrollOffset,
                    sessionId = analyticsContext.session.sessionId,
                    userId = analyticsContext.session.userId
                )
            )
        }
    }

    content()
}

private fun calculateScrollDepth(state: LazyListState): Int {
    val layoutInfo = state.layoutInfo
    if (layoutInfo.totalItemsCount == 0) return 0

    val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    val depth = ((lastVisibleIndex + 1).toFloat() / layoutInfo.totalItemsCount * 100).toInt()
    return depth.coerceIn(0, 100)
}
```

### 5.6 Screen-level tracking

```kotlin
// analytics/compose/ScreenTracker.kt

/**
 * Отслеживает время на экране.
 * Вызывать в корне каждого экрана один раз.
 */
@Composable
fun TrackScreen(
    screenName: String,
    analyticsContext: AnalyticsContext
) {
    val enterTime = remember { currentTimeMillis() }

    // screen_viewed при входе
    LaunchedEffect(screenName) {
        analyticsContext.tracker.track(
            ScreenEvents.viewed(
                screenName = screenName,
                sessionId = analyticsContext.session.sessionId,
                userId = analyticsContext.session.userId
            )
        )
    }

    // screen_left при выходе
    DisposableEffect(screenName) {
        onDispose {
            val timeOnScreen = currentTimeMillis() - enterTime
            analyticsContext.tracker.track(
                ScreenEvents.left(
                    screenName = screenName,
                    timeOnScreenMs = timeOnScreen,
                    sessionId = analyticsContext.session.sessionId,
                    userId = analyticsContext.session.userId
                )
            )
        }
    }
}
```

---

## 6. Production-ready подход

### 6.1 Главная проблема — избегать recomposition

**Плохо:** передавать `tracker` как State или читать его в теле Composable напрямую.

```kotlin
// ПЛОХО — каждый раз при изменении tracker → recomposition всего дерева
@Composable
fun MetricCard(tracker: AnalyticsTracker) {
    val analytics = tracker // читается в теле
}
```

**Хорошо:** `staticCompositionLocalOf` + `remember {}` + `Modifier.Node`

```kotlin
// ХОРОШО — tracker вынесен из render-пути
@Composable
fun MetricCard(
    blockId: String,
    blockType: BlockType = BlockType.METRIC_CARD,
    screenName: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // analyticsContext читается один раз, не влияет на recomposition
    val analyticsContext = rememberAnalyticsContext()

    Box(
        modifier = modifier
            .trackVisibility(
                blockId = blockId,
                blockType = blockType,
                screenName = screenName,
                analyticsContext = analyticsContext
            )
            .trackTap(
                blockId = blockId,
                blockType = blockType,
                screenName = screenName,
                analyticsContext = analyticsContext
            )
    ) {
        content()
    }
}
```

### 6.2 EventQueue — буферизация и батчинг

```kotlin
// analytics/queue/AnalyticsEventQueue.kt

class AnalyticsEventQueue(
    private val scope: CoroutineScope,
    private val flushIntervalMs: Long = 30_000L,
    private val maxBatchSize: Int = 50,
    private val onFlush: suspend (List<AnalyticsEvent>) -> Unit
) {
    private val buffer = mutableListOf<AnalyticsEvent>()
    private val mutex = Mutex()

    init {
        // Периодический flush
        scope.launch {
            while (isActive) {
                delay(flushIntervalMs)
                flush()
            }
        }
    }

    suspend fun enqueue(event: AnalyticsEvent) {
        mutex.withLock {
            buffer.add(event)
            if (buffer.size >= maxBatchSize) {
                flushLocked()
            }
        }
    }

    suspend fun flush() {
        mutex.withLock { flushLocked() }
    }

    private suspend fun flushLocked() {
        if (buffer.isEmpty()) return
        val batch = buffer.toList()
        buffer.clear()
        try {
            onFlush(batch)
        } catch (e: Exception) {
            // Retry logic или локальное сохранение при ошибке сети
            buffer.addAll(0, batch)
        }
    }
}
```

### 6.3 AnalyticsTracker с очередью

```kotlin
// analytics/BufferedAnalyticsTracker.kt

class BufferedAnalyticsTracker(
    private val delegate: AnalyticsTracker,
    scope: CoroutineScope
) : AnalyticsTracker {

    private val queue = AnalyticsEventQueue(
        scope = scope,
        onFlush = { events ->
            events.forEach { delegate.track(it) }
        }
    )

    // Быстрый неблокирующий вызов из UI-потока
    override fun track(event: AnalyticsEvent) {
        // fire-and-forget через scope
        (queue as? AnalyticsEventQueue)?.let {
            // используем launch для неблокирующей постановки в очередь
        }
    }

    override fun identify(userId: String, properties: Map<String, Any>) =
        delegate.identify(userId, properties)

    override fun reset() = delegate.reset()

    override suspend fun flush() {
        queue.flush()
        delegate.flush()
    }
}
```

---

## 7. Debounce / Throttle стратегия

### 7.1 Throttle — для scroll-событий

```kotlin
// analytics/throttle/EventThrottle.kt

/**
 * Позволяет отправить не более одного события в windowMs.
 * Потокобезопасно через AtomicLong.
 */
class EventThrottle(private val windowMs: Long = 500L) {
    private val lastEmitTime = AtomicLong(0L)

    fun shouldEmit(): Boolean {
        val now = currentTimeMillis()
        val last = lastEmitTime.get()
        return if (now - last >= windowMs) {
            lastEmitTime.compareAndSet(last, now)
        } else false
    }
}

// Использование в трекере
class ThrottledScrollTracker(
    private val delegate: AnalyticsTracker,
    private val throttleMs: Long = 500L
) : AnalyticsTracker by delegate {

    private val throttles = ConcurrentHashMap<String, EventThrottle>()

    override fun track(event: AnalyticsEvent) {
        val throttle = throttles.getOrPut(event.name) { EventThrottle(throttleMs) }
        if (throttle.shouldEmit()) {
            delegate.track(event)
        }
    }
}
```

### 7.2 Debounce — для visibility

```kotlin
// analytics/debounce/VisibilityDebouncer.kt

/**
 * Откладывает отправку события visibility на delayMs.
 * Если пользователь ушёл быстрее — событие не отправляется.
 */
class VisibilityDebouncer(
    private val scope: CoroutineScope,
    private val delayMs: Long = 1_000L
) {
    private val pendingJobs = ConcurrentHashMap<String, Job>()

    fun debounce(blockId: String, action: () -> Unit) {
        pendingJobs[blockId]?.cancel()
        pendingJobs[blockId] = scope.launch {
            delay(delayMs)
            action()
            pendingJobs.remove(blockId)
        }
    }

    fun cancel(blockId: String) {
        pendingJobs[blockId]?.cancel()
        pendingJobs.remove(blockId)
    }

    fun cancelAll() {
        pendingJobs.values.forEach { it.cancel() }
        pendingJobs.clear()
    }
}
```

### 7.3 Правила применения

| Тип события | Стратегия | Интервал |
|-------------|-----------|----------|
| `block_viewed` | debounce / dwell threshold | 2 000 ms |
| `metric_block_focus` | one-shot per block per screen session | 1 500 ms при `visibleRatio >= 0.6` |
| `scroll_depth_reached` | milestone (только 25/50/75/100, один раз) | — |
| `block_tapped` | без периодического spam; только по фактическому tap | — |
| `screen_viewed` | без ограничений (1 раз на сессию экрана) | — |
| `chart_interacted` | throttle | 500 ms |
| `filter_applied` | debounce | 400 ms |

---

## 8. Session ID / User ID — хранение

### 8.1 Интерфейс хранилища (`commonMain`)

```kotlin
// analytics/session/AnalyticsStorage.kt
interface AnalyticsStorage {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
}
```

### 8.2 Android-реализация (SharedPreferences)

```kotlin
// androidMain/analytics/session/AndroidAnalyticsStorage.kt
class AndroidAnalyticsStorage(context: Context) : AnalyticsStorage {

    private val prefs = context.getSharedPreferences(
        "analytics_prefs", Context.MODE_PRIVATE
    )

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}
```

### 8.3 iOS-реализация (UserDefaults)

```kotlin
// iosMain/analytics/session/IosAnalyticsStorage.kt
import platform.Foundation.NSUserDefaults

class IosAnalyticsStorage : AnalyticsStorage {

    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String): String? =
        defaults.stringForKey(key)

    override fun putString(key: String, value: String) {
        defaults.setObject(value, key)
    }

    override fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }
}
```

### 8.4 AnalyticsSession — управление сессией

Текущая production-реализация хранит не только `session_id`, но и стабильный `anonymous_id`. Он используется как fallback `distinct id`, если реальный `user_id` отсутствует. Это позволяет различать установки без отправки PII.

```kotlin
// analytics/session/AnalyticsSession.kt

class AnalyticsSession(private val storage: AnalyticsStorage) {

    companion object {
        private const val KEY_USER_ID    = "analytics_user_id"
        private const val KEY_ANON_ID    = "analytics_anonymous_id"
        private const val KEY_SESSION_ID = "analytics_session_id"
        private const val KEY_SESSION_TS = "analytics_session_ts"
        private const val SESSION_TIMEOUT_MS = 30 * 60 * 1_000L // 30 минут
    }

    val userId: String?
        get() = storage.getString(KEY_USER_ID)

    val anonymousId: String
        get() = storage.getString(KEY_ANON_ID) ?: createAnonymousId()

    val sessionId: String
        get() = storage.getString(KEY_SESSION_ID) ?: createNewSession()

    /**
     * Вызывать при старте приложения и возврате из фона.
     * Если прошло > 30 минут — создаём новую сессию.
     */
    fun refreshSession() {
        val lastTs = storage.getString(KEY_SESSION_TS)?.toLongOrNull() ?: 0L
        val now = currentTimeMillis()
        if (now - lastTs > SESSION_TIMEOUT_MS) {
            createNewSession()
        } else {
            storage.putString(KEY_SESSION_TS, now.toString())
        }
    }

    fun setUserId(userId: String?) {
        if (userId.isNullOrBlank()) {
            storage.remove(KEY_USER_ID)
        } else {
            storage.putString(KEY_USER_ID, userId)
        }
    }

    fun reset() {
        storage.remove(KEY_SESSION_ID)
        storage.remove(KEY_USER_ID)
        storage.remove(KEY_SESSION_TS)
        createNewSession()
    }

    private fun createNewSession(): String {
        val newId = generateUuid()
        storage.putString(KEY_SESSION_ID, newId)
        storage.putString(KEY_SESSION_TS, currentTimeMillis().toString())
        return newId
    }

    private fun createAnonymousId(): String {
        val id = "anon_${generateUuid()}"
        storage.putString(KEY_ANON_ID, id)
        return id
    }
}

// expect fun generateUuid(): String
// actual fun generateUuid(): String = UUID.randomUUID().toString()   // androidMain
// actual fun generateUuid(): String = NSUUID().UUIDString             // iosMain
```

### 8.5 Binding identity к auth lifecycle

- Источник auth identity: `AuthManager.currentUserId`.
- Перед отправкой в аналитику user id нормализуется в безопасный вид `usr_<stable_hash>`.
- Привязка происходит в `composeApp/src/commonMain/kotlin/com/spbu/projecttrack/analytics/compose/AnalyticsIdentityBinder.kt`.
- При логине binder вызывает:
  - `analyticsSession.setUserId(safeUserId)`
  - `analyticsTracker.identify(safeUserId)`
- При logout binder сначала делает `flush()`, потом `reset()`, чтобы не потерять уже набранные события предыдущего пользователя.
- Если auth identity ещё неизвестна, analytics продолжает работать на стабильном `anonymous_id`.

### 8.6 App lifecycle hooks

- Android: в `composeApp/src/androidMain/kotlin/com/spbu/projecttrack/App.android.kt`
  - `ON_START` -> `analyticsSession.refreshSession()`
  - `ON_STOP` -> `analyticsTracker.flush()`
- iOS: в `composeApp/src/iosMain/kotlin/com/spbu/projecttrack/App.ios.kt`
  - `UIApplicationWillEnterForegroundNotification` -> `analyticsSession.refreshSession()`
  - `UIApplicationDidEnterBackgroundNotification` -> `analyticsTracker.flush()`
  - `UIApplicationWillTerminateNotification` -> `analyticsTracker.flush()`

Это нужно, чтобы не терять батчи PostHog при быстром background/terminate и одновременно корректно ротировать session id.

---

## 9. Naming Convention для событий

### Формат

```
{object}_{action}
```

- **object** — что за сущность: `block`, `screen`, `scroll`, `filter`, `chart`, `session`
- **action** — что произошло: `viewed`, `tapped`, `expanded`, `ignored`, `left`, `applied`

### Примеры

| Событие | Описание |
|---------|----------|
| `screen_viewed` | Пользователь открыл экран |
| `screen_left` | Пользователь ушёл с экрана |
| `block_viewed` | Блок был виден достаточно долго |
| `block_tapped` | Нажатие на блок |
| `block_expanded` | Раскрытие деталей блока |
| `block_ignored` | Блок промотан без просмотра |
| `scroll_depth_reached` | Достигнута глубина прокрутки |
| `scroll_session_summary` | Итог прокрутки при уходе |
| `metric_block_focus` | Блок статистики действительно изучили |
| `metric_block_tap` | Осмысленное взаимодействие с блоком статистики |
| `project_stats_screen_close` | Итоговая сводка по сессии экрана статистики проекта |
| `user_stats_screen_close` | Итоговая сводка по сессии экрана личной статистики |
| `stats_screen_settings_changed` | Пользователь изменил набор или порядок блоков |
| `stats_repository_changed` | Пользователь сменил репозиторий на экране статистики |
| `stats_date_range_changed` | Пользователь сменил период на экране статистики |
| `stats_rapid_threshold_changed` | Пользователь сменил rapid threshold |
| `stats_detail_opened` | Пользователь открыл detail view секции |
| `stats_settings_opened` | Пользователь открыл настройки экрана статистики |
| `stats_export_requested` | Пользователь запросил экспорт |
| `project_stats_member_opened` | Пользователь открыл статистику участника проекта |
| `filter_applied` | Применён фильтр |
| `chart_interacted` | Взаимодействие с графиком |
| `session_started` | Начало новой сессии |

### Свойства — snake_case

```
block_id, block_type, screen_name, duration_ms, visible_ratio,
depth_percent, tap_count, filter_name, filter_value
```

---

## 10. Интеграция с Firebase Analytics

### 10.1 Что реализовано сейчас

- В `commonMain` есть единый `composeApp/src/commonMain/kotlin/com/spbu/projecttrack/analytics/firebase/FirebaseAnalyticsTracker.kt`, который получает `AnalyticsEvent` и маппит его под ограничения Firebase.
- На Android события отправляются напрямую через SDK в `composeApp/src/androidMain/kotlin/com/spbu/projecttrack/analytics/firebase/FirebaseAnalyticsTracker.android.kt`.
- На iOS события идут через Swift host app bridge: `composeApp/src/iosMain/kotlin/com/spbu/projecttrack/analytics/firebase/FirebaseAnalyticsTracker.ios.kt`, `iosApp/iosApp/FirebaseAnalyticsBridge.swift`, `iosApp/iosApp/FirebaseAppDelegate.swift`.
- Firebase подключён в `CompositeAnalyticsTracker`, поэтому экраны и Compose modifiers не знают, какой backend используется.

### 10.2 Ограничения и маппинг

- `screen_viewed` маппится в `screen_view`, чтобы Firebase корректно считал screen analytics.
- Имена событий и параметров нормализуются под Firebase:
  - только `[a-zA-Z0-9_]`
  - длина имени до `40`
  - зарезервированные префиксы `firebase_`, `google_`, `ga_` заменяются на безопасные
- String values режутся до `100` символов.
- Пользовательский идентификатор в Firebase берётся из `userId`, а если его нет — используется стабильный анонимный id установки.
- `userId` в production не является raw auth id: он сначала переводится в безопасный вид `usr_<stable_hash>`.
- `flush()` для Firebase пустой, потому что батчингом управляет сам SDK.

### 10.3 Важные platform notes

- Android: automatic screen reporting Firebase выключен в `composeApp/src/androidMain/AndroidManifest.xml`, иначе в single-activity Compose app появятся дубликаты screen events.
- iOS: automatic screen reporting тоже выключен в `iosApp/iosApp/Info.plist`.
- iOS runtime требует `GoogleService-Info.plist` в bundle. Если файла нет, bridge не падает, а просто не отправляет события в Firebase.

### 10.4 Зависимости

- Android: `com.google.firebase:firebase-analytics` через Firebase BOM в `composeApp/build.gradle.kts`.
- iOS: Swift Package `firebase-ios-sdk` в `iosApp/iosApp.xcodeproj/project.pbxproj`.

---

## 11. Интеграция с PostHog

### 11.1 Что реализовано сейчас

- Текущая реализация не использует официальный PostHog SDK.
- В проекте стоит собственный HTTP tracker: `composeApp/src/commonMain/kotlin/com/spbu/projecttrack/analytics/PostHogAnalyticsTracker.kt`.
- Он работает в `commonMain`, батчит события локально и шлёт их в `/batch/`.

### 11.2 Поведение runtime

- Flush происходит:
  - каждые `30` секунд
  - или при накоплении `20` событий
- Дополнительно `flush()` вызывается при уходе приложения в background и перед `reset()` на logout.
- `distinct_id` отправляется через свойства capture event.
- При `HTTP 4xx/5xx` ошибки логируются, а не проглатываются молча.
- В качестве fallback id используется стабильный `anonymousId`, если `userId` ещё не известен.

### 11.3 Что выбрать для каких задач

- Firebase лучше держать для верхнеуровневых продуктовых воронок, аудиторий и возможной связки с Remote Config / A-B testing.
- PostHog лучше подходит для сырого event stream, ad-hoc product analytics и дебага пользовательского поведения.
- Текущая архитектура поддерживает fan-out сразу в оба backend'а, поэтому экраны не нужно дублировать под каждую систему отдельно.

---

## 12. Что НЕ отправлять

| Категория | Примеры | Почему |
|-----------|---------|--------|
| **PII (Personal Identifiable Information)** | email, ФИО, телефон, IP | GDPR / закон о персональных данных |
| **Финансовые данные** | суммы, номера счетов | PCI DSS |
| **Данные из полей ввода** | пароли, поисковые запросы с именами | безопасность |
| **Высокочастотные события без смысла** | каждый pixel scroll | noise, стоимость |
| **Системные внутренние ID** | database row ID, UUID объектов БД | бесполезны для аналитики |
| **Точные временные метки длинных операций** | время рендера каждого кадра | уместно в crash reporting, не в analytics |
| **Избыточные дубликаты** | 10 `block_viewed` для одного блока | шум, портит метрики |

### Правила фильтрации

```kotlin
// analytics/filter/PrivacyEventFilter.kt

class PrivacyEventFilter(private val delegate: AnalyticsTracker) : AnalyticsTracker by delegate {

    private val blockedPropertyKeys = setOf(
        "email", "phone", "name", "password", "token", "ip_address"
    )

    override fun track(event: AnalyticsEvent) {
        val sanitized = event.copy(
            properties = event.properties.filterKeys { key ->
                key.lowercase() !in blockedPropertyKeys
            }
        )
        delegate.track(sanitized)
    }
}
```

---

## 13. Heatmap / Focus Analytics

### Концепция

Heatmap строится **на основе событий**, а не скриншотов. В текущей реализации основа для анализа:
- `block_viewed` — блок был реально виден достаточно долго
- `block_ignored` — блок появился, но не дотянул до просмотра
- `metric_block_focus` — блок на экране статистики проекта или личной статистики действительно изучили
- `metric_block_tap` — пользователь взаимодействовал с блоком
- `project_stats_screen_close` / `user_stats_screen_close` — итоговая сводка по сессии экрана

Для базового heatmap по-прежнему достаточно `block_viewed`, но для перестановки блоков в статистике лучше смотреть на связку `viewed + ignored + focus + tap`.

Каждый `block_viewed` содержит:
- `block_id` — идентификатор блока
- `duration_ms` — время внимания
- `visible_ratio` — процент видимости
- `screen_name` — экран источника

### Агрегация событий для heatmap

```
SELECT
    block_id,
    block_type,
    COUNT(*) AS view_count,
    AVG(duration_ms) AS avg_attention_ms,
    SUM(duration_ms) AS total_attention_ms,
    AVG(visible_ratio) AS avg_visibility,
    COUNT(*) FILTER (WHERE duration_ms > 5000) AS deep_engagement_count
FROM block_events
WHERE event_name = 'block_viewed'
  AND screen_name = 'statistics_screen'
  AND timestamp > NOW() - INTERVAL '7 days'
GROUP BY block_id, block_type
ORDER BY total_attention_ms DESC;
```

### Метрики внимания

| Метрика | Формула | Интерпретация |
|---------|---------|---------------|
| **Attention Score** | `avg_attention_ms × avg_visibility` | Взвешенное время |
| **Focus Rate** | `focus_count / view_count` | Доля просмотров, перешедших в осмысленное внимание |
| **Engagement Rate** | `tap_count / view_count` | CTR блока |
| **Ignore Rate** | `ignore_count / (view_count + ignore_count)` | Насколько блок игнорируют |
| **Scroll-past Rate** | события `block_ignored` / всего показов | Блок промотали |

### Клиентская передача позиции блока

Для построения визуальной heatmap отправляйте **относительную позицию блока** на экране:

```kotlin
data class BlockPosition(
    val scrollOffsetPx: Int,    // позиция от начала scroll-контейнера
    val heightPx: Int,          // высота блока
    val orderIndex: Int         // порядковый номер в списке
)

// В событие добавляем:
"block_order_index" to orderIndex,
"block_height_px"   to heightPx
```

---

## 14. Готовая реализация MetricBlock tracking

### 14.1 Полноценный MetricCard с трекингом

```kotlin
// ui/components/TrackedMetricCard.kt

@Composable
fun TrackedMetricCard(
    blockId: String,
    screenName: String,
    title: String,
    value: String,
    trend: Float? = null,
    isExpandable: Boolean = false,
    modifier: Modifier = Modifier
) {
    val analyticsContext = rememberAnalyticsContext()
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .trackVisibility(
                blockId = blockId,
                blockType = BlockType.METRIC_CARD,
                screenName = screenName,
                analyticsContext = analyticsContext
            )
            .trackTap(
                blockId = blockId,
                blockType = BlockType.METRIC_CARD,
                screenName = screenName,
                analyticsContext = analyticsContext
            )
            .then(
                if (isExpandable) Modifier.clickable {
                    isExpanded = !isExpanded
                    if (isExpanded) {
                        analyticsContext.tracker.track(
                            BlockEvents.expanded(
                                blockId = blockId,
                                blockType = BlockType.METRIC_CARD,
                                screenName = screenName,
                                sessionId = analyticsContext.session.sessionId,
                                userId = analyticsContext.session.userId
                            )
                        )
                    }
                } else Modifier
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium)

            trend?.let {
                TrendIndicator(trend = it)
            }

            AnimatedVisibility(visible = isExpanded) {
                MetricCardDetails(blockId = blockId)
            }
        }
    }
}
```

### 14.2 Полный экран статистики

```kotlin
// ui/screens/StatisticsScreen.kt

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    analyticsTracker: AnalyticsTracker = LocalAnalyticsTracker.current,
    analyticsSession: AnalyticsSession = LocalAnalyticsSession.current
) {
    val analyticsContext = rememberAnalyticsContext()
    val listState = rememberLazyListState()

    val screenName = "statistics_screen"

    // Трекинг экрана
    TrackScreen(
        screenName = screenName,
        analyticsContext = analyticsContext
    )

    // Трекинг прокрутки с оборачиванием
    TrackScrollDepth(
        screenName = screenName,
        scrollState = listState,
        analyticsContext = analyticsContext
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // KPI секция
            item {
                TrackedMetricCard(
                    blockId = "kpi_total_projects",
                    screenName = screenName,
                    title = "Всего проектов",
                    value = viewModel.totalProjects.toString()
                )
            }

            item {
                TrackedMetricCard(
                    blockId = "kpi_active_students",
                    screenName = screenName,
                    title = "Активных студентов",
                    value = viewModel.activeStudents.toString()
                )
            }

            // График
            item {
                TrackedChartBlock(
                    blockId = "chart_project_progress",
                    screenName = screenName,
                    title = "Прогресс проектов"
                ) {
                    ProjectProgressChart(data = viewModel.chartData)
                }
            }

            // Секции студентов
            items(
                items = viewModel.studentStats,
                key = { it.studentId }
            ) { student ->
                TrackedStudentStatsBlock(
                    blockId = "student_stats_${student.studentId}",
                    screenName = screenName,
                    student = student
                )
            }
        }
    }
}

// Переиспользуемый Chart block
@Composable
fun TrackedChartBlock(
    blockId: String,
    screenName: String,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val analyticsContext = rememberAnalyticsContext()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .trackVisibility(
                blockId = blockId,
                blockType = BlockType.CHART,
                screenName = screenName,
                analyticsContext = analyticsContext
            )
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}
```

### 14.3 DI — как это собрано сейчас

Сейчас аналитика не через Koin-модуль, а через `DependencyContainer`:

```kotlin
val analyticsSession: AnalyticsSession by lazy {
    AnalyticsSession(createAppPreferences()).also { it.refreshSession() }
}

val analyticsTracker: AnalyticsTracker by lazy {
    val trackers = buildList {
        add(LoggingAnalyticsTracker())

        if (AnalyticsConfig.ANALYTICS_ENABLED && AnalyticsConfig.POSTHOG_API_KEY.isNotBlank()) {
            add(
                PostHogAnalyticsTracker(
                    apiKey = AnalyticsConfig.POSTHOG_API_KEY,
                    host = AnalyticsConfig.POSTHOG_HOST,
                    httpClient = httpClient,
                )
            )
        }

        if (AnalyticsConfig.ANALYTICS_ENABLED) {
            add(FirebaseAnalyticsTracker())
        }
    }
    CompositeAnalyticsTracker(trackers)
}
```

Это важно: Firebase и PostHog подключаются параллельно, а UI работает только с единым `AnalyticsTracker`.

### 14.4 Инициализация в app roots

```kotlin
// App.android.kt / App.ios.kt

val analytics = rememberAnalyticsContext()
val currentUserId by AuthManager.currentUserId.collectAsState()

BindAnalyticsIdentity(
    rawUserId = currentUserId?.toString(),
    analyticsTracker = DependencyContainer.analyticsTracker,
    analyticsSession = DependencyContainer.analyticsSession,
)

// Android:
// ON_START -> analyticsSession.refreshSession()
// ON_STOP  -> analyticsTracker.flush()

// iOS:
// UIApplicationWillEnterForeground -> analyticsSession.refreshSession()
// UIApplicationDidEnterBackground  -> analyticsTracker.flush()
// UIApplicationWillTerminate       -> analyticsTracker.flush()
```

---

## 15. Памятка по производительности

| Правило | Реализация |
|---------|-----------|
| Не читать tracker в теле Composable | `staticCompositionLocalOf` + `Modifier.Node` |
| Не создавать lambda в теле Composable | `remember { }` для трекера и коллбэков |
| Scroll-события через `derivedStateOf` | Пересчёт только при реальном изменении depth |
| Visibility через `ModifierNode.onGloballyPositioned` | Без State, только side-effects |
| Батчинг событий | `AnalyticsEventQueue` — flush каждые 30 с |
| Debounce visibility | `block_viewed`: минимум 2 с при `visibleRatio >= 0.5`; `metric_block_focus`: минимум 1.5 с при `visibleRatio >= 0.6` |
| Throttle scroll | Не чаще 1 раза в 500 мс |
| Нет analytics в `LazyColumn` key | `key` должен быть стабильным, не содержать трекинг-логику |
| `DisposableEffect` для cleanup | Всегда отписываться и финально отправлять события |
| Тест с baseline profile | Убедитесь, что analytics-код не попадает в critical rendering path |

### Профилирование: как убедиться, что нет проблем

```kotlin
// Временная обёртка для профилирования в debug
class ProfilingAnalyticsTracker(
    private val delegate: AnalyticsTracker
) : AnalyticsTracker by delegate {

    override fun track(event: AnalyticsEvent) {
        val start = currentTimeMillis()
        delegate.track(event)
        val duration = currentTimeMillis() - start
        if (duration > 16L) { // > 1 frame
            println("SLOW analytics.track(${event.name}): ${duration}ms")
        }
    }
}
```

---

## Итоговая схема потока событий

```
[User interacts with MetricCard]
         │
         ▼
[Modifier.trackVisibility / trackTap]   ← нет recomposition
         │
         ▼
[PrivacyEventFilter]                     ← убирает PII
         │
         ▼
[CompositeAnalyticsTracker]              ← fan-out
    │              │
    ▼              ▼
[Firebase]    [PostHog]
 (batched)    (batched)
    │              │
    ▼              ▼
 BigQuery      PostHog DB
    └──────────────┘
           │
           ▼
   Dashboard / Heatmap
   (Retool / PostHog Insights)
```

---
