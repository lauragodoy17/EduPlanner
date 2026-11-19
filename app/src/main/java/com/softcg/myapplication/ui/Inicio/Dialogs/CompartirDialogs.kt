package com.softcg.myapplication.ui.Inicio.Dialogs

import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textfield.TextInputLayout
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.pintarEtiqueta
import com.softcg.myapplication.core.ex.showAsBottomSheet
import com.softcg.myapplication.core.qr.GeneradorQr
import com.softcg.myapplication.domain.compartir.AnalisisImportacion
import com.softcg.myapplication.domain.compartir.CodigoQr
import com.softcg.myapplication.domain.compartir.ContenidoCompartido
import com.softcg.myapplication.domain.compartir.DestinoAsignatura
import com.softcg.myapplication.domain.compartir.TextosCompartir
import kotlin.math.roundToInt

/**
 * El QR de una tarea, evento, asignatura o nota. Sube el brillo mientras se muestra: con la pantalla tenue
 * a la cámara del otro teléfono le cuesta leerlo.
 */
class CompartirQrDialog(
    private val context: Context,
    private val contenido: ContenidoCompartido
) {
    fun show() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.sheet_compartir_qr)

        val tipo = contenido.tipo
        dialog.findViewById<TextView>(R.id.compartirTipo)
            .pintarEtiqueta(TextosCompartir.accion("Compartir", tipo).uppercase(), R.color.acento, R.color.marca_superficie)
        dialog.findViewById<TextView>(R.id.compartirTitulo).text = TextosCompartir.titulo(contenido)
        dialog.findViewById<TextView>(R.id.compartirResumen).apply {
            val resumen = TextosCompartir.detalles(contenido).take(2).joinToString(" · ") { it.second }
            text = resumen
            visibility = if (resumen.isBlank()) View.GONE else View.VISIBLE
        }

        val lado = (248 * context.resources.displayMetrics.density).roundToInt()
        // Un contenido demasiado largo no cabe en un QR (ZXing lanza): se avisa en vez de cerrar la app
        val qr = runCatching { GeneradorQr.generar(CodigoQr.codificar(contenido), lado, context.getColor(R.color.degradado_inicio)) }
            .onFailure { Log.w("CompartirQr", "No se pudo generar el QR", it) }
            .getOrNull()
        if (qr == null) {
            Toast.makeText(context, "No se pudo crear el QR: el contenido es demasiado largo. Acorta la descripción.", Toast.LENGTH_LONG).show()
            return
        }
        dialog.findViewById<ImageView>(R.id.compartirQr).apply {
            setImageBitmap(qr)
            contentDescription = "Código QR de ${tipo.articulo} ${tipo.nombre} ${TextosCompartir.titulo(contenido)}"
        }
        dialog.findViewById<View>(R.id.compartirListo).setOnClickListener { dialog.dismiss() }

        dialog.showAsBottomSheet()
        dialog.window?.let { ventana ->
            ventana.attributes = ventana.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL }
        }
    }
}

/**
 * Lo que llegó por QR, antes de agregarlo. Si es una tarea o nota cuya asignatura no existe en este teléfono,
 * el estudiante elige crearla (con el nombre que trae o uno corregido) o asignarla a una suya.
 * [onAgregar] recibe el destino elegido; null si no hizo falta elegir.
 */
