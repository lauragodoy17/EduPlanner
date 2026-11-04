package com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorInt
import com.softcg.myapplication.R

/** Anillo de avance: el XP alrededor del avatar y el tiempo en las sesiones de enfoque. */
class AnilloProgresoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val densidad = resources.displayMetrics.density
    private val pista = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = context.getColor(R.color.pista_clara)
    }
    private val avance = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = context.getColor(R.color.purple_accent)
    }
    private val arco = RectF()
    private var fraccion = 0f
    private var animador: ValueAnimator? = null

    init {
        grosor(5f)
    }

    fun grosor(dp: Float) {
        pista.strokeWidth = dp * densidad
        avance.strokeWidth = dp * densidad
        invalidate()
    }

    fun colores(@ColorInt colorPista: Int, @ColorInt colorAvance: Int) {
        pista.color = colorPista
        avance.color = colorAvance
        invalidate()
    }

    /** [valor] entre 0 y 1. */
    fun fijar(valor: Float, animar: Boolean = true) {
        val destino = valor.coerceIn(0f, 1f)
        animador?.cancel()
        if (!animar || !ValueAnimator.areAnimatorsEnabled()) {
            fraccion = destino
            invalidate()
            return
        }
        animador = ValueAnimator.ofFloat(fraccion, destino).apply {
            duration = 700
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                fraccion = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animador?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val margen = pista.strokeWidth / 2
        arco.set(margen, margen, width - margen, height - margen)
        canvas.drawOval(arco, pista)
        if (fraccion > 0f) canvas.drawArc(arco, -90f, 360f * fraccion, false, avance)
    }
}
