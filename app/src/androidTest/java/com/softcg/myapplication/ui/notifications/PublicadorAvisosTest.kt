package com.softcg.myapplication.ui.notifications

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.softcg.myapplication.domain.gamificacion.ClaseProgramada
import com.softcg.myapplication.domain.notificaciones.RespuestaAccion
import com.softcg.myapplication.domain.notificaciones.TextosAvisos
import com.softcg.myapplication.domain.notificaciones.TipoAviso
import com.softcg.myapplication.domain.notificaciones.CanalAviso
import com.softcg.myapplication.ui.Inicio.Models.Evento
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Publica un aviso de cada tipo con referencias que no existen (si se pulsan sus botones, responden "ya no existe"
 * sin tocar los datos). Con el argumento `dejarVisibles=true` no los retira, para revisarlos a mano.
 */
@RunWith(AndroidJUnit4::class)
class PublicadorAvisosTest {

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val publicador = PublicadorAvisos(contexto)
    private val hoy = LocalDate.now()

    private val avisos = listOf(
        TextosAvisos.resumenDelDia(
            hoy.toString(),
            listOf(Tarea(999_001, "Taller de integrales", "", "Cálculo", hoy.toString(), 1, horaEntrega = "18:00")),
            listOf(Evento(999_002, "Feria de ciencias", "", hoy.toString(), 1, "08:00", "12:00"))
        )!!,
        TextosAvisos.clasePronto(clase(), "Prof. Ana Gómez"),
        TextosAvisos.tarea(Tarea(999_003, "Ensayo de historia", "", "Historia", hoy.toString(), 1, horaEntrega = "18:00"),
            TipoAviso.TAREA_UNA_HORA, hoy.atTime(18, 0)),
        TextosAvisos.tarea(Tarea(999_004, "Quiz de física", "", "Física", hoy.toString(), 1, horaEntrega = "12:00"),
            TipoAviso.TAREA_CINCO_MINUTOS, hoy.atTime(12, 0)),
        TextosAvisos.tarea(Tarea(999_005, "Informe de laboratorio", "", "Química", hoy.toString(), 1),
            TipoAviso.TAREA_VENCIDA, hoy.atTime(23, 59)),
        TextosAvisos.asistencia(clase(asignaturaId = 999_007))
    )

    private fun clase(asignaturaId: Int = 999_006) =
        ClaseProgramada(asignaturaId, "Cálculo II", hoy, hoy.atTime(10, 0), hoy.atTime(12, 0))

    @Test
    fun cadaTipoDeAvisoSePublicaYSeRetira() {
        assumeTrue("Sin permiso de notificaciones no se puede comprobar", publicador.puedeNotificar())
        avisos.forEach(publicador::publicar)
        val visibles = publicador.visibles()
        avisos.forEach { assertTrue("Falta ${it.claveNotificacion}", it.claveNotificacion in visibles) }

        // La respuesta sustituye a la pregunta y no cuenta como aviso: la replanificación no la retira
        publicador.mostrarRespuesta("tarea:999005", CanalAviso.TAREAS, RespuestaAccion("¡Tarea entregada!", "+8 EduCoins, ¡a tiempo!", true))
        assertTrue("tarea:999005" !in publicador.visibles())
    }

    @After
    fun limpiar() {
        val dejar = InstrumentationRegistry.getArguments().getString("dejarVisibles") == "true"
        if (!dejar) avisos.map { it.claveNotificacion }.forEach(publicador::retirar)
    }
}
