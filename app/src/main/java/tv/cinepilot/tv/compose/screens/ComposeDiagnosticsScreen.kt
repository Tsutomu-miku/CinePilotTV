package tv.cinepilot.tv.compose.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tv.cinepilot.tv.compose.components.InfoPanel
import tv.cinepilot.tv.compose.components.SettingsGrid
import tv.cinepilot.tv.compose.components.TvActionButton
import tv.cinepilot.tv.compose.components.TvLazyPage
import tv.cinepilot.tv.compose.theme.CinePilotPalette

@Composable
fun ComposeDiagnosticsScreen(
    palette: CinePilotPalette,
    diagnostics: String,
    returnToPlayer: Boolean,
    backLabel: String?,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onBackDiagnosticsTarget: () -> Unit,
) {
    TvLazyPage {
        item {
            InfoPanel(
                palette = palette,
                title = "当前诊断",
                body = diagnostics,
            )
        }
        item {
            DiagnosticsActions(
                palette = palette,
                primaryLabel = "导出诊断",
                onPrimary = onExport,
                secondaryLabel = "分享诊断",
                onSecondary = onShare,
                returnToPlayer = returnToPlayer,
                backLabel = backLabel,
                onBackDiagnosticsTarget = onBackDiagnosticsTarget,
            )
        }
    }
}

@Composable
fun ComposeDiagnosticsExportedScreen(
    palette: CinePilotPalette,
    path: String,
    returnToPlayer: Boolean,
    backLabel: String?,
    onShare: () -> Unit,
    onBackDiagnostics: () -> Unit,
    onBackDiagnosticsTarget: () -> Unit,
) {
    TvLazyPage {
        item {
            InfoPanel(
                palette = palette,
                title = "诊断已导出",
                body = path,
            )
        }
        item {
            SettingsGrid(
                palette = palette,
                title = "下一步",
                rows = listOf(
                    {
                        TvActionButton(
                            palette = palette,
                            label = "分享诊断",
                            selected = true,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onShare,
                        )
                    },
                    {
                        TvActionButton(
                            palette = palette,
                            label = "返回诊断信息",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onBackDiagnostics,
                        )
                    },
                    {
                        TvActionButton(
                            palette = palette,
                            label = diagnosticsBackLabel(returnToPlayer, backLabel),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onBackDiagnosticsTarget,
                        )
                    },
                ),
            )
        }
    }
}

@Composable
private fun DiagnosticsActions(
    palette: CinePilotPalette,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    returnToPlayer: Boolean,
    backLabel: String?,
    onBackDiagnosticsTarget: () -> Unit,
) {
    SettingsGrid(
        palette = palette,
        title = "诊断操作",
        rows = listOf(
            {
                TvActionButton(
                    palette = palette,
                    label = primaryLabel,
                    selected = true,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onPrimary,
                )
            },
            {
                TvActionButton(
                    palette = palette,
                    label = secondaryLabel,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSecondary,
                )
            },
            {
                TvActionButton(
                    palette = palette,
                    label = diagnosticsBackLabel(returnToPlayer, backLabel),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onBackDiagnosticsTarget,
                )
            },
        ),
    )
}

private fun diagnosticsBackLabel(returnToPlayer: Boolean, backLabel: String?): String {
    return backLabel ?: if (returnToPlayer) "返回播放器" else "返回错误页"
}
