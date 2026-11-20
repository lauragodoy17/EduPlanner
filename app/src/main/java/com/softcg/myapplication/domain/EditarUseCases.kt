package com.softcg.myapplication.domain

import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.domain.TextoUsuario
import com.softcg.myapplication.data.Repositories.CalificacionesRespository
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.domain.calificaciones.EscalaBloqueadaUseCase
import com.softcg.myapplication.domain.compartir.ImportarCompartidoUseCase
import com.softcg.myapplication.domain.prediccion.CatalogoModelo
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import javax.inject.Inject

sealed class ResultadoEdicion {
    object Guardada : ResultadoEdicion()
    object NombreVacio : ResultadoEdicion()
    /** Otra asignatura ya se llama así (sin distinguir tildes ni mayúsculas). */
    object NombreRepetido : ResultadoEdicion()
    /** Se borró mientras se editaba, o no hay sesión. */
    object NoExiste : ResultadoEdicion()
    /** Se intentó cambiar la escala de una asignatura que ya tiene notas. */
    object EscalaBloqueada : ResultadoEdicion()
}

/** Edita una tarea; si cambia de asignatura, su calificación (si la tiene) se va con ella. */
class EditarTareaUseCase @Inject constructor(
    private val tareas: TareasRepository,
    private val calificaciones: CalificacionesRespository
) {
    suspend operator fun invoke(tarea: Tarea): ResultadoEdicion {
        if (tarea.titulo.isBlank()) return ResultadoEdicion.NombreVacio
        val id = tarea.id ?: return ResultadoEdicion.NoExiste
        val anterior = tareas.getTarea(id) ?: return ResultadoEdicion.NoExiste
        val editada = tarea.copy(titulo = tarea.titulo.trim(), descrip = tarea.descrip.trim())
        if (!tareas.updateTarea(editada)) return ResultadoEdicion.NoExiste
        if (anterior.asignatura != editada.asignatura) calificaciones.moverDeTarea(id, editada.asignatura)
        return ResultadoEdicion.Guardada
    }
}

/**
 * Edita una asignatura. Las tareas, notas y sesiones se relacionan con ella también por su nombre: al renombrarla
 * pasan al nombre nuevo en la misma transacción. El área que confirmó el estudiante se conserva; si no tenía área,
 * se intenta detectar con el nombre nuevo. La escala de notas solo cambia si aún no tiene notas.
 */
class EditarAsignaturaUseCase @Inject constructor(
    private val asignaturas: AsignaturasRepository,
    private val gamificacion: GamificacionRepository,
    private val escalaBloqueada: EscalaBloqueadaUseCase
) {
    suspend operator fun invoke(id: Int, datos: NuevaAsignatura): ResultadoEdicion {
        val nombre = TextoUsuario.linea(datos.nombre, TextoUsuario.MAX_NOMBRE_ASIGNATURA)
        if (nombre.isEmpty()) return ResultadoEdicion.NombreVacio
        val actual = asignaturas.getAsignatura(id) ?: return ResultadoEdicion.NoExiste
        val otras = asignaturas.getAllAsignaturas().filter { it.id != id }.map { it.nombre }
        if (ImportarCompartidoUseCase.buscar(nombre, otras) != null) return ResultadoEdicion.NombreRepetido
        val nuevaEscala = datos.escala?.takeIf { it != actual.escala }
        if (nuevaEscala != null && escalaBloqueada(actual)) return ResultadoEdicion.EscalaBloqueada

        val editada = actual.copy(
            nombre = nombre,
            tutor = TextoUsuario.linea(datos.tutor, TextoUsuario.MAX_PERSONA),
            duracion = datos.duracion,
            hora = datos.hora.trim(),
            horario = CrearAsignaturaUseCase.horario(datos.dias),
            tipoCalificacion = nuevaEscala?.etiquetaGuardada ?: actual.tipoCalificacion,
            escalaMin = nuevaEscala?.min ?: actual.escalaMin,
            escalaMax = nuevaEscala?.max ?: actual.escalaMax
        )
        val guardada = gamificacion.enTransaccion {
            val ok = asignaturas.actualizarAsignatura(editada)
            if (ok && actual.nombre != nombre) asignaturas.renombrarEnRegistros(id, actual.nombre, nombre)
            if (ok && nuevaEscala != null) asignaturas.guardarEscala(id, nuevaEscala)
            ok
        }
        if (!guardada) return ResultadoEdicion.NoExiste

        if (actual.areaModelo == null && actual.nombre != nombre) {
            CatalogoModelo.areaDetectada(nombre)?.let { asignaturas.guardarDatosModelo(id, it, editada.escalaMin, editada.escalaMax) }
        }
        return ResultadoEdicion.Guardada
    }
}
