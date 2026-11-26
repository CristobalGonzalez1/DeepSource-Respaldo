package cl.deepsource.horas.feature.auth.presentation.forgot

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.deepsource.horas.R
import cl.deepsource.horas.core.ui.components.DsFormScaffold
import cl.deepsource.horas.core.ui.components.DsPrimaryButton
import cl.deepsource.horas.core.ui.components.DsTextField
import cl.deepsource.horas.core.ui.theme.DsError
import cl.deepsource.horas.core.ui.theme.DsSuccess

@Composable
// ForgotPasswordRoute: Pantalla de recuperar contraseña; recoge email y muestra respuesta del ViewModel.
fun ForgotPasswordRoute(
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel(factory = ForgotPasswordViewModel.Factory),
) {
    // Lee el resultado del ViewModel; al cambiar, actualiza los textos en pantalla.
    val state by viewModel.state.collectAsStateWithLifecycle()
    DsFormScaffold(
        title = stringResource(R.string.forgot_title),
        subtitle = stringResource(R.string.forgot_subtitle),
        onBack = onBack,
    ) {
        DsTextField(
            value = state.email,
            // Campo correo manda texto al ViewModel.
            onValueChange = viewModel::onEmailChange,
            label = stringResource(R.string.field_email),
            placeholder = stringResource(R.string.field_email_placeholder),
            leadingIcon = Icons.Outlined.Email,
            error = state.emailError,
            enabled = !state.isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.onSubmit() }),
        )
        state.generalError?.let { Text(stringResource(it), color = DsError, style = MaterialTheme.typography.bodyMedium) }
        DsPrimaryButton(
            text = stringResource(R.string.forgot_submit),
            // Botón Enviar enlace: valida email y ejecuta repositorio SIMULADO.
            onClick = viewModel::onSubmit,
            loading = state.isLoading,
        )
        state.sentTo?.let {
            Text(stringResource(R.string.forgot_sent, it), color = DsSuccess, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
