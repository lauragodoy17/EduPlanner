package com.softcg.myapplication.ui.Inicio.Dialogs

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputLayout
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.mostrarCarga
import com.softcg.myapplication.core.ex.showAsBottomSheet
import com.softcg.myapplication.core.ex.showDatePicker
import com.softcg.myapplication.core.ex.tintarFondo
import com.softcg.myapplication.domain.prediccion.CatalogoModelo
import com.softcg.myapplication.domain.prediccion.EstadoDato
import com.softcg.myapplication.domain.prediccion.NivelesPercepcion
import com.softcg.myapplication.domain.prediccion.ReglasExtraccion
import com.softcg.myapplication.domain.prediccion.ResultadoExtraccion
import com.softcg.myapplication.domain.prediccion.ResultadoGuardarDatos
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Texto para el estudiante de un resultado que no se pudo guardar; null si se guardó. */
fun ResultadoGuardarDatos.mensajeError(): String? = when (this) {
    ResultadoGuardarDatos.Guardado -> null
    is ResultadoGuardarDatos.Invalido -> motivo
    ResultadoGuardarDatos.SinSesion -> "Inicia sesión para guardar tus datos"
    ResultadoGuardarDatos.Error -> "No se pudo guardar. Inténtalo de nuevo"
}

/** Llena un ChipGroup con una escala corta y marca [seleccion] si la hay. */
private fun ChipGroup.llenarOpciones(opciones: List<String>, seleccion: Int?) {
    removeAllViews()
    val inflater = LayoutInflater.from(context)
    opciones.forEachIndexed { i, texto ->
        val chip = inflater.inflate(R.layout.item_chip_opcion, this, false) as Chip
        chip.id = View.generateViewId()
        chip.text = texto
        chip.tag = i
        addView(chip)
        if (i == seleccion) chip.isChecked = true
    }
}

private fun ChipGroup.indiceMarcado(): Int? = findViewById<Chip>(checkedChipId)?.tag as? Int

private fun numero(texto: String): Float? = texto.trim().replace(',', '.').toFloatOrNull()

/**
 * Área, escala de notas, interés y método de una asignatura. Se abre al crear una asignatura, desde su
 * panel de datos y desde la predicción de una tarea. Nada es obligatorio.
 */
class DatosAsignaturaDialog(
    private val context: Context,
    private val asignatura: Asignatura,
    private val interesActual: Float?,
    private val metodoActual: String?,
    private val scope: CoroutineScope,
    private val guardar: suspend (area: String?, escalaMin: Float?, escalaMax: Float?, interes: Float?, metodo: String?) -> ResultadoGuardarDatos,
    private val onGuardado: () -> Unit = {}
) {
    fun show() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.sheet_datos_asignatura)
        dialog.findViewById<TextView>(R.id.datosTitulo).text = asignatura.nombre

        // Área: la confirmada o, si no hay, la sugerida por el nombre (se confirma al guardar)
        val areas = CatalogoModelo.AREAS
        var area: String? = asignatura.areaModelo?.takeIf { CatalogoModelo.esArea(it) }
        val sugerida = if (area == null) CatalogoModelo.sugerirArea(asignatura.nombre) else null
        val campoArea = dialog.findViewById<AutoCompleteTextView>(R.id.datosArea)
        campoArea.setAdapter(ArrayAdapter(context, R.layout.item_menu_asignatura, areas.map(CatalogoModelo::etiqueta)))
        (area ?: sugerida)?.let { campoArea.setText(CatalogoModelo.etiqueta(it), false) }
        area = area ?: sugerida
        if (sugerida != null) {
            dialog.findViewById<TextView>(R.id.datosAreaSugerida).apply {
                text = "Sugerida por el nombre de la asignatura: confírmala o elige otra"
                visibility = View.VISIBLE
            }
        }
        campoArea.setOnItemClickListener { _, _, posicion, _ -> area = areas[posicion] }

        // Escala: solo si el tipo de calificación no dice el rango
        val sinRango = ReglasExtraccion.escalaDeTexto(asignatura.tipoCalificacion) == null
        val minimo = dialog.findViewById<EditText>(R.id.datosEscalaMin)
        val maximo = dialog.findViewById<EditText>(R.id.datosEscalaMax)
        if (sinRango) {
            dialog.findViewById<View>(R.id.datosEscala).visibility = View.VISIBLE
            dialog.findViewById<TextView>(R.id.datosEscalaDetalle).text =
                if (asignatura.tipoCalificacion.isBlank()) "No indicaste el tipo de calificación. ¿Entre qué valores van tus notas? Por ejemplo, de 1 a 5."
                else "“${asignatura.tipoCalificacion}” no indica un rango. ¿Entre qué valores van tus notas? Por ejemplo, de 1 a 5."
            asignatura.escalaMin?.let { minimo.setText(formatear(it)) }
            asignatura.escalaMax?.let { maximo.setText(formatear(it)) }
        }

        val interes = dialog.findViewById<ChipGroup>(R.id.datosInteres)
        interes.llenarOpciones(NivelesPercepcion.INTERES, interesActual?.let(NivelesPercepcion::indice))

        val metodos = CatalogoModelo.METODOS
        var metodo: String? = metodoActual?.takeIf { CatalogoModelo.esMetodo(it) }
        val campoMetodo = dialog.findViewById<AutoCompleteTextView>(R.id.datosMetodo)
        campoMetodo.setAdapter(ArrayAdapter(context, R.layout.item_menu_asignatura, metodos.map(CatalogoModelo::etiqueta)))
        metodo?.let { campoMetodo.setText(CatalogoModelo.etiqueta(it), false) }
        campoMetodo.setOnItemClickListener { _, _, posicion, _ -> metodo = metodos[posicion] }

        val error = dialog.findViewById<TextView>(R.id.datosError)
        val boton = dialog.findViewById<MaterialButton>(R.id.datosGuardar)
        boton.setOnClickListener {
            val min = if (sinRango) numero(minimo.text.toString()) else asignatura.escalaMin
            val max = if (sinRango) numero(maximo.text.toString()) else asignatura.escalaMax
            val valorInteres = interes.indiceMarcado()?.let(NivelesPercepcion::valor)
            error.visibility = View.GONE
            boton.mostrarCarga(true, "Guardar", "Guardando…")
            scope.launch {
                val resultado = guardar(area, min, max, valorInteres, metodo)
                boton.mostrarCarga(false, "Guardar", "")
                val mensaje = resultado.mensajeError()
                if (mensaje == null) {
                    dialog.dismiss()
                    onGuardado()
                } else {
                    error.text = mensaje
                    error.visibility = View.VISIBLE
                }
            }
        }
        dialog.findViewById<View>(R.id.datosAhoraNo).setOnClickListener { dialog.dismiss() }
        dialog.showAsBottomSheet()
    }

    private fun formatear(valor: Float) = if (valor % 1f == 0f) valor.toInt().toString() else valor.toString()
}

