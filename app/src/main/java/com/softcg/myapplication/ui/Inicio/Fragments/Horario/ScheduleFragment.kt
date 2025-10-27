package com.softcg.myapplication.ui.Inicio.Fragments.Horario

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ScrollView
import android.graphics.Color
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.cardview.widget.CardView
import androidx.core.widget.NestedScrollView
import com.softcg.myapplication.R
import android.widget.Button
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.softcg.myapplication.ui.Inicio.Fragments.Horario.Adapters.CalendarAdapter
import com.softcg.myapplication.core.utils.CalendarUtils
import com.softcg.myapplication.ui.Inicio.Fragments.Horario.Adapters.HorarioAdapter
import com.softcg.myapplication.ui.Inicio.Fragments.Horario.Adapters.OnItemClickListener
import com.softcg.myapplication.ui.Inicio.Fragments.Agenda.AgendaCalendarWidget
import com.softcg.myapplication.ui.Inicio.Fragments.Horario.Models.AsignaturaConHora
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ScheduleFragment : Fragment(), OnItemClickListener {

    private val horarioViewModel: HorarioViewModel by viewModels()
    private lateinit var calendarWidget: AgendaCalendarWidget
    private lateinit var adapter: HorarioAdapter
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
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
        synchronizeScrollViews(view)
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
                    // Initialize timeline based on asignaturas
                    initDynamicTimeline(view, asignaturas)
                    // Then display asignaturas
                    displayAsignaturasOnTimeline(view, asignaturas)
                    emptyMessageTextView?.visibility = View.GONE
                    Log.d("ScheduleFragment", "Empty message hidden - showing ${asignaturas.size} cards")
                } catch (e: Exception) {
                    Log.e("ScheduleFragment", "Error displaying asignaturas", e)
                    emptyMessageTextView?.visibility = View.VISIBLE
                }
            } else {
                // No hay asignaturas para este día - show default timeline
                Log.d("ScheduleFragment", "No asignaturas for selected day - showing empty message")
                initDefaultTimeline(view)
                displayAsignaturasOnTimeline(view, emptyList())
                emptyMessageTextView?.visibility = View.VISIBLE

                val currentDate = horarioViewModel._currentDate.value
                val dayName = getDayName(currentDate)
                Log.d("ScheduleFragment", "Empty message shown for: $dayName (date=$currentDate)")
            }
            Log.d("ScheduleFragment", "═══════════════════════════════════════")
        })
    }

    private fun getDayName(date: java.time.LocalDate?): String {
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

        // Convert to AsignaturaConHora with color indices
        val asignaturasConHora = asignaturas.mapIndexed { index, asignatura ->
            AsignaturaConHora(asignatura, index)
        }

        // Add each card positioned according to its time
        asignaturasConHora.forEachIndexed { index, asignaturaConHora ->
            Log.d("ScheduleFragment", "[$index] Adding card: ${asignaturaConHora.asignatura.nombre}")
            Log.d("ScheduleFragment", "    Time: ${asignaturaConHora.horaInicio} - ${asignaturaConHora.horaFin}")
            addAsignaturaCard(container, asignaturaConHora)
        }

        Log.d("ScheduleFragment", "Final children count in container: ${container.childCount}")
        Log.d("ScheduleFragment", "───────────────────────────────────────")
    }

    private fun addAsignaturaCard(container: FrameLayout, asignaturaConHora: AsignaturaConHora) {
        try {
            val cardView = layoutInflater.inflate(R.layout.schedule_asignatura_card, container, false) as CardView

            // Populate card data
            val textNombre = cardView.findViewById<TextView>(R.id.text_asignatura_nombre)
            val textTutor = cardView.findViewById<TextView>(R.id.text_tutor)
            val textHoraRango = cardView.findViewById<TextView>(R.id.text_hora_rango)
            val textDuracion = cardView.findViewById<TextView>(R.id.text_duracion)
            val colorBar = cardView.findViewById<View>(R.id.card_color_bar)

            if (textNombre == null) {
                Log.e("ScheduleFragment", "Card views not found in layout!")
                return
            }

            textNombre.text = asignaturaConHora.asignatura.nombre
            textTutor?.text = asignaturaConHora.asignatura.tutor
            textHoraRango?.text = "${asignaturaConHora.horaInicio} - ${asignaturaConHora.horaFin}"
            textDuracion?.text = "${asignaturaConHora.asignatura.duracion}h"

            // Apply color to left bar
            val colorPastel = AsignaturaConHora.getColorPastel(asignaturaConHora.colorIndex)
            colorBar?.setBackgroundColor(Color.parseColor(colorPastel))

            // Calculate position based on start time
            val topMargin = calculateTopMargin(asignaturaConHora.horaInicio)

            // Fixed card height for all cards (modern, uniform design)
            val density = resources.displayMetrics.density
            val fixedCardHeight = (100 * density).toInt() // 100dp fixed height

            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                fixedCardHeight // All cards have same height
            )
            layoutParams.topMargin = topMargin
            layoutParams.setMargins(0, topMargin, 0, 12) // Spacing between cards

            Log.d("ScheduleFragment", "    → Card for ${asignaturaConHora.asignatura.nombre}:")
            Log.d("ScheduleFragment", "       Time: ${asignaturaConHora.horaInicio} - ${asignaturaConHora.horaFin}")
            Log.d("ScheduleFragment", "       Duration: ${asignaturaConHora.asignatura.duracion} hours")
            Log.d("ScheduleFragment", "       Top margin: $topMargin px")
            Log.d("ScheduleFragment", "       Card height: $fixedCardHeight px (fixed)")

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
        // Each hour in timeline is 60dp + 4px divider
        val density = resources.displayMetrics.density
        val hourHeightPx = (60 * density).toInt() // 60dp per hour

        // Calculate height: duration × hourHeight + dividers
        // For example: 2 hours = 2 × 60dp + 2 dividers (4px each)
        val heightWithoutDividers = (duracionHoras * hourHeightPx)
        val dividersHeight = duracionHoras * 4 // 4px per hour

        val totalHeight = heightWithoutDividers + dividersHeight - 8 // Subtract small margin for better fit

        Log.d("ScheduleFragment", "    Calculate height: ${duracionHoras}h → ${totalHeight}px (${hourHeightPx}px/hour)")

        return totalHeight
    }

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

            // Each hour in timeline has a consistent height
            val density = resources.displayMetrics.density
            val hourHeightPx = (60 * density).toInt() // 60dp per hour
            val minuteHeightPx = hourHeightPx / 60.0 // Height per minute

            // Add 4px for each divider passed (one divider of 4px per hour)
            val hoursPassed = (minutesFromStart / 60)
            val dividerOffset = hoursPassed * 4 // 4px per divider

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

    private fun initDynamicTimeline(view: View, asignaturas: List<com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura>) {
        val timeColumn = view.findViewById<LinearLayout>(R.id.time_column)
        val horarioContainer = view.findViewById<FrameLayout>(R.id.horarioContainer)

        // Clear any existing time slots
        timeColumn.removeAllViews()

        Log.d("ScheduleFragment", "═══════════════════════════════════════")
        Log.d("ScheduleFragment", "24-HOUR TIMELINE INITIALIZATION")

        // Always show 24 hours (00:00 to 23:00)
        timelineStartHour = 0
        val timelineEndHour = 23

        Log.d("ScheduleFragment", "Timeline range: ${timelineStartHour}:00 - ${timelineEndHour}:00 (24 hours)")

        // Generate time slots
        val timeSlots = generateTimeSlots(timelineStartHour, timelineEndHour)

        // Calculate the exact height for each hour block
        val density = resources.displayMetrics.density
        val hourHeightPx = (60 * density).toInt() // 60dp per hour

        var totalHeight = 0

        timeSlots.forEach { timeSlot ->
            // Create container for time slot
            val slotContainer = LinearLayout(requireContext())
            slotContainer.orientation = LinearLayout.HORIZONTAL
            slotContainer.gravity = android.view.Gravity.CENTER_VERTICAL

            val containerParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                hourHeightPx
            )
            slotContainer.layoutParams = containerParams

            // Time text
            val timeView = TextView(requireContext())
            timeView.text = timeSlot
            timeView.textSize = 12f
            timeView.setTextColor(Color.parseColor("#555555"))
            timeView.gravity = android.view.Gravity.CENTER
            timeView.setPadding(4, 0, 4, 0)
            timeView.setTypeface(null, android.graphics.Typeface.BOLD)

            val textParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            timeView.layoutParams = textParams

            slotContainer.addView(timeView)
            timeColumn.addView(slotContainer)
            totalHeight += hourHeightPx

            // Add a modern styled divider line with dot indicator
            val dividerContainer = LinearLayout(requireContext())
            dividerContainer.orientation = LinearLayout.HORIZONTAL
            dividerContainer.gravity = android.view.Gravity.CENTER_VERTICAL

            val dividerContainerParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                4 // Fixed height to accommodate dot indicator
            )
            dividerContainer.layoutParams = dividerContainerParams
            dividerContainer.setPadding(0, 0, 0, 0)

            // Add small circle indicator
            val dotIndicator = View(requireContext())
            dotIndicator.setBackgroundColor(Color.parseColor("#9C27B0"))
            val dotParams = LinearLayout.LayoutParams(4, 4)
            dotParams.setMargins(8, 0, 4, 0)
            dotIndicator.layoutParams = dotParams

            // Divider line
            val divider = View(requireContext())
            divider.setBackgroundColor(Color.parseColor("#E0E0E0"))
            val dividerParams = LinearLayout.LayoutParams(
                0,
                2
            )
            dividerParams.weight = 1f
            divider.layoutParams = dividerParams

            dividerContainer.addView(dotIndicator)
            dividerContainer.addView(divider)

            timeColumn.addView(dividerContainer)
            totalHeight += 4 // 4px per divider to accommodate dot
        }

        // Set the exact height for horarioContainer to match timeline
        val containerParams = horarioContainer.layoutParams
        containerParams.height = totalHeight
        horarioContainer.layoutParams = containerParams

        Log.d("ScheduleFragment", "Timeline created with ${timeSlots.size} hours")
        Log.d("ScheduleFragment", "Total timeline height: $totalHeight px")
        Log.d("ScheduleFragment", "═══════════════════════════════════════")
    }

    private fun initDefaultTimeline(view: View) {
        val timeColumn = view.findViewById<LinearLayout>(R.id.time_column)
        val horarioContainer = view.findViewById<FrameLayout>(R.id.horarioContainer)

        // Clear any existing time slots
        timeColumn.removeAllViews()

        // Always show 24 hours (00:00 to 23:00)
        timelineStartHour = 0
        val timelineEndHour = 23

        Log.d("ScheduleFragment", "═══════════════════════════════════════")
        Log.d("ScheduleFragment", "DEFAULT 24-HOUR TIMELINE INITIALIZATION")
        Log.d("ScheduleFragment", "Timeline range: ${timelineStartHour}:00 - ${timelineEndHour}:00 (24 hours)")

        // Generate time slots
        val timeSlots = generateTimeSlots(timelineStartHour, timelineEndHour)

        // Calculate the exact height for each hour block
        val density = resources.displayMetrics.density
        val hourHeightPx = (60 * density).toInt() // 60dp per hour

        var totalHeight = 0

        timeSlots.forEach { timeSlot ->
            // Create container for time slot
            val slotContainer = LinearLayout(requireContext())
            slotContainer.orientation = LinearLayout.HORIZONTAL
            slotContainer.gravity = android.view.Gravity.CENTER_VERTICAL

            val containerParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                hourHeightPx
            )
            slotContainer.layoutParams = containerParams

            // Time text
            val timeView = TextView(requireContext())
            timeView.text = timeSlot
            timeView.textSize = 12f
            timeView.setTextColor(Color.parseColor("#555555"))
            timeView.gravity = android.view.Gravity.CENTER
            timeView.setPadding(4, 0, 4, 0)
            timeView.setTypeface(null, android.graphics.Typeface.BOLD)

            val textParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            timeView.layoutParams = textParams

            slotContainer.addView(timeView)
            timeColumn.addView(slotContainer)
            totalHeight += hourHeightPx

            // Add a modern styled divider line with dot indicator
            val dividerContainer = LinearLayout(requireContext())
            dividerContainer.orientation = LinearLayout.HORIZONTAL
            dividerContainer.gravity = android.view.Gravity.CENTER_VERTICAL

            val dividerContainerParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                4 // Fixed height to accommodate dot indicator
            )
            dividerContainer.layoutParams = dividerContainerParams
            dividerContainer.setPadding(0, 0, 0, 0)

            // Add small circle indicator
            val dotIndicator = View(requireContext())
            dotIndicator.setBackgroundColor(Color.parseColor("#9C27B0"))
            val dotParams = LinearLayout.LayoutParams(4, 4)
            dotParams.setMargins(8, 0, 4, 0)
            dotIndicator.layoutParams = dotParams

            // Divider line
            val divider = View(requireContext())
            divider.setBackgroundColor(Color.parseColor("#E0E0E0"))
            val dividerParams = LinearLayout.LayoutParams(
                0,
                2
            )
            dividerParams.weight = 1f
            divider.layoutParams = dividerParams

            dividerContainer.addView(dotIndicator)
            dividerContainer.addView(divider)

            timeColumn.addView(dividerContainer)
            totalHeight += 4 // 4px per divider to accommodate dot
        }

        // Set the exact height for horarioContainer to match timeline
        val containerParams = horarioContainer.layoutParams
        containerParams.height = totalHeight
        horarioContainer.layoutParams = containerParams

        Log.d("ScheduleFragment", "Default timeline created with ${timeSlots.size} hours")
        Log.d("ScheduleFragment", "═══════════════════════════════════════")
    }

    private fun generateTimeSlots(startHour: Int, endHour: Int): List<String> {
        val timeSlots = mutableListOf<String>()
        var currentHour = startHour

        while (currentHour <= endHour) {
            timeSlots.add(String.format("%02d:00", currentHour))
            currentHour++
        }

        return timeSlots
    }

    override fun onItemClick(date: LocalDate) {
        horarioViewModel.obtenerCurrentDate(date)
        horarioViewModel.obtenerAsignaturas()
    }

    private fun synchronizeScrollViews(view: View) {
        val timelineScrollView = view.findViewById<ScrollView>(R.id.timelineScrollView)
        val contentScrollView = view.findViewById<ScrollView>(R.id.contentScrollView)

        var isTimelineScrolling = false
        var isContentScrolling = false

        contentScrollView.viewTreeObserver.addOnScrollChangedListener {
            if (!isTimelineScrolling) {
                isContentScrolling = true
                timelineScrollView.scrollTo(0, contentScrollView.scrollY)
                isContentScrolling = false
            }
        }

        timelineScrollView.viewTreeObserver.addOnScrollChangedListener {
            if (!isContentScrolling) {
                isTimelineScrolling = true
                contentScrollView.scrollTo(0, timelineScrollView.scrollY)
                isTimelineScrolling = false
            }
        }
    }

}