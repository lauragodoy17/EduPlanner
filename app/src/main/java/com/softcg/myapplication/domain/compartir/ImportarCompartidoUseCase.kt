package com.softcg.myapplication.domain.compartir

import android.util.Log
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
import com.softcg.myapplication.domain.gamificacion.CondicionesRecompensa
import com.softcg.myapplication.domain.gamificacion.OtorgarRecompensaUseCase
import com.softcg.myapplication.domain.gamificacion.ResultadoRecompensa
import com.softcg.myapplication.domain.gamificacion.TipoRecompensa
import com.softcg.myapplication.domain.prediccion.CatalogoModelo
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models.Calificacion
import com.softcg.myapplication.ui.Inicio.Models.Evento
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import javax.inject.Inject

/** A qué asignatura del dispositivo va una tarea o nota importada. */
sealed class DestinoAsignatura {
    data class Existente(val nombre: String) : DestinoAsignatura()
    data class Nueva(val nombre: String) : DestinoAsignatura()
}

/** Lo que hay que preguntarle al estudiante antes de importar. */
data class AnalisisImportacion(
    val contenido: ContenidoCompartido,
    /** La asignatura del dispositivo que coincide con la del contenido (sin distinguir tildes ni mayúsculas). */
    val asignaturaLocal: String?,
    val asignaturasLocales: List<String>,
    /** Al compartir una asignatura: ya hay una con ese nombre en el dispositivo. */
    val asignaturaRepetida: Boolean
) {
    /** Tarea o nota cuya asignatura no existe aquí: hay que crearla o elegir otra. */
    val faltaAsignatura: Boolean get() = contenido.asignatura != null && asignaturaLocal == null
}

sealed class ResultadoImportacion {
    /**
     * [asignaturaCreada]: nombre de la asignatura que se creó para recibirlo, si hizo falta.
     * [recompensa]: EduCoins ganados por registrarlo; null si no hubo (tope diario o datos incompletos).
     */
    data class Importado(
        val tipo: TipoCompartido,
        val nombre: String,
        val asignaturaCreada: String?,
        val recompensa: Int? = null
    ) : ResultadoImportacion()
    /** Tarea o nota sin una asignatura válida de destino. */
    object FaltaAsignatura : ResultadoImportacion()
    object AsignaturaRepetida : ResultadoImportacion()
    object SinSesion : ResultadoImportacion()
    object Error : ResultadoImportacion()
}

/**
 * Crea en este dispositivo lo que llegó por QR, después de que el estudiante lo confirme.
 * Lo importado da los mismos EduCoins que registrarlo a mano, con las mismas condiciones y el mismo
 * tope diario por tipo ([TipoRecompensa.topeDiario]): escanear muchos códigos no da más que escribirlos.
 */
