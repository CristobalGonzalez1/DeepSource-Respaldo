package cl.deepsource.horas.feature.auth.domain

data class User(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
)

data class GoogleAccount(
    val idToken: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
)

data class AccessRequest(
    val fullName: String,
    val email: String,
    val project: String,
)

sealed interface GoogleSignInResult {
    data class Success(val account: GoogleAccount) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data class Failure(val reason: Reason) : GoogleSignInResult

    enum class Reason { NOT_CONFIGURED, NO_ACCOUNT, UNKNOWN }
}
