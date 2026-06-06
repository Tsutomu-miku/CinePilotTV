package tv.cinepilot.tv.playback

import tv.cinepilot.core.protocol.MediaStreamInfo
import tv.cinepilot.core.protocol.MediaStreamType
import tv.cinepilot.core.protocol.PlayMethod
import tv.cinepilot.core.protocol.PlayableMedia
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.tv.ui.formatPlaybackPosition
import tv.cinepilot.tv.ui.streamLabel

fun playbackDebugInfo(state: TvAppState): String {
    val playable = state.playableMedia() ?: return "暂无播放信息"
    val lines = mutableListOf<String>()
    lines.add("播放方式：${playMethodLabel(playable.playMethod())}")
    lines.add("媒体源：${playable.mediaSourceId()}")
    lines.add("播放会话：${playable.playSessionId()}")
    lines.add("URL 类型：${urlSourceLabel(playable)}")
    playable.request()?.let { request ->
        lines.add("请求路径：${request.path()}")
        lines.add("请求参数：${request.query().keys.joinToString(", ").ifBlank { "无" }}")
    } ?: playable.url()?.let { url ->
        lines.add("请求路径：${urlPath(url)}")
        lines.add("请求参数：${urlQueryKeys(url).joinToString(", ").ifBlank { "无" }}")
    }
    lines.add("起播位置：${formatPlaybackPosition(playable.startTimeTicks())}")
    lines.add("播放速度：${playable.playbackRate()?.let { "${it}x" } ?: "1.0x"}")
    lines.add("视频：${selectedVideoLabel(playable)}")
    lines.add("音轨：${streamByIndex(playable, MediaStreamType.AUDIO, playable.audioStreamIndex())}")
    lines.add("字幕：${subtitleLabel(playable)}")
    lines.add("字幕交付：${subtitleDeliveryLabel(playable)}")
    return lines.joinToString("\n")
}

private fun playMethodLabel(method: PlayMethod): String {
    return when (method) {
        PlayMethod.DIRECT_PLAY -> "直连 Direct Play"
        PlayMethod.DIRECT_STREAM -> "直传 Direct Stream"
        PlayMethod.TRANSCODE -> "转码 / HLS Transcode"
    }
}

private fun urlSourceLabel(playable: PlayableMedia): String {
    return if (playable.hasReadyUrl()) "服务器已返回播放 URL" else "由本机协议请求生成"
}

private fun urlPath(url: String): String {
    return url.substringBefore('?').substringAfter("://").substringAfter('/')
}

private fun urlQueryKeys(url: String): List<String> {
    val query = url.substringAfter('?', "")
    if (query.isBlank()) {
        return emptyList()
    }
    return query.substringBefore('#')
        .split('&')
        .mapNotNull { pair -> pair.substringBefore('=', "").takeIf { it.isNotBlank() } }
}

private fun selectedVideoLabel(playable: PlayableMedia): String {
    val video = playable.mediaStreams().firstOrNull { it.type() == MediaStreamType.VIDEO }
        ?: return "服务器未返回视频流信息"
    val parts = mutableListOf<String>()
    if (video.codec().isNotBlank()) {
        parts.add(video.codec().uppercase())
    }
    if (video.width() != null && video.height() != null) {
        parts.add("${video.width()}x${video.height()}")
    }
    if (video.videoRangeType().isNotBlank()) {
        parts.add(video.videoRangeType())
    }
    if (video.bitRate() > 0) {
        parts.add("${video.bitRate() / 1_000_000} Mbps")
    }
    return parts.ifEmpty { listOf(streamLabel(video)) }.joinToString(" · ")
}

private fun streamByIndex(playable: PlayableMedia, type: MediaStreamType, index: Int?): String {
    if (index == null) {
        return "服务器默认"
    }
    val stream = playable.mediaStreams().firstOrNull { it.type() == type && it.index() == index }
    return if (stream == null) "Stream #$index" else "Stream #$index · ${streamLabel(stream)}"
}

private fun subtitleLabel(playable: PlayableMedia): String {
    val index = playable.subtitleStreamIndex()
    if (index == null) {
        return "服务器默认 / 未显式选择"
    }
    if (index < 0) {
        return "关闭"
    }
    return streamByIndex(playable, MediaStreamType.SUBTITLE, index)
}

private fun subtitleDeliveryLabel(playable: PlayableMedia): String {
    val index = playable.subtitleStreamIndex()
    if (index == null) {
        return "由服务器默认决定"
    }
    if (index < 0) {
        return "关闭"
    }
    if (playable.subtitleDeliveryUrl().isNotBlank()) {
        return "Media3 外挂字幕 URL"
    }
    if (playable.subtitleDeliveryMethod().isNotBlank()) {
        return "服务器 ${playable.subtitleDeliveryMethod()} 交付"
    }
    return "服务器 HLS / 转码交付"
}
