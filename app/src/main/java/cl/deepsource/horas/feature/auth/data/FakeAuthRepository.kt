package cl.deepsource.horas.feature.auth.data
// [GUIA/ARCHIVO] Repositorio de autenticación de demo: no existe conexión a servidor.
// [GUIA/UBICACION] app/src/main/java/cl/deepsource/horas/feature/auth/data/FakeAuthRepository.kt
// [GUIA/LECTURA] Sigue los comentarios de funcion, datos, botones y flujo; son explicativos y no ejecutables.

import cl.deepsource.horas.feature.auth.domain.AccessRequest
import cl.deepsource.horas.feature.auth.domain.AuthRepository
import cl.deepsource.horas.feature.auth.domain.GoogleAccount
import cl.deepsource.horas.feature.auth.domain.User
import cl.deepsource.horas.feature.auth.domain.UserRole
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// [GUIA/TIPO] FakeAuthRepository: Implementación de demostración: login simulado y acciones sin backend de cuentas.
class FakeAuthRepository : AuthRepository {

    // [GUIA/DETALLE] MutableStateFlow guarda la sesión en memoria; al reiniciar el proceso se pierde.
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // [GUIA/FUNCION] signInWithEmail: Solo acepta demo@deepsource.cl / 123456; rememberSession no se persiste en esta versión.
    override suspend fun signInWithEmail(email: String, password: String, rememberSession: Boolean): Result<User> {
        // [GUIA/DETALLE] Simula latencia de red; no se hace una petición HTTP real.
        delay(FAKE_LATENCY_MS)
        // [V5/ROLES] Dos cuentas locales demostrativas; la administración NO tiene pestaña.
        val isNormal = email.trim().equals("demo@deepsource.cl", ignoreCase = true) && password == "123456"
        val isAdmin = email.trim().equals("admin@deepsource.cl", ignoreCase = true) && password == "admin123"
        if (isNormal || isAdmin) {
            val user = User(id = email, name = if (isAdmin) "Administrador Demo" else "Usuario Demo",
                email = email, role = if (isAdmin) UserRole.ADMIN else UserRole.NORMAL)
            _currentUser.value = user
            return Result.success(user)
        }
        return Result.failure(IllegalArgumentException("Credenciales incorrectas"))
    }

    // [GUIA/FUNCION] signInWithGoogle: Acepta datos de cuenta que devuelve Google y crea sesión en memoria; no comprueba token en servidor.
    // [GUIA/DETALLE] IMPORTANTE: la cuenta viene de Google, pero esta capa demo no valida el ID token en backend.
    override suspend fun signInWithGoogle(account: GoogleAccount): Result<User> {
        // [GUIA/DETALLE] Simula latencia de red; no se hace una petición HTTP real.
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

    // [GUIA/FUNCION] sendPasswordReset: SIMULADO: espera y devuelve éxito; no envía correo ni genera enlace real.
    // [GUIA/DETALLE] Solo retorna éxito local para la demo; no hay enlace ni correo enviado.
    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        // [GUIA/DETALLE] Simula latencia de red; no se hace una petición HTTP real.
        delay(FAKE_LATENCY_MS)
        return Result.success(Unit)
    }

    // [GUIA/FUNCION] requestAccess: SIMULADO: devuelve éxito sin enviar solicitud a un servidor.
    // [GUIA/DETALLE] Solo retorna éxito local para la demo; no queda una solicitud en servidor.
    override suspend fun requestAccess(request: AccessRequest): Result<Unit> {
        // [GUIA/DETALLE] Simula latencia de red; no se hace una petición HTTP real.
        delay(FAKE_LATENCY_MS)
        return Result.success(Unit)
    }

    // [GUIA/FUNCION] signOut: Borra el usuario de memoria para cerrar sesión.
    override fun signOut() {
        // [GUIA/DETALLE] StateFlow queda vacío: la interfaz lo interpreta como sesión cerrada.
        _currentUser.value = null
    }

    // [GUIA/FUNCION] nameFromEmail: Helper sin uso actual: arma un nombre legible a partir del correo.
    private fun nameFromEmail(email: String): String =
        email.substringBefore('@')
            .split('.', '_', '-')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }

    private companion object {
        const val FAKE_LATENCY_MS = 800L
    }
}
