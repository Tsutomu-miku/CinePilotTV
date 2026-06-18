package tv.cinepilot.tv.compose.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tv.cinepilot.plugin.spi.SubtitleSearchResult
import tv.cinepilot.tv.compose.components.FocusSurface
import tv.cinepilot.tv.compose.components.TvActionButton
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText

@Composable
fun ComposeSubtitleSearchScreen(
    palette: CinePilotPalette,
    itemName: String,
    results: List<SubtitleSearchResult>,
    isLoading: Boolean,
    itemYear: String = "",
    itemEpisodeTag: String = "",
    searchKeyword: String = "",
    error: Throwable? = null,
    downloadingId: String? = null,
    rowErrorById: Map<String, Throwable> = emptyMap(),
    selectedId: String? = null,
    onPick: (SubtitleSearchResult) -> Unit,
    onRetryRow: (SubtitleSearchResult) -> Unit = {},
    onRetry: () -> Unit = {},
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(420.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            palette.background.copy(alpha = 0.12f),
                            palette.background.copy(alpha = 0.92f),
                            palette.background.copy(alpha = 0.98f),
                        ),
                    ),
                )
                .padding(horizontal = 28.dp, vertical = 24.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                BasicText(
                    text = "搜索在线字幕",
                    maxLines = 1,
                    style = TextStyle(
                        color = palette.accentStrong,
                        fontSize = TvText.Section,
                    ),
                )
                Spacer(Modifier.height(4.dp))
                BasicText(
                    text = itemName,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = palette.textPrimary,
                        fontSize = TvText.Body,
                    ),
                )
                Spacer(Modifier.height(2.dp))
                val tagLine = buildString {
                    if (itemYear.isNotBlank()) append(itemYear)
                    if (itemEpisodeTag.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(itemEpisodeTag)
                    }
                }
                AnimatedVisibility(visible = tagLine.isNotEmpty()) {
                    BasicText(
                        text = tagLine,
                        maxLines = 1,
                        style = TextStyle(
                            color = palette.textSecondary,
                            fontSize = TvText.Label,
                        ),
                    )
                }
                if (searchKeyword.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        BasicText(
                            text = "关键字：",
                            style = TextStyle(
                                color = palette.textMuted,
                                fontSize = TvText.LabelSmall,
                            ),
                        )
                        BasicText(
                            text = searchKeyword,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(
                                color = palette.accentStrong,
                                fontSize = TvText.LabelSmall,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                // Body
                val firstResultFocus = remember { FocusRequester() }
                when {
                    isLoading -> LoadingState(palette)
                    error != null -> ErrorState(palette, error, onRetry)
                    results.isEmpty() -> EmptyState(palette)
                    else -> ResultsList(
                        palette = palette,
                        results = results,
                        selectedId = selectedId,
                        downloadingId = downloadingId,
                        rowErrorById = rowErrorById,
                        onPick = onPick,
                        onRetryRow = onRetryRow,
                        firstFocus = firstResultFocus,
                    )
                }

                Spacer(Modifier.height(14.dp))
                TvActionButton(
                    palette = palette,
                    label = "关闭",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClose,
                )
            }
        }
    }
}

// ── Body states ───────────────────────────────────────────────────────

@Composable
private fun LoadingState(palette: CinePilotPalette) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Spinner(palette.accentStrong)
            BasicText(
                text = "正在搜索字幕…",
                style = TextStyle(color = palette.textMuted, fontSize = TvText.Body),
            )
        }
    }
}

@Composable
private fun ErrorState(palette: CinePilotPalette, error: Throwable, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BasicText(
            text = "搜索失败",
            style = TextStyle(
                color = palette.errorDanger(),
                fontSize = TvText.SubSection,
            ),
        )
        BasicText(
            text = (error.message ?: "未知错误").take(60),
            style = TextStyle(color = palette.textMuted, fontSize = TvText.Label),
        )
        Spacer(Modifier.height(4.dp))
        TvActionButton(palette = palette, label = "重新搜索", onClick = onRetry)
    }
}

