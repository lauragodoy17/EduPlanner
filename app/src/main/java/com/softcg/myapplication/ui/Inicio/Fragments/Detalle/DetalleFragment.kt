package com.softcg.myapplication.ui.Inicio.Fragments.Detalle

import androidx.core.net.toUri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.confirmar
import com.softcg.myapplication.core.ex.copiarImagenEvento
import com.softcg.myapplication.core.ex.mostrarAviso
import com.softcg.myapplication.core.ex.mostrarMensaje
import com.softcg.myapplication.core.ex.pintarEtiqueta
import com.softcg.myapplication.domain.NuevaAsignatura
import com.softcg.myapplication.domain.compartir.ContenidoCompartido
import com.softcg.myapplication.ui.Inicio.Dialogs.AsignaturaDialog
import com.softcg.myapplication.ui.Inicio.Dialogs.CalificacionDialog
import com.softcg.myapplication.ui.Inicio.Dialogs.CompartirQrDialog
import com.softcg.myapplication.ui.Inicio.Dialogs.EventoDialog
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models.Calificacion
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Pantalla de un evento, asignatura o calificación (desde su tarjeta): toda su información y, arriba a la
 * derecha, editar, borrar y compartir por QR. Las tareas tienen la suya (PrediccionFragment), con los mismos iconos.
 */
@AndroidEntryPoint
class DetalleFragment : Fragment() {

    private val viewModel: DetalleViewModel by viewModels()

    private val tipo: TipoDetalle
        get() = TipoDetalle.deClave(requireArguments().getString(ARG_TIPO)) ?: TipoDetalle.EVENTO

    private var eventoDialog: EventoDialog? = null
    private val seleccionarImagen = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        // Se guarda una copia reducida: la Uri del selector deja de valer al cerrar la app
        uri ?: return@registerForActivityResult
        viewLifecycleOwner.lifecycleScope.launch {
            val copia = requireContext().copiarImagenEvento(uri)
            if (copia != null) eventoDialog?.mostrarImagen(copia, "Imagen seleccionada")
            else view?.mostrarAviso("No se pudo abrir esa imagen. Prueba con otra.", esError = true)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_detalle, container, false)
        viewModel.cargar(tipo, requireArguments().getInt(ARG_ID, -1))

