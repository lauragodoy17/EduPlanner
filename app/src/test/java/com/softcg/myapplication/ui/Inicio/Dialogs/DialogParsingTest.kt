package com.softcg.myapplication.ui.Inicio.Dialogs

import com.softcg.myapplication.domain.prediccion.CatalogoModelo
import com.softcg.myapplication.domain.prediccion.DeteccionArea
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DialogParsingTest {

    @Test
    fun `la prioridad de texto se traduce a su numero`() {
        assertEquals(1, TareaDialog.prioridadDesdeTexto("Alta"))
        assertEquals(2, TareaDialog.prioridadDesdeTexto("Media"))
        assertEquals(3, TareaDialog.prioridadDesdeTexto("Baja"))
    }

    @Test
    fun `sin prioridad elegida se guarda 0`() {
        assertEquals(0, TareaDialog.prioridadDesdeTexto(""))
        assertEquals(0, TareaDialog.prioridadDesdeTexto("Urgente"))
    }

    @Test
    fun `la tarea solo acepta una asignatura existente`() {
        val asignaturas = listOf("Física", "Química")
        assertTrue(TareaDialog.asignaturaValida("Física", asignaturas))
        assertFalse(TareaDialog.asignaturaValida("", asignaturas))
        assertFalse(TareaDialog.asignaturaValida("+ Agregar asignatura", asignaturas))
        assertFalse(TareaDialog.asignaturaValida("Historia", asignaturas))
    }

    @Test
    fun `la duracion vacia o invalida se guarda como 0`() {
        assertEquals(2, AsignaturaDialog.duracionDesdeTexto(" 2 "))
        assertEquals(0, AsignaturaDialog.duracionDesdeTexto(""))
        assertEquals(0, AsignaturaDialog.duracionDesdeTexto("dos"))
        assertEquals(0, AsignaturaDialog.duracionDesdeTexto("-3"))
    }

    @Test
    fun `al preguntar el area van primero las candidatas y luego todas las demas`() {
        val opciones = AreaAsignaturaDialog.opciones(DeteccionArea.Ambigua(listOf("Historia", "Arte")))
        assertEquals(listOf("Historia", "Arte"), opciones.take(2))
        assertEquals(CatalogoModelo.AREAS.toSet(), opciones.toSet())
        assertEquals(CatalogoModelo.AREAS.size, opciones.size)
        assertEquals(CatalogoModelo.AREAS, AreaAsignaturaDialog.opciones(DeteccionArea.Ninguna))
    }

    @Test
    fun `las horas de estudio aceptan coma o punto decimal`() {
        assertEquals(3f, EstudioDeclaradoDialog.horasDesdeTexto("3"))
        assertEquals(2.5f, EstudioDeclaradoDialog.horasDesdeTexto(" 2,5 "))
        assertEquals(2.5f, EstudioDeclaradoDialog.horasDesdeTexto("2.5"))
        assertEquals(null, EstudioDeclaradoDialog.horasDesdeTexto(""))
        assertEquals(null, EstudioDeclaradoDialog.horasDesdeTexto("tres"))
    }

    @Test
    fun `al crear una asignatura solo se abren sus datos si falta el area o la escala`() {
        val base = Asignatura(1, "Cálculo", "", 0, "", List(7) { false }, tipoCalificacion = "Numérico (0-10)", areaModelo = "Matematicas")
        assertFalse(DatosAsignaturaDialog.hayQuePreguntar(base))
        assertTrue(DatosAsignaturaDialog.hayQuePreguntar(base.copy(areaModelo = null)))
        assertTrue(DatosAsignaturaDialog.hayQuePreguntar(base.copy(tipoCalificacion = "Letras (A-F)")))
        assertFalse(DatosAsignaturaDialog.hayQuePreguntar(base.copy(tipoCalificacion = "", escalaMin = 1f, escalaMax = 5f)))
    }

    @Test
    fun `la tarea necesita una fecha real`() {
        assertEquals(java.time.LocalDate.of(2026, 11, 20), TareaDialog.fechaValida("2026-11-20"))
        assertNull(TareaDialog.fechaValida(""))
        assertNull(TareaDialog.fechaValida("mañana"))
    }

    @Test
    fun `la duracion de una clase va de 1 a 12 horas o vacia`() {
        assertNull(AsignaturaDialog.errorDuracion(""))
        assertNull(AsignaturaDialog.errorDuracion(" 2 "))
        assertNull(AsignaturaDialog.errorDuracion("12"))
        assertEquals("Entre 1 y 12 horas", AsignaturaDialog.errorDuracion("0"))
        assertEquals("Entre 1 y 12 horas", AsignaturaDialog.errorDuracion("30"))
    }
}
