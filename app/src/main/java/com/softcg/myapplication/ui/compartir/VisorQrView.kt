package com.softcg.myapplication.ui.compartir

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.softcg.myapplication.R
import kotlin.math.min

/**
 * Oscurece la imagen de la cámara salvo un marco cuadrado donde apuntar, con esquinas de la marca y una línea
 * que recorre el marco mientras busca. [exito] pinta las esquinas de verde al leer un código válido.
 */
class VisorQrView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val densidad = resources.displayMetrics.density
    private val marco = RectF()
    private val velo = Path()

    private val pinturaVelo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x99000000.toInt() }
    private val pinturaEsquinas = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.marca_suave)
        style = Paint.Style.STROKE
        strokeWidth = 5 * densidad
        strokeCap = Paint.Cap.ROUND
    }
    private val pinturaLinea = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.marca_suave)
        strokeWidth = 2 * densidad
        alpha = 200
    }

    private var avanceLinea = 0f
    private var buscando = true
    private val animacion = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1800
        repeatMode = ValueAnimator.REVERSE
        repeatCount = ValueAnimator.INFINITE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener {
            avanceLinea = it.animatedValue as Float
            invalidate()
        }
    }

    fun exito() {
        buscando = false
        animacion.cancel()
        pinturaEsquinas.color = context.getColor(R.color.estado_exito)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val lado = min(w * 0.72f, 300 * densidad)
        val izquierda = (w - lado) / 2
        // Un poco por encima del centro: abajo van las instrucciones
        val arriba = (h - lado) / 2 - h * 0.05f
        marco.set(izquierda, arriba, izquierda + lado, arriba + lado)
        val radio = 20 * densidad
        velo.reset()
        velo.fillType = Path.FillType.EVEN_ODD
        velo.addRect(0f, 0f, w.toFloat(), h.toFloat(), Path.Direction.CW)
        velo.addRoundRect(marco, radio, radio, Path.Direction.CW)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (buscando) animacion.start()
    }

    override fun onDetachedFromWindow() {
        animacion.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawPath(velo, pinturaVelo)
        dibujarEsquinas(canvas)
        if (buscando) {
            val margen = 16 * densidad
            val y = marco.top + margen + (marco.height() - 2 * margen) * avanceLinea
            canvas.drawLine(marco.left + margen, y, marco.right - margen, y, pinturaLinea)
        }
    }

    private fun dibujarEsquinas(canvas: Canvas) {
        val largo = 28 * densidad
        val radio = 20 * densidad
        with(marco) {
            // Cada esquina: tramo horizontal, arco y tramo vertical, siguiendo el borde redondeado del marco
            canvas.drawPath(Path().apply {
                moveTo(left, top + radio + largo); lineTo(left, top + radio)
                quadTo(left, top, left + radio, top); lineTo(left + radio + largo, top)
                moveTo(right - radio - largo, top); lineTo(right - radio, top)
                quadTo(right, top, right, top + radio); lineTo(right, top + radio + largo)
                moveTo(right, bottom - radio - largo); lineTo(right, bottom - radio)
                quadTo(right, bottom, right - radio, bottom); lineTo(right - radio - largo, bottom)
                moveTo(left + radio + largo, bottom); lineTo(left + radio, bottom)
                quadTo(left, bottom, left, bottom - radio); lineTo(left, bottom - radio - largo)
            }, pinturaEsquinas)
        }
    }
}
