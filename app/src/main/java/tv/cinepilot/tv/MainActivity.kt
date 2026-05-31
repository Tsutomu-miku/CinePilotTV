package tv.cinepilot.tv

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.Executors
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.PlaybackSelectionPreferences
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.tv.runtime.CinePilotRuntime

class MainActivity : Activity() {
    private lateinit var runtime: CinePilotRuntime
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runtime = CinePilotRuntime.create(this)
        showServerEntry()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun showServerEntry() {
        val serverInput = input("https://your-server.example.com")
        setContentView(screen("CinePilot TV") {
            addView(label("服务器地址"))
            addView(serverInput)
            addView(action("连接服务器") {
                runTask("正在连接服务器...", {
                    runtime.workflowController.submitServer(serverInput.text.toString())
                }) {
                    showLogin()
                }
            })
        })
    }

    private fun showLogin() {
        val usernameInput = input("用户名")
        val passwordInput = input("密码")
        setContentView(screen("登录 ${runtime.workflowController.state().server()?.serverName() ?: ""}") {
            addView(label("用户名"))
            addView(usernameInput)
            addView(label("密码"))
            addView(passwordInput)
            addView(action("登录") {
                runTask("正在登录并加载首页...", {
                    runtime.workflowController.login(
                        usernameInput.text.toString(),
                        passwordInput.text.toString(),
                    )
                }) {
                    showHome(runtime.workflowController.state())
                }
            })
            addView(action("返回服务器输入") { showServerEntry() })
        })
    }

    private fun showHome(state: TvAppState) {
        setContentView(screen("首页") {
            if (state.homeRows().isEmpty()) {
                addView(label("没有可显示的媒体"))
            }
            state.homeRows().forEach { row ->
                addView(section(row.title()))
                row.items().forEach { item ->
                    addView(itemButton(row, item))
                }
            }
            addView(action("重新加载首页") {
                runTask("正在重新加载首页...", {
                    runtime.workflowController.loadHome()
                }) {
                    showHome(runtime.workflowController.state())
                }
            })
        })
    }

    private fun showDetails(item: MediaItemSummary) {
        setContentView(screen(item.name()) {
            addView(label("${item.type()}${if (item.productionYear() != null) " · ${item.productionYear()}" else ""}"))
            if (item.hasResumePosition()) {
                addView(label("可从 ${item.userData().playbackPositionTicks()} ticks 继续播放"))
            }
            addView(action("播放") {
                runTask("正在准备播放...", {
                    runtime.workflowController.preparePlayback(PlaybackSelectionPreferences.defaults())
                }) {
                    showPlayerReady(runtime.workflowController.state())
                }
            })
            addView(action("返回首页") {
                runtime.workflowController.back()
                showHome(runtime.workflowController.state())
            })
        })
    }

    private fun showPlayerReady(state: TvAppState) {
        val playable = state.playableMedia()
        setContentView(screen("准备播放") {
            addView(label("播放方式：${playable?.playMethod() ?: ""}"))
            addView(label("媒体源：${playable?.mediaSourceId() ?: ""}"))
            addView(label(playable?.url() ?: playable?.request()?.path() ?: ""))
            addView(action("返回详情") {
                runtime.workflowController.back()
                runtime.workflowController.state().selectedItem()?.let(::showDetails)
            })
        })
    }

    private fun itemButton(row: HomeRow, item: MediaItemSummary): View {
        return action(item.name().ifBlank { item.id() }) {
            runtime.workflowController.openItem(item.id())
            runtime.workflowController.state().selectedItem()?.let(::showDetails)
        }.also {
            it.contentDescription = "${row.title()} ${item.name()}"
        }
    }

    private fun showLoading(message: String) {
        setContentView(screen("CinePilot TV") {
            addView(label(message))
        })
    }

    private fun showError(error: Throwable) {
        setContentView(screen("出错了") {
            addView(label(error.message ?: error::class.java.simpleName))
            addView(action("返回服务器输入") { showServerEntry() })
        })
    }

    private fun runTask(message: String, task: () -> Unit, onSuccess: () -> Unit) {
        showLoading(message)
        executor.execute {
            try {
                task()
                runOnUiThread { onSuccess() }
            } catch (error: Throwable) {
                runOnUiThread { showError(error) }
            }
        }
    }

    private fun screen(title: String, content: LinearLayout.() -> Unit): ScrollView {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 40, 48, 40)
            setBackgroundColor(Color.rgb(11, 16, 32))
        }
        container.addView(TextView(this).apply {
            text = title
            textSize = 32f
            setTextColor(Color.WHITE)
            gravity = Gravity.START
            setPadding(0, 0, 0, 24)
        })
        container.content()
        return ScrollView(this).apply { addView(container) }
    }

    private fun input(hintText: String): EditText {
        return EditText(this).apply {
            hint = hintText
            textSize = 20f
            singleLine = true
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setPadding(20, 12, 20, 12)
        }
    }

    private fun action(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            textSize = 20f
            isAllCaps = false
            setOnClickListener { onClick() }
            setPadding(20, 14, 20, 14)
        }
    }

    private fun label(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 20f
            setTextColor(Color.WHITE)
            setPadding(0, 10, 0, 10)
        }
    }

    private fun section(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 24f
            setTextColor(Color.rgb(45, 212, 191))
            setPadding(0, 28, 0, 8)
        }
    }
}
