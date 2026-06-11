package tv.cinepilot.tv.playback

import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.PlaybackInfo
import tv.cinepilot.core.tv.ShowStructure
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvWorkflowController
import tv.cinepilot.tv.details.playlistDetailScreen
import tv.cinepilot.tv.runtime.ArtworkTarget
import tv.cinepilot.tv.ui.createPlaylistSheet
import tv.cinepilot.tv.ui.playlistPickerSheet

/**
 * Coordinates playlist UI flows: picker sheet, create dialog, and playlist detail page.
 *
 * Navigation into individual playlist items is delegated back to the caller via
 * [onOpenItem] so the main playback controller retains ownership of player lifecycle.
 */
class PlaylistController(
    private val activity: ComponentActivity,
    private val workflowController: TvWorkflowController,
    private val runTask: (String, () -> Unit, () -> Unit) -> Unit,
    private val showHome: (TvAppState) -> Unit,
    private val loadBackdropImage: (ImageView, MediaItemSummary, Int, Int) -> Unit,
    private val loadArtworkImage: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
    private val setAuxiliaryBackAction: (() -> Unit) -> Unit,
    private val onOpenItem: (MediaItemSummary, () -> Unit) -> Unit,
) {

    /** Show the playlist picker for adding an item to a playlist. */
    fun showPicker(
        item: MediaItemSummary,
        playbackInfo: PlaybackInfo?,
        episodeContext: ShowStructure?,
        onClose: () -> Unit,
    ) {
        val auth = workflowController.state().authenticated() ?: return
        var playlists: List<MediaItemSummary> = emptyList()
        runTask("正在加载播放列表...", {
            playlists = workflowController.playlists(50).items()
        }) {
            val sheet = activity.playlistPickerSheet(
                playlists = playlists,
                onPick = { playlist ->
                    addItem(item, playlist, onClose)
                },
                onCreateNew = {
                    showCreateDialog(item, playbackInfo, episodeContext, onClose)
                },
                onClose = onClose,
            )
            setAuxiliaryBackAction(onClose)
            activity.setContentView(sheet)
        }
    }

    private fun addItem(
        item: MediaItemSummary,
        playlist: MediaItemSummary,
        onClose: () -> Unit,
    ) {
        val auth = workflowController.state().authenticated() ?: return
        runTask("正在添加到播放列表...", {
            workflowController.addToPlaylist(playlist.id(), listOf(item.id()))
        }) {
            toast("已添加到「${playlist.name()}」")
            onClose()
        }
    }

    private fun showCreateDialog(
        item: MediaItemSummary,
        playbackInfo: PlaybackInfo?,
        episodeContext: ShowStructure?,
        onClose: () -> Unit,
    ) {
        val sheet = activity.createPlaylistSheet(
            initialName = item.name(),
            onConfirm = { name ->
                createAndAdd(name, item, onClose)
            },
            onCancel = {
                showPicker(item, playbackInfo, episodeContext, onClose)
            },
        )
        setAuxiliaryBackAction { showPicker(item, playbackInfo, episodeContext, onClose) }
        activity.setContentView(sheet)
    }

    private fun createAndAdd(
        name: String,
        item: MediaItemSummary,
        onClose: () -> Unit,
    ) {
        val auth = workflowController.state().authenticated() ?: return
        var playlistId = ""
        runTask("正在创建播放列表...", {
            playlistId = workflowController.createPlaylist(name)
            if (playlistId.isNotBlank()) {
                workflowController.addToPlaylist(playlistId, listOf(item.id()))
            }
        }) {
            if (playlistId.isNotBlank()) {
                toast("已创建「$name」并添加")
            } else {
                toast("创建失败")
            }
            onClose()
        }
    }

    /** Open the playlist detail page. */
    fun openDetail(playlist: MediaItemSummary) {
        var items: List<MediaItemSummary> = emptyList()
        runTask("正在加载播放列表...", {
            items = workflowController.playlistItems(playlist.id(), 200).items()
        }) {
            showDetail(playlist, items)
        }
    }

    private fun showDetail(
        playlist: MediaItemSummary,
        items: List<MediaItemSummary>,
    ) {
        val sheet = activity.playlistDetailScreen(
            playlist = playlist,
            items = items,
            onPlayAll = {
                if (items.isNotEmpty()) {
                    // 连续播放功能待实现
                }
            },
            onDelete = {
                runTask("正在删除播放列表...", {
                    workflowController.deletePlaylist(playlist.id())
                }) {
                    toast("播放列表已删除")
                    showHome(workflowController.state())
                }
            },
            onOpenItem = { item ->
                onOpenItem(item) { showDetail(playlist, items) }
            },
            onBack = {
                showHome(workflowController.state())
            },
            loadBackdrop = { view, item -> loadBackdropImage(view, item, 1280, 720) },
            loadArtwork = loadArtworkImage,
        )
        setAuxiliaryBackAction({})
        activity.setContentView(sheet)
    }

    private fun toast(msg: String) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }
}
