package com.softcg.myapplication.ui.Inicio.Dialogs

import android.util.Log
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import com.google.android.material.button.MaterialButton
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.mostrarCarga
import com.softcg.myapplication.core.ex.pintarEtiqueta
import com.softcg.myapplication.core.ex.showAsBottomSheet
import com.softcg.myapplication.domain.compartir.ContenidoCompartido
import com.softcg.myapplication.domain.ia.ContextoPrediccion
import com.softcg.myapplication.domain.ia.Fuente
import com.softcg.myapplication.domain.ia.MotivoSinPrediccion
import com.softcg.myapplication.domain.ia.RecomendacionTarea
import com.softcg.myapplication.domain.ia.RecomendarTareaUseCase
import com.softcg.myapplication.domain.ia.ResultadoRecomendacion
import com.softcg.myapplication.domain.ia.TipoFuente
import com.softcg.myapplication.domain.prediccion.CatalogoModelo
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * "Ver más" de una tarea: sus detalles y la recomendación integrada (método del modelo predictivo + consejo de
 * Gemini + bibliografía comprobada). Se pide con un botón (no al abrir) para no gastar cuota; si ya se generó
 * para esta tarea se muestra directamente.
 * [scope] es el del ciclo de vida de quien abre la hoja, para que la petición no siga viva sin pantalla.
 */
