package com.softcg.myapplication.ui.Inicio.Fragments.Prediccion

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.softcg.myapplication.core.MainDispatcherRule
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.data.network.RespuestaApi
import com.softcg.myapplication.domain.ia.ContextoPrediccion
import com.softcg.myapplication.domain.ia.MotivoSinPrediccion
import com.softcg.myapplication.domain.ia.RecomendacionTarea
import com.softcg.myapplication.domain.ia.RecomendarTareaUseCase
import com.softcg.myapplication.domain.ia.ResultadoRecomendacion
import com.softcg.myapplication.domain.prediccion.DatosPrediccion
import com.softcg.myapplication.domain.prediccion.EstadoDato
import com.softcg.myapplication.domain.prediccion.Extraccion
import com.softcg.myapplication.domain.prediccion.ExtraerDatosPrediccionUseCase
import com.softcg.myapplication.domain.prediccion.ItemDato
import com.softcg.myapplication.domain.prediccion.PredecirTareaUseCase
import com.softcg.myapplication.domain.prediccion.ResultadoExtraccion
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
    private val recomendar: RecomendarTareaUseCase = mockk()

    private val sinModelo = ContextoPrediccion.SinPrediccion(MotivoSinPrediccion.MODELO_NO_DISPONIBLE, emptyList())
    private val plan = RecomendacionTarea("Mapas mentales", sinModelo, "intro", "consejo", listOf("paso"), "2 horas", emptyList())

    private val tarea = Tarea(7, "Taller de matrices", "", "Cálculo", "2026-11-20", 1, tema = "Algebra lineal", asignaturaId = 3)
    private val asignatura = Asignatura(3, "Cálculo", "Ana", 2, "8:00", List(7) { it == 0 }, areaModelo = "Matematicas")

    private val datosReales = DatosPrediccion(8.6, 2.5, 7.4, 5.0, 7.5, "Matematicas", "Algebra lineal", "Pomodoro")

    private fun resultado(datos: DatosPrediccion?) = ResultadoExtraccion(
        asignatura, tarea,
        ItemDato.entries.map { EstadoDato(it, listo = datos != null, valor = "", detalle = "") },
        datos, emptyList()
    )

    private fun viewModel(): PrediccionViewModel {
        every { recomendar.contextoDeRespuesta(any(), any()) } returns sinModelo
        every { recomendar.enCache(any()) } returns null
        coEvery { recomendar.conContexto(any(), any(), any()) } returns ResultadoRecomendacion.Exito(plan)
        return PrediccionViewModel(tareas, extraer, predecir, mockk(relaxed = true), mockk(relaxed = true), recomendar)
    }

    private fun conDatos(datos: DatosPrediccion? = datosReales) {
        coEvery { tareas.getTarea(7) } returns tarea
        coEvery { extraer.deTarea(tarea) } returns Extraccion.Lista(resultado(datos))
    }

    @Test
    fun `al abrir la tarea no se llama a la API ni a la IA`() {
        conDatos()
        val viewModel = viewModel()

        viewModel.cargar(7)
        viewModel.cargar(7)

        assertNull(viewModel.estado.value)
        assertNull(viewModel.recomendacion.value)
        coVerify(exactly = 0) { predecir(any(), any()) }
        coVerify(exactly = 0) { recomendar.conContexto(any(), any(), any()) }
    }

    @Test
    fun `al abrir se muestra la recomendacion que ya se habia pedido, sin gastar otra`() {
        conDatos()
        val viewModel = viewModel()
        every { recomendar.enCache(tarea) } returns plan

        viewModel.cargar(7)

        assertEquals(EstadoRecomendacion.Lista(plan), viewModel.recomendacion.value)
        coVerify(exactly = 0) { predecir(any(), any()) }
        coVerify(exactly = 0) { recomendar.conContexto(any(), any(), any()) }
    }

    @Test
    fun `el boton envia los datos reales al modelo y su resultado a la IA`() {
        val respuesta = RespuestaApi.Http(200, "{\"prediccion\":{}}")
        conDatos()
        coEvery { predecir(datosReales, PredecirTareaUseCase.TOP_N) } returns respuesta
        val viewModel = viewModel()

        viewModel.cargar(7)
        viewModel.predecirDeNuevo()

        assertEquals(EstadoPrediccion.Respuesta(200, "{\"prediccion\":{}}", true), viewModel.estado.value)
        assertEquals(datosReales, viewModel.datosEnviados)
        io.mockk.verify { recomendar.contextoDeRespuesta(datosReales, respuesta) }
        coVerify(exactly = 1) { recomendar.conContexto(tarea, sinModelo, false) }
        assertEquals(EstadoRecomendacion.Lista(plan), viewModel.recomendacion.value)
    }

    @Test
    fun `si falta algun dato el boton no envia nada`() {
        conDatos(null)
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
        viewModel.predecirDeNuevo()

        assertEquals(EstadoDatos.SinAsignatura, viewModel.datos.value)
        coVerify(exactly = 0) { predecir(any(), any()) }
    }

    @Test
    fun `con la misma prediccion el boton pide otro plan a la IA sin repetir la API`() {
        conDatos()
        coEvery { predecir(any(), any()) } returns RespuestaApi.Http(200, "{}")
        val viewModel = viewModel()

        viewModel.cargar(7)
        viewModel.predecirDeNuevo()
        viewModel.cargar(7)
        viewModel.predecirDeNuevo()

        coVerify(exactly = 1) { predecir(any(), any()) }
        coVerify(exactly = 1) { recomendar.conContexto(tarea, sinModelo, true) }
    }

    @Test
    fun `con datos nuevos el boton vuelve a predecir y pide un plan distinto`() {
        val nuevos = datosReales.copy(interestLevel = 10.0)
        coEvery { tareas.getTarea(7) } returns tarea
        coEvery { extraer.deTarea(tarea) } returnsMany listOf(Extraccion.Lista(resultado(datosReales)), Extraccion.Lista(resultado(nuevos)))
        coEvery { predecir(any(), any()) } returns RespuestaApi.Http(200, "{}")
        val viewModel = viewModel()

        viewModel.cargar(7)
        viewModel.predecirDeNuevo()
        viewModel.cargar(7)
        viewModel.predecirDeNuevo()

        coVerify { predecir(nuevos, PredecirTareaUseCase.TOP_N) }
        assertEquals(nuevos, viewModel.datosEnviados)
        coVerify(exactly = 1) { recomendar.conContexto(tarea, sinModelo, true) }
    }

    @Test
    fun `sin red o con error HTTP el boton reintenta la prediccion`() {
        conDatos()
        coEvery { predecir(any(), any()) } returnsMany listOf(RespuestaApi.ErrorRed("timeout"), RespuestaApi.Http(500, "error"), RespuestaApi.Http(200, "{}"))
        val viewModel = viewModel()
        coEvery { recomendar.conContexto(any(), any(), any()) } returns ResultadoRecomendacion.SinConexion

        viewModel.cargar(7)
        viewModel.predecirDeNuevo()
        assertTrue((viewModel.estado.value as EstadoPrediccion.Error).mensaje.contains("timeout"))
        viewModel.predecirDeNuevo()
        viewModel.predecirDeNuevo()

        coVerify(exactly = 3) { predecir(any(), any()) }
    }

    @Test
    fun `si la IA falla el boton la reintenta sin repetir la API`() {
        conDatos()
        coEvery { predecir(any(), any()) } returns RespuestaApi.Http(200, "{}")
        val viewModel = viewModel()
        coEvery { recomendar.conContexto(any(), any(), any()) } returns ResultadoRecomendacion.SinConexion

        viewModel.cargar(7)
        viewModel.predecirDeNuevo()
        assertTrue(viewModel.recomendacion.value is EstadoRecomendacion.Error)
        viewModel.predecirDeNuevo()

        coVerify(exactly = 1) { predecir(any(), any()) }
        coVerify(exactly = 2) { recomendar.conContexto(tarea, sinModelo, false) }
    }

    @Test
    fun `sin sesion no se llama a la IA`() {
        conDatos()
        coEvery { predecir(any(), any()) } returns RespuestaApi.SinSesion
        val viewModel = viewModel()

        viewModel.cargar(7)
        viewModel.predecirDeNuevo()

        assertNull(viewModel.recomendacion.value)
        coVerify(exactly = 0) { recomendar.conContexto(any(), any(), any()) }
    }

    @Test
    fun `una tarea borrada no se predice`() {
        coEvery { tareas.getTarea(7) } returns null
        val viewModel = viewModel()

        viewModel.cargar(7)
        viewModel.predecirDeNuevo()

        assertEquals(EstadoDatos.SinTarea, viewModel.datos.value)
        coVerify(exactly = 0) { predecir(any(), any()) }
    }
}
