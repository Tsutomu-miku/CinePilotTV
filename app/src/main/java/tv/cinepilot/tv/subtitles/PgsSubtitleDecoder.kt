package tv.cinepilot.tv.subtitles

import android.graphics.Bitmap
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.SubtitleDecoder
import androidx.media3.extractor.text.SubtitleDecoderException
import androidx.media3.extractor.text.SubtitleInputBuffer
import androidx.media3.extractor.text.SubtitleOutputBuffer
import java.nio.ByteBuffer

/**
 * Minimal Presentation Graphic Stream (PGS / Blu-ray sup) decoder.
 *
 * <p>Parses PGS segment streams into timestamped {@link PgsFrame} bitmaps, rendered
 * through the transparent {@link PgsSubtitleOverlay} layered above the video surface.
 *
 * <p>Parser supports the minimum segment types required to emit a composited frame:
 * PCS (0x16), WDS (0x17), PDS (0x14), ODS (0x15), END (0x80). Corrupt streams produce
 * an empty subtitle rather than throwing so playback continues uninterrupted.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class PgsSubtitleDecoder : SubtitleDecoder {

    private var released = false
    private val inputQueue = ArrayDeque<SubtitleInputBuffer>(4)
    private var pendingOutput: SubtitleOutputBuffer? = null

    init {
        repeat(4) { inputQueue.addLast(SubtitleInputBuffer()) }
    }

    override fun getName(): String = "PgsSubtitleDecoder"

    override fun setOutputStartTimeUs(timeUs: Long) = Unit

    override fun dequeueInputBuffer(): SubtitleInputBuffer {
        return inputQueue.removeFirstOrNull() ?: SubtitleInputBuffer()
    }

    override fun queueInputBuffer(inputBuffer: SubtitleInputBuffer) {
        if (inputBuffer.isEndOfStream) {
            val out = BorrowedOutputBuffer { releaseOutputBuffer(it) }
            out.addFlag(C.BUFFER_FLAG_END_OF_STREAM)
            pendingOutput = out
            return
        }
        val data = inputBuffer.data
        val ptsUs = inputBuffer.timeUs.takeIf { it != C.TIME_UNSET } ?: 0L
        val subtitle: PgsSubtitle = if (data != null) {
            val bytes = ByteArray(data.remaining())
            data.get(bytes)
            val frames = runCatching { parsePgs(ByteBuffer.wrap(bytes)) }.getOrDefault(emptyList())
            PgsSubtitle(
                width = frames.firstOrNull()?.bitmap?.width ?: 0,
                height = frames.firstOrNull()?.bitmap?.height ?: 0,
                frames = frames,
                basePtsUs = ptsUs,
            )
        } else {
            PgsSubtitle(0, 0, emptyList(), ptsUs)
        }
        val out = BorrowedOutputBuffer { releaseOutputBuffer(it) }
        out.setContent(ptsUs, subtitle, 0L)
        pendingOutput = out
    }

    override fun dequeueOutputBuffer(): SubtitleOutputBuffer? {
        val out = pendingOutput
        pendingOutput = null
        return out
    }

    fun releaseOutputBuffer(buffer: SubtitleOutputBuffer) {
        if (buffer is BorrowedOutputBuffer) buffer.clear()
    }

    override fun flush() {
        pendingOutput = null
    }

    override fun release() {
        if (released) return
        released = true
        pendingOutput = null
    }

    override fun setPositionUs(positionUs: Long) = Unit

    // ---- PGS segment parser --------------------------------------------------------

    private data class PgsPalette(val entries: IntArray) { // RGBA ints, indexed 0..255
        override fun equals(other: Any?): Boolean =
            other is PgsPalette && entries.contentEquals(other.entries)
        override fun hashCode(): Int = entries.contentHashCode()
    }

    private data class CompositionObject(
        val objectId: Int,
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
    )

    private fun parsePgs(buf: ByteBuffer): List<PgsFrame> {
        val frames = mutableListOf<PgsFrame>()
        var currentPalette: PgsPalette? = null
        var pendingCompositions: List<CompositionObject> = emptyList()
        var pendingPts: Long = 0L
        var width = 0
        var height = 0
        val objectBitmaps = hashMapOf<Int, Bitmap>()

        while (buf.remaining() >= 13) {
            val pts90 = buf.int.toLong() and 0xFFFFFFFFL
            @Suppress("UNUSED_VARIABLE") val dts90 = buf.int.toLong() and 0xFFFFFFFFL
            val type = buf.get().toInt() and 0xFF
            val segSize = buf.short.toInt() and 0xFFFF
            val payloadStart = buf.position()
            if (segSize < 0 || buf.remaining() < segSize) break
            val safePtsUs = if (pts90 != 0L) (pts90 * 1_000_000L) / 90_000L else 0L

            when (type) {
                0x16 -> { // PCS — Presentation Composition Segment
                    if (segSize < 11) { buf.position(payloadStart + segSize); continue }
                    width = buf.short.toInt() and 0xFFFF
                    height = buf.short.toInt() and 0xFFFF
                    buf.position(buf.position() + 4)
                    val numObjects = buf.short.toInt() and 0xFFFF
                    val comps = mutableListOf<CompositionObject>()
                    repeat(numObjects) {
                        if (buf.position() - payloadStart + 8 > segSize) return@repeat
                        val objectId = buf.short.toInt() and 0xFFFF
                        buf.position(buf.position() + 1)
                        val objectCroppedFlag = buf.get().toInt() and 0xFF
                        val x = buf.short.toInt() and 0xFFFF
                        val y = buf.short.toInt() and 0xFFFF
                        var objW = width
                        var objH = height
                        if (objectCroppedFlag == 0x40 && buf.position() - payloadStart + 8 <= segSize) {
                            buf.position(buf.position() + 4)
                            objW = (buf.short.toInt() and 0xFFFF) + 1
                            objH = (buf.short.toInt() and 0xFFFF) + 1
                        }
                        comps += CompositionObject(objectId, x, y, objW, objH)
                    }
                    pendingCompositions = comps
                    pendingPts = safePtsUs
                    objectBitmaps.clear()
                }
                0x17 -> { /* WDS — window definition, skip. */ }
                0x14 -> { // PDS — Palette Definition Segment
                    val entries = IntArray(256)
                    var pos = payloadStart + 2
                    while (pos < payloadStart + segSize - 4) {
                        buf.position(pos)
                        val idx = buf.get().toInt() and 0xFF
                        val y = buf.get().toInt() and 0xFF
                        val cb = buf.get().toInt() and 0xFF
                        val cr = buf.get().toInt() and 0xFF
                        val t = buf.get().toInt() and 0xFF
                        if (idx in 0..255) entries[idx] = ycbcrToArgb(y, cb, cr, t)
                        pos += 5
                    }
                    currentPalette = PgsPalette(entries)
                }
                0x15 -> { // ODS — Object Definition Segment
                    if (segSize < 11) { buf.position(payloadStart + segSize); continue }
                    buf.position(payloadStart)
                    val objectId = buf.short.toInt() and 0xFFFF
                    buf.position(buf.position() + 2) // version + flags
                    buf.position(buf.position() + 3) // skip data_length (24-bit)
                    // ODS payload: object_width (2 bytes) + object_height (2 bytes) + RLE data
                    val objectWidth = buf.short.toInt() and 0xFFFF
                    val objectHeight = buf.short.toInt() and 0xFFFF
                    val palette = currentPalette
                    val comp = pendingCompositions.firstOrNull { it.objectId == objectId }
                    // Prefer composition dimensions when set (e.g. cropped objects),
                    // fall back to the ODS native dimensions for unscaled glyphs.
                    val compW = comp?.width ?: 0
                    val compH = comp?.height ?: 0
                    val w = if (compW > 0) compW else objectWidth
                    val h = if (compH > 0) compH else objectHeight
                    val remaining = (segSize - (buf.position() - payloadStart)).coerceAtLeast(0)
                    if (remaining > 0 && palette != null && w > 0 && h > 0) {
                        val slice = ByteArray(remaining)
                        buf.get(slice)
                        val bmp = decodeRle(ByteBuffer.wrap(slice), w, h, palette)
                        if (bmp != null) objectBitmaps[objectId] = bmp
                    }
                }
                0x80 -> { // END segment — emit composited frame
                    if (pendingCompositions.isNotEmpty()) {
                        frames += PgsFrame(
                            bitmap = composite(width, height, pendingCompositions, objectBitmaps),
                            x = 0,
                            y = 0,
                            timeUs = pendingPts,
                            durationUs = C.TIME_UNSET,
                        )
                    }
                    pendingCompositions = emptyList()
                    objectBitmaps.clear()
                }
                else -> Unit
            }
            buf.position(payloadStart + segSize)
        }
        if (frames.isEmpty() && pendingCompositions.isNotEmpty()) {
            frames += PgsFrame(
                bitmap = composite(width, height, pendingCompositions, objectBitmaps),
                x = 0,
                y = 0,
                timeUs = pendingPts,
                durationUs = C.TIME_UNSET,
            )
        }
        // Set each frame's duration to the start of the next frame. PGS subtitles
        // are replaced by the next composition; without explicit durations every
        // frame stays visible forever and stacks up on the overlay.
        for (i in 0 until frames.size - 1) {
            val nextStart = frames[i + 1].timeUs
            if (nextStart > frames[i].timeUs) {
                frames[i] = frames[i].copy(durationUs = nextStart - frames[i].timeUs)
            }
        }
        return frames
    }

    private fun decodeRle(
        buf: ByteBuffer,
        width: Int,
        height: Int,
        palette: PgsPalette,
    ): Bitmap? {
        if (width <= 0 || height <= 0 || width > 4096 || height > 4096) return null
        val pixels = IntArray(width * height)
        var line = 0
        var px = 0
        while (line < height && buf.hasRemaining() && px < pixels.size) {
            val b = buf.get().toInt() and 0xFF
            when {
                b != 0x00 -> {
                    pixels[px++] = palette.entries[b]
                }
                else -> {
                    val next = if (buf.hasRemaining()) buf.get().toInt() and 0xFF else break
                    when {
                        next == 0x00 -> {
                            line++
                            px = line * width
                        }
                        (next and 0x80) != 0 -> {
                            // Colored run: bit 7 set.
                            if (next and 0x40 != 0) {
                                // Extended colored run (0xC0..0xFF): 2-byte length + 1 color byte.
                                if (buf.remaining() < 2) break
                                val third = buf.get().toInt() and 0xFF
                                val color = buf.get().toInt() and 0xFF
                                val run = ((next and 0x3F) shl 8) or third
                                val argb = palette.entries[color]
                                repeat(run) { if (px < pixels.size) pixels[px++] = argb }
                            } else {
                                // Short colored run (0x80..0xBF): 6-bit length + 1 color byte.
                                if (!buf.hasRemaining()) break
                                val color = buf.get().toInt() and 0xFF
                                val run = next and 0x3F
                                val argb = palette.entries[color]
                                repeat(run) { if (px < pixels.size) pixels[px++] = argb }
                            }
                        }
                        (next and 0x40) != 0 -> {
                            // Extended transparent run (0x40..0x7F): 2-byte length.
                            if (!buf.hasRemaining()) break
                            val third = buf.get().toInt() and 0xFF
                            val run = ((next and 0x3F) shl 8) or third
                            repeat(run) { if (px < pixels.size) pixels[px++] = 0 }
                        }
                        else -> {
                            // Short transparent run (0x01..0x3F): 6-bit length.
                            repeat(next) { if (px < pixels.size) pixels[px++] = 0 }
                        }
                    }
                }
            }
        }
        return runCatching {
            Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        }.getOrNull()
    }

    private fun composite(
        width: Int,
        height: Int,
        comps: List<CompositionObject>,
        objects: Map<Int, Bitmap>,
    ): Bitmap {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        val canvasBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val g = android.graphics.Canvas(canvasBmp)
        comps.forEach { comp ->
            objects[comp.objectId]?.let { bmp ->
                g.drawBitmap(bmp, comp.x.toFloat(), comp.y.toFloat(), null)
            }
        }
        return canvasBmp
    }

    private fun ycbcrToArgb(y: Int, cb: Int, cr: Int, t: Int): Int {
        val yy = (y - 16).coerceAtLeast(0)
        val cbb = cb - 128
        val crr = cr - 128
        val r = (yy * 298 + crr * 409 + 128) shr 8
        val g = (yy * 298 - cbb * 100 - crr * 208 + 128) shr 8
        val b = (yy * 298 + cbb * 516 + 128) shr 8
        val a = t
        return (a.coerceIn(0, 255) shl 24) or
            (r.coerceIn(0, 255) shl 16) or
            (g.coerceIn(0, 255) shl 8) or
            b.coerceIn(0, 255)
    }

    private class BorrowedOutputBuffer(
        private val onRelease: (SubtitleOutputBuffer) -> Unit,
    ) : SubtitleOutputBuffer() {
        override fun release() {
            runCatching { onRelease(this) }
        }
    }
}

