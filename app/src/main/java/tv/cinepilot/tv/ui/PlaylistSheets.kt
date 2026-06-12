package tv.cinepilot.tv.ui

import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary

/**
 * Side sheet for picking which playlist to add an item to.
 * Shows a vertical list of playlist names plus a "新建播放列表" entry.
 */
fun ComponentActivity.playlistPickerSheet(
    playlists: List<MediaItemSummary>,
    onPick: (MediaItemSummary) -> Unit,
    onCreateNew: () -> Unit,
    onClose: () -> Unit,
): View {
    return sideSheet {
        addView(infusePanelTitle("添加到播放列表"))
        addView(TextView(this@playlistPickerSheet).apply {
            text = "选择一个播放列表，或创建新的播放列表。"
            textSize = 12f
            setTextColor(TvColors.TextSecondary)
            setPadding(0, dp(4), 0, dp(14))
            setLineSpacing(2f, 1.05f)
        })
        playlists.forEach { playlist ->
            addView(playlistRow(playlist) { onPick(playlist) })
        }
        addView(playlistRow("新建播放列表…", true) { onCreateNew() })
        addView(detailsActions(listOf(
            InfuseAction("返回", TvIcon.BACK, InfuseActionEmphasis.SECONDARY, onClose),
        )))
    }
}

/**
 * Side sheet for naming a new playlist.
 * Has a single text input and "创建" / "取消" actions.
 */
fun ComponentActivity.createPlaylistSheet(
    initialName: String = "",
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
): View {
    val input = EditText(this).apply {
        setText(initialName)
        hint = "播放列表名称"
        setTextColor(TvColors.TextPrimary)
        setHintTextColor(TvColors.TextMuted)
        textSize = 16f
        inputType = InputType.TYPE_CLASS_TEXT
        setPadding(dp(16), dp(12), dp(16), dp(12))
        setBackgroundColor(TvColors.SurfaceControl)
        isFocusable = true
        isFocusableInTouchMode = true
        background = rounded(
            TvColors.SurfaceControl,
            dp(TvRadius.Control),
            dp(1),
            TvColors.FocusRing
        )
    }
    return sideSheet {
        addView(infusePanelTitle("新建播放列表"))
        addView(TextView(this@createPlaylistSheet).apply {
            text = "给新的播放列表起个名字。"
            textSize = 12f
            setTextColor(TvColors.TextSecondary)
            setPadding(0, dp(4), 0, dp(12))
        })
        addView(input.apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(16)
            }
        })
        addView(detailsActions(listOf(
            InfuseAction("取消", TvIcon.BACK, InfuseActionEmphasis.SECONDARY, onCancel),
            InfuseAction("创建", TvIcon.CHECK, InfuseActionEmphasis.PRIMARY) {
                val name = input.text?.toString()?.trim().orEmpty()
                if (name.isNotBlank()) onConfirm(name)
            },
        )))
    }
}

/** A single selectable row used in the playlist picker sheet. */
private fun ComponentActivity.playlistRow(
    playlist: MediaItemSummary,
    onClick: () -> Unit,
): View = playlistRow(
    name = playlist.name().ifBlank { "未命名播放列表" },
    isCreateNew = false,
    onClick = onClick,
)

/** Overload for simple text rows like "新建播放列表…" */
private fun ComponentActivity.playlistRow(
    name: String,
    isCreateNew: Boolean,
    onClick: () -> Unit,
): View {
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL or Gravity.START
        setPadding(dp(16), dp(12), dp(16), dp(12))
        isFocusable = true
        isClickable = true
        background = rounded(
            TvColors.SurfaceControl,
            dp(TvRadius.Control),
            dp(1),
            TvColors.FocusRing
        )
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            bottomMargin = dp(8)
        }
    }
    row.addView(TextView(this).apply {
        text = name
        textSize = 15f
        setTextColor(if (isCreateNew) TvColors.Accent else TvColors.TextPrimary)
        layoutParams = LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f,
        )
    })
    return row
}
