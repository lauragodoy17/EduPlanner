package com.softcg.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.auth.FirebaseAuth
import com.softcg.myapplication.ui.home.HomeActivity
import com.softcg.myapplication.ui.login.MainActivity

class PrincipalSplash : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal_splash2)

        splashScreen.setKeepOnScreenCondition { true }

        if (FirebaseAuth.getInstance().currentUser == null) {

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }else{
            val currentUser = FirebaseAuth.getInstance().currentUser
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

    }
}