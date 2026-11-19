package com.softcg.myapplication.ui.Inicio.Dialogs

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
            AccionDato.AREA -> elegirArea(asignaturaId, resultado.asignatura.nombre)
            AccionDato.ESCALA -> abrirDatosAsignatura(asignaturaId, soloSiFalta = false)
            AccionDato.ESTUDIO -> declararEstudio(asignaturaId, resultado.asignatura.nombre)
            AccionDato.PERCEPCION -> declararInteresYDificultad(asignaturaId, resultado.asignatura.nombre, resultado.tarea?.tema)
            null -> Unit
        }
    }

    /**
     * Área y escala de notas. Con [soloSiFalta] (al crear la asignatura) no se abre si el área ya se detectó
     * por el nombre y el tipo de calificación indica la escala: no hay nada que preguntar.
     */
    fun abrirDatosAsignatura(asignaturaId: Int, soloSiFalta: Boolean = true, onGuardado: () -> Unit = {}) {
        scope.launch {
            val asignatura = datos.asignaturas().firstOrNull { it.id == asignaturaId } ?: return@launch
            if (soloSiFalta && !DatosAsignaturaDialog.hayQuePreguntar(asignatura)) return@launch
            DatosAsignaturaDialog(
                context, asignatura, scope,
                guardar = { area, min, max -> datos.guardarAsignatura(asignaturaId, area, min, max) },
                onGuardado = {
                    fragment.view?.mostrarAviso("Datos guardados")
                    onGuardado()
                }
            ).show()
        }
    }

    private fun elegirArea(asignaturaId: Int, nombre: String) {
        AreaAsignaturaDialog(context, nombre, onGuardar = { area ->
            scope.launch { avisar(datos.guardarArea(asignaturaId, area), "Área guardada: ${CatalogoModelo.etiqueta(area)}") }
        }).show()
    }

    private fun declararEstudio(asignaturaId: Int, nombre: String) {
        scope.launch {
            EstudioDeclaradoDialog(
                context, nombre, datos.respuestasActuales(asignaturaId), scope,
                guardar = { horas, metodo -> datos.guardarEstudio(asignaturaId, horas, metodo) },
                onGuardado = { fragment.view?.mostrarAviso("Datos guardados") }
            ).show()
        }
    }

    private fun declararInteresYDificultad(asignaturaId: Int, nombre: String, tema: String?) {
        scope.launch {
            InteresDificultadDialog(
                context, nombre, tema, datos.respuestasActuales(asignaturaId), scope,
                guardar = { interes, dificultad -> datos.guardarInteresYDificultad(asignaturaId, tema, interes, dificultad) },
                onGuardado = { fragment.view?.mostrarAviso("Respuestas guardadas") }
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

    fun registrarEstudio(asignaturaId: Int? = null) {
        RegistroEstudioDialog(
            context, datos.asignaturas(), asignaturaId, scope,
            registrar = { id, fecha, minutos, metodo -> datos.registrarEstudioManual(id, fecha, minutos, metodo) },
            onGuardado = { fragment.view?.mostrarAviso("Estudio registrado") }
        ).show()
    }
}
