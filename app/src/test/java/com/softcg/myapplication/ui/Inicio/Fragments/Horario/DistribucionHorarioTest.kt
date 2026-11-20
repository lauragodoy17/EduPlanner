package com.softcg.myapplication.ui.Inicio.Fragments.Horario

import com.softcg.myapplication.ui.Inicio.Fragments.Horario.Models.DistribucionHorario
import com.softcg.myapplication.ui.Inicio.Fragments.Horario.Models.Ubicacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DistribucionHorarioTest {

    private fun h(hora: Int, min: Int = 0) = hora * 60 + min

    @Test
    fun `una clase sola usa todo el ancho`() {
        assertEquals(listOf(Ubicacion(0, 1)), DistribucionHorario.distribuir(listOf(h(8) to h(10))))
    }

    @Test
    fun `dos clases a la misma hora van en dos columnas`() {
        assertEquals(
            listOf(Ubicacion(0, 2), Ubicacion(1, 2)),
            DistribucionHorario.distribuir(listOf(h(8) to h(10), h(9) to h(11)))
        )
    }

    @Test
    fun `clases seguidas no se cruzan`() {
        assertEquals(
            listOf(Ubicacion(0, 1), Ubicacion(0, 1)),
            DistribucionHorario.distribuir(listOf(h(8) to h(10), h(10) to h(12)))
        )
    }

    @Test
    fun `tres a la vez dan tres columnas y una columna libre se reutiliza`() {
        // 8-12 cruza con 8-9, 9-10 y 10-11; 9-10 reutiliza la columna de 8-9: el grupo necesita dos columnas
        val r = DistribucionHorario.distribuir(listOf(h(8) to h(12), h(8) to h(9), h(9) to h(10), h(10) to h(11)))
        assertEquals(listOf(Ubicacion(0, 2), Ubicacion(1, 2), Ubicacion(1, 2), Ubicacion(1, 2)), r)
        val tres = DistribucionHorario.distribuir(listOf(h(14) to h(16), h(14) to h(15), h(14, 30) to h(15, 30)))
        assertEquals(setOf(0, 1, 2), tres.map { it.columna }.toSet())
        assertEquals(setOf(3), tres.map { it.columnas }.toSet())
    }

    @Test
    fun `grupos separados del mismo dia se reparten por su cuenta`() {
        val r = DistribucionHorario.distribuir(listOf(h(7) to h(9), h(8) to h(9), h(14) to h(16)))
        assertEquals(listOf(Ubicacion(0, 2), Ubicacion(1, 2), Ubicacion(0, 1)), r)
    }

    @Test
    fun `lee las horas guardadas como antes`() {
        assertEquals(h(14), DistribucionHorario.minutos("14: 0"))
        assertEquals(h(8, 30), DistribucionHorario.minutos("08:30"))
        assertNull(DistribucionHorario.minutos(""))
        assertNull(DistribucionHorario.minutos("25:00"))
    }
}
