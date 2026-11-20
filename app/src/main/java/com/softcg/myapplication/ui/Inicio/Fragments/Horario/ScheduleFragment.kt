package com.softcg.myapplication.ui.Inicio.Fragments.Horario

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.FrameLayout
import androidx.core.graphics.toColorInt
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.cardview.widget.CardView
import androidx.core.widget.NestedScrollView
import com.softcg.myapplication.R
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.softcg.myapplication.ui.Inicio.Fragments.Agenda.AgendaCalendarWidget
import com.softcg.myapplication.ui.Inicio.Fragments.Horario.Models.AsignaturaConHora
import com.softcg.myapplication.ui.Inicio.Fragments.Horario.Models.DistribucionHorario
import com.softcg.myapplication.ui.Inicio.Fragments.Horario.Models.Ubicacion
import androidx.core.view.doOnLayout
import com.softcg.myapplication.ui.Inicio.Fragments.Detalle.TipoDetalle
import com.softcg.myapplication.ui.Inicio.Fragments.Detalle.abrirDetalle
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ScheduleFragment : Fragment() {

    private companion object {
        const val ALTO_HORA_DP = 72
        const val ALTO_DIVISOR_PX = 4
        const val ALTO_LINEA_PX = 2
        /** Espacio entre dos clases que están a la misma hora. */
        const val SEPARACION_COLUMNAS_DP = 4
    }

    private val horarioViewModel: HorarioViewModel by viewModels()
    private lateinit var calendarWidget: AgendaCalendarWidget
    private var timelineStartHour: Int = 0 // Dynamic start hour based on current time

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_week_view, container, false)
        Log.d("ScheduleFragment", "onCreateView called")
        // Timeline will be initialized when asignaturas are loaded
        initCalendarWidget(view)
        initRecyclerAsignatura(view)
        return view
    }

    private fun initCalendarWidget(view: View) {
        try {
            calendarWidget = view.findViewById(R.id.schedule_calendar_widget)

            calendarWidget.onDateSelectedListener = { selectedDate ->
                Log.d("ScheduleFragment", "Calendar date selected: $selectedDate")
                // Convert string date to LocalDate for horario functionality
                convertAndFilterByDate(selectedDate)
            }

            // Initialize with today's date
            val today = LocalDate.now()
            horarioViewModel.obtenerCurrentDate(today)
            Log.d("ScheduleFragment", "Calendar initialized with today's date: $today")
        } catch (e: Exception) {
            Log.e("ScheduleFragment", "Error initializing calendar", e)
            e.printStackTrace()
        }
    }

    private fun convertAndFilterByDate(selectedDate: String) {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(selectedDate)
            val calendar = Calendar.getInstance()
            calendar.time = date!!

            val localDate = LocalDate.of(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            horarioViewModel.obtenerCurrentDate(localDate)
            horarioViewModel.obtenerAsignaturas()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun initRecyclerAsignatura(view: View){
        Log.d("ScheduleFragment", "═══════════════════════════════════════")
        Log.d("ScheduleFragment", "Initializing asignaturas display")

        val emptyMessageTextView = view.findViewById<TextView>(R.id.textoAsignaturas)
        // Al girar la pantalla o cambiar el ancho, las columnas se vuelven a repartir
        view.findViewById<FrameLayout>(R.id.horarioContainer).addOnLayoutChangeListener { v, l, _, r, _, ol, _, or, _ ->
            if (r - l != or - ol) v.post { repartirColumnas(v as FrameLayout) }
        }

        // Observe current date to update empty message
        horarioViewModel._currentDate.observe(viewLifecycleOwner, Observer { date ->
            Log.d("ScheduleFragment", "Current date changed to: $date (${date?.dayOfWeek})")

            // Update empty message with current day
            if (emptyMessageTextView != null && date != null) {
                val dayName = getDayName(date)
                emptyMessageTextView.text = "No tienes clases para $dayName"
                Log.d("ScheduleFragment", "Empty message updated: ${emptyMessageTextView.text}")
            }
        })

        // Observe filtered asignaturas for the selected date
        horarioViewModel._asignaturas.observe(viewLifecycleOwner, Observer { asignaturas ->
            Log.d("ScheduleFragment", "───────────────────────────────────────")
            Log.d("ScheduleFragment", "Asignaturas observed: ${asignaturas?.size ?: 0} items")

            if (asignaturas != null && asignaturas.isNotEmpty()) {
                // Hay asignaturas para mostrar en este día
                Log.d("ScheduleFragment", "Displaying ${asignaturas.size} asignaturas:")
                asignaturas.forEach { asig ->
                    Log.d("ScheduleFragment", "  → ${asig.nombre}: ${asig.hora} (${asig.duracion}h)")
                }
                try {
                    construirLineaDeTiempo(view)
                    displayAsignaturasOnTimeline(view, asignaturas)
                    llevarALaPrimeraClase(view, asignaturas)
                    emptyMessageTextView?.visibility = View.GONE
                    Log.d("ScheduleFragment", "Empty message hidden - showing ${asignaturas.size} cards")
                } catch (e: Exception) {
                    Log.e("ScheduleFragment", "Error displaying asignaturas", e)
                    emptyMessageTextView?.visibility = View.VISIBLE
                }
            } else {
                // No hay asignaturas para este día - show default timeline
                Log.d("ScheduleFragment", "No asignaturas for selected day - showing empty message")
                construirLineaDeTiempo(view)
                displayAsignaturasOnTimeline(view, emptyList())
                llevarALaPrimeraClase(view, emptyList())
                emptyMessageTextView?.visibility = View.VISIBLE

                val currentDate = horarioViewModel._currentDate.value
                val dayName = getDayName(currentDate)
                Log.d("ScheduleFragment", "Empty message shown for: $dayName (date=$currentDate)")
            }
            Log.d("ScheduleFragment", "═══════════════════════════════════════")
        })
    }

    private fun getDayName(date: LocalDate?): String {
        if (date == null) {
            Log.w("ScheduleFragment", "getDayName called with null date")
            return "este día"
        }

        val dayName = when (date.dayOfWeek.value) {
            1 -> "Lunes"
            2 -> "Martes"
            3 -> "Miércoles"
            4 -> "Jueves"
            5 -> "Viernes"
            6 -> "Sábado"
            7 -> "Domingo"
            else -> {
                Log.w("ScheduleFragment", "Invalid day of week: ${date.dayOfWeek.value}")
                "este día"
            }
        }

        Log.d("ScheduleFragment", "getDayName: $date -> $dayName (dayOfWeek=${date.dayOfWeek.value})")
        return dayName
    }

    private fun displayAsignaturasOnTimeline(view: View, asignaturas: List<com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura>) {
        val container = view.findViewById<FrameLayout>(R.id.horarioContainer)

        if (container == null) {
            Log.e("ScheduleFragment", "Container (horarioContainer) not found!")
            return
        }

        Log.d("ScheduleFragment", "───────────────────────────────────────")
        Log.d("ScheduleFragment", "displayAsignaturasOnTimeline called")
        Log.d("ScheduleFragment", "Container found: YES")
        Log.d("ScheduleFragment", "Previous children count: ${container.childCount}")

        // Clear existing views
        container.removeAllViews()

        if (asignaturas.isEmpty()) {
            Log.d("ScheduleFragment", "No asignaturas to display - container cleared")
            return
        }

        Log.d("ScheduleFragment", "Displaying ${asignaturas.size} asignaturas")

        // El color sale del id: la misma asignatura tiene el mismo color todos los días
        val asignaturasConHora = asignaturas.mapIndexed { index, asignatura ->
            AsignaturaConHora(asignatura, asignatura.id ?: index)
        }

        // Las clases que se cruzan se reparten en columnas en vez de taparse
        val ubicaciones = DistribucionHorario.distribuir(asignaturasConHora.map { a ->
            val inicio = DistribucionHorario.minutos(a.horaInicio) ?: 0
            inicio to inicio + maxOf(a.asignatura.duracion, 1) * 60
        })
        asignaturasConHora.forEachIndexed { index, asignaturaConHora ->
            addAsignaturaCard(container, asignaturaConHora, ubicaciones[index])
        }
        container.doOnLayout { repartirColumnas(it as FrameLayout) }

        Log.d("ScheduleFragment", "Final children count in container: ${container.childCount}")
        Log.d("ScheduleFragment", "───────────────────────────────────────")
    }

    /** Ancho y posición de cada tarjeta según su columna; necesita el ancho real del contenedor. */
    private fun repartirColumnas(container: FrameLayout) {
        val ancho = container.width
        if (ancho <= 0) return
        val separacion = (SEPARACION_COLUMNAS_DP * resources.displayMetrics.density).toInt()
        for (i in 0 until container.childCount) {
            val tarjeta = container.getChildAt(i)
            val ubicacion = tarjeta.tag as? Ubicacion ?: continue
            val anchoColumna = (ancho - separacion * (ubicacion.columnas - 1)) / ubicacion.columnas
            val parametros = tarjeta.layoutParams as FrameLayout.LayoutParams
            val izquierda = ubicacion.columna * (anchoColumna + separacion)
            if (parametros.width != anchoColumna || parametros.leftMargin != izquierda) {
                parametros.width = anchoColumna
                parametros.leftMargin = izquierda
                tarjeta.layoutParams = parametros
            }
        }
    }

    private fun addAsignaturaCard(container: FrameLayout, asignaturaConHora: AsignaturaConHora, ubicacion: Ubicacion) {
        try {
            val cardView = layoutInflater.inflate(R.layout.schedule_asignatura_card, container, false) as CardView

            // Populate card data
            val textNombre = cardView.findViewById<TextView>(R.id.text_asignatura_nombre)
            val textTutor = cardView.findViewById<TextView>(R.id.text_tutor)
            val textHoraRango = cardView.findViewById<TextView>(R.id.text_hora_rango)
            val textDuracion = cardView.findViewById<TextView>(R.id.text_duracion)
            val colorBar = cardView.findViewById<View>(R.id.card_color_bar)

            // Tocar la clase abre la pantalla de su asignatura, igual que desde Asignaturas
            cardView.isClickable = true
            cardView.isFocusable = true
            cardView.foreground = with(android.util.TypedValue()) {
                requireContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, this, true)
                ContextCompat.getDrawable(requireContext(), resourceId)
            }
            cardView.setOnClickListener { abrirDetalle(TipoDetalle.ASIGNATURA, asignaturaConHora.asignatura.id) }

            if (textNombre == null) {
                Log.e("ScheduleFragment", "Card views not found in layout!")
                return
            }

            textNombre.text = asignaturaConHora.asignatura.nombre
            textTutor?.text = asignaturaConHora.asignatura.tutor
            textHoraRango?.text = "${AsignaturaConHora.horaLegible(asignaturaConHora.horaInicio)} - ${asignaturaConHora.horaFin}"
            textDuracion?.text = "${asignaturaConHora.asignatura.duracion}h"
            // En una columna estrecha sobra la duración (ya está en el rango) y, desde tres, el profesor
            textDuracion?.visibility = if (ubicacion.columnas >= 2) View.GONE else View.VISIBLE
            textTutor?.visibility = if (ubicacion.columnas >= 3 || asignaturaConHora.asignatura.tutor.isBlank()) View.GONE else View.VISIBLE
            if (ubicacion.columnas >= 2) {
                textNombre.maxLines = 2
                val interior = cardView.getChildAt(0)
                val px = (6 * resources.displayMetrics.density).toInt()
                interior.setPadding(px, interior.paddingTop, px, interior.paddingBottom)
                colorBar?.let { barra -> (barra.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = px }
            }
            cardView.tag = ubicacion
            cardView.contentDescription = buildString {
                append(asignaturaConHora.asignatura.nombre).append(", ").append(textHoraRango?.text)
                if (ubicacion.columnas > 1) append(", al mismo tiempo que otra clase")
            }

            // Color que identifica la asignatura
            colorBar?.backgroundTintList = android.content.res.ColorStateList.valueOf(
                AsignaturaConHora.getColorAsignatura(asignaturaConHora.colorIndex).toColorInt()
            )

            // Calculate position based on start time
            val topMargin = calculateTopMargin(asignaturaConHora.horaInicio)

            // El alto refleja la duración, para que dos clases seguidas no se monten
            val fixedCardHeight = calculateCardHeight(asignaturaConHora.asignatura.duracion)

            // El ancho definitivo lo pone repartirColumnas cuando se conoce el del contenedor
            val layoutParams = FrameLayout.LayoutParams(
                if (ubicacion.columnas == 1) FrameLayout.LayoutParams.MATCH_PARENT else container.width.coerceAtLeast(1) / ubicacion.columnas,
                fixedCardHeight
            )
            layoutParams.topMargin = topMargin
            layoutParams.setMargins(if (ubicacion.columnas == 1) 0 else ubicacion.columna * layoutParams.width, topMargin, 0, 0)

            Log.d("ScheduleFragment", "    → Card for ${asignaturaConHora.asignatura.nombre}:")
            Log.d("ScheduleFragment", "       Time: ${asignaturaConHora.horaInicio} - ${asignaturaConHora.horaFin}")
            Log.d("ScheduleFragment", "       Duration: ${asignaturaConHora.asignatura.duracion} hours")
            Log.d("ScheduleFragment", "       Top margin: $topMargin px")
            Log.d("ScheduleFragment", "       Card height: $fixedCardHeight px")

            cardView.layoutParams = layoutParams
            container.addView(cardView)

            Log.d("ScheduleFragment", "    ✓ Card added successfully")

            // Add animation
            cardView.post {
                cardView.alpha = 0f
                cardView.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }
        } catch (e: Exception) {
            Log.e("ScheduleFragment", "Error adding card for ${asignaturaConHora.asignatura.nombre}", e)
            e.printStackTrace()
        }
    }

    private fun calculateCardHeight(duracionHoras: Int): Int {
        val horas = maxOf(duracionHoras, 1)
        val margen = (4 * resources.displayMetrics.density).toInt()
        return horas * altoHoraPx() + (horas - 1) * ALTO_DIVISOR_PX - margen
    }

    private fun altoHoraPx(): Int = (ALTO_HORA_DP * resources.displayMetrics.density).toInt()

    private fun calculateTopMargin(horaInicio: String): Int {
        try {
            // Remove ALL spaces from the time string
            val cleanHora = horaInicio.trim().replace(" ", "")
            val parts = cleanHora.split(":")

            if (parts.size < 2) {
                Log.e("ScheduleFragment", "    ✗ Invalid time format: '$horaInicio' (expected HH:mm)")
                return 0
            }

            val hour = parts[0].trim().toIntOrNull()
            val minute = parts[1].trim().toIntOrNull()

            if (hour == null || minute == null) {
                Log.e("ScheduleFragment", "    ✗ Could not parse time: '$horaInicio' -> hour=$hour, minute=$minute")
                return 0
            }

            // Calculate minutes from timeline start
            val minutesFromStart = ((hour - timelineStartHour) * 60) + minute

            if (minutesFromStart < 0) {
                Log.w("ScheduleFragment", "    ⚠ Time $horaInicio is before timeline start (${timelineStartHour}:00)")
                return 0
            }

            val minuteHeightPx = altoHoraPx() / 60.0 // Height per minute

            // Add 4px for each divider passed (one divider of 4px per hour)
            val hoursPassed = (minutesFromStart / 60)
            val dividerOffset = hoursPassed * ALTO_DIVISOR_PX

            val topMarginPx = (minutesFromStart * minuteHeightPx).toInt() + dividerOffset

            Log.d("ScheduleFragment", "    Calculate: $horaInicio -> ${hour}h ${minute}m")
            Log.d("ScheduleFragment", "    Timeline starts at: ${timelineStartHour}:00")
            Log.d("ScheduleFragment", "    Minutes from start: $minutesFromStart min")
            Log.d("ScheduleFragment", "    Top margin: $topMarginPx px")

            return topMarginPx
        } catch (e: Exception) {
            Log.e("ScheduleFragment", "    ✗ Error calculating margin for time: '$horaInicio'", e)
            return 0
        }
    }

    /** Horas a la izquierda y una línea por hora detrás de las tarjetas. Siempre 00:00 a 23:00. */
    private fun construirLineaDeTiempo(view: View) {
        val timeColumn = view.findViewById<LinearLayout>(R.id.time_column)
        val lineas = view.findViewById<FrameLayout>(R.id.horarioLineas)
        val horarioContainer = view.findViewById<FrameLayout>(R.id.horarioContainer)

        timeColumn.removeAllViews()
        lineas.removeAllViews()
        timelineStartHour = 0

        val altoHora = altoHoraPx()
        val colorLinea = ContextCompat.getColor(requireContext(), R.color.borde_suave)
        val colorTexto = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        generateTimeSlots().forEachIndexed { indice, hora ->
            val etiqueta = TextView(requireContext())
            etiqueta.text = hora
            etiqueta.textSize = 12f
            etiqueta.setTextColor(colorTexto)
            etiqueta.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                altoHora + ALTO_DIVISOR_PX
            )
            timeColumn.addView(etiqueta)

            val linea = View(requireContext())
            linea.setBackgroundColor(colorLinea)
            val parametros = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, ALTO_LINEA_PX)
            parametros.topMargin = indice * (altoHora + ALTO_DIVISOR_PX)
            linea.layoutParams = parametros
            lineas.addView(linea)
        }

        val containerParams = horarioContainer.layoutParams
        containerParams.height = 24 * (altoHora + ALTO_DIVISOR_PX)
        horarioContainer.layoutParams = containerParams
    }

    /** Con 24 horas de alto, abrir en las 00:00 dejaría las clases fuera de la vista. */
    private fun llevarALaPrimeraClase(view: View, asignaturas: List<com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura>) {
        val scroll = view.findViewById<NestedScrollView>(R.id.contentScrollView)
        // Se abre en la hora en punto de la primera clase, para que se vea su etiqueta (05:00 si empieza 05:30)
        val destino = asignaturas
            .mapNotNull { DistribucionHorario.minutos(it.hora) }
            .minOrNull()
            ?.let { calculateTopMargin(String.format(Locale.ROOT, "%02d:00", it / 60)) }
            ?: calculateTopMargin(String.format(Locale.ROOT, "%02d:00", Calendar.getInstance().get(Calendar.HOUR_OF_DAY)))
        val respiro = (12 * resources.displayMetrics.density).toInt()
        scroll.post { scroll.smoothScrollTo(0, maxOf(destino - respiro, 0)) }
    }

    /** Las 24 horas del día, "00:00" a "23:00". */
    private fun generateTimeSlots(): List<String> = (0..23).map { String.format(Locale.ROOT, "%02d:00", it) }

}
