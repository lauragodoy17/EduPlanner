package com.softcg.myapplication.core.dialog

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.formatHora
import java.util.Calendar

class TimePickerFragment(val listener: (String)-> Unit):DialogFragment(), TimePickerDialog.OnTimeSetListener {
    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        listener(formatHora(hourOfDay, minute))
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val calendar: Calendar= Calendar.getInstance()
        val hour: Int= calendar.get(Calendar.HOUR_OF_DAY)
        val minute: Int= calendar.get(Calendar.MINUTE)
        val dialog= TimePickerDialog(activity as Context, R.style.Edu_TemaSelector, this, hour,minute,true)
        return dialog
    }
}