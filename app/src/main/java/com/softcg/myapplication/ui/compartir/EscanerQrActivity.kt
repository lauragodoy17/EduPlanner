package com.softcg.myapplication.ui.compartir

import android.util.Log
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.softcg.myapplication.ui.tema.aplicarTemaElegido
import android.provider.Settings
import android.util.Size
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.softcg.myapplication.R
import com.softcg.myapplication.domain.compartir.CodigoQr
import com.softcg.myapplication.domain.compartir.LecturaQr
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Lee QR con la cámara y devuelve el primero que sea de EduPlanner y esté completo. Los demás no cierran el
 * lector: se avisa brevemente y sigue buscando. Solo lee: lo que se agrega lo decide quien lo abrió.
 */
class EscanerQrActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_CODIGO = "codigo"
        private const val DURACION_AVISO_MS = 2500L
        private const val ESPERA_AL_LEER_MS = 350L
    }

    /** Abre el lector; devuelve el texto de un QR de EduPlanner válido, o null si se cerró sin leer. */
    class Contrato : ActivityResultContract<Unit, String?>() {
        override fun createIntent(context: Context, input: Unit) = Intent(context, EscanerQrActivity::class.java)
        override fun parseResult(resultCode: Int, intent: Intent?): String? =
            if (resultCode == RESULT_OK) intent?.getStringExtra(EXTRA_CODIGO) else null
    }

    private val lector = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    )
    private lateinit var hiloAnalisis: ExecutorService
    private var camara: Camera? = null
    private var linternaEncendida = false
    private var terminado = false
    private var avisoActual: String? = null
    private var bloqueado = false

    private lateinit var vista: PreviewView
    private lateinit var visor: VisorQrView
    private lateinit var aviso: TextView
    private lateinit var linterna: ImageButton
    private lateinit var panelPermiso: View

    private val ocultarAviso = Runnable {
        avisoActual = null
        aviso.animate().alpha(0f).setDuration(200).withEndAction { aviso.visibility = View.INVISIBLE }.start()
    }

    private val pedirPermiso = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) iniciarCamara() else mostrarPermiso()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // La cámara se ve igual en todos los temas; los avisos y el panel de permiso siguen el elegido
        aplicarTemaElegido()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_escaner_qr)
        // Pantalla completa con la cámara detrás de las barras del sistema
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        vista = findViewById(R.id.escanerVista)
        visor = findViewById(R.id.escanerVisor)
        aviso = findViewById(R.id.escanerAviso)
        linterna = findViewById(R.id.escanerLinterna)
        panelPermiso = findViewById(R.id.escanerPermiso)
        hiloAnalisis = Executors.newSingleThreadExecutor()

        val barra = findViewById<View>(R.id.escanerBarra)
        val pie = findViewById<View>(R.id.escanerPie)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.escanerRaiz)) { _, insets ->
            val sistema = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            barra.updatePadding(top = sistema.top + (8 * resources.displayMetrics.density).toInt())
            pie.updatePadding(bottom = sistema.bottom + (56 * resources.displayMetrics.density).toInt())
            insets
        }

        findViewById<View>(R.id.escanerCerrar).setOnClickListener { finish() }
        findViewById<View>(R.id.escanerAhoraNo).setOnClickListener { finish() }
        findViewById<View>(R.id.escanerPermitir).setOnClickListener { solicitarPermiso() }
        linterna.setOnClickListener { alternarLinterna() }

        if (tienePermiso()) iniciarCamara() else pedirPermiso.launch(Manifest.permission.CAMERA)
    }

    override fun onResume() {
        super.onResume()
        // Volviendo de los ajustes del sistema con el permiso ya concedido
        if (panelPermiso.visibility == View.VISIBLE && tienePermiso()) iniciarCamara()
    }

    override fun onDestroy() {
        aviso.removeCallbacks(ocultarAviso)
        hiloAnalisis.shutdown()
        lector.close()
        super.onDestroy()
    }

    private fun tienePermiso() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun mostrarPermiso() {
        panelPermiso.visibility = View.VISIBLE
        // Denegado dos veces (o "no volver a preguntar"): Android ya no muestra el diálogo, solo queda ir a los ajustes
        bloqueado = !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        findViewById<TextView>(R.id.escanerPermitir).text = if (bloqueado) "Abrir ajustes" else "Permitir cámara"
        findViewById<TextView>(R.id.escanerPermisoDetalle).text =
            if (bloqueado) "Activa el permiso de cámara para EduPlanner en los ajustes del teléfono. Solo se usa para leer códigos QR."
            else "Solo se usa para leer los códigos QR de EduPlanner. No se toman ni se guardan fotos."
    }

    private fun solicitarPermiso() {
        if (bloqueado) startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        else pedirPermiso.launch(Manifest.permission.CAMERA)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun iniciarCamara() {
        panelPermiso.visibility = View.GONE
        val futuro = ProcessCameraProvider.getInstance(this)
        futuro.addListener({
            val proveedor = futuro.get()
            val previa = Preview.Builder().build().also { it.setSurfaceProvider(vista.surfaceProvider) }
            val analisis = ImageAnalysis.Builder()
                // 720p basta para un QR a un palmo y mantiene el análisis fluido
                .setResolutionSelector(
                    ResolutionSelector.Builder().setResolutionStrategy(
                        ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                    ).build()
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(hiloAnalisis, ::analizar) }
            try {
                proveedor.unbindAll()
                camara = proveedor.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, previa, analisis).also { cam ->
                    linterna.visibility = if (cam.cameraInfo.hasFlashUnit()) View.VISIBLE else View.INVISIBLE
                }
            } catch (e: Exception) {
                Log.w("EscanerQr", "No se pudo iniciar la cámara", e)
                avisar("No se pudo abrir la cámara. Cierra otras apps que la estén usando e inténtalo de nuevo")
            }
        }, ContextCompat.getMainExecutor(this))

        // Tocar la imagen enfoca en ese punto (útil con códigos pequeños o pantallas con reflejos)
        vista.setOnTouchListener { v, evento ->
            if (evento.action == MotionEvent.ACTION_UP) {
                enfocar(evento.x, evento.y)
                v.performClick()
            }
            true
        }
    }

    private fun enfocar(x: Float, y: Float) {
        val punto = vista.meteringPointFactory.createPoint(x, y)
        camara?.cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(punto).build())
    }

    private fun alternarLinterna() {
        val cam = camara ?: return
        linternaEncendida = !linternaEncendida
        cam.cameraControl.enableTorch(linternaEncendida)
        linterna.setImageResource(if (linternaEncendida) R.drawable.ic_linterna_apagada else R.drawable.ic_linterna)
        linterna.contentDescription = if (linternaEncendida) "Apagar la linterna" else "Encender la linterna"
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analizar(imagen: ImageProxy) {
        val media = imagen.image
        if (media == null || terminado) {
            imagen.close()
            return
        }
        lector.process(InputImage.fromMediaImage(media, imagen.imageInfo.rotationDegrees))
            .addOnSuccessListener(ContextCompat.getMainExecutor(this)) { codigos ->
                codigos.mapNotNull { it.rawValue }.forEach(::evaluar)
            }
            .addOnCompleteListener { imagen.close() }
    }

    private fun evaluar(texto: String) {
        if (terminado) return
        when (CodigoQr.leer(texto)) {
            is LecturaQr.Valida -> terminar(texto)
            LecturaQr.VersionNueva -> avisar("Este código es de una versión más nueva de EduPlanner. Actualiza la app para leerlo")
            LecturaQr.Danado -> avisar("El código no se leyó completo. Acércate un poco más")
            LecturaQr.NoEsEduPlanner -> avisar("Este código no es de EduPlanner")
        }
    }

    /** El mismo aviso no se repite en cada fotograma: se queda visible un momento y luego se oculta. */
    private fun avisar(mensaje: String) {
        if (mensaje == avisoActual) return
        avisoActual = mensaje
        aviso.removeCallbacks(ocultarAviso)
        aviso.text = mensaje
        if (aviso.visibility != View.VISIBLE) {
            aviso.alpha = 0f
            aviso.visibility = View.VISIBLE
        }
        aviso.animate().alpha(1f).setDuration(150).start()
        aviso.postDelayed(ocultarAviso, DURACION_AVISO_MS)
    }

    private fun terminar(texto: String) {
        terminado = true
        visor.exito()
        val vibracion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
        visor.performHapticFeedback(vibracion)
        if (linternaEncendida) camara?.cameraControl?.enableTorch(false)
        // Una pausa breve para que se vea el marco en verde antes de cerrar
        visor.postDelayed({
            setResult(RESULT_OK, Intent().putExtra(EXTRA_CODIGO, texto))
            finish()
        }, ESPERA_AL_LEER_MS)
    }
}
