package com.softcg.myapplication.domain.prediccion

import org.junit.Assert.assertEquals
import org.junit.Test

class TextosAreaTest {

    @Test
    fun `explica por que se pregunta el area`() {
        assertEquals(
            "«Historia del arte» encaja con Historia o Arte: elige la correcta",
            TextosArea.pregunta("Historia del arte", DeteccionArea.Ambigua(listOf("Historia", "Arte")))
        )
        assertEquals(
            "No pudimos relacionar «Ética» con un área: elige la que más se parezca",
            TextosArea.pregunta("Ética", DeteccionArea.Ninguna)
        )
    }

    @Test
    fun `une las opciones con comas y o`() {
        assertEquals("Química", TextosArea.unir(listOf("Química")))
        assertEquals("Química o Física", TextosArea.unir(listOf("Química", "Física")))
        assertEquals("A, B o C", TextosArea.unir(listOf("A", "B", "C")))
    }
}
