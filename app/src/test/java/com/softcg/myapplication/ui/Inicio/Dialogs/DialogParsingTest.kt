package com.softcg.myapplication.ui.Inicio.Dialogs

import com.softcg.myapplication.domain.prediccion.CatalogoModelo
import com.softcg.myapplication.domain.prediccion.DeteccionArea
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `la calificacion numerica se convierte a Float`() {
        assertEquals(4.5F, CalificacionDialog.valorDesdeTexto("4.5"))
        assertEquals(3F, CalificacionDialog.valorDesdeTexto("3"))
    }

    @Test
    fun `una calificacion vacia o invalida se guarda como 0`() {
        assertEquals(0F, CalificacionDialog.valorDesdeTexto(""))
        assertEquals(0F, CalificacionDialog.valorDesdeTexto("."))
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
    fun `el tipo de calificacion usa el texto propio con Otro y vacio sin elegir`() {
        assertEquals("Letras (A-F)", AsignaturaDialog.tipoCalificacion("Letras (A-F)", ""))
        assertEquals("Por rúbrica", AsignaturaDialog.tipoCalificacion("Otro", " Por rúbrica "))
        assertEquals("", AsignaturaDialog.tipoCalificacion("Selecciona un tipo", "x"))
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
}
