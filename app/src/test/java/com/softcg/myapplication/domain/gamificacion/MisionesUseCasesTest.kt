package com.softcg.myapplication.domain.gamificacion

import com.softcg.myapplication.core.RelojFijo
import com.softcg.myapplication.core.ejecutarTransaccionesDirectamente
import com.softcg.myapplication.core.millis
import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.data.network.SesionUsuario
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MisionesUseCasesTest {

    @RelaxedMockK private lateinit var asignaturasRepository: AsignaturasRepository
    @RelaxedMockK private lateinit var tareasRepository: TareasRepository
    @RelaxedMockK private lateinit var repository: GamificacionRepository
    @RelaxedMockK private lateinit var sesion: SesionUsuario

    // Martes 3 de noviembre de 2026
    private val hoy = LocalDate.of(2026, 11, 3)
    private val claveDia = "D:2026-11-03"
    private val claveSemana = "S:2026-11-02"
    private val reloj = RelojFijo(hoy.atTime(10, 0))

    private lateinit var obtener: ObtenerMisionesUseCase
    private lateinit var reclamar: ReclamarMisionUseCase

    @Before
    fun onBefore() {
        MockKAnnotations.init(this)
        every { sesion.uid() } returns "usuario-1"
        every { asignaturasRepository.getAllAsignaturas() } returns emptyList()
        every { tareasRepository.getTareas() } returns emptyList()
        coEvery { repository.misionesAsignadas(any(), any()) } returns emptyList()
        coEvery { repository.conteoPorTipoDesde(any(), any()) } returns emptyMap()
        coEvery { repository.diasConTipos(any(), any()) } returns emptyList()
        coEvery { repository.existeMovimiento(any(), any()) } returns false
        coEvery { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) } returns true
        repository.ejecutarTransaccionesDirectamente()
        obtener = ObtenerMisionesUseCase(asignaturasRepository, tareasRepository, repository, reloj)
        reclamar = ReclamarMisionUseCase(obtener, repository, sesion, reloj)
    }

    private fun asignar(diarias: List<String>, semanales: List<String> = emptyList()) {
        coEvery { repository.misionesAsignadas("usuario-1", claveDia) } returns diarias
        coEvery { repository.misionesAsignadas("usuario-1", claveSemana) } returns semanales
    }

    // Selección

    @Test
    fun `sin clases ni tareas no se pide nada imposible`() {
        val elegidas = MisionesConfig.seleccionar(PeriodoMision.DIARIA, ContextoMisiones(), 123)
        assertEquals(3, elegidas.size)
        assertFalse(elegidas.any { it.metrica == MetricaMision.ASISTENCIAS })
        assertFalse(elegidas.any { it.metrica == MetricaMision.TAREAS_TERMINADAS })
        assertFalse(elegidas.any { it.metrica == MetricaMision.TAREAS_A_TIEMPO })
    }

    @Test
    fun `siempre hay misiones suficientes en cualquier contexto y semana`() {
        repeat(50) { semilla ->
            assertEquals(3, MisionesConfig.seleccionar(PeriodoMision.DIARIA, ContextoMisiones(), semilla).size)
            assertEquals(3, MisionesConfig.seleccionar(PeriodoMision.SEMANAL, ContextoMisiones(), semilla).size)
        }
    }

    @Test
    fun `la seleccion depende de la semilla y es estable`() {
        val contexto = ContextoMisiones(clasesHoy = 2, clasesRestantesSemana = 5, tareasPendientes = 4, tareasPorVencer = 3)
        assertEquals(
            MisionesConfig.seleccionar(PeriodoMision.DIARIA, contexto, 7),
            MisionesConfig.seleccionar(PeriodoMision.DIARIA, contexto, 7)
        )
    }

    @Test
    fun `los ids de las misiones son unicos y las recompensas positivas`() {
        assertEquals(MisionesConfig.misiones.size, MisionesConfig.misiones.map { it.id }.toSet().size)
        MisionesConfig.misiones.forEach { assertTrue(it.recompensa > 0 && it.meta > 0) }
    }

    @Test
    fun `las claves de periodo usan el dia y el lunes de la semana`() {
        assertEquals(claveDia, PeriodoMision.DIARIA.clave(hoy))
        assertEquals(claveSemana, PeriodoMision.SEMANAL.clave(hoy))
        // El domingo sigue siendo la misma semana
        assertEquals(claveSemana, PeriodoMision.SEMANAL.clave(hoy.plusDays(5)))
    }

    // Asignación

    @Test
    fun `la primera consulta del periodo asigna y guarda las misiones`() = runBlocking {
        val panel = obtener("usuario-1")

        assertEquals(3, panel.diarias.size)
        assertEquals(3, panel.semanales.size)
        coVerify { repository.asignarMisiones("usuario-1", claveDia, any()) }
        coVerify { repository.asignarMisiones("usuario-1", claveSemana, any()) }
    }

    @Test
    fun `las misiones ya asignadas no cambian aunque cambie el contexto`() = runBlocking {
        asignar(listOf("d_clase", "d_enfoque"), listOf("s_enfoque"))

        val panel = obtener("usuario-1")

        assertEquals(listOf("d_enfoque", "d_clase"), panel.diarias.map { it.mision.id })
        coVerify(exactly = 0) { repository.asignarMisiones(any(), any(), any()) }
    }

    @Test
    fun `con clase hoy puede salir la mision de asistencia`() = runBlocking {
        // Martes: índice 1 del horario
        every { asignaturasRepository.getAllAsignaturas() } returns listOf(
            Asignatura(1, "Física", "Ana", 2, "14:00", List(7) { it == 1 }, fechaRegistro = hoy.minusDays(30).atStartOfDay().millis())
        )
        every { tareasRepository.getTareas() } returns listOf(Tarea(1, "Ensayo", "", "Historia", "2026-11-10", 1))

        val salen = (0..40).flatMap { semilla ->
            MisionesConfig.seleccionar(PeriodoMision.DIARIA, ContextoMisiones(clasesHoy = 1, tareasPendientes = 1, tareasPorVencer = 1), semilla)
        }.map { it.id }

        assertTrue(salen.contains("d_clase"))
    }

    // Avance

    @Test
    fun `el avance sale de los movimientos del periodo y no pasa de la meta`() = runBlocking {
        asignar(listOf("d_enfoque"), listOf("s_entregas"))
        coEvery { repository.conteoPorTipoDesde("usuario-1", "2026-11-03") } returns mapOf(
            TipoRecompensa.ENFOQUE_CORTO.name to 1, TipoRecompensa.ENFOQUE_LARGO.name to 1
        )
        coEvery { repository.conteoPorTipoDesde("usuario-1", "2026-11-02") } returns mapOf(TipoRecompensa.TAREA_COMPLETADA.name to 2)

        val panel = obtener("usuario-1")

        assertEquals(1, panel.diarias.single().avance)
        assertTrue(panel.diarias.single().reclamable)
        assertEquals(2, panel.semanales.single().avance)
        assertFalse(panel.semanales.single().completada)
    }

    @Test
    fun `la constancia cuenta dias distintos de la semana, no los anteriores`() = runBlocking {
        asignar(emptyList(), listOf("s_constancia"))
        coEvery { repository.diasConTipos("usuario-1", TIPOS_ACTIVIDAD) } returns listOf("2026-10-30", "2026-11-02", "2026-11-03")

        assertEquals(2, obtener("usuario-1").semanales.single().avance)
    }

    // Reclamar

    @Test
    fun `una mision completa se paga una vez con la cantidad del catalogo`() = runBlocking {
        asignar(listOf("d_enfoque"))
        coEvery { repository.conteoPorTipoDesde("usuario-1", any()) } returns mapOf(TipoRecompensa.ENFOQUE_MEDIO.name to 1)

        val resultado = reclamar("d_enfoque")

        // Es la única asignada: al reclamarla se completa el día
        assertEquals(ResultadoMision.Reclamada(10, MisionesConfig.BONUS_DIARIO), resultado)
        coVerify { repository.insertarMovimiento("usuario-1", "MISION:d_enfoque:$claveDia", TIPO_MISION_DIARIA, 10, any(), any(), "2026-11-03") }
    }

    @Test
    fun `una mision incompleta no se paga aunque la pantalla lo pida`() = runBlocking {
        asignar(listOf("d_enfoque"))

        assertEquals(ResultadoMision.Incompleta, reclamar("d_enfoque"))
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `una mision reclamada no se paga otra vez`() = runBlocking {
        asignar(listOf("d_enfoque"))
        coEvery { repository.conteoPorTipoDesde("usuario-1", any()) } returns mapOf(TipoRecompensa.ENFOQUE_MEDIO.name to 1)
        coEvery { repository.existeMovimiento("usuario-1", "MISION:d_enfoque:$claveDia") } returns true

        assertEquals(ResultadoMision.YaReclamada, reclamar("d_enfoque"))
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `una mision que no esta asignada hoy no se puede reclamar`() = runBlocking {
        asignar(listOf("d_enfoque"))

        assertEquals(ResultadoMision.NoDisponible, reclamar("d_agenda"))
    }

    @Test
    fun `el bono solo se paga al reclamar la ultima mision del dia`() = runBlocking {
        asignar(listOf("d_enfoque", "d_agenda"))
        coEvery { repository.conteoPorTipoDesde("usuario-1", any()) } returns mapOf(
            TipoRecompensa.ENFOQUE_MEDIO.name to 1, TipoRecompensa.EVENTO_REGISTRADO.name to 1
        )

        assertEquals(ResultadoMision.Reclamada(10, 0), reclamar("d_enfoque"))
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), match { it.startsWith(TIPO_MISION_BONUS) }, any(), any(), any(), any(), any()) }

        coEvery { repository.existeMovimiento("usuario-1", "MISION:d_enfoque:$claveDia") } returns true

        assertEquals(ResultadoMision.Reclamada(5, MisionesConfig.BONUS_DIARIO), reclamar("d_agenda"))
        coVerify { repository.insertarMovimiento("usuario-1", "$TIPO_MISION_BONUS:2026-11-03", TIPO_MISION_BONUS, MisionesConfig.BONUS_DIARIO, any(), any(), any()) }
    }

    @Test
    fun `las misiones semanales no activan el bono diario`() = runBlocking {
        asignar(emptyList(), listOf("s_enfoque"))
        coEvery { repository.conteoPorTipoDesde("usuario-1", any()) } returns mapOf(TipoRecompensa.ENFOQUE_MEDIO.name to 3)

        assertEquals(ResultadoMision.Reclamada(30, 0), reclamar("s_enfoque"))
    }

    @Test
    fun `sin sesion o con un error no se paga nada`() = runBlocking {
        every { sesion.uid() } returns null
        assertEquals(ResultadoMision.SinSesion, reclamar("d_enfoque"))

        every { sesion.uid() } returns "usuario-1"
        asignar(listOf("d_enfoque"))
        coEvery { repository.conteoPorTipoDesde(any(), any()) } throws IllegalStateException()
        assertEquals(ResultadoMision.Error, reclamar("d_enfoque"))
    }
}
