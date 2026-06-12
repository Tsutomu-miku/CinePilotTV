package tv.cinepilot.tv.ui

import android.graphics.Color
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.util.Locale

data class ProviderIdEditorEntry(
    val key: String,
    val label: String,
    val hint: String,
    val currentValue: String,
)

/**
 * Focused right-side micro-editor for TMDb / IMDb / TVDb / TMDbCollection provider ids.
 * Caller receives the updated map via [onSave] (with blank values stripped) and can
 * additionally request a server-side metadata refresh via the replace-all checkbox.
 */
fun ComponentActivity.providerIdsEditorSheet(
    entries: List<ProviderIdEditorEntry>,
    onCancel: () -> Unit,
    onSave: (Map<String, String>, refreshMetadata: Boolean) -> Unit,
): View {
    val inputs = mutableMapOf<String, EditText>()
    val sheet = sideSheet {
        addView(infusePanelTitle("修正 Provider Id"))
        addView(TextView(this@providerIdsEditorSheet).apply {
            text = "手动指定 TMDb / IMDb / TVDb 编号后，\n可重新触发服务端元数据扫描。"
            textSize = 12f
            setTextColor(TvColors.TextSecondary)
            includeFontPadding = false
            setPadding(0, dp(2), 0, dp(14))
            setLineSpacing(2f, 1.05f)
        })
        entries.forEach { entry ->
            addView(TextView(this@providerIdsEditorSheet).apply {
                text = entry.label
                textSize = 11f
                setTextColor(TvColors.TextMuted)
                includeFontPadding = false
                setPadding(0, dp(10), 0, dp(4))
            })
            addView(EditText(this@providerIdsEditorSheet).apply {
                hint = entry.hint
                setText(entry.currentValue)
                setTextColor(TvColors.TextPrimary)
                setHintTextColor(TvColors.TextMuted)
                textSize = 14f
                maxLines = 1
                isSingleLine = true
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
                background = rounded(
                    Color.argb(24, 255, 255, 255),
                    dp(10),
                    dp(1),
                    homeHairlineColor(42),
                )
                setPadding(dp(12), dp(8), dp(12), dp(8))
                includeFontPadding = false
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                    override fun afterTextChanged(s: Editable?) = Unit
                })
                setOnFocusChangeListener { view, focused ->
                    view.applyFocusOutline(focused, 10)
                }
                inputs[entry.key] = this
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.MarginLayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = dp(2)
                }
            })
        }
        // Checkbox: trigger server metadata refresh after save.
        val refreshBox = TextView(this@providerIdsEditorSheet).apply {
            text = "☐ 保存后重新扫描元数据（替换现有海报/剧情）"
            tag = false
            textSize = 12.5f
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(TvColors.TextSecondary)
            includeFontPadding = false
            isFocusable = true
            isClickable = true
            setPadding(dp(4), dp(10), dp(4), dp(10))
            background = rounded(Color.TRANSPARENT, dp(8), dp(1), Color.TRANSPARENT)
            setOnClickListener {
                val next = !(tag as? Boolean ?: false)
                tag = next
                text = if (next) {
                    "☑ 保存后重新扫描元数据（替换现有海报/剧情）"
                } else {
                    "☐ 保存后重新扫描元数据（替换现有海报/剧情）"
                }
            }
            setOnFocusChangeListener { view, focused ->
                view.applyFocusOutline(focused, 8)
                (view as? TextView)?.setTextColor(
                    if (focused) TvColors.TextPrimary else TvColors.TextSecondary
                )
            }
        }
        addView(refreshBox)
        addView(detailsActions(listOf(
            InfuseAction("保存", TvIcon.CHECK, InfuseActionEmphasis.PRIMARY) {
                val result = mutableMapOf<String, String>()
                inputs.forEach { (key, edit) ->
                    val raw = edit.text?.toString()?.trim() ?: ""
                    if (raw.isNotBlank()) result[key] = raw
                }
                val refresh = refreshBox.tag as? Boolean ?: false
                onSave(result, refresh)
            },
            InfuseAction("取消", TvIcon.BACK, InfuseActionEmphasis.SECONDARY, onCancel),
        )))
    }
    // Initial focus: first edit field.
    inputs.values.firstOrNull()?.let { input ->
        sheet.post {
            input.requestFocus()
            input.selectAll()
        }
    }
    return sheet
}

/**
 * Builds the editable entry list for the editor sheet, using the four most
 * commonly supported provider ids in Jellyfin / Emby.
 */
fun buildProviderIdEditorEntries(values: Map<String, String>): List<ProviderIdEditorEntry> {
    return listOf(
        ProviderIdEditorEntry(
            key = "Tmdb",
            label = "TMDb 编号",
            hint = "例如：155（Movie） / 1399（TV Series）",
            currentValue = values["Tmdb"] ?: "",
        ),
        ProviderIdEditorEntry(
            key = "Imdb",
            label = "IMDb 编号",
            hint = "例如：tt0468569",
            currentValue = values["Imdb"] ?: "",
        ),
        ProviderIdEditorEntry(
            key = "Tvdb",
            label = "TVDb 编号",
            hint = "数字 Id，例如：80379",
            currentValue = values["Tvdb"] ?: "",
        ),
        ProviderIdEditorEntry(
            key = "TmdbCollection",
            label = "TMDb Collection 编号",
            hint = "例如：1241 （Batman 系列合集）",
            currentValue = values["TmdbCollection"] ?: "",
        ),
    )
}

private fun EditText.selectAll() {
    this.setSelection(0, length())
}
