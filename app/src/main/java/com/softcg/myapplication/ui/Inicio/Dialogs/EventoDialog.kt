package com.softcg.myapplication.ui.Inicio.Dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
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

        private val COLOR_SELECCIONADO = Color.parseColor("#E1BEE7")
        private val COLORES_PRIORIDAD = mapOf(
            PRIORIDAD_ALTA to Color.parseColor("#FFF5F5"),
            PRIORIDAD_MEDIA to Color.parseColor("#FFFEF5"),
            PRIORIDAD_BAJA to Color.parseColor("#F0F8FF")
        )
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
            PRIORIDAD_ALTA to dialog.findViewById<CardView>(R.id.prioridadAlta),
            PRIORIDAD_MEDIA to dialog.findViewById<CardView>(R.id.prioridadMedia),
            PRIORIDAD_BAJA to dialog.findViewById<CardView>(R.id.prioridadBaja)
        )

        imagePreview = dialog.findViewById(R.id.imagePreview)
        imagePreviewCard = dialog.findViewById(R.id.imagePreviewCard)
        imageNameText = dialog.findViewById(R.id.imageNameText)
        imagePreviewCard.visibility = View.GONE

        evento?.let { cargarEvento(it, titulo, descripcion, fecha, horaInicio, horaFin, guardarBoton) }

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
                Toast.makeText(context, "Complete los campos obligatorios", Toast.LENGTH_SHORT).show()
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
        guardarBoton.text = "Actualizar Evento"

        if (!evento.imagenUri.isNullOrEmpty()) {
            try {
                mostrarImagen(Uri.parse(evento.imagenUri), "Imagen cargada")
            } catch (e: Exception) {
                selectedImageUri = null
                imagePreviewCard.visibility = View.GONE
            }
        }
    }

    private fun pintarPrioridad(tarjetas: Map<Int, CardView>) {
        tarjetas.forEach { (prioridad, tarjeta) ->
            tarjeta.setCardBackgroundColor(
                if (prioridad == selectedPrioridad) COLOR_SELECCIONADO else COLORES_PRIORIDAD.getValue(prioridad)
            )
        }
    }
}
