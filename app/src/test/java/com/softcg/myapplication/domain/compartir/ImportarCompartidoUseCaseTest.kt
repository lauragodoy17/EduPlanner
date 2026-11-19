package com.softcg.myapplication.domain.compartir

import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.data.Repositories.CalificacionesRespository
import com.softcg.myapplication.data.Repositories.EventosRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.data.network.SesionUsuario
import com.softcg.myapplication.domain.CrearAsignaturaUseCase
import com.softcg.myapplication.domain.NuevaAsignatura
import com.softcg.myapplication.domain.ResultadoCrearAsignatura
import com.softcg.myapplication.domain.compartir.ContenidoCompartido.AsignaturaCompartida
import com.softcg.myapplication.domain.compartir.ContenidoCompartido.EventoCompartido
import com.softcg.myapplication.domain.compartir.ContenidoCompartido.NotaCompartida
import com.softcg.myapplication.domain.compartir.ContenidoCompartido.TareaCompartida
import com.softcg.myapplication.domain.gamificacion.OtorgarRecompensaUseCase
import com.softcg.myapplication.domain.gamificacion.ResultadoRecompensa
import com.softcg.myapplication.domain.gamificacion.TipoRecompensa
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models.Calificacion
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ImportarCompartidoUseCaseTest {

    private val asignaturas: AsignaturasRepository = mockk(relaxed = true)
    private val tareas: TareasRepository = mockk(relaxed = true)
    private val eventos: EventosRepository = mockk(relaxed = true)
    private val calificaciones: CalificacionesRespository = mockk(relaxed = true)
    private val crearAsignatura: CrearAsignaturaUseCase = mockk()
    private val otorgarRecompensa: OtorgarRecompensaUseCase = mockk()
    private val sesion: SesionUsuario = mockk()
    private val caso = ImportarCompartidoUseCase(asignaturas, tareas, eventos, calificaciones, crearAsignatura, otorgarRecompensa, sesion)

    private val tarea = TareaCompartida("Taller", "", "Calculo", "2026-11-20", 1)

    private fun asignatura(id: Int, nombre: String) = Asignatura(id, nombre, "", 0, "", List(7) { false })

    @Before
    fun setUp() {
        every { sesion.uid() } returns "u1"
        every { asignaturas.getAllAsignaturas() } returns listOf(asignatura(1, "Cálculo"), asignatura(2, "Física"))
        coEvery { tareas.insertTarea(any()) } returns 10L
        coEvery { eventos.insertEvento(any()) } returns 11L
        coEvery { calificaciones.insertCalificacion(any()) } returns 12L
        coEvery { otorgarRecompensa(any(), any()) } returns ResultadoRecompensa.TopeDiarioAlcanzado
    }

    @Test
    fun `la asignatura se reconoce aunque se escriba sin tildes o con otras mayusculas`() {
        val analisis = caso.analizar(tarea.copy(asignatura = "  CALCULO "))
        assertEquals("Cálculo", analisis.asignaturaLocal)
        assertFalse(analisis.faltaAsignatura)
        assertEquals(listOf("Cálculo", "Física"), analisis.asignaturasLocales)
    }

    @Test
    fun `si la asignatura no existe aqui hay que elegir que hacer`() {
        val analisis = caso.analizar(tarea.copy(asignatura = "Química"))
        assertNull(analisis.asignaturaLocal)
        assertTrue(analisis.faltaAsignatura)
        // Un evento no tiene asignatura: nunca pregunta
        assertFalse(caso.analizar(EventoCompartido("Feria", "", "2026-12-01", "", "", 0)).faltaAsignatura)
    }

    @Test
    fun `una tarea de una asignatura que ya existe se guarda con el nombre local`() = runBlocking {
        val guardada = slot<Tarea>()
        coEvery { tareas.insertTarea(capture(guardada)) } returns 10L

        val r = caso.importar(tarea)

        assertEquals(ResultadoImportacion.Importado(TipoCompartido.TAREA, "Taller", null), r)
        assertEquals("Cálculo", guardada.captured.asignatura)
        assertNull("Llega sin estado: ni entregada ni calificada", guardada.captured.entregadaMillis)
        coVerify(exactly = 0) { crearAsignatura(any()) }
    }

    @Test
    fun `importar una tarea da los EduCoins de registrarla, con el mismo tope diario`() = runBlocking {
        coEvery { otorgarRecompensa(TipoRecompensa.TAREA_REGISTRADA, "10") } returns ResultadoRecompensa.Otorgada(2)
        assertEquals(ResultadoImportacion.Importado(TipoCompartido.TAREA, "Taller", null, 2), caso.importar(tarea))

        // Con el tope del día alcanzado se agrega igual, sin monedas
        coEvery { otorgarRecompensa(TipoRecompensa.TAREA_REGISTRADA, "10") } returns ResultadoRecompensa.TopeDiarioAlcanzado
        assertEquals(ResultadoImportacion.Importado(TipoCompartido.TAREA, "Taller", null, null), caso.importar(tarea))
    }

    @Test
    fun `sin asignatura local ni destino elegido no se guarda`() = runBlocking {
        assertEquals(ResultadoImportacion.FaltaAsignatura, caso.importar(tarea.copy(asignatura = "Química")))
        coVerify(exactly = 0) { tareas.insertTarea(any()) }
    }

    @Test
    fun `se puede crear la asignatura que falta`() = runBlocking {
        coEvery { crearAsignatura(NuevaAsignatura("Química")) } returns ResultadoCrearAsignatura.Creada(3, "Química", null, "Quimica")
        val guardada = slot<Tarea>()
        coEvery { tareas.insertTarea(capture(guardada)) } returns 10L

        val r = caso.importar(tarea.copy(asignatura = "Quimica"), DestinoAsignatura.Nueva("Química"))

        assertEquals(ResultadoImportacion.Importado(TipoCompartido.TAREA, "Taller", "Química"), r)
        assertEquals("Química", guardada.captured.asignatura)
        coVerify { crearAsignatura(NuevaAsignatura("Química")) }
    }

    @Test
    fun `crear una que en realidad ya existe reutiliza la existente`() = runBlocking {
        val r = caso.importar(tarea.copy(asignatura = "Química"), DestinoAsignatura.Nueva("fisica"))
        assertEquals(ResultadoImportacion.Importado(TipoCompartido.TAREA, "Taller", null), r)
        coVerify(exactly = 0) { crearAsignatura(any()) }
    }

    @Test
    fun `una nota se puede reasignar a una asignatura que ya tengo`() = runBlocking {
        val guardada = slot<Calificacion>()
        coEvery { calificaciones.insertCalificacion(capture(guardada)) } returns 12L

        val r = caso.importar(NotaCompartida("Parcial", 4.5f, "Química", ""), DestinoAsignatura.Existente("Física"))

        assertEquals(ResultadoImportacion.Importado(TipoCompartido.NOTA, "Parcial", null), r)
        assertEquals("Física", guardada.captured.asignatura)
        assertEquals(4.5f, guardada.captured.valor)
        // Una asignatura elegida que no existe no vale
        assertEquals(ResultadoImportacion.FaltaAsignatura, caso.importar(NotaCompartida("Parcial", 4f, "X", ""), DestinoAsignatura.Existente("Arte")))
    }

    @Test
    fun `un evento se guarda sin preguntar nada`() = runBlocking {
        val r = caso.importar(EventoCompartido("Feria", "Stand 4", "2026-12-01", "08:00", "12:00", 2))
        assertEquals(ResultadoImportacion.Importado(TipoCompartido.EVENTO, "Feria", null), r)
        coVerify { eventos.insertEvento(match { it.titulo == "Feria" && it.horaFin == "12:00" && it.imagenUri == null }) }
        coVerify { otorgarRecompensa(TipoRecompensa.EVENTO_REGISTRADO, "11") }
    }

    @Test
    fun `una asignatura compartida conserva su area y escala y da los EduCoins de registrar la clase`() = runBlocking {
        val compartida = AsignaturaCompartida(
            "Historia del arte", "Luis", 2, "10:00", listOf(false, true, false, false, true, false, false), "", "Arte", 1f, 5f
        )
        coEvery { crearAsignatura(any()) } returns ResultadoCrearAsignatura.Creada(4, "Historia del arte", 5, null)

        val r = caso.importar(compartida)

        assertEquals(ResultadoImportacion.Importado(TipoCompartido.ASIGNATURA, "Historia del arte", null, 5), r)
        coVerify { crearAsignatura(NuevaAsignatura("Historia del arte", "Luis", 2, "10:00", listOf("Martes", "Viernes"), "")) }
        coVerify { asignaturas.guardarDatosModelo(4, "Arte", 1f, 5f) }
    }

    @Test
    fun `una asignatura que ya tengo no se duplica`() = runBlocking {
        val repetida = AsignaturaCompartida("calculo", "", 0, "", List(7) { false }, "", null, null, null)
        assertTrue(caso.analizar(repetida).asignaturaRepetida)
        assertEquals(ResultadoImportacion.AsignaturaRepetida, caso.importar(repetida))
        coVerify(exactly = 0) { crearAsignatura(any()) }
    }

    @Test
    fun `sin sesion no se importa nada`() = runBlocking {
        every { sesion.uid() } returns null
        assertEquals(ResultadoImportacion.SinSesion, caso.importar(tarea))
    }
}
