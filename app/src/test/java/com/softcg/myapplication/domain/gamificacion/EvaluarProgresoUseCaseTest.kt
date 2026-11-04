package com.softcg.myapplication.domain.gamificacion

import com.softcg.myapplication.core.RelojFijo
import com.softcg.myapplication.core.ejecutarTransaccionesDirectamente
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.database.entities.ClaveConFecha
import com.softcg.myapplication.data.network.SesionUsuario
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class EvaluarProgresoUseCaseTest {

    @RelaxedMockK private lateinit var repository: GamificacionRepository
    @RelaxedMockK private lateinit var sesion: SesionUsuario

    private val reloj = RelojFijo(LocalDateTime.of(2026, 11, 3, 10, 0))
    private lateinit var evaluar: EvaluarProgresoUseCase

    @Before
    fun onBefore() {
        MockKAnnotations.init(this)
        every { sesion.uid() } returns "usuario-1"
        coEvery { repository.conteoPorTipo(any()) } returns emptyMap()
        coEvery { repository.diasConTipos(any(), any()) } returns emptyList()
        coEvery { repository.clavesDeTipo(any(), any()) } returns emptyList()
        coEvery { repository.xpTotal(any()) } returns 0
        coEvery { repository.posee(any(), any()) } returns false
        coEvery { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) } returns true
        repository.ejecutarTransaccionesDirectamente()
        evaluar = EvaluarProgresoUseCase(repository, sesion, reloj)
    }

    @Test
    fun `sin actividad no se concede nada`() = runBlocking {
        assertEquals(emptyList<Celebracion>(), evaluar())
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `al llegar a la meta se concede el logro con la recompensa de su rareza`() = runBlocking {
        coEvery { repository.conteoPorTipo("usuario-1") } returns mapOf(TipoRecompensa.TAREA_REGISTRADA.name to 1)

        val nuevas = evaluar()

        assertEquals("primer_paso", (nuevas.single() as Celebracion.LogroDesbloqueado).logro.id)
        coVerify { repository.insertarMovimiento("usuario-1", "LOGRO:primer_paso", TIPO_LOGRO, RarezaLogro.BRONCE.recompensa, any(), any(), "2026-11-03") }
    }

    @Test
    fun `un logro ya concedido no se vuelve a pagar ni a celebrar`() = runBlocking {
        coEvery { repository.conteoPorTipo("usuario-1") } returns mapOf(TipoRecompensa.TAREA_REGISTRADA.name to 1)
        coEvery { repository.clavesDeTipo("usuario-1", TIPO_LOGRO) } returns listOf(ClaveConFecha("LOGRO:primer_paso", 1L))

        assertEquals(emptyList<Celebracion>(), evaluar())
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), "LOGRO:primer_paso", any(), any(), any(), any(), any()) }
    }

    @Test
    fun `si la base rechaza la clave no se celebra`() = runBlocking {
        coEvery { repository.conteoPorTipo("usuario-1") } returns mapOf(TipoRecompensa.TAREA_REGISTRADA.name to 1)
        coEvery { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) } returns false

        assertEquals(emptyList<Celebracion>(), evaluar())
    }

    @Test
    fun `un logro exclusivo entrega su accesorio sin cobrarlo`() = runBlocking {
        coEvery { repository.conteoPorTipo("usuario-1") } returns mapOf(TipoRecompensa.TAREA_A_TIEMPO.name to 30)

        val nuevas = evaluar().filterIsInstance<Celebracion.LogroDesbloqueado>()

        val conPremio = nuevas.single { it.logro.id == "sin_procrastinar" }
        assertEquals("gafas_doradas", conPremio.accesorio?.id)
        coVerify { repository.agregarAlInventario("usuario-1", "gafas_doradas", any()) }
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), match { it.startsWith(TIPO_COMPRA) }, any(), any(), any(), any(), any()) }
    }

    @Test
    fun `la racha cuenta para los logros aunque ya se haya perdido`() = runBlocking {
        val inicio = LocalDate.of(2026, 9, 1)
        coEvery { repository.diasConTipos("usuario-1", TIPOS_ACTIVIDAD) } returns (0L until 7L).map { inicio.plusDays(it).toString() }

        val ids = evaluar().filterIsInstance<Celebracion.LogroDesbloqueado>().map { it.logro.id }

        assertTrue(ids.containsAll(listOf("racha_3", "racha_7")))
    }

    @Test
    fun `subir de nivel paga su recompensa y se celebra`() = runBlocking {
        coEvery { repository.xpTotal("usuario-1") } returns 60

        val subida = evaluar().single() as Celebracion.SubidaNivel

        assertEquals(2, subida.nivel.numero)
        assertEquals(20, subida.monedas)
        coVerify { repository.insertarMovimiento("usuario-1", "NIVEL:2", TIPO_NIVEL, 20, any(), any(), any()) }
    }

    @Test
    fun `un nivel ya pagado no se paga ni se celebra otra vez`() = runBlocking {
        coEvery { repository.xpTotal("usuario-1") } returns 60
        coEvery { repository.insertarMovimiento(any(), "NIVEL:2", any(), any(), any(), any(), any()) } returns false

        assertEquals(emptyList<Celebracion>(), evaluar())
    }

    @Test
    fun `subir varios niveles paga todos pero celebra solo el ultimo e informa sus desbloqueos`() = runBlocking {
        coEvery { repository.xpTotal("usuario-1") } returns 150

        val subida = evaluar().single() as Celebracion.SubidaNivel

        assertEquals(3, subida.nivel.numero)
        assertEquals(50, subida.monedas)
        assertTrue(subida.desbloqueos.any { it.id == "audifonos" })
    }

    @Test
    fun `la subida de nivel se muestra antes que los logros`() = runBlocking {
        coEvery { repository.xpTotal("usuario-1") } returns 60
        coEvery { repository.conteoPorTipo("usuario-1") } returns mapOf(TipoRecompensa.TAREA_REGISTRADA.name to 1)

        assertTrue(evaluar().first() is Celebracion.SubidaNivel)
    }

    @Test
    fun `sin sesion no se evalua y un error no se informa como logro`() = runBlocking {
        every { sesion.uid() } returns null
        assertEquals(emptyList<Celebracion>(), evaluar())

        every { sesion.uid() } returns "usuario-1"
        coEvery { repository.conteoPorTipo(any()) } throws IllegalStateException()
        assertEquals(emptyList<Celebracion>(), evaluar())
    }

    @Test
    fun `los logros tienen id unico y metas alcanzables`() {
        assertEquals(LogrosCatalogo.logros.size, LogrosCatalogo.logros.map { it.id }.toSet().size)
        LogrosCatalogo.logros.forEach { assertTrue(it.meta > 0) }
        assertNull(LogrosCatalogo.buscar("no_existe"))
    }

    // Estadísticas

    @Test
    fun `sin datos los porcentajes no se muestran como cero`() {
        val stats = EstadisticasReglas.calcular(0, emptyMap(), emptyMap(), emptyList(), emptyList(), 0, LocalDate.of(2026, 11, 3))
        assertNull(stats.porcentajeATiempo)
        assertNull(stats.porcentajeAsistencia)
        assertEquals(7, stats.ultimos7Dias.size)
    }

    @Test
    fun `las estadisticas salen de los datos reales`() {
        val hoy = LocalDate.of(2026, 11, 3) // Martes
        val stats = EstadisticasReglas.calcular(
            xpTotal = 200,
            conteos = mapOf(TipoRecompensa.TAREA_COMPLETADA.name to 4, TipoRecompensa.TAREA_A_TIEMPO.name to 3),
            xpPorDia = mapOf(hoy to 10, hoy.minusDays(1) to 20, hoy.minusDays(2) to 30),
            asistencias = listOf(true, true, false, true),
            sesiones = listOf(hoy to 25, hoy.minusDays(10) to 50),
            mejorRacha = 6,
            hoy = hoy
        )
        assertEquals(75, stats.porcentajeATiempo)
        assertEquals(75, stats.porcentajeAsistencia)
        assertEquals(75, stats.minutosEnfoqueTotal)
        assertEquals(25, stats.minutosEnfoqueSemana)
        assertEquals(30, stats.xpSemana)
        assertEquals(30, stats.xpSemanaAnterior)
        assertEquals(hoy to 10, stats.ultimos7Dias.last())
    }
}
