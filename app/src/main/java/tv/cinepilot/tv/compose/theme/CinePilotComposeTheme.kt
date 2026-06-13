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
                focusRing = Color(0xFFBAE6FD),
                focusText = Color(0xFF02121E),
            )
            AppTheme.EMBY -> defaultPalette(
                accent = Color(0xFF84CC16),
                accentStrong = Color(0xFFBEF264),
                focusRing = Color(0xFFBBF7D0),
                focusText = Color(0xFF05160C),
            )
            AppTheme.INFUSE -> defaultPalette(
                background = Color(0xFF03050C),
                glass = Color(0xAA090B11),
                glassFocus = Color(0xC4121722),
                accent = Color(0xFF9AB9FF),
                accentStrong = Color(0xFFEEF4FF),
                focusRing = Color(0xFFE0EBFF),
                focusText = Color(0xFF050B18),
                textSecondary = Color(0xFFD7DEEE),
                textMuted = Color(0xFF8E98AE),
            )
            AppTheme.CINEPILOT -> defaultPalette()
        }
    }

    private fun defaultPalette(
        background: Color = Color(0xFF080D18),
        glass: Color = Color(0xB00A0C12),
        glassFocus: Color = Color(0xC4121722),
        accent: Color = Color(0xFF2DD4BF),
        accentStrong: Color = Color(0xFF5EEAD4),
        focusRing: Color = Color(0xFF99F6E4),
        focusText: Color = Color(0xFF031216),
        textSecondary: Color = Color(0xFFE2E8F0),
        textMuted: Color = Color(0xFF94A3B8),
    ) = CinePilotPalette(
        background = background,
        scrim = Color(0xD9000000),
        glass = glass,
        glassFocus = glassFocus,
        glassBorder = Color(0x56F4F7FF),
        focusRing = focusRing,
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
    val ScreenX: Dp = 44.dp
    val ScreenTop: Dp = 20.dp
    val ScreenBottom: Dp = 34.dp
    val RowGap: Dp = 14.dp
    val CellGap: Dp = 8.dp
    val PosterWidth: Dp = 104.dp
    val PosterHeight: Dp = 156.dp
    val LandscapeWidth: Dp = 188.dp
    val LandscapeHeight: Dp = 106.dp
    val ControlHeight: Dp = 44.dp
    val IconButtonSize: Dp = 44.dp
    val SettingsRowHeight: Dp = 64.dp
    val PanelRadius: Dp = 18.dp
    val ControlRadius: Dp = 12.dp
    val CardRadius: Dp = 8.dp
}

object TvText {
    val Brand = 12.sp
    val PageTitle = 30.sp
    val DetailTitle = 36.sp
    val Section = 14.sp
    val Body = 15.sp
    val Metadata = 12.sp
    val Label = 11.sp
    val CardTitle = 12.sp
}
