package com.softcg.myapplication.domain.gamificacion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ReglasEntregaTest {

    @Test
    fun `sin hora la tarea vence a las 23 59 de su dia`() {
        assertEquals(LocalDateTime.of(2026, 11, 20, 23, 59), ReglasEntrega.limite("2026-11-20", null))
        assertEquals(LocalDateTime.of(2026, 11, 20, 23, 59), ReglasEntrega.limite("2026-11-20", "no es hora"))
        assertEquals(LocalDateTime.of(2026, 11, 20, 14, 30), ReglasEntrega.limite("2026-11-20", "14:30"))
        assertNull(ReglasEntrega.limite("mañana", "14:30"))
    }

    @Test
    fun `a tiempo hasta el minuto limite incluido`() {
        assertTrue(ReglasEntrega.aTiempo("2026-11-20", "14:30", LocalDateTime.of(2026, 11, 20, 14, 30, 40)))
        assertFalse(ReglasEntrega.aTiempo("2026-11-20", "14:30", LocalDateTime.of(2026, 11, 20, 14, 31)))
        assertTrue(ReglasEntrega.aTiempo("2026-11-20", null, LocalDateTime.of(2026, 11, 20, 23, 59, 30)))
        assertFalse(ReglasEntrega.aTiempo("2026-11-20", null, LocalDateTime.of(2026, 11, 21, 0, 0)))
    }
}
