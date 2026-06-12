package tv.cinepilot.tv.player

import androidx.media3.common.Format
import tv.cinepilot.core.protocol.MediaStreamInfo
import tv.cinepilot.core.protocol.MediaStreamType

class Media3StreamIndexResolver(
    streams: List<MediaStreamInfo>,
) {
    private val audioStreams = streams.filter { it.type() == MediaStreamType.AUDIO }
    private val subtitleStreams = streams.filter { it.type() == MediaStreamType.SUBTITLE }

    fun audioStreamIndex(format: Format): Int? {
        return selectedIndex(format, audioStreams)
    }

    fun subtitleStreamIndex(format: Format): Int? {
        return selectedIndex(format, subtitleStreams)
    }

    fun findAudioStream(streamIndex: Int): MediaStreamInfo? {
        return audioStreams.firstOrNull { it.index() == streamIndex }
    }

    fun findSubtitleStream(streamIndex: Int): MediaStreamInfo? {
        return subtitleStreams.firstOrNull { it.index() == streamIndex }
    }

    fun allAudioStreams(): List<MediaStreamInfo> = audioStreams

    fun allSubtitleStreams(): List<MediaStreamInfo> = subtitleStreams

    private fun selectedIndex(format: Format, candidates: List<MediaStreamInfo>): Int? {
        val label = normalized(format.label)
        if (label.isNotBlank()) {
            candidates.uniqueBy { normalized(it.displayTitle()) == label }?.let { return it.index() }
        }

        val language = normalized(format.language)
        val codec = normalizedCodec(format)
        if (language.isNotBlank() && codec.isNotBlank()) {
            candidates.uniqueBy {
                normalized(it.language()) == language && normalizedCodec(it.codec()) == codec
            }?.let { return it.index() }
        }
        if (language.isNotBlank()) {
            candidates.uniqueBy { normalized(it.language()) == language }?.let { return it.index() }
        }
        return null
    }

    private fun List<MediaStreamInfo>.uniqueBy(
        predicate: (MediaStreamInfo) -> Boolean,
    ): MediaStreamInfo? {
        val matches = filter(predicate)
        return if (matches.size == 1) matches.first() else null
    }

    private fun normalized(value: String?): String {
        return value.orEmpty().trim().lowercase()
    }

    private fun normalizedCodec(format: Format): String {
        val sample = normalized(format.sampleMimeType).substringAfterLast('/')
        val codecs = normalized(format.codecs).substringBefore(',').substringAfterLast('.')
        return (sample.ifBlank { codecs }).normalizeCodecAlias()
    }

    private fun normalizedCodec(value: String?): String {
        return normalized(value).normalizeCodecAlias()
    }

    private fun String.normalizeCodecAlias(): String {
        return when (this) {
            "ec-3" -> "eac3"
            "ac-3" -> "ac3"
            "mp4a", "mp4a-latm" -> "aac"
            "subrip" -> "srt"
            "webvtt" -> "vtt"
            else -> this
        }
    }
}
