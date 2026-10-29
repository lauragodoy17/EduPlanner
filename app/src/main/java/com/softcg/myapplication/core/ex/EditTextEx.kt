package com.softcg.myapplication.core.ex

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.softcg.myapplication.R
import java.util.Calendar
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

/** Abre un selector de fecha y escribe el resultado con formato yyyy-MM-dd. */
fun EditText.showDatePicker() {
    val calendar = Calendar.getInstance()
    DatePickerDialog(
        context,
        R.style.CustomDatePickerDialogTheme,
        { _, year, month, dayOfMonth ->
            setText(formatFecha(year, month, dayOfMonth))
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

/** Abre un selector de hora en formato 24 h y escribe el resultado con formato HH:mm. */
fun EditText.showTimePicker() {
    val calendar = Calendar.getInstance()
    TimePickerDialog(
        context,
        { _, hour, minute ->
            setText(formatHora(hour, minute))
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    ).show()
}

/** [month] empieza en 0, como en [Calendar]. */
fun formatFecha(year: Int, month: Int, dayOfMonth: Int): String =
    String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)

fun formatHora(hour: Int, minute: Int): String =
    String.format(Locale.US, "%02d:%02d", hour, minute)