package com.softcg.myapplication.core.ex

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun `la fecha se formatea con dos digitos y el mes empieza en 0`() {
        assertEquals("2026-01-05", formatFecha(2026, 0, 5))
        assertEquals("2026-12-31", formatFecha(2026, 11, 31))
    }

    @Test
    fun `la hora se formatea en 24 h con dos digitos`() {
        assertEquals("08:05", formatHora(8, 5))
        assertEquals("23:30", formatHora(23, 30))
    }
}
