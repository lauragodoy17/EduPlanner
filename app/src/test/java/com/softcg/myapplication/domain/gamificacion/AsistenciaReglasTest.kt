package com.softcg.myapplication.domain.gamificacion

import com.softcg.myapplication.core.ZONA_PRUEBAS
import com.softcg.myapplication.core.millis
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class AsistenciaReglasTest {

    /** Miércoles 28 de octubre de 2026. */
    private val fecha = LocalDate.of(2026, 10, 28)

    private fun asignatura(
        id: Int = 1,
        nombre: String = "Física",
        hora: String = "14:00",
        duracion: Int = 2,
        dia: LocalDate = fecha,
        registro: LocalDateTime = LocalDateTime.of(2026, 10, 1, 8, 0)
    ) = Asignatura(
        id = id, nombre = nombre, tutor = "Ana", duracion = duracion, hora = hora,
        horario = List(7) { it == dia.dayOfWeek.value - 1 }, fechaRegistro = registro.millis()
    )

    private fun clase(a: Asignatura = asignatura(), f: LocalDate = fecha) =
        AsistenciaReglas.claseEnFecha(a, f, ZONA_PRUEBAS)!!

    private fun estadoA(hora: LocalDateTime, c: ClaseProgramada = clase()) =
        AsistenciaReglas.estado(c, null, hora)

    // Casos 1 a 4: clase del 28 de octubre de 14:00 a 16:00

    @Test
    fun `1 antes de las 4 pm la clase sigue pendiente y no se puede confirmar`() {
        assertEquals(EstadoAsistencia.PENDIENTE, estadoA(fecha.atTime(15, 59, 59)))
        assertEquals(EstadoAsistencia.PENDIENTE, estadoA(fecha.atTime(14, 30)))
    }

    @Test
    fun `2 a las 4 pm en punto y despues se puede confirmar`() {
        assertEquals(EstadoAsistencia.DISPONIBLE, estadoA(fecha.atTime(16, 0)))
        assertEquals(EstadoAsistencia.DISPONIBLE, estadoA(fecha.atTime(20, 15)))
    }

    @Test
    fun `3 a las 11 59 59 pm del mismo dia todavia se puede confirmar`() {
        assertEquals(EstadoAsistencia.DISPONIBLE, estadoA(fecha.atTime(23, 59, 59)))
        assertEquals(EstadoAsistencia.DISPONIBLE, estadoA(fecha.atTime(LocalTime.of(23, 59, 59, 999_999_999))))
    }

    @Test
    fun `4 a las 12 00 00 am del dia siguiente el plazo ya vencio`() {
        assertEquals(LocalDateTime.of(2026, 10, 29, 0, 0), clase().plazo)
        assertEquals(EstadoAsistencia.VENCIDA, estadoA(LocalDateTime.of(2026, 10, 29, 0, 0, 0)))
        assertEquals(EstadoAsistencia.VENCIDA, estadoA(LocalDateTime.of(2026, 10, 29, 12, 0)))
    }

    @Test
    fun `8 una clase futura nunca se puede confirmar`() {
        val futura = clase(f = fecha.plusWeeks(1))
        assertEquals(EstadoAsistencia.PENDIENTE, estadoA(fecha.atTime(18, 0), futura))
    }

    @Test
    fun `una respuesta guardada manda sobre la hora`() {
        val c = clase()
        assertEquals(EstadoAsistencia.ASISTI, AsistenciaReglas.estado(c, true, fecha.atTime(18, 0)))
        assertEquals(EstadoAsistencia.NO_ASISTI, AsistenciaReglas.estado(c, false, fecha.plusDays(5).atTime(9, 0)))
    }

    // Caso 14 y clases largas o nocturnas

    @Test
    fun `14 una clase que termina despues de medianoche vence a las 00 00 tras el dia en que termina`() {
        val nocturna = clase(asignatura(hora = "23:00", duracion = 2))
        assertEquals(LocalDateTime.of(2026, 10, 29, 1, 0), nocturna.fin)
        assertEquals(LocalDateTime.of(2026, 10, 30, 0, 0), nocturna.plazo)

        assertEquals(EstadoAsistencia.PENDIENTE, estadoA(LocalDateTime.of(2026, 10, 29, 0, 30), nocturna))
        assertEquals(EstadoAsistencia.DISPONIBLE, estadoA(LocalDateTime.of(2026, 10, 29, 1, 0), nocturna))
        assertEquals(EstadoAsistencia.DISPONIBLE, estadoA(LocalDateTime.of(2026, 10, 29, 23, 59, 59), nocturna))
        assertEquals(EstadoAsistencia.VENCIDA, estadoA(LocalDateTime.of(2026, 10, 30, 0, 0), nocturna))
    }

    @Test
    fun `una clase de varias horas solo se habilita al terminar`() {
        val larga = clase(asignatura(hora = "8:00", duracion = 5))
        assertEquals(EstadoAsistencia.PENDIENTE, estadoA(fecha.atTime(12, 59), larga))
        assertEquals(EstadoAsistencia.DISPONIBLE, estadoA(fecha.atTime(13, 0), larga))
    }

    @Test
    fun `la hora se interpreta en los formatos que guarda la app`() {
        assertEquals(LocalTime.of(8, 5), AsistenciaReglas.parsearHora("8:5"))
        assertEquals(LocalTime.of(8, 5), AsistenciaReglas.parsearHora("8: 5"))
        assertEquals(LocalTime.of(14, 0), AsistenciaReglas.parsearHora("14:00"))
        assertNull(AsistenciaReglas.parsearHora(""))
        assertNull(AsistenciaReglas.parsearHora("25:00"))
        assertNull(AsistenciaReglas.parsearHora("abc"))
    }

    // Datos incompletos y registro retroactivo

    @Test
    fun `una clase sin hora valida o sin duracion nunca se puede confirmar`() {
        assertNull(AsistenciaReglas.claseEnFecha(asignatura(hora = ""), fecha, ZONA_PRUEBAS))
        assertNull(AsistenciaReglas.claseEnFecha(asignatura(duracion = 0), fecha, ZONA_PRUEBAS))
        assertNull(AsistenciaReglas.claseEnFecha(asignatura().copy(id = null), fecha, ZONA_PRUEBAS))
    }

    @Test
    fun `un dia sin clase no genera clase`() {
        assertNull(AsistenciaReglas.claseEnFecha(asignatura(), fecha.plusDays(1), ZONA_PRUEBAS))
    }

    @Test
    fun `una clase registrada despues de empezar no se puede confirmar`() {
        val registradaTarde = asignatura(registro = fecha.atTime(18, 0))
        assertNull(AsistenciaReglas.claseEnFecha(registradaTarde, fecha, ZONA_PRUEBAS))
        // La de la semana siguiente sí cuenta
        assertNotNull(AsistenciaReglas.claseEnFecha(registradaTarde, fecha.plusWeeks(1), ZONA_PRUEBAS))
    }

    // Casos 10, 11 y 13: lista de clases por confirmar

    @Test
    fun `11 varias clases pendientes se ordenan por hora de fin`() {
        val asignaturas = listOf(
            asignatura(id = 1, nombre = "Química", hora = "14:00", duracion = 2),
            asignatura(id = 2, nombre = "Física", hora = "8:00", duracion = 2),
            asignatura(id = 3, nombre = "Arte", hora = "19:00", duracion = 2)
        )
        val ahora = fecha.atTime(20, 0)

        val pendientes = AsistenciaReglas.clasesPorConfirmar(asignaturas, emptySet(), ahora, ZONA_PRUEBAS)

        // Arte aún no termina (21:00); las otras dos, de la que terminó antes a la última
        assertEquals(listOf("Física", "Química"), pendientes.map { it.nombre })
    }

    @Test
    fun `10 el estado se calcula con datos guardados y la hora actual, no con lo que se mostro antes`() {
        val asignaturas = listOf(asignatura())

        // Mismas entradas tras "reabrir" la app: el resultado depende solo de la hora
        assertEquals(1, AsistenciaReglas.clasesPorConfirmar(asignaturas, emptySet(), fecha.atTime(17, 0), ZONA_PRUEBAS).size)
        assertTrue(AsistenciaReglas.clasesPorConfirmar(asignaturas, emptySet(), fecha.plusDays(1).atTime(0, 0), ZONA_PRUEBAS).isEmpty())
    }

    @Test
    fun `una clase ya respondida no vuelve a preguntarse`() {
        val pendientes = AsistenciaReglas.clasesPorConfirmar(
            listOf(asignatura(id = 7)), setOf(7 to fecha), fecha.atTime(17, 0), ZONA_PRUEBAS
        )
        assertTrue(pendientes.isEmpty())
    }

    @Test
    fun `13 volver a crear una asignatura con otro horario no reabre una ventana ya cerrada`() {
        // La clase de 14 a 16 venció a medianoche; al día siguiente se registra de nuevo a las 23:00
        val ahora = LocalDateTime.of(2026, 10, 29, 0, 30)
        val recreada = asignatura(id = 2, hora = "23:00", duracion = 2, registro = ahora)

        val pendientes = AsistenciaReglas.clasesPorConfirmar(listOf(recreada), emptySet(), ahora, ZONA_PRUEBAS)

        assertTrue(pendientes.isEmpty())
    }

    @Test
    fun `una clase nocturna de ayer aparece hoy cuando termina`() {
        val nocturna = asignatura(hora = "23:00", duracion = 2)
        val pendientes = AsistenciaReglas.clasesPorConfirmar(
            listOf(nocturna), emptySet(), LocalDateTime.of(2026, 10, 29, 2, 0), ZONA_PRUEBAS
        )
        assertEquals(listOf(fecha), pendientes.map { it.fecha })
    }
}
