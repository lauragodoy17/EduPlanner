package com.softcg.myapplication

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.softcg.myapplication.core.AppCheckProveedor
import com.softcg.myapplication.ui.notifications.VigilanteAvisos
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MvvmEduPlannerApp:Application() {

    @Inject lateinit var vigilanteAvisos: VigilanteAvisos

    override fun onCreate() {
        super.onCreate()
        // Cualquier cambio en tareas, asignaturas o asistencias replanifica las notificaciones
        vigilanteAvisos.iniciar()
        // Firebase AI Logic exige App Check: sin token, Gemini rechaza las recomendaciones de tareas
        FirebaseApp.initializeApp(this)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(AppCheckProveedor.fabrica())
    }
}
