package com.softcg.myapplication.ui.Inicio.Fragments.Prediccion

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.softcg.myapplication.core.MainDispatcherRule
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.data.network.RespuestaApi
import com.softcg.myapplication.domain.prediccion.DatosPrediccion
import com.softcg.myapplication.domain.prediccion.EstadoDato
import com.softcg.myapplication.domain.prediccion.Extraccion
import com.softcg.myapplication.domain.prediccion.ExtraerDatosPrediccionUseCase
import com.softcg.myapplication.domain.prediccion.ParametroModelo
import com.softcg.myapplication.domain.prediccion.PredecirTareaUseCase
import com.softcg.myapplication.domain.prediccion.ResultadoExtraccion
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PrediccionViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tareas: TareasRepository = mockk()
    private val extraer: ExtraerDatosPrediccionUseCase = mockk()
    private val predecir: PredecirTareaUseCase = mockk()

    private val tarea = Tarea(7, "Taller de matrices", "", "Cálculo", "2026-11-20", 1, tema = "Algebra lineal", asignaturaId = 3)
    private val asignatura = Asignatura(3, "Cálculo", "Ana", 2, "8:00", List(7) { it == 0 }, areaModelo = "Matematicas")

    private val datosReales = DatosPrediccion(8.6, 2.5, 7.4, 5.0, 7.5, "Matematicas", "Algebra lineal", "Pomodoro")

    private fun resultado(datos: DatosPrediccion?) = ResultadoExtraccion(
        asignatura, tarea,
        ParametroModelo.values().map { EstadoDato(it, listo = datos != null, valor = "", detalle = "") },
        datos, emptyList()
    )

    private fun viewModel() = PrediccionViewModel(tareas, extraer, predecir)

    @Test
    fun `con todos los datos envia los datos reales extraidos y muestra la respuesta`() {
        coEvery { tareas.getTarea(7) } returns tarea
        coEvery { extraer.deTarea(tarea) } returns Extraccion.Lista(resultado(datosReales))
        coEvery { predecir(datosReales, PredecirTareaUseCase.TOP_N) } returns RespuestaApi.Http(200, "{\"prediccion\":{}}")
        val viewModel = viewModel()

        viewModel.cargar(7)

        assertEquals(EstadoPrediccion.Respuesta(200, "{\"prediccion\":{}}", true), viewModel.estado.value)
        assertEquals(datosReales, viewModel.datosEnviados)
    }

    @Test
    fun `si falta algun dato no se envia nada y se muestra que falta`() {
        coEvery { tareas.getTarea(7) } returns tarea
        coEvery { extraer.deTarea(tarea) } returns Extraccion.Lista(resultado(null))
        val viewModel = viewModel()

        viewModel.cargar(7)
        viewModel.predecirDeNuevo()

        assertNull(viewModel.estado.value)
        assertTrue((viewModel.datos.value as EstadoDatos.Listos).resultado.estados.none { it.listo })
        coVerify(exactly = 0) { predecir(any(), any()) }
    }

    @Test
    fun `una tarea sin asignatura registrada no se puede predecir`() {
        coEvery { tareas.getTarea(7) } returns tarea
        coEvery { extraer.deTarea(tarea) } returns Extraccion.SinAsignatura
        val viewModel = viewModel()

        viewModel.cargar(7)

        assertEquals(EstadoDatos.SinAsignatura, viewModel.datos.value)
        coVerify(exactly = 0) { predecir(any(), any()) }
    }

    @Test
    fun `volver a la pantalla con los mismos datos no repite la peticion`() {
        coEvery { tareas.getTarea(7) } returns tarea
        coEvery { extraer.deTarea(tarea) } returns Extraccion.Lista(resultado(datosReales))
        coEvery { predecir(any(), any()) } returns RespuestaApi.Http(200, "{}")
        val viewModel = viewModel()

        viewModel.cargar(7)
        viewModel.cargar(7)
        viewModel.predecirDeNuevo()

        coVerify(exactly = 1) { predecir(any(), any()) }
    }

    @Test
    fun `con datos nuevos se puede volver a predecir`() {
        val nuevos = datosReales.copy(interestLevel = 10.0)
        coEvery { tareas.getTarea(7) } returns tarea
        coEvery { extraer.deTarea(tarea) } returnsMany listOf(Extraccion.Lista(resultado(datosReales)), Extraccion.Lista(resultado(nuevos)))
        coEvery { predecir(any(), any()) } returns RespuestaApi.Http(200, "{}")
        val viewModel = viewModel()

        viewModel.cargar(7)
        viewModel.cargar(7)
        viewModel.predecirDeNuevo()

        coVerify { predecir(nuevos, PredecirTareaUseCase.TOP_N) }
        assertEquals(nuevos, viewModel.datosEnviados)
    }

    @Test
    fun `sin red se muestra el error y se puede reintentar`() {
        coEvery { tareas.getTarea(7) } returns tarea
        coEvery { extraer.deTarea(tarea) } returns Extraccion.Lista(resultado(datosReales))
        coEvery { predecir(any(), any()) } returns RespuestaApi.ErrorRed("timeout")
        val viewModel = viewModel()

        viewModel.cargar(7)
        assertTrue((viewModel.estado.value as EstadoPrediccion.Error).mensaje.contains("timeout"))

        viewModel.predecirDeNuevo()
        coVerify(exactly = 2) { predecir(any(), any()) }
    }

    @Test
    fun `una tarea borrada no se predice`() {
        coEvery { tareas.getTarea(7) } returns null
        val viewModel = viewModel()

        viewModel.cargar(7)

        assertEquals(EstadoDatos.SinTarea, viewModel.datos.value)
    }
}
