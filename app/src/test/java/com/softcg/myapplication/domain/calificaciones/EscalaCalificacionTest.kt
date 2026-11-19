package com.softcg.myapplication.domain.calificaciones

import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.data.Repositories.CalificacionesRespository
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EscalaCalificacionTest {

    private val unoACinco = EscalaCalificacion.de(TipoEscala.UNO_A_CINCO)
    private val letras = EscalaCalificacion.de(TipoEscala.LETRAS)
    private val aprobado = EscalaCalificacion.de(TipoEscala.APROBADO)
    private val porcentaje = EscalaCalificacion.de(TipoEscala.PORCENTAJE)

    @Test
    fun `una nota se ajusta al rango y al paso de su escala`() {
        assertEquals(5f, unoACinco.ajustar(7f))
        assertEquals(1f, unoACinco.ajustar(-2f))
        assertEquals(4.5f, unoACinco.ajustar(4.46f))
        assertEquals(86f, porcentaje.ajustar(85.6f))
        assertTrue(unoACinco.contiene(4.5f))
        assertFalse(unoACinco.contiene(0.5f))
        assertFalse(unoACinco.contiene(5.5f))
    }

    @Test
    fun `cada escala muestra la nota a su manera`() {
        assertEquals("4,5", unoACinco.formatear(4.5f))
        assertEquals("4", unoACinco.formatear(4f))
        assertEquals("85 %", porcentaje.formatear(85f))
        assertEquals("A", letras.formatear(4f))
        assertEquals("F", letras.formatear(0f))
        assertEquals("Aprobado", aprobado.formatear(1f))
        assertEquals("Reprobado", aprobado.formatear(0f))
    }

    @Test
    fun `letras y aprobado solo admiten sus opciones y se guardan como numeros`() {
        assertEquals(listOf("A", "B", "C", "D", "F"), letras.opciones!!.map { it.second })
        assertTrue(letras.contiene(3f))
        assertFalse(letras.contiene(3.5f))
        assertFalse(aprobado.contiene(0.5f))
    }

    @Test
    fun `el mismo rendimiento vale lo mismo para el modelo en cualquier escala numerica`() {
        // 60 % del máximo: 3 de 5, 6 de 10, 60 de 100
        assertEquals(6.0, unoACinco.a10(3f), 1e-9)
        assertEquals(6.0, EscalaCalificacion.de(TipoEscala.CERO_A_DIEZ).a10(6f), 1e-9)
        assertEquals(6.0, porcentaje.a10(60f), 1e-9)
        assertEquals(10.0, unoACinco.a10(5f), 1e-9)
        assertEquals(2.0, unoACinco.a10(1f), 1e-9)
        assertEquals(8.5, porcentaje.a10(85f), 1e-9)
        // Sin arrastrar el error del Float: 4,3 de 5 es exactamente 8,6
        assertEquals(8.6, unoACinco.a10(4.3f), 1e-9)
        assertEquals(7.3, EscalaCalificacion.de(TipoEscala.CERO_A_DIEZ).a10(7.3f), 1e-9)
    }

    @Test
    fun `letras y aprobado valen el punto medio de su banda`() {
        assertEquals(listOf(9.5, 8.5, 7.5, 6.5, 3.0), letras.opciones!!.map { letras.a10(it.first) })
        assertEquals(8.0, aprobado.a10(1f), 1e-9)
        assertEquals(3.0, aprobado.a10(0f), 1e-9)
    }

    @Test
    fun `solo se pueden elegir las escalas de la app`() {
        assertEquals(
            listOf(TipoEscala.CERO_A_DIEZ, TipoEscala.UNO_A_CINCO, TipoEscala.PORCENTAJE, TipoEscala.LETRAS, TipoEscala.APROBADO),
            TipoEscala.ELEGIBLES
        )
        assertTrue(EscalaCalificacion.PREDEFINIDAS.none { it.tipo == TipoEscala.PERSONALIZADA })
    }

    @Test
    fun `la escala guardada se vuelve a leer igual`() {
        EscalaCalificacion.PREDEFINIDAS.forEach { escala ->
            assertEquals(escala, EscalaCalificacion.deAsignatura(escala.etiquetaGuardada, escala.min, escala.max))
        }
    }

    @Test
    fun `asignaturas antiguas conservan su escala o la piden si no es fiable`() {
        // Sin rango propio: la del tipo, si es numérica
        assertEquals(EscalaCalificacion.de(TipoEscala.CERO_A_DIEZ), EscalaCalificacion.deAsignatura("Numérico (0-10)", null, null))
        // El rango que indicó el estudiante manda; uno que no es de la app se sigue leyendo (sus notas ya existen)
        assertEquals(unoACinco, EscalaCalificacion.deAsignatura("Otro", 1f, 5f))
        val antigua = EscalaCalificacion.deAsignatura("Otro", 1f, 7f)!!
        assertEquals(TipoEscala.PERSONALIZADA, antigua.tipo)
        assertEquals(10.0 * 4 / 7, antigua.a10(4f), 1e-9)
        assertEquals(TipoEscala.PERSONALIZADA, EscalaCalificacion.deAsignatura("Letras (A-F)", 0f, 5f)!!.tipo)
        // Letras sin rango: sus notas eran números libres, hay que preguntar
        assertNull(EscalaCalificacion.deAsignatura("Letras (A-F)", null, null))
        assertNull(EscalaCalificacion.deAsignatura("", null, null))
        // El tipo de una nota de tarea no dice escala
        assertNull(EscalaCalificacion.deTexto("Tarea"))
        assertEquals(unoACinco, EscalaCalificacion.deTexto("Numerico (1-5)"))
    }

    // Bloqueo

    private val asignaturas: AsignaturasRepository = mockk(relaxed = true)
    private val calificaciones: CalificacionesRespository = mockk()
    private val bloqueada = EscalaBloqueadaUseCase(calificaciones)
    private val guardar = GuardarEscalaAsignaturaUseCase(asignaturas, bloqueada)
    private val calculo = Asignatura(
        3, "Cálculo", "", 0, "", List(7) { false },
        tipoCalificacion = unoACinco.etiquetaGuardada, escalaMin = 1f, escalaMax = 5f
    )

    @Test
    fun `sin notas la escala se puede cambiar`() = runBlocking {
        coEvery { asignaturas.getAsignatura(3) } returns calculo
        coEvery { calificaciones.contarDeAsignatura(3, "Cálculo") } returns 0
        coEvery { asignaturas.guardarEscala(3, any()) } returns true

        assertEquals(ResultadoEscala.Guardada, guardar(3, porcentaje))
        coVerify { asignaturas.guardarEscala(3, porcentaje) }
    }

    @Test
    fun `con una nota la escala ya no cambia`() = runBlocking {
        coEvery { asignaturas.getAsignatura(3) } returns calculo
        coEvery { calificaciones.contarDeAsignatura(3, "Cálculo") } returns 1

        assertTrue(bloqueada(calculo))
        assertEquals(ResultadoEscala.Bloqueada, guardar(3, porcentaje))
        coVerify(exactly = 0) { asignaturas.guardarEscala(any(), any()) }
    }

    @Test
    fun `una asignatura antigua sin escala la recibe aunque ya tenga notas`() = runBlocking {
        val antigua = calculo.copy(tipoCalificacion = "", escalaMin = null, escalaMax = null)
        coEvery { asignaturas.getAsignatura(3) } returns antigua
        coEvery { calificaciones.contarDeAsignatura(3, "Cálculo") } returns 4
        coEvery { asignaturas.guardarEscala(3, any()) } returns true

        assertFalse(bloqueada(antigua))
        assertEquals(ResultadoEscala.Guardada, guardar(3, unoACinco))
    }
}
