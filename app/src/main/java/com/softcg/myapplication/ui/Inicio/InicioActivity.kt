package com.softcg.myapplication.ui.Inicio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.widget.Toolbar
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.softcg.myapplication.R
import com.softcg.myapplication.ui.Inicio.Fragments.Home.HomeViewModel
import com.softcg.myapplication.ui.login.MainActivity
import com.softcg.myapplication.ui.tarea.TareaActivity
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class InicioActivity : AppCompatActivity() {

    private val inicioViewModel : InicioViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()


    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var addtarea:View
    private lateinit var addevento:View
    private lateinit var addcalificacion:View
    private var rotate=false


    companion object {
        fun create(context: Context): Intent =
            Intent(context, InicioActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initObserver()
        initNavegate()
        initfloatingBottons()
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController=findNavController(R.id.fragmentContainerView)
        return navController.navigateUp(appBarConfiguration)|| super.onSupportNavigateUp()
    }


    fun initNavegate(){
        toolbar=findViewById(R.id.myToolbar)
        setSupportActionBar(toolbar)

        drawerLayout=findViewById(R.id.drawer)
        navigationView=findViewById(R.id.nav_view)

        navController=findNavController(R.id.fragmentContainerView)

        val backButton: ImageButton = findViewById(R.id.backButton)
        if (navController.currentDestination?.id == R.id.id_home_fragment) {
            backButton.visibility = View.GONE
        } else {
            backButton.visibility = View.VISIBLE
        }

        val actionBarDrawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,  // Use toolbar reference here
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.id_home_fragment, R.id.id_diary_fragment, R.id.id_subjects_fragment,
                R.id.id_help_fragment, R.id.id_schedule_fragment, R.id.id_ratings_fragment),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navigationView.setupWithNavController(navController)

        val signoutItem:MenuItem = navigationView.menu.findItem(R.id.signout)

        signoutItem.setOnMenuItemClickListener {
            FirebaseAuth.getInstance().signOut()
            val intents=Intent(this, MainActivity::class.java)
            intents.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intents)
            finish()
            true
        }
    }

    fun initfloatingBottons(){

        addtarea= findViewById(R.id.addTarea)
        addevento= findViewById(R.id.addEvento)
        addcalificacion= findViewById(R.id.addCalificacion)
        initShowOut(addtarea)
        initShowOut(addevento)
        initShowOut(addcalificacion)
        val tareab = findViewById<FloatingActionButton>(R.id.tareaB)
        val eventob = findViewById<FloatingActionButton>(R.id.eventoB)
        val calificacionb = findViewById<FloatingActionButton>(R.id.calificacionB)
        val anadirb=findViewById<FloatingActionButton>(R.id.botonAñadir)

        anadirb.setOnClickListener{
            toggleFabMode(it)
        }
        tareab.setOnClickListener {
            inicioViewModel.onTareaSelected()
        }
        eventob.setOnClickListener {
            inicioViewModel.onEventoSelected()
        }
        calificacionb.setOnClickListener {
            inicioViewModel.onCalificacionSelected()
        }

    }


    private fun initObserver(){
        inicioViewModel._tareas.observe(this){
            homeViewModel.obtenerTareas()
        }
        inicioViewModel._eventos.observe(this){
            homeViewModel.obtenerEventos()
        }
        inicioViewModel.navigateToTarea.observe(this){
            it.getContentIfNotHandled()?.let {
                showDialogTarea()
                homeViewModel.obtenerTareas()
            }
        }
        inicioViewModel.navigateToEvento.observe(this){
            it.getContentIfNotHandled()?.let {
                showDialogEvento()
                homeViewModel.obtenerTareas()
            }
        }
        inicioViewModel.navigateToCalificacion.observe(this){
            it.getContentIfNotHandled()?.let {
                goToCalificacion()
            }
        }
    }

    private fun initShowOut(v: View){
        v.apply {
            visibility=View.GONE
            translationY=height.toFloat()
            alpha=0f
        }
    }

    private fun toggleFabMode(v:View){
        rotate = rotateFab(v,!rotate)
        if (rotate){
            showIn(addtarea)
            showIn(addevento)
            showIn(addcalificacion)
        }else{
            showOut(addtarea)
            showOut(addevento)
            showOut(addcalificacion)
        }
    }

    private fun showIn(view:View){
        view.apply {
            visibility=View.VISIBLE
            alpha=0f
            translationY=height.toFloat()
            animate()
                .setDuration(200)
                .translationY(0f)
                .setListener(object :AnimatorListenerAdapter(){})
                .alpha(1f)
                .start()
        }
    }
    private fun showOut(view: View){
        view.apply {
            visibility=View.VISIBLE
            alpha=0f
            translationY=height.toFloat()
            animate()
                .setDuration(200)
                .translationY(height.toFloat())
                .setListener(object :AnimatorListenerAdapter(){
                    override fun onAnimationEnd(animation: Animator) {
                        visibility=View.GONE
                        super.onAnimationEnd(animation)
                    }
                })
                .alpha(0f)
                .start()
        }

    }

    private fun rotateFab(v: View,rotate:Boolean):Boolean{
        v.animate()
            .setDuration(200)
            .setListener(object :AnimatorListenerAdapter() {})
            .rotation(if (rotate) 180f else 0f)
        return rotate
    }

    private fun goToCalificacion(){
        startActivity(TareaActivity.create(this))
    }

    private fun showDialogTarea(){
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.sheet_agregar_tarea)
        val titulo=dialog.findViewById<EditText>(R.id.NombreEditText)
        val descripcion=dialog.findViewById<EditText>(R.id.DescripcionEditText)
        val asignatura =dialog.findViewById<AutoCompleteTextView>(R.id.AsignaturaEditText)
        val fecha = dialog.findViewById<EditText>(R.id.FechaEditText)

        val guardarBoton= dialog.findViewById<Button>(R.id.botonAgregar)

        fecha.setOnClickListener {
            onClickScheduledDate(fecha)
        }
        guardarBoton.setOnClickListener {
            dialog.dismiss()
            inicioViewModel.onAgregarTareaSelected(titulo.text.toString(),descripcion.text.toString(),asignatura.text.toString(),fecha.text.toString())
            homeViewModel.obtenerTareas()
            Toast.makeText(this,"Asignatura guardada", Toast.LENGTH_SHORT).show()
        }
        dropDown(asignatura)
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.window?.setGravity(Gravity.BOTTOM)
    }

    private fun showDialogEvento(){
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.sheet_agregar_evento)
        val titulo=dialog.findViewById<EditText>(R.id.NombreEditText)
        val descripcion=dialog.findViewById<EditText>(R.id.DescripcionEditTextevent)
        val fecha = dialog.findViewById<EditText>(R.id.FechaEditTextevent)
        val guardarBoton= dialog.findViewById<Button>(R.id.botonAgregar)
        fecha.setOnClickListener {
            onClickScheduledDate(fecha)
        }
        guardarBoton.setOnClickListener {
            dialog.dismiss()
            inicioViewModel.onAgregarEventoSelected(titulo.text.toString(),descripcion.text.toString(),fecha.text.toString())
            homeViewModel.obtenerEventos()
            Toast.makeText(this,"Evento guardada", Toast.LENGTH_SHORT).show()
        }
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.window?.setGravity(Gravity.BOTTOM)
    }


    private fun onClickScheduledDate(fecha:EditText){
        val etScheduledDate= fecha
        val selectedCalendar= Calendar.getInstance()
        val year=selectedCalendar.get(Calendar.YEAR)
        val month= selectedCalendar.get(Calendar.MONTH)
        val dayOfMonth=selectedCalendar.get(Calendar.DAY_OF_MONTH)
        val listener= DatePickerDialog.OnDateSetListener{datePicker,y,m,d-> etScheduledDate.setText("$y-${m+1}-$d")}
        DatePickerDialog(this, R.style.CustomDatePickerDialogTheme,listener,year, month, dayOfMonth).show()
    }


    private fun dropDown (asignatura: AutoCompleteTextView){
        inicioViewModel.obtenerAsignaturas()
        val items :List<String> = inicioViewModel._asignaturas.value!!
        val autoComplete:AutoCompleteTextView = asignatura
        val adapter = ArrayAdapter(this,R.layout.item_menu_asignatura, items)
        autoComplete.setAdapter(adapter)
    }

}
