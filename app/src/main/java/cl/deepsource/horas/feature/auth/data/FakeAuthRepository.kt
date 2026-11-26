package cl.deepsource.horas.feature.auth.data

import android.content.Context
import cl.deepsource.horas.feature.auth.domain.AccessRequest
import cl.deepsource.horas.feature.auth.domain.AuthRepository
import cl.deepsource.horas.feature.auth.domain.GoogleAccount
import cl.deepsource.horas.feature.auth.domain.User
import cl.deepsource.horas.feature.auth.domain.UserRole
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * [V9/SESION] Autenticación de DEMOSTRACIÓN persistente.
 * Guarda solo la identidad/perfil y el origen de sesión en preferencias privadas:
 * NO almacena claves ni tokens, ni constituye una autenticación segura de producción.
 * La sesión vuelve después de cerrar la app / reiniciar Android, hasta Cerrar sesión.
 */
class FakeAuthRepository(context: Context) : AuthRepository {
    private val prefs = context.applicationContext.getSharedPreferences(
        "deepsource_demo_session_v9", Context.MODE_PRIVATE
    )

    // La restauración es sincrónica: NavHost obtiene la sesión ANTES de elegir su pantalla inicial.
    private val _currentUser = MutableStateFlow(restoreSavedUser())
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun signInWithEmail(email: String, password: String, rememberSession: Boolean): Result<User> {
        delay(FAKE_LATENCY_MS)
        // V9: permanecer conectado es obligatorio en la demo, independientemente del antiguo checkbox.
        val normalized = email.trim().lowercase(Locale.ROOT)
        val isAdmin = normalized == ADMIN_EMAIL && password == "admin123"
        val isNormal = normalized == NORMAL_EMAIL && password == "123456"
        if (!isAdmin && !isNormal) return Result.failure(IllegalArgumentException("Credenciales incorrectas"))
        val user = demoUser(normalized) ?: return Result.failure(IllegalArgumentException("Cuenta inválida"))
        if (!prefs.edit().putString(KEY_PROVIDER, PROVIDER_DEMO)
                .putString(KEY_EMAIL, normalized)
                .remove(KEY_NAME).remove(KEY_AVATAR).commit()) {
            return Result.failure(IllegalStateException("No se pudo guardar la sesión"))
        }
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun signInWithGoogle(account: GoogleAccount): Result<User> {
        delay(FAKE_LATENCY_MS)
        val email = account.email.trim()
        if (email.isBlank()) return Result.failure(IllegalArgumentException("Correo Google vacío"))
        val user = User(
            id = email, name = account.displayName.ifBlank { email.substringBefore('@') },
            email = email, avatarUrl = account.photoUrl, role = UserRole.NORMAL
        )
        // Google permanece con rol NORMAL; nunca obtiene permisos ADMIN por coincidir el correo.
        if (!prefs.edit().putString(KEY_PROVIDER, PROVIDER_GOOGLE)
                .putString(KEY_EMAIL, user.email).putString(KEY_NAME, user.name)
                .putString(KEY_AVATAR, user.avatarUrl).commit()) {
            return Result.failure(IllegalStateException("No se pudo guardar la sesión"))
        }
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        delay(FAKE_LATENCY_MS)
        return Result.success(Unit) // DEMO: no envía correo
    }

    override suspend fun requestAccess(request: AccessRequest): Result<Unit> {
        delay(FAKE_LATENCY_MS)
        return Result.success(Unit) // DEMO: no envía solicitud
    }

    override fun signOut() {
        // commit() inmediato, antes de borrar el estado observable y regresar a Login.
        prefs.edit().clear().commit()
        _currentUser.value = null
    }

    private fun restoreSavedUser(): User? {
        val email = prefs.getString(KEY_EMAIL, null)?.trim().orEmpty()
        return when (prefs.getString(KEY_PROVIDER, null)) {
            PROVIDER_DEMO -> demoUser(email.lowercase(Locale.ROOT))
            PROVIDER_GOOGLE -> if (email.isBlank()) null else User(
                id = email, email = email,
                name = prefs.getString(KEY_NAME, null)?.takeIf { it.isNotBlank() }
                    ?: email.substringBefore('@'),
                avatarUrl = prefs.getString(KEY_AVATAR, null),
                role = UserRole.NORMAL
            )
            else -> null
        }
    }

    private fun demoUser(email: String): User? = when (email) {
        ADMIN_EMAIL -> User(id = email, email = email, name = "Administrador Demo", role = UserRole.ADMIN)
        NORMAL_EMAIL -> User(id = email, email = email, name = "Usuario Demo", role = UserRole.NORMAL)
        else -> null
    }

    private companion object {
        const val ADMIN_EMAIL = "admin@deepsource.cl"
        const val NORMAL_EMAIL = "demo@deepsource.cl"
        const val PROVIDER_DEMO = "demo"
        const val PROVIDER_GOOGLE = "google_demo"
        const val KEY_PROVIDER = "provider"
        const val KEY_EMAIL = "email"
        const val KEY_NAME = "name"
        const val KEY_AVATAR = "avatar"
        const val FAKE_LATENCY_MS = 800L
    }
}
