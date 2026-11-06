package cl.deepsource.horas.feature.auth.domain

import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    /** Usuario con sesión activa, o null si no hay sesión. */
    val currentUser: StateFlow<User?>

    suspend fun signInWithEmail(email: String, password: String, rememberSession: Boolean): Result<User>

    /** Intercambia el idToken de Google por una sesión de DeepSource (validación en el backend). */
    suspend fun signInWithGoogle(account: GoogleAccount): Result<User>

    suspend fun sendPasswordReset(email: String): Result<Unit>

    suspend fun requestAccess(request: AccessRequest): Result<Unit>

    fun signOut()
}
