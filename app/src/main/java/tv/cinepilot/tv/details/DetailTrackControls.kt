package tv.cinepilot.tv.details

import android.view.View
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaSourceInfo
import tv.cinepilot.core.protocol.MediaStreamInfo
import tv.cinepilot.core.protocol.MediaStreamType
import tv.cinepilot.core.protocol.PlaybackInfo
import tv.cinepilot.tv.ui.TvOptionSelectItem
import tv.cinepilot.tv.ui.optionSelect
import tv.cinepilot.tv.ui.section
import tv.cinepilot.tv.ui.sourceLabel
import tv.cinepilot.tv.ui.streamLabel

data class DetailTrackSelection(
    val mediaSourceId: String? = null,
    val audioSelected: Boolean = false,
    val audioStreamIndex: Int? = null,
    val subtitleSelected: Boolean = false,
    val subtitleStreamIndex: Int? = null,
    val burnSubtitleWhenTranscoding: Boolean = false,
    val focusKey: String? = null,
) {
    fun hasExplicitChoice(): Boolean {
        return mediaSourceId != null || audioSelected || subtitleSelected
    }

    fun normalizedFor(playbackInfo: PlaybackInfo?): DetailTrackSelection {
        val sources = playbackInfo?.mediaSources().orEmpty()
        if (!hasExplicitChoice() || sources.isEmpty()) {
            return this
        }
        val selectedSource = sources.firstOrNull { it.id() == mediaSourceId } ?: return DetailTrackSelection()
        val audioValid = !audioSelected || selectedSource.hasStream(MediaStreamType.AUDIO, audioStreamIndex)
        val subtitleValid = !subtitleSelected ||
            subtitleStreamIndex == SUBTITLES_OFF_INDEX ||
            selectedSource.hasStream(MediaStreamType.SUBTITLE, subtitleStreamIndex)
        return copy(
            audioSelected = audioSelected && audioValid,
            audioStreamIndex = if (audioSelected && audioValid) audioStreamIndex else null,
            subtitleSelected = subtitleSelected && subtitleValid,
            subtitleStreamIndex = if (subtitleSelected && subtitleValid) subtitleStreamIndex else null,
            burnSubtitleWhenTranscoding = subtitleSelected && subtitleValid && burnSubtitleWhenTranscoding,
        )
    }
}

fun ComponentActivity.detailTrackControls(
    playbackInfo: PlaybackInfo?,
    selection: DetailTrackSelection,
    onSelection: (DetailTrackSelection) -> Unit,
): View? {
    val sources = playbackInfo?.mediaSources().orEmpty()
    if (sources.isEmpty()) {
        return null
    }
    val activeSource = sources.firstOrNull { it.id() == selection.mediaSourceId } ?: sources.first()
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(section("播放设置"))
        if (sources.size > 1) {
            addView(optionSelect(
                title = "媒体源",
                selectedLabel = sourceValue(activeSource),
                options = sources.map { source ->
                    TvOptionSelectItem(sourceValue(source), source.id() == activeSource.id()) {
                        onSelection(DetailTrackSelection(mediaSourceId = source.id(), focusKey = FOCUS_SOURCE))
                    }
                },
                requestFocus = selection.focusKey == FOCUS_SOURCE,
            ))
        }
        addView(audioOptionSelect(activeSource, selection, onSelection))
        addView(subtitleOptionSelect(activeSource, selection, onSelection))
    }
}

private fun ComponentActivity.audioOptionSelect(
    source: MediaSourceInfo,
    selection: DetailTrackSelection,
    onSelection: (DetailTrackSelection) -> Unit,
): View {
    val streams = source.streamsOf(MediaStreamType.AUDIO)
    val selectedStream = streams.firstOrNull { selection.audioSelected && selection.audioStreamIndex == it.index() }
    val defaultLabel = serverDefaultLabel(streams)
    return optionSelect(
        title = "音轨",
        selectedLabel = selectedStream?.let(::streamLabel) ?: defaultLabel,
        options = listOf(TvOptionSelectItem(defaultLabel, !selection.audioSelected) {
            onSelection(selection.forSource(source.id()).copy(
                audioSelected = false,
                audioStreamIndex = null,
                focusKey = FOCUS_AUDIO,
            ))
        }) + streams.map { stream ->
            TvOptionSelectItem(streamLabel(stream), selection.audioSelected && selection.audioStreamIndex == stream.index()) {
                onSelection(selection.forSource(source.id()).copy(
                    audioSelected = true,
                    audioStreamIndex = stream.index(),
                    focusKey = FOCUS_AUDIO,
                ))
            }
        },
        requestFocus = selection.focusKey == FOCUS_AUDIO,
    )
}

