package cl.deepsource.horas.feature.auth.domain

/** [V5/ROLES] Roles locales de demostración. Autorización real requeriría backend. */
enum class UserRole { NORMAL, ADMIN }

// User: Identidad mostrada en el Home.
data class User(
    val id: String,
    val name: String,
    val email: String,
    // avatarUrl: URL opcional de foto de perfil.
    val avatarUrl: String? = null,
    val role: UserRole = UserRole.NORMAL,
)

// GoogleAccount: Información obtenida de credenciales Google (incluye token ID).
data class GoogleAccount(
    // idToken: Token de Google recibido; aquí no existe verificación de servidor.
    val idToken: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
)

// AccessRequest: Datos capturados para una solicitud de acceso.
data class AccessRequest(
    val fullName: String,
    val email: String,
    val project: String,
)

// GoogleSignInResult: Resultado tipado del intento Google: éxito, cancelación o error.
sealed interface GoogleSignInResult {
    data class Success(val account: GoogleAccount) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data class Failure(val reason: Reason) : GoogleSignInResult

    enum class Reason { NOT_CONFIGURED, NO_ACCOUNT, UNKNOWN }
}
