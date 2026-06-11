package tv.cinepilot.tv.ui

import java.util.Locale
import tv.cinepilot.core.protocol.MediaStreamInfo
import tv.cinepilot.core.protocol.MediaStreamType
import tv.cinepilot.core.protocol.PlaybackInfo

fun mediaTechnicalPills(
    playbackInfo: PlaybackInfo?,
    supportedHdrTypes: Set<String> = emptySet(),
    supportedPassthroughCodecs: Set<String> = emptySet(),
): List<String> {
    val source = playbackInfo?.mediaSources()?.firstOrNull() ?: return emptyList()
    val video = source.mediaStreams().firstOrNull { it.type() == MediaStreamType.VIDEO }
    val audioStreams = source.mediaStreams().filter { it.type() == MediaStreamType.AUDIO }
    val primaryAudio = audioStreams.firstOrNull { it.defaultStream() } ?: audioStreams.firstOrNull()
    val subtitles = source.mediaStreams().filter { it.type() == MediaStreamType.SUBTITLE }
    val pills = mutableListOf<String>()
    video?.let { stream ->
        resolutionLabel(stream)?.let(pills::add)
        codecLabel(stream.codec()).takeIf { it.isNotBlank() }?.let(pills::add)
        bitDepthLabel(stream)?.let(pills::add)
        videoRangeLabel(stream)?.let(pills::add)
        videoCompatibilityLabel(stream, supportedHdrTypes)?.let(pills::add)
    }
    primaryAudio?.let { stream ->
        audioLabel(stream)?.let(pills::add)
        audioCompatibilityLabel(stream, supportedPassthroughCodecs)?.let(pills::add)
    }
    if (audioStreams.size > 1) {
        pills.add("音轨 ${audioStreams.size} 条")
    }
    primaryAudio?.let(::defaultAudioLabel)?.let(pills::add)
    if (source.container().isNotBlank()) {
        pills.add(source.container().uppercase(Locale.ROOT))
    }
    if (source.sizeBytes() > 0) {
        pills.add(sizeLabel(source.sizeBytes()))
    }
    if (source.bitRate() > 0) {
        pills.add(bitRateLabel(source.bitRate()))
        highBitrateLabel(source.bitRate())?.let(pills::add)
    }
    pills.addAll(subtitleLabels(subtitles))
    // Device capability info (P3-4)
    if (supportedHdrTypes.isNotEmpty()) {
        pills.add("设备支持 HDR：${supportedHdrTypes.joinToString(" / ")}")
    }
    if (supportedPassthroughCodecs.isNotEmpty()) {
        pills.add("音频直通：${supportedPassthroughCodecs.joinToString(" / ")}")
    }
    return pills.distinct()
}

private fun resolutionLabel(stream: MediaStreamInfo): String? {
    val width = stream.width()
    val height = stream.height()
    if (width != null && height != null && width > 0 && height > 0) {
        val shortLabel = when {
            width >= 3840 || height >= 2160 -> "4K UHD"
            height >= 1440 -> "1440p"
            height >= 1080 -> "1080p"
            height >= 720 -> "720p"
            else -> null
        }
        return if (shortLabel == null) "${width}x${height}" else "$shortLabel (${width}x${height})"
    }
    return stream.displayTitle().takeIf {
        it.contains("p", ignoreCase = true) || it.contains("k", ignoreCase = true)
    }
}

private fun codecLabel(codec: String): String {
    return when (codec.lowercase(Locale.ROOT)) {
        "h264", "avc" -> "H.264"
        "hevc", "h265" -> "HEVC"
        "av1" -> "AV1"
        "mpeg4" -> "MPEG-4"
        "aac" -> "AAC"
        "mp3" -> "MP3"
        "ac3" -> "Dolby Digital"
        "eac3", "eac-3" -> "Dolby Digital+"
        "truehd" -> "Dolby TrueHD"
        "dts", "dca" -> "DTS"
        "dtshd", "dts-hd" -> "DTS-HD"
        "flac" -> "FLAC"
        "srt", "subrip" -> "SRT"
        "ass", "ssa" -> "ASS"
        "pgs", "pgssub", "hdmv_pgs_subtitle" -> "PGS"
        "dvdsub", "dvd_subtitle" -> "DVD 字幕"
        "vobsub" -> "VobSub"
        else -> codec.uppercase(Locale.ROOT)
    }
}

private fun bitDepthLabel(stream: MediaStreamInfo): String? {
    val bitDepth = stream.bitDepth()
    return if (bitDepth != null && bitDepth >= 10) "${bitDepth}-bit" else null
}

private fun videoRangeLabel(stream: MediaStreamInfo): String? {
    val value = listOf(stream.videoRangeType(), stream.videoRange(), stream.profile(), stream.displayTitle())
        .joinToString(" ")
    return when {
        value.contains("dovi", ignoreCase = true) || value.contains("dolby vision", ignoreCase = true) -> "杜比视界 Dolby Vision"
        value.contains("hdr10", ignoreCase = true) -> "HDR10"
        value.contains("hdr", ignoreCase = true) -> "HDR"
        else -> null
    }
}

