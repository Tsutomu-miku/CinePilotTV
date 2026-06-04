package tv.cinepilot.tv.playback

import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvDiagnostics

class PlaybackDiagnosticsController(
    private val activity: ComponentActivity,
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
        val diagnostics = TvDiagnostics.describe(state)
        activity.setContentView(activity.diagnosticsScreen(
            diagnostics = diagnostics,
            returnToPlayer = returnToPlayer,
            backLabel = backLabel,
            onExport = {
                val file = activity.filesDir.resolve(DIAGNOSTICS_FILE_NAME)
                file.writeText(diagnostics)
                showExported(state, file.absolutePath, returnToPlayer, backLabel, onBackDiagnosticsTarget)
            },
            onShare = { share(diagnostics) },
            onBackDiagnosticsTarget = onBackDiagnosticsTarget,
        ))
    }

    private fun showExported(
        state: TvAppState,
        path: String,
        returnToPlayer: Boolean = false,
        backLabel: String? = null,
        onBackDiagnosticsTarget: () -> Unit,
    ) {
        val diagnostics = TvDiagnostics.describe(state)
        activity.setContentView(activity.diagnosticsExportedScreen(
            path = path,
            returnToPlayer = returnToPlayer,
            backLabel = backLabel,
            onShare = { share(diagnostics) },
            onBackDiagnostics = {
                show(state, returnToPlayer, backLabel, onBackDiagnosticsTarget)
            },
            onBackDiagnosticsTarget = onBackDiagnosticsTarget,
        ))
    }

    private fun share(diagnostics: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "CinePilot TV 诊断信息")
            putExtra(Intent.EXTRA_TEXT, diagnostics)
        }
        runCatching {
            activity.startActivity(Intent.createChooser(shareIntent, "分享诊断"))
        }.onFailure {
            Toast.makeText(activity, "没有可用的分享应用", Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        private const val DIAGNOSTICS_FILE_NAME = "cinepilot-diagnostics.txt"
    }
}
