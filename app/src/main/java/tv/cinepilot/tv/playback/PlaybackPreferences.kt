package tv.cinepilot.tv.playback

import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.PlaybackSelectionPreferences

fun trackPreferences(
    item: MediaItemSummary,
    mediaSourceId: String,
    audioStreamIndex: Int?,
    subtitleStreamIndex: Int?,
): PlaybackSelectionPreferences {
    return PlaybackSelectionPreferences(resumeStartTicks(item), audioStreamIndex, subtitleStreamIndex, null, 0, 0, 0)
        .withMediaSourceId(mediaSourceId)
}

fun sourcePreferences(item: MediaItemSummary, mediaSourceId: String): PlaybackSelectionPreferences {
    return PlaybackSelectionPreferences(resumeStartTicks(item), null, null, null, 0, 0, 0).withMediaSourceId(mediaSourceId)
}

fun lowBitratePreferences(item: MediaItemSummary): PlaybackSelectionPreferences {
    return PlaybackSelectionPreferences.lowBitrate(resumeStartTicks(item))
}

fun speedPreferences(item: MediaItemSummary, rate: Float): PlaybackSelectionPreferences {
    return PlaybackSelectionPreferences(resumeStartTicks(item), null, null, null, 0, 0, 0).withPlaybackRate(rate)
}

private fun resumeStartTicks(item: MediaItemSummary): Long =
    if (item.hasResumePosition()) item.userData().playbackPositionTicks() else 0L
