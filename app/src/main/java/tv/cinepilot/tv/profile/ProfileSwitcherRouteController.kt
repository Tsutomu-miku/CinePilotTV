package tv.cinepilot.tv.profile

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import tv.cinepilot.core.protocol.ProfileSummary
import tv.cinepilot.core.protocol.ServerIdentity
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvWorkflowController
import tv.cinepilot.tv.compose.screens.ComposeProfileSwitcherScreen
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.home.HomeSettingsStore
import tv.cinepilot.tv.runtime.ArtworkRequestFactory

class ProfileSwitcherRouteController(
    private val activity: ComponentActivity,
    private val workflowController: TvWorkflowController,
    private val homeSettingsStore: HomeSettingsStore,
    private val artworkFactory: ArtworkRequestFactory,
    private val runTask: (String, () -> Unit, () -> Unit) -> Unit,
    private val runSilentTask: (() -> Unit, () -> Unit, (Throwable) -> Unit) -> Unit,
    private val showHome: (TvAppState) -> Unit,
    private val showServerEntry: () -> Unit,
    private val renderCompose: (String, @Composable (CinePilotPalette) -> Unit) -> Unit,
) {
    private var visible = false
    private var returnState: TvAppState? = null

    fun show(state: TvAppState) {
        returnState = state
        visible = true
        val server = state.server()
        var profiles: List<ProfileSummary> = emptyList()
        renderProfiles(server = server, profiles = profiles, isLoading = true)
        runSilentTask({
            profiles = workflowController.profiles()
        }, {
            // Guard against stale callbacks: if the user dismissed the switcher
            // while the profiles request was in flight, don't re-open it.
            if (visible) renderProfiles(server = server, profiles = profiles, isLoading = false)
        }, {
            if (visible) renderProfiles(server = server, profiles = emptyList(), isLoading = false)
        })
    }

    fun hide() {
        visible = false
    }

    fun closeIfVisible(): Boolean {
        if (!visible) return false
        close()
        return true
    }

    private fun renderProfiles(
        server: ServerIdentity?,
        profiles: List<ProfileSummary>,
        isLoading: Boolean,
    ) {
        renderCompose("切换用户") { palette ->
            ComposeProfileSwitcherScreen(
                palette = palette,
                owner = activity,
                artworkFactory = artworkFactory,
                server = server,
                profiles = profiles,
                isLoading = isLoading,
                onSwitch = ::switchTo,
                onRemove = ::removeProfile,
                onClose = ::close,
            )
        }
    }

    private fun switchTo(userId: String) {
        runTask("正在切换用户...", {
            val includeSmartCollections = homeSettingsStore.load().showSmartCollections
            workflowController.switchProfile(userId, includeSmartCollections)
        }, {
            visible = false
            showHome(workflowController.state())
        })
    }

    private fun removeProfile(userId: String) {
        val wasActive = workflowController.state().authenticated()?.session()?.userId() == userId
        workflowController.removeProfile(userId)
        if (wasActive) {
            visible = false
            showServerEntry()
        } else {
            // Re-render list without the removed profile.
            returnState?.let(::show)
        }
    }

    private fun close() {
        visible = false
        val state = returnState
        if (state != null) showHome(state) else showServerEntry()
    }
}
