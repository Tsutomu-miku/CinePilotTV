package tv.cinepilot.tv.profile

import android.widget.ImageView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.ProfileSummary
import tv.cinepilot.core.protocol.ServerIdentity
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvWorkflowController
import tv.cinepilot.tv.home.HomeSettingsStore
import tv.cinepilot.tv.runtime.PrimaryImageLoader

class ProfileSwitcherRouteController(
    private val activity: ComponentActivity,
    private val workflowController: TvWorkflowController,
    private val homeSettingsStore: HomeSettingsStore,
    private val imageLoader: PrimaryImageLoader,
    private val runTask: (String, () -> Unit, () -> Unit) -> Unit,
    private val showHome: (TvAppState) -> Unit,
    private val showServerEntry: () -> Unit,
) {
    private var visible = false
    private var returnState: TvAppState? = null

    fun show(state: TvAppState) {
        returnState = state
        visible = true
        val server = state.server()
        var profiles: List<ProfileSummary> = emptyList()
        runTask("正在加载账号列表...", {
            profiles = workflowController.profiles()
        }) {
            activity.setContentView(activity.profileSwitcherScreen(
                profiles = profiles,
                loadImage = { target, profile, w, h -> loadProfileAvatar(server, profile, target, w, h) },
                onSwitch = ::switchTo,
                onRemove = ::removeProfile,
                onClose = ::close,
            ))
        }
    }

    fun hide() {
        visible = false
    }

    fun closeIfVisible(): Boolean {
        if (!visible) return false
        close()
        return true
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

    private fun loadProfileAvatar(
        server: ServerIdentity?,
        profile: ProfileSummary,
        target: ImageView,
        width: Int,
        height: Int,
    ) {
        if (server == null) return
        val stub = tv.cinepilot.core.protocol.PublicUserSummary(
            profile.userId(),
            profile.userName(),
            true,
            profile.userImageTag(),
        )
        imageLoader.loadPublicUser(activity, server, target, stub, width, height)
    }
}
