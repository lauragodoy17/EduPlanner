package com.softcg.myapplication.ui.login

data class LoginViewState(
    val isLoading: Boolean = false,
    val isValidEmail : Boolean =true,
    val isValidPasswords : Boolean=true
)