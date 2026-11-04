package com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import com.softcg.myapplication.R
import java.time.LocalDate

/** XP de los últimos 7 días. Solo se rotulan hoy y el día tocado; el resto se lee por la altura. */
class BarrasSemanaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val densidad = resources.displayMetrics.density
    private val tamanoTexto = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, resources.displayMetrics)

    private val barra = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.purple_soft) }
    private val barraDestacada = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.purple_accent) }
    private val base = Paint().apply {
        color = context.getColor(R.color.input_border)
        strokeWidth = densidad
    }
    private val textoDia = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.text_secondary)
        textSize = tamanoTexto
        textAlign = Paint.Align.CENTER
    }
    private val textoValor = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.text_primary)
        textSize = tamanoTexto
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val forma = Path()

    private var dias = emptyList<Pair<LocalDate, Int>>()
    private var seleccion = -1

    fun mostrar(dias: List<Pair<LocalDate, Int>>) {
        this.dias = dias
        seleccion = dias.lastIndex
        contentDescription = "XP por día: " + dias.joinToString { (dia, xp) -> "${INICIALES[dia.dayOfWeek.value - 1]} $xp" }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (ALTO_DP * densidad).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        if (dias.isEmpty()) return
        val zonaDias = 22 * densidad
        val zonaValor = 20 * densidad
        val baseY = height - zonaDias
        val altoUtil = baseY - zonaValor
        val paso = width.toFloat() / dias.size
        val anchoBarra = (paso * 0.45f).coerceAtMost(24 * densidad)
        val radio = 4 * densidad
        val maximo = dias.maxOf { it.second }.coerceAtLeast(1)

        dias.forEachIndexed { i, (dia, xp) ->
            val centro = paso * i + paso / 2
            val destacada = i == seleccion
            if (xp > 0) {
                val alto = (altoUtil * xp / maximo).coerceAtLeast(4 * densidad)
                val arriba = baseY - alto
                val izquierda = centro - anchoBarra / 2
                val derecha = centro + anchoBarra / 2
                forma.reset()
                forma.moveTo(izquierda, baseY)
                forma.lineTo(izquierda, arriba + radio)
                forma.quadTo(izquierda, arriba, izquierda + radio, arriba)
                forma.lineTo(derecha - radio, arriba)
                forma.quadTo(derecha, arriba, derecha, arriba + radio)
                forma.lineTo(derecha, baseY)
                forma.close()
                canvas.drawPath(forma, if (destacada) barraDestacada else barra)
                if (destacada) canvas.drawText("$xp", centro, arriba - 5 * densidad, textoValor)
            } else if (destacada) {
                canvas.drawText("0", centro, baseY - 5 * densidad, textoValor)
            }
            canvas.drawText(INICIALES[dia.dayOfWeek.value - 1], centro, height - 5 * densidad, if (destacada) textoValor else textoDia)
        }
        canvas.drawLine(0f, baseY, width.toFloat(), baseY, base)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (dias.isEmpty()) return false
        // Solo al soltar: al desplazar la pantalla el padre cancela el gesto y la selección no cambia
        if (event.action == MotionEvent.ACTION_DOWN) return true
        if (event.action != MotionEvent.ACTION_UP) return super.onTouchEvent(event)
        seleccion = (event.x / (width.toFloat() / dias.size)).toInt().coerceIn(0, dias.lastIndex)
        invalidate()
        return true
    }

    companion object {
        private const val ALTO_DP = 140
        private val INICIALES = listOf("L", "M", "X", "J", "V", "S", "D")
    }
}
