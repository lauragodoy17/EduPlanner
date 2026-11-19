package com.softcg.myapplication.ui.Inicio.Dialogs

import android.app.Dialog
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.showAsBottomSheet
import com.softcg.myapplication.core.ex.showDatePicker
import com.softcg.myapplication.core.ex.showTimePicker
import com.softcg.myapplication.ui.Inicio.Models.Evento

/**
 * Hoja para crear un evento o, si se pasa [evento], editarlo.
 * La selección de imagen la lanza quien abre el diálogo (necesita un ActivityResultLauncher)
 * y devuelve el resultado con [mostrarImagen].
 */
class EventoDialog(
    private val context: Context,
    private val evento: Evento? = null,
    private val onSeleccionarImagen: () -> Unit,
    private val onGuardar: (Evento) -> Unit
) {

    companion object {
        const val PRIORIDAD_ALTA = 1
        const val PRIORIDAD_MEDIA = 2
        const val PRIORIDAD_BAJA = 3
    }

    private var selectedPrioridad = evento?.prioridad ?: PRIORIDAD_MEDIA
    private var selectedImageUri: Uri? = null

    private lateinit var imagePreview: ImageView
    private lateinit var imagePreviewCard: CardView
    private lateinit var imageNameText: TextView

    fun show() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.sheet_agregar_evento)

        val titulo = dialog.findViewById<EditText>(R.id.NombreEditText)
        val descripcion = dialog.findViewById<EditText>(R.id.DescripcionEditTextevent)
        val fecha = dialog.findViewById<EditText>(R.id.FechaEditTextevent)
        val horaInicio = dialog.findViewById<EditText>(R.id.HoraInicioEditText)
        val horaFin = dialog.findViewById<EditText>(R.id.HoraFinEditText)
        val guardarBoton = dialog.findViewById<Button>(R.id.botonAgregar)

        val tarjetasPrioridad = mapOf(
            PRIORIDAD_ALTA to dialog.findViewById<MaterialCardView>(R.id.prioridadAlta),
            PRIORIDAD_MEDIA to dialog.findViewById<MaterialCardView>(R.id.prioridadMedia),
            PRIORIDAD_BAJA to dialog.findViewById<MaterialCardView>(R.id.prioridadBaja)
        )

        imagePreview = dialog.findViewById(R.id.imagePreview)
        imagePreviewCard = dialog.findViewById(R.id.imagePreviewCard)
        imageNameText = dialog.findViewById(R.id.imageNameText)
        imagePreviewCard.visibility = View.GONE

        evento?.let {
            dialog.findViewById<TextView>(R.id.formTitulo).text = "Editar evento"
            cargarEvento(it, titulo, descripcion, fecha, horaInicio, horaFin, guardarBoton)
        }

        fecha.setOnClickListener { fecha.showDatePicker() }
        horaInicio.setOnClickListener { horaInicio.showTimePicker() }
        horaFin.setOnClickListener { horaFin.showTimePicker() }

        tarjetasPrioridad.forEach { (prioridad, tarjeta) ->
            tarjeta.setOnClickListener {
                selectedPrioridad = prioridad
                pintarPrioridad(tarjetasPrioridad)
            }
        }
        pintarPrioridad(tarjetasPrioridad)

        dialog.findViewById<CardView>(R.id.imageSelectorCard).setOnClickListener {
            onSeleccionarImagen()
        }

        guardarBoton.setOnClickListener {
            if (titulo.text.toString().isNotEmpty() && fecha.text.toString().isNotEmpty()) {
                dialog.dismiss()
                onGuardar(
                    Evento(
                        evento?.id,
                        titulo.text.toString(),
                        descripcion.text.toString(),
                        fecha.text.toString(),
                        selectedPrioridad,
                        horaInicio.text.toString(),
                        horaFin.text.toString(),
                        selectedImageUri?.toString()
                    )
                )
            } else {
                dialog.findViewById<TextInputLayout>(R.id.NombreContainer).error =
                    if (titulo.text.isNullOrEmpty()) "Escribe un nombre" else null
                dialog.findViewById<TextInputLayout>(R.id.FechaContainer).error =
                    if (fecha.text.isNullOrEmpty()) "Elige una fecha" else null
            }
        }

        dialog.showAsBottomSheet()
    }

    fun mostrarImagen(uri: Uri, nombre: String = uri.lastPathSegment ?: "Imagen seleccionada") {
        selectedImageUri = uri
        imagePreview.setImageURI(uri)
        imagePreviewCard.visibility = View.VISIBLE
        imageNameText.text = nombre
    }

    private fun cargarEvento(
        evento: Evento,
        titulo: EditText,
        descripcion: EditText,
        fecha: EditText,
        horaInicio: EditText,
        horaFin: EditText,
        guardarBoton: Button
    ) {
        titulo.setText(evento.titulo)
        descripcion.setText(evento.descrip)
        fecha.setText(evento.fecha)
        horaInicio.setText(evento.horaInicio)
        horaFin.setText(evento.horaFin)
        guardarBoton.text = "Guardar cambios"

        // Una imagen que ya no existe (se borró del teléfono) simplemente no se muestra
        evento.imagenUri?.takeIf { it.isNotEmpty() }?.let { guardada ->
            runCatching { mostrarImagen(guardada.toUri(), "Imagen cargada") }.onFailure {
                selectedImageUri = null
                imagePreviewCard.visibility = View.GONE
            }
        }
    }

    private fun pintarPrioridad(tarjetas: Map<Int, MaterialCardView>) {
        tarjetas.forEach { (prioridad, tarjeta) ->
            val elegida = prioridad == selectedPrioridad
            tarjeta.setCardBackgroundColor(context.getColor(if (elegida) R.color.marca_superficie else R.color.superficie))
            tarjeta.strokeColor = context.getColor(if (elegida) R.color.acento else R.color.borde_suave)
            tarjeta.isSelected = elegida
        }
    }
}
