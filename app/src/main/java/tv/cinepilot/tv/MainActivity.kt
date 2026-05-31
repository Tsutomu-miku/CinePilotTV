package tv.cinepilot.tv

import android.app.Activity
import android.os.Bundle
import tv.cinepilot.tv.runtime.CinePilotRuntime

class MainActivity : Activity() {
    private lateinit var runtime: CinePilotRuntime

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runtime = CinePilotRuntime.create(this)
    }
}
