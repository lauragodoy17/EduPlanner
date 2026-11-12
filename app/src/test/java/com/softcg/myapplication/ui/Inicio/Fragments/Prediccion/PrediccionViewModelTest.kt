package com.softcg.myapplication.ui.Inicio.Fragments.Prediccion

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.softcg.myapplication.core.MainDispatcherRule
import com.softcg.myapplication.data.network.RespuestaApi
import com.softcg.myapplication.domain.prediccion.DatosPrediccion
import com.softcg.myapplication.domain.prediccion.PredecirTareaUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PrediccionViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val predecir: PredecirTareaUseCase = mockk()

    @Test
    fun `envia los datos de prueba y muestra la respuesta de la API`() {
        coEvery { predecir(DatosPrediccion.PRUEBA, PredecirTareaUseCase.TOP_N) } returns
            RespuestaApi.Http(200, "{\"prediccion\":{}}")
        val viewModel = PrediccionViewModel(predecir)

        viewModel.consultarSiHaceFalta()

        assertEquals(EstadoPrediccion.Respuesta(200, "{\"prediccion\":{}}", true), viewModel.estado.value)
    }

    @Test
    fun `un error de la API se muestra con su codigo`() {
        coEvery { predecir(any(), any()) } returns RespuestaApi.Http(401, "{\"detail\":\"Token\"}")
        val viewModel = PrediccionViewModel(predecir)

        viewModel.consultarSiHaceFalta()

        assertEquals(EstadoPrediccion.Respuesta(401, "{\"detail\":\"Token\"}", false), viewModel.estado.value)
    }

    @Test
    fun `sin sesion o sin red se muestra un error`() {
        coEvery { predecir(any(), any()) } returns RespuestaApi.SinSesion
        val sinSesion = PrediccionViewModel(predecir).apply { consultarSiHaceFalta() }
        assertTrue(sinSesion.estado.value is EstadoPrediccion.Error)

        coEvery { predecir(any(), any()) } returns RespuestaApi.ErrorRed("timeout")
        val sinRed = PrediccionViewModel(predecir).apply { consultarSiHaceFalta() }
        assertTrue((sinRed.estado.value as EstadoPrediccion.Error).mensaje.contains("timeout"))
    }

    @Test
    fun `volver a la pantalla no repite la peticion`() {
        coEvery { predecir(any(), any()) } returns RespuestaApi.Http(200, "{}")
        val viewModel = PrediccionViewModel(predecir)

        viewModel.consultarSiHaceFalta()
        viewModel.consultarSiHaceFalta()

        coVerify(exactly = 1) { predecir(any(), any()) }
    }
}
