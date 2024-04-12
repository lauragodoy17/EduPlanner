package com.softcg.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.softcg.myapplication.ui.login.MainActivity

class PrincipalSplash : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal_splash2)

        splashScreen.setKeepOnScreenCondition { true }

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()

    }
}