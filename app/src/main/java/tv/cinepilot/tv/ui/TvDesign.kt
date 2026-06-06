package tv.cinepilot.tv.ui

import android.graphics.Color

object TvColors {
    var Background = Color.rgb(8, 13, 24)
        private set
    var Surface = Color.rgb(13, 20, 33)
        private set
    var SurfaceRaised = Color.rgb(18, 26, 38)
        private set
    var SurfaceControl = Color.rgb(26, 36, 52)
        private set
    var SurfaceInput = Color.rgb(16, 24, 39)
        private set
    var PosterFallback = Color.rgb(24, 34, 49)
        private set
    var PosterBorder = Color.rgb(30, 41, 59)
        private set
    var Focus = Color.rgb(20, 184, 166)
        private set
    var FocusRing = Color.rgb(153, 246, 228)
        private set
    var FocusText = Color.rgb(3, 18, 22)
        private set
    var Accent = Color.rgb(45, 212, 191)
        private set
    var AccentStrong = Color.rgb(94, 234, 212)
        private set
    var Resume = Color.rgb(15, 118, 110)
        private set
    var TextPrimary = Color.WHITE
        private set
    var TextSecondary = Color.rgb(226, 232, 240)
        private set
    var TextMuted = Color.rgb(148, 163, 184)
        private set

    fun applyTheme(themeId: String) {
        val palette = when (themeId) {
            "jellyfin" -> ThemePalette(
                focus = Color.rgb(0, 168, 232),
                focusRing = Color.rgb(186, 230, 253),
                focusText = Color.rgb(2, 18, 30),
                accent = Color.rgb(56, 189, 248),
                accentStrong = Color.rgb(125, 211, 252),
                resume = Color.rgb(3, 105, 161),
            )
            "emby" -> ThemePalette(
                focus = Color.rgb(82, 196, 26),
                focusRing = Color.rgb(187, 247, 208),
                focusText = Color.rgb(5, 22, 12),
                accent = Color.rgb(132, 204, 22),
                accentStrong = Color.rgb(190, 242, 100),
                resume = Color.rgb(63, 98, 18),
            )
            else -> ThemePalette()
        }
        Focus = palette.focus
        FocusRing = palette.focusRing
        FocusText = palette.focusText
        Accent = palette.accent
        AccentStrong = palette.accentStrong
        Resume = palette.resume
    }

    private data class ThemePalette(
        val focus: Int = Color.rgb(20, 184, 166),
        val focusRing: Int = Color.rgb(153, 246, 228),
        val focusText: Int = Color.rgb(3, 18, 22),
        val accent: Int = Color.rgb(45, 212, 191),
        val accentStrong: Int = Color.rgb(94, 234, 212),
        val resume: Int = Color.rgb(15, 118, 110),
    )
}

object TvSpacing {
    const val ScreenX = 40
    const val ScreenTop = 28
    const val ScreenBottom = 44
    const val SectionTop = 16
    const val SectionBottom = 8
    const val ControlGap = 10
    const val CardGap = 10
    const val FocusInset = 0
    const val PlayerOverlayX = 40
}

object TvRadius {
    const val Control = 8
    const val Card = 10
}

object TvType {
    const val Brand = 13f
    const val Title = 30f
    const val Section = 19f
    const val Body = 16f
    const val Metadata = 13f
    const val CardTitle = 14f
    const val PlayerTitle = 18f
}

object TvSize {
    const val ControlHeight = 48
    const val InputHeight = 48
    const val PosterWidth = 118
    const val PosterHeight = 177
    const val DetailPosterWidth = 160
    const val DetailPosterHeight = 240
    const val PlayerTopOverlay = 72
    const val PlayerBottomOverlay = 84
}
