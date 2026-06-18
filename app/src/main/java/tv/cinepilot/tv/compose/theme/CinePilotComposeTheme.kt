package tv.cinepilot.tv.compose.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.cinepilot.tv.settings.AppTheme

@Immutable
data class CinePilotPalette(
    val background: Color,
    val scrim: Color,
    val glass: Color,
    val glassFocus: Color,
    val glassBorder: Color,
    val focusRing: Color,
    val focusGlow: Color,
    val accent: Color,
    val accentStrong: Color,
    val focusText: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val posterFallback: Color,
)

object CinePilotComposeTheme {
    fun palette(theme: AppTheme): CinePilotPalette {
        return when (theme) {
            AppTheme.JELLYFIN -> defaultPalette(
                accent = Color(0xFF38BDF8),
                accentStrong = Color(0xFF7DD3FC),
                focusRing = Color(0xFFD7F1FE),
                focusGlow = Color(0x77BAE6FD),
                focusText = Color(0xFF02121E),
            )
            AppTheme.EMBY -> defaultPalette(
                accent = Color(0xFF84CC16),
                accentStrong = Color(0xFFBEF264),
                focusRing = Color(0xFFDCFCE6),
                focusGlow = Color(0x77BBF7D0),
                focusText = Color(0xFF05160C),
            )
            AppTheme.INFUSE -> defaultPalette(
                background = Color(0xFF03050C),
                glass = Color(0xAA090B11),
                glassFocus = Color(0xC4121722),
                accent = Color(0xFF9AB9FF),
                accentStrong = Color(0xFFEEF4FF),
                focusRing = Color(0xFFEEF4FF),
                focusGlow = Color(0x77E0EBFF),
                focusText = Color(0xFF050B18),
                textSecondary = Color(0xFFD7DEEE),
                textMuted = Color(0xFF8E98AE),
            )
            AppTheme.CINEPILOT -> defaultPalette()
        }
    }

    private fun defaultPalette(
        background: Color = Color(0xFF080D18),
        glass: Color = Color(0xC80F0F17),
        glassFocus: Color = Color(0xE0262E42),
        accent: Color = Color(0xFF3B82F6),
        accentStrong: Color = Color(0xFF60A5FA),
        focusRing: Color = Color(0xFFC4DDFE),
        focusGlow: Color = Color(0x66A0C8FF),
        focusText: Color = Color(0xFF0A162E),
        textSecondary: Color = Color(0xFFE2E8F0),
        textMuted: Color = Color(0xFF94A3B8),
    ) = CinePilotPalette(
        background = background,
        scrim = Color(0xD9000000),
        glass = glass,
        glassFocus = glassFocus,
        glassBorder = Color(0x30E2E8F0),
        focusRing = focusRing,
        focusGlow = focusGlow,
        accent = accent,
        accentStrong = accentStrong,
        focusText = focusText,
        textPrimary = Color.White,
        textSecondary = textSecondary,
        textMuted = textMuted,
        posterFallback = Color(0xFF182231),
    )
}

object TvDp {
    val ScreenX: Dp = 32.dp
    val ScreenTop: Dp = 20.dp
    val ScreenBottom: Dp = 18.dp
    val TopBarHeight: Dp = 36.dp
    val RowGap: Dp = 20.dp
    val CellGap: Dp = 10.dp
    val PosterWidth: Dp = 106.dp
    val PosterHeight: Dp = 158.dp
    val LandscapeWidth: Dp = 176.dp
    val LandscapeHeight: Dp = 99.dp
    val ContinueWidth: Dp = 224.dp
    val ContinueHeight: Dp = 126.dp
    val DetailPosterWidth: Dp = 140.dp
    val DetailPosterHeight: Dp = 205.dp
    val ControlHeight: Dp = 42.dp
    val IconButtonSize: Dp = 36.dp
    val SettingsRowHeight: Dp = 42.dp
    val ProfileRowHeight: Dp = 56.dp
    val AvatarSize: Dp = 44.dp
    val SearchInputHeight: Dp = 42.dp
    val PanelWidth: Dp = 300.dp
    val PlayerSettingsPanelWidth: Dp = 344.dp
    val NextUpWidth: Dp = 344.dp
    val PlayerControlHeight: Dp = 44.dp
    val FocusRing: Dp = 3.dp
    val PanelRadius: Dp = 16.dp
    val ControlRadius: Dp = 14.dp
    val CardRadius: Dp = 14.dp
}

object TvText {
    val Brand = 15.sp
    val PageTitle = 20.sp
    val DetailTitle = 25.sp
    val Section = 15.sp
    val Body = 13.sp
    val Metadata = 10.5.sp
    val Label = 9.5.sp
    val CardTitle = 12.sp
    val PlayerTime = 12.sp
}
