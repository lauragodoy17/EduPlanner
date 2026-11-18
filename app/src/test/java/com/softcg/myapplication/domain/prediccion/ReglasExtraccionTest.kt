package com.softcg.myapplication.domain.prediccion

import com.softcg.myapplication.core.ZONA_PRUEBAS
import com.softcg.myapplication.core.millis
import com.softcg.myapplication.data.database.entities.AsistenciaEntity
import com.softcg.myapplication.data.database.entities.PercepcionEntity
import com.softcg.myapplication.data.database.entities.SesionEnfoqueEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ReglasExtraccionTest {

    private val hoy = LocalDate.of(2026, 11, 12)
    private val ahora = hoy.atTime(18, 0)

    private fun asistencia(fecha: LocalDate, asistio: Boolean) =
        AsistenciaEntity("u1", 3, fecha.toString(), asistio, 0, 0, 0)

    private fun sesion(inicio: LocalDateTime, minutos: Int, metodo: String? = null) =
        SesionEnfoqueEntity("u1", inicio.millis(), minutos, "Cálculo", inicio.millis() + minutos * 60_000L, asignaturaId = 3, metodo = metodo)

    private fun percepcion(tipo: String, cuando: LocalDateTime, valor: Float? = null, tema: String? = null, texto: String? = null) =
        PercepcionEntity("u1", tipo, "$tipo:${cuando.millis()}", 3, tema, valor, texto, cuando.millis())

    // ESCALAS Y NOTAS

    @Test
    fun `lee el rango de los tipos de calificacion`() {
        assertEquals(Escala(0f, 10f), ReglasExtraccion.escalaDeTexto("Numérico (0-10)"))
        assertEquals(Escala(0f, 100f), ReglasExtraccion.escalaDeTexto("Porcentaje (0-100%)"))
        assertEquals(Escala(1f, 5f), ReglasExtraccion.escalaDeTexto("Numerico (1-5)"))
        assertEquals(Escala(1f, 5f), ReglasExtraccion.escalaDeTexto("de 1 a 5"))
        assertEquals(Escala(0f, 5.5f), ReglasExtraccion.escalaDeTexto("0 - 5,5"))
        assertNull(ReglasExtraccion.escalaDeTexto("Letras (A-F)"))
        assertNull(ReglasExtraccion.escalaDeTexto("Aprobado/Reprobado"))
        assertNull(ReglasExtraccion.escalaDeTexto("Tarea"))
        assertNull(ReglasExtraccion.escalaDeTexto("5-1"))
    }

    @Test
    fun `la escala de la nota manda, despues la que fijo el estudiante y por ultimo la de la asignatura`() {
        assertEquals(Escala(1f, 5f), ReglasExtraccion.escalaDe("Numerico (1-5)", 0f, 100f, "Numérico (0-10)"))
        assertEquals(Escala(0f, 100f), ReglasExtraccion.escalaDe("Tarea", 0f, 100f, "Numérico (0-10)"))
        assertEquals(Escala(0f, 10f), ReglasExtraccion.escalaDe("Tarea", null, null, "Numérico (0-10)"))
        assertNull(ReglasExtraccion.escalaDe("Tarea", null, null, "Letras (A-F)"))
    }

    @Test
    fun `el promedio se lleva a 0-10 y no usa notas sin escala o fuera de ella`() {
        val medida = ReglasExtraccion.notas(
            listOf(
                4f to Escala(1f, 5f),    // 7,5
                80f to Escala(0f, 100f), // 8,0
                9f to null,              // sin escala: no se adivina
                7f to Escala(1f, 5f)     // fuera de 1-5: inconsistente
            )
        )
        assertEquals(7.75, medida.promedio10!!, 0.001)
        assertEquals(2, medida.usadas)
        assertEquals(1, medida.sinEscala)
        assertEquals(1, medida.fueraDeEscala)
        assertNull(ReglasExtraccion.notas(emptyList()).promedio10)
    }

    // ASISTENCIA

    @Test
    fun `la asistencia es el porcentaje de clases respondidas, en escala 0-10 para el modelo`() {
        val registros = listOf(
            asistencia(hoy.minusDays(60), false),
            asistencia(hoy.minusDays(40), true),
            asistencia(hoy.minusDays(10), true),
            asistencia(hoy.minusDays(3), true)
        )
        val medida = ReglasExtraccion.asistencia(registros, hoy)
        assertEquals(4, medida.respondidas)
        assertEquals(75.0, medida.porcentaje, 0.001)
        assertEquals(7.5, ReglasExtraccion.asistenciaModelo(medida), 0.001)
        // Evolución: las últimas 4 semanas
        assertEquals(100.0, medida.porcentajeReciente!!, 0.001)
    }

    // TIEMPO DE ESTUDIO

    @Test
    fun `las horas por semana se calculan sobre las ultimas 4 semanas`() {
        val sesiones = listOf(
            sesion(ahora.minusDays(60), 600), // fuera de la ventana
            sesion(ahora.minusDays(20), 120),
            sesion(ahora.minusDays(5), 60),
            sesion(ahora.minusDays(1), 60)
        )
        val medida = ReglasExtraccion.intensidad(sesiones, ahora.millis(), ZONA_PRUEBAS)!!
        assertEquals(4.0 / 4.0, medida.horasPorSemana, 0.001) // 4 h en 4 semanas
        assertEquals(3, medida.sesiones)
        assertTrue(medida.diasDeHistorial > ReglasExtraccion.MIN_DIAS_HISTORIAL_ESTUDIO)
    }

    @Test
    fun `con poco historial la ventana es desde la primera sesion`() {
        val sesiones = listOf(sesion(ahora.minusDays(6), 90), sesion(ahora, 90))
        val medida = ReglasExtraccion.intensidad(sesiones, ahora.millis(), ZONA_PRUEBAS)!!
        assertEquals(7L, medida.diasDeHistorial)
        assertEquals(3.0, medida.horasPorSemana, 0.001) // 3 h en 1 semana
        assertNull(ReglasExtraccion.intensidad(emptyList(), ahora.millis(), ZONA_PRUEBAS))
    }

    @Test
    fun `semanas sin estudiar cuentan como cero horas, no como dato faltante`() {
        val medida = ReglasExtraccion.intensidad(listOf(sesion(ahora.minusDays(40), 60)), ahora.millis(), ZONA_PRUEBAS)!!
        assertEquals(0.0, medida.horasPorSemana, 0.001)
    }

    // PERCEPCIONES

    @Test
    fun `el interes es la respuesta mas reciente y caduca a los 60 dias`() {
        val vieja = percepcion(ReglasExtraccion.TIPO_INTERES, ahora.minusDays(90), 2.5f)
        val reciente = percepcion(ReglasExtraccion.TIPO_INTERES, ahora.minusDays(10), 7.5f)
        val interes = ReglasExtraccion.interes(listOf(vieja, reciente))!!
        assertEquals(7.5f, interes.valor)
        assertTrue(ReglasExtraccion.interesVigente(interes, ahora.millis()))
        assertFalse(ReglasExtraccion.interesVigente(vieja, ahora.millis()))
    }

    @Test
    fun `la dificultad usa las respuestas del tema y si no hay, las de la asignatura`() {
        val respuestas = listOf(
            percepcion(ReglasExtraccion.TIPO_DIFICULTAD, ahora.minusDays(3), 10f, tema = "Integrales"),
            percepcion(ReglasExtraccion.TIPO_DIFICULTAD, ahora.minusDays(2), 5f, tema = "Integrales"),
            percepcion(ReglasExtraccion.TIPO_DIFICULTAD, ahora.minusDays(1), 0f, tema = "Trigonometria")
        )
        assertEquals(7.5 to 2, ReglasExtraccion.dificultad(respuestas, "Integrales"))
        assertEquals(5.0 to 3, ReglasExtraccion.dificultad(respuestas, "Algebra lineal"))
        assertNull(ReglasExtraccion.dificultad(emptyList(), null))
    }

    @Test
    fun `el metodo es el mas usado en lo ultimo, y en empate el mas reciente`() {
        val sesiones = listOf(
            sesion(ahora.minusDays(4), 25, "Pomodoro"),
            sesion(ahora.minusDays(3), 25, "Pomodoro"),
            sesion(ahora.minusDays(2), 25, "inventado"), // fuera del catálogo: no cuenta
            sesion(ahora.minusDays(1), 25, null)
        )
        val declarado = percepcion(ReglasExtraccion.TIPO_METODO, ahora, texto = "Mapas mentales")
        assertEquals("Pomodoro", ReglasExtraccion.metodo(sesiones, listOf(declarado)))
        assertEquals("Mapas mentales", ReglasExtraccion.metodo(sesiones.take(1), listOf(declarado)))
        assertNull(ReglasExtraccion.metodo(listOf(sesion(ahora, 25)), emptyList()))
    }
}