/** Single composited PGS subtitle frame. */
data class PgsFrame(
    val bitmap: Bitmap,
    val x: Int,
    val y: Int,
    val timeUs: Long,
    val durationUs: Long,
)

/**
 * {@link androidx.media3.extractor.text.Subtitle} payload produced by
 * {@link PgsSubtitleDecoder}. {@link PgsSubtitleOverlay} calls {@link #framesAt} to
 * locate bitmaps for the current playback position.
 */
class PgsSubtitle(
    internal val width: Int,
    internal val height: Int,
    internal val frames: List<PgsFrame>,
    private val basePtsUs: Long = 0L,
) : CinePilotSubtitle {
    override fun getEventTimeCount(): Int = frames.size
    override fun getEventTime(index: Int): Long = frames[index].timeUs + basePtsUs

    override fun getNextEventTimeIndex(timeUs: Long): Int {
        for (i in frames.indices) {
            if (frames[i].timeUs + basePtsUs > timeUs) return i
        }
        return C.INDEX_UNSET
    }

    override fun getCues(timeUs: Long): MutableList<androidx.media3.common.text.Cue> {
        // Side-channel push: PGS bitmap payload is published here so
        // PgsSubtitleOverlay can pick it up on every render tick; the standard
        // text SubtitleView gets an empty list (bitmap-only subtitles).
        SubtitleSideChannel.publish(this)
        return mutableListOf()
    }

    fun framesAt(positionUs: Long): List<PgsFrame> {
        return frames.filter { frame ->
            val start = frame.timeUs + basePtsUs
            positionUs >= start &&
                (frame.durationUs == C.TIME_UNSET || positionUs < start + frame.durationUs)
        }
    }
}

class PgsSubtitleException(message: String) : SubtitleDecoderException(message)
