package com.softcg.myapplication.ui.tema

import android.app.Activity
import androidx.annotation.StyleRes
import androidx.core.view.WindowCompat
import com.softcg.myapplication.R
import com.softcg.myapplication.data.preferencias.PreferenciasTema
import com.softcg.myapplication.domain.preferencias.TemaApp

/** El estilo de values/themes.xml de cada tema. */
@get:StyleRes
val TemaApp.estilo: Int
    get() = when (this) {
        TemaApp.CLARO -> R.style.Tema_EduPlanner_Claro
        TemaApp.OSCURO -> R.style.Tema_EduPlanner_Oscuro
        TemaApp.AZUL -> R.style.Tema_EduPlanner_Azul
        TemaApp.CLASICO -> R.style.Tema_EduPlanner_Clasico
    }

/**
 * Aplica el tema elegido. Va antes de super.onCreate(): la ventana y las vistas toman sus colores del tema
 * que haya en ese momento. Devuelve el tema para ajustar lo que depende de él (iconos de las barras del sistema).
 */
fun Activity.aplicarTemaElegido(): TemaApp {
    val tema = PreferenciasTema(applicationContext).tema()
    setTheme(tema.estilo)
    return tema
}

/** Iconos de la barra de navegación del sistema: oscuros sobre un tema claro y claros sobre el oscuro. */
fun Activity.ajustarBarrasDelSistema(tema: TemaApp) {
    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = !tema.oscuro
}
