package com.softcg.myapplication.ui.login

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.softcg.myapplication.R
import com.softcg.myapplication.core.MainDispatcherRule
import com.softcg.myapplication.data.network.ResultadoCredencialGoogle
import com.softcg.myapplication.data.response.LoginResult
import com.softcg.myapplication.domain.LoginConGoogleUseCase
import com.softcg.myapplication.domain.LoginUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MiViewModelGoogleTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loginConGoogle: LoginConGoogleUseCase = mockk()
    private val viewModel = MiViewModel(mockk<LoginUseCase>(relaxed = true), loginConGoogle)

    @Test
    fun `al pulsar el boton pide la credencial y muestra la carga`() {
        viewModel.onGoogleSelected()

        assertTrue(viewModel.viewState.value.isLoadingGoogle)
        assertEquals(Unit, viewModel.solicitarCredencialGoogle.value?.getContentIfNotHandled())
    }

    @Test
    fun `un segundo toque mientras carga no abre otro selector`() {
        viewModel.onGoogleSelected()
        viewModel.solicitarCredencialGoogle.value?.getContentIfNotHandled()

        viewModel.onGoogleSelected()

        assertNull(viewModel.solicitarCredencialGoogle.value?.getContentIfNotHandled())
    }

    @Test
    fun `con un token valido inicia sesion y navega al inicio`() {
        coEvery { loginConGoogle("token") } returns LoginResult.Success(true)
        viewModel.onGoogleSelected()

        viewModel.onResultadoGoogle(ResultadoCredencialGoogle.Token("token"))

        assertEquals(true, viewModel.navigateToHome.value?.getContentIfNotHandled())
        assertFalse(viewModel.viewState.value.isLoadingGoogle)
    }

    @Test
    fun `si Firebase rechaza el token muestra el error`() {
        coEvery { loginConGoogle("token") } returns LoginResult.Error
        viewModel.onGoogleSelected()

        viewModel.onResultadoGoogle(ResultadoCredencialGoogle.Token("token"))

        assertEquals(R.string.login_google_error, viewModel.mensajeError.value?.getContentIfNotHandled())
        assertNull(viewModel.navigateToHome.value)
        assertFalse(viewModel.viewState.value.isLoadingGoogle)
    }

    @Test
    fun `cancelar el selector no muestra ningun error`() {
        viewModel.onGoogleSelected()

        viewModel.onResultadoGoogle(ResultadoCredencialGoogle.Cancelado)

        assertNull(viewModel.mensajeError.value)
        assertFalse(viewModel.viewState.value.isLoadingGoogle)
        coVerify(exactly = 0) { loginConGoogle(any()) }
    }

    @Test
    fun `sin cuentas en el dispositivo avisa como agregar una`() {
        viewModel.onGoogleSelected()

        viewModel.onResultadoGoogle(ResultadoCredencialGoogle.SinCuentas)

        assertEquals(R.string.login_google_sin_cuentas, viewModel.mensajeError.value?.getContentIfNotHandled())
    }
}
