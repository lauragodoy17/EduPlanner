package com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import com.softcg.myapplication.R
import com.softcg.myapplication.domain.gamificacion.EstadisticasReglas
import com.softcg.myapplication.domain.gamificacion.RachaReglas
import java.time.LocalDate

/**
 * Mapa de actividad: una columna por semana, de lunes a domingo. Un solo tono, de claro a oscuro,
 * con escala fija para que una semana buena se vea igual de buena el mes siguiente.
 */
class ActividadHeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    /** Se llama al tocar un día: la vista no muestra números por sí sola. */
    var onDiaTocado: ((LocalDate, Int) -> Unit)? = null

    private val densidad = resources.displayMetrics.density
    private val hueco = 3 * densidad
    private val radio = 3 * densidad
    private val anchoEtiquetas = 16 * densidad

    private val rampa = listOf(R.color.pista_clara, R.color.purple_soft, R.color.purple_light, R.color.purple_accent, R.color.purple_deep)
        .map { context.getColor(it) }
    private val celda = Paint(Paint.ANTI_ALIAS_FLAG)
    private val marcaHoy = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * densidad
        color = context.getColor(R.color.text_primary)
    }
    private val etiqueta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10f, resources.displayMetrics)
        color = context.getColor(R.color.text_hint)
    }
    private val rect = RectF()

    private var actividad = emptyMap<LocalDate, Int>()
    private var hoy: LocalDate = LocalDate.now()
    private var inicio: LocalDate = RachaReglas.lunes(hoy).minusWeeks((COLUMNAS - 1).toLong())

    fun mostrar(actividad: Map<LocalDate, Int>, hoy: LocalDate) {
        this.actividad = actividad
        this.hoy = hoy
        inicio = RachaReglas.lunes(hoy).minusWeeks((COLUMNAS - 1).toLong())
        contentDescription = "Actividad de las últimas $COLUMNAS semanas: ${actividad.count { it.value > 0 }} días con actividad"
        invalidate()
    }

    private fun lado(ancho: Int) = (ancho - paddingLeft - paddingRight - anchoEtiquetas - hueco * (COLUMNAS - 1)) / COLUMNAS

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val ancho = MeasureSpec.getSize(widthMeasureSpec)
        val alto = lado(ancho) * FILAS + hueco * (FILAS - 1) + paddingTop + paddingBottom
        setMeasuredDimension(ancho, alto.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        val lado = lado(width)
        val inicioX = paddingLeft + anchoEtiquetas

        ETIQUETAS_FILAS.forEach { (fila, texto) ->
            val y = paddingTop + fila * (lado + hueco) + lado * 0.75f
            canvas.drawText(texto, paddingLeft.toFloat(), y, etiqueta)
        }

        for (columna in 0 until COLUMNAS) {
            for (fila in 0 until FILAS) {
                val dia = inicio.plusDays((columna * 7 + fila).toLong())
                if (dia.isAfter(hoy)) continue
                val x = inicioX + columna * (lado + hueco)
                val y = paddingTop + fila * (lado + hueco)
                rect.set(x, y, x + lado, y + lado)
                celda.color = rampa[nivelDe(actividad[dia] ?: 0)]
                canvas.drawRoundRect(rect, radio, radio, celda)
                if (dia == hoy) canvas.drawRoundRect(rect, radio, radio, marcaHoy)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) return true
        if (event.action != MotionEvent.ACTION_UP) return super.onTouchEvent(event)

        val paso = lado(width) + hueco
        val columna = ((event.x - paddingLeft - anchoEtiquetas) / paso).toInt()
        val fila = ((event.y - paddingTop) / paso).toInt()
        if (columna in 0 until COLUMNAS && fila in 0 until FILAS) {
            val dia = inicio.plusDays((columna * 7 + fila).toLong())
            if (!dia.isAfter(hoy)) onDiaTocado?.invoke(dia, actividad[dia] ?: 0)
        }
        return true
    }

    companion object {
        private const val COLUMNAS = EstadisticasReglas.SEMANAS_ACTIVIDAD
        private const val FILAS = 7
        private val ETIQUETAS_FILAS = listOf(0 to "L", 2 to "X", 4 to "V", 6 to "D")

        /** Tramos fijos en XP diario: 0, hasta 9, hasta 24, hasta 49, 50 o más. */
        fun nivelDe(xp: Int): Int = when {
            xp <= 0 -> 0
            xp < 10 -> 1
            xp < 25 -> 2
            xp < 50 -> 3
            else -> 4
        }
    }
}
