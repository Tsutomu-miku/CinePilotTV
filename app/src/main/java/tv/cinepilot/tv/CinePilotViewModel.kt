package tv.cinepilot.tv

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import tv.cinepilot.tv.runtime.CinePilotRuntime

class CinePilotViewModel private constructor(context: Context) : ViewModel() {
    val runtime: CinePilotRuntime = CinePilotRuntime.create(context.applicationContext)
    val workflowController = runtime.workflowController
    val mediaBrowserClient = runtime.mediaBrowserClient
    val deviceCodecDiagnostics = runtime.deviceCodecDiagnostics

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(CinePilotViewModel::class.java)) {
                        return CinePilotViewModel(appContext) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
