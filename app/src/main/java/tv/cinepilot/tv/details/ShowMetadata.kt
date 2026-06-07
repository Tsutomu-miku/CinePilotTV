package tv.cinepilot.tv.details

import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaPerson
import tv.cinepilot.core.protocol.MediaStreamInfo
import tv.cinepilot.core.protocol.MediaStreamType
import tv.cinepilot.tv.ui.durationText
import java.util.Locale

internal fun showHeroTitle(season: MediaItemSummary, episode: MediaItemSummary?): String {
    if (episode == null) {
        return season.name().ifBlank { "季详情" }
    }
    val prefix = listOfNotNull(
        episode.parentIndexNumber()?.let { "第 $it 季" },
        episode.indexNumber()?.let { "第 $it 集" },
    ).joinToString(" · ")
    val name = episode.name().ifBlank { "未命名剧集" }
    return if (prefix.isBlank()) name else "$prefix - $name"
}

internal fun showMetaLine(item: MediaItemSummary): String {
    val values = mutableListOf<String>()
    item.communityRating()?.let { values.add("★ ${"%.1f".format(Locale.US, it)}") }
    formattedPremiereDate(item.premiereDate()).takeIf { it.isNotBlank() }?.let(values::add)
    item.runTimeTicks()?.let { values.add(durationText(it)) }
    item.officialRating().takeIf { it.isNotBlank() }?.let(values::add)
    item.productionYear()?.let { values.add(it.toString()) }
    values.addAll(item.genres().take(2))
    return values.distinct().joinToString(" · ")
}

internal fun showTechnicalBadges(item: MediaItemSummary): List<String> {
    val video = item.mediaStreams().firstOrNull { it.type() == MediaStreamType.VIDEO }
    val audio = item.mediaStreams().firstOrNull { it.type() == MediaStreamType.AUDIO }
    val subtitles = item.mediaStreams().count { it.type() == MediaStreamType.SUBTITLE }
    val values = mutableListOf<String>()
    video?.resolutionLabel()?.let(values::add)
    video?.codecLabel()?.let(values::add)
    video?.rangeLabel()?.let(values::add)
    audio?.audioLabel()?.let(values::add)
    if (subtitles > 0) {
        values.add("$subtitles 字幕")
    } else {
        values.add("无字幕")
    }
    return values.distinct().take(7)
}

internal fun peopleSummary(item: MediaItemSummary): List<MediaPerson> {
    return item.people()
        .filter { it.name().isNotBlank() }
        .take(14)
}

private fun MediaStreamInfo.resolutionLabel(): String? {
    val height = height() ?: return null
    return when {
        height >= 2160 -> "4K"
        height >= 1440 -> "1440p"
        height >= 1080 -> "1080p"
        height >= 720 -> "720p"
        else -> "${height}p"
    }
}

private fun MediaStreamInfo.codecLabel(): String? {
    return codec().takeIf { it.isNotBlank() }?.uppercase(Locale.US)
}

private fun MediaStreamInfo.rangeLabel(): String? {
    return listOf(videoRange(), videoRangeType())
        .firstOrNull { it.isNotBlank() }
        ?.uppercase(Locale.US)
}

private fun MediaStreamInfo.audioLabel(): String? {
    val parts = mutableListOf<String>()
    codec().takeIf { it.isNotBlank() }?.uppercase(Locale.US)?.let(parts::add)
    channels()?.let { parts.add(if (it >= 6) "5.1" else "${it}ch") }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" ")
}

private fun formattedPremiereDate(value: String): String {
    val date = value.take(10)
    if (date.length != 10) {
        return ""
    }
    val parts = date.split("-")
    if (parts.size != 3) {
        return date
    }
    return "${parts[0]}年${parts[1].toIntOrNull() ?: parts[1]}月${parts[2].toIntOrNull() ?: parts[2]}日"
}
