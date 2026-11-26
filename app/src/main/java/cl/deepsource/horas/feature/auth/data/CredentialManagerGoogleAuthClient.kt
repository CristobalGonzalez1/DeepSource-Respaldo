package cl.deepsource.horas.feature.auth.data
// [GUIA/ARCHIVO] Integración Android con selector de cuenta Google (Credential Manager).
// [GUIA/UBICACION] app/src/main/java/cl/deepsource/horas/feature/auth/data/CredentialManagerGoogleAuthClient.kt
// [GUIA/LECTURA] Sigue los comentarios de funcion, datos, botones y flujo; son explicativos y no ejecutables.

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
// [GUIA/DETALLE] Usuario canceló: no debe tratarse como contraseña incorrecta.
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
// [GUIA/DETALLE] Android no encontró cuenta apta para completar la operación.
import androidx.credentials.exceptions.NoCredentialException
import cl.deepsource.horas.feature.auth.domain.GoogleAccount
import cl.deepsource.horas.feature.auth.domain.GoogleAuthClient
import cl.deepsource.horas.feature.auth.domain.GoogleSignInResult
import cl.deepsource.horas.feature.auth.domain.GoogleSignInResult.Reason
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException


// [GUIA/TIPO] CredentialManagerGoogleAuthClient: Adaptador Android Credential Manager para obtener la cuenta Google desde el dispositivo.
class CredentialManagerGoogleAuthClient(
    private val webClientId: String,
) : GoogleAuthClient {

    // [GUIA/FUNCION] signIn: Solicita credenciales Google y devuelve Success, Cancelled o Failure; necesita GOOGLE_WEB_CLIENT_ID.
    override suspend fun signIn(context: Context): GoogleSignInResult {
        // [GUIA/DETALLE] Sin client ID, devuelve error de configuración antes de abrir selector de Google.
        if (webClientId.isBlank()) return GoogleSignInResult.Failure(Reason.NOT_CONFIGURED)

        // [GUIA/DETALLE] Construye solicitud al proveedor de cuentas de Google.
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()

        return try {
            // [GUIA/DETALLE] Abre flujo nativo de Google en el dispositivo y obtiene una credencial.
            val credential = CredentialManager.create(context).getCredential(context, request).credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ){
                // [GUIA/DETALLE] Interpreta credencial y recupera idToken, correo y nombre.
                val google = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleSignInResult.Success(
                    GoogleAccount(
                        idToken = google.idToken,
                        email = google.id,
                        displayName = google.displayName ?: google.id,
                        photoUrl = google.profilePictureUri?.toString(),
                    ),
                )
            } else {
                GoogleSignInResult.Failure(Reason.UNKNOWN)
            }
        // [GUIA/DETALLE] Usuario canceló: no debe tratarse como contraseña incorrecta.
        } catch (e: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        // [GUIA/DETALLE] Android no encontró cuenta apta para completar la operación.
        } catch (e: NoCredentialException) {
            GoogleSignInResult.Failure(Reason.NO_ACCOUNT)
        } catch (e: GetCredentialException) {
            GoogleSignInResult.Failure(Reason.UNKNOWN)
        } catch (e: GoogleIdTokenParsingException) {
            GoogleSignInResult.Failure(Reason.UNKNOWN)
        }
    }
}
