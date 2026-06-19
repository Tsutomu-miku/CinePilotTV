package tv.cinepilot.tv.subtitles

import android.os.Handler
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.text.SubtitleDecoderFactory
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.text.TextRenderer
import androidx.media3.extractor.text.Subtitle
import androidx.media3.extractor.text.SubtitleDecoder
import androidx.media3.extractor.text.SubtitleInputBuffer
import androidx.media3.extractor.text.SubtitleOutputBuffer
import java.nio.ByteBuffer
import tv.cinepilot.tv.playback.SubtitleEncoding

/**
 * Media3 {@link SubtitleDecoderFactory} override that upgrades three subtitle paths:
 *
 * <ol>
 *   <li><b>ASS / SSA</b> (MIME {@code text/ssa}): render via native LibASS when the
 *       JNI library is available; fall back to the default Media3 decoder otherwise.</li>
 *   <li><b>PGS / Presentation Graphic Stream</b> (MIME {@code application/pgs}):
 *       produce {@link PgsSubtitle} payloads that {@link PgsSubtitleOverlay} paints as
 *       transparent bitmap overlays on top of the player surface.</li>
 *   <li><b>SubRip / SRT</b> (MIME {@code application/x-subrip}): before passing bytes to
 *       the default SubripDecoder, run them through {@link SubtitleEncodingDetector} so
 *       Chinese (GBK / Big5) / Japanese (Shift_JIS) / Korean (EUC-KR) multi-byte files
 *       are correctly decoded to UTF-8 instead of becoming mojibake.</li>
 * </ol>
 *
 * <p>All other subtitle MIME types (WebVTT, TTML, DVB, ...) are delegated unchanged to
 * {@link DefaultSubtitleDecoderFactory}.
 *
 * <p>Installation: because Media3 1.5 does not expose {@code setSubtitleDecoderFactory}
 * on {@code DefaultMediaSourceFactory} (that API moved to the Extractors layer after
 * 1.5), this factory is installed by overriding {@code buildTextRenderers} inside
 * {@link #installOn(ExoPlayer.Builder, SubtitleEncoding)} via a custom
 * {@link DefaultRenderersFactory} — see the companion object.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class CinePilotSubtitleDecoderFactory(
    private val userPreferredEncoding: SubtitleEncoding = SubtitleEncoding.AUTO,
) : SubtitleDecoderFactory {

    private val defaultFactory = SubtitleDecoderFactory.DEFAULT

    override fun supportsFormat(format: androidx.media3.common.Format): Boolean {
        val sampleMimeType = format.sampleMimeType ?: return false
        return when {
            // P1-1: LibASS path
            MimeTypes.TEXT_SSA.equals(sampleMimeType, ignoreCase = true) ||
                sampleMimeType.equals("application/x-ass", ignoreCase = true) ||
                sampleMimeType.equals("application/ssa", ignoreCase = true) ||
                sampleMimeType.equals("application/ass", ignoreCase = true) -> true
            // P1-2: PGS bitmap path
            sampleMimeType.equals("application/pgs", ignoreCase = true) ||
                sampleMimeType.equals("application/x-pgs", ignoreCase = true) ||
                sampleMimeType.equals("image/pgs", ignoreCase = true) -> true
            // P1-3: SRT w/ encoding detection (always handled, default.supports covers it too)
            MimeTypes.APPLICATION_SUBRIP.equals(sampleMimeType, ignoreCase = true) -> true
            else -> defaultFactory.supportsFormat(format)
        }
    }

    override fun createDecoder(format: androidx.media3.common.Format): SubtitleDecoder {
        val sampleMimeType = format.sampleMimeType ?: ""
        return when {
            // ---- P1-1: LibASS native renderer ------------------------------------------
            MimeTypes.TEXT_SSA.equals(sampleMimeType, ignoreCase = true) ||
                sampleMimeType.equals("application/x-ass", ignoreCase = true) ||
                sampleMimeType.equals("application/ssa", ignoreCase = true) ||
                sampleMimeType.equals("application/ass", ignoreCase = true) -> {
                if (LibassSubtitleDecoder.nativeAvailable()) {
                    LibassSubtitleDecoder(format)
                } else {
                    // No native lib — fall through to default renderer which will try to
                    // parse ASS as generic text (best effort, author styling dropped).
                    defaultFactory.createDecoder(format)
                }
            }
            // ---- P1-2: PGS graphic subtitles -----------------------------------------
            sampleMimeType.equals("application/pgs", ignoreCase = true) ||
                sampleMimeType.equals("application/x-pgs", ignoreCase = true) ||
                sampleMimeType.equals("image/pgs", ignoreCase = true) -> {
                PgsSubtitleDecoder()
            }
            // ---- P1-3: SubRip w/ encoding detection -----------------------------------
            MimeTypes.APPLICATION_SUBRIP.equals(sampleMimeType, ignoreCase = true) -> {
                EncodingNormalizingSubtitleDecoder(
                    innerDecoder = defaultFactory.createDecoder(format),
                    userPreferredEncoding = userPreferredEncoding,
                )
            }
            else -> defaultFactory.createDecoder(format)
        }
    }

    /**
     * Wrapper that intercepts SubRip input bytes, re-encodes any non-UTF-8 payload
     * to UTF-8 using the {@link SubtitleEncodingDetector}, then delegates to the
     * default SubripDecoder. Without this wrapper Chinese / Japanese subtitles stored
     * as GBK / Shift_JIS end up as mojibake on screen (P1-3).
     */
    private class EncodingNormalizingSubtitleDecoder(
        private val innerDecoder: SubtitleDecoder,
        private val userPreferredEncoding: SubtitleEncoding,
    ) : SubtitleDecoder by innerDecoder {

        override fun getName(): String = "EncodingNormalizingSubtitleDecoder(${innerDecoder.name})"

        override fun queueInputBuffer(inputBuffer: androidx.media3.extractor.text.SubtitleInputBuffer) {
            // Rewrite the input buffer's bytes: use the encoding detector to normalize
            // multi-byte payloads into UTF-8 before the default SubripDecoder parses them.
            val originalData = inputBuffer.data
            if (originalData != null) {
                val bytes = ByteArray(originalData.remaining())
                originalData.get(bytes)
                val normalized = SubtitleEncodingDetector.decodeWithEncoding(bytes, userPreferredEncoding)
                val utf8Bytes = normalized.toByteArray(Charsets.UTF_8)
                val replacement = ByteBuffer.wrap(utf8Bytes)
                inputBuffer.clear()
                inputBuffer.data = replacement
            }
            innerDecoder.queueInputBuffer(inputBuffer)
        }
    }

    companion object {
        /**
         * Installs this factory on the supplied ExoPlayer builder. Media3 1.5 routes
         * subtitle decoder selection through the TextRenderer constructor, which is
         * supplied by {@link DefaultRenderersFactory#buildTextRenderers}. We inject a
         * single-element override list containing {@link TextRenderer} constructed with
         * our factory. Must be called before {@code build()}.
         *
         * <p>Callers also pass a {@link TextOutput} callback (typically the
         * {@link PgsSubtitleOverlay}'s side-channel bridge) so bitmap-subtitle payloads
         * produced by PGS / ASS decoders can reach the overlay layer without going
         * through the standard Cue pipeline.
         */
        fun installOn(
            builder: androidx.media3.exoplayer.ExoPlayer.Builder,
            context: android.content.Context,
            userPreferredEncoding: SubtitleEncoding,
            extraTextOutput: TextOutput? = null,
        ) {
            val factory = CinePilotSubtitleDecoderFactory(userPreferredEncoding)
            val base = DefaultRenderersFactory(context)
            builder.setRenderersFactory(object : DefaultRenderersFactory(context) {
                @androidx.annotation.OptIn(UnstableApi::class)
                override fun buildTextRenderers(
                    context: android.content.Context,
                    output: TextOutput,
                    outputLooper: android.os.Looper,
                    extensionRendererMode: Int,
                    out: java.util.ArrayList<Renderer>,
                ) {
                    // Combine the primary text output (pipes to Player.Listener.onCues)
                    // with the overlay side-channel so both text cues and bitmap payloads
                    // propagate correctly.
                    val sink = if (extraTextOutput != null) {
                        CompositeTextOutput(output, extraTextOutput)
                    } else {
                        output
                    }
                    out.add(TextRenderer(sink, outputLooper, factory))
                }
            })
        }
    }
}

