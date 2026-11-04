package com.softcg.myapplication.domain.gamificacion

import com.softcg.myapplication.core.Reloj
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.data.network.SesionUsuario
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

sealed class Celebracion {
    data class SubidaNivel(val nivel: NivelInfo, val monedas: Int, val desbloqueos: List<Accesorio>) : Celebracion()
    data class LogroDesbloqueado(val logro: Logro, val accesorio: Accesorio?) : Celebracion()
}

/**
 * Concede lo que el usuario ya se ganó: logros y recompensas de nivel. Se ejecuta cada vez que cambian
 * los movimientos, venga de donde venga la recompensa. Las claves únicas hacen que evaluar de más
 * no conceda nada dos veces; lo que escribe vuelve a dispararlo y la segunda pasada ya no encuentra nada.
 */
class EvaluarProgresoUseCase @Inject constructor(
    private val repository: GamificacionRepository,
    private val sesion: SesionUsuario,
    private val reloj: Reloj
) {
    suspend operator fun invoke(): List<Celebracion> {
        val uid = sesion.uid() ?: return emptyList()
        return try {
            repository.enTransaccion { evaluar(uid) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun evaluar(uid: String): List<Celebracion> {
        val ahora = reloj.ahora()
        val hoy = ahora.toLocalDate()
        val millis = ahora.toInstant().toEpochMilli()
        val dia = hoy.toString()

        val contexto = ContextoLogros(
            conteos = repository.conteoPorTipo(uid),
            mejorRacha = RachaReglas.calcular(diasActivos(uid), hoy).mejor,
            nivel = NivelesConfig.progreso(repository.xpTotal(uid)).nivel.numero
        )
        val concedidos = repository.clavesDeTipo(uid, TIPO_LOGRO).map { it.clave }.toSet()

        val celebraciones = mutableListOf<Celebracion>()
        LogrosCatalogo.logros
            .filter { it.clave !in concedidos && contexto.valor(it.metrica) >= it.meta }
            .forEach { logro ->
                val insertado = repository.insertarMovimiento(
                    uid, logro.clave, TIPO_LOGRO, logro.rareza.recompensa, "Logro: ${logro.titulo}", millis, dia
                )
                if (insertado) {
                    val accesorio = AccesoriosCatalogo.exclusivoDeLogro(logro.id)
                    if (accesorio != null && !repository.posee(uid, accesorio.id)) {
                        repository.agregarAlInventario(uid, accesorio.id, millis)
                    }
                    celebraciones += Celebracion.LogroDesbloqueado(logro, accesorio)
                }
            }

        // Después de los logros: su recompensa también es XP
        val nivelActual = NivelesConfig.progreso(repository.xpTotal(uid)).nivel
        val nuevos = NivelesConfig.niveles
            .filter { it.numero in 2..nivelActual.numero }
            .filter { nivel ->
                repository.insertarMovimiento(
                    uid, NivelesConfig.clave(nivel.numero), TIPO_NIVEL, nivel.recompensa,
                    "Subiste al nivel ${nivel.numero}: ${nivel.nombre}", millis, dia
                )
            }
        // Si se sube varios niveles de golpe solo se celebra el último
        nuevos.lastOrNull()?.let { nivel ->
            val desbloqueos = AccesoriosCatalogo.accesorios.filter { acc -> nuevos.any { it.numero == acc.nivelMinimo } }
            celebraciones.add(0, Celebracion.SubidaNivel(nivel, nuevos.sumOf { it.recompensa }, desbloqueos))
        }
        return celebraciones
    }

    private suspend fun diasActivos(uid: String): Set<LocalDate> =
        repository.diasConTipos(uid, TIPOS_ACTIVIDAD).mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
}

data class LogroEstado(val logro: Logro, val avance: Int, val desbloqueadoMillis: Long?) {
    val desbloqueado: Boolean get() = desbloqueadoMillis != null
    val fraccion: Float get() = (avance.toFloat() / logro.meta).coerceIn(0f, 1f)
}

data class PanelProgreso(
    val nivel: ProgresoNivel,
    val racha: Racha,
    val misiones: PanelMisiones,
    val logros: List<LogroEstado>,
    val estadisticas: EstadisticasProgreso,
    val mensaje: String
)

/** Una foto de el progreso, calculada siempre desde lo guardado. */
class ObtenerPanelProgresoUseCase @Inject constructor(
    private val obtenerMisiones: ObtenerMisionesUseCase,
    private val tareasRepository: TareasRepository,
    private val repository: GamificacionRepository,
    private val sesion: SesionUsuario,
    private val reloj: Reloj
) {
    suspend operator fun invoke(): PanelProgreso? {
        val uid = sesion.uid() ?: return null
        return try {
            val ahora = reloj.ahora()
            val hoy = ahora.toLocalDate()

            val xp = repository.xpTotal(uid)
            val nivel = NivelesConfig.progreso(xp)
            val dias = repository.diasConTipos(uid, TIPOS_ACTIVIDAD)
                .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
            val racha = RachaReglas.calcular(dias, hoy)
            val misiones = obtenerMisiones(uid)
            val conteos = repository.conteoPorTipo(uid)

            val contexto = ContextoLogros(conteos, racha.mejor, nivel.nivel.numero)
            val concedidos = repository.clavesDeTipo(uid, TIPO_LOGRO).associate { it.clave to it.fechaMillis }
            val logros = LogrosCatalogo.logros.map { logro ->
                LogroEstado(logro, contexto.valor(logro.metrica).coerceAtMost(logro.meta), concedidos[logro.clave])
            }

            val estadisticas = EstadisticasReglas.calcular(
                xpTotal = xp,
                conteos = conteos,
                xpPorDia = repository.xpPorDiaDesde(uid, hoy.minusDays(EstadisticasReglas.DIAS_ACTIVIDAD.toLong()).toString())
                    .mapNotNull { fila -> runCatching { LocalDate.parse(fila.dia) to fila.total }.getOrNull() }.toMap(),
                asistencias = repository.asistencias(uid).map { it.asistio },
                sesiones = repository.sesionesEnfoqueCompletadas(uid).map { sesion ->
                    Instant.ofEpochMilli(sesion.inicioMillis).atZone(ahora.zone).toLocalDate() to sesion.minutos
                },
                mejorRacha = racha.mejor,
                hoy = hoy
            )

            val pendientes = tareasRepository.getTareas().filter { !it.completada }
            val situacion = SituacionEstudiante(
                nivel = nivel,
                racha = racha,
                porReclamar = misiones.porReclamar,
                entregasHoy = pendientes.count { it.fecha == hoy.toString() },
                entregasManana = pendientes.count { it.fecha == hoy.plusDays(1).toString() },
                hora = ahora.hour,
                dia = hoy
            )
            PanelProgreso(nivel, racha, misiones, logros, estadisticas, MensajesMotivacion.para(situacion))
        } catch (e: Exception) {
            null
        }
    }
}
