package tv.cinepilot.tv.ui

import androidx.media3.common.PlaybackException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import tv.cinepilot.core.protocol.MediaBrowserException
import tv.cinepilot.core.tv.TvWorkflowController

fun tvErrorMessage(error: Throwable): String {
    if (error is MediaBrowserException && error.statusCode() > 0) {
        return when (error.statusCode()) {
            401 -> "会话已过期，请重新登录"
            403 -> "服务器拒绝访问，请确认账号权限"
            404 -> "服务器没有找到请求的资源，请确认服务器地址和媒体库是否正确"
            in 500..599 -> "服务器暂时无法处理请求，请稍后重试或检查 Jellyfin / Emby 服务状态"
            else -> "服务器请求失败，HTTP ${error.statusCode()}"
        }
    }
    val cause = rootCause(error)
    if (error is PlaybackException || cause is PlaybackException) {
        val playbackError = (error as? PlaybackException) ?: (cause as PlaybackException)
        return playbackErrorMessage(playbackError)
    }
    when (cause) {
        is UnknownHostException -> return "无法解析服务器地址，请检查主机名、端口或网络 DNS"
        is ConnectException -> return "无法连接到服务器，请确认地址、端口和 Jellyfin / Emby 服务已启动"
        is SocketTimeoutException -> return "连接服务器超时，请检查网络或稍后重试"
        is SSLException -> return "HTTPS 连接失败，请检查服务器证书；家庭服务器也可以先使用 http:// 地址测试"
    }
    return when (error.message) {
        "Server address is required" -> "请输入服务器地址"
        "Only HTTP and HTTPS server addresses are supported" -> "服务器地址只支持 http:// 或 https://"
        "Server address must include a host" -> "服务器地址需要包含主机名或 IP"
        TvWorkflowController.NO_CHILD_ITEM_MESSAGE -> "目录中没有可打开的媒体"
        TvWorkflowController.NO_PLAYABLE_SOURCE_MESSAGE -> "没有可用播放源"
        TvWorkflowController.QUICK_CONNECT_DISABLED_MESSAGE -> "服务器未启用 Quick Connect"
        TvWorkflowController.QUICK_CONNECT_NOT_APPROVED_MESSAGE -> "Quick Connect 还没有完成授权"
        else -> error.message ?: error::class.java.simpleName
    }
}

private fun playbackErrorMessage(error: PlaybackException): String {
    val codeName = error.errorCodeName
    val normalized = codeName.lowercase()
    val advice = when {
        normalized.contains("network") || normalized.contains("io_bad_http_status") -> {
            "网络或服务器响应异常，请检查电视网络、服务器地址、反向代理和媒体文件访问权限"
        }
        normalized.contains("timeout") -> {
            "连接或读取超时，请检查 Wi-Fi 稳定性，或尝试低码率播放"
        }
        normalized.contains("decoder") ||
            normalized.contains("decoding") ||
            normalized.contains("format_unsupported") ||
            normalized.contains("format_exceeds") -> {
            "当前设备可能不支持这个视频 / 音频编码，请尝试低码率播放，让服务器转码后再播放"
        }
        normalized.contains("drm") -> {
            "媒体可能包含 DRM 或受保护内容，当前版本无法播放受 DRM 保护的视频"
        }
        normalized.contains("cleartext") -> {
            "HTTP 明文播放被系统拦截，请确认应用允许本地 HTTP，或改用 HTTPS 服务器地址"
        }
        else -> {
            "请尝试低码率播放、切换音轨 / 字幕，或检查服务器转码设置"
        }
    }
    return "播放器无法打开媒体：$advice（$codeName）"
}

private fun rootCause(error: Throwable): Throwable {
    var current = error
    while (current.cause != null && current.cause !== current) {
        current = current.cause!!
    }
    return current
}
