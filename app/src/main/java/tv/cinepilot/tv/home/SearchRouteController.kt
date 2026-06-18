package tv.cinepilot.tv.home

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import tv.cinepilot.core.tv.SearchFilter
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvWorkflowController
import tv.cinepilot.tv.compose.screens.ComposeSearchScreen
import tv.cinepilot.tv.compose.theme.CinePilotPalette

class SearchRouteController(
    private val activity: ComponentActivity,
    private val workflowController: TvWorkflowController,
    private val runTask: (String, () -> Unit, () -> Unit) -> Unit,
    private val showHome: (TvAppState) -> Unit,
    private val renderView: (View) -> Unit,
    private val renderCompose: (String, @Composable (CinePilotPalette) -> Unit) -> Unit,
) {
    private var visible = false
    private var searchFilter = SearchFilter.ALL
    private var pendingVoiceSearchTerm = ""
    private val voiceSearchLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val spokenTerm = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
        if (result.resultCode == Activity.RESULT_OK && spokenTerm.isNotBlank()) {
            submitSearchTerm(spokenTerm, searchFilter)
        } else {
            showSearch(pendingVoiceSearchTerm)
        }
    }

    fun showSearch(initialTerm: String = "") {
        visible = true
        renderCompose("搜索") { palette ->
            ComposeSearchScreen(
                palette = palette,
                initialTerm = initialTerm,
                selectedFilter = searchFilter,
                onFilter = { filter, currentTerm ->
                    searchFilter = filter
                    showSearch(currentTerm)
                },
                onVoiceInput = ::startVoiceSearch,
                onSubmit = { term, filter ->
                    if (term.isBlank()) {
                        showSearch(term)
                    } else {
                        submitSearchTerm(term, filter)
                    }
                },
            )
        }
    }

    fun closeIfVisible(): Boolean {
        if (!visible) {
            return false
        }
        visible = false
        showHome(workflowController.state())
        return true
    }

    fun hide() {
        visible = false
    }

    private fun submitSearchTerm(term: String, filter: SearchFilter) {
        visible = false
        runTask("正在搜索...", {
            workflowController.search(term, filter)
        }) {
            showHome(workflowController.state())
        }
    }

    private fun startVoiceSearch(currentTerm: String) {
        pendingVoiceSearchTerm = currentTerm
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "说出要搜索的媒体")
        }
        try {
            voiceSearchLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(activity, "当前设备没有可用的语音输入", Toast.LENGTH_SHORT).show()
            showSearch(currentTerm)
        }
    }
}
