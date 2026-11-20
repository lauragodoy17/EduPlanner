package com.softcg.myapplication.domain.encuesta

import com.softcg.myapplication.core.RelojFijo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class EnviarEncuestaUseCaseTest {

    private val enviador = mockk<EnviadorEncuesta>()
    private val almacen = mockk<AlmacenEncuesta>(relaxed = true) {
        every { idRespuesta() } returns "resp-1"
        every { participante() } returns "anon-1"
    }
    private val useCase = EnviarEncuestaUseCase(enviador, almacen, RelojFijo(LocalDateTime.of(2026, 11, 19, 15, 30)), VersionApp("1.0 (1)"))

    private val completa: RespuestasEncuesta = EncuestaCatalogo.preguntas.mapNotNull { p ->
        when (p) {
            is Pregunta.Likert -> p.id to "3"
            is Pregunta.Puntaje -> p.id to "10"
            is Pregunta.Opcion -> p.id to p.opciones.last()
            is Pregunta.Texto -> if (p.obligatoria) p.id to "Grado 11" else null
        }
    }.toMap()

    @Test
    fun `incompleta no envia y dice que seccion falta`() = runTest {
        assertEquals(ResultadoEnvioEncuesta.Incompleta(1), useCase(completa - "S2_4"))
        coVerify(exactly = 0) { enviador.enviar(any(), any()) }
    }

    @Test
    fun `enviada con su id y fecha, y se marca respondida`() = runTest {
        val columnas = slot<List<ColumnaEncuesta>>()
        coEvery { enviador.enviar("resp-1", capture(columnas)) } returns ResultadoEnvioEncuesta.Enviada
        assertEquals(ResultadoEnvioEncuesta.Enviada, useCase(completa))
        assertEquals("2026-11-19 15:30:00", columnas.captured.first { it.encabezado == ReglasEncuesta.COL_FECHA }.valor)
        assertEquals("anon-1", columnas.captured.first { it.encabezado == ReglasEncuesta.COL_PARTICIPANTE }.valor)
        verify { almacen.marcarEnviada() }
    }

    @Test
    fun `si falla no se marca respondida`() = runTest {
        coEvery { enviador.enviar(any(), any()) } returns ResultadoEnvioEncuesta.SinConexion
        assertEquals(ResultadoEnvioEncuesta.SinConexion, useCase(completa))
        verify(exactly = 0) { almacen.marcarEnviada() }
    }
}
