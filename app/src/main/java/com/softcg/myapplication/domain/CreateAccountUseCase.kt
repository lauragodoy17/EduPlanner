package com.softcg.myapplication.domain
import com.softcg.myapplication.data.network.AuthenticationService
import com.softcg.myapplication.ui.register.model.UserRegister
import javax.inject.Inject
class CreateAccountUseCase @Inject constructor(
    private val authenticationService: AuthenticationService,
) {

    suspend operator fun invoke(userRegister: UserRegister): Boolean {
        val accountCreated =
            authenticationService.createAccount(userRegister.email, userRegister.password) != null
        return if(accountCreated){
            true
        }else{
            false
        }

    }
}