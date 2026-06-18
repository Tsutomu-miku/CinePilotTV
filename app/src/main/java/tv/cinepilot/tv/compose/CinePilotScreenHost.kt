package tv.cinepilot.tv.compose

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.flow.StateFlow
import tv.cinepilot.tv.compose.components.ImmersiveStage
import tv.cinepilot.tv.compose.theme.CinePilotComposeTheme
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText
import tv.cinepilot.tv.settings.AppTheme

class CinePilotScreenHost(
    private val activity: ComponentActivity,
    private val themeFlow: StateFlow<AppTheme>,
) {
    private var screen by mutableStateOf<CinePilotScreen>(CinePilotScreen.Loading("正在启动..."))
    private var legacyKey by mutableIntStateOf(0)
    private var busyMessage by mutableStateOf<String?>(null)
    private val busyTokens = LinkedHashMap<Any, String>()

    fun install() {
        activity.setContent {
            val theme by themeFlow.collectAsState()
            CinePilotApp(
                screen = screen,
                theme = theme,
                busyMessage = busyMessage,
            )
        }
    }

    fun showLoading(message: String) {
        clearBusy()
        screen = CinePilotScreen.Loading(message)
    }

    fun showLegacy(view: View) {
        clearBusy()
        legacyKey += 1
        screen = CinePilotScreen.LegacyView(legacyKey, view)
    }

    fun showCompose(title: String, content: @Composable (CinePilotPalette) -> Unit) {
        clearBusy()
        screen = CinePilotScreen.ComposePage(title, content)
    }

    fun showFullScreen(content: @Composable (CinePilotPalette) -> Unit) {
        clearBusy()
        screen = CinePilotScreen.ComposeFullScreen(content)
    }

    fun beginBusy(message: String): () -> Unit {
        val token = Any()
        busyTokens[token] = message
        busyMessage = message
        return {
            busyTokens.remove(token)
            busyMessage = busyTokens.values.lastOrNull()
        }
    }

    fun clearBusy() {
        busyTokens.clear()
        busyMessage = null
    }
}

sealed interface CinePilotScreen {
    data class Loading(val message: String) : CinePilotScreen
    data class LegacyView(val key: Int, val view: View) : CinePilotScreen
    class ComposePage(
        val title: String,
        val content: @Composable (CinePilotPalette) -> Unit,
    ) : CinePilotScreen
    class ComposeFullScreen(
        val content: @Composable (CinePilotPalette) -> Unit,
    ) : CinePilotScreen

    data object ServerEntry : CinePilotScreen
    data object Login : CinePilotScreen
    data object QuickConnect : CinePilotScreen
    data object Home : CinePilotScreen
    data object Search : CinePilotScreen
    data object Details : CinePilotScreen
    data object Player : CinePilotScreen
    data object Settings : CinePilotScreen
    data object ProfileSwitcher : CinePilotScreen
    data object Error : CinePilotScreen
    data object Diagnostics : CinePilotScreen
}

@Composable
fun CinePilotApp(screen: CinePilotScreen, theme: AppTheme, busyMessage: String? = null) {
    val palette = CinePilotComposeTheme.palette(theme)
    Box(modifier = Modifier.fillMaxSize()) {
        when (screen) {
            is CinePilotScreen.Loading -> LoadingScreen(palette, screen.message)
            is CinePilotScreen.LegacyView -> LegacyViewScreen(screen)
            is CinePilotScreen.ComposePage -> ImmersiveStage(palette = palette, title = screen.title) {
                Box(modifier = Modifier.widthIn(max = TvDp.PanelWidth)) {
                    screen.content(palette)
                }
            }
            is CinePilotScreen.ComposeFullScreen -> screen.content(palette)
            else -> LoadingScreen(palette, "正在打开...")
        }
        if (busyMessage != null && screen !is CinePilotScreen.Loading) {
            BusyOverlay(palette = palette, message = busyMessage)
        }
    }
}

@Composable
private fun LoadingScreen(palette: CinePilotPalette, message: String) {
    ImmersiveStage(palette = palette, title = "CinePilot TV") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(TvDp.CellGap),
        ) {
            BasicText(
                text = message,
                style = TextStyle(
                    color = palette.textSecondary,
                    fontSize = TvText.Body,
                ),
            )
        }
    }
}

@Composable
private fun BusyOverlay(palette: CinePilotPalette, message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 40.dp, top = 28.dp, end = 40.dp, bottom = 28.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        BasicText(
            text = message,
            maxLines = 1,
            style = TextStyle(
                color = palette.textPrimary,
                fontSize = TvText.Body,
            ),
            modifier = Modifier
                .background(palette.glass.copy(alpha = 0.92f), RoundedCornerShape(TvDp.ControlRadius))
                .padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun LegacyViewScreen(screen: CinePilotScreen.LegacyView) {
    AndroidView(
        factory = { context ->
            FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = { host ->
            val view = screen.view
            (view.parent as? ViewGroup)?.removeView(view)
            host.removeAllViews()
            host.addView(view, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ))
        },
    )
}
