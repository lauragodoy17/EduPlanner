package com.softcg.myapplication.domain

import com.softcg.myapplication.data.network.AuthenticationService
import com.softcg.myapplication.ui.register.model.UserRegister
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class CreateAccountUseCaseTest {

    @RelaxedMockK
    private lateinit var authenticationService: AuthenticationService
    lateinit var createAccountUseCase: CreateAccountUseCase

    @Before
    fun onBefore(){
        MockKAnnotations.init(this)
        createAccountUseCase = CreateAccountUseCase(authenticationService)
    }

    @Test
    fun `cuando el api registra un usuario`()= runBlocking {
        //GIVE
        val userRegister= UserRegister("prueba@prueba.prueba","111111","111111")
        //WHEN
        createAccountUseCase(userRegister)
        //THEN
        coVerify { authenticationService.createAccount(userRegister.email,userRegister.password) }
    }
}