class ImportarCompartidoUseCase @Inject constructor(
    private val asignaturas: AsignaturasRepository,
    private val tareas: TareasRepository,
    private val eventos: EventosRepository,
    private val calificaciones: CalificacionesRespository,
    private val crearAsignatura: CrearAsignaturaUseCase,
    private val otorgarRecompensa: OtorgarRecompensaUseCase,
    private val sesion: SesionUsuario
) {
    fun analizar(contenido: ContenidoCompartido): AnalisisImportacion {
        val locales = asignaturas.getAllAsignaturas().map { it.nombre }
        val nombre = (contenido as? AsignaturaCompartida)?.nombre ?: contenido.asignatura
        val coincidente = nombre?.let { buscar(it, locales) }
        return AnalisisImportacion(
            contenido = contenido,
            asignaturaLocal = if (contenido.asignatura != null) coincidente else null,
            asignaturasLocales = locales,
            asignaturaRepetida = contenido is AsignaturaCompartida && coincidente != null
        )
    }

    /** [destino] solo hace falta para tareas y notas cuya asignatura no existe en el dispositivo. */
    suspend fun importar(contenido: ContenidoCompartido, destino: DestinoAsignatura? = null): ResultadoImportacion {
        sesion.uid() ?: return ResultadoImportacion.SinSesion
        return try {
            when (contenido) {
                is AsignaturaCompartida -> importarAsignatura(contenido)
                is EventoCompartido -> {
                    val evento = Evento(null, contenido.titulo, contenido.descripcion, contenido.fecha, contenido.prioridad, contenido.horaInicio, contenido.horaFin)
                    val id = eventos.insertEvento(evento)
                    if (id <= 0) return ResultadoImportacion.Error
                    val recompensa = if (CondicionesRecompensa.eventoValido(contenido.titulo, contenido.fecha)) {
                        premiar(TipoRecompensa.EVENTO_REGISTRADO, id)
                    } else null
                    ResultadoImportacion.Importado(contenido.tipo, contenido.titulo, null, recompensa)
                }
                is TareaCompartida -> conAsignatura(contenido, destino) { asignatura ->
                    val id = tareas.insertTarea(Tarea(null, contenido.titulo, contenido.descripcion, asignatura, contenido.fecha, contenido.prioridad))
                    when {
                        id <= 0 -> null
                        CondicionesRecompensa.tareaValida(contenido.titulo, contenido.fecha) -> Guardado(premiar(TipoRecompensa.TAREA_REGISTRADA, id))
                        else -> Guardado(null)
                    }
                }
                is NotaCompartida -> conAsignatura(contenido, destino) { asignatura ->
                    val id = calificaciones.insertCalificacion(Calificacion(null, contenido.tipoNota, contenido.valor, asignatura, contenido.descripcion))
                    when {
                        id <= 0 -> null
                        CondicionesRecompensa.calificacionValida(contenido.valor, asignatura) -> Guardado(premiar(TipoRecompensa.CALIFICACION_REGISTRADA, id))
                        else -> Guardado(null)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("ImportarCompartido", "No se pudo guardar lo leído del QR", e)
            ResultadoImportacion.Error
        }
    }

    private suspend fun importarAsignatura(a: AsignaturaCompartida): ResultadoImportacion {
        if (buscar(a.nombre, nombresLocales()) != null) return ResultadoImportacion.AsignaturaRepetida
        val dias = CrearAsignaturaUseCase.DIAS.filterIndexed { i, _ -> a.dias.getOrElse(i) { false } }
        val creada = crearAsignatura(NuevaAsignatura(a.nombre, a.tutor, a.duracion, a.hora, dias, a.tipoCalificacion))
            as? ResultadoCrearAsignatura.Creada ?: return ResultadoImportacion.Error
        // El área elegida por quien la comparte vale más que la detectada por el nombre
        if (a.area != null || a.escalaMin != null) {
            asignaturas.guardarDatosModelo(creada.id, a.area ?: creada.area, a.escalaMin, a.escalaMax)
        }
        return ResultadoImportacion.Importado(a.tipo, creada.nombre, null, creada.recompensa)
    }

    private suspend fun conAsignatura(
        contenido: ContenidoCompartido,
        destino: DestinoAsignatura?,
        guardar: suspend (asignatura: String) -> Guardado?
    ): ResultadoImportacion {
        val locales = nombresLocales()
        var creada: String? = null
        val asignatura = when (destino) {
            // Lo que llegó ya tiene su asignatura aquí
            null -> contenido.asignatura?.let { buscar(it, locales) } ?: return ResultadoImportacion.FaltaAsignatura
            is DestinoAsignatura.Existente -> buscar(destino.nombre, locales) ?: return ResultadoImportacion.FaltaAsignatura
            is DestinoAsignatura.Nueva -> {
                val nombre = destino.nombre.trim().ifEmpty { return ResultadoImportacion.FaltaAsignatura }
                buscar(nombre, locales) ?: when (val r = crearAsignatura(NuevaAsignatura(nombre))) {
                    is ResultadoCrearAsignatura.Creada -> r.nombre.also { creada = it }
                    else -> return ResultadoImportacion.Error
                }
            }
        }
        val nombre = when (contenido) {
            is TareaCompartida -> contenido.titulo
            is NotaCompartida -> contenido.tipoNota.ifBlank { "Nota" }
            else -> ""
        }
        val guardado = guardar(asignatura) ?: return ResultadoImportacion.Error
        return ResultadoImportacion.Importado(contenido.tipo, nombre, creada, guardado.recompensa)
    }

    /** Se guardó; [recompensa] son los EduCoins ganados, si hubo. */
    private class Guardado(val recompensa: Int?)

    private suspend fun premiar(tipo: TipoRecompensa, id: Long): Int? =
        (otorgarRecompensa(tipo, "$id") as? ResultadoRecompensa.Otorgada)?.cantidad

    private fun nombresLocales() = asignaturas.getAllAsignaturas().map { it.nombre }

    companion object {
        /** El nombre tal como está guardado en el dispositivo, comparando sin tildes, mayúsculas ni espacios extra. */
        fun buscar(nombre: String, locales: List<String>): String? {
            val buscado = CatalogoModelo.normalizar(nombre).replace(Regex("\\s+"), " ")
            if (buscado.isEmpty()) return null
            return locales.firstOrNull { CatalogoModelo.normalizar(it).replace(Regex("\\s+"), " ") == buscado }
        }
    }
}
