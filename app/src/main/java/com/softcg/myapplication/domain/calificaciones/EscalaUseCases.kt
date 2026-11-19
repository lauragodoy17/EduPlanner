package com.softcg.myapplication.domain.calificaciones

import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.data.Repositories.CalificacionesRespository
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import javax.inject.Inject

sealed class ResultadoEscala {
    object Guardada : ResultadoEscala()
    /** Ya tiene notas con otra escala: cambiarla haría que se leyeran mal. */
    object Bloqueada : ResultadoEscala()
    /** Se borró, o no hay sesión. */
    object NoExiste : ResultadoEscala()
}

/**
 * La escala se elige al crear la asignatura. Solo se puede cambiar mientras no tenga ninguna nota; una asignatura
 * antigua sin escala puede recibirla una vez aunque ya tenga notas (sin escala no se podían leer).
 */
class EscalaBloqueadaUseCase @Inject constructor(
    private val calificaciones: CalificacionesRespository
) {
    suspend operator fun invoke(asignatura: Asignatura): Boolean {
        val id = asignatura.id ?: return false
        if (asignatura.escala == null) return false
        return calificaciones.contarDeAsignatura(id, asignatura.nombre) > 0
    }
}

class GuardarEscalaAsignaturaUseCase @Inject constructor(
    private val asignaturas: AsignaturasRepository,
    private val bloqueada: EscalaBloqueadaUseCase
) {
    suspend operator fun invoke(asignaturaId: Int, escala: EscalaCalificacion): ResultadoEscala {
        val actual = asignaturas.getAsignatura(asignaturaId) ?: return ResultadoEscala.NoExiste
        if (actual.escala == escala) return ResultadoEscala.Guardada
        if (bloqueada(actual)) return ResultadoEscala.Bloqueada
        return if (asignaturas.guardarEscala(asignaturaId, escala)) ResultadoEscala.Guardada else ResultadoEscala.NoExiste
    }
}
