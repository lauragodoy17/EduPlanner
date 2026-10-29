package com.softcg.myapplication.ui.Inicio.Dialogs

import org.junit.Assert.assertEquals
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
}