/** "¿Qué tan difícil fue?" con cinco respuestas; se puede omitir. */
class DificultadDialog(
    private val context: Context,
    private val titulo: String,
    private val detalle: String,
    private val onGuardar: (valor: Float) -> Unit,
    private val onOmitir: () -> Unit = {}
) {
    fun show() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.sheet_dificultad)
        dialog.findViewById<TextView>(R.id.dificultadTitulo).text = titulo
        dialog.findViewById<TextView>(R.id.dificultadDetalle).text = detalle

        val opciones = dialog.findViewById<ChipGroup>(R.id.dificultadOpciones)
        opciones.llenarOpciones(NivelesPercepcion.DIFICULTAD, null)
        val guardar = dialog.findViewById<MaterialButton>(R.id.dificultadGuardar)
        opciones.setOnCheckedStateChangeListener { grupo, _ -> guardar.isEnabled = grupo.indiceMarcado() != null }

        guardar.setOnClickListener {
            val indice = opciones.indiceMarcado() ?: return@setOnClickListener
            dialog.dismiss()
            onGuardar(NivelesPercepcion.valor(indice))
        }
        dialog.findViewById<View>(R.id.dificultadOmitir).setOnClickListener {
            dialog.dismiss()
            onOmitir()
        }
        dialog.showAsBottomSheet()
    }
}

/** Pinta una fila por parámetro: listo (verde) o pendiente (ámbar) con lo que falta y su acción. */
fun pintarDatos(contenedor: LinearLayout, estados: List<EstadoDato>, onAccion: (EstadoDato) -> Unit) {
    contenedor.removeAllViews()
    val inflater = LayoutInflater.from(contenedor.context)
    val contexto = contenedor.context
    for (estado in estados) {
        val fila = inflater.inflate(R.layout.item_dato_prediccion, contenedor, false)
        val icono = fila.findViewById<ImageView>(R.id.datoIcono)
        if (estado.listo) {
            icono.setImageResource(R.drawable.ic_gami_check)
            icono.imageTintList = android.content.res.ColorStateList.valueOf(contexto.getColor(R.color.estado_exito))
            icono.tintarFondo(R.color.estado_exito_suave)
        } else {
            icono.setImageResource(R.drawable.ic_clock)
            icono.imageTintList = android.content.res.ColorStateList.valueOf(contexto.getColor(R.color.estado_aviso))
            icono.tintarFondo(R.color.estado_aviso_suave)
        }
        fila.findViewById<TextView>(R.id.datoTitulo).text = estado.parametro.titulo
        fila.findViewById<TextView>(R.id.datoValor).apply {
            text = estado.valor
            visibility = if (estado.valor.isBlank()) View.GONE else View.VISIBLE
        }
        fila.findViewById<TextView>(R.id.datoDetalle).text = estado.detalle
        fila.contentDescription = "${estado.parametro.titulo}: ${if (estado.listo) "listo" else "pendiente"}. ${estado.valor} ${estado.detalle}"
        val accion = estado.accion
        fila.findViewById<TextView>(R.id.datoAccion).apply {
            visibility = if (accion != null && !estado.listo) View.VISIBLE else View.GONE
            text = accion?.texto
            setOnClickListener { onAccion(estado) }
        }
        contenedor.addView(fila)
    }
}