class TareaDetallesDialog(
    private val context: Context,
    private val tarea: Tarea,
    private val scope: CoroutineScope,
    private val recomendarTarea: RecomendarTareaUseCase,
    /** Abre la pantalla donde se completan los datos del modelo; null oculta el enlace. */
    private val onCompletarDatos: ((Tarea) -> Unit)? = null,
    /** Acciones de la tarea; null oculta el botón. La hoja se cierra antes de llamarlas. */
    private val onEntregar: ((Tarea) -> Unit)? = null,
    private val onCalificar: ((Tarea) -> Unit)? = null,
    private val onBorrar: ((Tarea) -> Unit)? = null
) {
    private val dialog = Dialog(context)
    private var peticion: Job? = null

    private val contenido by lazy { dialog.findViewById<View>(R.id.iaContenido) }
    private val intro by lazy { dialog.findViewById<TextView>(R.id.iaIntro) }
    private val error by lazy { dialog.findViewById<TextView>(R.id.iaError) }
    private val boton by lazy { dialog.findViewById<MaterialButton>(R.id.btnRecomendar) }

    fun show() {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.sheet_detalles_tarea)
        pintarDetalles()
        configurarAcciones()

        recomendarTarea.enCache(tarea)?.let { mostrarRecomendacion(it) }
        boton.setOnClickListener { pedirRecomendacion() }
        dialog.findViewById<View>(R.id.btnCerrar).setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener { peticion?.cancel() }
        dialog.showAsBottomSheet()
    }

    private fun pintarDetalles() {
        dialog.findViewById<TextView>(R.id.tareaEstado).pintarEstadoTarea(tarea)
        pintarInfoTarea(dialog.window!!.decorView, tarea)
    }

    /**
     * Entregar mientras no se haya entregado; después, calificar (la nota suele llegar más tarde). Una tarea
     * calificada ya está completa: solo queda borrarla. Compartir genera un QR con sus datos, sin su estado.
     */
    private fun configurarAcciones() {
        dialog.findViewById<View>(R.id.btnCompartirTarea).setOnClickListener {
            CompartirQrDialog(context, ContenidoCompartido.de(tarea)).show()
        }

        val principal = dialog.findViewById<MaterialButton>(R.id.btnEntregarTarea)
        val accion: ((Tarea) -> Unit)? = when {
            tarea.calificacion != null -> null
            tarea.entregadaMillis != null -> onCalificar?.also { principal.text = "Calificar" }
            else -> onEntregar?.also { principal.text = "Entregar" }
        }
        principal.visibility = if (accion != null) View.VISIBLE else View.GONE
        principal.setOnClickListener {
            dialog.dismiss()
            accion?.invoke(tarea)
        }

        val borrar = dialog.findViewById<MaterialButton>(R.id.btnBorrarTarea)
        borrar.visibility = if (onBorrar != null) View.VISIBLE else View.GONE
        borrar.setOnClickListener {
            dialog.dismiss()
            onBorrar?.invoke(tarea)
        }
        // Sin borrar, el botón principal ocupa toda la fila
        if (onBorrar == null) (principal.layoutParams as LinearLayout.LayoutParams).marginStart = 0
    }

    private fun pedirRecomendacion() {
        // Si ya hay una, el botón pide otra distinta (y vuelve a consultar el modelo con los datos de ahora)
        val nueva = contenido.visibility == View.VISIBLE
        error.visibility = View.GONE
        boton.mostrarCarga(true, textoBoton(), "Consultando el modelo y la IA…")
        peticion = scope.launch {
            val resultado = recomendarTarea(tarea, nueva)
            if (!dialog.isShowing) return@launch
            when (resultado) {
                is ResultadoRecomendacion.Exito -> mostrarRecomendacion(resultado.recomendacion)
                ResultadoRecomendacion.SinConexion -> mostrarError("No se pudo conectar con la IA. Revisa tu conexión a internet e inténtalo de nuevo.")
                ResultadoRecomendacion.Error -> mostrarError("No se pudieron generar las recomendaciones. Inténtalo de nuevo en unos minutos.")
            }
        }
    }

    private fun mostrarRecomendacion(recomendacion: RecomendacionTarea) {
        intro.visibility = View.GONE
        contenido.visibility = View.VISIBLE

        // Método: siempre el del modelo (o el elegido sin predicción), nunca leído del texto de la IA
        dialog.findViewById<TextView>(R.id.iaMetodo).text = CatalogoModelo.etiqueta(recomendacion.metodo)
        val origen = dialog.findViewById<TextView>(R.id.iaMetodoOrigen)
        val faltantes = dialog.findViewById<TextView>(R.id.iaFaltantes)
        val completar = dialog.findViewById<TextView>(R.id.iaCompletarDatos)
        when (val contexto = recomendacion.contexto) {
            is ContextoPrediccion.ConPrediccion -> {
                origen.pintarEtiqueta("MODELO PREDICTIVO · ${recomendacion.confianza} DE CONFIANZA", R.color.acento, R.color.marca_superficie)
                faltantes.visibility = View.GONE
                completar.visibility = View.GONE
            }
            is ContextoPrediccion.SinPrediccion -> {
                origen.pintarEtiqueta("SUGERENCIA DE LA IA · SIN PREDICCIÓN", R.color.estado_aviso, R.color.estado_aviso_suave)
                faltantes.text = when (contexto.motivo) {
                    MotivoSinPrediccion.FALTAN_DATOS ->
                        if (contexto.faltantes.isEmpty()) "Aún no hay datos suficientes para usar el modelo predictivo."
                        else "Para usar el modelo predictivo faltan: ${contexto.faltantes.joinToString(", ")}."
                    MotivoSinPrediccion.MODELO_NO_DISPONIBLE -> "El modelo predictivo no respondió. Inténtalo de nuevo más tarde."
                }
                faltantes.visibility = View.VISIBLE
                val puedeCompletar = contexto.motivo == MotivoSinPrediccion.FALTAN_DATOS && onCompletarDatos != null && tarea.id != null
                completar.visibility = if (puedeCompletar) View.VISIBLE else View.GONE
                completar.setOnClickListener {
                    dialog.dismiss()
                    onCompletarDatos?.invoke(tarea)
                }
            }
        }

        dialog.findViewById<TextView>(R.id.iaParrafo).mostrarSiHay(recomendacion.intro)
        dialog.findViewById<TextView>(R.id.iaConsejo).mostrarSiHay(recomendacion.consejo)

        val pasos = recomendacion.comoAplicar.mapIndexed { i, paso -> "${i + 1}. $paso" }.joinToString("\n")
        dialog.findViewById<TextView>(R.id.iaPasos).mostrarSiHay(pasos, R.id.labelPasos)
        dialog.findViewById<TextView>(R.id.iaTiempo).mostrarSiHay(recomendacion.tiempoEstimado)
        val visibilidadTiempo = if (recomendacion.tiempoEstimado.isBlank()) View.GONE else View.VISIBLE
        dialog.findViewById<View>(R.id.containerTiempo).visibility = visibilidadTiempo
        dialog.findViewById<View>(R.id.labelTiempo).visibility = visibilidadTiempo

        pintarFuentes(recomendacion.fuentes)
        boton.mostrarCarga(false, textoBoton(), "")
    }

    private fun pintarFuentes(fuentes: List<Fuente>) {
        val lista = dialog.findViewById<LinearLayout>(R.id.iaFuentes)
        lista.removeAllViews()
        val inflater = LayoutInflater.from(context)
        fuentes.forEach { fuente ->
            val fila = inflater.inflate(R.layout.item_fuente, lista, false)
            fila.findViewById<ImageView>(R.id.fuenteIcono).setImageResource(
                if (fuente.tipo == TipoFuente.LIBRO) R.drawable.ic_fuente_libro else R.drawable.ic_fuente_web
            )
            fila.findViewById<TextView>(R.id.fuenteTitulo).text = fuente.titulo
            fila.findViewById<TextView>(R.id.fuenteDetalle).text = fuente.detalle
            fila.contentDescription = "${fuente.titulo}, ${fuente.detalle}. Abrir enlace"
            fila.setOnClickListener { abrir(fuente.url) }
            lista.addView(fila)
        }
        dialog.findViewById<View>(R.id.iaSinFuentes).visibility = if (fuentes.isEmpty()) View.VISIBLE else View.GONE
        // Una búsqueda siempre es un enlace real: complementa la lista, especialmente si quedó vacía
        dialog.findViewById<View>(R.id.iaBuscarMas).setOnClickListener {
            abrir("https://scholar.google.com/scholar?hl=es&q=" + Uri.encode(tarea.titulo.trim()))
        }
    }

    private fun abrir(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (e: ActivityNotFoundException) {
            Log.w("TareaDetalles", "Sin navegador para $url", e)
            Toast.makeText(context, "No hay un navegador para abrir el enlace", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarError(mensaje: String) {
        error.text = mensaje
        error.visibility = View.VISIBLE
        boton.mostrarCarga(false, textoBoton(), "")
    }

    private fun textoBoton() =
        if (contenido.visibility == View.VISIBLE) "Generar otras recomendaciones" else "Generar recomendaciones"

    private fun TextView.mostrarSiHay(valor: String, rotulo: Int? = null) {
        text = valor
        visibility = if (valor.isBlank()) View.GONE else View.VISIBLE
        rotulo?.let { dialog.findViewById<View>(it).visibility = visibility }
    }
}
