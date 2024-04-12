package com.softcg.myapplication.ui.register

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.core.Event
import com.softcg.myapplication.domain.CreateAccountUseCase
import com.softcg.myapplication.ui.register.model.UserRegister
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class viewmodelregister @Inject constructor(val createAccountUseCase: CreateAccountUseCase) :
    ViewModel() {

    private companion object{
        const val MIN_PASSWORD_LENGTH=6
    }

    private val _navigateToLogin= MutableLiveData<Event<Boolean>>()
    val navigateToLogin: LiveData<Event<Boolean>>
        get() = _navigateToLogin

    private val _navegateToHome=MutableLiveData<Event<Boolean>>()
    val navegateToHome:LiveData<Event<Boolean>>
        get()=_navegateToHome

    private val _viewState = MutableStateFlow(RegisterViewState())
    val viewState: StateFlow<RegisterViewState>
        get() = _viewState

    private var _showErrorDialog = MutableLiveData(false)
    val showErrorDialog: LiveData<Boolean>
        get() = _showErrorDialog

    fun onRegisterSelected(userRegister: UserRegister){
        val viewState = userRegister.toRegisterViewState()
        if (viewState.userValidated()&&userRegister.isNotEmpty()){
            registerUser(userRegister)
        }else{
            onFieldsChanged(userRegister)
        }
    }

    private fun registerUser(userRegister: UserRegister){
        viewModelScope.launch {
            _viewState.value = RegisterViewState(isLoading =true)
            val accountCreated= createAccountUseCase(userRegister)
            if (accountCreated){
                _navegateToHome.value=Event(true)
            }else{
                _showErrorDialog.value=true
            }
            _viewState.value=RegisterViewState(isLoading=false)
        }
    }

    fun onFieldsChanged(userRegister: UserRegister){
        _viewState.value=userRegister.toRegisterViewState()
    }
    fun onLoginSelected(){
        _navigateToLogin.value= Event(true)
    }
    private fun isValidOrEmptyEmail(email: String) =
        Patterns.EMAIL_ADDRESS.matcher(email).matches() || email.isEmpty()

    private fun isValidOrEmptyPassword(password: String, passwordConfirmation: String): Boolean =
        (password.length >= MIN_PASSWORD_LENGTH && password == passwordConfirmation) || password.isEmpty() || passwordConfirmation.isEmpty()

    private fun UserRegister.toRegisterViewState(): RegisterViewState{
        return RegisterViewState(
            isValidEmail= isValidOrEmptyEmail(email),
            isValidPassword=isValidOrEmptyPassword(password,passwordConfirmation)
        )
    }
}