        viewModel.elemento.observe(viewLifecycleOwner) { pintar(view, it) }
        viewModel.mensaje.observe(viewLifecycleOwner) { it.getContentIfNotHandled()?.let { texto -> view.mostrarAviso(texto) } }
        viewModel.borrado.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { texto ->
                // Esta pantalla se cierra: el aviso queda sobre la anterior
                requireActivity().mostrarMensaje(texto)
                findNavController().popBackStack()
            }
        }
        configurarAccionesDetalle(
            view.findViewById(R.id.detalleAcciones),
            onEditar = ::editar,
            onBorrar = ::confirmarBorrar,
            onCompartir = ::compartir
        )
        return view
    }

    private fun pintar(view: View, elemento: ElementoDetalle) {
        val tarjeta = view.findViewById<View>(R.id.detalleTarjeta)
        val noExiste = elemento == ElementoDetalle.NoExiste
        tarjeta.visibility = if (noExiste) View.GONE else View.VISIBLE
        view.findViewById<View>(R.id.detalleNoExiste).visibility = if (noExiste) View.VISIBLE else View.GONE
        val ficha = when (elemento) {
            is ElementoDetalle.DeEvento -> FichaDetalle.de(elemento.evento, LocalDate.now())
            is ElementoDetalle.DeAsignatura -> FichaDetalle.de(elemento.asignatura)
            is ElementoDetalle.DeNota -> FichaDetalle.de(elemento.calificacion)
            ElementoDetalle.NoExiste -> return
        }

        val (color, fondo) = when (ficha.tipo) {
            TipoDetalle.EVENTO -> R.color.evento to R.color.evento_suave
            TipoDetalle.ASIGNATURA -> R.color.acento to R.color.marca_superficie
            TipoDetalle.NOTA -> R.color.estado_exito to R.color.estado_exito_suave
        }
        view.findViewById<TextView>(R.id.detalleEtiqueta).pintarEtiqueta(ficha.tipo.etiqueta, color, fondo)
        view.findViewById<TextView>(R.id.detalleTitulo).text = ficha.titulo

        val descripcion = view.findViewById<TextView>(R.id.detalleDescripcion)
        descripcion.text = ficha.descripcion
        descripcion.visibility = if (ficha.descripcion.isNotBlank()) View.VISIBLE else View.GONE
        view.findViewById<View>(R.id.detalleLabelDescripcion).visibility = descripcion.visibility

        val imagen = view.findViewById<ImageView>(R.id.detalleImagen)
        val conImagen = ficha.imagenUri?.let { uri -> runCatching { imagen.setImageURI(uri.toUri()) }.isSuccess && imagen.drawable != null } ?: false
        imagen.visibility = if (conImagen) View.VISIBLE else View.GONE

        val filas = view.findViewById<LinearLayout>(R.id.detalleFilas)
        filas.removeAllViews()
        ficha.filas.forEach { (rotulo, valor) ->
            val fila = layoutInflater.inflate(R.layout.item_info_detalle, filas, false)
            fila.findViewById<TextView>(R.id.infoRotulo).text = rotulo
            fila.findViewById<TextView>(R.id.infoValor).text = valor
            filas.addView(fila)
        }
        filas.visibility = if (ficha.filas.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun editar() {
        when (val actual = viewModel.elemento.value) {
            is ElementoDetalle.DeEvento -> {
                eventoDialog = EventoDialog(
                    context = requireContext(),
                    evento = actual.evento,
                    onSeleccionarImagen = { seleccionarImagen.launch("image/*") },
                    onGuardar = { viewModel.actualizarEvento(it) }
                ).also { it.show() }
            }
            is ElementoDetalle.DeAsignatura -> viewLifecycleOwner.lifecycleScope.launch {
                // Con notas registradas la escala se muestra fija
                val bloqueada = viewModel.escalaBloqueada(actual.asignatura)
                AsignaturaDialog(
                    context = requireContext(),
                    asignatura = actual.asignatura,
                    escalaBloqueada = bloqueada,
                    otrosNombres = viewModel.asignaturasCompletas().filter { it.id != actual.asignatura.id }.map { it.nombre },
                    onGuardar = { datos: NuevaAsignatura -> viewModel.actualizarAsignatura(datos) }
                ).show()
            }
            is ElementoDetalle.DeNota -> CalificacionDialog(
                requireContext(), viewModel.asignaturasCompletas(), actual.calificacion,
                onEscalaNueva = { asignaturaId, escala -> viewModel.guardarEscala(asignaturaId, escala) }
            ) { tipoNota, valor, asignatura, descripcion ->
                viewModel.actualizarCalificacion(
                    Calificacion(null, tipoNota, valor, asignatura, descripcion, tareaId = actual.calificacion.tareaId)
                )
            }.show()
            else -> Unit
        }
    }

    private fun confirmarBorrar() {
        if (viewModel.elemento.value == ElementoDetalle.NoExiste) return
        requireContext().confirmar(
            "¿Eliminar ${tipo.articulo}?",
            "Esta acción no se puede deshacer.",
            "Eliminar"
        ) { viewModel.borrar() }
    }

    private fun compartir() {
        val contenido = when (val actual = viewModel.elemento.value) {
            is ElementoDetalle.DeEvento -> ContenidoCompartido.de(actual.evento)
            is ElementoDetalle.DeAsignatura -> ContenidoCompartido.de(actual.asignatura)
            is ElementoDetalle.DeNota -> ContenidoCompartido.de(actual.calificacion)
            else -> return
        }
        CompartirQrDialog(requireContext(), contenido).show()
    }

    companion object {
        const val ARG_TIPO = "tipo"
        const val ARG_ID = "id"
    }
}
