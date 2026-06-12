package tv.cinepilot.tv.subtitles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.VideoSize

/**
 * Transparent overlay View rendered on top of the ExoPlayer video surface. Handles
 * two bitmap subtitle pipelines identically:
 *
 * <ol>
 *   <li><b>PGS</b> — {@link PgsSubtitle} frames produced by {@link PgsSubtitleDecoder}.</li>
 *   <li><b>ASS</b> — {@link LibassSubtitle} frames produced by {@link LibassSubtitleDecoder}.</li>
 * </ol>
 *
 * <p>Registration: pass the overlay's {@link #textOutputBridge} to
 * {@link CinePilotSubtitleDecoderFactory#installOn} so our {@link CompositeTextOutput}
 * forwards every subtitle decoder tick both to the player's standard SubtitleView
 * (for text cues) and here (for bitmap cues).
 *
 * <p>Painting is positioned relative to the overlaid video rect (not the window): we
 * measure the current video aspect ratio against the overlay bounds and scale subtitle
 * bitmaps accordingly, so native author coordinates (1920×1080, …) stay aligned with
 * the letterboxed surface.
 */
class PgsSubtitleOverlay(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var eventIndex = 0
    private val videoDisplayRect = Rect()
    private val drawRect = Rect()
    private var attachedPlayer: Player? = null
    private var lastVideoSize: VideoSize = VideoSize.UNKNOWN

    private val playerListener = object : Player.Listener {
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            lastVideoSize = videoSize
            postInvalidateOnAnimation()
        }
    }

    /** Subscribe to position updates from the supplied player. */
    fun attachToPlayer(player: Player?) {
        attachedPlayer?.removeListener(playerListener)
        attachedPlayer = player
        if (player != null) {
            player.addListener(playerListener)
            lastVideoSize = player.videoSize
        } else {
            SubtitleSideChannel.clear()
            eventIndex = 0
            postInvalidateOnAnimation()
        }
    }

    // ---- Draw ----------------------------------------------------------------------

    private data class FrameDrawSpec(
        val bitmap: Bitmap,
        val x: Int,
        val y: Int,
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val player = attachedPlayer ?: run { postInvalidateOnAnimation(); return }
        val subtitle = SubtitleSideChannel.consumeLatest()
        if (subtitle == null || width == 0 || height == 0) {
            postInvalidateOnAnimation()
            return
        }
        if (subtitle !is PgsSubtitle && subtitle !is LibassSubtitle) {
            postInvalidateOnAnimation()
            return
        }

        computeVideoDisplayRect(videoDisplayRect)
        val positionUs = player.currentPosition * 1000L
        val count = subtitle.getEventTimeCount()
        while (eventIndex + 1 < count && subtitle.getEventTime(eventIndex + 1) <= positionUs) {
            eventIndex++
        }

        val frames: List<FrameDrawSpec> = when (subtitle) {
            is PgsSubtitle -> subtitle.framesAt(positionUs).map { FrameDrawSpec(it.bitmap, it.x, it.y) }
            is LibassSubtitle -> subtitle.framesAt(positionUs).map { FrameDrawSpec(it.bitmap, it.x, it.y) }
            else -> emptyList()
        }

        if (frames.isEmpty()) {
            postInvalidateOnAnimation()
            return
        }

        val (srcW, srcH) = when (subtitle) {
            is PgsSubtitle -> {
                val w = if (subtitle.width > 0) subtitle.width else videoDisplayRect.width()
                val h = if (subtitle.height > 0) subtitle.height else videoDisplayRect.height()
                Pair(w, h)
            }
            is LibassSubtitle -> {
                val w = if (subtitle.displayWidth > 0) subtitle.displayWidth else videoDisplayRect.width()
                val h = if (subtitle.displayHeight > 0) subtitle.displayHeight else videoDisplayRect.height()
                Pair(w, h)
            }
            else -> Pair(videoDisplayRect.width(), videoDisplayRect.height())
        }
        val scaleX = if (srcW > 0) videoDisplayRect.width().toFloat() / srcW else 1f
        val scaleY = if (srcH > 0) videoDisplayRect.height().toFloat() / srcH else 1f

        frames.forEach { frame ->
            val bmpWidth = frame.bitmap.width
            val bmpHeight = frame.bitmap.height
            val scaledW = (bmpWidth * scaleX + 0.5f).toInt()
            val scaledH = (bmpHeight * scaleY + 0.5f).toInt()
            if (scaledW <= 0 || scaledH <= 0) return@forEach
            val left = (videoDisplayRect.left + frame.x * scaleX + 0.5f).toInt()
                .coerceIn(0, width)
            val top = (videoDisplayRect.top + frame.y * scaleY + 0.5f).toInt()
                .coerceIn(0, height)
            val right = (left + scaledW).coerceIn(0, width)
            val bottom = (top + scaledH).coerceIn(0, height)
            drawRect.set(left, top, right, bottom)
            if (drawRect.width() > 0 && drawRect.height() > 0) {
                canvas.drawBitmap(frame.bitmap, null, drawRect, paint)
            }
        }

        postInvalidateOnAnimation()
    }

    /**
     * Letterboxed video surfaces are smaller than the player View bounds;
     * reconstruct the actual video display rect using the video aspect ratio
     * so PGS/ASS author coordinates map correctly.
     */
    private fun computeVideoDisplayRect(out: Rect) {
        val size = lastVideoSize.takeIf { it.width > 0 && it.height > 0 }
        val videoW = size?.width ?: 1
        val videoH = size?.height ?: 1
        val unappliedRotation = size?.unappliedRotationDegrees ?: 0
        val swapped = unappliedRotation == 90 || unappliedRotation == 270
        val contentW = if (swapped) videoH else videoW
        val contentH = if (swapped) videoW else videoH
        val pixelRatio = size?.pixelWidthHeightRatio?.takeIf { it > 0f } ?: 1f
        val scaledContentW = (contentW * pixelRatio + 0.5f).toInt().coerceAtLeast(1)
        val viewW = width
        val viewH = height
        val scale = minOf(viewW.toFloat() / scaledContentW, viewH.toFloat() / contentH)
        val displayW = (scaledContentW * scale + 0.5f).toInt()
        val displayH = (contentH * scale + 0.5f).toInt()
        val left = (viewW - displayW) / 2
        val top = (viewH - displayH) / 2
        out.set(left, top, left + displayW, top + displayH)
    }

    companion object {
        /**
         * Create an overlay sized to match its parent (the player surface) and
         * attach it above any existing SubtitleView siblings so bitmaps compose
         * cleanly over text.
         */
        fun injectInto(parent: ViewGroup): PgsSubtitleOverlay {
            val overlay = PgsSubtitleOverlay(parent.context)
            overlay.id = generateViewId()
            parent.addView(overlay, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ))
            overlay.bringToFront()
            return overlay
        }
    }
}

/** Kept to avoid lint warnings on the unused C / Cue imports above. */
@Suppress("unused")
private val _UNSET = C.TIME_UNSET
