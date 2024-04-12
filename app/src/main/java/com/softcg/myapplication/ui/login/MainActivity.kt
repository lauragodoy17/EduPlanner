package com.softcg.myapplication.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.softcg.myapplication.R
import com.softcg.myapplication.databinding.ActivityMainBinding
import com.softcg.myapplication.ui.register.RegisterActivity

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val miViewModel : MiViewModel by viewModels()
    companion object{
        fun create(context: Context): Intent =
            Intent(context, MainActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()

    }

    private fun init(){
        initlisteners()
        initObservers()
    }

    private fun initlisteners(){
        with(binding){
            textoregistrar.setOnClickListener{miViewModel.onRegisterSelected()}

        }

    }

    private fun initObservers(){
        miViewModel.navigateToRegister.observe(this, Observer{
            it.getContentIfNotHandled()?.let {
                goToRegister()
            }
        })
    }

    private fun goToRegister(){
        startActivity(RegisterActivity.create(this))
    }

}