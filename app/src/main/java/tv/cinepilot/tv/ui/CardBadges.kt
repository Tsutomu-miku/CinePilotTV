package tv.cinepilot.tv.ui

import java.util.Locale
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaStreamInfo
import tv.cinepilot.core.protocol.MediaStreamType

/**
 * 提取卡片上显示的技术标签（如 4K、HDR10、杜比视界 等）。
 * 用于首页海报/横幅卡片的角标展示。
 */
fun MediaItemSummary.cardBadgeLabels(): List<String> {
    val streams = mediaStreams() ?: return emptyList()
    val video = streams.firstOrNull { it.type() == MediaStreamType.VIDEO } ?: return emptyList()
    val badges = mutableListOf<String>()

    // 分辨率
    resolutionShortLabel(video)?.let(badges::add)

    // HDR / 杜比视界
    videoRangeShortLabel(video)?.let(badges::add)

    return badges.distinct()
}

private fun resolutionShortLabel(stream: MediaStreamInfo): String? {
    val width = stream.width()
    val height = stream.height()
    if (width != null && height != null && width > 0 && height > 0) {
        return when {
            width >= 3840 || height >= 2160 -> "4K"
            height >= 1440 -> "1440p"
            height >= 1080 -> "1080p"
            height >= 720 -> "720p"
            else -> null
        }
    }
    val display = stream.displayTitle()
    return when {
        display.contains("2160", ignoreCase = true) || display.contains("4k", ignoreCase = true) -> "4K"
        display.contains("1080", ignoreCase = true) -> "1080p"
        display.contains("720", ignoreCase = true) -> "720p"
        else -> null
    }
}

private fun videoRangeShortLabel(stream: MediaStreamInfo): String? {
    val value = listOf(
        stream.videoRangeType(),
        stream.videoRange(),
        stream.profile(),
        stream.displayTitle(),
    ).joinToString(" ").lowercase(Locale.ROOT)
    return when {
        value.contains("dovi") || value.contains("dolby vision") -> "杜比视界"
        value.contains("hdr10+") || value.contains("hdr10 plus") -> "HDR10+"
        value.contains("hdr10") -> "HDR10"
        value.contains("hdr") && !value.contains("sdr") -> "HDR"
        else -> null
    }
}
