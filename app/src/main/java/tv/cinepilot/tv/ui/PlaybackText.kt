package tv.cinepilot.tv.ui

import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaSourceInfo
import tv.cinepilot.core.protocol.MediaStreamInfo
import tv.cinepilot.core.protocol.MediaTicks

data class PlaybackSpeedOption(
    val label: String,
    val rate: Float,
)

fun sourceLabel(source: MediaSourceInfo): String {
    val parts = mutableListOf<String>()
    if (source.name().isNotBlank()) {
        parts.add(source.name())
    } else if (source.path().isNotBlank()) {
        parts.add(source.path().substringAfterLast('/').substringAfterLast('\\'))
    } else if (source.container().isNotBlank()) {
        parts.add(source.container().uppercase())
    } else {
        parts.add(source.id())
    }
    if (source.container().isNotBlank()) {
        parts.add(source.container().uppercase())
    }
    if (source.bitRate() > 0) {
        parts.add("${source.bitRate() / 1_000_000} Mbps")
    }
    return "媒体源：${parts.distinct().joinToString(" · ")}"
}

fun streamLabel(stream: MediaStreamInfo): String {
    val parts = mutableListOf<String>()
    if (stream.displayTitle().isNotBlank()) {
        parts.add(stream.displayTitle())
    } else {
        if (stream.language().isNotBlank()) {
            parts.add(stream.language())
        }
        if (stream.codec().isNotBlank()) {
            parts.add(stream.codec())
        }
    }
    if (stream.defaultStream()) {
        parts.add("默认")
    }
    if (stream.forced()) {
        parts.add("强制")
    }
    if (stream.external()) {
        parts.add("外挂")
    }
    return parts.ifEmpty { listOf("未命名") }.joinToString(" · ")
}

fun episodeLabel(item: MediaItemSummary): String {
    val parts = mutableListOf<String>()
    if (item.seriesName().isNotBlank()) {
        parts.add(item.seriesName())
    }
    item.parentIndexNumber()?.let { season ->
        parts.add("第 $season 季")
    }
    item.indexNumber()?.let { episode ->
        parts.add("第 $episode 集")
    }
    return parts.joinToString(" · ")
}

fun playbackSpeedOptions(): List<PlaybackSpeedOption> {
    return listOf(
        PlaybackSpeedOption("0.75x", 0.75f),
        PlaybackSpeedOption("1.0x（正常）", 1.0f),
        PlaybackSpeedOption("1.25x", 1.25f),
        PlaybackSpeedOption("1.5x", 1.5f),
        PlaybackSpeedOption("2.0x", 2.0f),
    )
}

fun formatPlaybackPosition(ticks: Long): String {
    val totalSeconds = MediaTicks.toMilliseconds(ticks) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
