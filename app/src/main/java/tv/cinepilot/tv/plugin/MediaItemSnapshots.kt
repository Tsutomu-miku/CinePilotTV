package tv.cinepilot.tv.plugin

import tv.cinepilot.core.protocol.AuthenticatedServer
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.plugin.spi.MediaItemSnapshot

/**
 * Convert a :core [MediaItemSummary] to the SPI-only [MediaItemSnapshot]
 * so plugins never depend on internal :core types. All conversions here are
 * lossy by design -- the snapshot only keeps the stable subset of fields
 * plugins are contractually allowed to depend on.
 */
internal fun MediaItemSummary.toSnapshot(authenticated: AuthenticatedServer): MediaItemSnapshot {
    return MediaItemSnapshot(
        this.id(),
        authenticated.server().serverId(),
        this.name(),
        this.toSnapshotKind(),
        this.productionYear() ?: 0,
        this.indexNumber() ?: 0,
        this.parentIndexNumber() ?: 0,
        this.parentId() ?: "",
        this.seriesId() ?: "",
        ticksToMs(this.runTimeTicks() ?: 0L),
        this.providerIds(),
    )
}

private fun MediaItemSummary.toSnapshotKind(): MediaItemSnapshot.Kind {
    return when (this.typeWireName()) {
        "movie" -> MediaItemSnapshot.Kind.MOVIE
        "series" -> MediaItemSnapshot.Kind.SERIES
        "season" -> MediaItemSnapshot.Kind.SEASON
        "episode" -> MediaItemSnapshot.Kind.EPISODE
        "video" -> MediaItemSnapshot.Kind.VIDEO
        else -> MediaItemSnapshot.Kind.OTHER
    }
}

/** Jellyfin uses 100-nanosecond ticks (same as .NET). Convert to milliseconds. */
private fun ticksToMs(ticks: Long): Long = if (ticks <= 0L) 0L else ticks / 10_000L
