package cl.deepsource.horas.feature.auth.presentation.login

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

data class LoginUiState(
    val email: String = "demo@deepsource.cl",
    val password: String = "123456",
    val rememberSession: Boolean = true,
    val passwordVisible: Boolean = false,
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val generalError: Int? = null,
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
) {
    val isBusy: Boolean get() = isLoading || isGoogleLoading
}

sealed interface LoginEvent {
    data object LoggedIn : LoginEvent
}

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, emailError = null, generalError = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, passwordError = null, generalError = null) }
    fun onTogglePasswordVisibility() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    fun onRememberChange(value: Boolean) = _state.update { it.copy(rememberSession = value) }

    fun onSubmit() {
        val current = _state.value
        if (current.isBusy) return

        val emailError = Validators.email(current.email)
        val passwordError = Validators.password(current.password)
        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, generalError = null) }
            authRepository.signInWithEmail(current.email.trim(), current.password, current.rememberSession)
                .onSuccess { _events.send(LoginEvent.LoggedIn) }
                .onFailure { _state.update { s -> s.copy(generalError = R.string.error_credentials) } }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun onGoogleStarted() = _state.update { it.copy(isGoogleLoading = true, generalError = null) }

    fun onGoogleResult(result: GoogleSignInResult) {
        when (result) {
            is GoogleSignInResult.Success -> viewModelScope.launch {
                authRepository.signInWithGoogle(result.account)
                    .onSuccess { _events.send(LoginEvent.LoggedIn) }
                    .onFailure { _state.update { s -> s.copy(generalError = R.string.error_google_unknown) } }
                _state.update { it.copy(isGoogleLoading = false) }
            }
            GoogleSignInResult.Cancelled -> _state.update { it.copy(isGoogleLoading = false) }
            is GoogleSignInResult.Failure -> _state.update {
                it.copy(
                    isGoogleLoading = false,
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
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { LoginViewModel(appContainer().authRepository) }
        }
    }
}
