package com.softcg.myapplication.domain
import com.softcg.myapplication.data.network.AuthenticationService
import com.softcg.myapplication.data.response.LoginResult
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val authenticationService: AuthenticationService) {

    suspend operator fun invoke(email:String, password: String): LoginResult =
        authenticationService.login(email,password)

}