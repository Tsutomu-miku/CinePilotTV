package tv.cinepilot.tv.runtime

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build

class DeviceCodecDiagnostics {
    fun describe(): String {
        val builder = StringBuilder()
        builder.append("deviceModel=").append(Build.MODEL ?: "unknown").append('\n')
        builder.append("androidSdk=").append(Build.VERSION.SDK_INT).append('\n')
        val codecInfos = runCatching { MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.toList() }
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

    private fun decoderSummary(codecInfos: List<MediaCodecInfo>, mimeType: String): String {
        val decoders = codecInfos
            .asSequence()
            .filterNot { it.isEncoder }
            .filter { info -> info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) } }
            .map(::decoderLabel)
            .distinct()
            .take(MAX_DECODERS_PER_CODEC)
            .toList()
        return if (decoders.isEmpty()) "unsupported" else decoders.joinToString(", ")
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
    )

    private companion object {
        private const val MAX_DECODERS_PER_CODEC = 3

        private val TARGET_CODECS = listOf(
            TargetCodec("codec.video.h264", "video/avc"),
            TargetCodec("codec.video.hevc", "video/hevc"),
            TargetCodec("codec.video.av1", "video/av01"),
            TargetCodec("codec.video.vp9", "video/x-vnd.on2.vp9"),
            TargetCodec("codec.video.dolbyVision", "video/dolby-vision"),
            TargetCodec("codec.audio.ac3", "audio/ac3"),
            TargetCodec("codec.audio.eac3", "audio/eac3"),
            TargetCodec("codec.audio.truehd", "audio/true-hd"),
            TargetCodec("codec.audio.dts", "audio/vnd.dts"),
            TargetCodec("codec.audio.dtsHd", "audio/vnd.dts.hd"),
        )
    }
}
