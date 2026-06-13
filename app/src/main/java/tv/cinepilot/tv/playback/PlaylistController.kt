package tv.cinepilot.tv.playback

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.PlaybackInfo
import tv.cinepilot.core.tv.ShowStructure
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvWorkflowController
import tv.cinepilot.tv.compose.screens.ComposeCreatePlaylistScreen
import tv.cinepilot.tv.compose.screens.ComposePlaylistDetailScreen
import tv.cinepilot.tv.compose.screens.ComposePlaylistPickerScreen
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.runtime.ArtworkRequestFactory

/**
 * Coordinates playlist UI flows: picker sheet, create dialog, and playlist detail page.
 *
 * Navigation into individual playlist items is delegated back to the caller via
 * [onOpenItem] so the main playback controller retains ownership of player lifecycle.
 *
 * The "Play All" action is also delegated via [onPlayAll] so the caller can start
 * playback from the top of the playlist. The caller receives the list of playable
 * items plus an [onBack] callback that returns to the playlist detail page, so it
 * can push the return action onto its own media back stack.
 */
class PlaylistController(
    private val activity: ComponentActivity,
    private val workflowController: TvWorkflowController,
    private val runTask: (String, () -> Unit, () -> Unit) -> Unit,
    private val showHome: (TvAppState) -> Unit,
    private val renderCompose: (String, @Composable (CinePilotPalette) -> Unit) -> Unit,
    private val artworkFactory: ArtworkRequestFactory,
    private val setAuxiliaryBackAction: (() -> Unit) -> Unit,
    private val onOpenItem: (MediaItemSummary, () -> Unit) -> Unit,
    private val onPlayAll: (List<MediaItemSummary>, () -> Unit) -> Unit,
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
            setAuxiliaryBackAction(onClose)
            renderCompose("播放列表") { palette ->
                ComposePlaylistPickerScreen(
                    palette = palette,
                    playlists = playlists,
                    onPick = { playlist ->
                        addItem(item, playlist, onClose)
                    },
                    onCreateNew = {
                        showCreateDialog(item, playbackInfo, episodeContext, onClose)
                    },
                    onClose = onClose,
                )
            }
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
        setAuxiliaryBackAction { showPicker(item, playbackInfo, episodeContext, onClose) }
        renderCompose("新建播放列表") { palette ->
            ComposeCreatePlaylistScreen(
                palette = palette,
                initialName = item.name(),
                onConfirm = { name ->
                    createAndAdd(name, item, onClose)
                },
                onCancel = {
                    showPicker(item, playbackInfo, episodeContext, onClose)
                },
            )
        }
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
        setAuxiliaryBackAction { showHome(workflowController.state()) }
        renderCompose("播放列表详情") { palette ->
            ComposePlaylistDetailScreen(
                palette = palette,
                playlist = playlist,
                items = items,
                artworkFactory = artworkFactory,
                authenticated = workflowController.state().authenticated(),
                onPlayAll = {
                    val playable = items.filter { it.playable() }
                    if (playable.isNotEmpty()) {
                        onPlayAll(playable) { showDetail(playlist, items) }
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
            )
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }
}
