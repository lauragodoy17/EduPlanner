package com.softcg.myapplication.domain.compartir

import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import javax.inject.Inject

/**
 * Antes de dibujar el QR de una tarea o nota, le agrega la asignatura completa del dispositivo que la comparte,
 * para que quien la reciba sin tener esa asignatura la cree con su profesor, horario, escala y área.
 */
class PrepararCompartidoUseCase @Inject constructor(private val asignaturas: AsignaturasRepository) {

    operator fun invoke(contenido: ContenidoCompartido): ContenidoCompartido {
        val nombre = contenido.asignatura ?: return contenido
        val locales = asignaturas.getAllAsignaturas()
        val encontrada = ImportarCompartidoUseCase.buscar(nombre, locales.map { it.nombre })
            ?.let { n -> locales.firstOrNull { it.nombre == n } }
        return contenido.conAsignatura(encontrada)
    }
}