private fun videoCompatibilityLabel(stream: MediaStreamInfo, supportedHdrTypes: Set<String>): String? {
    val value = stream.searchableTechnicalText()
    val isDolbyVision = value.contains("dovi") || value.contains("dolby vision")
    val isAv1 = value.contains("av1")
    val isHevc = value.contains("hevc") || value.contains("h265") || value.contains("h.265")

    return when {
        isDolbyVision -> {
            val hdrSet = supportedHdrTypes.joinToString(" / ").ifBlank { null }
            if (hdrSet != null && (supportedHdrTypes.contains("Dolby Vision") || supportedHdrTypes.any { it.contains("dolby", ignoreCase = true) })) {
                "杜比视界 · 设备支持"
            } else if (supportedHdrTypes.isNotEmpty()) {
                "杜比视界 · 设备不支持（需转码）"
            } else {
                "杜比视界兼容性依赖设备或转码"
            }
        }
        isAv1 -> "AV1 兼容性依赖设备或转码"
        isHevc -> "HEVC 兼容性依赖设备或转码"
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
        "杜比全景声 Dolby Atmos"
    } else {
        null
    }
    return listOfNotNull(codec, channels, atmos).takeIf { it.isNotEmpty() }?.joinToString(" / ")
}

private fun audioCompatibilityLabel(stream: MediaStreamInfo, supportedPassthrough: Set<String>): String? {
    val value = stream.searchableTechnicalText()
    val isTrueHd = value.contains("truehd")
    val isDtsHd = value.contains("dts-hd") || value.contains("dtshd")
    val isAtmos = value.contains("atmos")
    val isHighDef = isTrueHd || isDtsHd || isAtmos

    if (!isHighDef) return null

    return when {
        supportedPassthrough.isEmpty() -> "高清音频可能触发转码"
        isTrueHd && supportedPassthrough.any { it.contains("TrueHD", ignoreCase = true) } ->
            "TrueHD · 支持直通"
        isDtsHd && supportedPassthrough.any { it.contains("DTS-HD", ignoreCase = true) } ->
            "DTS-HD · 支持直通"
        isAtmos && (supportedPassthrough.any { it.contains("TrueHD", ignoreCase = true) } ||
                supportedPassthrough.any { it.contains("DTS-HD", ignoreCase = true) }) ->
            "全景声 · 支持直通"
        else -> "高清音频 · 设备不支持直通（需转码）"
    }
}

private fun defaultAudioLabel(stream: MediaStreamInfo): String? {
    if (!stream.defaultStream()) {
        return null
    }
    return stream.language().takeIf { it.isNotBlank() }?.let { "默认音轨：${languageLabel(it)}" }
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

private fun highBitrateLabel(bitRate: Long): String? {
    return if (bitRate >= 50_000_000L) {
        "高码率，弱网建议低码率播放"
    } else {
        null
    }
}

private fun subtitleLabels(subtitles: List<MediaStreamInfo>): List<String> {
    if (subtitles.isEmpty()) {
        return listOf("无字幕")
    }
    val languages = subtitles.mapNotNull { stream ->
        stream.language().takeIf { it.isNotBlank() }?.let(::languageLabel)
    }.distinct().take(3)
    val labels = mutableListOf(
        if (languages.isEmpty()) {
            "字幕 ${subtitles.size} 条"
        } else {
            "字幕 ${subtitles.size} 条：${languages.joinToString(" / ")}"
        },
    )
    val formatParts = mutableListOf<String>()
    val subtitleFormats = subtitles.mapNotNull { stream ->
        codecLabel(stream.codec()).takeIf { it.isNotBlank() }
    }.distinct().take(4)
    if (subtitleFormats.isNotEmpty()) {
        formatParts.add("字幕格式 ${subtitleFormats.joinToString(" / ")}")
    }
    val externalCount = subtitles.count { it.external() }
    if (externalCount > 0) {
        formatParts.add("外挂字幕 $externalCount 条")
    }
    if (formatParts.isNotEmpty()) {
        labels.add(formatParts.joinToString(" / "))
    }
    subtitleHintLabel(subtitles)?.let(labels::add)
    return labels
}

private fun subtitleHintLabel(subtitles: List<MediaStreamInfo>): String? {
    val hints = mutableListOf<String>()
    subtitles.firstOrNull { it.defaultStream() }?.language()?.takeIf { it.isNotBlank() }?.let { language ->
        hints.add("默认字幕 ${languageLabel(language)}")
    }
    if (subtitles.any { it.forced() }) {
        hints.add("含强制字幕")
    }
    if (subtitles.any(::isImageSubtitle)) {
        hints.add("图形字幕可能触发转码")
    }
    return hints.takeIf { it.isNotEmpty() }?.joinToString(" / ")
}

private fun isImageSubtitle(stream: MediaStreamInfo): Boolean {
    val value = stream.searchableTechnicalText()
    return value.contains("pgs") || value.contains("dvdsub") ||
        value.contains("dvd_subtitle") || value.contains("vobsub")
}

private fun MediaStreamInfo.searchableTechnicalText(): String {
    return listOf(codec(), profile(), videoRange(), videoRangeType(), displayTitle())
        .joinToString(" ")
        .lowercase(Locale.ROOT)
}

private fun languageLabel(language: String): String {
    return when (language.lowercase(Locale.ROOT)) {
        "zh", "zho", "chi", "chs", "cht", "cn", "cmn" -> "中文"
        "en", "eng" -> "English"
        "ja", "jpn" -> "日语"
        "ko", "kor" -> "韩语"
        "fr", "fre", "fra" -> "法语"
        "de", "ger", "deu" -> "德语"
        "es", "spa" -> "西班牙语"
        "pt", "por" -> "葡萄牙语"
        "ru", "rus" -> "俄语"
        else -> language.uppercase(Locale.ROOT)
    }
}
