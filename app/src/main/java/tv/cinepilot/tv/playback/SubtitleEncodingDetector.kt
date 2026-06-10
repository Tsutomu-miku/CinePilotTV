package tv.cinepilot.tv.playback

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * Lightweight subtitle encoding detector.
 *
 * <p>Intentionally heuristic and dependency-free; avoids bundling juniversalchardet for
 * the ~99% of real-world subtitles that are one of UTF-8 (with or without BOM), GBK,
 * BIG5, Shift_JIS or EUC-KR. Unknown bytes fall back to UTF-8 with
 * {@link CodingErrorAction#REPLACE} so renderers never crash on mixed byte streams.
 *
 * <p>Supported output:
 * <ul>
 *   <li>{@code UTF-8} — explicit BOM, or valid UTF-8 without CJK bytes.
 *   <li>{@code UTF-16LE/BE} — explicit BOM.
 *   <li>{@code GBK} — high byte range [0x81..0xFE] + [0x40..0xFE] byte pairs dominate.
 *   <li>{@code BIG5} — high byte range [0x81..0xFE] + [0x40..0x7E, 0xA1..0xFE] pairs.
 *   <li>{@code Shift_JIS} — high byte [0x81..0x9F, 0xE0..0xEF] + trailing pairs or
 *       single-byte Hiragana Katakana block.
 *   <li>{@code EUC-KR} — dominant 0xB0..0xC5 leading byte (Hangul syllables).
 * </ul>
 *
 * <p>Always decoded with {@code onMalformedInput=REPLACE}, so callers never need to
 * catch {@link java.nio.charset.MalformedInputException}.
 */
object SubtitleEncodingDetector {

    fun detect(raw: ByteArray, override: SubtitleEncoding): String {
        return when (override) {
            SubtitleEncoding.UTF8 -> StandardCharsets.UTF_8.name()
            SubtitleEncoding.GBK -> "GBK"
            SubtitleEncoding.BIG5 -> "Big5"
            SubtitleEncoding.SHIFT_JIS -> "Shift_JIS"
            SubtitleEncoding.EUC_KR -> "EUC-KR"
            SubtitleEncoding.AUTO -> detectAuto(raw)
        }
    }

    fun decodeToString(raw: ByteArray, override: SubtitleEncoding): String {
        val charsetName = detect(raw, override)
        val charset = runCatching { Charset.forName(charsetName) }
            .getOrDefault(StandardCharsets.UTF_8)
        val decoder = charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
        return runCatching { decoder.decode(ByteBuffer.wrap(raw)).toString() }
            .getOrDefault(String(raw, StandardCharsets.UTF_8))
    }

    private fun detectAuto(raw: ByteArray): String {
        val sampleSize = (raw.size).coerceAtMost(16 * 1024)
        if (sampleSize >= 3 &&
            raw[0] == 0xEF.toByte() && raw[1] == 0xBB.toByte() && raw[2] == 0xBF.toByte()
        ) {
            return StandardCharsets.UTF_8.name()
        }
        if (sampleSize >= 2 && raw[0] == 0xFF.toByte() && raw[1] == 0xFE.toByte()) {
            return "UTF-16LE"
        }
        if (sampleSize >= 2 && raw[0] == 0xFE.toByte() && raw[1] == 0xFF.toByte()) {
            return "UTF-16BE"
        }
        if (isValidUtf8(raw, sampleSize)) {
            return StandardCharsets.UTF_8.name()
        }
        var gbkHits = 0
        var big5Hits = 0
        var sjisHits = 0
        var euckrHits = 0
        var i = 0
        while (i < sampleSize - 1) {
            val b0 = raw[i].toInt() and 0xFF
            val b1 = raw[i + 1].toInt() and 0xFF
            if (gbkLead(b0) && gbkTrail(b1)) gbkHits++
            if (big5Lead(b0) && big5Trail(b1)) big5Hits++
            if (sjisLead(b0) && sjisTrail(b1)) sjisHits++
            if (euckrLead(b0) && euckrTrail(b1)) euckrHits++
            i += if (b0 >= 0x80) 2 else 1
        }
        val scores = listOf(
            "GBK" to gbkHits,
            "Big5" to big5Hits,
            "Shift_JIS" to sjisHits,
            "EUC-KR" to euckrHits,
        )
        val best = scores.maxByOrNull { it.second }
        return if (best != null && best.second > 0) best.first else StandardCharsets.UTF_8.name()
    }

    private fun isValidUtf8(raw: ByteArray, sample: Int): Boolean {
        var i = 0
        var highByteCount = 0
        while (i < sample) {
            val b = raw[i].toInt() and 0xFF
            when {
                b < 0x80 -> i += 1
                b < 0xC0 -> return false
                b < 0xE0 -> {
                    if (i + 1 >= sample || !cont(raw[i + 1])) return false
                    i += 2
                    highByteCount++
                }
                b < 0xF0 -> {
                    if (i + 2 >= sample || !cont(raw[i + 1]) || !cont(raw[i + 2])) return false
                    i += 3
                    highByteCount++
                }
                b < 0xF8 -> {
                    if (i + 3 >= sample ||
                        !cont(raw[i + 1]) || !cont(raw[i + 2]) || !cont(raw[i + 3])
                    ) return false
                    i += 4
                    highByteCount++
                }
                else -> return false
            }
        }
        // Pure-ASCII texts are UTF-8. Small CJK UTF-8 texts (e.g. a subtitle with Chinese
        // chars) should also remain UTF-8; only when multi-byte sequences dominate do we
        // bother with legacy CJK detection.
        return highByteCount < sample * 20 / 100
    }

    private fun cont(b: Byte): Boolean {
        val v = b.toInt() and 0xFF
        return v in 0x80..0xBF
    }

    private fun gbkLead(b: Int): Boolean = b in 0x81..0xFE
    private fun gbkTrail(b: Int): Boolean = b in 0x40..0xFE && b != 0x7F
    private fun big5Lead(b: Int): Boolean = b in 0x81..0xFE
    private fun big5Trail(b: Int): Boolean = b in 0x40..0x7E || b in 0xA1..0xFE
    private fun sjisLead(b: Int): Boolean = b in 0x81..0x9F || b in 0xE0..0xEF
    private fun sjisTrail(b: Int): Boolean = b in 0x40..0x7E || b in 0x80..0xFC
    private fun euckrLead(b: Int): Boolean = b in 0xB0..0xC5 || b in 0x81..0xFE
    private fun euckrTrail(b: Int): Boolean = b in 0x41..0xFE
}
