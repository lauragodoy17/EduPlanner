package com.softcg.myapplication.ui.Inicio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.widget.Toolbar
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.softcg.myapplication.R
import com.softcg.myapplication.ui.Inicio.Fragments.Agenda.AgendaViewModel
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.CalificacionesViewModel
import com.softcg.myapplication.core.ex.mostrarAvisoLogro
import com.softcg.myapplication.data.network.CredencialGoogleService
import javax.inject.Inject
import com.softcg.myapplication.core.ex.copiarImagenEvento
import com.softcg.myapplication.core.ex.mostrarMensaje
import com.softcg.myapplication.core.ex.mostrarRecompensa
import com.softcg.myapplication.domain.ResultadoCrearAsignatura
import com.softcg.myapplication.domain.compartir.ResultadoImportacion
import com.softcg.myapplication.domain.compartir.TextosCompartir
import com.softcg.myapplication.domain.compartir.TipoCompartido
import com.softcg.myapplication.ui.compartir.CompartirViewModel
import com.softcg.myapplication.ui.tema.TemaDialog
import com.softcg.myapplication.ui.tema.TemaViewModel
import com.softcg.myapplication.ui.tema.ajustarBarrasDelSistema
import com.softcg.myapplication.ui.tema.aplicarTemaElegido
import com.softcg.myapplication.ui.Inicio.Fragments.Detalle.DetalleFragment
import com.softcg.myapplication.ui.Inicio.Fragments.Detalle.TipoDetalle
import com.softcg.myapplication.ui.compartir.EscanerQrActivity
import com.softcg.myapplication.domain.prediccion.CatalogoModelo
import com.softcg.myapplication.domain.gamificacion.Celebracion
import com.softcg.myapplication.domain.gamificacion.ClaseProgramada
import com.softcg.myapplication.domain.gamificacion.ResultadoAsistencia
import com.softcg.myapplication.ui.Inicio.Dialogs.AreaAsignaturaDialog
import com.softcg.myapplication.ui.Inicio.Dialogs.ImportarCompartidoDialog
import com.softcg.myapplication.ui.Inicio.Dialogs.AsignaturaDialog
import com.softcg.myapplication.ui.Inicio.Dialogs.mensajeError
import com.softcg.myapplication.ui.Inicio.Dialogs.AsistenciaDialog
import com.softcg.myapplication.ui.Inicio.Dialogs.CalificacionDialog
import com.softcg.myapplication.ui.Inicio.Dialogs.EventoDialog
import com.softcg.myapplication.ui.Inicio.Dialogs.SubidaNivelDialog
import com.softcg.myapplication.ui.Inicio.Dialogs.TareaDialog
import com.softcg.myapplication.ui.Inicio.Fragments.Home.HomeViewModel
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.ProfileFragment
import com.softcg.myapplication.ui.login.MainActivity
import com.softcg.myapplication.ui.notifications.EnfoqueNotification
import com.softcg.myapplication.ui.notifications.CanalesNotificacion
import com.softcg.myapplication.ui.notifications.IntentsAvisos
import com.softcg.myapplication.ui.notifications.VigilanteAvisos
import com.softcg.myapplication.data.preferencias.PreferenciasAvisos
import com.softcg.myapplication.ui.Inicio.Fragments.Prediccion.PrediccionFragment
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.softcg.myapplication.core.ex.preguntar
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
    private val datosEstudio: DatosEstudioViewModel by viewModels()

    @Inject
    lateinit var credencialGoogle: CredencialGoogleService

    @Inject lateinit var vigilanteAvisos: VigilanteAvisos
    @Inject lateinit var preferenciasAvisos: PreferenciasAvisos

    /** Android 13+: sin este permiso no se ve ningún aviso (clases, tareas, asistencia). */
    private val pedirPermisoNotificaciones = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (!concedido) mostrarMensaje("Sin notificaciones no podremos recordarte clases ni entregas. Puedes activarlas en Ajustes")
    }

    /** Formulario de tarea abierto; se oculta mientras se crea una asignatura desde él. */
    private var tareaDialog: TareaDialog? = null


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

    /** Con el menú del botón flotante abierto, Atrás lo cierra en vez de salir de la pantalla. */
    private val cerrarMenuAlVolver = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = toggleFabMode(findViewById(R.id.botonAñadir))
    }

    private var eventoDialog: EventoDialog? = null
    private val seleccionarImagen = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        // Se guarda una copia reducida: la Uri del selector deja de valer al cerrar la app
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            val copia = copiarImagenEvento(uri)
            if (copia != null) eventoDialog?.mostrarImagen(copia, "Imagen seleccionada")
            else mostrarMensaje("No se pudo abrir esa imagen. Prueba con otra.")
        }
    }

    // COMPARTIR POR QR: el lector devuelve el texto de un QR de EduPlanner; aquí se confirma y se agrega
    private val compartir: CompartirViewModel by viewModels()
    private val temaViewModel: TemaViewModel by viewModels()
    private val leerQr = registerForActivityResult(EscanerQrActivity.Contrato()) { texto ->
        texto?.let { compartir.onQrLeido(it) }
    }


    companion object {
        private const val MAX_AVISOS_LOGRO = 3
        // Pantallas con flecha para volver en vez del menú lateral
        private val SUBPANTALLAS = setOf(
            R.id.id_shop_fragment, R.id.id_focus_fragment, R.id.id_prediction_fragment, R.id.id_detail_fragment,
            R.id.id_survey_fragment
        )

        fun create(context: Context): Intent =
            Intent(context, InicioActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // El tema elegido en Opciones, antes de crear la ventana y las vistas
        val tema = aplicarTemaElegido()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        makeSystemBarsTransparent()
        ajustarBarrasDelSistema(tema)
        
        // De borde a borde: el contenido (no el DrawerLayout, que ignora su propio padding) deja sitio a la
        // barra de navegación del sistema; arriba no, porque la cabecera se extiende bajo la barra de estado
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.contenidoPrincipal)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        // La cabecera crece con la barra de estado para que el degradado llegue hasta arriba
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header_card)) { v, insets ->
            val arriba = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(v.paddingLeft, arriba + (4 * resources.displayMetrics.density).toInt(), v.paddingRight, v.paddingBottom)
            insets
        }
        initObserver()
        initNavegate()
        initfloatingBottons()
        onBackPressedDispatcher.addCallback(this, cerrarMenuAlVolver)
        setupHamburgerMenu()
        setupBottomNavigation()
        iniciarComprobacionAsistencia()
        abrirEnfoqueSiSePidio(intent)
        // Al recrear (p. ej. al cambiar el tema) el intent es el mismo: no se vuelve a abrir el destino
        if (savedInstanceState == null) abrirDestinoDeAviso(intent)
        CanalesNotificacion.crear(this)
        vigilanteAvisos.solicitar()
        pedirPermisoNotificacionesSiHaceFalta()
    }

    /** Se explica para qué antes de que el sistema pregunte; si se rechaza, no se insiste en cada apertura. */
    private fun pedirPermisoNotificacionesSiHaceFalta() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        if (preferenciasAvisos.permisoPedido) return
        preferenciasAvisos.permisoPedido = true
        preguntar(
            titulo = "¿Activamos tus recordatorios?",
            mensaje = "EduPlanner te avisa 10 minutos antes de cada clase, antes de que venza una tarea y cada mañana con lo que tienes ese día. " +
                "Al terminar una clase podrás registrar tu asistencia desde la notificación.",
            textoSi = "Activar",
            textoNo = "Ahora no",
            onSi = { pedirPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS) },
            onNo = {}
        )
    }

    /** Al tocar una notificación se llega a lo que avisaba: la tarea, el horario o la pregunta de asistencia. */
    private fun abrirDestinoDeAviso(intent: Intent?) {
        val destino = intent?.getStringExtra(IntentsAvisos.EXTRA_DESTINO) ?: return
        intent.removeExtra(IntentsAvisos.EXTRA_DESTINO)
        when (destino) {
            IntentsAvisos.DESTINO_TAREA -> {
                val id = intent.getIntExtra(IntentsAvisos.EXTRA_TAREA_ID, -1).takeIf { it > 0 } ?: return
                navController.navigate(
                    R.id.id_prediction_fragment,
                    bundleOf(
                        PrediccionFragment.ARG_TAREA_ID to id,
                        PrediccionFragment.ARG_TITULO to intent.getStringExtra(IntentsAvisos.EXTRA_TAREA_TITULO).orEmpty(),
                        PrediccionFragment.ARG_ASIGNATURA to intent.getStringExtra(IntentsAvisos.EXTRA_TAREA_ASIGNATURA)
                    )
                )
            }
            IntentsAvisos.DESTINO_HORARIO -> irAPestana(R.id.id_schedule_fragment)
            IntentsAvisos.DESTINO_ASISTENCIA -> comprobarAsistencia()
            else -> irAPestana(R.id.id_home_fragment)
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration)|| super.onSupportNavigateUp()
    }


    fun initNavegate(){
        toolbar=findViewById(R.id.myToolbar)
        setSupportActionBar(toolbar)

        drawerLayout=findViewById(R.id.drawer)
        navigationView=findViewById(R.id.nav_view)

        // Con FragmentContainerView el NavController se obtiene del NavHostFragment, no de la vista
        navController = (supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment).navController

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
        // El menú navega igual que la barra inferior (irAPestana). NavigationUI guarda y restaura el estado de cada
        // destino y, mezclado con la barra, a veces no hacía nada (p. ej. Inicio después de abrir Perfil desde abajo)
        navigationView.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawer(GravityCompat.START)
            if (navController.graph.findNode(item.itemId) != null) {
                irAPestana(item.itemId)
                true
            } else false
        }
        // El elemento marcado sigue a la pantalla abierta, venga de donde venga
        navController.addOnDestinationChangedListener { _, destination, _ ->
            navigationView.menu.findItem(destination.id)?.isChecked = true
        }

        // Opciones > Tema de la app: no es un destino, abre la hoja para elegirlo
        navigationView.menu.findItem(R.id.opcion_tema).setOnMenuItemClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            TemaDialog(this, temaViewModel.temas, temaViewModel.temaActual()) { temaViewModel.elegir(it) }.show()
            true
        }

        val signoutItem:MenuItem = navigationView.menu.findItem(R.id.signout)

        signoutItem.setOnMenuItemClickListener {
            lifecycleScope.launch {
                // Olvida también la cuenta de Google elegida para que el próximo inicio vuelva a preguntar
                credencialGoogle.olvidarCuenta()
                // Los avisos son de este estudiante: no deben llegarle a quien entre después
                vigilanteAvisos.cancelarTodo()
                FirebaseAuth.getInstance().signOut()
                val intents=Intent(this@InicioActivity, MainActivity::class.java)
                intents.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intents)
                finish()
            }
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
        findViewById<View>(R.id.fab_velo).setOnClickListener {
            toggleFabMode(anadirb)
        }
        tareab.setOnClickListener {
            toggleFabMode(anadirb)
            inicioViewModel.onTareaSelected()
        }
        eventob.setOnClickListener {
            toggleFabMode(anadirb)
            inicioViewModel.onEventoSelected()
        }
        calificacionb.setOnClickListener {
            toggleFabMode(anadirb)
            showDialogCalificacion()
        }
        qrb.setOnClickListener {
            toggleFabMode(anadirb)
            leerQr.launch(Unit)
        }

    }

    private fun setupHamburgerMenu() {
        val hamburgerMenu = findViewById<ImageView>(R.id.menu_hamburger)
        hamburgerMenu?.setOnClickListener {
            // En Tienda, Enfoque y Predicción es la flecha para volver
            if (navController.currentDestination?.id in SUBPANTALLAS) {
                navController.navigateUp()
            } else if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
    }

    private fun setupBottomNavigation() {
        val pestanas = mapOf(
            R.id.id_home_fragment to findViewById<View>(R.id.nav_home),
            R.id.id_diary_fragment to findViewById<View>(R.id.nav_calendar),
            R.id.id_schedule_fragment to findViewById<View>(R.id.nav_schedule),
            R.id.id_ratings_fragment to findViewById<View>(R.id.nav_ratings),
            R.id.id_profile_fragment to findViewById<View>(R.id.nav_profile)
        )
        pestanas.forEach { (destino, pestana) ->
            pestana.setOnClickListener { irAPestana(destino) }
        }

        // El saldo de la cabecera también lleva al perfil
        findViewById<View>(R.id.header_saldo).setOnClickListener { abrirPerfil() }

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            // En una sesión de enfoque no se ofrece agregar nada: es la distracción que se quiere evitar; en la
            // encuesta el botón taparía "Siguiente"
            val anadir = findViewById<View>(R.id.botonAñadir)
            val enEnfoque = destination.id == R.id.id_focus_fragment || destination.id == R.id.id_survey_fragment
            if (rotate) toggleFabMode(anadir)
            anadir.visibility = if (enEnfoque) View.GONE else View.VISIBLE

            findViewById<TextView>(R.id.header_titulo).text =
                if (destination.id == R.id.id_detail_fragment) {
                    TipoDetalle.deClave(arguments?.getString(DetalleFragment.ARG_TIPO))?.pantalla ?: destination.label
                } else destination.label
            val subpantalla = destination.id in SUBPANTALLAS
            findViewById<ImageView>(R.id.menu_hamburger).apply {
                setImageResource(if (subpantalla) R.drawable.ic_arrow_back else R.drawable.ic_menu_hamburger)
                contentDescription = if (subpantalla) "Volver" else "Abrir menú"
            }
            // Perfil y Enfoque continúan el degradado de la cabecera: sin esquinas no se ve el corte
            val continua = destination.id == R.id.id_profile_fragment || destination.id == R.id.id_focus_fragment
            findViewById<View>(R.id.header_card).setBackgroundResource(
                if (continua) R.drawable.bg_cabecera_recta else R.drawable.bg_cabecera
            )

            // Tienda y Enfoque cuelgan del perfil; una tarea o un detalle, de la pestaña desde la que se abrió
            val activa = when {
                destination.id == R.id.id_prediction_fragment || destination.id == R.id.id_detail_fragment ->
                    navController.previousBackStackEntry?.destination?.id ?: R.id.id_home_fragment
                // La encuesta cuelga de Ayuda, que no es una pestaña
                destination.id == R.id.id_survey_fragment -> R.id.id_help_fragment
                subpantalla -> R.id.id_profile_fragment
                else -> destination.id
            }
            pestanas.forEach { (destino, pestana) -> pestana.isSelected = destino == activa }
        }
    }

    /** Sin apilar copias: volver desde cualquier pestaña lleva al inicio. */
    /** Avisa y lleva a la pantalla donde quedó lo agregado, ya recargada. */
    private fun onImportado(resultado: ResultadoImportacion) {
        mostrarMensaje(TextosCompartir.resultado(resultado))
        val importado = resultado as? ResultadoImportacion.Importado ?: return
        importado.recompensa?.let { mostrarRecompensa(it) }
        inicioViewModel.obtenerAsignaturas()
        when (importado.tipo) {
            TipoCompartido.TAREA -> {
                inicioViewModel.obtenerTareas()
                irAPestana(R.id.id_home_fragment)
            }
            TipoCompartido.EVENTO -> {
                inicioViewModel.obtenerEventos()
                irAPestana(R.id.id_home_fragment)
            }
            TipoCompartido.NOTA -> {
                inicioViewModel.obtenerCalificaciones()
                irAPestana(R.id.id_ratings_fragment)
            }
            // Asignaturas carga su lista al crearse: si ya está abierta se vuelve a abrir
            TipoCompartido.ASIGNATURA -> recargarPestana(R.id.id_subjects_fragment)
        }
        // Una asignatura creada para recibir una tarea o nota también debe aparecer en su pantalla
        if (importado.asignaturaCreada != null && navController.currentDestination?.id == R.id.id_subjects_fragment) {
            recargarPestana(R.id.id_subjects_fragment)
        }
    }

    private fun recargarPestana(destino: Int) {
        if (navController.currentDestination?.id != destino) return irAPestana(destino)
        navController.navigate(destino, null, navOptions {
            launchSingleTop = false
            popUpTo(destino) { inclusive = true }
        })
    }

    private fun irAPestana(destino: Int) {
        if (navController.currentDestination?.id == destino) return
        navController.navigate(destino, null, navOptions {
            launchSingleTop = true
            popUpTo(R.id.id_home_fragment)
        })
    }

    private fun abrirPerfil(pestana: Int = 0) {
        // Con una pestaña concreta se vuelve a abrir aunque ya esté en el perfil
        if (navController.currentDestination?.id != R.id.id_profile_fragment || pestana != 0) {
            navController.navigate(R.id.id_profile_fragment, bundleOf(ProfileFragment.ARG_PESTANA to pestana))
        }
    }

    private fun initObserver(){
        // Al cambiar de tema se vuelve a crear la pantalla, que se pinta de nuevo con la paleta elegida
        temaViewModel.aplicar.observe(this) { it.getContentIfNotHandled()?.let { recreate() } }
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
        inicioViewModel.asignaturaParaTarea.observe(this){
            it.getContentIfNotHandled()?.let { resultado -> onAsignaturaParaTarea(resultado) }
        }
        compartir.analisis.observe(this){
            it.getContentIfNotHandled()?.let { analisis ->
                ImportarCompartidoDialog(this, analisis) { destino -> compartir.onAgregar(analisis.contenido, destino) }.show()
            }
        }
        compartir.resultado.observe(this){
            it.getContentIfNotHandled()?.let { resultado -> onImportado(resultado) }
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
        // El observador mantiene el resumen del inicio al día aunque esa pantalla no esté visible; además pinta la
        // racha de la cabecera, junto a las monedas
        inicioViewModel.panelProgreso.observe(this){ panel ->
            rachaMostrada = panel.racha.actual
            findViewById<TextView>(R.id.racha_counter).text = panel.racha.actual.toString()
            describirCabecera()
        }
    }

    private var saldoMostrado: Int? = null
    private var rachaMostrada: Int = 0

    /** Lo que lee TalkBack en la pastilla de la cabecera. */
    private fun describirCabecera() {
        val dias = if (rachaMostrada == 1) "1 día" else "$rachaMostrada días"
        findViewById<View>(R.id.header_saldo).contentDescription =
            "Racha de $dias, ${saldoMostrado ?: 0} EduCoins. Ver tu progreso"
    }

    /** La primera lectura se muestra tal cual; los cambios posteriores se cuentan y el contador pulsa. */
    private fun actualizarContadorMonedas(saldo: Int) {
        val contador = findViewById<TextView>(R.id.coin_counter)
        val anterior = saldoMostrado
        saldoMostrado = saldo
        describirCabecera()
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
        abrirDestinoDeAviso(intent)
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
        tareaDialog?.cerrar()
        tareaDialog = null
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
        animarVelo(rotate)
        cerrarMenuAlVolver.isEnabled = rotate
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

    private fun animarVelo(mostrar: Boolean) {
        val velo = findViewById<View>(R.id.fab_velo)
        velo.animate().cancel()
        if (mostrar) velo.visibility = View.VISIBLE
        velo.animate()
            .alpha(if (mostrar) 1f else 0f)
            .setDuration(200)
            .withEndAction { if (!mostrar) velo.visibility = View.GONE }
            .start()
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
            .rotation(if (rotate) 135f else 0f)
        return rotate
    }


    private fun showDialogTarea(){
        tareaDialog = TareaDialog(
            context = this,
            asignaturas = obtenerNombresAsignaturas(),
            onAgregarAsignatura = { showDialogAsignaturaParaTarea() },
            onGuardar = { titulo, descrip, asignatura, fecha, prioridad, horaEntrega ->
                tareaDialog = null
                inicioViewModel.onAgregarTareaSelected(titulo, descrip, asignatura, fecha, prioridad, horaEntrega)
                // La lista se recarga sola al terminar de guardar; aquí solo se muestra el día de la tarea
                homeViewModel.mostrarFecha(fecha)
                mostrarMensaje("Tarea guardada")
            }
        ).also { it.show() }
    }

    /** Formulario de asignatura abierto desde la tarea: al guardar o cancelar se vuelve a la tarea. */
    private fun showDialogAsignaturaParaTarea() {
        AsignaturaDialog(
            context = this,
            otrosNombres = inicioViewModel.asignaturasCompletas().map { it.nombre },
            onGuardar = { nueva -> inicioViewModel.onCrearAsignaturaDesdeTarea(nueva) },
            // Si se guardó, la tarea vuelve cuando el ViewModel confirma (observador de asignaturaParaTarea)
            onCerrar = { guardada -> if (!guardada) tareaDialog?.volver() }
        ).show()
    }

    private fun preguntarArea(asignaturaId: Int, nombre: String, alTerminar: () -> Unit) {
        AreaAsignaturaDialog(
            context = this,
            nombreAsignatura = nombre,
            onGuardar = { area ->
                lifecycleScope.launch {
                    val error = datosEstudio.guardarArea(asignaturaId, area).mensajeError()
                    mostrarMensaje(error ?: "Asignatura guardada · Área: ${CatalogoModelo.etiqueta(area)}")
                }
            },
            onCerrar = alTerminar
        ).show()
    }

    private fun onAsignaturaParaTarea(resultado: ResultadoCrearAsignatura) {
        val tarea = tareaDialog ?: return
        when (resultado) {
            is ResultadoCrearAsignatura.Creada -> {
                if (resultado.area != null) {
                    tarea.asignaturaCreada(resultado.nombre)
                    mostrarMensaje("Asignatura guardada · Área: ${CatalogoModelo.etiqueta(resultado.area)}")
                } else {
                    // El nombre no dejó claro el área: se pregunta antes de volver a la tarea
                    preguntarArea(resultado.id, resultado.nombre) { tarea.asignaturaCreada(resultado.nombre) }
                }
            }
            else -> {
                tarea.volver()
                mostrarMensaje(getString(R.string.asignatura_error_guardar))
            }
        }
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
                mostrarMensaje("Evento guardado")
            }
        ).also { it.show() }
    }

    private fun showDialogCalificacion(){
        CalificacionDialog(
            this, inicioViewModel.asignaturasCompletas(),
            onEscalaNueva = { id, escala -> inicioViewModel.guardarEscala(id, escala) }
        ) { tipo, valor, asignatura, descripcion ->
            inicioViewModel.onAgregarCalificacionSelected(tipo, valor, asignatura, descripcion)
            mostrarMensaje("Calificación guardada")
        }.show()
    }

    private fun obtenerNombresAsignaturas(): List<String> {
        inicioViewModel.obtenerAsignaturas()
        return inicioViewModel._asignaturas.value.orEmpty()
    }

    private fun makeSystemBarsTransparent() {
        // De borde a borde; sin barra de estado clara: sus iconos van en blanco sobre el degradado
        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
    }

}
