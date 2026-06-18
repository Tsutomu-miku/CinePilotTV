package tv.cinepilot.tv.playback

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.core.content.FileProvider
import java.io.File
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvDiagnostics
import tv.cinepilot.tv.compose.screens.ComposeDiagnosticsExportedScreen
import tv.cinepilot.tv.compose.screens.ComposeDiagnosticsScreen
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.runtime.DeviceCodecDiagnostics

class PlaybackDiagnosticsController(
    private val activity: ComponentActivity,
    private val deviceCodecDiagnostics: DeviceCodecDiagnostics,
    private val renderCompose: (String, @Composable (CinePilotPalette) -> Unit) -> Unit,
) {
    fun showFromError(state: TvAppState, onBackError: () -> Unit) {
        show(
            state = state,
            backLabel = "返回错误页",
            onBackDiagnosticsTarget = onBackError,
        )
    }

    private fun show(
        state: TvAppState,
        returnToPlayer: Boolean = false,
        backLabel: String? = null,
        onBackDiagnosticsTarget: () -> Unit,
    ) {
        val diagnostics = diagnosticsText(state)
        renderCompose("诊断信息") { palette ->
            ComposeDiagnosticsScreen(
                palette = palette,
                diagnostics = diagnostics,
                returnToPlayer = returnToPlayer,
                backLabel = backLabel,
                onExport = {
                    val file = writeDiagnostics(diagnostics)
                    showExported(state, file.absolutePath, returnToPlayer, backLabel, onBackDiagnosticsTarget)
                },
                onShare = { share(writeDiagnostics(diagnostics), diagnostics) },
                onBackDiagnosticsTarget = onBackDiagnosticsTarget,
            )
        }
    }

    private fun showExported(
        state: TvAppState,
        path: String,
        returnToPlayer: Boolean = false,
        backLabel: String? = null,
        onBackDiagnosticsTarget: () -> Unit,
    ) {
        val diagnostics = diagnosticsText(state)
        renderCompose("诊断信息") { palette ->
            ComposeDiagnosticsExportedScreen(
                palette = palette,
                path = path,
                returnToPlayer = returnToPlayer,
                backLabel = backLabel,
                onShare = { share(writeDiagnostics(diagnostics), diagnostics) },
                onBackDiagnostics = {
                    show(state, returnToPlayer, backLabel, onBackDiagnosticsTarget)
                },
                onBackDiagnosticsTarget = onBackDiagnosticsTarget,
            )
        }
    }

    private fun share(file: File, diagnostics: String) {
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "CinePilot TV 诊断信息")
            putExtra(Intent.EXTRA_TEXT, diagnostics)
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(activity.contentResolver, DIAGNOSTICS_FILE_NAME, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            activity.startActivity(Intent.createChooser(shareIntent, "分享诊断"))
        }.onFailure {
            Toast.makeText(activity, "没有可用的分享应用", Toast.LENGTH_SHORT).show()
        }
    }

    private fun writeDiagnostics(diagnostics: String): File {
        val directory = activity.filesDir.resolve(DIAGNOSTICS_DIR_NAME).apply { mkdirs() }
        return directory.resolve(DIAGNOSTICS_FILE_NAME).apply {
            writeText(diagnostics)
        }
    }

    private fun diagnosticsText(state: TvAppState): String {
        return TvDiagnostics.describe(state) + deviceCodecDiagnostics.describe()
    }

    private companion object {
        private const val DIAGNOSTICS_DIR_NAME = "diagnostics"
        private const val DIAGNOSTICS_FILE_NAME = "cinepilot-diagnostics.txt"
    }
}
