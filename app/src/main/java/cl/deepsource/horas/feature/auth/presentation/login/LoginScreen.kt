package cl.deepsource.horas.feature.auth.presentation.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.deepsource.horas.R
import cl.deepsource.horas.appContainer
import cl.deepsource.horas.widget.DeepSourceAppWidget
import cl.deepsource.horas.core.ui.components.DsBrandHeader
import cl.deepsource.horas.core.ui.components.DsPrimaryButton
import cl.deepsource.horas.core.ui.components.DsScreenBackground
import cl.deepsource.horas.core.ui.components.DsTextField
import cl.deepsource.horas.core.ui.components.GoogleSignInButton
import cl.deepsource.horas.core.ui.theme.DeepSourceTheme
import cl.deepsource.horas.core.ui.theme.DsCyan
import cl.deepsource.horas.core.ui.theme.DsError
import cl.deepsource.horas.core.ui.theme.DsOutline
import cl.deepsource.horas.core.ui.theme.DsSuccess
import cl.deepsource.horas.core.ui.theme.DsSurface
import cl.deepsource.horas.core.ui.theme.DsTextMuted
import kotlinx.coroutines.launch


@Composable
// LoginRoute: Conecta LoginViewModel con Compose, recibe evento LoggedIn y ejecuta navegación.
fun LoginRoute(
    onLoggedIn: () -> Unit,
    onForgotPassword: () -> Unit,
    onRequestAccess: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory),
) {
    // Compose se repinta automáticamente cuando LoginViewModel cambia su StateFlow.
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Obtiene cliente nativo Google desde AppContainer; remember evita recrearlo innecesariamente.
    val googleClient = remember(context) { context.appContainer().googleAuthClient }

    // Escucha evento de login exitoso y llama onLoggedIn: la navegación no vive en el ViewModel.
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                LoginEvent.LoggedIn -> {
                    DeepSourceAppWidget.sendUpdateBroadcast(context)
                    onLoggedIn()
                }
            }
        }
    }

    // La función de dibujo recibe estado y callbacks; no contiene autenticación real.
    LoginScreen(
        state = state,
        // Al escribir el email llama a LoginViewModel.onEmailChange.
        onEmailChange = viewModel::onEmailChange,
        // Al escribir la contraseña llama a LoginViewModel.onPasswordChange.
        onPasswordChange = viewModel::onPasswordChange,
        // El icono del ojo cambia el estado de visibilidad de la clave.
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        // El botón Iniciar sesión inicia validación y autenticación en LoginViewModel.
        onSubmit = viewModel::onSubmit,
        // El botón Continuar con Google abre Credential Manager en una corrutina.
        onGoogleClick = {
            scope.launch {
                // Muestra spinner y deshabilita botones mientras se elige cuenta.
                viewModel.onGoogleStarted()
                // Entrega resultado a LoginViewModel para iniciar sesión o mostrar error.
                viewModel.onGoogleResult(googleClient.signIn(context))
            }
        },
        onForgotPassword = onForgotPassword,
        onRequestAccess = onRequestAccess,
    )
}

