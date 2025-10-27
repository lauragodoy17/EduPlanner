package com.softcg.myapplication.ui.Inicio.Fragments.Agenda

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.softcg.myapplication.R
import java.text.SimpleDateFormat
import java.util.*

class AgendaCalendarWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    private lateinit var monthYearText: TextView
    private lateinit var previousButton: ImageView
    private lateinit var nextButton: ImageView
    private lateinit var expandCollapseButton: ImageView
    private lateinit var calendarGrid: LinearLayout

    private val calendar = Calendar.getInstance()
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private var selectedDate: String? = null
    private var datesWithEvents: Set<String> = emptySet()
    private var dayViews: MutableList<TextView> = mutableListOf()
    private var isExpanded: Boolean = false // Calendar starts collapsed

    var onDateSelectedListener: ((String) -> Unit)? = null

    init {
        try {
            LayoutInflater.from(context).inflate(R.layout.agenda_calendar_widget, this, true)
            initViews()
            setupClickListeners()
            updateCalendar()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initViews() {
        monthYearText = findViewById(R.id.tv_month_year)
        previousButton = findViewById(R.id.btn_previous_month)
        nextButton = findViewById(R.id.btn_next_month)
        expandCollapseButton = findViewById(R.id.btn_expand_collapse)
        calendarGrid = findViewById(R.id.calendar_grid)
    }

    private fun setupClickListeners() {
        previousButton.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        nextButton.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        expandCollapseButton.setOnClickListener {
            isExpanded = !isExpanded
            // Animate rotation: 0 degrees when collapsed (arrow down), 180 degrees when expanded (arrow up)
            expandCollapseButton.animate()
                .rotation(if (isExpanded) 180f else 0f)
                .setDuration(200)
                .start()
            updateCalendar()
        }
    }

    fun updateCalendar() {
        if (::monthYearText.isInitialized && ::calendarGrid.isInitialized) {
            monthYearText.text = monthYearFormat.format(calendar.time).capitalize()
            generateCalendarDays()
        }
    }

    fun setEventsForDates(dates: Set<String>) {
        datesWithEvents = dates
        updateCalendar()
    }

    private fun generateCalendarDays() {
        calendarGrid.removeAllViews()
        dayViews.clear()

        // Get first day of month
        val monthCalendar = calendar.clone() as Calendar
        monthCalendar.set(Calendar.DAY_OF_MONTH, 1)

        // Get first day of week (Monday = 2, Sunday = 1)
        val firstDayOfWeek = monthCalendar.get(Calendar.DAY_OF_WEEK)
        val startOffset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

        // Get today's date
        val today = Calendar.getInstance()
        val todayString = dayFormat.format(today.time)

        // Create all days for the month (6 weeks = 42 days)
        val allDays = mutableListOf<DayInfo>()

        // Add previous month days
        val prevMonthCalendar = monthCalendar.clone() as Calendar
        prevMonthCalendar.add(Calendar.MONTH, -1)
        val daysInPrevMonth = prevMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in startOffset - 1 downTo 0) {
            prevMonthCalendar.set(Calendar.DAY_OF_MONTH, daysInPrevMonth - i)
            allDays.add(
                DayInfo(
                    dayOfMonth = daysInPrevMonth - i,
                    dateString = dayFormat.format(prevMonthCalendar.time),
                    isCurrentMonth = false,
                    isToday = false
                )
            )
        }

        // Add current month days
        val daysInMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..daysInMonth) {
            monthCalendar.set(Calendar.DAY_OF_MONTH, day)
            val dayString = dayFormat.format(monthCalendar.time)
            allDays.add(
                DayInfo(
                    dayOfMonth = day,
                    dateString = dayString,
                    isCurrentMonth = true,
                    isToday = dayString == todayString
                )
            )
        }

        // Add next month days
        val nextMonthCalendar = monthCalendar.clone() as Calendar
        nextMonthCalendar.add(Calendar.MONTH, 1)
        val remainingDays = 42 - allDays.size
        for (day in 1..remainingDays) {
            nextMonthCalendar.set(Calendar.DAY_OF_MONTH, day)
            allDays.add(
                DayInfo(
                    dayOfMonth = day,
                    dateString = dayFormat.format(nextMonthCalendar.time),
                    isCurrentMonth = false,
                    isToday = false
                )
            )
        }

        // Determine which weeks to show
        val weeksToShow = if (isExpanded) {
            // Show all 6 weeks
            0 until 6
        } else {
            // Find the week containing today's date
            val currentWeek = findWeekContainingDate(allDays, todayString)
            currentWeek..currentWeek
        }

        // Create week rows
        for (week in weeksToShow) {
            val weekLayout = LinearLayout(context)
            weekLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            weekLayout.orientation = LinearLayout.HORIZONTAL

            for (day in 0 until 7) {
                val dayIndex = week * 7 + day
                val dayInfo = allDays[dayIndex]
                val dayView = createDayView(dayInfo)
                dayViews.add(dayView)
                weekLayout.addView(dayView)
            }

            calendarGrid.addView(weekLayout)
        }
    }

    private fun findWeekContainingDate(allDays: List<DayInfo>, dateString: String): Int {
        // Find which week (0-5) contains the given date
        for (week in 0 until 6) {
            for (day in 0 until 7) {
                val dayIndex = week * 7 + day
                if (allDays[dayIndex].dateString == dateString) {
                    return week
                }
            }
        }
        // Default to first week if not found
        return 0
    }

    private fun createDayView(dayInfo: DayInfo): TextView {
        val dayView = TextView(context)
        val layoutParams = LinearLayout.LayoutParams(0, 120)
        layoutParams.weight = 1f
        layoutParams.setMargins(4, 4, 4, 4)
        dayView.layoutParams = layoutParams

        dayView.text = dayInfo.dayOfMonth.toString()
        dayView.gravity = android.view.Gravity.CENTER
        dayView.textSize = 16f
        dayView.background = ContextCompat.getDrawable(context, R.drawable.agenda_day_selector)
        dayView.tag = dayInfo // Store day info in tag

        // Set text color and states
        when {
            !dayInfo.isCurrentMonth -> {
                dayView.setTextColor(ContextCompat.getColor(context, R.color.text_hint))
                dayView.alpha = 0.5f
            }
            dayInfo.dateString == selectedDate -> {
                dayView.setTextColor(ContextCompat.getColor(context, R.color.white))
                dayView.isSelected = true
            }
            dayInfo.isToday -> {
                dayView.setTextColor(ContextCompat.getColor(context, R.color.purple_primary))
                dayView.isActivated = true
            }
            else -> {
                dayView.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
        }

        // Add click listener
        dayView.setOnClickListener {
            if (dayInfo.isCurrentMonth) {
                selectDate(dayInfo.dateString)
                onDateSelectedListener?.invoke(dayInfo.dateString)
            }
        }

        return dayView
    }

    private fun selectDate(dateString: String) {
        selectedDate = dateString

        // Update all day views
        dayViews.forEach { dayView ->
            dayView.isSelected = false
            dayView.isActivated = false

            // Re-apply colors
            val tag = dayView.tag
            if (tag is DayInfo) {
                when {
                    !tag.isCurrentMonth -> {
                        dayView.setTextColor(ContextCompat.getColor(context, R.color.text_hint))
                        dayView.alpha = 0.5f
                    }
                    tag.dateString == selectedDate -> {
                        dayView.setTextColor(ContextCompat.getColor(context, R.color.white))
                        dayView.isSelected = true
                        dayView.alpha = 1.0f
                    }
                    tag.isToday -> {
                        dayView.setTextColor(ContextCompat.getColor(context, R.color.purple_primary))
                        dayView.isActivated = true
                        dayView.alpha = 1.0f
                    }
                    else -> {
                        dayView.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                        dayView.alpha = 1.0f
                    }
                }
            }
        }
    }

    data class DayInfo(
        val dayOfMonth: Int,
        val dateString: String,
        val isCurrentMonth: Boolean,
        val isToday: Boolean
    )
}