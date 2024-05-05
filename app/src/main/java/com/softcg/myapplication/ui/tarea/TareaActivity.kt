package com.softcg.myapplication.ui.tarea

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
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.dismissKeyboard
import com.softcg.myapplication.core.ex.loseFocusAfterAction
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.data.database.TareasDatabase.TareasDatabase
import com.softcg.myapplication.data.database.dao.TareasDao
import com.softcg.myapplication.databinding.ActivityTareaBinding
import com.softcg.myapplication.ui.home.HomeActivity
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import javax.inject.Inject

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
    }

    private fun initlisteners(){
        findViewById<EditText>(R.id.TituloEditText).loseFocusAfterAction(EditorInfo.IME_ACTION_NEXT)
        findViewById<EditText>(R.id.AsignaturaEditText).loseFocusAfterAction(EditorInfo.IME_ACTION_NEXT)
        findViewById<EditText>(R.id.DescripcionEditText).loseFocusAfterAction(EditorInfo.IME_ACTION_NEXT)
        findViewById<EditText>(R.id.FechaEditText).loseFocusAfterAction(EditorInfo.IME_ACTION_NEXT)

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

    fun onClickScheduledDate(v: View?){
        val etScheduledDate= findViewById<EditText>(R.id.FechaEditText)
        val selectedCalendar= Calendar.getInstance()
        val year=selectedCalendar.get(Calendar.YEAR)
        val month= selectedCalendar.get(Calendar.MONTH)
        val dayOfMonth=selectedCalendar.get(Calendar.DAY_OF_MONTH)
        val listener= DatePickerDialog.OnDateSetListener{datePicker,y,m,d-> etScheduledDate.setText("$y-$m-$d")}
        DatePickerDialog(this, R.style.CustomDatePickerDialogTheme,listener,year, month, dayOfMonth).show()
    }
    fun goToHome(){
        startActivity(HomeActivity.create(this))
    }
}
