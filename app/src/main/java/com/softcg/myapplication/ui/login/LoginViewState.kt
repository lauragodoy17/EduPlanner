package com.softcg.myapplication.ui.login

data class LoginViewState(
    val isLoading: Boolean = false,
    val isValidEmail : Boolean =true,
    val isValidPasswords : Boolean=true,
    /** Selector de Google abierto o canjeando el token en Firebase. */
    val isLoadingGoogle: Boolean = false
)
