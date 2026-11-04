package com.softcg.myapplication.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.softcg.myapplication.core.Reloj
import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.data.Repositories.CalificacionesRespository
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.data.database.TareasDatabase.TareasDatabase
import com.softcg.myapplication.data.database.entities.TareaEntity
import com.softcg.myapplication.data.network.SesionUsuario
import com.softcg.myapplication.domain.gamificacion.AvatarBase
import com.softcg.myapplication.domain.gamificacion.CalificarTareaUseCase
import com.softcg.myapplication.domain.gamificacion.CambiarAparienciaUseCase
import com.softcg.myapplication.domain.gamificacion.Celebracion
import com.softcg.myapplication.domain.gamificacion.CompletarEnfoqueUseCase
import com.softcg.myapplication.domain.gamificacion.ComprarAccesorioUseCase
import com.softcg.myapplication.domain.gamificacion.ConfirmarAsistenciaUseCase
import com.softcg.myapplication.domain.gamificacion.EntregarTareaUseCase
import com.softcg.myapplication.domain.gamificacion.EquiparAccesorioUseCase
import com.softcg.myapplication.domain.gamificacion.EvaluarProgresoUseCase
import com.softcg.myapplication.domain.gamificacion.IniciarEnfoqueUseCase
import com.softcg.myapplication.domain.gamificacion.MisionesConfig
import com.softcg.myapplication.domain.gamificacion.ObtenerEnfoqueActivoUseCase
import com.softcg.myapplication.domain.gamificacion.ObtenerMisionesUseCase
import com.softcg.myapplication.domain.gamificacion.OtorgarRecompensaUseCase
import com.softcg.myapplication.domain.gamificacion.ReclamarMisionUseCase
import com.softcg.myapplication.domain.gamificacion.ResultadoAsistencia
import com.softcg.myapplication.domain.gamificacion.ResultadoCalificarTarea
import com.softcg.myapplication.domain.gamificacion.ResultadoCompletarEnfoque
import com.softcg.myapplication.domain.gamificacion.ResultadoCompra
import com.softcg.myapplication.domain.gamificacion.ResultadoEntregarTarea
import com.softcg.myapplication.domain.gamificacion.ResultadoEquipar
import com.softcg.myapplication.domain.gamificacion.ResultadoIniciarEnfoque
import com.softcg.myapplication.domain.gamificacion.ResultadoMision
import com.softcg.myapplication.domain.gamificacion.ResultadoRecompensa
import com.softcg.myapplication.domain.gamificacion.TIPOS_ACTIVIDAD
import com.softcg.myapplication.domain.gamificacion.TIPO_LOGRO
import com.softcg.myapplication.domain.gamificacion.TipoRecompensa
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Reglas de EduCoins contra una base Room real: transacciones, claves únicas, usuarios y persistencia. */
@RunWith(AndroidJUnit4::class)
class GamificacionPersistenciaTest {

    private class RelojFijo(var ahora: LocalDateTime) : Reloj {
        override fun ahora(): ZonedDateTime = ahora.atZone(ZoneId.systemDefault())
    }

    private class Sesion(var uid: String?) : SesionUsuario {
        override fun uid() = uid
        override fun email(): String? = null
    }

    private val nombre = "gamificacion-test.db"
    private lateinit var context: Context
    private lateinit var db: TareasDatabase
    private lateinit var repository: GamificacionRepository

