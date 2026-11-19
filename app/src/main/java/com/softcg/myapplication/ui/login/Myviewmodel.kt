package com.softcg.myapplication.ui.login

import android.util.Log
import android.util.Patterns
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.R
import com.softcg.myapplication.core.Event
import com.softcg.myapplication.data.network.ResultadoCredencialGoogle
import com.softcg.myapplication.data.response.LoginResult
import com.softcg.myapplication.domain.LoginConGoogleUseCase
import com.softcg.myapplication.domain.LoginUseCase
import com.softcg.myapplication.ui.login.model.UserLogin
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MiViewModel @Inject constructor(
    val loginUseCase: LoginUseCase,
    private val loginConGoogleUseCase: LoginConGoogleUseCase
) :ViewModel() {

    private companion object{
        const val MIN_PASSWORD_LENGHT = 6
        const val TAG = "LoginGoogle"
    }

    private val _navigateToHome = MutableLiveData<Event<Boolean>>()
    val navigateToHome: LiveData<Event<Boolean>>
        get() = _navigateToHome

    private val _navigateToRegister=MutableLiveData<Event<Boolean>>()
    val navigateToRegister: LiveData<Event<Boolean>>
        get() = _navigateToRegister

    private val _viewState = MutableStateFlow(LoginViewState())
    val viewState: StateFlow<LoginViewState>
        get() = _viewState

    private var _showErrorDialog = MutableLiveData(UserLogin())
    val showErrorDialog: LiveData<UserLogin>
        get() = _showErrorDialog

    /** La Activity abre el selector de cuentas de Google: necesita su contexto, el ViewModel no lo tiene. */
    private val _solicitarCredencialGoogle = MutableLiveData<Event<Unit>>()
    val solicitarCredencialGoogle: LiveData<Event<Unit>>
        get() = _solicitarCredencialGoogle

    private val _mensajeError = MutableLiveData<Event<Int>>()
    val mensajeError: LiveData<Event<Int>>
        get() = _mensajeError

    fun onLoginSelected(email: String,password: String){
        if (_viewState.value.isLoading) return
        val correo = email.trim()
        if (correo.isEmpty() || password.isEmpty()) {
            // Vacíos no son "válidos": se marcan en vez de mandar una petición que Firebase rechaza
            _viewState.value = LoginViewState(isValidEmail = correo.isNotEmpty() && isValidEmail(correo), isValidPasswords = password.isNotEmpty())
            return
        }
        if (isValidEmail(correo) && isValidPassword(password)){
            loginUser(correo, password)
        }else{
            onFieldsChanged(email,password)
        }
    }

    fun loginUser(email:String,password:String){
        viewModelScope.launch {
            _viewState.value = LoginViewState(isLoading = true)
            when (loginUseCase(email,password)){
                LoginResult.Error -> {
                    _showErrorDialog.value =
                        UserLogin(email=email,password=password,showErrorDialog=true)
                    _viewState.value = LoginViewState(isLoading = false)
                }
                is LoginResult.Success -> {
                    _navigateToHome.value = Event(true)
                }
            }
            _viewState.value = LoginViewState(isLoading = false)
        }
    }

    /** Ignora toques repetidos mientras ya hay un inicio de sesión en curso. */
    fun onGoogleSelected() {
        val estado = _viewState.value
        if (estado.isLoading || estado.isLoadingGoogle) return
        _viewState.value = estado.copy(isLoadingGoogle = true)
        _solicitarCredencialGoogle.value = Event(Unit)
    }

    fun onResultadoGoogle(resultado: ResultadoCredencialGoogle) {
        when (resultado) {
            is ResultadoCredencialGoogle.Token -> loginConGoogle(resultado.idToken)
            ResultadoCredencialGoogle.Cancelado -> terminarCargaGoogle()
            ResultadoCredencialGoogle.SinCuentas -> fallarGoogle(R.string.login_google_sin_cuentas)
            is ResultadoCredencialGoogle.Error -> {
                Log.w(TAG, "Credential Manager: ${resultado.detalle}")
                fallarGoogle(R.string.login_google_error)
            }
        }
    }

    private fun loginConGoogle(idToken: String) {
        viewModelScope.launch {
            when (loginConGoogleUseCase(idToken)) {
                is LoginResult.Success -> {
                    terminarCargaGoogle()
                    _navigateToHome.value = Event(true)
                }
                LoginResult.Error -> fallarGoogle(R.string.login_google_error)
            }
        }
    }

    private fun fallarGoogle(@StringRes mensaje: Int) {
        terminarCargaGoogle()
        _mensajeError.value = Event(mensaje)
    }

    private fun terminarCargaGoogle() {
        _viewState.value = _viewState.value.copy(isLoadingGoogle = false)
    }

    fun onFieldsChanged(email: String, password: String){
        _viewState.value= _viewState.value.copy(
            isValidEmail = isValidEmail(email),
            isValidPasswords = isValidPassword(password)
        )
    }

    fun onRegisterSelected(){
        _navigateToRegister.value=Event(true)
    }

    private fun isValidEmail(email: String)=
        Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() || email.isBlank()

    private fun isValidPassword(password: String): Boolean =
        password.length >= MIN_PASSWORD_LENGHT || password.isEmpty()

}
