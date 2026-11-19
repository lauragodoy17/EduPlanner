package com.softcg.myapplication.domain.notificaciones

import com.softcg.myapplication.core.RelojFijo
import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.data.Repositories.EventosRepository
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.data.network.SesionUsuario
import com.softcg.myapplication.domain.gamificacion.ConfirmarAsistenciaUseCase
import com.softcg.myapplication.domain.gamificacion.EntregarTareaUseCase
import com.softcg.myapplication.domain.gamificacion.ResultadoAsistencia
import com.softcg.myapplication.domain.gamificacion.ResultadoEntregarTarea
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class AvisosUseCasesTest {

    private val tareas: TareasRepository = mockk(relaxed = true)
    private val asignaturas: AsignaturasRepository = mockk(relaxed = true)
    private val eventos: EventosRepository = mockk(relaxed = true)
    private val gamificacion: GamificacionRepository = mockk(relaxed = true)
    private val sesion: SesionUsuario = mockk()
    private val reloj = RelojFijo(LocalDateTime.of(2026, 11, 19, 8, 0))
    private val programador: ProgramadorAvisos = mockk(relaxed = true)

    private val taller = Tarea(7, "Taller", "", "Cálculo", "2026-11-19", 1, horaEntrega = "18:00")

    init {
        every { sesion.uid() } returns "u1"
        every { tareas.getTareas() } returns listOf(taller)
        every { asignaturas.getAllAsignaturas() } returns emptyList()
        coEvery { gamificacion.asistencias("u1") } returns emptyList()
    }

    @Test
    fun `reprogramar pone las alarmas de las tareas pendientes`() = runBlocking {
        val plan = slot<PlanAvisos>()
        every { programador.programar(capture(plan)) } returns Unit

        ReprogramarAvisosUseCase(tareas, asignaturas, gamificacion, sesion, reloj, programador)()

        assertEquals(3, plan.captured.programar.count { it.referencia == "7" })
        assertEquals(1, plan.captured.programar.count { it.tipo == TipoAviso.RESUMEN_DIA })
    }

    @Test
    fun `sin sesion se quitan todas las alarmas`() = runBlocking {
        every { sesion.uid() } returns null
        ReprogramarAvisosUseCase(tareas, asignaturas, gamificacion, sesion, reloj, programador)()
        verify { programador.cancelarTodo() }
    }

    @Test
    fun `al dispararse se comprueba que la tarea siga pendiente`() = runBlocking {
        val construir = ConstruirAvisoUseCase(tareas, asignaturas, eventos, gamificacion, sesion, reloj)
        coEvery { tareas.getTarea(7) } returns taller
        assertEquals("tarea:7", construir(TipoAviso.TAREA_UNA_HORA, "7")?.claveNotificacion)

        coEvery { tareas.getTarea(7) } returns taller.copy(entregadaMillis = 1L)
        assertNull(construir(TipoAviso.TAREA_UNA_HORA, "7"))
        assertNull("Una tarea borrada no avisa", construir(TipoAviso.TAREA_VENCIDA, "99"))
    }

    @Test
    fun `el resumen solo cuenta lo de ese dia y sin nada no aparece`() = runBlocking {
        val construir = ConstruirAvisoUseCase(tareas, asignaturas, eventos, gamificacion, sesion, reloj)
        every { eventos.getAllEventos() } returns emptyList()
        assertEquals("Buenos días: hoy tienes 1 tarea", construir(TipoAviso.RESUMEN_DIA, "2026-11-19")?.titulo)
        assertNull(construir(TipoAviso.RESUMEN_DIA, "2026-11-20"))
    }

    @Test
    fun `responder desde la notificacion registra la asistencia o entrega la tarea`() = runBlocking {
        val confirmar: ConfirmarAsistenciaUseCase = mockk()
        val entregar: EntregarTareaUseCase = mockk()
        val responder = ResponderAvisoUseCase(confirmar, entregar, tareas)

        coEvery { confirmar(1, LocalDate.of(2026, 11, 19), true) } returns ResultadoAsistencia.Registrada(true, 10)
        val asistio = responder(TipoAccionAviso.ASISTI, "1:2026-11-19")
        assertEquals("¡Asistencia registrada!", asistio.titulo)
        assertEquals("+10 EduCoins por ir a clase", asistio.texto)

        coEvery { confirmar(1, LocalDate.of(2026, 11, 19), false) } returns ResultadoAsistencia.PlazoVencido
        assertTrue(!responder(TipoAccionAviso.NO_ASISTI, "1:2026-11-19").exito)

        coEvery { tareas.getTarea(7) } returns taller
        coEvery { entregar(taller) } returns ResultadoEntregarTarea.Entregada(8, true)
        assertEquals("+8 EduCoins, ¡a tiempo!", responder(TipoAccionAviso.ENTREGAR, "7").texto)
    }
}
