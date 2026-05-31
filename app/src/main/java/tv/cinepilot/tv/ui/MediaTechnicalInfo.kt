package tv.cinepilot.tv.ui

import java.util.Locale
import tv.cinepilot.core.protocol.MediaSourceInfo
import tv.cinepilot.core.protocol.MediaStreamInfo
import tv.cinepilot.core.protocol.MediaStreamType
import tv.cinepilot.core.protocol.PlaybackInfo

fun mediaTechnicalPills(playbackInfo: PlaybackInfo?): List<String> {
    val source = playbackInfo?.mediaSources()?.firstOrNull() ?: return emptyList()
    val video = source.mediaStreams().firstOrNull { it.type() == MediaStreamType.VIDEO }
    val audio = source.mediaStreams().firstOrNull { it.type() == MediaStreamType.AUDIO }
    val subtitles = source.mediaStreams().filter { it.type() == MediaStreamType.SUBTITLE }
    val pills = mutableListOf<String>()
    video?.let { stream ->
        resolutionLabel(stream)?.let(pills::add)
        codecLabel(stream.codec()).takeIf { it.isNotBlank() }?.let(pills::add)
        videoRangeLabel(stream)?.let(pills::add)
    }
    audio?.let { stream ->
        audioLabel(stream)?.let(pills::add)
    }
    if (source.container().isNotBlank()) {
        pills.add(source.container().uppercase(Locale.ROOT))
    }
    if (source.sizeBytes() > 0) {
        pills.add(sizeLabel(source.sizeBytes()))
    }
    if (source.bitRate() > 0) {
        pills.add(bitRateLabel(source.bitRate()))
    }
    pills.add(subtitleSummary(subtitles))
    return pills.distinct()
}

private fun resolutionLabel(stream: MediaStreamInfo): String? {
    val width = stream.width()
    val height = stream.height()
    if (width != null && height != null && width > 0 && height > 0) {
        return "${width}x${height}"
    }
    return stream.displayTitle().takeIf {
        it.contains("p", ignoreCase = true) || it.contains("k", ignoreCase = true)
    }
}

private fun codecLabel(codec: String): String {
    return when (codec.lowercase(Locale.ROOT)) {
        "h264" -> "H.264"
        "hevc", "h265" -> "HEVC"
        "av1" -> "AV1"
        "mpeg4" -> "MPEG-4"
        else -> codec.uppercase(Locale.ROOT)
    }
}

private fun videoRangeLabel(stream: MediaStreamInfo): String? {
    val value = listOf(stream.videoRangeType(), stream.videoRange(), stream.profile(), stream.displayTitle())
        .joinToString(" ")
    return when {
        value.contains("dovi", ignoreCase = true) || value.contains("dolby vision", ignoreCase = true) -> "Dolby Vision"
        value.contains("hdr10", ignoreCase = true) -> "HDR10"
        value.contains("hdr", ignoreCase = true) -> "HDR"
        else -> null
    }
}

private fun audioLabel(stream: MediaStreamInfo): String? {
    val codec = codecLabel(stream.codec()).takeIf { it.isNotBlank() }
    val channels = stream.channels()?.let(::channelsLabel)
    val atmos = if (
        stream.displayTitle().contains("atmos", ignoreCase = true) ||
        stream.profile().contains("atmos", ignoreCase = true)
    ) {
        "Dolby Atmos"
    } else {
        null
    }
    return listOfNotNull(codec, channels, atmos).takeIf { it.isNotEmpty() }?.joinToString(" / ")
}

private fun channelsLabel(channels: Int): String {
    return when (channels) {
        1 -> "单声道"
        2 -> "立体声"
        6 -> "5.1 声道"
        8 -> "7.1 声道"
        else -> "${channels} 声道"
    }
}

private fun sizeLabel(bytes: Long): String {
    val gib = bytes.toDouble() / 1_073_741_824.0
    return if (gib >= 1.0) {
        "%.1f GB".format(Locale.US, gib)
    } else {
        "%.0f MB".format(Locale.US, bytes.toDouble() / 1_048_576.0)
    }
}

private fun bitRateLabel(bitRate: Long): String {
    return if (bitRate >= 1_000_000L) {
        "%.1f Mbps".format(Locale.US, bitRate.toDouble() / 1_000_000.0)
    } else {
        "${bitRate / 1_000} Kbps"
    }
}

private fun subtitleSummary(subtitles: List<MediaStreamInfo>): String {
    if (subtitles.isEmpty()) {
        return "无字幕"
    }
    val externalCount = subtitles.count { it.external() }
    return if (externalCount > 0) {
        "字幕 ${subtitles.size} 条 / 外挂 ${externalCount} 条"
    } else {
        "字幕 ${subtitles.size} 条"
    }
}