class ImportarCompartidoDialog(
    private val context: Context,
    private val analisis: AnalisisImportacion,
    private val onAgregar: (DestinoAsignatura?) -> Unit
) {
    fun show() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.sheet_importar_compartido)

        val contenido = analisis.contenido
        val tipo = contenido.tipo
        dialog.findViewById<TextView>(R.id.importarTipo).pintarEtiqueta(
            "TE COMPARTIERON ${if (tipo.articulo == "el") "UN" else "UNA"} ${tipo.nombre.uppercase()}", R.color.acento, R.color.marca_superficie
        )
        dialog.findViewById<TextView>(R.id.importarTitulo).text = TextosCompartir.titulo(contenido)
        pintarDetalles(dialog.findViewById(R.id.importarDetalles), TextosCompartir.detalles(contenido))

        val agregar = dialog.findViewById<MaterialButton>(R.id.importarAgregar)
        val aviso = dialog.findViewById<TextView>(R.id.importarAviso)
        agregar.text = TextosCompartir.accion("Agregar", tipo)
        dialog.findViewById<View>(R.id.importarCancelar).setOnClickListener { dialog.dismiss() }

        when {
            analisis.asignaturaRepetida -> {
                aviso.mostrar("Ya tienes una asignatura llamada «${TextosCompartir.titulo(contenido)}». No se agregará otra igual.")
                agregar.text = "Entendido"
                agregar.setOnClickListener { dialog.dismiss() }
            }
            analisis.faltaAsignatura -> configurarDestino(dialog, agregar)
            else -> {
                analisis.asignaturaLocal?.let { aviso.mostrar("Se agregará a tu asignatura «$it»") }
                agregar.setOnClickListener {
                    dialog.dismiss()
                    onAgregar(null)
                }
            }
        }
        dialog.showAsBottomSheet()
    }

    private fun configurarDestino(dialog: Dialog, agregar: MaterialButton) {
        val compartida = analisis.contenido.asignatura.orEmpty()
        val locales = analisis.asignaturasLocales
        dialog.findViewById<View>(R.id.importarAsignatura).visibility = View.VISIBLE
        dialog.findViewById<TextView>(R.id.importarAsignaturaPregunta).text =
            if (compartida.isBlank()) "No tiene asignatura. ¿A cuál la agregas?"
            else "No tienes la asignatura «$compartida». ¿Qué quieres hacer?"

        val opciones = dialog.findViewById<RadioGroup>(R.id.importarOpciones)
        val crear = dialog.findViewById<MaterialRadioButton>(R.id.importarCrear)
        val usarExistente = dialog.findViewById<MaterialRadioButton>(R.id.importarUsarExistente)
        crear.text = if (compartida.isBlank()) "Crear una asignatura nueva" else "Crear la asignatura «$compartida»"
        usarExistente.visibility = if (locales.isEmpty()) View.GONE else View.VISIBLE

        val cajaNombre = dialog.findViewById<TextInputLayout>(R.id.importarNombreCaja)
        val nombre = dialog.findViewById<EditText>(R.id.importarNombre)
        nombre.setText(compartida)
        val cajaExistente = dialog.findViewById<TextInputLayout>(R.id.importarExistenteCaja)
        val existente = dialog.findViewById<AutoCompleteTextView>(R.id.importarExistente)
        existente.setAdapter(ArrayAdapter(context, R.layout.item_menu_asignatura, locales))
        existente.setOnItemClickListener { _, _, _, _ -> cajaExistente.error = null }

        opciones.setOnCheckedChangeListener { _, id ->
            val usandoExistente = id == R.id.importarUsarExistente
            cajaNombre.visibility = if (usandoExistente) View.GONE else View.VISIBLE
            cajaExistente.visibility = if (usandoExistente) View.VISIBLE else View.GONE
        }
        // Sin nombre que crear y con asignaturas propias, lo más probable es asignarla a una de ellas
        opciones.check(if (compartida.isBlank() && locales.isNotEmpty()) R.id.importarUsarExistente else R.id.importarCrear)

        agregar.setOnClickListener {
            val destino = if (opciones.checkedRadioButtonId == R.id.importarUsarExistente) {
                existente.text.toString().takeIf { it in locales }?.let { DestinoAsignatura.Existente(it) }
                    .also { cajaExistente.error = if (it == null) "Elige una de tus asignaturas" else null }
            } else {
                nombre.text.toString().trim().takeIf { it.isNotEmpty() }?.let { DestinoAsignatura.Nueva(it) }
                    .also { cajaNombre.error = if (it == null) "Escribe el nombre de la asignatura" else null }
            } ?: return@setOnClickListener
            dialog.dismiss()
            onAgregar(destino)
        }
    }

    private fun pintarDetalles(contenedor: LinearLayout, filas: List<Pair<String, String>>) {
        contenedor.visibility = if (filas.isEmpty()) View.GONE else View.VISIBLE
        val inflater = LayoutInflater.from(context)
        filas.forEach { (rotulo, valor) ->
            val fila = inflater.inflate(R.layout.item_dato_compartido, contenedor, false)
            fila.findViewById<TextView>(R.id.datoCompartidoRotulo).text = rotulo
            fila.findViewById<TextView>(R.id.datoCompartidoValor).text = valor
            contenedor.addView(fila)
        }
    }

    private fun TextView.mostrar(texto: String) {
        text = texto
        visibility = View.VISIBLE
    }
}
