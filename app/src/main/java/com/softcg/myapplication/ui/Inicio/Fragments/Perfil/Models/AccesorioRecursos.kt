package com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Models

import android.graphics.Color
import androidx.annotation.DrawableRes
import com.softcg.myapplication.R

/** Dibujos de cada accesorio del catálogo. Todas las capas comparten el lienzo 200x200 del avatar. */
object AccesorioRecursos {

    private val dibujos = mapOf(
        "fondo_estrellas" to R.drawable.acc_fondo_estrellas,
        "fondo_biblioteca" to R.drawable.acc_fondo_biblioteca,
        "fondo_atardecer" to R.drawable.acc_fondo_atardecer,
        "fondo_aurora" to R.drawable.acc_fondo_aurora,
        "fondo_laureles" to R.drawable.acc_fondo_laureles,
        "mochila_escolar" to R.drawable.acc_mochila_escolar,
        "libro" to R.drawable.acc_libro,
        "medalla" to R.drawable.acc_medalla,
        "gafas_redondas" to R.drawable.acc_gafas_redondas,
        "gafas_sol" to R.drawable.acc_gafas_sol,
        "gafas_doradas" to R.drawable.acc_gafas_doradas,
        "gorra" to R.drawable.acc_gorra,
        "gorro_lana" to R.drawable.acc_gorro_lana,
        "audifonos" to R.drawable.acc_audifonos,
        "birrete" to R.drawable.acc_birrete,
        "corona_laurel" to R.drawable.acc_corona_laurel
    )

    /** Partes que van delante del cuerpo en accesorios que se dibujan detrás (las correas de la mochila). */
    private val dibujosFrontales = mapOf(
        "mochila_escolar" to R.drawable.acc_mochila_escolar_frente
    )

    @DrawableRes
    fun dibujo(itemId: String): Int? = dibujos[itemId]

    @DrawableRes
    fun dibujoFrontal(itemId: String): Int? = dibujosFrontales[itemId]

    val TONOS_PIEL = listOf("#FFDBAC", "#F1C27D", "#E0AC69", "#C68642", "#8D5524").map(Color::parseColor)
    val TONOS_PELO = listOf("#2C1B12", "#4A2C17", "#A0522D", "#1B1B1B", "#D4A017").map(Color::parseColor)
}