private fun ComponentActivity.subtitleOptionSelect(
    source: MediaSourceInfo,
    selection: DetailTrackSelection,
    onSelection: (DetailTrackSelection) -> Unit,
): View {
    val streams = source.streamsOf(MediaStreamType.SUBTITLE)
    val selectedStream = streams.firstOrNull { selection.subtitleSelected && selection.subtitleStreamIndex == it.index() }
    val defaultLabel = serverDefaultLabel(streams)
    return optionSelect(
        title = "字幕",
        selectedLabel = when {
            selection.subtitleSelected && selection.subtitleStreamIndex == SUBTITLES_OFF_INDEX -> "关闭字幕"
            selectedStream != null -> streamLabel(selectedStream)
            else -> defaultLabel
        },
        options = listOf(
            TvOptionSelectItem(defaultLabel, !selection.subtitleSelected) {
                onSelection(selection.forSource(source.id()).copy(
                    subtitleSelected = false,
                    subtitleStreamIndex = null,
                    burnSubtitleWhenTranscoding = false,
                    focusKey = FOCUS_SUBTITLE,
                ))
            },
            TvOptionSelectItem("关闭字幕", selection.subtitleSelected && selection.subtitleStreamIndex == SUBTITLES_OFF_INDEX) {
                onSelection(selection.forSource(source.id()).copy(
                    subtitleSelected = true,
                    subtitleStreamIndex = SUBTITLES_OFF_INDEX,
                    burnSubtitleWhenTranscoding = false,
                    focusKey = FOCUS_SUBTITLE,
                ))
            },
        ) + streams.map { stream ->
            TvOptionSelectItem(streamLabel(stream), selection.subtitleSelected && selection.subtitleStreamIndex == stream.index()) {
                onSelection(selection.forSource(source.id()).copy(
                    subtitleSelected = true,
                    subtitleStreamIndex = stream.index(),
                    burnSubtitleWhenTranscoding = stream.requiresBurnInWhenTranscoding(),
                    focusKey = FOCUS_SUBTITLE,
                ))
            }
        },
        requestFocus = selection.focusKey == FOCUS_SUBTITLE,
    )
}

private fun DetailTrackSelection.forSource(sourceId: String): DetailTrackSelection {
    return if (mediaSourceId == sourceId) {
        this
    } else {
        DetailTrackSelection(mediaSourceId = sourceId)
    }
}

private fun MediaSourceInfo.streamsOf(type: MediaStreamType): List<MediaStreamInfo> {
    return mediaStreams().filter { stream -> stream.type() == type }
}

private fun MediaSourceInfo.hasStream(type: MediaStreamType, streamIndex: Int?): Boolean {
    return streamsOf(type).any { stream -> stream.index() == streamIndex }
}

private fun sourceValue(source: MediaSourceInfo): String {
    return sourceLabel(source).removePrefix("媒体源：")
}

private fun serverDefaultLabel(streams: List<MediaStreamInfo>): String {
    val defaultStream = streams.firstOrNull { it.defaultStream() } ?: return "服务器默认"
    return "服务器默认 · ${streamLabel(defaultStream)}"
}

private fun MediaStreamInfo.requiresBurnInWhenTranscoding(): Boolean {
    val value = listOf(codec(), displayTitle()).joinToString(" ").lowercase()
    return value.contains("pgs") ||
        value.contains("dvdsub") ||
        value.contains("dvd_subtitle") ||
        value.contains("vobsub")
}

private const val FOCUS_SOURCE = "source"
private const val FOCUS_AUDIO = "audio"
private const val FOCUS_SUBTITLE = "subtitle"
private const val SUBTITLES_OFF_INDEX = -1
