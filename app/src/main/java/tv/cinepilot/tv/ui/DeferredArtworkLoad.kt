package tv.cinepilot.tv.ui

import android.widget.ImageView
import java.util.concurrent.atomic.AtomicInteger

private val deferredArtworkSequence = AtomicInteger()

fun ImageView.deferArtworkLoad(load: () -> Unit) {
    val delayMs = (deferredArtworkSequence.getAndIncrement() % 48) * 12L
    postDelayed({
        if (isAttachedToWindow) {
            load()
        }
    }, delayMs)
}
