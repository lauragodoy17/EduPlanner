package com.softcg.myapplication.ui.Inicio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.appcompat.widget.Toolbar
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
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
import com.softcg.myapplication.ui.Inicio.Fragments.Agenda.AgendaViewModel
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.CalificacionesViewModel
import com.softcg.myapplication.ui.Inicio.Dialogs.CalificacionDialog
import com.softcg.myapplication.ui.Inicio.Dialogs.EventoDialog
import com.softcg.myapplication.ui.Inicio.Dialogs.TareaDialog
import com.softcg.myapplication.ui.Inicio.Fragments.Home.HomeViewModel
import com.softcg.myapplication.ui.login.MainActivity
import com.softcg.myapplication.ui.notifications.NotificationScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InicioActivity : AppCompatActivity() {

    private val inicioViewModel : InicioViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val agendaViewModel:AgendaViewModel by viewModels()
    private val calificacionesViewModel :CalificacionesViewModel by viewModels()


    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var addtarea:View
    private lateinit var addevento:View
    private lateinit var addcalificacion:View
    private lateinit var leerqr:View
    private var rotate=false

    private var eventoDialog: EventoDialog? = null
    private val seleccionarImagen = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { eventoDialog?.mostrarImagen(it) }
    }


    companion object {
        fun create(context: Context): Intent =
            Intent(context, InicioActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        // Make system bars transparent
        makeSystemBarsTransparent()
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Only apply left and right padding, not top/bottom since we want transparency
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        // Apply dynamic padding to header based on status bar height
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header_card)) { v, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val headerLayout = (v as androidx.cardview.widget.CardView).getChildAt(0) as LinearLayout

            // Maintain existing left, right, bottom padding, but adjust top padding dynamically
            val existingPaddingLeft = headerLayout.paddingLeft
            val existingPaddingRight = headerLayout.paddingRight
            val existingPaddingBottom = headerLayout.paddingBottom

            // Add status bar height to base padding (20dp)
            val basePaddingDp = 0
            val basePaddingPx = (basePaddingDp * resources.displayMetrics.density).toInt()
            val dynamicTopPadding = statusBarInsets.top + basePaddingPx

            headerLayout.setPadding(existingPaddingLeft, dynamicTopPadding, existingPaddingRight, existingPaddingBottom)
            insets
        }
        initObserver()
        initNavegate()
        initfloatingBottons()
        setupHamburgerMenu()
        setupBottomNavigation()
        NotificationScheduler.createChannel(this)
        NotificationScheduler.scheduleDailyReminders(this, homeViewModel._tareas.value?.size.toString())
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
        leerqr= findViewById(R.id.leerQR)
        initShowOut(addtarea)
        initShowOut(addevento)
        initShowOut(addcalificacion)
        initShowOut(leerqr)
        val tareab = findViewById<FloatingActionButton>(R.id.tareaB)
        val eventob = findViewById<FloatingActionButton>(R.id.eventoB)
        val calificacionb = findViewById<FloatingActionButton>(R.id.calificacionB)
        val qrb = findViewById<FloatingActionButton>(R.id.qrB)
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
            showDialogCalificacion()
        }
        qrb.setOnClickListener {
            // TODO: Implementar funcionalidad de lector QR
            Toast.makeText(this, "Lector QR - Próximamente", Toast.LENGTH_SHORT).show()
        }

    }

    private fun setupHamburgerMenu() {
        val hamburgerMenu = findViewById<ImageView>(R.id.menu_hamburger)
        hamburgerMenu?.setOnClickListener {
            try {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    drawerLayout.openDrawer(GravityCompat.START)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupBottomNavigation() {
        val navHome = findViewById<LinearLayout>(R.id.nav_home)
        val navCalendar = findViewById<LinearLayout>(R.id.nav_calendar)
        val navProfile = findViewById<LinearLayout>(R.id.nav_profile)

        navHome.setOnClickListener {
            navController.navigate(R.id.id_home_fragment)
            updateBottomNavigationSelection(navHome, navCalendar, navProfile)
        }

        navCalendar.setOnClickListener {
            navController.navigate(R.id.id_diary_fragment)
            updateBottomNavigationSelection(navCalendar, navHome, navProfile)
        }

        navProfile.setOnClickListener {
            // Navigate to profile or settings fragment if it exists
            // For now, you can add a profile fragment to the navigation graph
        }

        // Set initial state - home should be selected by default
        updateBottomNavigationSelection(navHome, navCalendar, navProfile)

        // Listen for navigation changes to update selection state
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.id_home_fragment -> {
                    updateBottomNavigationSelection(navHome, navCalendar, navProfile)
                }
                R.id.id_diary_fragment -> {
                    updateBottomNavigationSelection(navCalendar, navHome, navProfile)
                }
            }
        }
    }

    private fun updateBottomNavigationSelection(selected: LinearLayout, vararg others: LinearLayout) {
        // Set selected state
        selected.alpha = 1.0f
        selected.setBackgroundColor(getColor(R.color.purple_light))

        // Set unselected state
        others.forEach { nav ->
            nav.alpha = 0.5f
            nav.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
    }

    private fun initObserver(){
        inicioViewModel._tareas.observe(this){
            CoroutineScope(Dispatchers.Main).launch {
                homeViewModel.obtenerTareas()
                agendaViewModel.obtenerAgendaList()
            }
        }
        inicioViewModel._eventos.observe(this){
            CoroutineScope(Dispatchers.Main).launch {
                homeViewModel.obtenerEventos()
                agendaViewModel.obtenerAgendaList()
            }
        }
        inicioViewModel._calificaciones.observe(this){
            CoroutineScope(Dispatchers.Main).launch {
                calificacionesViewModel.obtenerCalificaciones()
            }
        }
        inicioViewModel.navigateToTarea.observe(this){
            it.getContentIfNotHandled()?.let {
                CoroutineScope(Dispatchers.Main).launch {
                    showDialogTarea()
                }
            }
        }
        inicioViewModel.navigateToEvento.observe(this){
            it.getContentIfNotHandled()?.let {
                showDialogEvento()
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
            showIn(leerqr)
        }else{
            showOut(addtarea)
            showOut(addevento)
            showOut(addcalificacion)
            showOut(leerqr)
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


    private fun showDialogTarea(){
        TareaDialog(this, obtenerNombresAsignaturas()) { titulo, descrip, asignatura, fecha, prioridad ->
            inicioViewModel.onAgregarTareaSelected(titulo, descrip, asignatura, fecha, prioridad)
            homeViewModel.obtenerTareas()
            Toast.makeText(this,"Tarea guardada", Toast.LENGTH_SHORT).show()
        }.show()
    }

    private fun showDialogEvento(){
        eventoDialog = EventoDialog(
            context = this,
            onSeleccionarImagen = { seleccionarImagen.launch("image/*") },
            onGuardar = { evento ->
                inicioViewModel.onAgregarEventoSelected(
                    evento.titulo,
                    evento.descrip,
                    evento.fecha,
                    evento.prioridad,
                    evento.horaInicio,
                    evento.horaFin,
                    evento.imagenUri
                )
                homeViewModel.obtenerEventos()
                Toast.makeText(this, "Evento guardado", Toast.LENGTH_SHORT).show()
            }
        ).also { it.show() }
    }

    private fun showDialogCalificacion(){
        CalificacionDialog(this, obtenerNombresAsignaturas()) { tipo, valor, asignatura, descripcion ->
            inicioViewModel.onAgregarCalificacionSelected(tipo, valor, asignatura, descripcion)
            calificacionesViewModel.obtenerCalificaciones()
            Toast.makeText(this,"Calificación guardada", Toast.LENGTH_SHORT).show()
        }.show()
    }

    private fun obtenerNombresAsignaturas(): List<String> {
        inicioViewModel.obtenerAsignaturas()
        return inicioViewModel._asignaturas.value.orEmpty()
    }

    private fun makeSystemBarsTransparent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Make status bar transparent
            window.statusBarColor = Color.TRANSPARENT
            
            // Set light status bar icons for better visibility
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                )
            } else {
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
            }
        }
    }

}
