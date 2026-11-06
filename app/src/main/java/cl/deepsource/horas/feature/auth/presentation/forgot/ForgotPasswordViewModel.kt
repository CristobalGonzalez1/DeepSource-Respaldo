package cl.deepsource.horas.feature.auth.presentation.forgot

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val email: String = "",
    @StringRes val emailError: Int? = null,
    @StringRes val generalError: Int? = null,
    val isLoading: Boolean = false,
    /** Correo al que se envió el enlace; no nulo cuando el envío fue exitoso. */
    val sentTo: String? = null,
)

class ForgotPasswordViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) =
        _state.update { it.copy(email = value, emailError = null, generalError = null, sentTo = null) }

    fun onSubmit() {
        val current = _state.value
        if (current.isLoading) return
        val error = Validators.email(current.email)
        if (error != null) {
            _state.update { it.copy(emailError = error) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, generalError = null) }
            val email = current.email.trim()
            authRepository.sendPasswordReset(email)
                .onSuccess { _state.update { s -> s.copy(sentTo = email) } }
                .onFailure { _state.update { s -> s.copy(generalError = R.string.error_generic) } }
            _state.update { it.copy(isLoading = false) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ForgotPasswordViewModel(appContainer().authRepository) }
        }
    }
}