    private val fecha = LocalDate.of(2026, 10, 28)
    private val reloj = RelojFijo(fecha.atTime(17, 0))
    private val sesion = Sesion("usuario-1")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(nombre)
        abrir()
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(nombre)
    }

    private fun abrir() {
        db = Room.databaseBuilder(context, TareasDatabase::class.java, nombre).allowMainThreadQueries().build()
        repository = GamificacionRepository(db, db.getGamificacionDao())
    }

    /** Simula cerrar y volver a abrir la app. */
    private fun reabrir() {
        db.close()
        abrir()
    }

    private fun otorgar() = OtorgarRecompensaUseCase(repository, sesion, reloj)

    private suspend fun equipados(): Map<String, String> =
        db.getGamificacionDao().equipados("usuario-1").associate { it.categoria to it.itemId }

    private suspend fun darMonedas(cantidad: Int, uid: String = "usuario-1") {
        repository.insertarMovimiento(uid, "PRUEBA:$cantidad:${System.nanoTime()}", "PRUEBA", cantidad, "", 0, "2026-10-28")
    }

    private suspend fun registrarClase(): Int {
        val asignaturas = AsignaturasRepository(db.getAsignaturasDao())
        val registro = LocalDateTime.of(2026, 10, 1, 8, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return asignaturas.insertAsignatura(
            Asignatura(null, "Física", "Ana", 2, "14:00", List(7) { it == fecha.dayOfWeek.value - 1 }), registro
        ).toInt()
    }

    private fun confirmar() = ConfirmarAsistenciaUseCase(
        AsignaturasRepository(db.getAsignaturasDao()), repository, otorgar(), sesion, reloj
    )

    @Test
    fun laMismaRecompensaNoSeGuardaDosVecesAunqueSeInsertenDirectamente() = runBlocking {
        assertTrue(repository.insertarMovimiento("usuario-1", "TAREA_REGISTRADA:1", "TAREA_REGISTRADA", 2, "", 0, "2026-10-28"))
        assertFalse(repository.insertarMovimiento("usuario-1", "TAREA_REGISTRADA:1", "TAREA_REGISTRADA", 2, "", 0, "2026-10-28"))
        assertEquals(2, repository.saldo("usuario-1"))
    }

    @Test
    fun cadaUsuarioTieneSuSaldoInventarioYAvatar() = runBlocking {
        darMonedas(100, "usuario-1")
        assertEquals(ResultadoCompra.Comprado::class, ComprarAccesorioUseCase(repository, sesion, reloj)("gorra")::class)

        sesion.uid = "usuario-2"
        assertEquals(0, repository.saldo("usuario-2"))
        assertFalse(repository.posee("usuario-2", "gorra"))
        assertEquals(ResultadoEquipar.NoLoPosee, EquiparAccesorioUseCase(repository, sesion).equipar("gorra"))
        assertEquals(50, repository.saldo("usuario-1"))
    }

    @Test
    fun laCompraCobraUnaSolaVezYElSaldoNuncaQuedaNegativo() = runBlocking {
        darMonedas(50)
        val comprar = ComprarAccesorioUseCase(repository, sesion, reloj)

        assertEquals(0, (comprar("gorra") as ResultadoCompra.Comprado).saldoRestante)
        assertEquals(ResultadoCompra.YaLoTiene, comprar("gorra"))
        assertEquals(ResultadoCompra.SaldoInsuficiente(40, 0), comprar("gafas_redondas"))
        assertEquals(0, repository.saldo("usuario-1"))
        assertTrue(repository.posee("usuario-1", "gorra"))
    }

    @Test
    fun siFallaUnaTransaccionNoQuedaNadaGuardado() = runBlocking {
        darMonedas(100)
        try {
            repository.enTransaccion {
                repository.insertarMovimiento("usuario-1", "COMPRA:gorra", "COMPRA", -50, "", 0, "2026-10-28")
                throw IllegalStateException("fallo al guardar el inventario")
            }
            fail("Debía lanzar la excepción")
        } catch (e: IllegalStateException) {
            // esperado
        }
        assertEquals("El cobro se deshizo", 100, repository.saldo("usuario-1"))
        assertFalse(repository.posee("usuario-1", "gorra"))
    }

    @Test
    fun equiparReemplazaEnLaCategoriaYRetirarConservaElInventario() = runBlocking {
        darMonedas(300)
        val comprar = ComprarAccesorioUseCase(repository, sesion, reloj)
        val equipar = EquiparAccesorioUseCase(repository, sesion)
        comprar("gorra")
        comprar("audifonos")

        equipar.equipar("gorra")
        equipar.equipar("audifonos")
        assertEquals(mapOf("CABEZA" to "audifonos"), equipados())

        equipar.retirar(com.softcg.myapplication.domain.gamificacion.CategoriaAccesorio.CABEZA)
        assertTrue(equipados().isEmpty())
        assertTrue(repository.posee("usuario-1", "audifonos"))
        assertEquals(300 - 50 - 90, repository.saldo("usuario-1"))
    }

    // Progreso: el SQL nuevo solo se ejecuta de verdad aquí

    private fun evaluar() = EvaluarProgresoUseCase(repository, sesion, reloj)

    private fun obtenerMisiones() = ObtenerMisionesUseCase(
        AsignaturasRepository(db.getAsignaturasDao()), TareasRepository(db.getTareasDao()), repository, reloj
    )

    @Test
    fun comprarNoBajaElXpYSubirDeNivelNoLoAumenta() = runBlocking {
        darMonedas(60)
        assertEquals(60, repository.xpTotal("usuario-1"))

        val subida = evaluar()().filterIsInstance<Celebracion.SubidaNivel>().single()
        assertEquals(2, subida.nivel.numero)
        assertEquals("La recompensa de nivel no suma XP", 60, repository.xpTotal("usuario-1"))
        assertEquals(60 + subida.monedas, repository.saldo("usuario-1"))

        ComprarAccesorioUseCase(repository, sesion, reloj)("gorra")
        assertEquals("Comprar no resta XP", 60, repository.xpTotal("usuario-1"))
    }

    @Test
    fun evaluarDosVecesNoConcedeNadaDosVeces() = runBlocking {
        val otorgar = otorgar()
        otorgar(TipoRecompensa.TAREA_REGISTRADA, "1")

        val primera = evaluar()()
        assertTrue(primera.any { it is Celebracion.LogroDesbloqueado && it.logro.id == "primer_paso" })
        val saldo = repository.saldo("usuario-1")

        assertEquals(emptyList<Celebracion>(), evaluar()())
        assertEquals(saldo, repository.saldo("usuario-1"))
    }

    @Test
    fun laRachaYLasEstadisticasSalenDeLosMovimientosGuardados() = runBlocking {
        repository.insertarMovimiento("usuario-1", "TAREA_REGISTRADA:1", "TAREA_REGISTRADA", 2, "", 0, "2026-10-27")
        repository.insertarMovimiento("usuario-1", "TAREA_REGISTRADA:2", "TAREA_REGISTRADA", 2, "", 0, "2026-10-28")
        repository.insertarMovimiento("usuario-1", "LOGRO:x", TIPO_LOGRO, 10, "", 0, "2026-10-20")
        repository.insertarMovimiento("otro", "TAREA_REGISTRADA:3", "TAREA_REGISTRADA", 2, "", 0, "2026-10-26")

        assertEquals(setOf("2026-10-27", "2026-10-28"), repository.diasConTipos("usuario-1", TIPOS_ACTIVIDAD).toSet())
        assertEquals(mapOf("TAREA_REGISTRADA" to 2, TIPO_LOGRO to 1), repository.conteoPorTipo("usuario-1"))
        assertEquals(mapOf("TAREA_REGISTRADA" to 1), repository.conteoPorTipoDesde("usuario-1", "2026-10-28"))
        assertEquals(
            mapOf("2026-10-27" to 2, "2026-10-28" to 2),
            repository.xpPorDiaDesde("usuario-1", "2026-10-21").associate { it.dia to it.total }
        )
    }

    @Test
    fun lasMisionesAsignadasNoCambianAlReabrirYSeReclamanUnaVez() = runBlocking {
        val asignadas = obtenerMisiones()("usuario-1").diarias.map { it.mision.id }
        assertEquals(3, asignadas.size)

        reabrir()
        assertEquals(asignadas, obtenerMisiones()("usuario-1").diarias.map { it.mision.id })

        // Cumple todas las posibles sin tareas ni clases: anticiparse, agendar y enfocarse
        repository.insertarMovimiento("usuario-1", "TAREA_REGISTRADA:1", "TAREA_REGISTRADA", 2, "", 0, "2026-10-28")
        repository.insertarMovimiento("usuario-1", "EVENTO_REGISTRADO:1", "EVENTO_REGISTRADO", 2, "", 0, "2026-10-28")
        repository.insertarMovimiento("usuario-1", "ENFOQUE_MEDIO:1", "ENFOQUE_MEDIO", 10, "", 0, "2026-10-28")

        val reclamar = ReclamarMisionUseCase(obtenerMisiones(), repository, sesion, reloj)
        val resultados = asignadas.map { reclamar(it) }
        assertTrue(resultados.all { it is ResultadoMision.Reclamada })
        assertEquals(MisionesConfig.BONUS_DIARIO, (resultados.last() as ResultadoMision.Reclamada).bonus)
        assertEquals(ResultadoMision.YaReclamada, reclamar(asignadas.first()))
    }

    @Test
    fun laSesionDeEnfoqueSobreviveAlReabrirYSoloPagaAlTerminar() = runBlocking {
        val inicio = reloj.ahora
        val resultado = IniciarEnfoqueUseCase(ObtenerEnfoqueActivoUseCase(repository, sesion, reloj), repository, sesion, reloj)(25, "Física")
        assertTrue(resultado is ResultadoIniciarEnfoque.Iniciada)

        reabrir()
        val completar = CompletarEnfoqueUseCase(otorgar(), repository, sesion, reloj)
        assertEquals("Física", ObtenerEnfoqueActivoUseCase(repository, sesion, reloj)()?.asignatura)
        assertEquals(ResultadoCompletarEnfoque.AunNoTermina, completar())
        assertEquals(0, repository.saldo("usuario-1"))

        reloj.ahora = inicio.plusMinutes(25)
        assertEquals(ResultadoCompletarEnfoque.Completada(25, 10), completar())
        assertEquals(10, repository.saldo("usuario-1"))
        assertEquals(ResultadoCompletarEnfoque.SinSesionActiva, completar())
        assertEquals(25, repository.sesionesEnfoqueCompletadas("usuario-1").single().minutos)
    }

    @Test
    fun entregarUnaTareaYCalificarlaDespuesNoPagaDosVecesLaEntrega() = runBlocking {
        val tareasRepository = TareasRepository(db.getTareasDao())
        val tareaId = db.getTareasDao().insertTarea(TareaEntity(null, "Ensayo", "", "Historia", "2026-10-29", 1)).toInt()
        val entregar = EntregarTareaUseCase(tareasRepository, repository, otorgar(), sesion, reloj)
        val calificar = CalificarTareaUseCase(CalificacionesRespository(db.getCalificacionesDao()), repository, otorgar(), sesion)
        val tarea = Tarea(tareaId, "Ensayo", "", "Historia", "2026-10-29", 1)

        assertEquals(ResultadoEntregarTarea.Entregada(8, aTiempo = true), entregar(tarea))
        assertEquals(ResultadoEntregarTarea.YaEntregada, entregar(tarea))

        val entregada = tareasRepository.getTareas().single()
        assertTrue(entregada.completada)
        assertEquals(ResultadoCalificarTarea.Completada(3), calificar(entregada, "4.5"))
        assertEquals(8 + 3, repository.saldo("usuario-1"))
    }

    @Test
    fun laAparienciaElegidaSeGuardaPorUsuario() = runBlocking {
        val cambiar = CambiarAparienciaUseCase(repository, sesion)
        assertTrue(cambiar(AvatarBase(3, 1)))
        assertTrue(cambiar(AvatarBase(4, 2)))
        sesion.uid = "usuario-2"
        assertTrue(cambiar(AvatarBase(0, 0)))

        assertEquals(4, db.query("SELECT indice_piel FROM avatar_apariencia WHERE uid = 'usuario-1'", null).use { it.moveToFirst(); it.getInt(0) })
        assertEquals(2, db.query("SELECT COUNT(*) FROM avatar_apariencia", null).use { it.moveToFirst(); it.getInt(0) })
    }

    @Test
    fun saldoInventarioAvatarYAsistenciaSobrevivenAlReabrir() = runBlocking {
        darMonedas(100)
        ComprarAccesorioUseCase(repository, sesion, reloj)("gafas_sol")
        EquiparAccesorioUseCase(repository, sesion).equipar("gafas_sol")
        val claseId = registrarClase()
        confirmar()(claseId, fecha, true)

        reabrir()

        assertEquals(100 - 60 + 10, repository.saldo("usuario-1"))
        assertTrue(repository.posee("usuario-1", "gafas_sol"))
        assertEquals(mapOf("GAFAS" to "gafas_sol"), equipados())
        // La asistencia ya respondida sigue respondida: no se puede cobrar de nuevo
        assertEquals(ResultadoAsistencia.YaRespondida, confirmar()(claseId, fecha, true))
    }

    @Test
    fun confirmarAsistenciaDosVecesSoloPagaUnaVez() = runBlocking {
        val claseId = registrarClase()

        assertEquals(ResultadoAsistencia.Registrada(true, 10), confirmar()(claseId, fecha, true))
        assertEquals(ResultadoAsistencia.YaRespondida, confirmar()(claseId, fecha, true))
        assertEquals(10, repository.saldo("usuario-1"))
    }

    @Test
    fun noAsistiYVencidaNoPagan() = runBlocking {
        val claseId = registrarClase()
        assertEquals(ResultadoAsistencia.Registrada(false, 0), confirmar()(claseId, fecha, false))

        reloj.ahora = fecha.plusDays(8).atTime(0, 0) // la clase del miércoles siguiente ya venció
        assertEquals(ResultadoAsistencia.PlazoVencido, confirmar()(claseId, fecha.plusDays(7), true))
        assertEquals(0, repository.saldo("usuario-1"))
    }

    @Test
    fun calificarUnaTareaLaCompletaYPagaUnaSolaVez() = runBlocking {
        val tareaId = db.getTareasDao().insertTarea(TareaEntity(null, "Ensayo", "", "Historia", "2026-10-28", 1)).toInt()
        val calificar = CalificarTareaUseCase(CalificacionesRespository(db.getCalificacionesDao()), repository, otorgar(), sesion)
        val tarea = Tarea(tareaId, "Ensayo", "", "Historia", "2026-10-28", 1)

        // Vence el mismo día: completar más el bono a tiempo
        assertEquals(ResultadoCalificarTarea.Completada(8, aTiempo = true), calificar(tarea, "4.5"))
        assertEquals(ResultadoCalificarTarea.YaCalificada, calificar(tarea, "5"))

        val guardada = db.getTareasDao().getAllTareasConCalificacion().single()
        assertEquals(4.5F, guardada.calificacion)
        assertEquals(8, repository.saldo("usuario-1"))
    }

    @Test
    fun elTopeDiarioSeAplicaConLaBaseReal() = runBlocking {
        val otorgar = otorgar()
        repeat(TipoRecompensa.CLASE_REGISTRADA.topeDiario) {
            assertTrue(otorgar(TipoRecompensa.CLASE_REGISTRADA, "$it") is ResultadoRecompensa.Otorgada)
        }
        assertEquals(ResultadoRecompensa.TopeDiarioAlcanzado, otorgar(TipoRecompensa.CLASE_REGISTRADA, "99"))
    }
}
