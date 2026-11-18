package com.softcg.myapplication.ui.Inicio.Dialogs

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.mostrarAviso
import com.softcg.myapplication.domain.prediccion.AccionDato
import com.softcg.myapplication.domain.prediccion.CatalogoModelo
import com.softcg.myapplication.domain.prediccion.EstadoDato
import com.softcg.myapplication.domain.prediccion.Extraccion
import com.softcg.myapplication.domain.prediccion.ResultadoExtraccion
import com.softcg.myapplication.domain.prediccion.ResultadoGuardarDatos
import com.softcg.myapplication.ui.Inicio.DatosEstudioViewModel
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import kotlinx.coroutines.launch

/**
 * Abre la hoja adecuada para completar cada dato de las predicciones, desde cualquier pantalla.
 * Así asignaturas, inicio, enfoque y predicción recogen los datos de la misma forma.
 */
class AccionesDatosEstudio(
    private val fragment: Fragment,
    private val datos: DatosEstudioViewModel
) {
    private val context get() = fragment.requireContext()
    private val scope get() = fragment.viewLifecycleOwner.lifecycleScope

    private fun avisar(resultado: ResultadoGuardarDatos, exito: String) {
        val vista = fragment.view ?: return
        val error = resultado.mensajeError()
        if (error == null) vista.mostrarAviso(exito) else vista.mostrarAviso(error, esError = true)
    }

    fun ejecutar(estado: EstadoDato, resultado: ResultadoExtraccion) {
        val asignaturaId = resultado.asignatura.id ?: return
        when (estado.accion) {
            AccionDato.DATOS_ASIGNATURA -> abrirDatosAsignatura(asignaturaId)
            AccionDato.DIFICULTAD -> preguntarDificultadAsignatura(asignaturaId, resultado.asignatura.nombre, resultado.tarea?.tema)
            AccionDato.ENFOQUE -> fragment.findNavController().navigate(R.id.id_focus_fragment)
            AccionDato.REGISTRAR_ESTUDIO -> registrarEstudio(asignaturaId)
            AccionDato.CALIFICACIONES -> fragment.findNavController().navigate(R.id.id_ratings_fragment)
            null -> Unit
        }
    }

    fun abrirDatosAsignatura(asignaturaId: Int, onGuardado: () -> Unit = {}) {
        scope.launch {
            val asignatura = datos.asignaturas().firstOrNull { it.id == asignaturaId } ?: return@launch
            val (interes, metodo) = datos.respuestasActuales(asignaturaId)
            DatosAsignaturaDialog(
                context, asignatura, interes, metodo, scope,
                guardar = { area, min, max, valorInteres, valorMetodo ->
                    datos.guardarAsignatura(asignaturaId, area, min, max, valorInteres, valorMetodo)
                },
                onGuardado = {
                    fragment.view?.mostrarAviso("Datos guardados")
                    onGuardado()
                }
            ).show()
        }
    }

    fun abrirProgreso(asignaturaId: Int) {
        scope.launch {
            val extraccion = datos.progresoAsignatura(asignaturaId) as? Extraccion.Lista ?: return@launch
            ProgresoDatosDialog(context, extraccion.resultado) { estado -> ejecutar(estado, extraccion.resultado) }.show()
        }
    }

    /** Al entregar o calificar una tarea, una sola vez por tarea. */
    fun preguntarDificultadDeTarea(tarea: Tarea) {
        scope.launch {
            if (!datos.debePreguntarDificultad(tarea)) return@launch
            DificultadDialog(
                context, "¿Qué tan difícil fue?",
                listOf(tarea.titulo, tarea.asignatura).filter { it.isNotBlank() }.joinToString(" · "),
                onGuardar = { valor -> scope.launch { avisar(datos.dificultadDeTarea(tarea, valor), "Respuesta guardada") } }
            ).show()
        }
    }

    private fun preguntarDificultadAsignatura(asignaturaId: Int, nombre: String, tema: String?) {
        DificultadDialog(
            context,
            if (tema != null) "¿Qué tan difícil te parece este tema?" else "¿Qué tan difícil te parece la asignatura?",
            listOfNotNull(tema?.let(CatalogoModelo::etiqueta), nombre).joinToString(" · "),
            onGuardar = { valor -> scope.launch { avisar(datos.dificultadDeAsignatura(asignaturaId, tema, valor), "Respuesta guardada") } }
        ).show()
    }

    fun registrarEstudio(asignaturaId: Int? = null) {
        RegistroEstudioDialog(
            context, datos.asignaturas(), asignaturaId, scope,
            registrar = { id, fecha, minutos, metodo -> datos.registrarEstudioManual(id, fecha, minutos, metodo) },
            onGuardado = { fragment.view?.mostrarAviso("Estudio registrado") }
        ).show()
    }
}
