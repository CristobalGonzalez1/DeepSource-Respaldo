package cl.deepsource.horas.core.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cl.deepsource.horas.core.ui.theme.DsCyan
import cl.deepsource.horas.core.ui.theme.DsError
import cl.deepsource.horas.core.ui.theme.DsInput
import cl.deepsource.horas.core.ui.theme.DsOutline
import cl.deepsource.horas.core.ui.theme.DsTextMuted
import cl.deepsource.horas.core.ui.theme.DsTextPrimary

@Composable
fun DsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    @StringRes error: Int? = null,
    errorMessage: String? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = DsTextMuted)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            isError = error != null || errorMessage != null,
            shape = MaterialTheme.shapes.medium,
            placeholder = placeholder?.let { text ->
                @Composable { Text(text, color = DsTextMuted.copy(alpha = 0.6f)) }
            },
            leadingIcon = leadingIcon?.let { icon -> @Composable { Icon(icon, contentDescription = null) } },
            trailingIcon = trailingIcon,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DsInput,
                unfocusedContainerColor = DsInput,
                errorContainerColor = DsInput,
                disabledContainerColor = DsInput,
                focusedBorderColor = DsCyan,
                unfocusedBorderColor = DsOutline,
                errorBorderColor = DsError,
                cursorColor = DsCyan,
                focusedLeadingIconColor = DsCyan,
                unfocusedLeadingIconColor = DsCyan,
                errorLeadingIconColor = DsError,
                focusedTextColor = DsTextPrimary,
                unfocusedTextColor = DsTextPrimary,
                disabledTextColor = DsTextMuted,
                errorTextColor = DsTextPrimary,
                focusedPlaceholderColor = DsTextMuted,
                unfocusedPlaceholderColor = DsTextMuted,
            ),
        )
        AnimatedVisibility(
            visible = error != null || errorMessage != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            val feedback = errorMessage ?: error?.let { stringResource(it) }
            if (feedback != null) {
                Column {
                    Spacer(Modifier.height(6.dp))
                    Text(feedback, style = MaterialTheme.typography.bodyMedium, color = DsError)
                }
            }
        }
    }
}
