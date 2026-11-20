package com.softcg.myapplication.ui.Inicio.Dialogs

import android.app.Dialog
import java.time.LocalDate
import com.softcg.myapplication.domain.TextoUsuario
import android.content.Context
import android.net.Uri
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.net.toUri
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.showAsBottomSheet
import com.softcg.myapplication.core.ex.showDatePicker
import com.softcg.myapplication.core.ex.showTimePicker
import com.softcg.myapplication.ui.Inicio.Models.Evento
import java.time.LocalTime

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

        fecha.setOnClickListener { fecha.showDatePicker(maxima = LocalDate.now().plusYears(TareaDialog.MAX_ANIOS_ADELANTE)) }
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

        val nombreContainer = dialog.findViewById<TextInputLayout>(R.id.NombreContainer)
        val fechaContainer = dialog.findViewById<TextInputLayout>(R.id.FechaContainer)
        val horaFinContainer = dialog.findViewById<TextInputLayout>(R.id.HoraFinContainer)
        titulo.doAfterTextChanged { if (!it.isNullOrBlank()) nombreContainer.error = null }
        fecha.doAfterTextChanged { fechaContainer.error = null }
        horaInicio.doAfterTextChanged { horaFinContainer.error = null }
        horaFin.doAfterTextChanged { horaFinContainer.error = null }

        guardarBoton.setOnClickListener {
            val textoTitulo = TextoUsuario.linea(titulo.text.toString(), TextoUsuario.MAX_TITULO)
            val sinTitulo = textoTitulo.isEmpty()
            val errorFecha = TareaDialog.errorFecha(fecha.text.toString())
            val errorHoras = errorHoras(horaInicio.text.toString(), horaFin.text.toString())
            nombreContainer.error = if (sinTitulo) "Escribe un nombre" else null
            fechaContainer.error = errorFecha
            horaFinContainer.error = errorHoras
            if (sinTitulo || errorFecha != null || errorHoras != null) return@setOnClickListener
            dialog.dismiss()
            onGuardar(
                Evento(
                    evento?.id,
                    textoTitulo,
                    TextoUsuario.parrafo(descripcion.text.toString(), TextoUsuario.MAX_DESCRIPCION),
                    fecha.text.toString(),
                    selectedPrioridad,
                    horaInicio.text.toString(),
                    horaFin.text.toString(),
                    selectedImageUri?.toString()
                )
            )
        }

        dialog.showAsBottomSheet()
    }

    /** La hora de fin necesita la de inicio y tiene que ser posterior; null si están bien (o vacías). */
    private fun errorHoras(inicio: String, fin: String): String? {
        if (fin.isBlank()) return null
        val desde = runCatching { LocalTime.parse(inicio) }.getOrNull() ?: return "Elige primero la hora de inicio"
        val hasta = runCatching { LocalTime.parse(fin) }.getOrNull() ?: return "Hora no válida"
        return if (hasta.isAfter(desde)) null else "Debe ser después de la hora de inicio"
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
