package cl.deepsource.horas.feature.auth.presentation.login
// [GUIA/ARCHIVO] Lógica de autenticación de formulario; delega datos al repositorio.
// [GUIA/UBICACION] app/src/main/java/cl/deepsource/horas/feature/auth/presentation/login/LoginViewModel.kt
// [GUIA/LECTURA] Sigue los comentarios de funcion, datos, botones y flujo; son explicativos y no ejecutables.

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.deepsource.horas.R
import cl.deepsource.horas.appContainer
import cl.deepsource.horas.core.util.Validators
import cl.deepsource.horas.feature.auth.domain.AuthRepository
import cl.deepsource.horas.feature.auth.domain.GoogleSignInResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// [GUIA/TIPO] LoginUiState: Estado completo del formulario y cargas del inicio de sesión.
data class LoginUiState(
    // [GUIA/DETALLE] Correo precargado para que el docente pueda hacer la demostración.
    val email: String = "demo@deepsource.cl",
    // [GUIA/DETALLE] Contraseña precargada de la cuenta demo; no es una práctica recomendada para producción.
    val password: String = "123456",
    // [GUIA/DETALLE] Preferencia visual: esta versión demo no implementa guardar sesión realmente.
    val rememberSession: Boolean = true,
    val passwordVisible: Boolean = false,
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val generalError: Int? = null,
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
) {
    // [GUIA/DETALLE] Bloquea múltiples intentos simultáneos de login normal y Google.
    val isBusy: Boolean get() = isLoading || isGoogleLoading
}

// [GUIA/TIPO] LoginEvent: Evento de una sola vez usado para ordenar navegar tras autenticar.
sealed interface LoginEvent {
    data object LoggedIn : LoginEvent
}

// [GUIA/TIPO] LoginViewModel: Gestiona formularios, validación, login normal/Google y evento de navegación.
class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    // [GUIA/DETALLE] MutableStateFlow interno: es la fuente mutable de datos del formulario.
    private val _state = MutableStateFlow(LoginUiState())
    // [GUIA/DETALLE] Estado expuesto solo en lectura para que Compose lo observe.
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    // [GUIA/DETALLE] Channel envía evento puntual de éxito para navegar una sola vez.
    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // [GUIA/FUNCION] onEmailChange: Actualiza correo y limpia error anterior.
    fun onEmailChange(value: String) = _state.update { it.copy(email = value, emailError = null, generalError = null) }
    // [GUIA/FUNCION] onPasswordChange: Actualiza contraseña y limpia error anterior.
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, passwordError = null, generalError = null) }
    // [GUIA/FUNCION] onTogglePasswordVisibility: Alterna si la contraseña se muestra u oculta.
    fun onTogglePasswordVisibility() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    // [GUIA/FUNCION] onRememberChange: Actualiza casilla Recordar sesión; repositorio demo no la guarda.
    fun onRememberChange(value: Boolean) = _state.update { it.copy(rememberSession = value) }

    // [GUIA/FUNCION] onSubmit: Valida correo/clave, llama al repositorio y emite LoggedIn solo cuando hay éxito.
    fun onSubmit() {
        val current = _state.value
        if (current.isBusy) return

        // [GUIA/DETALLE] Validación local antes de llamar al repositorio.
        val emailError = Validators.email(current.email)
        val passwordError = Validators.password(current.password)
        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, generalError = null) }
            // [GUIA/DETALLE] Capa de datos: realiza la validación de credenciales demo.
            authRepository.signInWithEmail(current.email.trim(), current.password, current.rememberSession)
                // [GUIA/DETALLE] Solamente si el repositorio confirmó éxito, avisa al Route para ir al Home.
                .onSuccess { _events.send(LoginEvent.LoggedIn) }
                .onFailure { _state.update { s -> s.copy(generalError = R.string.error_credentials) } }
            _state.update { it.copy(isLoading = false) }
        }
    }

    // [GUIA/FUNCION] onGoogleStarted: Activa indicador de carga para proceso Google.
    fun onGoogleStarted() = _state.update { it.copy(isGoogleLoading = true, generalError = null) }

    // [GUIA/FUNCION] onGoogleResult: Según resultado Google, crea sesión, limpia carga o expone un mensaje de error.
    fun onGoogleResult(result: GoogleSignInResult) {
        when (result) {
            is GoogleSignInResult.Success -> viewModelScope.launch {
                // [GUIA/DETALLE] Crea sesión de prueba usando la cuenta Google obtenida previamente por CredentialManager.
                authRepository.signInWithGoogle(result.account)
                    // [GUIA/DETALLE] Solamente si el repositorio confirmó éxito, avisa al Route para ir al Home.
                    .onSuccess { _events.send(LoginEvent.LoggedIn) }
                    .onFailure { _state.update { s -> s.copy(generalError = R.string.error_google_unknown) } }
                _state.update { it.copy(isGoogleLoading = false) }
            }
            GoogleSignInResult.Cancelled -> _state.update { it.copy(isGoogleLoading = false) }
            is GoogleSignInResult.Failure -> _state.update {
                it.copy(
                    isGoogleLoading = false,
                    // [GUIA/DETALLE] Mapea error técnico Google a un texto localizado en strings.xml.
                    generalError = when (result.reason) {
                        GoogleSignInResult.Reason.NOT_CONFIGURED -> R.string.error_google_not_configured
                        GoogleSignInResult.Reason.NO_ACCOUNT -> R.string.error_google_no_account
                        GoogleSignInResult.Reason.UNKNOWN -> R.string.error_google_unknown
                    },
                )
            }
        }
    }

    companion object {
        // [GUIA/DETALLE] Fábrica de ViewModel: recibe la implementación desde AppContainer.
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { LoginViewModel(appContainer().authRepository) }
        }
    }
}
