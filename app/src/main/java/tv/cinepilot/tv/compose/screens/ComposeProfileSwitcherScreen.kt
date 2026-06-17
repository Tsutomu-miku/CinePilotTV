package tv.cinepilot.tv.compose.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tv.cinepilot.core.protocol.ProfileSummary
import tv.cinepilot.core.protocol.PublicUserSummary
import tv.cinepilot.core.protocol.ServerIdentity
import tv.cinepilot.tv.compose.artwork.CinePilotAsyncImage
import tv.cinepilot.tv.compose.artwork.rememberPublicUserRequest
import tv.cinepilot.tv.compose.components.FocusSurface
import tv.cinepilot.tv.compose.components.SettingsGrid
import tv.cinepilot.tv.compose.components.TvActionButton
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText
import tv.cinepilot.tv.runtime.ArtworkRequestFactory
import tv.cinepilot.tv.runtime.ArtworkRequestSpec

@Composable
fun ComposeProfileSwitcherScreen(
    palette: CinePilotPalette,
    owner: ComponentActivity,
    artworkFactory: ArtworkRequestFactory,
    server: ServerIdentity?,
    profiles: List<ProfileSummary>,
    isLoading: Boolean = false,
    onSwitch: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(TvDp.RowGap),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (isLoading) {
            LoadingProfiles(palette = palette, onClose = onClose)
        } else if (profiles.isEmpty()) {
            EmptyProfiles(palette = palette, onClose = onClose)
        } else {
            SettingsGrid(
                palette = palette,
                title = "已保存账号",
                rows = profiles.map { profile ->
                    {
                        val user = profile.toPublicUser()
                        val avatar = rememberPublicUserRequest(
                            factory = artworkFactory,
                            server = server,
                            user = user,
                            width = 96,
                            height = 96,
                        )
                        ProfileRow(
                            palette = palette,
                            profile = profile,
                            avatar = avatar,
                            onClick = { onSwitch(profile.userId()) },
                        )
                    }
                },
            )
            SettingsGrid(
                palette = palette,
                title = "账号管理",
                rows = buildList {
                    add {
                        TvActionButton(
                            palette = palette,
                            label = "关闭",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onClose,
                        )
                    }
                    profiles.forEach { profile ->
                        add {
                            AccountRemoveRow(
                                palette = palette,
                                profile = profile,
                                onRemove = { onRemove(profile.userId()) },
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun LoadingProfiles(palette: CinePilotPalette, onClose: () -> Unit) {
    SettingsGrid(
        palette = palette,
        title = "账号",
        rows = listOf(
            {
                BasicText(
                    text = "正在加载账号列表...",
                    style = TextStyle(color = palette.textSecondary, fontSize = TvText.Body),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                )
            },
            {
                TvActionButton(
                    palette = palette,
                    label = "关闭",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClose,
                )
            },
        ),
    )
}

@Composable
private fun EmptyProfiles(palette: CinePilotPalette, onClose: () -> Unit) {
    SettingsGrid(
        palette = palette,
        title = "账号",
        rows = listOf(
            {
                TvActionButton(
                    palette = palette,
                    label = "返回登录",
                    selected = true,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClose,
                )
            },
        ),
    )
}

@Composable
private fun ProfileRow(
    palette: CinePilotPalette,
    profile: ProfileSummary,
    avatar: ArtworkRequestSpec?,
    onClick: () -> Unit,
) {
    FocusSurface(
        palette = palette,
        selected = profile.isActive(),
        modifier = Modifier
            .fillMaxWidth()
            .height(TvDp.ProfileRowHeight),
        padding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        onClick = onClick,
    ) { focused ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Avatar(palette = palette, artwork = avatar, label = profile.displayName())
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = profile.displayName(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = if (focused) palette.textPrimary else palette.textSecondary,
                        fontSize = TvText.Body,
                    ),
                )
                Spacer(Modifier.height(4.dp))
                BasicText(
                    text = if (profile.isActive()) "当前账号" else "切换到此账号",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = palette.textMuted, fontSize = TvText.Metadata),
                )
            }
            BasicText(
                text = if (profile.isActive()) "当前" else "切换",
                maxLines = 1,
                style = TextStyle(
                    color = if (focused || profile.isActive()) palette.accentStrong else palette.textMuted,
                    fontSize = TvText.Metadata,
                ),
            )
        }
    }
}

@Composable
private fun AccountRemoveRow(
    palette: CinePilotPalette,
    profile: ProfileSummary,
    onRemove: () -> Unit,
) {
    FocusSurface(
        palette = palette,
        modifier = Modifier
            .fillMaxWidth()
            .height(TvDp.SettingsRowHeight),
        onClick = onRemove,
    ) { focused ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            BasicText(
                text = "移除 ${profile.displayName()}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = if (focused) palette.textPrimary else palette.textSecondary,
                    fontSize = TvText.Body,
                ),
                modifier = Modifier.weight(1f),
            )
            BasicText(
                text = "移除",
                maxLines = 1,
                style = TextStyle(
                    color = if (focused) palette.accentStrong else palette.textMuted,
                    fontSize = TvText.Metadata,
                ),
            )
        }
    }
}

@Composable
private fun Avatar(palette: CinePilotPalette, artwork: ArtworkRequestSpec?, label: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(TvDp.AvatarSize)
            .clip(CircleShape)
            .background(palette.posterFallback),
    ) {
        if (artwork != null) {
            CinePilotAsyncImage(
                request = artwork,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            BasicText(
                text = label.firstOrNull()?.uppercaseChar()?.toString().orEmpty(),
                style = TextStyle(color = palette.textSecondary, fontSize = TvText.Body),
            )
        }
    }
}

private fun ProfileSummary.displayName(): String = userName().ifBlank { userId() }

private fun ProfileSummary.toPublicUser(): PublicUserSummary {
    return PublicUserSummary(userId(), userName(), false, userImageTag())
}
