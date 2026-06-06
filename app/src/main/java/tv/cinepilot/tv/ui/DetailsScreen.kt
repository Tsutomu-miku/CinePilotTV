package tv.cinepilot.tv.ui

import android.view.View
import android.widget.ImageView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary

fun ComponentActivity.detailsScreen(
    item: MediaItemSummary,
    playbackActions: List<InfuseAction>,
    trackControls: View?,
    technicalInfo: List<String>,
    folderAction: InfuseAction,
    loadPoster: (ImageView, MediaItemSummary, Int, Int) -> Unit,
    loadBackdrop: (ImageView, MediaItemSummary) -> Unit,
): View {
    val presentation = item.toMediaPresentation(
        technicalTags = technicalInfo,
    )
    return detailsStage(item, loadBackdrop) {
        addView(detailsHero(
            item = item,
            presentation = presentation,
            actions = playbackActions,
            folderAction = folderAction,
            loadPoster = loadPoster,
        ))
        detailsInfoSections(
            trackControls = trackControls,
            technicalInfo = presentation.technicalTags,
            overview = item.overview(),
        ).forEach(::addView)
    }
}
