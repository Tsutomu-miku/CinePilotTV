package tv.cinepilot.tv.runtime

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import tv.cinepilot.core.protocol.PlaybackDeviceProfile

class DeviceCodecDiagnostics {
    fun describe(): String {
        val builder = StringBuilder()
        builder.append("deviceModel=").append(Build.MODEL ?: "unknown").append('\n')
        builder.append("androidSdk=").append(Build.VERSION.SDK_INT).append('\n')
        val codecInfos = codecInfosOrNull()
            .getOrElse { error ->
                builder.append("codecProbeError=").append(error::class.java.simpleName).append('\n')
                return builder.toString()
            }
        TARGET_CODECS.forEach { target ->
            builder.append(target.key)
                .append('=')
                .append(decoderSummary(codecInfos, target.mimeType))
                .append('\n')
        }
        return builder.toString()
    }

    fun playbackDeviceProfile(): PlaybackDeviceProfile? {
        val codecInfos = codecInfosOrNull().getOrNull() ?: return null
        val videoCodecs = TARGET_VIDEO_CODECS.mapNotNull { target ->
            target.codecName.takeIf { decoderSummary(codecInfos, target.mimeType) != UNSUPPORTED }
        }
        val audioCodecs = TARGET_AUDIO_CODECS.mapNotNull { target ->
            target.codecName.takeIf { decoderSummary(codecInfos, target.mimeType) != UNSUPPORTED }
        }
        return PlaybackDeviceProfile(
            "CinePilot TV ${Build.MODEL ?: "Android TV"}",
            videoCodecs,
            audioCodecs,
            SUPPORTED_SUBTITLE_FORMATS,
        ).takeIf { it.hasCodecHints() }
    }

    private fun codecInfosOrNull(): Result<List<MediaCodecInfo>> {
        return runCatching { MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.toList() }
    }

    private fun decoderSummary(codecInfos: List<MediaCodecInfo>, mimeType: String): String {
        val decoders = codecInfos
            .asSequence()
            .filterNot { it.isEncoder }
            .filter { info -> info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) } }
            .map(::decoderLabel)
            .distinct()
            .take(MAX_DECODERS_PER_CODEC)
            .toList()
        return if (decoders.isEmpty()) UNSUPPORTED else decoders.joinToString(", ")
    }

    private fun decoderLabel(info: MediaCodecInfo): String {
        val acceleration = when {
            Build.VERSION.SDK_INT < 29 -> "unknown"
            info.isHardwareAccelerated -> "hardware"
            info.isSoftwareOnly -> "software"
            else -> "unknown"
        }
        return "$acceleration:${info.name}"
    }

    private data class TargetCodec(
        val key: String,
        val mimeType: String,
        val codecName: String,
    )

    private companion object {
        private const val MAX_DECODERS_PER_CODEC = 3
        private const val UNSUPPORTED = "unsupported"

        private val TARGET_VIDEO_CODECS = listOf(
            TargetCodec("codec.video.h264", "video/avc", "h264"),
            TargetCodec("codec.video.hevc", "video/hevc", "hevc"),
            TargetCodec("codec.video.av1", "video/av01", "av1"),
            TargetCodec("codec.video.vp9", "video/x-vnd.on2.vp9", "vp9"),
            TargetCodec("codec.video.dolbyVision", "video/dolby-vision", "dovi"),
        )

        private val TARGET_AUDIO_CODECS = listOf(
            TargetCodec("codec.audio.ac3", "audio/ac3", "ac3"),
            TargetCodec("codec.audio.eac3", "audio/eac3", "eac3"),
            TargetCodec("codec.audio.truehd", "audio/true-hd", "truehd"),
            TargetCodec("codec.audio.dts", "audio/vnd.dts", "dts"),
            TargetCodec("codec.audio.dtsHd", "audio/vnd.dts.hd", "dtshd"),
        )

        private val TARGET_CODECS = TARGET_VIDEO_CODECS + TARGET_AUDIO_CODECS
        private val SUPPORTED_SUBTITLE_FORMATS = listOf("srt", "ass", "ssa", "vtt")
    }
}
