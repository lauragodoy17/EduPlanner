package com.softcg.myapplication.ui.evento

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
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
import com.softcg.myapplication.core.ex.loseFocusAfterAction
import com.softcg.myapplication.ui.Inicio.InicioActivity
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class EventoActivity  : AppCompatActivity() {

    private val eventoViewModel:EventoViewModel by viewModels()

    companion object{
        fun create(context: Context): Intent =
            Intent(context,EventoActivity ::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_evento)
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
    }

    private fun initlisteners() {
        findViewById<EditText>(R.id.TituloEditTextevent).loseFocusAfterAction(EditorInfo.IME_ACTION_NEXT)
        findViewById<EditText>(R.id.FechaEditTextevent).loseFocusAfterAction(EditorInfo.IME_ACTION_NEXT)
        findViewById<EditText>(R.id.DescripcionEditTextevent).loseFocusAfterAction(EditorInfo.IME_ACTION_NEXT)

        findViewById<Button>(R.id.botonAgregarEvento).setOnClickListener {
            it.dismissKeyboard()
            eventoViewModel.onAgregarEventoSelected(
                findViewById<EditText>(R.id.TituloEditTextevent).text.toString(),
                findViewById<EditText>(R.id.DescripcionEditTextevent).text.toString(),
                findViewById<EditText>(R.id.FechaEditTextevent).text.toString()
            )
            Toast.makeText(this, "Evento agregado", Toast.LENGTH_SHORT).show()
            goToHome()
        }

        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            it.dismissKeyboard()
            eventoViewModel.onBackSelected()
        }
    }

    fun initObservers(){
        eventoViewModel.navigateToHome.observe(this){
            it.getContentIfNotHandled()?.let {
                goToHome()
            }
        }
    }

    fun onClickScheduledDate(v: View?){
        val etScheduledDate= findViewById<EditText>(R.id.FechaEditTextevent)
        val selectedCalendar= Calendar.getInstance()
        val year=selectedCalendar.get(Calendar.YEAR)
        val month= selectedCalendar.get(Calendar.MONTH)+1
        val dayOfMonth=selectedCalendar.get(Calendar.DAY_OF_MONTH)
        val listener= DatePickerDialog.OnDateSetListener{ datePicker, y, m, d-> etScheduledDate.setText("$y-$m-$d")}
        DatePickerDialog(this, R.style.CustomDatePickerDialogTheme,listener,year, month, dayOfMonth).show()
    }

    fun goToHome(){
        startActivity(InicioActivity.create(this))
    }

}