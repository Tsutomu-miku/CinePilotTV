package tv.cinepilot.tv.details

import android.view.View
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaSourceInfo
import tv.cinepilot.core.protocol.MediaStreamInfo
import tv.cinepilot.core.protocol.MediaStreamType
import tv.cinepilot.core.protocol.PlaybackInfo
import tv.cinepilot.tv.ui.label
import tv.cinepilot.tv.ui.radioChoice
import tv.cinepilot.tv.ui.section
import tv.cinepilot.tv.ui.settingChoiceGroup
import tv.cinepilot.tv.ui.sourceLabel
import tv.cinepilot.tv.ui.streamLabel

data class DetailTrackSelection(
    val mediaSourceId: String? = null,
    val audioSelected: Boolean = false,
    val audioStreamIndex: Int? = null,
    val subtitleSelected: Boolean = false,
    val subtitleStreamIndex: Int? = null,
    val burnSubtitleWhenTranscoding: Boolean = false,
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
        if (sources.size > 1) {
            addView(section("媒体源"))
            addView(choiceGroup(sources) { source ->
                radioChoice(sourceLabel(source), source.id() == activeSource.id()) {
                    onSelection(DetailTrackSelection(mediaSourceId = source.id()))
                }
            })
        }
        addTrackGroup(
            title = "音轨",
            emptyText = "服务器未返回可选音轨",
            streams = activeSource.streamsOf(MediaStreamType.AUDIO),
            defaultSelected = !selection.audioSelected,
            defaultText = "服务器默认",
            optionSelected = { selection.audioSelected && selection.audioStreamIndex == it.index() },
            onDefault = {
                onSelection(selection.forSource(activeSource.id()).copy(audioSelected = false, audioStreamIndex = null))
            },
            onStream = { stream ->
                onSelection(selection.forSource(activeSource.id()).copy(audioSelected = true, audioStreamIndex = stream.index()))
            },
        )
        addTrackGroup(
            title = "字幕",
            emptyText = "服务器未返回可选字幕",
            streams = activeSource.streamsOf(MediaStreamType.SUBTITLE),
            defaultSelected = !selection.subtitleSelected,
            defaultText = "服务器默认",
            optionSelected = { selection.subtitleSelected && selection.subtitleStreamIndex == it.index() },
            onDefault = {
                onSelection(selection.forSource(activeSource.id()).copy(subtitleSelected = false, subtitleStreamIndex = null))
            },
            onStream = { stream ->
                onSelection(selection.forSource(activeSource.id()).copy(
                    subtitleSelected = true,
                    subtitleStreamIndex = stream.index(),
                    burnSubtitleWhenTranscoding = stream.requiresBurnInWhenTranscoding(),
                ))
            },
            extraChoice = radioChoice("关闭字幕", selection.subtitleSelected && selection.subtitleStreamIndex == SUBTITLES_OFF_INDEX) {
                onSelection(selection.forSource(activeSource.id()).copy(
                    subtitleSelected = true,
                    subtitleStreamIndex = SUBTITLES_OFF_INDEX,
                    burnSubtitleWhenTranscoding = false,
                ))
            },
        )
    }
}

private fun LinearLayout.addTrackGroup(
    title: String,
    emptyText: String,
    streams: List<MediaStreamInfo>,
    defaultSelected: Boolean,
    defaultText: String,
    optionSelected: (MediaStreamInfo) -> Boolean,
    onDefault: () -> Unit,
    onStream: (MediaStreamInfo) -> Unit,
    extraChoice: View? = null,
) {
    val activity = context as ComponentActivity
    addView(activity.section(title))
    if (streams.isEmpty() && extraChoice == null) {
        addView(activity.label(emptyText))
        return
    }
    val choices = mutableListOf<View>(activity.radioChoice(defaultText, defaultSelected, onDefault))
    extraChoice?.let(choices::add)
    streams.forEach { stream ->
        choices.add(activity.radioChoice(streamLabel(stream), optionSelected(stream)) {
            onStream(stream)
        })
    }
    addView(activity.settingChoiceGroup(choices))
}

private fun <T> ComponentActivity.choiceGroup(values: List<T>, build: (T) -> View): View {
    return settingChoiceGroup(values.map(build))
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

private fun MediaStreamInfo.requiresBurnInWhenTranscoding(): Boolean {
    val value = listOf(codec(), displayTitle()).joinToString(" ").lowercase()
    return value.contains("pgs") ||
        value.contains("dvdsub") ||
        value.contains("dvd_subtitle") ||
        value.contains("vobsub")
}

private const val SUBTITLES_OFF_INDEX = -1
