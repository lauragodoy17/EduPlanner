package com.softcg.myapplication.domain.prediccion

import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import javax.inject.Inject

/**
 * Asigna el área a las asignaturas que aún no la tienen (las creadas antes de la detección automática),
 * solo cuando su nombre corresponde a una única área. Las demás siguen esperando a que el estudiante la elija.
 * Devuelve cuántas se completaron.
 */
class DetectarAreasPendientesUseCase @Inject constructor(private val repository: AsignaturasRepository) {

    suspend operator fun invoke(): Int =
        repository.getAllAsignaturas()
            .filter { it.id != null && !CatalogoModelo.esArea(it.areaModelo) }
            .count { asignatura ->
                val area = CatalogoModelo.areaDetectada(asignatura.nombre) ?: return@count false
                repository.guardarDatosModelo(asignatura.id!!, area, asignatura.escalaMin, asignatura.escalaMax)
            }
}
