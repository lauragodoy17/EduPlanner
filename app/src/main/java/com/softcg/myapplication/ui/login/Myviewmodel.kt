package com.softcg.myapplication.ui.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.core.Event
import com.softcg.myapplication.data.response.LoginResult
import com.softcg.myapplication.domain.LoginUseCase
import com.softcg.myapplication.ui.login.model.UserLogin
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MiViewModel @Inject constructor(val loginUseCase: LoginUseCase) :ViewModel() {

    private companion object{
        const val MIN_PASSWORD_LENGHT = 6
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

    fun onLoginSelected(email: String,password: String){
        if (isValidEmail(email) && isValidPassword(password)){
            loginUser(email, password)
        }else{
            onFieldsChanged(email,password)
        }
    }

    fun loginUser(email:String,password:String){
        viewModelScope.launch {
            _viewState.value = LoginViewState(isLoading = true)
            when (val result = loginUseCase(email,password)){
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

    fun onFieldsChanged(email: String, password: String){
        _viewState.value= LoginViewState(
            isValidEmail = isValidEmail(email),
            isValidPasswords = isValidPassword(password)
        )
    }

    fun onRegisterSelected(){
        _navigateToRegister.value=Event(true)
    }

    private fun isValidEmail(email: String)=
        Patterns.EMAIL_ADDRESS.matcher(email).matches() || email.isEmpty()

    private fun isValidPassword(password: String): Boolean =
        password.length >= MIN_PASSWORD_LENGHT || password.isEmpty()

}