package cl.deepsource.horas.core.di

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
// AppContainer: Inyección de dependencias manual: crea las implementaciones usadas por los ViewModels.
class AppContainer(val context: Context) {
    // La autenticación implementada aquí es FakeAuthRepository con perfil persistente local para demo; no es autenticación con servidor.
    val authRepository: AuthRepository = FakeAuthRepository(context)

    // Este repositorio sí persiste datos del dispositivo usando SharedPreferences.
    val projectRepository: ProjectRepository = LocalProjectRepository(context)

    // Adaptador a Google Credential Manager; necesita el ID web configurado en Gradle.
    val googleAuthClient: GoogleAuthClient =
        CredentialManagerGoogleAuthClient(webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID)
}