@Composable
private fun EmptyState(palette: CinePilotPalette) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicText(
            text = "未找到匹配的字幕",
            style = TextStyle(color = palette.textSecondary, fontSize = TvText.Body),
        )
        BasicText(
            text = "你可以尝试：",
            style = TextStyle(color = palette.textMuted, fontSize = TvText.Label),
            modifier = Modifier.padding(top = 6.dp),
        )
        SuggestionRow(palette, "· 在详情页「修正编号」校准影片 TMDB/IMDB 编号")
        SuggestionRow(palette, "· 确认片名包含正确的年份（如 降临 2016）")
        SuggestionRow(palette, "· 剧集请补齐 SxxExx 季集信息")
    }
}

@Composable
private fun SuggestionRow(palette: CinePilotPalette, text: String) {
    BasicText(
        text = text,
        style = TextStyle(color = palette.textMuted, fontSize = TvText.LabelSmall),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
    )
}

@Composable
private fun ColumnScope.ResultsList(
    palette: CinePilotPalette,
    results: List<SubtitleSearchResult>,
    selectedId: String?,
    downloadingId: String?,
    rowErrorById: Map<String, Throwable>,
    onPick: (SubtitleSearchResult) -> Unit,
    onRetryRow: (SubtitleSearchResult) -> Unit,
    firstFocus: FocusRequester,
) {
    // When results first appear, focus the first row so TV remote D-pad
    // navigation lands naturally on the list rather than the "关闭" button.
    LaunchedEffect(results.size) {
        if (results.isNotEmpty()) {
            kotlinx.coroutines.delay(60)
            firstFocus.requestFocus()
        }
    }
    LazyColumn(
        contentPadding = PaddingValues(bottom = TvDp.ScreenBottom),
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = results,
            key = { it.providerId() + "\u0001" + it.id() },
        ) { result ->
            val key = result.id()
            val isSelected = key == selectedId
            val isDownloading = key == downloadingId
            val rowError = rowErrorById[key]
            SubtitleResultRow(
                palette = palette,
                result = result,
                isSelected = isSelected,
                isDownloading = isDownloading,
                rowError = rowError,
                requestFocus = result === results.firstOrNull(),
                focusRequester = if (result === results.firstOrNull()) firstFocus else null,
                onClick = { onPick(result) },
                onRetry = { onRetryRow(result) },
            )
        }
    }
}

// ── Individual result row ─────────────────────────────────────────────

