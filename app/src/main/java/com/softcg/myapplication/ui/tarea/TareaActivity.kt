package com.softcg.myapplication.ui.tarea

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.softcg.myapplication.R
import com.softcg.myapplication.ui.home.HomeActivity
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class TareaActivity : AppCompatActivity() {

    companion object {
        fun create(context: Context): Intent =
            Intent(context, TareaActivity ::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tarea)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    fun onClickScheduledDate(v: View?){
        val etScheduledDate= findViewById<EditText>(R.id.FechaEditText)
        val selectedCalendar= Calendar.getInstance()
        val year=selectedCalendar.get(Calendar.YEAR)
        val month= selectedCalendar.get(Calendar.MONTH)
        val dayOfMonth=selectedCalendar.get(Calendar.DAY_OF_MONTH)
        val listener= DatePickerDialog.OnDateSetListener{datePicker,y,m,d-> etScheduledDate.setText("$y-$m-$d")}
        DatePickerDialog(this, R.style.CustomDatePickerDialogTheme,listener,year, month, dayOfMonth).show()

    }
}