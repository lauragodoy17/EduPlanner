package com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Views

import androidx.core.graphics.withRotation
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.softcg.myapplication.R
import kotlin.math.sin
import kotlin.random.Random

/** Una sola ráfaga de confeti para celebrar. No hace nada si el sistema tiene las animaciones desactivadas. */
class ConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private class Particula(
        val x: Float,
        val retraso: Float,
        val velocidad: Float,
        val oscilacion: Float,
        val fase: Float,
        val giro: Float,
        val color: Int
    )

    private val densidad = resources.displayMetrics.density
    private val pintura = Paint(Paint.ANTI_ALIAS_FLAG)
    private val colores = listOf(R.color.acento, R.color.marca_suave, R.color.xp_dorado, R.color.white)
        .map { context.getColor(it) }
    private var particulas = emptyList<Particula>()
    private var tiempo = 0f
    private var animador: ValueAnimator? = null

    fun lanzar() {
        if (!ValueAnimator.areAnimatorsEnabled()) return
        particulas = List(CANTIDAD) {
            Particula(
                x = Random.nextFloat(),
                retraso = Random.nextFloat() * 0.35f,
                velocidad = 0.8f + Random.nextFloat() * 0.5f,
                oscilacion = (8 + Random.nextFloat() * 18) * densidad,
                fase = Random.nextFloat() * 6.28f,
                giro = Random.nextFloat() * 720f,
                color = colores[it % colores.size]
            )
        }
        animador?.cancel()
        animador = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = DURACION_MS
            interpolator = LinearInterpolator()
            addUpdateListener {
                tiempo = it.animatedValue as Float
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
        val ancho = 6 * densidad
        val alto = 10 * densidad
        particulas.forEach { p ->
            val avance = ((tiempo - p.retraso) / (1f - p.retraso)).coerceIn(0f, 1f)
            if (avance <= 0f) return@forEach
            val y = -alto + (height + alto * 2) * avance * p.velocidad
            val x = p.x * width + sin(avance * 8f + p.fase) * p.oscilacion
            pintura.color = p.color
            pintura.alpha = ((1f - avance) * 2f * 255).toInt().coerceIn(0, 255)
            canvas.withRotation(p.giro * avance, x, y) {
                drawRect(x - ancho / 2, y - alto / 2, x + ancho / 2, y + alto / 2, pintura)
            }
        }
    }

    companion object {
        private const val CANTIDAD = 70
        private const val DURACION_MS = 2600L
    }
}
