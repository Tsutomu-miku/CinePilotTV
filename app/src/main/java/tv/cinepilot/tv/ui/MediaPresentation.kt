package tv.cinepilot.tv.ui

import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaItemType
import tv.cinepilot.core.protocol.MediaTicks

data class ArtworkSet(
    val primaryTag: String,
    val thumbTag: String,
    val backdropTags: List<String>,
    val fallbackStrategy: ArtworkFallbackStrategy,
) {
    val hasBackdrop: Boolean = backdropTags.isNotEmpty() || thumbTag.isNotBlank() || primaryTag.isNotBlank()
}

enum class ArtworkFallbackStrategy {
    BACKDROP_THUMB_PRIMARY,
    PRIMARY_ONLY,
    NONE,
}

data class MediaMetadataLine(
    val values: List<String>,
) {
    fun text(): String = values.filter { it.isNotBlank() }.joinToString(" · ")
}

data class MediaPresentation(
    val title: String,
    val subtitle: String,
    val contextLine: String,
    val metadata: MediaMetadataLine,
    val microMetadata: List<String>,
    val watchState: String,
    val progressLabel: String,
    val primaryArtwork: ArtworkSet,
    val backdropArtwork: ArtworkSet,
    val deliveryBadges: List<String>,
    val technicalTags: List<String>,
)

enum class InfuseActionEmphasis {
    PRIMARY,
    SECONDARY,
    QUIET,
}

data class InfuseAction(
    val label: String,
    val icon: TvIcon,
    val emphasis: InfuseActionEmphasis,
    val onClick: () -> Unit,
)

fun MediaItemSummary.toMediaPresentation(
    subtitle: String = episodeLabel(this),
    progressLabel: String = resumeProgressLabel(),
    technicalTags: List<String> = emptyList(),
): MediaPresentation {
    val artwork = artworkSet()
    return MediaPresentation(
        title = name().ifBlank { id() },
        subtitle = subtitle,
        contextLine = contextLine(this, subtitle),
        metadata = MediaMetadataLine(mediaMetadataValues(this, subtitle)),
        microMetadata = mediaMetadataValues(this, subtitle).take(5),
        watchState = progressLabel,
        progressLabel = progressLabel,
        primaryArtwork = artwork.copy(fallbackStrategy = ArtworkFallbackStrategy.PRIMARY_ONLY),
        backdropArtwork = artwork,
        deliveryBadges = technicalTags.take(4),
        technicalTags = technicalTags,
    )
}

private fun MediaItemSummary.artworkSet(): ArtworkSet {
    val primary = imageTags()["Primary"].orEmpty()
    val thumb = imageTags()["Thumb"].orEmpty()
    val fallback = when {
        backdropImageTags().isNotEmpty() || thumb.isNotBlank() -> ArtworkFallbackStrategy.BACKDROP_THUMB_PRIMARY
        primary.isNotBlank() -> ArtworkFallbackStrategy.PRIMARY_ONLY
        else -> ArtworkFallbackStrategy.NONE
    }
    return ArtworkSet(primary, thumb, backdropImageTags(), fallback)
}

private fun MediaItemSummary.resumeProgressLabel(): String {
    return if (hasResumePosition()) {
        "可从 ${formatPlaybackPosition(userData().playbackPositionTicks())} 继续播放"
    } else {
        ""
    }
}

private fun mediaMetadataValues(item: MediaItemSummary, subtitle: String): List<String> {
    val values = mutableListOf<String>()
    presentationMediaTypeLabel(item.type()).takeIf { it.isNotBlank() }?.let(values::add)
    if (subtitle.isNotBlank()) {
        values.add(subtitle.replace(" · ", " / "))
    }
    if (item.type() != MediaItemType.EPISODE) {
        item.productionYear()?.let { values.add(it.toString()) }
    }
    item.runTimeTicks()?.let { values.add(durationText(it)) }
    values.addAll(item.genres().take(3))
    return values
}

private fun contextLine(item: MediaItemSummary, subtitle: String): String {
    return mediaMetadataValues(item, subtitle).take(3).joinToString(" · ")
}

fun presentationMediaTypeLabel(type: MediaItemType): String = when (type) {
    MediaItemType.MOVIE -> "电影"
    MediaItemType.SERIES -> "剧集"
    MediaItemType.SEASON -> "季"
    MediaItemType.EPISODE -> "单集"
    MediaItemType.VIDEO -> "视频"
    MediaItemType.PLAYLIST -> "播放列表"
    MediaItemType.COLLECTION_FOLDER,
    MediaItemType.FOLDER -> "目录"
    MediaItemType.UNKNOWN -> ""
}

fun durationText(ticks: Long): String {
    val totalMinutes = (MediaTicks.toMilliseconds(ticks) / 60_000).coerceAtLeast(1)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours} 小时 ${minutes} 分钟"
        hours > 0 -> "${hours} 小时"
        else -> "${minutes} 分钟"
    }
}

// ------ UI/protocol hygiene helpers -----------------------------------------
//
// The hygiene rule in scripts/check.sh forbids importing MediaItemType from
// any UI/home/details/playback/auth/error/settings/player source file EXCEPT
// the three presentation-adapter files (this one, DetailPresentation.kt,
// HomeRowPresentation.kt). Anywhere else that needs a type() comparison must
// call one of these helpers. That keeps the protocol boundary explicit: if
// we ever port CinePilot TV to another backend, only these adapters must
// be rewritten.

fun MediaItemSummary.isEpisode(): Boolean = type() == MediaItemType.EPISODE
fun MediaItemSummary.isSeries(): Boolean = type() == MediaItemType.SERIES
fun MediaItemSummary.isSeason(): Boolean = type() == MediaItemType.SEASON
fun MediaItemSummary.isPlaylist(): Boolean = type() == MediaItemType.PLAYLIST
fun MediaItemSummary.isFolderBrowse(): Boolean = type() == MediaItemType.FOLDER ||
    type() == MediaItemType.COLLECTION_FOLDER
fun MediaItemSummary.isSeriesStructureRoot(): Boolean = isSeries() || isSeason()

/**
 * Returns the Chinese label for the "browse children" action shown on the
 * detail page of a series-root item (SERIES → "查看季集", SEASON → "选集").
 * Empty string for non-series-root items.
 */
fun MediaItemSummary.browseChildrenLabel(): String = when (type()) {
    MediaItemType.SERIES -> "查看季集"
    MediaItemType.SEASON -> "选集"
    MediaItemType.PLAYLIST -> "查看列表"
    else -> ""
}
