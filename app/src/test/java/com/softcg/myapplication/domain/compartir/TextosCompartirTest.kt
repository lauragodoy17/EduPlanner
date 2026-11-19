package com.softcg.myapplication.domain.compartir

import com.softcg.myapplication.domain.compartir.ContenidoCompartido.AsignaturaCompartida
import com.softcg.myapplication.domain.compartir.ContenidoCompartido.TareaCompartida
import org.junit.Assert.assertEquals
import org.junit.Test

class TextosCompartirTest {

    @Test
    fun `los detalles muestran solo lo que tiene valor y con formato legible`() {
        val tarea = TareaCompartida("Taller", "", "Cálculo", "2026-11-20", 1)
        assertEquals(
            listOf("Asignatura" to "Cálculo", "Fecha de entrega" to "20/11/2026", "Prioridad" to "Alta"),
            TextosCompartir.detalles(tarea)
        )

        val asignatura = AsignaturaCompartida("Arte", "", 2, "08:00", listOf(true, false, true, false, false, false, false), "", "Arte", null, null)
        assertEquals(
            listOf("Días" to "Lunes, Miércoles", "Hora de inicio" to "08:00", "Duración" to "2 h", "Área" to "Arte"),
            TextosCompartir.detalles(asignatura)
        )
    }

    @Test
    fun `el aviso final dice que se agrego y si hizo falta crear la asignatura`() {
        assertEquals("Tarea agregada", TextosCompartir.resultado(ResultadoImportacion.Importado(TipoCompartido.TAREA, "Taller", null)))
        assertEquals("Evento agregado", TextosCompartir.resultado(ResultadoImportacion.Importado(TipoCompartido.EVENTO, "Feria", null)))
        assertEquals(
            "Nota agregada · se creó la asignatura «Química»",
            TextosCompartir.resultado(ResultadoImportacion.Importado(TipoCompartido.NOTA, "Parcial", "Química"))
        )
    }
}