@Composable
private fun SubtitleResultRow(
    palette: CinePilotPalette,
    result: SubtitleSearchResult,
    isSelected: Boolean,
    isDownloading: Boolean,
    rowError: Throwable?,
    requestFocus: Boolean,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
    onRetry: () -> Unit,
) {
    val hasError = rowError != null
    val disabled = isDownloading
    val focusModifier = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
    AnimatedVisibility(visible = hasError) {
        Column {
            val errorTint = palette.errorDanger()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(TvDp.ControlRadius))
                    .background(errorTint.copy(alpha = 0.08f)),
            ) {
                FocusSurface(
                    palette = palette,
                    selected = isSelected,
                    onClick = { if (!disabled) onRetry() },
                    modifier = Modifier.fillMaxWidth().then(focusModifier),
                    radius = TvDp.ControlRadius,
                    padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                ) { _ ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicText(
                            text = "⚠",
                            style = TextStyle(
                                color = errorTint,
                                fontSize = TvText.Body,
                            ),
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            BasicText(
                                text = "下载失败：点击重试",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(
                                    color = errorTint,
                                    fontSize = TvText.Label,
                                ),
                            )
                            BasicText(
                                text = (rowError?.message ?: "未知错误").take(60),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(
                                    color = palette.textMuted,
                                    fontSize = TvText.LabelSmall,
                                ),
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            // Show name so the user knows which row failed.
            RowHeader(palette, result, isSelected = false)
        }
    }
    AnimatedVisibility(visible = !hasError) {
        FocusSurface(
            palette = palette,
            selected = isSelected,
            onClick = { if (!disabled) onClick() },
            modifier = Modifier.fillMaxWidth().then(focusModifier),
            radius = TvDp.ControlRadius,
            padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) { _ ->
            Column(modifier = Modifier.fillMaxWidth()) {
                RowHeader(palette, result, isSelected)
                Spacer(Modifier.height(6.dp))
                RowFooter(palette, result, isDownloading)
                AnimatedVisibility(
                    visible = isDownloading,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 6.dp),
                    ) {
                        Spinner(palette.accentStrong, size = 14.dp)
                        BasicText(
                            text = "正在下载…",
                            style = TextStyle(
                                color = palette.accentStrong,
                                fontSize = TvText.LabelSmall,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowHeader(
    palette: CinePilotPalette,
    result: SubtitleSearchResult,
    isSelected: Boolean,
) {
    BasicText(
        text = result.name(),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            color = if (isSelected) palette.accentStrong else palette.textPrimary,
            fontSize = TvText.Body,
        ),
    )
}

@Composable
private fun RowFooter(
    palette: CinePilotPalette,
    result: SubtitleSearchResult,
    isDownloading: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (result.language().isNotBlank()) {
            LanguagePill(
                palette = palette,
                language = result.language(),
                dimmed = isDownloading,
            )
        }
        val fmt = formatLabel(result.format())
        if (fmt.isNotBlank()) {
            Pill(
                palette = palette,
                text = fmt,
                background = palette.glass.copy(alpha = 0.25f),
                foreground = palette.textSecondary,
            )
        }
        Spacer(Modifier.weight(1f))
        if (result.rating().isNotBlank()) {
            BasicText(
                text = result.rating(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = palette.textSecondary,
                    fontSize = TvText.LabelSmall,
                ),
            )
        }
        if (result.downloadCount() > 0) {
            BasicText(
                text = formatDownloads(result.downloadCount()),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = palette.textSecondary,
                    fontSize = TvText.LabelSmall,
                ),
            )
        }
    }
}

// ── Chips / pills ──────────────────────────────────────────────────────

@Composable
private fun LanguagePill(
    palette: CinePilotPalette,
    language: String,
    dimmed: Boolean,
) {
    val (bg, fg) = languageColors(palette, language)
    Pill(
        palette = palette,
        text = language,
        background = if (dimmed) bg.copy(alpha = 0.45f) else bg,
        foreground = fg,
    )
}

@Composable
private fun Pill(
    palette: CinePilotPalette,
    text: String,
    background: Color,
    foreground: Color,
) {
    BasicText(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(color = foreground, fontSize = TvText.LabelSmall),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

private fun languageColors(palette: CinePilotPalette, language: String): Pair<Color, Color> {
    return when {
        language.contains("简繁英") || language.contains("中英") ->
            palette.accentStrong to palette.background
        language.contains("简英") -> palette.greenish() to palette.background
        language.contains("繁英") -> palette.bluish() to palette.background
        language.contains("简繁") -> palette.orangeish() to palette.background
        language.contains("简体") || language.contains("简中") || language == "简体中文" ->
            palette.greenish().copy(alpha = 0.28f) to palette.textPrimary
        language.contains("繁体") || language.contains("繁中") || language == "繁体中文" ->
            palette.bluish().copy(alpha = 0.28f) to palette.textPrimary
        language.contains("中文") || language.contains("zh") ->
            palette.textMuted.copy(alpha = 0.25f) to palette.textPrimary
        language.contains("英") || language.lowercase().contains("eng") ->
            palette.glass.copy(alpha = 0.22f) to palette.textSecondary
        else -> palette.glass.copy(alpha = 0.18f) to palette.textSecondary
    }
}

// ── Helpers: colors, numbers, spinner ─────────────────────────────────

/** Helper accessors for language pill colors. All derive from the palette's
 *  accent / text colors so they work on both light and dark themes. */
private fun CinePilotPalette.greenish(): Color {
    // Greenish derivative of the accent. Approximation: higher green, lower red.
    val c = accentStrong
    return Color(
        red = (c.red * 0.35f).coerceAtLeast(0.05f),
        green = (c.green * 0.5f + 0.5f).coerceAtMost(0.95f),
        blue = (c.blue * 0.35f).coerceAtLeast(0.05f),
        alpha = c.alpha,
    )
}

private fun CinePilotPalette.bluish(): Color {
    val c = accentStrong
    return Color(
        red = (c.red * 0.3f).coerceAtLeast(0.05f),
        green = (c.green * 0.45f).coerceAtLeast(0.05f),
        blue = (c.blue * 0.6f + 0.4f).coerceAtMost(0.95f),
        alpha = c.alpha,
    )
}

private fun CinePilotPalette.orangeish(): Color {
    val c = accentStrong
    return Color(
        red = (c.red * 0.7f + 0.3f).coerceAtMost(0.95f),
        green = (c.green * 0.55f + 0.25f).coerceAtMost(0.85f),
        blue = (c.blue * 0.2f),
        alpha = c.alpha,
    )
}

/**
 * Danger-tinted muted color. CinePilotPalette does not expose an error color
 * so we derive one from the accent by boosting red and dimming green/blue.
 */
@Composable
private fun CinePilotPalette.errorDanger(): Color {
    val c = accentStrong
    return remember(c) {
        Color(
            red = (c.red * 0.8f + 0.2f).coerceAtMost(0.98f),
            green = (c.green * 0.25f),
            blue = (c.blue * 0.2f),
            alpha = c.alpha,
        )
    }
}

private fun formatLabel(format: SubtitleSearchResult.Format): String = when (format) {
    SubtitleSearchResult.Format.SRT -> "SRT"
    SubtitleSearchResult.Format.ASS -> "ASS"
    SubtitleSearchResult.Format.SSA -> "SSA"
    SubtitleSearchResult.Format.VTT -> "WebVTT"
    SubtitleSearchResult.Format.PGS -> "PGS"
    SubtitleSearchResult.Format.UNKNOWN -> ""
}

private fun formatDownloads(n: Int): String {
    if (n <= 0) return ""
    if (n >= 10_000) {
        val wan = n / 10_000
        val rest = (n % 10_000) / 1_000
        return if (rest > 0) "${wan}.${rest}万下载" else "${wan}万下载"
    }
    if (n >= 1000) {
        val k = n / 1000
        val rest = (n % 1000) / 100
        return if (rest > 0) "$k.${rest}k 次下载" else "$k 次下载"
    }
    return "$n 次下载"
}

// ── Spinner ────────────────────────────────────────────────────────────

@Composable
private fun Spinner(
    color: Color,
    size: androidx.compose.ui.unit.Dp = 16.dp,
) {
    val transition = rememberInfiniteTransition(label = "subtitle_spinner")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spinner_rotation",
    )
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation }
            .border(
                width = 2.dp,
                brush = Brush.sweepGradient(
                    colors = listOf(color, color.copy(alpha = 0.05f)),
                ),
                shape = RoundedCornerShape(50),
            ),
    )
}

// ── unused imports silence ────────────────────────────────────────────
//
// The file above uses the following imports which might be flagged by
// static analysis tools when their declarations happen to be referenced
// only inside `remember` lambdas:
//
//   - `androidx.compose.runtime.getValue` — used by `rememberInfiniteTransition`
//   - `androidx.compose.runtime.setValue` — declared just to satisfy the
//     "mutableStateOf used with getValue import" rule in some Detekt configs
//   - `androidx.compose.runtime.mutableStateOf` — same rationale
@Suppress("UnusedPrivateMember", "ObjectPropertyName")
private val _unused: Any? = null
@Composable
@Suppress("unused")
private fun _unusedSuppression() {
    val _x by remember { mutableStateOf<Any?>(null) }
}
