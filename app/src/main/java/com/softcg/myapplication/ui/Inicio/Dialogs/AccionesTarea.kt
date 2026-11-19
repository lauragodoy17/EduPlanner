package com.softcg.myapplication.ui.Inicio.Dialogs

import androidx.fragment.app.Fragment
import com.softcg.myapplication.core.ex.confirmar
import com.softcg.myapplication.core.ex.mostrarAviso
import com.softcg.myapplication.core.ex.mostrarMensaje
import com.softcg.myapplication.core.ex.mostrarRecompensa
import com.softcg.myapplication.core.ex.preguntar
import com.softcg.myapplication.domain.gamificacion.ResultadoCalificarTarea
import com.softcg.myapplication.domain.gamificacion.ResultadoEntregarTarea
import com.softcg.myapplication.ui.Inicio.InicioViewModel
import com.softcg.myapplication.ui.Inicio.Models.Tarea

/**
 * Entregar, calificar y borrar una tarea, igual desde Inicio, Agenda y la pantalla de la tarea.
 * Al entregarla se ofrece añadir la calificación o dejarlo para más tarde; al terminar (con nota o sin ella)
 * se pregunta su dificultad, una sola vez por tarea.
 */
class AccionesTarea(
    private val fragment: Fragment,
    private val inicio: InicioViewModel,
    private val datosEstudio: AccionesDatosEstudio,
    /** Borra la tarea en el ViewModel de la pantalla (ya confirmado). */
    private val onBorrar: (Tarea) -> Unit,
    /** Recarga la lista de la pantalla tras entregar o calificar. */
    private val onCambio: () -> Unit
) {
    private val context get() = fragment.requireContext()

    /** Tarea que se está entregando o calificando, para seguir su flujo cuando llegue el resultado. */
    private var tareaEnCurso: Tarea? = null

    /** Llamar al crear la vista de la pantalla. */
    fun observarResultados() {
        val dueno = fragment.viewLifecycleOwner
        inicio.resultadoEntregarTarea.observe(dueno) { evento ->
            val resultado = evento.getContentIfNotHandled() ?: return@observe
            val tarea = tareaEnCurso
            tareaEnCurso = null
            when (resultado) {
                is ResultadoEntregarTarea.Entregada -> {
                    onCambio()
                    celebrar(resultado.monedas, resultado.aTiempo, "Tarea entregada")
                    tarea?.let { ofrecerCalificacion(it) }
                }
                ResultadoEntregarTarea.YaEntregada -> mensaje("Esta tarea ya estaba entregada")
                ResultadoEntregarTarea.Error -> mensaje("No se pudo marcar la tarea. Inténtalo de nuevo")
            }
        }
        inicio.resultadoCalificarTarea.observe(dueno) { evento ->
            val resultado = evento.getContentIfNotHandled() ?: return@observe
            val tarea = tareaEnCurso
            tareaEnCurso = null
            when (resultado) {
                is ResultadoCalificarTarea.Completada -> {
                    onCambio()
                    celebrar(resultado.monedas, resultado.aTiempo, "Tarea completada")
                    tarea?.let { datosEstudio.preguntarDificultadDeTarea(it) }
                }
                ResultadoCalificarTarea.YaCalificada -> mensaje("Esta tarea ya tiene una calificación")
                ResultadoCalificarTarea.ValorInvalido -> mensaje("Ingresa una calificación válida")
                ResultadoCalificarTarea.Error -> mensaje("No se pudo guardar la calificación. Inténtalo de nuevo")
            }
        }
    }

    fun entregar(tarea: Tarea) {
        tareaEnCurso = tarea
        inicio.entregarTarea(tarea)
    }

    fun calificar(tarea: Tarea) {
        CalificarTareaDialog(
            context, tarea, inicio.asignaturaDe(tarea),
            onEscalaNueva = { id, escala -> inicio.guardarEscala(id, escala) }
        ) { texto ->
            tareaEnCurso = tarea
            inicio.calificarTarea(tarea, texto)
        }.show()
    }

    fun borrar(tarea: Tarea) {
        context.confirmar("¿Eliminar tarea?", "Esta acción no se puede deshacer.", "Eliminar") {
            onBorrar(tarea)
            fragment.view?.mostrarAviso("Tarea eliminada")
        }
    }

    /** La nota suele llegar después de entregar: se ofrece ahora sin obligar. */
    private fun ofrecerCalificacion(tarea: Tarea) {
        context.preguntar(
            titulo = "¿Ya tienes la calificación?",
            mensaje = "Puedes añadirla ahora o más tarde desde la tarea «${tarea.titulo}».",
            textoSi = "Añadir calificación",
            textoNo = "Más tarde",
            onSi = { calificar(tarea) },
            onNo = { datosEstudio.preguntarDificultadDeTarea(tarea) }
        )
    }

    private fun celebrar(monedas: Int, aTiempo: Boolean, sinMonedas: String) {
        val actividad = fragment.activity ?: return
        if (monedas > 0) actividad.mostrarRecompensa(monedas, if (aTiempo) "A tiempo" else null)
        else actividad.mostrarMensaje(sinMonedas)
    }

    private fun mensaje(texto: String) {
        fragment.activity?.mostrarMensaje(texto)
    }
}
