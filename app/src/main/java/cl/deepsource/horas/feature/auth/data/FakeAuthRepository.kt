package cl.deepsource.horas.feature.auth.data

import cl.deepsource.horas.feature.auth.domain.AccessRequest
import cl.deepsource.horas.feature.auth.domain.AuthRepository
import cl.deepsource.horas.feature.auth.domain.GoogleAccount
import cl.deepsource.horas.feature.auth.domain.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAuthRepository : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun signInWithEmail(email: String, password: String, rememberSession: Boolean): Result<User> {
        delay(FAKE_LATENCY_MS)
        if (email.trim().equals("demo@deepsource.cl", ignoreCase = true) && password == "123456") {
            val user = User(id = email, name = "Usuario Demo", email = email)
            _currentUser.value = user
            return Result.success(user)
        }
        return Result.failure(IllegalArgumentException("Credenciales incorrectas"))
    }

    override suspend fun signInWithGoogle(account: GoogleAccount): Result<User> {
        delay(FAKE_LATENCY_MS)
        val user = User(
            id = account.email,
            name = account.displayName,
            email = account.email,
            avatarUrl = account.photoUrl,
        )
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        delay(FAKE_LATENCY_MS)
        return Result.success(Unit)
    }

    override suspend fun requestAccess(request: AccessRequest): Result<Unit> {
        delay(FAKE_LATENCY_MS)
        return Result.success(Unit)
    }

    override fun signOut() {
        _currentUser.value = null
    }

    private fun nameFromEmail(email: String): String =
        email.substringBefore('@')
            .split('.', '_', '-')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }

    private companion object {
        const val FAKE_LATENCY_MS = 800L
    }
}
