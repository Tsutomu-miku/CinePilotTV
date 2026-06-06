package tv.cinepilot.tv.player

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.media3.ui.PlayerView
import androidx.media3.ui.R as Media3UiR

internal class RemotePlayerView(
    context: Context,
    private val onSeekBack: () -> Unit,
    private val onSeekForward: () -> Unit,
    private val onTogglePlayPause: () -> Boolean,
    private val onPlay: () -> Unit,
    private val onPause: () -> Unit,
    private val normalControllerTimeoutMs: Int,
) : PlayerView(context) {
    private var shortcutSeekMode = false
    private val feedbackView = TextView(context).apply {
        setTextColor(Color.WHITE)
        textSize = 18f
        gravity = Gravity.CENTER
        setPadding(dp(14), dp(7), dp(14), dp(7))
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(999).toFloat()
            setColor(Color.argb(180, 10, 14, 20))
        }
        visibility = View.GONE
        isFocusable = false
        isClickable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private val hideFeedback = Runnable {
        feedbackView.animate()
            .alpha(0f)
            .setDuration(FEEDBACK_FADE_MS)
            .withEndAction { feedbackView.visibility = View.GONE }
            .start()
    }
    private val clearShortcutSeekMode = Runnable {
        shortcutSeekMode = false
    }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        addView(feedbackView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
        ).apply {
            bottomMargin = dp(154)
        })
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP && consumesRemoteKey(event.keyCode)) {
            return true
        }
        if (event.action != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event)
        }
        return when (event.keyCode) {
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_SETTINGS,
            KeyEvent.KEYCODE_CAPTIONS -> {
                shortcutSeekMode = false
                removeCallbacks(clearShortcutSeekMode)
                showController()
                true
            }
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN -> handleControllerNavigation(event)
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                seekBackWithFeedback()
                true
            }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                seekForwardWithFeedback()
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (isControllerFullyVisible && !shortcutSeekMode) {
                    super.dispatchKeyEvent(event)
                } else {
                    seekBackWithFeedback()
                    true
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (isControllerFullyVisible && !shortcutSeekMode) {
                    super.dispatchKeyEvent(event)
                } else {
                    seekForwardWithFeedback()
                    true
                }
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> handleConfirmKey(event)
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                onPlay()
                enterPlayingMode()
                true
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                onPause()
                enterPausedControlMode()
                true
            }
            else -> super.dispatchKeyEvent(event)
        }
    }

    fun hideInlineTransportButtons() {
        post {
            transportButtonIds.forEach { viewId ->
                findViewById<View>(viewId)?.visibility = View.GONE
            }
            playPauseButtonIds.forEach { viewId ->
                findViewById<View>(viewId)?.setOnClickListener {
                    togglePlayPauseFromControl()
                }
            }
        }
    }

    private fun handleControllerNavigation(event: KeyEvent): Boolean {
        if (isControllerFullyVisible && controllerChildHasFocus()) {
            return super.dispatchKeyEvent(event)
        }
        shortcutSeekMode = false
        removeCallbacks(clearShortcutSeekMode)
        showController()
        requestPlayPauseFocus()
        return true
    }

    private fun handleConfirmKey(event: KeyEvent): Boolean {
        if (isControllerFullyVisible && controllerChildHasFocus()) {
            if (playPauseButtonHasFocus()) {
                if (event.repeatCount == 0) {
                    togglePlayPauseFromControl()
                }
                return true
            }
            return super.dispatchKeyEvent(event)
        }
        if (event.repeatCount == 0) {
            val isPlaying = onTogglePlayPause()
            if (isPlaying) {
                enterPlayingMode()
            } else {
                enterPausedControlMode()
            }
        }
        return true
    }

    private fun togglePlayPauseFromControl() {
        val isPlaying = onTogglePlayPause()
        if (isPlaying) {
            enterPlayingMode()
        } else {
            enterPausedControlMode()
        }
    }

    private fun seekBackWithFeedback() {
        onSeekBack()
        showSeekFeedback("-30 秒")
    }

    private fun seekForwardWithFeedback() {
        onSeekForward()
        showSeekFeedback("+30 秒")
    }

    private fun showSeekFeedback(message: String) {
        shortcutSeekMode = true
        removeCallbacks(clearShortcutSeekMode)
        postDelayed(clearShortcutSeekMode, SHORTCUT_SEEK_MODE_MS)
        showController()
        showTextFeedback(message)
    }

    private fun showTextFeedback(message: String) {
        removeCallbacks(hideFeedback)
        feedbackView.animate().cancel()
        feedbackView.text = message
        feedbackView.alpha = 1f
        feedbackView.visibility = View.VISIBLE
        postDelayed(hideFeedback, FEEDBACK_VISIBLE_MS)
    }

    private fun enterPausedControlMode() {
        shortcutSeekMode = false
        removeCallbacks(clearShortcutSeekMode)
        setControllerShowTimeoutMs(0)
        showController()
        requestPlayPauseFocus()
    }

    private fun enterPlayingMode() {
        shortcutSeekMode = false
        removeCallbacks(clearShortcutSeekMode)
        setControllerShowTimeoutMs(normalControllerTimeoutMs)
        hideController()
        requestFocus()
    }

    private fun requestPlayPauseFocus() {
        post {
            playPauseButton()?.requestFocus()
        }
        postDelayed({
            playPauseButton()?.requestFocus()
        }, PLAY_PAUSE_FOCUS_RETRY_MS)
    }

    private fun playPauseButtonHasFocus(): Boolean {
        val focused = findFocus()
        return playPauseButtonIds.any { viewId -> findViewById<View>(viewId) === focused }
    }

    private fun playPauseButton(): View? {
        return playPauseButtonIds.firstNotNullOfOrNull { viewId ->
            findViewById<View>(viewId)?.takeIf { it.isShown && it.isFocusable }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    private fun consumesRemoteKey(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_SETTINGS,
            KeyEvent.KEYCODE_CAPTIONS,
            KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE -> true
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN -> !isControllerFullyVisible || !controllerChildHasFocus()
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> !controllerChildHasFocus()
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> !isControllerFullyVisible || shortcutSeekMode
            else -> false
        }
    }

    private fun controllerChildHasFocus(): Boolean {
        val focused = findFocus()
        return focused != null && focused !== this && focused.isShown
    }

    private companion object {
        private const val FEEDBACK_VISIBLE_MS = 520L
        private const val FEEDBACK_FADE_MS = 150L
        private const val SHORTCUT_SEEK_MODE_MS = 1_500L
        private const val PLAY_PAUSE_FOCUS_RETRY_MS = 80L
        private val playPauseButtonIds = listOf(
            Media3UiR.id.exo_play_pause,
            Media3UiR.id.exo_play,
            Media3UiR.id.exo_pause,
        )
        private val transportButtonIds = listOf(
            Media3UiR.id.exo_rew,
            Media3UiR.id.exo_rew_with_amount,
            Media3UiR.id.exo_ffwd,
            Media3UiR.id.exo_ffwd_with_amount,
        )
    }
}
