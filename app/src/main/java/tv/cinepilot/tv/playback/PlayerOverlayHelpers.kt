package tv.cinepilot.tv.playback

import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaStreamType
import tv.cinepilot.core.protocol.MediaTicks
import tv.cinepilot.tv.player.DisplayCapabilities
import tv.cinepilot.tv.player.Media3PlayerHost

/**
 * Ordered speed cycle used by the OSD "1.0x" shortcut button.
 * The preset order is intentionally linear so the user can always
 * recover 1.0x by walking forward once past the top of the list.
 */
internal val PLAYBACK_SPEED_PRESETS: FloatArray =
    floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

/** Human-readable subtitle description shown below the OSD subtitle icon. */
internal fun subtitleShortcutLabel(playerHost: Media3PlayerHost): String {
    val subs = playerHost.availableSubtitleStreams()
    if (subs.isEmpty()) return "无"
    val index = playerHost.currentSubtitleStreamIndex() ?: return "关"
    val match = subs.firstOrNull { it.index() == index }
    val title = match?.displayTitle().orEmpty()
    val short = when {
        title.isBlank() -> "字幕"
        title.length <= 8 -> title
        else -> title.take(7) + "…"
    }
    return if (playerHost.subtitlesEnabled()) short else "关"
}

/** Human-readable audio description shown below the OSD audio icon. */
internal fun audioShortcutLabel(playerHost: Media3PlayerHost): String {
    val audios = playerHost.availableAudioStreams()
    if (audios.isEmpty()) return "音轨"
    val index = playerHost.currentAudioStreamIndex()
        ?: return audios.size.toString()
    val match = audios.firstOrNull { it.index() == index }
    val lang = match?.language().orEmpty().uppercase()
    return lang.ifBlank {
        (audios.indexOfFirst { it.index() == index } + 1).toString()
    }
}

/**
 * Quality label for the OSD text button. Returns the display height when
 * metadata is available (e.g. "4K", "1080p"), otherwise falls back to the
 * HDR format readout already computed for the debug info panel.
 */
internal fun qualityShortcutLabel(
    playable: tv.cinepilot.core.protocol.PlayableMedia?,
    hdrFallback: String,
): String {
    val video = playable?.mediaStreams()?.firstOrNull { it.type() == MediaStreamType.VIDEO }
    val height = video?.height()
    val base = when {
        height == null -> null
        height >= 2000 -> "4K"
        height >= 1080 -> "1080p"
        height >= 720 -> "720p"
        height >= 480 -> "480p"
        else -> "${height}p"
    } ?: hdrFallback
    val hdrShort = hdrFallback.split("（").first().trim()
    return if (hdrShort.isBlank() || hdrShort == "SDR" || hdrShort == base) base
    else "$base·$hdrShort"
}

/** Formats the current playback speed for the OSD speed text button. */
internal fun speedShortcutLabel(playerHost: Media3PlayerHost): String {
    val speed = playerHost.currentPlaybackSpeed()
    if (speed == 1f) return "1.0x"
    val raw = String.format("%.2f", speed).trimEnd('0').trimEnd('.')
    return "${raw}x"
}

/**
 * Cycle to the next speed preset, wrapping back to 1.0x after the fastest
 * preset. 1.0x is always a valid reset point even when the current speed
 * does not exactly match any preset.
 */
internal fun cycleNextPlaybackSpeed(playerHost: Media3PlayerHost) {
    val current = playerHost.currentPlaybackSpeed()
    val idx = PLAYBACK_SPEED_PRESETS.indexOfFirst { kotlin.math.abs(it - current) < 0.001f }
    val next = if (idx < 0 || idx >= PLAYBACK_SPEED_PRESETS.lastIndex) {
        val resetIdx = PLAYBACK_SPEED_PRESETS.indexOfFirst { kotlin.math.abs(it - 1.0f) < 0.001f }
            .coerceAtLeast(0)
        PLAYBACK_SPEED_PRESETS[resetIdx]
    } else {
        PLAYBACK_SPEED_PRESETS[idx + 1]
    }
    playerHost.setPlaybackSpeed(next)
}

/**
 * Format a single chapter button label: "HH:MM:SS  Chapter Name" (truncated).
 * Falls back to a plain timestamp when the chapter has no server-side name.
 */
internal fun chapterButtonLabel(startTicks: Long, rawName: String): String = buildString {
    append(MediaTicks.formatShort(startTicks))
    val name = rawName.trim()
    if (name.isNotBlank()) {
        append("  ")
        append(name)
    }
}

/**
 * Locate the chapter that contains [positionTicks], defined as the last
 * chapter whose start time is not greater than the playback position.
 * Returns 0 when the chapters list is empty or the position precedes every
 * chapter marker.
 */
internal fun chapterIndexForPosition(
    chapters: List<Pair<Long, String>>,
    positionTicks: Long,
): Int {
    if (chapters.isEmpty()) return 0
    var idx = 0
    while (idx < chapters.lastIndex && chapters[idx + 1].first <= positionTicks) idx++
    return idx
}

/** Short display name for the running HDR format, used in quality shortcuts. */
internal fun hdrFormatShortLabel(format: DisplayCapabilities.HdrFormat): String = when (format) {
    DisplayCapabilities.HdrFormat.DOLBY_VISION -> "DV"
    DisplayCapabilities.HdrFormat.HDR10 -> "HDR10"
    DisplayCapabilities.HdrFormat.HDR10_PLUS -> "HDR10+"
    DisplayCapabilities.HdrFormat.HLG -> "HLG"
    DisplayCapabilities.HdrFormat.UNKNOWN -> "SDR"
}

/**
 * Whether the given media item is eligible for the server-scoped genre-chip
 * path. Returns false for library-root overviews and search results because
 * `genresForCurrentView` does not have a well-defined server view for those
 * synthetic workflow routes.
 */
internal fun itemSupportsGenreRefresh(item: MediaItemSummary?): Boolean = item != null
