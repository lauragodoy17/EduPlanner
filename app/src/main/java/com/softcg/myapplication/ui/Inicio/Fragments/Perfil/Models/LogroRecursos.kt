package com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Models

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.softcg.myapplication.R
import com.softcg.myapplication.domain.gamificacion.MetricaLogro
import com.softcg.myapplication.domain.gamificacion.MetricaMision
import com.softcg.myapplication.domain.gamificacion.RarezaLogro

/** Icono y color con que se dibujan logros y misiones. */
object LogroRecursos {

    @DrawableRes
    fun icono(metrica: MetricaLogro): Int = when (metrica) {
        MetricaLogro.TAREAS_REGISTRADAS -> R.drawable.ic_gami_tarea
        MetricaLogro.TAREAS_TERMINADAS -> R.drawable.ic_gami_check
        MetricaLogro.TAREAS_A_TIEMPO -> R.drawable.ic_gami_rayo
        MetricaLogro.EVENTOS -> R.drawable.ic_gami_evento
        MetricaLogro.CLASES, MetricaLogro.ASISTENCIAS -> R.drawable.ic_gami_clase
        MetricaLogro.CALIFICACIONES -> R.drawable.ic_gami_nota
        MetricaLogro.SESIONES_ENFOQUE -> R.drawable.ic_gami_enfoque
        MetricaLogro.MISIONES -> R.drawable.ic_gami_objetivo
        MetricaLogro.MEJOR_RACHA -> R.drawable.ic_gami_racha
        MetricaLogro.NIVEL -> R.drawable.ic_gami_estrella
    }

    @DrawableRes
    fun icono(metrica: MetricaMision): Int = when (metrica) {
        MetricaMision.TAREAS_REGISTRADAS -> R.drawable.ic_gami_tarea
        MetricaMision.EVENTOS_REGISTRADOS -> R.drawable.ic_gami_evento
        MetricaMision.TAREAS_TERMINADAS -> R.drawable.ic_gami_check
        MetricaMision.TAREAS_A_TIEMPO -> R.drawable.ic_gami_rayo
        MetricaMision.ASISTENCIAS -> R.drawable.ic_gami_clase
        MetricaMision.SESIONES_ENFOQUE -> R.drawable.ic_gami_enfoque
        MetricaMision.MISIONES_DIARIAS -> R.drawable.ic_gami_objetivo
        MetricaMision.DIAS_ACTIVOS -> R.drawable.ic_gami_racha
    }

    @ColorRes
    fun color(rareza: RarezaLogro): Int = when (rareza) {
        RarezaLogro.BRONCE -> R.color.logro_bronce
        RarezaLogro.PLATA -> R.color.logro_plata
        RarezaLogro.ORO -> R.color.logro_oro
    }
}
