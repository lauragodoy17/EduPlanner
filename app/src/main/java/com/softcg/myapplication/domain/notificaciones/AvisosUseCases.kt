package com.softcg.myapplication.domain.notificaciones

import com.softcg.myapplication.core.Reloj
import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.data.Repositories.EventosRepository
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.data.network.SesionUsuario
import com.softcg.myapplication.domain.gamificacion.AsistenciaReglas
import com.softcg.myapplication.domain.gamificacion.ConfirmarAsistenciaUseCase
import com.softcg.myapplication.domain.gamificacion.EntregarTareaUseCase
import com.softcg.myapplication.domain.gamificacion.EstadoAsistencia
import com.softcg.myapplication.domain.gamificacion.ReglasEntrega
import com.softcg.myapplication.domain.gamificacion.ResultadoAsistencia
import com.softcg.myapplication.domain.gamificacion.ResultadoEntregarTarea
import java.time.LocalDate
import javax.inject.Inject

/** Pone y quita las alarmas del sistema y retira las notificaciones que ya no tienen sentido. */
interface ProgramadorAvisos {
    fun programar(plan: PlanAvisos)
    fun cancelarTodo()
}

/**
 * Vuelve a calcular todas las alarmas con los datos guardados. Se llama al abrir la app, cuando cambian las tareas,
 * asignaturas o asistencias, al dispararse cualquier aviso y al reiniciar el teléfono: así funciona con la app cerrada.
 */
class ReprogramarAvisosUseCase @Inject constructor(
    private val tareas: TareasRepository,
    private val asignaturas: AsignaturasRepository,
    private val gamificacion: GamificacionRepository,
    private val sesion: SesionUsuario,
    private val reloj: Reloj,
    private val programador: ProgramadorAvisos
) {
    suspend operator fun invoke() {
        val uid = sesion.uid() ?: return programador.cancelarTodo()
        val ahora = reloj.ahora()
        val respondidas = gamificacion.asistencias(uid)
            .mapNotNull { a -> runCatching { a.asignaturaId to LocalDate.parse(a.fecha) }.getOrNull() }
            .toSet()
        programador.programar(
            ReglasAvisos.planificar(tareas.getTareas(), asignaturas.getAllAsignaturas(), respondidas, ahora.toLocalDateTime(), ahora.zone)
        )
    }

    /** Al cerrar sesión: los avisos de un estudiante no le llegan a quien entre después. */
    fun cancelarTodo() = programador.cancelarTodo()
}

/**
 * Arma el aviso en el momento de mostrarlo, con los datos de ese momento. Devuelve null si ya no aplica
 * (la tarea se entregó, la clase se borró, la asistencia ya se respondió, el día no tiene nada…).
 */
class ConstruirAvisoUseCase @Inject constructor(
    private val tareas: TareasRepository,
    private val asignaturas: AsignaturasRepository,
    private val eventos: EventosRepository,
    private val gamificacion: GamificacionRepository,
    private val sesion: SesionUsuario,
    private val reloj: Reloj
) {
    suspend operator fun invoke(tipo: TipoAviso, referencia: String): ContenidoAviso? {
        val uid = sesion.uid() ?: return null
        val ahora = reloj.ahora()
        return when (tipo) {
            TipoAviso.RESUMEN_DIA -> {
                val pendientes = tareas.getTareas().filter { it.fecha == referencia && ReglasAvisos.pendiente(it) }
                    .sortedBy { ReglasEntrega.limite(it.fecha, it.horaEntrega) }
                val delDia = eventos.getAllEventos().filter { it.fecha == referencia }.sortedBy { it.horaInicio.ifBlank { "99" } }
                TextosAvisos.resumenDelDia(referencia, pendientes, delDia)
            }
            TipoAviso.CLASE_PRONTO, TipoAviso.ASISTENCIA -> {
                val (asignaturaId, fecha) = ReglasAvisos.leerReferenciaClase(referencia) ?: return null
                val asignatura = asignaturas.getAsignatura(asignaturaId) ?: return null
                val clase = AsistenciaReglas.claseEnFecha(asignatura, fecha, ahora.zone) ?: return null
                if (tipo == TipoAviso.CLASE_PRONTO) {
                    if (!ahora.toLocalDateTime().isBefore(clase.fin)) return null
                    TextosAvisos.clasePronto(clase, asignatura.tutor)
                } else {
                    if (gamificacion.asistencia(uid, asignaturaId, fecha.toString()) != null) return null
                    if (AsistenciaReglas.estado(clase, null, ahora.toLocalDateTime()) != EstadoAsistencia.DISPONIBLE) return null
                    TextosAvisos.asistencia(clase)
                }
            }
            TipoAviso.TAREA_UNA_HORA, TipoAviso.TAREA_CINCO_MINUTOS, TipoAviso.TAREA_VENCIDA -> {
                val tarea = referencia.toIntOrNull()?.let { tareas.getTarea(it) } ?: return null
                if (!ReglasAvisos.pendiente(tarea)) return null
                val limite = ReglasEntrega.limite(tarea.fecha, tarea.horaEntrega) ?: return null
                TextosAvisos.tarea(tarea, tipo, limite)
            }
        }
    }
}

