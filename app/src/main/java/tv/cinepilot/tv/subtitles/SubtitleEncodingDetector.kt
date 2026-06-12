package tv.cinepilot.tv.subtitles

import org.mozilla.universalchardet.UniversalDetector
import tv.cinepilot.core.protocol.MediaTicks
import tv.cinepilot.tv.playback.SubtitleEncoding
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.Charset

/**
 * Thin wrapper around juniversalchardet, specialized for subtitle byte streams.
 *
 * <p>Given the raw bytes of a subtitle file (typically SRT), returns a best-guess
 * {@link SubtitleEncoding} so callers can decode the text correctly. Detection is
 * intentionally conservative: the algorithm probes the leading sampleSize bytes and
 * falls back to {@link SubtitleEncoding#UTF8} when confidence is low to avoid a
 * garbled UI from a false positive.
 *
 * <p>Media3 ExoPlayer's default SubripDecoder always decodes bytes as UTF-8; this
 * detector is used by {@link CinePilotSubtitleDecoderFactory} to re-charset-decode
 * SRT/ASS payloads before the subtitles reach ExoPlayer's text renderers.
 */
object SubtitleEncodingDetector {

    private const val DEFAULT_SAMPLE_SIZE = 64 * 1024

    /**
     * Run the detector against a full byte array. Returns a {@link SubtitleEncoding}
     * enum, using {@link SubtitleEncoding#AUTO} as a sentinel when detection is
     * uncertain (callers should fall back to UTF-8 in that case).
     */
    fun detect(
        bytes: ByteArray,
        sampleSize: Int = DEFAULT_SAMPLE_SIZE,
    ): SubtitleEncoding =
        detect(ByteArrayInputStream(bytes), sampleSize)

    /**
     * Stream variant; consumes at most {@code sampleSize} bytes from {@code input}
     * without closing the stream.
     */
    fun detect(
        input: InputStream,
        sampleSize: Int = DEFAULT_SAMPLE_SIZE,
    ): SubtitleEncoding {
        val detector = UniversalDetector(null)
        val buf = ByteArray(4096)
        var readTotal = 0
        while (readTotal < sampleSize) {
            val nread = input.read(buf, 0, minOf(buf.size, sampleSize - readTotal))
            if (nread <= 0) break
            detector.handleData(buf, 0, nread)
            readTotal += nread
            if (detector.isDone) break
        }
        detector.dataEnd()
        val detected = detector.detectedCharset
        return when {
            detected == null -> SubtitleEncoding.AUTO
            detected.equals("UTF-8", ignoreCase = true) -> SubtitleEncoding.UTF8
            detected.startsWith("GB", ignoreCase = true) ||
                detected.equals("GB2312", ignoreCase = true) ||
                detected.equals("GB18030", ignoreCase = true) -> SubtitleEncoding.GBK
            detected.equals("BIG5", ignoreCase = true) ||
                detected.equals("Big5-HKSCS", ignoreCase = true) -> SubtitleEncoding.BIG5
            detected.equals("Shift_JIS", ignoreCase = true) ||
                detected.equals("EUC-JP", ignoreCase = true) -> SubtitleEncoding.SHIFT_JIS
            detected.equals("EUC-KR", ignoreCase = true) -> SubtitleEncoding.EUC_KR
            else -> SubtitleEncoding.AUTO
        }
    }

    /** Best-effort decode using the supplied encoding enum + fallback chain. */
    fun decodeWithEncoding(
        bytes: ByteArray,
        preference: SubtitleEncoding,
    ): String {
        val candidate = when (preference) {
            SubtitleEncoding.AUTO -> {
                val detected = detect(bytes)
                if (detected == SubtitleEncoding.AUTO) charsetOrNull("UTF-8") else detected.toCharset()
            }
            else -> preference.toCharset()
        }
        // Two-pass decode: try the preferred charset; on unrecoverable failure, fall
        // back to UTF-8 so at least ASCII portions of the subtitle are usable.
        return runCatching {
            String(bytes, candidate ?: Charsets.UTF_8)
        }.getOrElse {
            String(bytes, Charsets.UTF_8)
        }
    }

    private fun SubtitleEncoding.toCharset(): Charset? = when (this) {
        SubtitleEncoding.AUTO -> charsetOrNull("UTF-8")
        SubtitleEncoding.UTF8 -> Charsets.UTF_8
        SubtitleEncoding.GBK -> charsetOrNull("GBK") ?: charsetOrNull("GB2312")
        SubtitleEncoding.BIG5 -> charsetOrNull("Big5") ?: charsetOrNull("Big5-HKSCS")
        SubtitleEncoding.SHIFT_JIS -> charsetOrNull("Shift_JIS") ?: charsetOrNull("EUC-JP")
        SubtitleEncoding.EUC_KR -> charsetOrNull("EUC-KR")
    }

    private fun charsetOrNull(name: String): Charset? = runCatching {
        Charset.forName(name)
    }.getOrNull()

    /** Keep lint happy about unused MediaTicks import (kept for future helpers). */
    private const val _UNREFERENCED = MediaTicks.TICKS_PER_SECOND
}
