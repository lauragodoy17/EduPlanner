package com.softcg.myapplication.ui.Inicio.Fragments.Perfil

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButtonToggleGroup
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.mostrarMensaje
import com.softcg.myapplication.core.ex.mostrarRecompensa
import com.softcg.myapplication.domain.gamificacion.DuracionEnfoque
import com.softcg.myapplication.domain.gamificacion.ResultadoCompletarEnfoque
import com.softcg.myapplication.domain.gamificacion.ResultadoIniciarEnfoque
import com.softcg.myapplication.domain.gamificacion.SesionEnfoque
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Views.AnilloProgresoView
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Views.ConfettiView
import com.softcg.myapplication.ui.notifications.NotificationScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class EnfoqueFragment : Fragment() {

    private val enfoqueViewModel: EnfoqueViewModel by viewModels()
    private var temporizador: Job? = null

    private val pedirNotificaciones = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_enfoque, container, false)
        initConfigurar(view)
        initEnCurso(view)
        initTerminada(view)
        initResultados(view)
        return view
    }

    override fun onResume() {
        super.onResume()
        // Al volver a la app, la sesión pudo haber terminado mientras tanto
        enfoqueViewModel.cargar()
    }

    private fun initConfigurar(view: View) {
        val duraciones = view.findViewById<MaterialButtonToggleGroup>(R.id.enfoqueDuraciones)
        val recompensa = view.findViewById<TextView>(R.id.enfoqueRecompensa)
        val asignatura = view.findViewById<AutoCompleteTextView>(R.id.enfoqueAsignatura)
        asignatura.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, enfoqueViewModel.asignaturas)
        )

        fun duracionElegida() = when (duraciones.checkedButtonId) {
            R.id.enfoque15 -> DuracionEnfoque.CORTA
            R.id.enfoque50 -> DuracionEnfoque.LARGA
            else -> DuracionEnfoque.MEDIA
        }
        duraciones.addOnButtonCheckedListener { _, _, marcado ->
            if (marcado) {
                val monedas = duracionElegida().tipo.cantidad
                recompensa.text = "Ganarás $monedas EduCoins y $monedas XP"
            }
        }
        duraciones.check(R.id.enfoque25)

        view.findViewById<Button>(R.id.enfoqueIniciar).setOnClickListener {
            pedirPermisoNotificaciones()
            enfoqueViewModel.iniciar(duracionElegida().minutos, asignatura.text.toString())
        }
    }

    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            requireContext().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pedirNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun initEnCurso(view: View) {
        val anillo = view.findViewById<AnilloProgresoView>(R.id.enfoqueAnillo)
        anillo.grosor(10f)
        anillo.colores(requireContext().getColor(R.color.blanco_25), requireContext().getColor(R.color.xp_dorado))

        view.findViewById<Button>(R.id.enfoqueAbandonar).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("¿Abandonar la sesión?")
                .setMessage("No ganarás EduCoins por esta sesión.")
                .setPositiveButton("Abandonar") { _, _ ->
                    NotificationScheduler.cancelarFinEnfoque(requireContext())
                    enfoqueViewModel.abandonar()
                }
                .setNegativeButton("Seguir estudiando", null)
                .show()
        }

        enfoqueViewModel.sesion.observe(viewLifecycleOwner) { sesion ->
            temporizador?.cancel()
            if (view.findViewById<View>(R.id.enfoqueTerminada).visibility == View.VISIBLE) return@observe
            mostrarEstado(view, enCurso = sesion != null)
            if (sesion != null) {
                // También al reanudar: forzar el cierre de la app o reiniciar el teléfono borra las alarmas
                if (sesion.finMillis > enfoqueViewModel.ahoraMillis()) {
                    NotificationScheduler.programarFinEnfoque(requireContext(), sesion.finMillis)
                }
                arrancarTemporizador(view, anillo, sesion)
            }
        }
    }

    private fun mostrarEstado(view: View, enCurso: Boolean) {
        view.findViewById<View>(R.id.enfoqueConfigurar).visibility = if (enCurso) View.GONE else View.VISIBLE
        view.findViewById<View>(R.id.enfoqueEnCurso).visibility = if (enCurso) View.VISIBLE else View.GONE
        // La pantalla no se apaga mientras se ve el contador
        view.keepScreenOn = enCurso
    }

    private fun arrancarTemporizador(view: View, anillo: AnilloProgresoView, sesion: SesionEnfoque) {
        view.findViewById<TextView>(R.id.enfoqueAsignaturaActual).text = sesion.asignatura.ifBlank { "Modo enfoque" }
        val tiempo = view.findViewById<TextView>(R.id.enfoqueTiempo)
        val total = sesion.minutos * 60_000f

        temporizador = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    val restante = sesion.finMillis - enfoqueViewModel.ahoraMillis()
                    if (restante <= 0) {
                        enfoqueViewModel.completar()
                        delay(TIC_MS)
                        continue
                    }
                    val segundos = (restante + 999) / 1000
                    tiempo.text = String.format(Locale.US, "%02d:%02d", segundos / 60, segundos % 60)
                    tiempo.contentDescription = "Quedan ${segundos / 60} minutos"
                    anillo.fijar(1f - restante / total, animar = false)
                    delay(TIC_MS)
                }
            }
        }
    }

    private fun initTerminada(view: View) {
        view.findViewById<Button>(R.id.enfoqueOtra).setOnClickListener {
            view.findViewById<View>(R.id.enfoqueTerminada).visibility = View.GONE
            mostrarEstado(view, enCurso = false)
        }
        view.findViewById<Button>(R.id.enfoqueVolver).setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun initResultados(view: View) {
        enfoqueViewModel.resultadoInicio.observe(viewLifecycleOwner) { evento ->
            when (val resultado = evento.getContentIfNotHandled() ?: return@observe) {
                is ResultadoIniciarEnfoque.Iniciada -> {}
                ResultadoIniciarEnfoque.YaHayUna -> enfoqueViewModel.cargar()
                ResultadoIniciarEnfoque.DuracionInvalida, ResultadoIniciarEnfoque.SinSesion, ResultadoIniciarEnfoque.Error ->
                    requireActivity().mostrarMensaje("No se pudo iniciar la sesión. Inténtalo de nuevo")
            }
        }
        enfoqueViewModel.resultadoFin.observe(viewLifecycleOwner) { evento ->
            when (val resultado = evento.getContentIfNotHandled() ?: return@observe) {
                is ResultadoCompletarEnfoque.Completada -> mostrarTerminada(view, resultado)
                ResultadoCompletarEnfoque.Caducada ->
                    requireActivity().mostrarMensaje("Esa sesión terminó hace demasiado tiempo y ya no cuenta")
                ResultadoCompletarEnfoque.Error ->
                    requireActivity().mostrarMensaje("No se pudo guardar la sesión. Inténtalo de nuevo")
                ResultadoCompletarEnfoque.AunNoTermina, ResultadoCompletarEnfoque.SinSesionActiva -> {}
            }
        }
    }

    private fun mostrarTerminada(view: View, resultado: ResultadoCompletarEnfoque.Completada) {
        NotificationScheduler.cancelarFinEnfoque(requireContext())
        view.findViewById<View>(R.id.enfoqueEnCurso).visibility = View.GONE
        view.findViewById<View>(R.id.enfoqueConfigurar).visibility = View.GONE
        view.keepScreenOn = false
        view.findViewById<View>(R.id.enfoqueTerminada).visibility = View.VISIBLE
        view.findViewById<TextView>(R.id.enfoqueTerminadaTitulo).text = "${resultado.minutos} minutos de estudio real"
        view.findViewById<TextView>(R.id.enfoqueTerminadaDetalle).text =
            if (resultado.monedas > 0) "+${resultado.monedas} EduCoins y ${resultado.monedas} XP"
            else "Ya alcanzaste las recompensas de hoy para esta duración, pero el tiempo cuenta en tus estadísticas."
        view.findViewById<ConfettiView>(R.id.enfoqueConfeti).lanzar()
        if (resultado.monedas > 0) requireActivity().mostrarRecompensa(resultado.monedas)
    }

    companion object {
        private const val TIC_MS = 1000L
    }
}