/** Resultado de pulsar un botón de la notificación: título y texto con los que se actualiza. */
data class RespuestaAccion(val titulo: String, val texto: String, val exito: Boolean)

/** Resuelve los botones de las notificaciones (asistí / no asistí / entregar) sin abrir la app. */
class ResponderAvisoUseCase @Inject constructor(
    private val confirmarAsistencia: ConfirmarAsistenciaUseCase,
    private val entregarTarea: EntregarTareaUseCase,
    private val tareas: TareasRepository
) {
    suspend operator fun invoke(accion: TipoAccionAviso, referencia: String): RespuestaAccion {
        return when (accion) {
            TipoAccionAviso.ASISTI, TipoAccionAviso.NO_ASISTI -> {
                val (asignaturaId, fecha) = ReglasAvisos.leerReferenciaClase(referencia)
                    ?: return RespuestaAccion("No se pudo registrar", "La clase ya no existe", false)
                when (val r = confirmarAsistencia(asignaturaId, fecha, accion == TipoAccionAviso.ASISTI)) {
                    is ResultadoAsistencia.Registrada -> TextosAvisos.confirmacionAsistencia(r.asistio, r.monedas).let { RespuestaAccion(it.first, it.second, true) }
                    ResultadoAsistencia.YaRespondida -> RespuestaAccion("Ya habías respondido", "Esta clase ya tiene tu respuesta", true)
                    ResultadoAsistencia.PlazoVencido -> RespuestaAccion("Se pasó el plazo", "Solo se puede responder hasta la medianoche del día de la clase", false)
                    ResultadoAsistencia.AunNoTermina -> RespuestaAccion("Aún no termina", "Podrás responder cuando acabe la clase", false)
                    ResultadoAsistencia.ClaseNoExiste -> RespuestaAccion("No se pudo registrar", "La clase ya no existe", false)
                    ResultadoAsistencia.SinSesion -> RespuestaAccion("Inicia sesión", "Abre EduPlanner para responder", false)
                    ResultadoAsistencia.Error -> RespuestaAccion("No se pudo registrar", "Inténtalo desde la app", false)
                }
            }
            TipoAccionAviso.ENTREGAR -> {
                val tarea = referencia.toIntOrNull()?.let { tareas.getTarea(it) }
                    ?: return RespuestaAccion("No se pudo entregar", "La tarea ya no existe", false)
                when (val r = entregarTarea(tarea)) {
                    is ResultadoEntregarTarea.Entregada -> TextosAvisos.confirmacionEntrega(r.monedas, r.aTiempo).let { RespuestaAccion(it.first, it.second, true) }
                    ResultadoEntregarTarea.YaEntregada -> RespuestaAccion("Ya estaba entregada", tarea.titulo, true)
                    else -> RespuestaAccion("No se pudo entregar", "Inténtalo desde la app", false)
                }
            }
        }
    }
}
