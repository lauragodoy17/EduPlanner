package com.softcg.myapplication.domain.notificaciones

import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Models.Evento
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import com.softcg.myapplication.domain.gamificacion.AsistenciaReglas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ReglasAvisosTest {

    private val zona = ZoneId.of("America/Bogota")
    private val jueves = LocalDate.of(2026, 11, 19)

    /** Cálculo los jueves de 10:00 a 12:00, registrada hace tiempo. */
    private val calculo = Asignatura(
        1, "Cálculo", "Ana", 2, "10:00", listOf(false, false, false, true, false, false, false), fechaRegistro = 0L
    )

    private fun plan(
        ahora: LocalDateTime,
        tareas: List<Tarea> = emptyList(),
        respondidas: Set<Pair<Int, LocalDate>> = emptySet()
    ) = ReglasAvisos.planificar(tareas, listOf(calculo), respondidas, ahora, zona)

    private fun PlanAvisos.de(tipo: TipoAviso) = programar.filter { it.tipo == tipo }

    @Test
    fun `la clase se avisa 10 minutos antes y la asistencia al terminar`() {
        val p = plan(jueves.atTime(7, 0))
        assertEquals(listOf(jueves.atTime(9, 50)), p.de(TipoAviso.CLASE_PRONTO).filter { it.momento.toLocalDate() == jueves }.map { it.momento })
        assertEquals(listOf(jueves.atTime(12, 1)), p.de(TipoAviso.ASISTENCIA).filter { it.momento.toLocalDate() == jueves }.map { it.momento })
        assertEquals("1:2026-11-19", p.de(TipoAviso.CLASE_PRONTO).first().referencia)
    }

    @Test
    fun `lo que ya paso no se programa y una asistencia respondida no se vuelve a preguntar`() {
        val durante = plan(jueves.atTime(10, 30))
        assertTrue(durante.de(TipoAviso.CLASE_PRONTO).none { it.momento.toLocalDate() == jueves })
        assertEquals(1, durante.de(TipoAviso.ASISTENCIA).count { it.momento.toLocalDate() == jueves })

        val respondida = plan(jueves.atTime(10, 30), respondidas = setOf(1 to jueves))
        assertTrue(respondida.de(TipoAviso.ASISTENCIA).none { it.momento.toLocalDate() == jueves })
        assertFalse("La notificación de esa clase se retira", "clase:1:2026-11-19" in respondida.vigentes)
    }

    @Test
    fun `el resumen sale a las 9 de hoy o, si ya paso, a las 9 de manana`() {
        assertEquals(jueves.atTime(9, 0), plan(jueves.atTime(7, 0)).de(TipoAviso.RESUMEN_DIA).single().momento)
        assertEquals(jueves.plusDays(1).atTime(9, 0), plan(jueves.atTime(9, 0)).de(TipoAviso.RESUMEN_DIA).single().momento)
    }

    @Test
    fun `una tarea pendiente se avisa 1 hora y 5 minutos antes y al vencer, en la misma notificacion`() {
        val taller = Tarea(7, "Taller", "", "Cálculo", "2026-11-19", 1, horaEntrega = "18:00")
        val p = plan(jueves.atTime(8, 0), listOf(taller))
        assertEquals(
            listOf(jueves.atTime(17, 0), jueves.atTime(17, 55), jueves.atTime(18, 1)),
            p.programar.filter { it.referencia == "7" }.map { it.momento }
        )
        assertEquals(setOf("tarea:7"), p.programar.filter { it.referencia == "7" }.map { it.claveNotificacion }.toSet())
        assertTrue("tarea:7" in p.vigentes)
    }

    @Test
    fun `sin hora vence a las 23 59 y las tareas entregadas o calificadas no se avisan`() {
        val sinHora = Tarea(8, "Ensayo", "", "Historia", "2026-11-19", 2)
        assertEquals(jueves.atTime(22, 59), plan(jueves.atTime(8, 0), listOf(sinHora)).de(TipoAviso.TAREA_UNA_HORA).single().momento)

        val entregada = sinHora.copy(entregadaMillis = 1L)
        val calificada = sinHora.copy(id = 9, calificacion = 4f)
        val p = plan(jueves.atTime(8, 0), listOf(entregada, calificada))
        assertTrue(p.programar.none { it.tipo.clave.startsWith("tarea") || it.tipo == TipoAviso.TAREA_VENCIDA })
        assertFalse("tarea:8" in p.vigentes)
    }

    @Test
    fun `si se crea la tarea a 30 minutos del limite solo quedan el de 5 minutos y el de vencida`() {
        val urgente = Tarea(10, "Quiz", "", "Física", "2026-11-19", 1, horaEntrega = "18:00")
        val p = plan(jueves.atTime(17, 30), listOf(urgente))
        assertEquals(listOf(TipoAviso.TAREA_CINCO_MINUTOS, TipoAviso.TAREA_VENCIDA), p.programar.filter { it.referencia == "10" }.map { it.tipo })
    }

    @Test
    fun `textos de la tarea, la clase y la asistencia`() {
        val taller = Tarea(7, "Taller", "", "Cálculo", "2026-11-19", 1, horaEntrega = "18:00")
        val unaHora = TextosAvisos.tarea(taller, TipoAviso.TAREA_UNA_HORA, jueves.atTime(18, 0))
        assertEquals("Taller", unaHora.titulo)
        assertEquals("Vence en 1 hora · Cálculo · 18:00", unaHora.texto)
        assertEquals(listOf(TipoAccionAviso.ENTREGAR), unaHora.acciones.map { it.tipo })
        assertEquals(DestinoAviso.AbrirTarea(7, "Taller", "Cálculo"), unaHora.destino)
        assertEquals(TonoAviso.URGENTE, TextosAvisos.tarea(taller, TipoAviso.TAREA_CINCO_MINUTOS, jueves.atTime(18, 0)).tono)

        val clase = AsistenciaReglas.claseEnFecha(calculo, jueves, zona)!!
        assertEquals("Cálculo en 10 minutos", TextosAvisos.clasePronto(clase, "Ana").titulo)
        assertEquals("10:00 – 12:00 · Ana", TextosAvisos.clasePronto(clase, "Ana").texto)
        val asistencia = TextosAvisos.asistencia(clase)
        assertEquals("¿Fuiste a Cálculo?", asistencia.titulo)
        assertEquals(listOf(TipoAccionAviso.ASISTI, TipoAccionAviso.NO_ASISTI), asistencia.acciones.map { it.tipo })
        assertEquals("clase:1:2026-11-19", asistencia.claveNotificacion)
    }

    @Test
    fun `el resumen del dia cuenta tareas y eventos y no sale si no hay nada`() {
        assertNull(TextosAvisos.resumenDelDia("2026-11-19", emptyList(), emptyList()))

        val tareas = listOf(Tarea(1, "Taller", "", "Cálculo", "2026-11-19", 1, horaEntrega = "18:00"))
        val eventos = listOf(Evento(2, "Feria", "", "2026-11-19", 1, "08:00", "12:00"), Evento(3, "Charla", "", "2026-11-19", 1))
        val resumen = TextosAvisos.resumenDelDia("2026-11-19", tareas, eventos)!!
        assertEquals("Buenos días: hoy tienes 1 tarea y 2 eventos", resumen.titulo)
        assertEquals("Taller y 2 más", resumen.texto)
        assertEquals(listOf("Tarea: Taller · Cálculo · hasta las 18:00", "Evento: Feria · 08:00 – 12:00", "Evento: Charla"), resumen.lineas)
    }

    @Test
    fun `las claves de las alarmas ya programadas no cambian`() {
        assertEquals(listOf("clase", "asistencia", "resumen", "tarea1h", "tarea5m", "vencida"), TipoAviso.entries.map { it.clave })
        TipoAviso.entries.forEach { assertEquals(it, TipoAviso.deClave(it.clave)) }
        assertEquals(1 to jueves, ReglasAvisos.leerReferenciaClase("1:2026-11-19"))
        assertNull(ReglasAvisos.leerReferenciaClase("basura"))
    }
}