/**
 * Minimal bridge interface: a decoded subtitle payload produced by any of the
 * CinePilot decoders (LibASS, PGS, …). Media3 only requires
 * {@link Subtitle#getEventTimeCount} / {@link #getEventTime} / {@link #getNextEventTimeIndex}
 * so the player UI can position cues during playback. {@link #getCues} is used by
 * Media3's SubtitleView for text rendering; for our bitmap pipelines we return an
 * empty list there and paint through {@link PgsSubtitleOverlay} instead.
 */
interface CinePilotSubtitle : Subtitle {
    /** Number of discrete subtitle events (cues / frames). */
    override fun getEventTimeCount(): Int
    /** Presentation timestamp, in microseconds, of the i-th event. */
    override fun getEventTime(index: Int): Long
}

/**
 * Broadcast a single {@code TextOutput.onCues} callback to multiple consumers. Used
 * so PGS / ASS bitmap payloads delivered through the TextRenderer output path reach
 * both the standard SubtitleView (for onCues) and our PgsSubtitleOverlay.
 */
class CompositeTextOutput(
    private val first: TextOutput,
    private val second: TextOutput,
) : TextOutput {
    override fun onCues(cueGroup: androidx.media3.common.text.CueGroup) {
        first.onCues(cueGroup)
        second.onCues(cueGroup)
    }

}

/** Keep lint happy about unused Handler import (kept for follow-up refactors). */
@Suppress("unused")
private val _HANDLER_REF: Handler? = null