/** Pantalla sin estado propio: fácil de previsualizar y probar. */
@Composable
// LoginScreen: Dibuja campos, validaciones y botones; delega acciones mediante callbacks al Route/ViewModel.
// La función de dibujo recibe estado y callbacks; no contiene autenticación real.
fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onGoogleClick: () -> Unit,
    onForgotPassword: () -> Unit,
    onRequestAccess: () -> Unit,
) {
    // Usa el fondo degradado común de toda la aplicación.
    DsScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 24.dp),
        ) {
            DsBrandHeader(iconSize = 44.dp)
            Spacer(Modifier.height(32.dp))

            Text(
                stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(28.dp))

            Surface(
                shape = MaterialTheme.shapes.large,
                color = DsSurface.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, DsOutline),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Campo personalizado: su valor viene del estado y sus cambios salen por un callback.
                    DsTextField(
                        value = state.email,
                        // onValueChange: recibe el nuevo texto y comunica el cambio al estado; delega en onEmailChange.
                        onValueChange = onEmailChange,
                        label = stringResource(R.string.field_email),
                        placeholder = stringResource(R.string.field_email_placeholder),
                        leadingIcon = Icons.Outlined.Email,
                        error = state.emailError,
                        enabled = !state.isBusy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    )
                    // Campo personalizado: su valor viene del estado y sus cambios salen por un callback.
                    DsTextField(
                        value = state.password,
                        // onValueChange: recibe el nuevo texto y comunica el cambio al estado; delega en onPasswordChange.
                        onValueChange = onPasswordChange,
                        label = stringResource(R.string.field_password),
                        leadingIcon = Icons.Outlined.Lock,
                        error = state.passwordError,
                        enabled = !state.isBusy,
                        visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        // La tecla Listo del teclado ejecuta lo mismo que Iniciar sesión.
                        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                        trailingIcon = {
                            // Botón del ojo: cambia entre texto plano y contraseña oculta.
                            IconButton(onClick = onTogglePasswordVisibility) {
                                Icon(
                                    imageVector = if (state.passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = stringResource(
                                        if (state.passwordVisible) R.string.cd_hide_password else R.string.cd_show_password,
                                    ),
                                )
                            }
                        },
                    )

                    Text(
                        "Tu sesión permanecerá iniciada hasta que pulses Cerrar sesión en Perfil.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DsTextMuted,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            // Botón ¿Olvidaste tu contraseña?: abre la ruta FORGOT_PASSWORD.
                            onClick = onForgotPassword,
                            contentPadding = PaddingValues(horizontal = 4.dp),
                        ) {
                            Text(
                                stringResource(R.string.login_forgot),
                                color = DsCyan,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                            )
                        }
                    }

                    // Solo presenta el recuadro de error si el ViewModel ha informado uno.
                    state.generalError?.let {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = DsError.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, DsError.copy(alpha = 0.5f)),
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = DsError,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(stringResource(it), color = DsError, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    DsPrimaryButton(
                        text = stringResource(R.string.login_submit),
                        // Botón Iniciar sesión: dispara LoginViewModel.onSubmit.
                        onClick = onSubmit,
                        loading = state.isLoading,
                        loadingText = stringResource(R.string.login_loading),
                        enabled = !state.isGoogleLoading,
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(Modifier.weight(1f), color = DsOutline)
                        Text(
                            stringResource(R.string.login_or),
                            modifier = Modifier.padding(horizontal = 12.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = DsTextMuted,
                        )
                        HorizontalDivider(Modifier.weight(1f), color = DsOutline)
                    }

                    GoogleSignInButton(
                        text = stringResource(R.string.login_google),
                        // Botón Google: invoca el flujo nativo de cuenta Google.
                        onClick = onGoogleClick,
                        loading = state.isGoogleLoading,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.login_no_account), style = MaterialTheme.typography.bodyMedium, color = DsTextMuted)
                // Botón Solicitar acceso: navega a REQUEST_ACCESS.
                TextButton(onClick = onRequestAccess) {
                    Text(stringResource(R.string.login_request_access), color = DsCyan)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Shield, contentDescription = null, tint = DsSuccess, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.login_secure),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DsTextMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
// LoginScreenPreview: Previsualización de diseño en Android Studio; no usa servicios reales.
private fun LoginScreenPreview() {
    DeepSourceTheme {
        // La función de dibujo recibe estado y callbacks; no contiene autenticación real.
        LoginScreen(
            state = LoginUiState(email = "demo@deepsource.cl"),
            onEmailChange = {}, onPasswordChange = {}, onTogglePasswordVisibility = {},
            // El botón Continuar con Google abre Credential Manager en una corrutina.
            onSubmit = {}, onGoogleClick = {},
            onForgotPassword = {}, onRequestAccess = {},
        )
    }
}