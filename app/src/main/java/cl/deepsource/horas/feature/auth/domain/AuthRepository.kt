package cl.deepsource.horas.feature.auth.domain
// [GUIA/ARCHIVO] Interfaz de autenticación, separada de la implementación fake.
// [GUIA/UBICACION] app/src/main/java/cl/deepsource/horas/feature/auth/domain/AuthRepository.kt
// [GUIA/LECTURA] Sigue los comentarios de funcion, datos, botones y flujo; son explicativos y no ejecutables.

import kotlinx.coroutines.flow.StateFlow

// [GUIA/TIPO] AuthRepository: Interfaz del servicio de autenticación; permite cambiar el fake por backend real.
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
