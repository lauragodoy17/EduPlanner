package com.softcg.myapplication.data.network

import android.app.Activity
import android.util.Log
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.softcg.myapplication.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Lo que devuelve el selector de cuentas de Google. */
sealed class ResultadoCredencialGoogle {
    data class Token(val idToken: String) : ResultadoCredencialGoogle()

    /** El usuario cerró el selector: no es un error y no se le avisa. */
    object Cancelado : ResultadoCredencialGoogle()

    /** El dispositivo no tiene ninguna cuenta de Google. */
    object SinCuentas : ResultadoCredencialGoogle()

    data class Error(val detalle: String) : ResultadoCredencialGoogle()
}

/**
 * Obtiene el ID token de Google con Credential Manager. El selector necesita la Activity que lo muestra,
 * por eso esto vive fuera del ViewModel: la Activity pide el token y le pasa el resultado al ViewModel.
 * El cliente web (default_web_client_id) lo genera el plugin de Google Services desde google-services.json.
 */
@Singleton
class CredencialGoogleService @Inject constructor(@ApplicationContext private val context: Context) {

    private val credentialManager by lazy { CredentialManager.create(context) }

    suspend fun obtenerIdToken(activity: Activity): ResultadoCredencialGoogle {
        val opcion = GetSignInWithGoogleOption.Builder(activity.getString(R.string.default_web_client_id)).build()
        val solicitud = GetCredentialRequest.Builder().addCredentialOption(opcion).build()

        return try {
            val credencial = credentialManager.getCredential(activity, solicitud).credential
            if (credencial is CustomCredential &&
                credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                ResultadoCredencialGoogle.Token(GoogleIdTokenCredential.createFrom(credencial.data).idToken)
            } else {
                ResultadoCredencialGoogle.Error("Credencial inesperada: ${credencial.type}")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "El estudiante cerró el selector de cuentas", e)
            ResultadoCredencialGoogle.Cancelado
        } catch (e: NoCredentialException) {
            Log.d(TAG, "No hay cuentas de Google en el dispositivo", e)
            ResultadoCredencialGoogle.SinCuentas
        } catch (e: GetCredentialException) {
            ResultadoCredencialGoogle.Error(e.message ?: e.type)
        } catch (e: GoogleIdTokenParsingException) {
            ResultadoCredencialGoogle.Error(e.message ?: "Token de Google inválido")
        }
    }

    /** Al cerrar sesión: sin esto, el siguiente inicio con Google entraría con la misma cuenta sin preguntar. */
    suspend fun olvidarCuenta() {
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
    }
}

private const val TAG = "CredencialGoogle"
