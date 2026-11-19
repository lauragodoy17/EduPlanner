package com.softcg.myapplication.data.network

import android.util.Log
import com.softcg.myapplication.data.response.LoginResult
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class AuthenticationService @Inject constructor(private val firebase: FirebaseClient) {


    suspend fun login(email: String, password: String): LoginResult = runCatching {
        firebase.auth.signInWithEmailAndPassword(email, password).await()
    }.toLoginResult()

    /** Canjea el ID token de Google por una sesión de Firebase; la primera vez crea la cuenta. */
    suspend fun loginConGoogle(idToken: String): LoginResult = runCatching {
        firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    }.toLoginResult()

    /**
     * Crea la cuenta. Nunca lanza: Firebase rechaza el registro con excepciones (correo ya usado, contraseña débil,
     * sin conexión) y, sin capturarlas, cerraban la app.
     */
    suspend fun createAccount(email: String, password: String): ResultadoRegistro = try {
        firebase.auth.createUserWithEmailAndPassword(email, password).await()
        ResultadoRegistro.Creada
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseAuthUserCollisionException) {
        ResultadoRegistro.CorreoEnUso
    } catch (e: FirebaseAuthWeakPasswordException) {
        ResultadoRegistro.ContrasenaDebil
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        ResultadoRegistro.CorreoInvalido
    } catch (e: FirebaseNetworkException) {
        ResultadoRegistro.SinConexion
    } catch (e: Exception) {
        Log.w("AuthenticationService", "No se pudo crear la cuenta", e)
        ResultadoRegistro.Error
    }

    private fun Result<AuthResult>.toLoginResult() = when (val result = getOrNull()) {
        null -> LoginResult.Error
        else -> {
            val userId = result.user
            checkNotNull(userId)
            LoginResult.Success(result.user?.isEmailVerified ?: false)
        }
    }


}

/** Cómo terminó el registro con correo y contraseña. */
sealed class ResultadoRegistro {
    object Creada : ResultadoRegistro()
    object CorreoEnUso : ResultadoRegistro()
    object ContrasenaDebil : ResultadoRegistro()
    object CorreoInvalido : ResultadoRegistro()
    object SinConexion : ResultadoRegistro()
    object Error : ResultadoRegistro()

    /** Texto para el estudiante; null si se creó. */
    val mensaje: String?
        get() = when (this) {
            Creada -> null
            CorreoEnUso -> "Ya existe una cuenta con ese correo. Inicia sesión o usa otro correo."
            ContrasenaDebil -> "La contraseña es muy débil: usa al menos 6 caracteres, mezclando letras y números."
            CorreoInvalido -> "El correo no es válido. Revisa que esté bien escrito."
            SinConexion -> "No hay conexión a internet. Revisa tu conexión e inténtalo de nuevo."
            Error -> "No hemos podido crear tu cuenta, inténtalo de nuevo más tarde."
        }
}
