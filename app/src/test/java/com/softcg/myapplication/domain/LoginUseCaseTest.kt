package com.softcg.myapplication.domain

import com.softcg.myapplication.data.network.AuthenticationService
import com.softcg.myapplication.data.response.LoginResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class LoginUseCaseTest{

    @RelaxedMockK
    private lateinit var authenticationService: AuthenticationService

    lateinit var loginUseCase: LoginUseCase
    @Before
    fun onBefore(){
        MockKAnnotations.init(this)
        loginUseCase = LoginUseCase(authenticationService)
    }

    @Test
    fun `cuando el api loguea al usuario`()= runBlocking {
        //GIVEN
        //WHEN

            loginUseCase("hola@hola.hola","111111")

        //THEN
            coVerify { authenticationService.login("hola@hola.hola","111111") }

    }
}