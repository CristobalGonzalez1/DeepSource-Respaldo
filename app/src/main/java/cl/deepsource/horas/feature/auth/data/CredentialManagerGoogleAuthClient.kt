package cl.deepsource.horas.feature.auth.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import cl.deepsource.horas.feature.auth.domain.GoogleAccount
import cl.deepsource.horas.feature.auth.domain.GoogleAuthClient
import cl.deepsource.horas.feature.auth.domain.GoogleSignInResult
import cl.deepsource.horas.feature.auth.domain.GoogleSignInResult.Reason
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException


class CredentialManagerGoogleAuthClient(
    private val webClientId: String,
) : GoogleAuthClient {

    override suspend fun signIn(context: Context): GoogleSignInResult {
        if (webClientId.isBlank()) return GoogleSignInResult.Failure(Reason.NOT_CONFIGURED)

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()

        return try {
            val credential = CredentialManager.create(context).getCredential(context, request).credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ){
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
        } catch (e: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            GoogleSignInResult.Failure(Reason.NO_ACCOUNT)
        } catch (e: GetCredentialException) {
            GoogleSignInResult.Failure(Reason.UNKNOWN)
        } catch (e: GoogleIdTokenParsingException) {
            GoogleSignInResult.Failure(Reason.UNKNOWN)
        }
    }
}
