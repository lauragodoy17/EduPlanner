package com.softcg.myapplication.domain

import com.softcg.myapplication.data.network.AuthenticationService
import com.softcg.myapplication.data.network.ResultadoRegistro
import com.softcg.myapplication.ui.register.model.UserRegister
import javax.inject.Inject

/** Crea la cuenta con correo y contraseña. El correo va sin espacios (el teclado suele añadir uno al final). */
class CreateAccountUseCase @Inject constructor(
    private val authenticationService: AuthenticationService,
) {
    suspend operator fun invoke(userRegister: UserRegister): ResultadoRegistro =
        authenticationService.createAccount(userRegister.email.trim(), userRegister.password)
}
