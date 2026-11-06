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
class AppContainer(val context: Context) {
    val authRepository: AuthRepository = FakeAuthRepository()

    val projectRepository: ProjectRepository = LocalProjectRepository(context)

    val googleAuthClient: GoogleAuthClient =
        CredentialManagerGoogleAuthClient(webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID)
}
