package com.softcg.myapplication.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.softcg.myapplication.R
import com.softcg.myapplication.core.dialog.DialogFragmentLauncher
import com.softcg.myapplication.core.dialog.ErrorDialog
import com.softcg.myapplication.core.ex.dismissKeyboard
import com.softcg.myapplication.core.ex.loseFocusAfterAction
import com.softcg.myapplication.core.ex.onTextChanged
import com.softcg.myapplication.core.ex.show
import com.softcg.myapplication.databinding.ActivityMainBinding
import com.softcg.myapplication.ui.home.HomeActivity
import com.softcg.myapplication.ui.login.model.UserLogin
import com.softcg.myapplication.ui.register.RegisterActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val miViewModel : MiViewModel by viewModels()


    companion object{
        fun create(context: Context): Intent =
            Intent(context, MainActivity::class.java)
    }

    @Inject
    lateinit var dialogLauncher: DialogFragmentLauncher

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

    private fun pass(){

    }

    private fun init(){
        initlisteners()
        initObservers()
    }

    private fun initlisteners(){

        binding.emailText.loseFocusAfterAction(EditorInfo.IME_ACTION_NEXT)
        binding.campocontraseA.setOnFocusChangeListener { _, hasFocus -> onFieldChanged(hasFocus) }
        binding.emailText.onTextChanged { onFieldChanged() }

        binding.contrasenaText.loseFocusAfterAction(EditorInfo.IME_ACTION_NEXT)
        binding.contrasenaText.setOnFocusChangeListener { _, hasFocus -> onFieldChanged(hasFocus) }
        binding.contrasenaText.onTextChanged { onFieldChanged() }

        binding.botoningresar.setOnClickListener {
            it.dismissKeyboard()
            miViewModel.onLoginSelected(
                binding.emailText.text.toString(),
                binding.contrasenaText.text.toString()
            )
        }

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

        miViewModel.navigateToHome.observe(this){
            it.getContentIfNotHandled()?.let{
                goToHome()
            }
        }

        miViewModel.showErrorDialog.observe(this){userLogin ->
            if (userLogin.showErrorDialog) showErrorDialog(userLogin)
        }

        lifecycleScope.launchWhenStarted {
            miViewModel.viewState.collect(){ viewState ->
                updateUI(viewState)
            }
        }
    }

    private fun updateUI(viewState: LoginViewState){
        with(binding){
            campoemail.error =
                if (viewState.isValidEmail) null else getString(R.string.login_error_mail)
            campocontraseA.error=
                if (viewState.isValidPasswords) null else getString(R.string.login_error_password)

        }
    }

    private fun onFieldChanged(hasFocus:Boolean=false){
        if (!hasFocus){
            miViewModel.onFieldsChanged(
                email = binding.emailText.text.toString(),
                password = binding.contrasenaText.text.toString()
            )
        }
    }

    private fun showErrorDialog(userLogin: UserLogin){
        ErrorDialog.create(
            title = getString(R.string.login_error_dialog_title),
            description = getString(R.string.login_error_dialog_body),
            negativeAction = ErrorDialog.Action(getString(R.string.login_error_dialog_negative_action)) {
                it.dismiss()
            },
            positiveAction = ErrorDialog.Action(getString(R.string.login_error_dialog_positive_action)) {
                miViewModel.onLoginSelected(
                    userLogin.email,
                    userLogin.password
                )
                it.dismiss()
            }
        ).show(dialogLauncher,this)
    }

    private fun goToHome(){
        val intents=Intent(this, HomeActivity::class.java)
        intents.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intents)
        finish()
    }
    private fun goToRegister(){
        startActivity(RegisterActivity.create(this))
    }

}