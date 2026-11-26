package cl.deepsource.horas.core.di
// [GUIA/ARCHIVO] Instancia repositorios y GoogleAuthClient para toda la app.
// [GUIA/UBICACION] app/src/main/java/cl/deepsource/horas/core/di/AppContainer.kt
// [GUIA/LECTURA] Sigue los comentarios de funcion, datos, botones y flujo; son explicativos y no ejecutables.

import android.content.Context
import cl.deepsource.horas.BuildConfig
import cl.deepsource.horas.data.repository.LocalProjectRepository
import cl.deepsource.horas.domain.repository.ProjectRepository
import cl.deepsource.horas.feature.auth.data.CredentialManagerGoogleAuthClient
import cl.deepsource.horas.feature.auth.data.FakeAuthRepository
import cl.deepsource.horas.feature.auth.domain.AuthRepository
import cl.deepsource.horas.feature.auth.domain.GoogleAuthClient

/**
 * Contenedor de dependencias manual.
 */
// [GUIA/TIPO] AppContainer: Inyección de dependencias manual: crea las implementaciones usadas por los ViewModels.
class AppContainer(val context: Context) {
    // [GUIA/DETALLE] La autenticación implementada aquí es FakeAuthRepository: sirve para la demo, no es una base de usuarios real.
    val authRepository: AuthRepository = FakeAuthRepository()

    // [GUIA/DETALLE] Este repositorio sí persiste datos del dispositivo usando SharedPreferences.
    val projectRepository: ProjectRepository = LocalProjectRepository(context)

    // [GUIA/DETALLE] Adaptador a Google Credential Manager; necesita el ID web configurado en Gradle.
    val googleAuthClient: GoogleAuthClient =
        CredentialManagerGoogleAuthClient(webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID)
}
