package tv.cinepilot.tv.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tv.cinepilot.core.tv.SearchFilter
import tv.cinepilot.tv.compose.components.SettingsGrid
import tv.cinepilot.tv.compose.components.TvActionButton
import tv.cinepilot.tv.compose.components.TvOptionRow
import tv.cinepilot.tv.compose.components.TvTextField
import tv.cinepilot.tv.compose.theme.CinePilotPalette

@Composable
fun ComposeSearchScreen(
    palette: CinePilotPalette,
    initialTerm: String,
    selectedFilter: SearchFilter,
    onFilter: (SearchFilter, String) -> Unit,
    onVoiceInput: (String) -> Unit,
    onSubmit: (String, SearchFilter) -> Unit,
) {
    var term by remember(initialTerm) { mutableStateOf(initialTerm) }
    Column(modifier = Modifier.fillMaxWidth()) {
        TvTextField(
            palette = palette,
            value = term,
            hint = "搜索媒体",
            onValueChange = { term = it },
        )
        Spacer(Modifier.height(12.dp))
        SettingsGrid(
            palette = palette,
            title = "范围",
            rows = SearchFilter.values().map { filter ->
                {
                    TvOptionRow(
                        palette = palette,
                        label = filter.label(),
                        value = if (filter == selectedFilter) "当前" else "切换",
                        selected = filter == selectedFilter,
                        onClick = { onFilter(filter, term) },
                    )
                }
            },
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TvActionButton(
                palette = palette,
                label = "搜索",
                selected = true,
                modifier = Modifier.weight(1f),
                onClick = { onSubmit(term.trim(), selectedFilter) },
            )
            TvActionButton(
                palette = palette,
                label = "语音",
                modifier = Modifier.weight(1f),
                onClick = { onVoiceInput(term) },
            )
        }
    }
}
