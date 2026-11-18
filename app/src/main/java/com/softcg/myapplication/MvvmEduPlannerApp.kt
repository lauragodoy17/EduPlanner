package com.softcg.myapplication

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.softcg.myapplication.core.AppCheckProveedor
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MvvmEduPlannerApp:Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase AI Logic exige App Check: sin token, Gemini rechaza las recomendaciones de tareas
        FirebaseApp.initializeApp(this)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(AppCheckProveedor.fabrica())
    }
}
