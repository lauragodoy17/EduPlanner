package com.softcg.myapplication.domain

import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.domain.gamificacion.CondicionesRecompensa
import com.softcg.myapplication.domain.gamificacion.OtorgarRecompensaUseCase
import com.softcg.myapplication.domain.gamificacion.ResultadoRecompensa
import com.softcg.myapplication.domain.gamificacion.TipoRecompensa
import com.softcg.myapplication.domain.prediccion.CatalogoModelo
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import java.util.Locale
import javax.inject.Inject

/** Lo que el estudiante escribe en el formulario de asignatura. [dias] son los nombres ("Lunes", "Martes"...). */
data class NuevaAsignatura(
    val nombre: String,
    val tutor: String = "",
    val duracion: Int = 0,
    val hora: String = "",
    val dias: List<String> = emptyList(),
    val tipoCalificacion: String = ""
)

sealed class ResultadoCrearAsignatura {
    /**
     * [recompensa] son los EduCoins ganados por registrar la clase; null si no hubo.
     * [area] es la detectada por el nombre; null si no se pudo y hay que preguntársela al estudiante.
     */
    data class Creada(val id: Int, val nombre: String, val recompensa: Int?, val area: String? = null) : ResultadoCrearAsignatura()
    object NombreVacio : ResultadoCrearAsignatura()
    /** Sin sesión o Room no la guardó. */
    object Error : ResultadoCrearAsignatura()
}

/**
 * Guarda una asignatura con el área del modelo detectada por su nombre y premia la clase si tiene hora, duración
 * y algún día. La usan Asignaturas y el formulario de tarea.
 */
class CrearAsignaturaUseCase @Inject constructor(
    private val repository: AsignaturasRepository,
    private val otorgarRecompensa: OtorgarRecompensaUseCase
) {

    companion object {
        val DIAS = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

        /** Siete booleanos de lunes a domingo, como los guarda la asignatura. */
        fun horario(dias: List<String>): List<Boolean> {
            val elegidos = dias.map { it.trim().lowercase(Locale.getDefault()) }.toSet()
            return DIAS.map { it.lowercase(Locale.getDefault()) in elegidos }
        }
    }

    suspend operator fun invoke(nueva: NuevaAsignatura): ResultadoCrearAsignatura {
        val nombre = nueva.nombre.trim()
        if (nombre.isEmpty()) return ResultadoCrearAsignatura.NombreVacio

        val horario = horario(nueva.dias)
        val hora = nueva.hora.trim()
        val area = CatalogoModelo.areaDetectada(nombre)
        val asignatura = Asignatura(
            null, nombre, nueva.tutor.trim(), nueva.duracion, hora, horario, nueva.tipoCalificacion, areaModelo = area
        )
        val id = repository.insertAsignatura(asignatura)
        if (id <= 0) return ResultadoCrearAsignatura.Error

        val recompensa = if (CondicionesRecompensa.claseValida(hora, nueva.duracion, horario)) {
            (otorgarRecompensa(TipoRecompensa.CLASE_REGISTRADA, "$id") as? ResultadoRecompensa.Otorgada)?.cantidad
        } else null
        return ResultadoCrearAsignatura.Creada(id.toInt(), nombre, recompensa, area)
    }
}
