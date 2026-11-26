package cl.deepsource.horas.feature.auth.domain
// [GUIA/ARCHIVO] Contrato para acceso a cuenta de Google.
// [GUIA/UBICACION] app/src/main/java/cl/deepsource/horas/feature/auth/domain/GoogleAuthClient.kt
// [GUIA/LECTURA] Sigue los comentarios de funcion, datos, botones y flujo; son explicativos y no ejecutables.

import android.content.Context

/** Abstrae el inicio de sesión con Google para poder probar la UI sin Play Services. */
// [GUIA/TIPO] GoogleAuthClient: Interfaz para solicitar autenticación de Google desde Android.
interface GoogleAuthClient {
    /** @param context debe ser el contexto de una Activity (se muestra una hoja de selección de cuenta). */
    suspend fun signIn(context: Context): GoogleSignInResult
}
