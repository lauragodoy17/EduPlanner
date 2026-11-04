package com.softcg.myapplication.domain.gamificacion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NivelesYRachaTest {

    // 2026-11-02 es lunes
    private val lunes = LocalDate.of(2026, 11, 2)

    private fun dias(vararg desplazamientos: Int) = desplazamientos.map { lunes.plusDays(it.toLong()) }.toSet()

    // Niveles

    @Test
    fun `sin XP se empieza en el nivel 1`() {
        val progreso = NivelesConfig.progreso(0)
        assertEquals(1, progreso.nivel.numero)
        assertEquals(60, progreso.xpRestante)
        assertEquals(0f, progreso.fraccion)
    }

    @Test
    fun `el limite exacto de un nivel ya cuenta como ese nivel`() {
        assertEquals(1, NivelesConfig.progreso(59).nivel.numero)
        assertEquals(2, NivelesConfig.progreso(60).nivel.numero)
    }

    @Test
    fun `la fraccion es el avance dentro del nivel, no sobre el total`() {
        val progreso = NivelesConfig.progreso(105) // Nivel 2: de 60 a 150
        assertEquals(0.5f, progreso.fraccion, 0.001f)
        assertEquals(45, progreso.xpRestante)
    }

    @Test
    fun `en el nivel maximo no hay siguiente y el avance queda completo`() {
        val progreso = NivelesConfig.progreso(99_999)
        assertEquals(10, progreso.nivel.numero)
        assertNull(progreso.siguiente)
        assertEquals(1f, progreso.fraccion)
        assertEquals(0, progreso.xpRestante)
    }

    @Test
    fun `los niveles estan ordenados y cada uno pide mas que el anterior`() {
        NivelesConfig.niveles.zipWithNext { a, b ->
            assertEquals(a.numero + 1, b.numero)
            assertTrue(b.xpDesde > a.xpDesde)
        }
        // Cada salto es mayor que el anterior: subir nunca se vuelve más fácil
        val saltos = NivelesConfig.niveles.zipWithNext { a, b -> b.xpDesde - a.xpDesde }
        saltos.zipWithNext { a, b -> assertTrue(b > a) }
    }

    // Racha

    @Test
    fun `sin actividad no hay racha`() {
        val racha = RachaReglas.calcular(emptySet(), lunes)
        assertEquals(0, racha.actual)
        assertEquals(0, racha.mejor)
        assertFalse(racha.enRiesgo)
    }

    @Test
    fun `dias seguidos hasta hoy cuentan todos`() {
        val racha = RachaReglas.calcular(dias(0, 1, 2), lunes.plusDays(2))
        assertEquals(3, racha.actual)
        assertTrue(racha.activaHoy)
    }

    @Test
    fun `hoy sin actividad todavia no rompe la racha`() {
        val racha = RachaReglas.calcular(dias(0, 1), lunes.plusDays(2))
        assertEquals(2, racha.actual)
        assertFalse(racha.activaHoy)
        assertFalse(racha.enRiesgo)
    }

    @Test
    fun `un dia sin actividad por semana se perdona y no suma`() {
        // Lunes, martes, (miércoles libre), jueves
        val racha = RachaReglas.calcular(dias(0, 1, 3), lunes.plusDays(3))
        assertEquals(3, racha.actual)
        assertFalse(racha.descansoDisponible)
    }

    @Test
    fun `dos dias sin actividad en la misma semana rompen la racha`() {
        // Lunes y jueves: martes y miércoles libres
        val racha = RachaReglas.calcular(dias(0, 3), lunes.plusDays(3))
        assertEquals(1, racha.actual)
        assertEquals(1, racha.mejor)
    }

    @Test
    fun `cada semana tiene su propio descanso`() {
        // Domingo libre de una semana y lunes libre de la siguiente: uno por semana, sigue viva
        val racha = RachaReglas.calcular(dias(5, 8), lunes.plusDays(8))
        assertEquals(2, racha.actual)
    }

    @Test
    fun `sin actividad durante dias la racha actual es cero pero se conserva la mejor`() {
        val racha = RachaReglas.calcular(dias(0, 1, 2, 3), lunes.plusDays(20))
        assertEquals(0, racha.actual)
        assertEquals(4, racha.mejor)
    }

    @Test
    fun `tras el descanso usado, un dia sin actividad deja la racha en riesgo`() {
        // Ayer ya se gastó el descanso: si hoy termina sin actividad, se pierde
        val racha = RachaReglas.calcular(dias(0, 1), lunes.plusDays(3))
        assertEquals(2, racha.actual)
        assertTrue(racha.enRiesgo)
    }

    @Test
    fun `los dias futuros se ignoran`() {
        val racha = RachaReglas.calcular(dias(0, 9), lunes)
        assertEquals(1, racha.actual)
    }

    // Entrega a tiempo

    @Test
    fun `una tarea entregada el mismo dia que vence esta a tiempo`() {
        assertTrue(ReglasEntrega.aTiempo("2026-11-02", lunes))
        assertTrue(ReglasEntrega.aTiempo("2026-11-03", lunes))
        assertFalse(ReglasEntrega.aTiempo("2026-11-01", lunes))
    }

    @Test
    fun `una fecha que no se puede leer no cuenta como a tiempo`() {
        assertFalse(ReglasEntrega.aTiempo("", lunes))
        assertFalse(ReglasEntrega.aTiempo("mañana", lunes))
    }

    // Mensajes

    private fun situacion(
        xp: Int = 0, racha: Racha = Racha(0, 0, false, true, false), porReclamar: Int = 0,
        hoyVencen: Int = 0, mananaVencen: Int = 0, hora: Int = 10
    ) = SituacionEstudiante(NivelesConfig.progreso(xp), racha, porReclamar, hoyVencen, mananaVencen, hora, lunes)

    @Test
    fun `lo mas urgente gana y el mensaje usa datos reales`() {
        assertEquals("Tienes 2 recompensas listas para reclamar.", MensajesMotivacion.para(situacion(porReclamar = 2, hoyVencen = 1)))
        assertEquals("Hoy vence 1 tarea: entrégala a tiempo y gana el bono.", MensajesMotivacion.para(situacion(hoyVencen = 1)))
        assertEquals("Te faltan 10 XP para ser Organizado.", MensajesMotivacion.para(situacion(xp = 50)))
    }

    @Test
    fun `el aviso de racha en riesgo espera a la tarde`() {
        val enRiesgo = Racha(5, 5, false, false, true)
        assertTrue(MensajesMotivacion.para(situacion(racha = enRiesgo, hora = 19)).contains("racha de 5 días"))
        assertFalse(MensajesMotivacion.para(situacion(racha = enRiesgo, hora = 10)).contains("racha de 5 días"))
    }
}
