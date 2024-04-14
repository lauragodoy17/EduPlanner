package com.softcg.myapplication.ui.register.model

data class UserRegister (
    val email: String,
    val password: String,
    val passwordConfirmation: String

)
{
    fun isNotEmpty()=
        email.isNotEmpty()&&password.isNotEmpty()&&passwordConfirmation.isNotEmpty()
}