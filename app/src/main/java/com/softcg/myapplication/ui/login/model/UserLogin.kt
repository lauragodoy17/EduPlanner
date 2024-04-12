package com.softcg.myapplication.ui.login.model

data class UserLogin(
    val email: String = "",
    val password : String = "",
    val showErrorDialog: Boolean = false
)