package com.softcg.myapplication.domain.gamificacion

import com.softcg.myapplication.core.RelojFijo
import com.softcg.myapplication.core.ejecutarTransaccionesDirectamente
import com.softcg.myapplication.data.Repositories.CalificacionesRespository
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.network.SesionUsuario
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models.Calificacion
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class CalificarTareaUseCaseTest {

    @RelaxedMockK private lateinit var calificacionesRepository: CalificacionesRespository
    @RelaxedMockK private lateinit var repository: GamificacionRepository
    @RelaxedMockK private lateinit var sesion: SesionUsuario

    private val tarea = Tarea(12, "Ensayo", "", "Historia", "2026-10-28", 1)
    private lateinit var calificar: CalificarTareaUseCase

    @Before
    fun onBefore() {
        MockKAnnotations.init(this)
        every { sesion.uid() } returns "usuario-1"
        coEvery { calificacionesRepository.tareaTieneCalificacion(any()) } returns false
        coEvery { repository.existeMovimiento(any(), any()) } returns false
        coEvery { repository.contarMovimientosDelDia(any(), any(), any()) } returns 0
        coEvery { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) } returns true
        repository.ejecutarTransaccionesDirectamente()
        val otorgar = OtorgarRecompensaUseCase(repository, sesion, RelojFijo(LocalDateTime.of(2026, 10, 28, 10, 0)))
        calificar = CalificarTareaUseCase(calificacionesRepository, repository, otorgar, sesion)
    }

    @Test
    fun `calificar una tarea la completa, guarda la calificacion y da la recompensa de completar`() = runBlocking {
        // Vence el mismo día: 5 por completar y 3 de bono a tiempo
        assertEquals(ResultadoCalificarTarea.Completada(monedas = 8, aTiempo = true), calificar(tarea, "4,5"))

        coVerify { calificacionesRepository.insertCalificacion(Calificacion(null, "Tarea", 4.5F, "Historia", "Ensayo", tareaId = 12)) }
        coVerify { repository.insertarMovimiento("usuario-1", "TAREA_COMPLETADA:12", "TAREA_COMPLETADA", 5, any(), any(), any()) }
        coVerify { repository.insertarMovimiento("usuario-1", "TAREA_A_TIEMPO:12", "TAREA_A_TIEMPO", 3, any(), any(), any()) }
        // No se premia además como "calificación registrada"
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), match { it.startsWith("CALIFICACION_REGISTRADA") }, any(), any(), any(), any(), any()) }
    }

    @Test
    fun `una tarea vencida se completa sin bono a tiempo`() = runBlocking {
        val vencida = tarea.copy(fecha = "2026-10-27")

        assertEquals(ResultadoCalificarTarea.Completada(monedas = 5, aTiempo = false), calificar(vencida, "4"))
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), "TAREA_A_TIEMPO:12", any(), any(), any(), any(), any()) }
    }

    @Test
    fun `calificar una tarea ya entregada da la recompensa de calificacion, no otra de completar`() = runBlocking {
        val entregada = tarea.copy(entregadaMillis = 1000L)

        assertEquals(ResultadoCalificarTarea.Completada(monedas = 3), calificar(entregada, "4"))
        coVerify { repository.insertarMovimiento("usuario-1", "CALIFICACION_REGISTRADA:tarea-12", "CALIFICACION_REGISTRADA", 3, any(), any(), any()) }
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), "TAREA_COMPLETADA:12", any(), any(), any(), any(), any()) }
    }

    @Test
    fun `una tarea ya calificada no se vuelve a calificar ni a premiar`() = runBlocking {
        coEvery { calificacionesRepository.tareaTieneCalificacion(12) } returns true

        assertEquals(ResultadoCalificarTarea.YaCalificada, calificar(tarea, "5"))
        coVerify(exactly = 0) { calificacionesRepository.insertCalificacion(any()) }
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `volver a calificar tras borrar la calificacion no da otra recompensa`() = runBlocking {
        coEvery { repository.existeMovimiento("usuario-1", "TAREA_COMPLETADA:12") } returns true

        assertEquals(ResultadoCalificarTarea.Completada(monedas = 0), calificar(tarea, "5"))
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `una calificacion vacia o invalida no completa la tarea`() = runBlocking {
        assertEquals(ResultadoCalificarTarea.ValorInvalido, calificar(tarea, ""))
        assertEquals(ResultadoCalificarTarea.ValorInvalido, calificar(tarea, "abc"))
        assertEquals(ResultadoCalificarTarea.ValorInvalido, calificar(tarea, "-1"))
        coVerify(exactly = 0) { calificacionesRepository.insertCalificacion(any()) }
    }

    @Test
    fun `el valor acepta coma o punto y el cero`() {
        assertEquals(4.5F, CalificarTareaUseCase.valorDesdeTexto("4,5"))
        assertEquals(4.5F, CalificarTareaUseCase.valorDesdeTexto(" 4.5 "))
        assertEquals(0F, CalificarTareaUseCase.valorDesdeTexto("0"))
        assertNull(CalificarTareaUseCase.valorDesdeTexto("."))
    }

    @Test
    fun `si falla al guardar no se informa como completada`() = runBlocking {
        coEvery { calificacionesRepository.insertCalificacion(any()) } throws IllegalStateException()

        assertEquals(ResultadoCalificarTarea.Error, calificar(tarea, "5"))
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) }
    }
}
