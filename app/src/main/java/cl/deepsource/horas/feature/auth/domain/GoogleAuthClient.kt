package cl.deepsource.horas.feature.auth.domain

import android.content.Context

/** Abstrae el inicio de sesión con Google para poder probar la UI sin Play Services. */
interface GoogleAuthClient {
    /** @param context debe ser el contexto de una Activity (se muestra una hoja de selección de cuenta). */
    suspend fun signIn(context: Context): GoogleSignInResult
}
