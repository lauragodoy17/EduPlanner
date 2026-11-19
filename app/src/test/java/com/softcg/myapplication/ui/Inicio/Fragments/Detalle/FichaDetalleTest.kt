package com.softcg.myapplication.ui.Inicio.Fragments.Detalle

import com.softcg.myapplication.ui.Inicio.Dialogs.AsignaturaDialog
import com.softcg.myapplication.ui.Inicio.Dialogs.TareaDialog
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models.Calificacion
import com.softcg.myapplication.ui.Inicio.Models.Evento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FichaDetalleTest {

    private val hoy = LocalDate.of(2026, 11, 18)

    @Test
    fun `un evento muestra fecha con plazo, horario y prioridad, sin filas vacias`() {
        val ficha = FichaDetalle.de(Evento(1, " Feria ", "Stand 4", "2026-11-21", 1, "08:00", "12:00", ""), hoy)
        assertEquals("Feria", ficha.titulo)
        assertEquals("Stand 4", ficha.descripcion)
        assertEquals(
            listOf("Fecha" to "21/11/2026 · faltan 3 días", "Horario" to "08:00 - 12:00", "Prioridad" to "Alta"),
            ficha.filas
        )
        assertNull("Sin imagen no se reserva espacio", ficha.imagenUri)

        val sinHora = FichaDetalle.de(Evento(1, "Charla", "", "2026-11-17", 0), hoy)
        assertEquals(listOf("Fecha" to "17/11/2026 · fue ayer"), sinHora.filas)
    }

    @Test
    fun `una asignatura muestra sus clases y el area con tildes`() {
        val ficha = FichaDetalle.de(
            Asignatura(1, "Cálculo", "", 2, "08:00", listOf(true, false, true, false, false, false, false), areaModelo = "Matematicas")
        )
        assertEquals(TipoDetalle.ASIGNATURA, ficha.tipo)
        assertEquals(
            listOf("Días de clase" to "Lunes, Miércoles", "Hora de inicio" to "08:00", "Duración" to "2 h", "Área" to "Matemáticas"),
            ficha.filas
        )
        assertEquals(
            listOf("Días de clase" to "Sin días asignados"),
            FichaDetalle.de(Asignatura(2, "Arte", "", 0, "", List(7) { false })).filas
        )
    }

    @Test
    fun `una calificacion se titula con su asignatura`() {
        val ficha = FichaDetalle.de(Calificacion(1, "Numerico (1-5)", 4.5f, "Física", "Primer corte"))
        assertEquals("Física", ficha.titulo)
        assertEquals("Primer corte", ficha.descripcion)
        assertEquals(listOf("Calificación" to "4,5", "Tipo" to "Numerico (1-5)"), ficha.filas)
    }

    @Test
    fun `la clave de cada tipo viaja en la navegacion`() {
        TipoDetalle.entries.forEach { assertEquals(it, TipoDetalle.deClave(it.clave)) }
        assertNull(TipoDetalle.deClave("otra"))
    }

    @Test
    fun `los formularios se rellenan con lo guardado al editar`() {
        assertEquals("Media", TareaDialog.textoPrioridad(2))
        assertEquals("", TareaDialog.textoPrioridad(0))
    }
}
