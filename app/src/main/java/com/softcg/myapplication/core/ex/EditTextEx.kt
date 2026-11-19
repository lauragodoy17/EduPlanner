package com.softcg.myapplication.core.ex

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.softcg.myapplication.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

fun EditText.onTextChanged(listener: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            listener(s.toString())
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}

fun EditText.loseFocusAfterAction(action: Int) {
    this.setOnEditorActionListener { v, actionId, _ ->
        if (actionId == action) {
            this.dismissKeyboard()
            v.clearFocus()
        }
        return@setOnEditorActionListener false
    }
}

/**
 * Abre un selector de fecha y escribe el resultado con formato yyyy-MM-dd. Empieza en la fecha que ya tiene el campo
 * (al editar) o en hoy; [minima]/[maxima] limitan los días que se pueden elegir.
 */
fun EditText.showDatePicker(minima: LocalDate? = null, maxima: LocalDate? = null) {
    val actual = runCatching { LocalDate.parse(text.toString()) }.getOrNull() ?: LocalDate.now()
    val inicial = actual.let { f -> if (minima != null && f.isBefore(minima)) minima else f }
        .let { f -> if (maxima != null && f.isAfter(maxima)) maxima else f }
    val selector = DatePickerDialog(
        context,
        R.style.Edu_TemaSelector,
        { _, year, month, dayOfMonth ->
            setText(formatFecha(year, month, dayOfMonth))
        },
        inicial.year,
        inicial.monthValue - 1,
        inicial.dayOfMonth
    )
    val zona = ZoneId.systemDefault()
    minima?.let { selector.datePicker.minDate = it.atStartOfDay(zona).toInstant().toEpochMilli() }
    maxima?.let { selector.datePicker.maxDate = it.atTime(23, 59).atZone(zona).toInstant().toEpochMilli() }
    selector.show()
}

/** Abre un selector de hora en formato 24 h y escribe el resultado con formato HH:mm. Empieza en la hora del campo, si la tiene. */
fun EditText.showTimePicker(onElegida: (String) -> Unit = {}) {
    val actual = runCatching { LocalTime.parse(text.toString()) }.getOrNull() ?: LocalTime.now()
    TimePickerDialog(
        context,
        { _, hour, minute ->
            val hora = formatHora(hour, minute)
            setText(hora)
            onElegida(hora)
        },
        actual.hour,
        actual.minute,
        true
    ).show()
}

fun formatFecha(year: Int, month: Int, dayOfMonth: Int): String =
    String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)

fun formatHora(hour: Int, minute: Int): String =
    String.format(Locale.US, "%02d:%02d", hour, minute)