/** Panel de los datos de una asignatura para las predicciones. */
class ProgresoDatosDialog(
    private val context: Context,
    private val resultado: ResultadoExtraccion,
    private val onAccion: (EstadoDato) -> Unit
) {
    fun show() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.sheet_progreso_datos)
        dialog.findViewById<TextView>(R.id.progresoTitulo).text = resultado.asignatura.nombre
        dialog.findViewById<TextView>(R.id.progresoResumen).text =
            if (resultado.completo) "Todos los datos de la asignatura están listos"
            else "${resultado.listos} de ${resultado.total} datos listos para tus predicciones"
        dialog.findViewById<LinearProgressIndicator>(R.id.progresoBarra)
            .setProgressCompat(resultado.listos * 100 / resultado.total.coerceAtLeast(1), false)
        pintarDatos(dialog.findViewById(R.id.progresoDatos), resultado.estados) { estado ->
            dialog.dismiss()
            onAccion(estado)
        }
        dialog.findViewById<View>(R.id.progresoCerrar).setOnClickListener { dialog.dismiss() }
        dialog.showAsBottomSheet()
    }
}

/** Estudio hecho fuera del temporizador. */
class RegistroEstudioDialog(
    private val context: Context,
    private val asignaturas: List<Asignatura>,
    private val asignaturaInicial: Int?,
    private val scope: CoroutineScope,
    private val registrar: suspend (asignaturaId: Int, fecha: LocalDate, minutos: Int, metodo: String?) -> ResultadoGuardarDatos,
    private val onGuardado: () -> Unit = {}
) {
    fun show() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.sheet_registro_estudio)

        val conId = asignaturas.filter { it.id != null }
        var asignaturaId: Int? = asignaturaInicial?.takeIf { id -> conId.any { it.id == id } }
        val campoAsignatura = dialog.findViewById<AutoCompleteTextView>(R.id.registroAsignatura)
        campoAsignatura.setAdapter(ArrayAdapter(context, R.layout.item_menu_asignatura, conId.map { it.nombre }))
        conId.firstOrNull { it.id == asignaturaId }?.let { campoAsignatura.setText(it.nombre, false) }
        campoAsignatura.setOnItemClickListener { _, _, posicion, _ -> asignaturaId = conId[posicion].id }

        val fecha = dialog.findViewById<EditText>(R.id.registroFecha)
        fecha.setText(LocalDate.now().toString())
        fecha.setOnClickListener { fecha.showDatePicker() }

        val metodos = CatalogoModelo.METODOS
        var metodo: String? = null
        val campoMetodo = dialog.findViewById<AutoCompleteTextView>(R.id.registroMetodo)
        campoMetodo.setAdapter(ArrayAdapter(context, R.layout.item_menu_asignatura, metodos.map(CatalogoModelo::etiqueta)))
        campoMetodo.setOnItemClickListener { _, _, posicion, _ -> metodo = metodos[posicion] }

        val minutos = dialog.findViewById<EditText>(R.id.registroMinutos)
        val error = dialog.findViewById<TextView>(R.id.registroError)
        val boton = dialog.findViewById<MaterialButton>(R.id.registroGuardar)
        boton.setOnClickListener {
            val id = asignaturaId
            val dia = runCatching { LocalDate.parse(fecha.text.toString()) }.getOrNull()
            val cantidad = minutos.text.toString().trim().toIntOrNull()
            dialog.findViewById<TextInputLayout>(R.id.registroAsignaturaCaja).error = if (id == null) "Elige una asignatura" else null
            dialog.findViewById<TextInputLayout>(R.id.registroMinutosCaja).error =
                if (cantidad == null) "Indica cuántos minutos estudiaste" else null
            if (id == null || dia == null || cantidad == null) return@setOnClickListener

            error.visibility = View.GONE
            boton.mostrarCarga(true, "Guardar", "Guardando…")
            scope.launch {
                val resultado = registrar(id, dia, cantidad, metodo)
                boton.mostrarCarga(false, "Guardar", "")
                val mensaje = resultado.mensajeError()
                if (mensaje == null) {
                    dialog.dismiss()
                    onGuardado()
                } else {
                    error.text = mensaje
                    error.visibility = View.VISIBLE
                }
            }
        }
        dialog.findViewById<View>(R.id.registroCancelar).setOnClickListener { dialog.dismiss() }
        dialog.showAsBottomSheet()
    }
}
