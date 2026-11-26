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

// ForgotPasswordUiState: Correo, errores, carga y destinatario de supuesto envío.
data class ForgotPasswordUiState(
    val email: String = "",
    @StringRes val emailError: Int? = null,
    @StringRes val generalError: Int? = null,
    val isLoading: Boolean = false,
    /** Correo al que se envió el enlace; no nulo cuando el envío fue exitoso. */
    // Al no ser null se muestra mensaje de supuesta recuperación.
    val sentTo: String? = null,
)

// ForgotPasswordViewModel: Valida correo y llama al repositorio demo de recuperación.
class ForgotPasswordViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    // onEmailChange: Guarda correo y borra errores/resultado anterior.
    fun onEmailChange(value: String) =
        _state.update { it.copy(email = value, emailError = null, generalError = null, sentTo = null) }

    // onSubmit: Intenta envío mediante repositorio demo: muestra confirmación aunque no se mande email real.
    fun onSubmit() {
        val current = _state.value
        if (current.isLoading) return
        // Verifica formato; si falla, no llama al repositorio.
        val error = Validators.email(current.email)
        if (error != null) {
            _state.update { it.copy(emailError = error) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, generalError = null) }
            val email = current.email.trim()
            // Acción demo: el repositorio no realiza envío de correo.
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
