package tv.cinepilot.tv.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import tv.cinepilot.tv.compose.components.FocusSurface
import tv.cinepilot.tv.compose.components.TvActionButton
import tv.cinepilot.tv.compose.components.TvTextField
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText
import tv.cinepilot.tv.ui.ProviderIdEditorEntry

@Composable
fun ComposeProviderIdEditorScreen(
    palette: CinePilotPalette,
    entries: List<ProviderIdEditorEntry>,
    onCancel: () -> Unit,
    onSave: (Map<String, String>, refreshMetadata: Boolean) -> Unit,
) {
    var values by remember { mutableStateOf(entries.associate { it.key to it.currentValue }.toMutableMap()) }
    var refreshMetadata by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(420.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            palette.background.copy(alpha = 0.12f),
                            palette.background.copy(alpha = 0.92f),
                            palette.background.copy(alpha = 0.98f),
                        ),
                    ),
                )
                .padding(horizontal = 28.dp, vertical = 24.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                BasicText(
                    text = "修正 Provider Id",
                    maxLines = 1,
                    style = TextStyle(color = palette.accentStrong, fontSize = TvText.Section),
                )
                BasicText(
                    text = "手动指定 TMDb / IMDb / TVDb 编号后，\n可重新触发服务端元数据扫描。",
                    style = TextStyle(
                        color = palette.textSecondary,
                        fontSize = TvText.Label,
                        lineHeight = TvText.Body * 1.05f,
                    ),
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    entries.forEachIndexed { index, entry ->
                        ProviderIdField(
                            palette = palette,
                            label = entry.label,
                            hint = entry.hint,
                            value = values[entry.key] ?: "",
                            requestInitialFocus = index == 0,
                            selectAllOnFocus = index == 0,
                            onValueChange = { newValue ->
                                values = values.toMutableMap().apply { put(entry.key, newValue) }
                            },
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    RefreshToggle(
                        palette = palette,
                        checked = refreshMetadata,
                        onCheckedChange = { refreshMetadata = it },
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TvActionButton(
                        palette = palette,
                        label = "保存",
                        selected = true,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val result = values.filterValues { it.isNotBlank() }
                            onSave(result, refreshMetadata)
                        },
                    )
                    TvActionButton(
                        palette = palette,
                        label = "取消",
                        modifier = Modifier.weight(1f),
                        onClick = onCancel,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderIdField(
    palette: CinePilotPalette,
    label: String,
    hint: String,
    value: String,
    requestInitialFocus: Boolean = false,
    selectAllOnFocus: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    Column {
        BasicText(
            text = label,
            style = TextStyle(color = palette.textMuted, fontSize = TvText.Label),
            modifier = Modifier.padding(bottom = 4.dp),
        )
        TvTextField(
            palette = palette,
            value = value,
            hint = hint,
            requestInitialFocus = requestInitialFocus,
            selectAllOnFocus = selectAllOnFocus,
            onValueChange = onValueChange,
        )
    }
}

@Composable
private fun RefreshToggle(
    palette: CinePilotPalette,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    FocusSurface(
        palette = palette,
        selected = checked,
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.fillMaxWidth(),
        radius = TvDp.ControlRadius,
        padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) { focused ->
        BasicText(
            text = if (checked) "☑ 保存后重新扫描元数据（替换现有海报/剧情）" else "☐ 保存后重新扫描元数据（替换现有海报/剧情）",
            maxLines = 1,
            style = TextStyle(
                color = if (focused) palette.textPrimary else palette.textSecondary,
                fontSize = TvText.Body,
            ),
        )
    }
}
