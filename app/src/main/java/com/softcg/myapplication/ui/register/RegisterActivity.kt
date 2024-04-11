package com.softcg.myapplication.ui.register

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.softcg.myapplication.R
import androidx.lifecycle.Observer
import com.softcg.myapplication.databinding.ActivityRegisterBinding
import com.softcg.myapplication.ui.login.MainActivity

import dagger.hilt.android.AndroidEntryPoint
@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val viewmodelregister : viewmodelregister by viewModels()
    companion object{
        fun create(context: Context): Intent =
            Intent(context, RegisterActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
    }

    private fun init(){
        initlisteners()
        initObservers()
    }

    private fun initlisteners(){
        with(binding){
            tvIniciaSesion.setOnClickListener{viewmodelregister.onLoginSelected()}
            Boton1.setOnClickListener{viewmodelregister.onLoginSelected()}
        }
    }

    private fun initObservers(){
        viewmodelregister.navigateToLogin.observe(this, Observer{
            it.getContentIfNotHandled()?.let {
                goToRegister()
            }
        })
    }
    private fun goToRegister(){
        startActivity(MainActivity.create(this))
    }

}