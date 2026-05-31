package tv.cinepilot.tv.ui

import java.util.Locale
import tv.cinepilot.core.protocol.MediaStreamInfo
import tv.cinepilot.core.protocol.MediaStreamType
import tv.cinepilot.core.protocol.PlaybackInfo

fun mediaTechnicalPills(playbackInfo: PlaybackInfo?): List<String> {
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
    }
    primaryAudio?.let { stream ->
        audioLabel(stream)?.let(pills::add)
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
    }
    pills.addAll(subtitleLabels(subtitles))
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

private fun subtitleLabels(subtitles: List<MediaStreamInfo>): List<String> {
    if (subtitles.isEmpty()) {
        return listOf("无字幕")
    }
    val labels = mutableListOf("字幕 ${subtitles.size} 条")
    val languages = subtitles.mapNotNull { stream ->
        stream.language().takeIf { it.isNotBlank() }?.let(::languageLabel)
    }.distinct().take(3)
    if (languages.isNotEmpty()) {
        labels.add("字幕语言：${languages.joinToString(" / ")}")
    }
    val externalCount = subtitles.count { it.external() }
    if (externalCount > 0) {
        labels.add("外挂字幕 $externalCount 条")
    }
    if (subtitles.any { it.forced() }) {
        labels.add("含强制字幕")
    }
    subtitles.firstOrNull { it.defaultStream() }?.language()?.takeIf { it.isNotBlank() }?.let { language ->
        labels.add("默认字幕：${languageLabel(language)}")
    }
    return labels
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
