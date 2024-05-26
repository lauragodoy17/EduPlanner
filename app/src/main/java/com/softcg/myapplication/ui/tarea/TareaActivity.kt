package com.softcg.myapplication.ui.tarea

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.dismissKeyboard
import com.softcg.myapplication.ui.Inicio.InicioActivity
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class TareaActivity : AppCompatActivity() {

    private val tareaViewModel:TareaViewModel by viewModels()

    companion object {
        fun create(context: Context): Intent =
            Intent(context, TareaActivity ::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tarea)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainT)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        init()
    }

    private fun init(){
        initlisteners()
        initObservers()
        dropdown()
    }

    private fun initlisteners(){


        findViewById<Button>(R.id.botonAgregarTarea).setOnClickListener {
            it.dismissKeyboard()
            tareaViewModel.onAgregarTareaSelected(
                findViewById<EditText>(R.id.TituloEditText).text.toString(),
                findViewById<EditText>(R.id.DescripcionEditText).text.toString(),
                findViewById<EditText>(R.id.AsignaturaEditText).text.toString(),
                findViewById<EditText>(R.id.FechaEditText).text.toString()
            )
            Toast.makeText(this,"Tarea agregada", Toast.LENGTH_SHORT).show()
            goToHome()
        }

        findViewById<ImageButton>(R.id.backButton).setOnClickListener{
            it.dismissKeyboard()
            tareaViewModel.onBackSelected()
        }

    }

    fun initObservers(){
        tareaViewModel.navigateToHome.observe(this){
            it.getContentIfNotHandled()?.let {
                goToHome()
            }
        }
    }

    private fun dropdown (){
        tareaViewModel.obtenerAsignaturas()
        val items :List<String> = tareaViewModel._asignaturas.value!!
        val autoComplete:AutoCompleteTextView = findViewById(R.id.AsignaturaEditText)
        val adapter = ArrayAdapter(this,R.layout.item_menu_asignatura, items)
        autoComplete.setAdapter(adapter)
    }

    fun onClickScheduledDate(v: View?){
        val etScheduledDate= findViewById<EditText>(R.id.FechaEditText)
        val selectedCalendar= Calendar.getInstance()
        val year=selectedCalendar.get(Calendar.YEAR)
        val month= selectedCalendar.get(Calendar.MONTH)
        val dayOfMonth=selectedCalendar.get(Calendar.DAY_OF_MONTH)
        val listener= DatePickerDialog.OnDateSetListener{datePicker,y,m,d-> etScheduledDate.setText("$y-${m+1}-$d")}
        DatePickerDialog(this, R.style.CustomDatePickerDialogTheme,listener,year, month, dayOfMonth).show()
    }
    fun goToHome(){
        startActivity(InicioActivity.create(this))
    }
}
