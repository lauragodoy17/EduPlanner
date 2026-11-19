package com.softcg.myapplication.ui.Inicio.Dialogs

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.pintarEtiqueta
import com.softcg.myapplication.domain.ia.ContextoPrediccion
import com.softcg.myapplication.domain.ia.Fuente
import com.softcg.myapplication.domain.ia.MotivoSinPrediccion
import com.softcg.myapplication.domain.ia.RecomendacionTarea
import com.softcg.myapplication.domain.ia.TipoFuente
import com.softcg.myapplication.domain.prediccion.CatalogoModelo

/**
 * Pinta una recomendación en `include_recomendacion_ia.xml` (pantalla de la tarea).
 * [raiz] es cualquier vista que contenga el include. [onCompletarDatos] null oculta el enlace "Completar mis datos".
 */
fun pintarRecomendacion(
    raiz: View,
    recomendacion: RecomendacionTarea,
    temaTarea: String,
    onCompletarDatos: (() -> Unit)? = null
) {
    raiz.findViewById<View>(R.id.iaContenido).visibility = View.VISIBLE

    // : siempre el del modelo (o el elegido sin predicción), nunca leído del texto de la IA
    raiz.findViewById<TextView>(R.id.iaMetodo).text = CatalogoModelo.etiqueta(recomendacion.metodo)
    val origen = raiz.findViewById<TextView>(R.id.iaMetodoOrigen)
    val faltantes = raiz.findViewById<TextView>(R.id.iaFaltantes)
    val completar = raiz.findViewById<TextView>(R.id.iaCompletarDatos)
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
            val puedeCompletar = contexto.motivo == MotivoSinPrediccion.FALTAN_DATOS && onCompletarDatos != null
            completar.visibility = if (puedeCompletar) View.VISIBLE else View.GONE
            completar.setOnClickListener { onCompletarDatos?.invoke() }
        }
    }

    raiz.findViewById<TextView>(R.id.iaParrafo).mostrarSiHay(recomendacion.intro)
    raiz.findViewById<TextView>(R.id.iaConsejo).mostrarSiHay(recomendacion.consejo)

    val pasos = recomendacion.comoAplicar.mapIndexed { i, paso -> "${i + 1}. $paso" }.joinToString("\n")
    raiz.findViewById<TextView>(R.id.iaPasos).mostrarSiHay(pasos, raiz.findViewById(R.id.labelPasos))
    raiz.findViewById<TextView>(R.id.iaTiempo).mostrarSiHay(recomendacion.tiempoEstimado)
    val visibilidadTiempo = if (recomendacion.tiempoEstimado.isBlank()) View.GONE else View.VISIBLE
    raiz.findViewById<View>(R.id.containerTiempo).visibility = visibilidadTiempo
    raiz.findViewById<View>(R.id.labelTiempo).visibility = visibilidadTiempo

    pintarFuentes(raiz, recomendacion.fuentes, temaTarea)
}

private fun pintarFuentes(raiz: View, fuentes: List<Fuente>, temaTarea: String) {
    val context = raiz.context
    val lista = raiz.findViewById<LinearLayout>(R.id.iaFuentes)
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
        fila.setOnClickListener { context.abrirEnlace(fuente.url) }
        lista.addView(fila)
    }
    raiz.findViewById<View>(R.id.iaSinFuentes).visibility = if (fuentes.isEmpty()) View.VISIBLE else View.GONE
    // Una búsqueda siempre es un enlace real: complementa la lista, especialmente si quedó vacía
    raiz.findViewById<View>(R.id.iaBuscarMas).setOnClickListener {
        context.abrirEnlace("https://scholar.google.com/scholar?hl=es&q=" + Uri.encode(temaTarea.trim()))
    }
}

private fun Context.abrirEnlace(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (e: ActivityNotFoundException) {
        Log.w("RecomendacionIa", "Sin navegador para $url", e)
        Toast.makeText(this, "No hay un navegador para abrir el enlace", Toast.LENGTH_SHORT).show()
    }
}

private fun TextView.mostrarSiHay(valor: String, rotulo: View? = null) {
    text = valor
    visibility = if (valor.isBlank()) View.GONE else View.VISIBLE
    rotulo?.visibility = visibility
}
