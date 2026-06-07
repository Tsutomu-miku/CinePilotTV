package tv.cinepilot.tv.ui

import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaItemType

data class DetailPresentation(
    val title: String,
    val contextLine: String,
    val primaryMeta: String,
    val qualityBadges: List<String>,
    val trackSummaries: List<String>,
    val overview: String,
    val nextUpAction: InfuseAction?,
)

fun MediaItemSummary.toDetailPresentation(
    technicalTags: List<String>,
    nextUpAction: InfuseAction? = null,
): DetailPresentation {
    val episode = episodeLabel(this)
    val context = detailContextValues(this, episode).joinToString(" · ")
    return DetailPresentation(
        title = name().ifBlank { id() },
        contextLine = context,
        primaryMeta = detailPrimaryMeta(this, episode),
        qualityBadges = detailQualityBadges(technicalTags),
        trackSummaries = detailTrackSummaries(technicalTags),
        overview = overview(),
        nextUpAction = nextUpAction,
    )
}

private fun detailContextValues(item: MediaItemSummary, episode: String): List<String> {
    val values = mutableListOf<String>()
    presentationMediaTypeLabel(item.type()).takeIf { it.isNotBlank() }?.let(values::add)
    if (item.type() == MediaItemType.EPISODE && episode.isNotBlank()) {
        values.add(episode.replace(" · ", " / "))
    } else {
        item.productionYear()?.let { values.add(it.toString()) }
    }
    item.runTimeTicks()?.let { values.add(durationText(it)) }
    values.addAll(item.genres().take(2))
    return values.distinct()
}

private fun detailPrimaryMeta(item: MediaItemSummary, episode: String): String {
    if (episode.isNotBlank()) {
        return episode.replace(" · ", " / ")
    }
    val values = mutableListOf<String>()
    item.productionYear()?.let { values.add(it.toString()) }
    values.addAll(item.genres().take(3))
    return values.joinToString(" · ")
}

private fun detailQualityBadges(tags: List<String>): List<String> {
    return tags
        .filterNot { it.contains("兼容性") || it.contains("触发转码") }
        .filterNot { it.startsWith("默认音轨") || it.startsWith("音轨 ") }
        .take(8)
}

private fun detailTrackSummaries(tags: List<String>): List<String> {
    return tags.filter { tag ->
        tag.startsWith("默认音轨") || tag.startsWith("音轨 ") || tag.contains("字幕")
    }.take(3)
}
