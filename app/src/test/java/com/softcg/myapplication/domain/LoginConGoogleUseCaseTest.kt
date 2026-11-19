package com.softcg.myapplication.domain

import com.softcg.myapplication.data.network.AuthenticationService
import com.softcg.myapplication.data.response.LoginResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LoginConGoogleUseCaseTest {

    private val authenticationService: AuthenticationService = mockk()
    private val useCase = LoginConGoogleUseCase(authenticationService)

    @Test
    fun `canjea el token de Google en Firebase`() = runBlocking {
        coEvery { authenticationService.loginConGoogle("token") } returns LoginResult.Success(true)

        assertEquals(LoginResult.Success(true), useCase("token"))
        coVerify { authenticationService.loginConGoogle("token") }
    }

    @Test
    fun `un token vacio falla sin llamar a Firebase`() = runBlocking {
        assertEquals(LoginResult.Error, useCase("  "))
        coVerify(exactly = 0) { authenticationService.loginConGoogle(any()) }
    }
}
