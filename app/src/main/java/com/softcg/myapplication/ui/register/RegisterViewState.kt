package com.softcg.myapplication.ui.register

data class RegisterViewState (
    val isLoading:Boolean=false,
    val isValidEmail: Boolean=true,
    val isValidPassword: Boolean=true,
    val isValidPasswordConfirmation:Boolean=true
){
    fun userValidated()= isValidEmail&&isValidPassword&&isValidPasswordConfirmation
}