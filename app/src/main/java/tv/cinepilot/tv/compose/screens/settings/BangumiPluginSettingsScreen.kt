package tv.cinepilot.tv.compose.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tv.cinepilot.plugin.spi.PluginStatus
import tv.cinepilot.tv.compose.components.buttons.TvActionButton
import tv.cinepilot.tv.compose.components.input.TvTextField
import tv.cinepilot.tv.compose.components.layout.SettingsGrid
import tv.cinepilot.tv.compose.components.settings.TvOptionRow
import tv.cinepilot.tv.plugin.PluginHost
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText

private val SuccessGreen = Color(0xFF22C55E)
private val WarnAmber = Color(0xFFF59E0B)
private val ErrorRed = Color(0xFFEF4444)
private val MutedBlue = Color(0xFF93C5FD)

/**
 * Builds the list of rows for the "插件管理" settings section.
 *
 * @param onOpenPlugin sub-routing callback the host uses to open the plugin
 *     detail screen (auth form, status, error, disconnect)
 */
@Composable
fun PluginSettingsSection(
    palette: CinePilotPalette,
    pluginHost: PluginHost,
    onOpenPlugin: (PluginHost.PluginInfo) -> Unit,
) {
    // Do NOT remember { listPlugins() } here: plugin status can change while
    // the settings screen stays open (e.g. after verify/retry), and we must
    // pick up the fresh value on every recomposition.
    val plugins = pluginHost.listPlugins()
    if (plugins.isEmpty()) return
    SettingsGrid(
        palette = palette,
        title = "插件管理",
        columns = 1,
        rows = plugins.map { info ->
            {
                TvOptionRow(
                    palette = palette,
                    label = info.descriptor.name(),
                    value = pluginStatusLabel(info.status),
                    description = info.descriptor.description(),
                    selected = info.status == PluginStatus.READY,
                    onClick = { onOpenPlugin(info) },
                )
            }
        },
    )
}

private fun pluginStatusLabel(status: PluginStatus): String = when (status) {
    PluginStatus.READY -> "已连接"
    PluginStatus.AUTH_REQUIRED -> "需授权"
    PluginStatus.DISABLED -> "已禁用"
    PluginStatus.TEMPORARILY_UNAVAILABLE -> "连接异常"
}

private fun pluginStatusColor(palette: CinePilotPalette, status: PluginStatus): Color = when (status) {
    PluginStatus.READY -> SuccessGreen
    PluginStatus.AUTH_REQUIRED -> WarnAmber
    PluginStatus.DISABLED -> palette.textMuted
    PluginStatus.TEMPORARILY_UNAVAILABLE -> ErrorRed
}

/**
 * Bangumi plugin detail page — PAT paste field, verify button, status,
 * last-error display, and disconnect.
 *
 * <p>Reads and writes only go through the host; the screen itself stays
 * pure-Compose, side-effect free. All state is owned by the caller so
 * background async verification can be rendered correctly.
 */
@Composable
fun BangumiPluginSettingsScreen(
    palette: CinePilotPalette,
    pluginHost: PluginHost,
    pluginInfo: PluginHost.PluginInfo,
    tokenFieldValue: String,
    onTokenChange: (String) -> Unit,
    verifying: Boolean,
    verifyMessage: String,
    verifyMessageIsError: Boolean,
    lastError: String,
    onBack: () -> Unit,
    onVerify: () -> Unit,
    onDisconnect: () -> Unit,
) {
    // statusColor is a cheap `when`; skip remember() to stay correct when
    // the palette (theme) switches while this screen is composed.
    val statusColor = pluginStatusColor(palette, pluginInfo.status)
    var showToken by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = TvDp.ScreenX,
                top = TvDp.ScreenTop,
                end = TvDp.ScreenX,
                bottom = TvDp.ScreenBottom,
            )
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BasicText(
                text = pluginInfo.descriptor.name(),
                style = TextStyle(
                    color = palette.textPrimary,
                    fontSize = TvText.Section,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            BasicText(
                text = "v" + pluginInfo.descriptor.version(),
                style = TextStyle(color = palette.textMuted, fontSize = TvText.Label),
            )
        }

        BasicText(
            text = pluginInfo.descriptor.description(),
            style = TextStyle(color = palette.textSecondary, fontSize = TvText.Body),
        )

        Spacer(Modifier.height(4.dp))

        // Status row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicText(
                text = "状态：",
                style = TextStyle(color = palette.textMuted, fontSize = TvText.Body),
            )
            BasicText(
                text = pluginStatusLabel(pluginInfo.status),
                style = TextStyle(
                    color = statusColor,
                    fontSize = TvText.Body,
                    fontWeight = FontWeight.Medium,
                ),
            )
            BasicText(
                text = "· 开发方：" + pluginInfo.descriptor.vendor(),
                style = TextStyle(color = palette.textMuted, fontSize = TvText.Label),
                modifier = Modifier.padding(start = 6.dp),
            )
        }

        if (lastError.isNotBlank()) {
            BasicText(
                text = "最近错误：$lastError",
                style = TextStyle(
                    color = ErrorRed,
                    fontSize = TvText.Label,
                    lineHeight = TvText.Body.times(1.1f),
                ),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(10.dp))

        BasicText(
            text = "个人访问令牌 (PAT)",
            style = TextStyle(color = palette.textSecondary, fontSize = TvText.Label),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TvTextField(
                palette = palette,
                value = tokenFieldValue,
                hint = "请在 next.bangumi.tv 生成后粘贴到此处",
                password = !showToken,
                requestInitialFocus = true,
                selectAllOnFocus = true,
                onValueChange = onTokenChange,
                modifier = Modifier.weight(1f),
            )
            TvActionButton(
                palette = palette,
                label = if (showToken) "隐藏" else "显示",
                onClick = { showToken = !showToken },
            )
        }
        BasicText(
            text = "访问 https://next.bangumi.tv/dev/apps 创建 PAT，建议授权最少的「用户资料」+「收藏」scope。",
            style = TextStyle(color = palette.textMuted, fontSize = TvText.Label),
        )

        Spacer(Modifier.height(6.dp))

        if (verifyMessage.isNotBlank()) {
            BasicText(
                text = verifyMessage,
                style = TextStyle(
                    color = if (verifyMessageIsError) ErrorRed else MutedBlue,
                    fontSize = TvText.Body,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(Modifier.height(6.dp))
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TvActionButton(
                palette = palette,
                label = "验证并保存",
                selected = true,
                onClick = onVerify,
            )
            TvActionButton(
                palette = palette,
                label = "断开连接",
                onClick = onDisconnect,
            )
            TvActionButton(
                palette = palette,
                label = "返回",
                onClick = onBack,
            )
        }
        if (verifying) {
            Spacer(Modifier.height(6.dp))
            BasicText(
                text = "正在与 Bangumi API 握手...",
                style = TextStyle(color = palette.textMuted, fontSize = TvText.Label),
            )
        }
    }
}
