package tv.cinepilot.tv.compose.components.input

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText

@Composable
fun TvTextField(
    palette: CinePilotPalette,
    value: String,
    hint: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    requestInitialFocus: Boolean = false,
    selectAllOnFocus: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    var textFieldValue by remember(value) { mutableStateOf(TextFieldValue(value)) }

    // Keep internal TextFieldValue in sync with external String value (preserve selection).
    LaunchedEffect(value) {
        if (textFieldValue.text != value) {
            textFieldValue = textFieldValue.copy(text = value)
        }
    }

    LaunchedEffect(Unit) {
        if (requestInitialFocus) {
            focusRequester.requestFocus()
        }
    }

    val shape = RoundedCornerShape(TvDp.ControlRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TvDp.SearchInputHeight)
            .clip(shape)
            .background(if (focused) palette.glassFocus else palette.glass, shape)
            .border(BorderStroke(if (focused) TvDp.FocusRing else 0.5.dp, if (focused) palette.focusRing else palette.glassBorder), shape)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused && selectAllOnFocus && textFieldValue.text.isNotEmpty()) {
                    textFieldValue = textFieldValue.copy(
                        selection = TextRange(0, textFieldValue.text.length),
                    )
                }
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (textFieldValue.text.isBlank()) {
            BasicText(
                text = hint,
                maxLines = 1,
                style = TextStyle(color = palette.textMuted, fontSize = TvText.Body),
            )
        }
        BasicTextField(
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it
                onValueChange(it.text)
            },
            singleLine = true,
            textStyle = TextStyle(color = palette.textPrimary, fontSize = TvText.Body),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )
    }
}
