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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tv.cinepilot.core.protocol.AuthenticatedServer
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.tv.compose.artwork.CinePilotAsyncImage
import tv.cinepilot.tv.compose.artwork.rememberArtworkRequest
import tv.cinepilot.tv.compose.components.FocusSurface
import tv.cinepilot.tv.compose.components.MediaRail
import tv.cinepilot.tv.compose.components.PosterCard
import tv.cinepilot.tv.compose.components.TvActionButton
import tv.cinepilot.tv.compose.components.TvTextField
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText
import tv.cinepilot.tv.runtime.ArtworkRequestFactory
import tv.cinepilot.tv.runtime.ArtworkTarget

// ---------------------------------------------------------------------------
// Playlist Picker — side sheet for adding an item to a playlist
// ---------------------------------------------------------------------------

@Composable
fun ComposePlaylistPickerScreen(
    palette: CinePilotPalette,
    playlists: List<MediaItemSummary>,
    onPick: (MediaItemSummary) -> Unit,
    onCreateNew: () -> Unit,
    onClose: () -> Unit,
) {
    SideSheetScaffold(palette = palette) {
        Column(modifier = Modifier.fillMaxSize()) {
            BasicText(
                text = "添加到播放列表",
                maxLines = 1,
                style = TextStyle(color = palette.accentStrong, fontSize = TvText.Section),
            )
            BasicText(
                text = "选择一个播放列表，或创建新的播放列表。",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = palette.textSecondary, fontSize = TvText.Body),
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
            )
            LazyColumn(
                contentPadding = PaddingValues(bottom = TvDp.ScreenBottom),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = playlists,
                    key = { it.id() },
                ) { playlist ->
                    PlaylistRow(
                        palette = palette,
                        name = playlist.name().ifBlank { "未命名播放列表" },
                        isCreateNew = false,
                        onClick = { onPick(playlist) },
                    )
                }
                item {
                    PlaylistRow(
                        palette = palette,
                        name = "新建播放列表…",
                        isCreateNew = true,
                        onClick = onCreateNew,
                    )
                }
            }
            TvActionButton(
                palette = palette,
                label = "返回",
                modifier = Modifier.fillMaxWidth(),
                onClick = onClose,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Create Playlist — side sheet for naming a new playlist
// ---------------------------------------------------------------------------

@Composable
fun ComposeCreatePlaylistScreen(
    palette: CinePilotPalette,
    initialName: String = "",
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    SideSheetScaffold(palette = palette) {
        Column(modifier = Modifier.fillMaxSize()) {
            BasicText(
                text = "新建播放列表",
                maxLines = 1,
                style = TextStyle(color = palette.accentStrong, fontSize = TvText.Section),
            )
            BasicText(
                text = "给新的播放列表起个名字。",
                style = TextStyle(color = palette.textSecondary, fontSize = TvText.Body),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            TvTextField(
                palette = palette,
                value = name,
                hint = "播放列表名称",
                onValueChange = { name = it },
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                TvActionButton(
                    palette = palette,
                    label = "取消",
                    modifier = Modifier.weight(1f),
                    onClick = onCancel,
                )
                TvActionButton(
                    palette = palette,
                    label = "创建",
                    selected = true,
                    requestInitialFocus = true,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val trimmed = name.trim()
                        if (trimmed.isNotBlank()) onConfirm(trimmed)
                    },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Playlist Detail — full-screen view of a playlist's contents
// ---------------------------------------------------------------------------

@Composable
fun ComposePlaylistDetailScreen(
    palette: CinePilotPalette,
    playlist: MediaItemSummary,
    items: List<MediaItemSummary>,
    artworkFactory: ArtworkRequestFactory,
    authenticated: AuthenticatedServer?,
    onPlayAll: () -> Unit,
    onDelete: () -> Unit,
    onOpenItem: (MediaItemSummary) -> Unit,
    onBack: () -> Unit,
) {
    val backdrop = rememberArtworkRequest(
        factory = artworkFactory,
        authenticated = authenticated,
        item = playlist,
        target = ArtworkTarget.BACKDROP,
        width = 1280,
        height = 720,
    )
    Box(modifier = Modifier.fillMaxSize()) {
        // Backdrop image
        if (backdrop != null) {
            CinePilotAsyncImage(
                request = backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.84f),
            )
        }
        // Dark base overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 42f / 255f)),
        )
        // Left-side readable scrim
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1180.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 236f / 255f),
                            Color.Black.copy(alpha = 184f / 255f),
                            Color.Black.copy(alpha = 72f / 255f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        // Content
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(
                start = 44.dp,
                top = 40.dp,
                end = 44.dp,
                bottom = TvDp.ScreenBottom,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Column {
                    BasicText(
                        text = playlist.name().ifBlank { "播放列表" },
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            color = palette.textPrimary,
                            fontSize = TvText.PageTitle,
                        ),
                    )
                    val itemCount = items.size
                    val metaText = when {
                        itemCount == 0 -> "空播放列表"
                        itemCount == 1 -> "1 个项目"
                        else -> "$itemCount 个项目"
                    }
                    BasicText(
                        text = metaText,
                        style = TextStyle(color = palette.textSecondary, fontSize = TvText.Body),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TvActionButton(
                        palette = palette,
                        label = "播放全部",
                        selected = true,
                        requestInitialFocus = true,
                        onClick = onPlayAll,
                    )
                    TvActionButton(
                        palette = palette,
                        label = "删除播放列表",
                        onClick = onDelete,
                    )
                    TvActionButton(
                        palette = palette,
                        label = "返回",
                        onClick = onBack,
                    )
                }
            }
            if (items.isNotEmpty()) {
                item {
                    MediaRail(
                        palette = palette,
                        title = "列表内容",
                        items = items,
                        key = { it.id() },
                    ) { item ->
                        PosterCard(
                            palette = palette,
                            item = item,
                            artwork = rememberArtworkRequest(
                                factory = artworkFactory,
                                authenticated = authenticated,
                                item = item,
                                target = ArtworkTarget.POSTER,
                                width = 260,
                                height = 390,
                            ),
                            onFocus = {},
                            onClick = { onOpenItem(item) },
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared side-sheet scaffold
// ---------------------------------------------------------------------------

@Composable
private fun SideSheetScaffold(
    palette: CinePilotPalette,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
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
            content()
        }
    }
}

// ---------------------------------------------------------------------------
// Shared playlist row (used in the picker sheet)
// ---------------------------------------------------------------------------

@Composable
private fun PlaylistRow(
    palette: CinePilotPalette,
    name: String,
    isCreateNew: Boolean,
    onClick: () -> Unit,
) {
    FocusSurface(
        palette = palette,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        radius = TvDp.ControlRadius,
        padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) { _ ->
        BasicText(
            text = name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = if (isCreateNew) palette.accentStrong else palette.textPrimary,
                fontSize = TvText.Body,
            ),
        )
    }
}
