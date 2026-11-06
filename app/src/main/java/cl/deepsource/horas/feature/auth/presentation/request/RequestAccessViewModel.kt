package cl.deepsource.horas.feature.auth.presentation.request

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.deepsource.horas.R
import cl.deepsource.horas.appContainer
import cl.deepsource.horas.core.util.Validators
import cl.deepsource.horas.feature.auth.domain.AccessRequest
import cl.deepsource.horas.feature.auth.domain.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// TODO: obtener la lista de proyectos desde el backend.
val AvailableProjects = listOf("Portal Cliente AFC", "Automatización QA", "Mesa Operaciones")

data class RequestAccessUiState(
    val fullName: String = "",
    val email: String = "",
    val project: String = AvailableProjects.first(),
    @StringRes val nameError: Int? = null,
    @StringRes val emailError: Int? = null,
    @StringRes val generalError: Int? = null,
    val isLoading: Boolean = false,
    val sentTo: String? = null,
)

class RequestAccessViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(RequestAccessUiState())
    val state: StateFlow<RequestAccessUiState> = _state.asStateFlow()

    fun onNameChange(v: String) = _state.update { it.copy(fullName = v, nameError = null, sentTo = null) }
    fun onEmailChange(v: String) = _state.update { it.copy(email = v, emailError = null, sentTo = null) }
    fun onProjectChange(v: String) = _state.update { it.copy(project = v, sentTo = null) }

    fun onSubmit() {
        val current = _state.value
        if (current.isLoading) return
        val nameError = Validators.required(current.fullName)
        val emailError = Validators.email(current.email)
        if (nameError != null || emailError != null) {
            _state.update { it.copy(nameError = nameError, emailError = emailError) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, generalError = null) }
            val request = AccessRequest(current.fullName.trim(), current.email.trim(), current.project)
            authRepository.requestAccess(request)
                .onSuccess { _state.update { s -> s.copy(sentTo = request.email) } }
                .onFailure { _state.update { s -> s.copy(generalError = R.string.error_generic) } }
            _state.update { it.copy(isLoading = false) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { RequestAccessViewModel(appContainer().authRepository) }
        }
    }
}
