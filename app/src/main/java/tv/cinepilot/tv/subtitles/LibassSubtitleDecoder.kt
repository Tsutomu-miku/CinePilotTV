package tv.cinepilot.tv.subtitles

import android.graphics.Bitmap
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.SubtitleDecoder
import androidx.media3.extractor.text.SubtitleDecoderException
import androidx.media3.extractor.text.SubtitleInputBuffer
import androidx.media3.extractor.text.SubtitleOutputBuffer

// -----------------------------------------------------------------------------
// Value types (declared first so the decoder can reference them without issues
// when the Kotlin compiler evaluates top-level declaration order).
// -----------------------------------------------------------------------------

/**
 * Single rendered ASS cue: the LibASS renderer has already composed an RGBA_8888
 * bitmap for this subtitle event at the display dimensions supplied to
 * {@link LibassSubtitleDecoder}. {@code x / y} are in display pixels (top-left
 * origin), relative to the video surface's unscaled coordinate space.
 */
data class LibassFrame(
    val bitmap: Bitmap,
    val x: Int,
    val y: Int,
    val timeUs: Long,
    val durationUs: Long,
)

/** Glue so {@link PgsSubtitleOverlay} can render ASS frames identically to PGS. */
class LibassSubtitle(
    internal val displayWidth: Int,
    internal val displayHeight: Int,
    internal val frames: List<LibassFrame>,
) : CinePilotSubtitle {
    override fun getEventTimeCount(): Int = frames.size
    override fun getEventTime(index: Int): Long = frames[index].timeUs

    override fun getNextEventTimeIndex(timeUs: Long): Int {
        for (i in frames.indices) {
            if (frames[i].timeUs > timeUs) return i
        }
        return C.INDEX_UNSET
    }

    override fun getCues(timeUs: Long): MutableList<androidx.media3.common.text.Cue> {
        // Side-channel push: ASS bitmap payload is published here so
        // PgsSubtitleOverlay can pick it up on every render tick; SubtitleView
        // gets an empty list because this is a bitmap-only pipeline.
        SubtitleSideChannel.publish(this)
        return mutableListOf()
    }

    fun framesAt(positionUs: Long): List<LibassFrame> {
        return frames.filter { frame ->
            positionUs >= frame.timeUs &&
                (frame.durationUs == C.TIME_UNSET || positionUs < frame.timeUs + frame.durationUs)
        }
    }
}

class LibassSubtitleException(message: String) : SubtitleDecoderException(message)

// -----------------------------------------------------------------------------
// Decoder
// -----------------------------------------------------------------------------

/**
 * ASS / SSA subtitle decoder backed by a native LibASS renderer via JNI.
 *
 * <p>Availability is gated by {@link #nativeAvailable()}; when the native library
 * cannot be loaded, callers fall back to {@link SubtitleDecoderFactory#DEFAULT}
 * which drops author styling but keeps cue timing legible.
 *
 * <p>Implements {@link SubtitleDecoder} directly so the input/output buffers
 * match exactly what TextRenderer expects; this keeps the pipeline smaller than
 * subclassing the parser-centric {@code SimpleSubtitleDecoder}.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class LibassSubtitleDecoder(
    format: androidx.media3.common.Format,
) : SubtitleDecoder {

    private val displayWidth = format.width.coerceAtLeast(0).takeIf { it > 0 } ?: 1920
    private val displayHeight = format.height.coerceAtLeast(0).takeIf { it > 0 } ?: 1080
    private val nativeHandle: Long = if (nativeAvailable()) {
        nativeInit(displayWidth, displayHeight)
    } else {
        0L
    }
    private var released = false
    private var pendingOutput: SubtitleOutputBuffer? = null

    override fun getName(): String = "LibassSubtitleDecoder"

    override fun setOutputStartTimeUs(timeUs: Long) = Unit

    override fun dequeueInputBuffer(): SubtitleInputBuffer = SubtitleInputBuffer()

    override fun queueInputBuffer(inputBuffer: SubtitleInputBuffer) {
        if (inputBuffer.isEndOfStream) {
            val out = BorrowedOutputBuffer()
            out.addFlag(C.BUFFER_FLAG_END_OF_STREAM)
            pendingOutput = out
            return
        }
        val data = inputBuffer.data
        val ptsUs = inputBuffer.timeUs.takeIf { it != C.TIME_UNSET } ?: 0L
        val result: LibassSubtitle = if (data != null && nativeHandle != 0L) {
            val bytes = ByteArray(data.remaining())
            data.get(bytes)
            val nativeFrames: Array<LibassFrame>? =
                nativeDecodeAss(nativeHandle, bytes, bytes.size, ptsUs)
            LibassSubtitle(displayWidth, displayHeight, nativeFrames?.toList().orEmpty())
        } else {
            LibassSubtitle(displayWidth, displayHeight, emptyList())
        }
        val out = BorrowedOutputBuffer()
        out.setContent(ptsUs, result, 0L)
        pendingOutput = out
    }

    override fun dequeueOutputBuffer(): SubtitleOutputBuffer? {
        val out = pendingOutput
        pendingOutput = null
        return out
    }

    override fun flush() {
        pendingOutput = null
    }

    override fun release() {
        if (released) return
        released = true
        pendingOutput = null
        if (nativeHandle != 0L) runCatching { nativeRelease(nativeHandle) }
    }

    override fun setPositionUs(positionUs: Long) = Unit

    // --- JNI bridge -----------------------------------------------------------------
    private external fun nativeInit(width: Int, height: Int): Long
    private external fun nativeRelease(handle: Long)
    private external fun nativeDecodeAss(
        handle: Long,
        bytes: ByteArray,
        size: Int,
        ptsUs: Long,
    ): Array<LibassFrame>?

    companion object {
        private var nativeLoadAttempted = false
        private var nativeLoaded = false
        private var hasRenderer = false

        fun nativeAvailable(): Boolean {
            if (!nativeLoadAttempted) {
                nativeLoadAttempted = true
                nativeLoaded = runCatching {
                    System.loadLibrary("cinepilot_subs")
                    hasRenderer = nativeHasAssRenderer()
                    true
                }.getOrDefault(false)
            }
            return nativeLoaded && hasRenderer
        }

        @JvmStatic
        private external fun nativeHasAssRenderer(): Boolean
    }

    /**
     * Concrete {@link SubtitleOutputBuffer} — base is abstract and requires
     * {@link #release()} to call back. Here we just clear (the GC owns the buffer).
     */
    private class BorrowedOutputBuffer : SubtitleOutputBuffer() {
        override fun release() {
            clear()
        }
    }
}
