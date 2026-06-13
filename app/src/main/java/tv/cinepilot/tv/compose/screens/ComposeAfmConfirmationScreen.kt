package tv.cinepilot.tv.compose.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.cinepilot.tv.compose.components.FocusSurface
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText
import tv.cinepilot.tv.ui.DisplayModeSwitchPrompt
import tv.cinepilot.tv.ui.TvIcon

@Composable
fun ComposeAfmConfirmationScreen(
    palette: CinePilotPalette,
    prompt: DisplayModeSwitchPrompt,
    currentModeLabel: String,
    onSwitchNow: () -> Unit,
    onSkipOnce: () -> Unit,
    onAlwaysSkip: () -> Unit,
) {
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
                    text = "切换显示模式",
                    maxLines = 1,
                    style = TextStyle(color = palette.accentStrong, fontSize = TvText.Section),
                )
                Spacer(Modifier.height(12.dp))

                AfmMetaRow(
                    palette = palette,
                    label = "当前模式",
                    value = currentModeLabel,
                )
                AfmMetaRow(
                    palette = palette,
                    label = "建议分辨率",
                    value = prompt.resolutionLabel,
                )
                AfmMetaRow(
                    palette = palette,
                    label = "建议帧率",
                    value = prompt.refreshRateLabel,
                )

                Spacer(Modifier.height(6.dp))
                BasicText(
                    text = "内容帧率与当前屏幕模式不匹配。\n切换后可消除画面抖动与撕裂。",
                    style = TextStyle(
                        color = palette.textSecondary,
                        fontSize = TvText.Metadata,
                        lineHeight = (12.5 * 1.05).sp,
                    ),
                    modifier = Modifier.padding(bottom = 14.dp),
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Primary action: 立即切换
                    FocusSurface(
                        palette = palette,
                        selected = true,
                        requestInitialFocus = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(TvDp.ControlHeight),
                        padding = PaddingValues(horizontal = 16.dp),
                        onClick = onSwitchNow,
                    ) { focused ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Image(
                                painter = painterResource(TvIcon.PLAY.drawableRes),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(
                                    if (focused) palette.focusText else palette.textPrimary
                                ),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(7.dp))
                            BasicText(
                                text = "立即切换",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(
                                    color = if (focused) palette.focusText else palette.textPrimary,
                                    fontSize = TvText.Body,
                                ),
                            )
                        }
                    }

                    // Secondary action: 本次不切换
                    FocusSurface(
                        palette = palette,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(TvDp.ControlHeight),
                        padding = PaddingValues(horizontal = 16.dp),
                        onClick = onSkipOnce,
                    ) { focused ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Image(
                                painter = painterResource(TvIcon.BACK.drawableRes),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(
                                    if (focused) palette.textPrimary else palette.textSecondary
                                ),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(7.dp))
                            BasicText(
                                text = "本次不切换",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(
                                    color = if (focused) palette.textPrimary else palette.textSecondary,
                                    fontSize = TvText.Body,
                                ),
                            )
                        }
                    }

                    // Quiet action: 不再提醒
                    FocusSurface(
                        palette = palette,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(TvDp.ControlHeight),
                        padding = PaddingValues(horizontal = 16.dp),
                        onClick = onAlwaysSkip,
                    ) { focused ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Image(
                                painter = painterResource(TvIcon.CHECK.drawableRes),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(
                                    if (focused) palette.textPrimary else palette.textMuted
                                ),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(7.dp))
                            BasicText(
                                text = "不再提醒",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(
                                    color = if (focused) palette.textPrimary else palette.textMuted,
                                    fontSize = TvText.Body,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AfmMetaRow(
    palette: CinePilotPalette,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = palette.textMuted,
                fontSize = TvText.Metadata,
            ),
            modifier = Modifier.width(140.dp),
        )
        BasicText(
            text = value,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = palette.textPrimary,
                fontSize = TvText.Metadata,
            ),
        )
    }
}
