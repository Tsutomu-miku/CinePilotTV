package tv.cinepilot.tv.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import tv.cinepilot.core.protocol.PublicUserSummary
import tv.cinepilot.core.protocol.QuickConnectSession
import tv.cinepilot.tv.compose.components.InfoPanel
import tv.cinepilot.tv.compose.components.SettingsGrid
import tv.cinepilot.tv.compose.components.TvActionButton
import tv.cinepilot.tv.compose.components.TvOptionRow
import tv.cinepilot.tv.compose.components.TvTextField
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvText
import tv.cinepilot.tv.runtime.RecentAccount
import tv.cinepilot.tv.runtime.RecentServer

@Composable
fun ComposeServerEntryScreen(
    palette: CinePilotPalette,
    recentAccounts: List<RecentAccount>,
    recentServers: List<RecentServer>,
    onContinueAccount: (RecentAccount) -> Unit,
    onOpenServer: (String) -> Unit,
    onClearAccounts: () -> Unit,
) {
    var serverAddress by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth()) {
        if (recentAccounts.isNotEmpty()) {
            val accountRows = buildList<@Composable () -> Unit> {
                recentAccounts.forEach { account ->
                    add {
                        TvOptionRow(
                            palette = palette,
                            label = account.displayName(),
                            description = account.serverName,
                            value = "继续",
                            selected = true,
                            onClick = { onContinueAccount(account) },
                        )
                    }
                }
                add {
                    TvOptionRow(
                        palette = palette,
                        label = "清除已保存登录",
                        value = "清除",
                        onClick = onClearAccounts,
                    )
                }
            }
            SettingsGrid(
                palette = palette,
                title = "最近账号",
                rows = accountRows,
            )
        }
        if (recentServers.isNotEmpty()) {
            SettingsGrid(
                palette = palette,
                title = "最近服务器",
                rows = recentServers.map { server ->
                    {
                        TvOptionRow(
                            palette = palette,
                            label = server.displayName(),
                            value = "连接",
                            onClick = { onOpenServer(server.serverAddress) },
                        )
                    }
                },
            )
        }
        BasicText(
            text = "服务器地址",
            style = TextStyle(color = palette.textSecondary, fontSize = TvText.Section),
        )
        Spacer(Modifier.height(8.dp))
        TvTextField(
            palette = palette,
            value = serverAddress,
            hint = "http://192.168.1.10:8096",
            onValueChange = { serverAddress = it },
        )
        Spacer(Modifier.height(12.dp))
        TvActionButton(
            palette = palette,
            label = "连接服务器",
            selected = true,
            modifier = Modifier.fillMaxWidth(),
            onClick = { onOpenServer(serverAddress.trim()) },
        )
    }
}

@Composable
fun ComposeLoginScreen(
    palette: CinePilotPalette,
    serverName: String,
    publicUsers: List<PublicUserSummary>,
    quickConnectAvailable: Boolean,
    onLogin: (String, String) -> Unit,
    onQuickConnect: () -> Unit,
    onBackToServer: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth()) {
        BasicText(
            text = serverName,
            style = TextStyle(color = palette.textSecondary, fontSize = TvText.Section),
        )
        Spacer(Modifier.height(12.dp))
        if (publicUsers.isNotEmpty()) {
            SettingsGrid(
                palette = palette,
                title = "选择用户",
                rows = publicUsers.map { user ->
                    {
                        val name = user.name().ifBlank { user.id() }
                        TvOptionRow(
                            palette = palette,
                            label = name,
                            description = if (user.passwordRequired()) "需要密码" else "免密码登录",
                            value = if (user.passwordRequired()) "填写" else "登录",
                            onClick = {
                                username = name
                                if (!user.passwordRequired()) {
                                    onLogin(name, "")
                                }
                            },
                        )
                    }
                },
            )
        }
        TvTextField(
            palette = palette,
            value = username,
            hint = "用户名",
            onValueChange = { username = it },
        )
        Spacer(Modifier.height(10.dp))
        TvTextField(
            palette = palette,
            value = password,
            hint = "密码",
            password = true,
            onValueChange = { password = it },
        )
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TvActionButton(
                palette = palette,
                label = "登录",
                selected = true,
                modifier = Modifier.weight(1f),
                onClick = { onLogin(username.trim(), password) },
            )
            if (quickConnectAvailable) {
                TvActionButton(
                    palette = palette,
                    label = "Quick Connect",
                    modifier = Modifier.weight(1f),
                    onClick = onQuickConnect,
                )
            }
            TvActionButton(
                palette = palette,
                label = "返回",
                modifier = Modifier.weight(1f),
                onClick = onBackToServer,
            )
        }
    }
}

@Composable
fun ComposeQuickConnectScreen(
    palette: CinePilotPalette,
    quickConnect: QuickConnectSession,
    status: String,
    onCheckNow: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BasicText(
            text = quickConnect.code(),
            maxLines = 1,
            style = TextStyle(
                color = palette.accentStrong,
                fontSize = TvText.DetailTitle,
                fontFamily = FontFamily.Monospace,
            ),
        )
        Spacer(Modifier.height(12.dp))
        InfoPanel(
            palette = palette,
            title = "授权状态",
            body = status,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TvActionButton(
                palette = palette,
                label = "立即检查授权",
                selected = true,
                requestInitialFocus = true,
                modifier = Modifier.weight(1f),
                onClick = onCheckNow,
            )
            TvActionButton(
                palette = palette,
                label = "返回登录",
                modifier = Modifier.weight(1f),
                onClick = onBackToLogin,
            )
        }
    }
}
