package com.softcg.myapplication.ui.Inicio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
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
import com.softcg.myapplication.core.ex.mostrarAvisoLogro
import com.softcg.myapplication.core.ex.mostrarMensaje
import com.softcg.myapplication.core.ex.mostrarRecompensa
import com.softcg.myapplication.domain.gamificacion.Celebracion
import com.softcg.myapplication.domain.gamificacion.ClaseProgramada
import com.softcg.myapplication.domain.gamificacion.ResultadoAsistencia
import com.softcg.myapplication.ui.Inicio.Dialogs.AsistenciaDialog
import com.softcg.myapplication.ui.Inicio.Dialogs.CalificacionDialog
import com.softcg.myapplication.ui.Inicio.Dialogs.EventoDialog
import com.softcg.myapplication.ui.Inicio.Dialogs.SubidaNivelDialog
import com.softcg.myapplication.ui.Inicio.Dialogs.TareaDialog
import com.softcg.myapplication.ui.Inicio.Fragments.Home.HomeViewModel
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.ProfileFragment
import com.softcg.myapplication.ui.login.MainActivity
import com.softcg.myapplication.ui.notifications.EnfoqueNotification
import com.softcg.myapplication.ui.notifications.NotificationScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
        private const val MAX_AVISOS_LOGRO = 3

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
        iniciarComprobacionAsistencia()
        abrirEnfoqueSiSePidio(intent)
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
                R.id.id_help_fragment, R.id.id_schedule_fragment, R.id.id_ratings_fragment,
                R.id.id_profile_fragment),
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
            abrirPerfil()
        }

        // El saldo de la cabecera también lleva al perfil
        findViewById<View>(R.id.coin_counter).setOnClickListener { abrirPerfil() }
        findViewById<View>(R.id.coin_icon).setOnClickListener { abrirPerfil() }

        // Set initial state - home should be selected by default
        updateBottomNavigationSelection(navHome, navCalendar, navProfile)

        // Listen for navigation changes to update selection state
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // En una sesión de enfoque no se ofrece agregar nada: es la distracción que se quiere evitar
            val anadir = findViewById<View>(R.id.botonAñadir)
            val enEnfoque = destination.id == R.id.id_focus_fragment
            if (enEnfoque && rotate) toggleFabMode(anadir)
            anadir.visibility = if (enEnfoque) View.GONE else View.VISIBLE

            when (destination.id) {
                R.id.id_home_fragment -> {
                    updateBottomNavigationSelection(navHome, navCalendar, navProfile)
                }
                R.id.id_diary_fragment -> {
                    updateBottomNavigationSelection(navCalendar, navHome, navProfile)
                }
                R.id.id_profile_fragment, R.id.id_shop_fragment, R.id.id_focus_fragment -> {
                    updateBottomNavigationSelection(navProfile, navHome, navCalendar)
                }
            }
        }
    }

    private fun abrirPerfil(pestana: Int = 0) {
        // Con una pestaña concreta se vuelve a abrir aunque ya esté en el perfil
        if (navController.currentDestination?.id != R.id.id_profile_fragment || pestana != 0) {
            navController.navigate(R.id.id_profile_fragment, bundleOf(ProfileFragment.ARG_PESTANA to pestana))
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
        inicioViewModel.saldo.observe(this){ saldo -> actualizarContadorMonedas(saldo) }
        inicioViewModel.recompensa.observe(this){
            it.getContentIfNotHandled()?.let { cantidad -> mostrarRecompensa(cantidad) }
        }
        inicioViewModel.resultadoAsistencia.observe(this){
            it.getContentIfNotHandled()?.let { (clase, resultado) -> onResultadoAsistencia(clase, resultado) }
        }
        // Cualquier movimiento, venga de la pantalla que venga, puede completar un logro o subir de nivel
        inicioViewModel.totalMovimientos.observe(this){ inicioViewModel.evaluarProgreso() }
        inicioViewModel.celebraciones.observe(this){
            it.getContentIfNotHandled()?.let { nuevas -> encolarCelebraciones(nuevas) }
        }
        // El observador mantiene el resumen del inicio al día aunque esa pantalla no esté visible
        inicioViewModel.panelProgreso.observe(this){ }
    }

    private var saldoMostrado: Int? = null

    /** La primera lectura se muestra tal cual; los cambios posteriores se cuentan y el contador pulsa. */
    private fun actualizarContadorMonedas(saldo: Int) {
        val contador = findViewById<TextView>(R.id.coin_counter)
        val anterior = saldoMostrado
        saldoMostrado = saldo
        if (anterior == null || anterior == saldo) {
            contador.text = saldo.toString()
            return
        }
        ValueAnimator.ofInt(anterior, saldo).apply {
            duration = 600
            addUpdateListener { contador.text = (it.animatedValue as Int).toString() }
            start()
        }
        val moneda = findViewById<View>(R.id.coin_icon)
        moneda.animate().scaleX(1.35f).scaleY(1.35f).setDuration(150).withEndAction {
            moneda.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
        }.start()
    }

    // CELEBRACIONES: de una en una, para que un logro no tape a otro ni a la subida de nivel.

    private val colaCelebraciones = ArrayDeque<Celebracion>()
    private var celebrando = false

    private fun encolarCelebraciones(nuevas: List<Celebracion>) {
        val logros = nuevas.filterIsInstance<Celebracion.LogroDesbloqueado>()
        colaCelebraciones += nuevas.filterIsInstance<Celebracion.SubidaNivel>()
        if (logros.size > MAX_AVISOS_LOGRO) {
            // Muchos a la vez (p. ej. datos anteriores a los logros): un solo aviso en vez de una larga fila
            mostrarResumenLogros(logros.size)
        } else {
            colaCelebraciones += logros
        }
        mostrarSiguienteCelebracion()
    }

    private fun mostrarResumenLogros(cantidad: Int) {
        mostrarAvisoLogro(null, "$cantidad logros conseguidos", "Míralos en tu perfil",
            onClick = { abrirPerfil(ProfileFragment.PESTANA_LOGROS) },
            onFin = { })
    }

    private fun mostrarSiguienteCelebracion() {
        if (celebrando || isFinishing || isDestroyed) return
        val siguiente = colaCelebraciones.removeFirstOrNull() ?: return
        celebrando = true
        val alTerminar = {
            celebrando = false
            mostrarSiguienteCelebracion()
        }
        when (siguiente) {
            is Celebracion.SubidaNivel -> SubidaNivelDialog(this, siguiente, alTerminar).show()
            is Celebracion.LogroDesbloqueado -> {
                val logro = siguiente.logro
                val detalle = buildString {
                    append("Logro · +${logro.rareza.recompensa} EduCoins")
                    siguiente.accesorio?.let { append(" · ${it.nombre}") }
                }
                mostrarAvisoLogro(logro, logro.titulo, detalle,
                    onClick = { abrirPerfil(ProfileFragment.PESTANA_LOGROS) },
                    onFin = alTerminar)
            }
        }
    }

    /** Si la app se abrió desde la notificación de fin de enfoque, lleva a esa pantalla para cobrar. */
    private fun abrirEnfoqueSiSePidio(intent: Intent?) {
        if (intent?.getBooleanExtra(EnfoqueNotification.EXTRA_ABRIR_ENFOQUE, false) == true) {
            intent.removeExtra(EnfoqueNotification.EXTRA_ABRIR_ENFOQUE)
            abrirEnfoque()
        }
    }

    /** Sin duplicar la pantalla: una copia debajo cobraría la sesión sin que se viera el resultado. */
    private fun abrirEnfoque() {
        if (navController.currentDestination?.id != R.id.id_focus_fragment) {
            navController.navigate(R.id.id_focus_fragment)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        abrirEnfoqueSiSePidio(intent)
    }

    override fun onResume() {
        super.onResume()
        // Pudo cambiar el día mientras la app estaba en segundo plano
        inicioViewModel.refrescarProgreso()
        lifecycleScope.launch {
            if (inicioViewModel.hayEnfoquePorCobrar()) abrirEnfoque()
        }
    }

    // ASISTENCIA: único punto de comprobación. Se revisa al volver a la app y cada minuto mientras está
    // abierta; las clases pendientes se preguntan de una en una, empezando por la que terminó antes.

    private var asistenciaDialog: AsistenciaDialog? = null
    private var buscandoAsistencia = false

    private fun iniciarComprobacionAsistencia() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    comprobarAsistencia()
                    delay(60_000)
                }
            }
        }
    }

    private fun comprobarAsistencia() {
        asistenciaDialog?.let { dialogo ->
            if (inicioViewModel.plazoVencido(dialogo.clase)) {
                dialogo.cerrar()
                mostrarMensaje("Venció el plazo para confirmar la asistencia a ${dialogo.clase.nombre}")
            }
            return
        }
        if (buscandoAsistencia) return
        buscandoAsistencia = true
        lifecycleScope.launch {
            try {
                val clase = inicioViewModel.siguienteClasePorConfirmar()
                if (clase != null && asistenciaDialog == null && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    mostrarDialogoAsistencia(clase)
                }
            } finally {
                buscandoAsistencia = false
            }
        }
    }

    private fun mostrarDialogoAsistencia(clase: ClaseProgramada) {
        asistenciaDialog = AsistenciaDialog(
            context = this,
            clase = clase,
            onResponder = { asistio -> inicioViewModel.confirmarAsistencia(clase, asistio) },
            onMasTarde = { inicioViewModel.posponerAsistencia(clase) },
            onCerrado = {
                asistenciaDialog = null
                comprobarAsistencia()
            }
        ).also { it.show() }
    }

    private fun onResultadoAsistencia(clase: ClaseProgramada, resultado: ResultadoAsistencia) {
        val dialogo = asistenciaDialog?.takeIf { it.clase == clase }
        if (resultado is ResultadoAsistencia.Error || resultado is ResultadoAsistencia.SinSesion) {
            // No se guardó nada: se puede volver a intentar
            dialogo?.habilitar()
            mostrarMensaje("No se pudo guardar la asistencia. Inténtalo de nuevo")
            return
        }
        dialogo?.cerrar()
        when (resultado) {
            is ResultadoAsistencia.Registrada -> when {
                resultado.monedas > 0 -> mostrarRecompensa(resultado.monedas)
                resultado.asistio -> mostrarMensaje("Asistencia registrada")
                else -> mostrarMensaje("Registramos que no asististe a ${clase.nombre}")
            }
            ResultadoAsistencia.YaRespondida -> mostrarMensaje("Ya habías respondido la asistencia de esta clase")
            ResultadoAsistencia.PlazoVencido -> mostrarMensaje("Venció el plazo para confirmar esta clase")
            ResultadoAsistencia.AunNoTermina -> mostrarMensaje("La clase aún no termina")
            ResultadoAsistencia.ClaseNoExiste -> mostrarMensaje("Esta clase ya no está registrada")
            else -> {}
        }
    }

    override fun onStop() {
        super.onStop()
        // Al salir de la app, las clases pospuestas se vuelven a preguntar en la siguiente visita
        if (!isChangingConfigurations) inicioViewModel.reiniciarPospuestas()
    }

    override fun onDestroy() {
        val dialogo = asistenciaDialog
        asistenciaDialog = null
        dialogo?.cerrar()
        super.onDestroy()
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
            // La lista se recarga sola al terminar de guardar; aquí solo se muestra el día de la tarea
            homeViewModel.mostrarFecha(fecha)
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
                homeViewModel.mostrarFecha(evento.fecha)
                Toast.makeText(this, "Evento guardado", Toast.LENGTH_SHORT).show()
            }
        ).also { it.show() }
    }

    private fun showDialogCalificacion(){
        CalificacionDialog(this, obtenerNombresAsignaturas()) { tipo, valor, asignatura, descripcion ->
            inicioViewModel.onAgregarCalificacionSelected(tipo, valor, asignatura, descripcion)
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
