package tv.cinepilot.tv.subtitles

import androidx.media3.common.text.Cue
import androidx.media3.extractor.text.Subtitle
import androidx.media3.extractor.text.SubtitleOutputBuffer

/**
 * Side-channel used by {@link PgsSubtitleDecoder} / {@link LibassSubtitleDecoder} to
 * publish the most recent subtitle payload produced by their decode loop.
 *
 * <p>Why this exists: Media3's {@code TextRenderer} only exposes the flattened
 * {@link Cue} list to its {@code TextOutput} callback; the original
 * {@link Subtitle} object (carrying bitmap PGS / composited ASS frames) is
 * discarded after {@link SubtitleOutputBuffer#getCues} returns. To render bitmap
 * subtitles we therefore need the decoder to hand its output here on every
 * {@code getCues} call, and the overlay reads it each frame during
 * {@link PgsSubtitleOverlay#onDraw}.
 *
 * <p>Publisher and subscriber are both constrained to the main / playback Looper
 * thread because:
 *   (a) {@code TextRenderer} is driven on the player's application Looper (usually
 *       the main Looper), and
 *   (b) {@code onDraw} is always called on the main thread.
 * No additional locking is therefore required.
 */
object SubtitleSideChannel {

    @Volatile
    private var latest: Subtitle? = null

    /** Called by PGS / ASS output buffers inside their {@code getCues} override. */
    fun publish(subtitle: Subtitle?) {
        latest = subtitle
    }

    /** Called once per overlay {@code onDraw} tick; null if nothing has been published. */
    fun consumeLatest(): Subtitle? = latest

    /** Clears any cached payload (e.g. when the player is released). */
    fun clear() {
        latest = null
    }
}
