package tv.cinepilot.tv.details

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.tv.runtime.ArtworkTarget
import tv.cinepilot.tv.ui.HomeRowPresentation
import tv.cinepilot.tv.ui.InfuseAction
import tv.cinepilot.tv.ui.InfuseActionEmphasis
import tv.cinepilot.tv.ui.MediaWallType
import tv.cinepilot.tv.ui.RowVisualStyle
import tv.cinepilot.tv.ui.TvColors
import tv.cinepilot.tv.ui.TvIcon
import tv.cinepilot.tv.ui.detailsActions
import tv.cinepilot.tv.ui.detailsStage
import tv.cinepilot.tv.ui.dp
import tv.cinepilot.tv.ui.mediaWallRow

/**
 * Playlist detail screen — shows playlist name, action buttons, and the list
 * of items inside the playlist. Uses the same cinematic stage as regular detail
 * pages for visual consistency.
 */
fun ComponentActivity.playlistDetailScreen(
    playlist: MediaItemSummary,
    items: List<MediaItemSummary>,
    onPlayAll: () -> Unit = {},
    onDelete: () -> Unit = {},
    onOpenItem: (MediaItemSummary) -> Unit = {},
    onBack: () -> Unit = {},
    loadBackdrop: (ImageView, MediaItemSummary) -> Unit,
    loadArtwork: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
): View {
    val itemCount = items.size
    return detailsStage(playlist, loadBackdrop) {
        addView(playlistTitle(playlist.name().ifBlank { "播放列表" }))
        addView(playlistMetaRow(itemCount))
        addView(playlistActionsRow(
            onPlayAll = onPlayAll,
            onDelete = onDelete,
            onBack = onBack,
        ))
        if (items.isNotEmpty()) {
            addView(mediaWallRow(
                presentation = HomeRowPresentation(
                    row = HomeRow("playlist:items", "列表内容", items),
                    title = "列表内容",
                    visualStyle = RowVisualStyle.POSTER_RAIL,
                    wrapItems = false,
                ),
                onCell = { _, _ -> },
                onFocus = { _, _ -> },
                onOpen = { _, item -> onOpenItem(item) },
                loadArtwork = loadArtwork,
            ).apply {
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin = dp(16)
                }
            })
        }
    }
}

private fun ComponentActivity.playlistTitle(title: String): TextView {
    return TextView(this).apply {
        text = title
        textSize = MediaWallType.DetailTitle
        setTextColor(TvColors.TextPrimary)
        includeFontPadding = false
        maxLines = 3
        ellipsize = android.text.TextUtils.TruncateAt.END
        setLineSpacing(2f, 1.0f)
        setPadding(0, 0, 0, dp(8))
    }
}

private fun ComponentActivity.playlistMetaRow(itemCount: Int): TextView {
    val text = when {
        itemCount == 0 -> "空播放列表"
        itemCount == 1 -> "1 个项目"
        else -> "$itemCount 个项目"
    }
    return TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(TvColors.TextSecondary)
        includeFontPadding = false
        setPadding(0, dp(4), 0, dp(12))
    }
}

private fun ComponentActivity.playlistActionsRow(
    onPlayAll: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
): View {
    val actions = mutableListOf<InfuseAction>()
    actions.add(InfuseAction("播放全部", TvIcon.PLAY, InfuseActionEmphasis.PRIMARY, onPlayAll))
    actions.add(InfuseAction("删除播放列表", TvIcon.BACK, InfuseActionEmphasis.QUIET, onDelete))
    actions.add(InfuseAction("返回", TvIcon.BACK, InfuseActionEmphasis.QUIET, onBack))
    return detailsActions(actions)
}
