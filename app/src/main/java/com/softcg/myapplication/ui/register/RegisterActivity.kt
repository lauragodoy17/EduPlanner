package com.softcg.myapplication.ui.register

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.utils.ViewState
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.softcg.myapplication.R
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.softcg.myapplication.core.dialog.DialogFragmentLauncher
import com.softcg.myapplication.core.dialog.ErrorDialog
import com.softcg.myapplication.core.ex.*
import com.softcg.myapplication.databinding.ActivityRegisterBinding
import com.softcg.myapplication.ui.home.HomeActivity
import com.softcg.myapplication.ui.login.MainActivity
import com.softcg.myapplication.ui.register.model.UserRegister

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    companion object{
        fun create(context: Context): Intent =
            Intent(context, RegisterActivity::class.java)
    }

    private lateinit var binding: ActivityRegisterBinding
    private val viewmodelregister : viewmodelregister by viewModels()

    @Inject
    lateinit var dialogLauncher: DialogFragmentLauncher

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
        binding.campoemail.loseFocusAfterAction(EditorInfo.IME_ACTION_NEXT)
        binding.campoemail.setOnFocusChangeListener{_, hasFocus -> onFieldChanged(hasFocus)}
        binding.campoemail.onTextChanged { onFieldChanged() }

        binding.campocontrasena.loseFocusAfterAction(EditorInfo.IME_ACTION_NEXT)
        binding.campocontrasena.setOnFocusChangeListener{_, hasFocus -> onFieldChanged(hasFocus)}
        binding.campocontrasena.onTextChanged { onFieldChanged() }

        binding.campoconfirmarcontra.loseFocusAfterAction(EditorInfo.IME_ACTION_NEXT)
        binding.campoconfirmarcontra.setOnFocusChangeListener{_, hasFocus -> onFieldChanged(hasFocus)}
        binding.campoconfirmarcontra.onTextChanged { onFieldChanged() }

        with(binding){
            tvIniciaSesion.setOnClickListener{viewmodelregister.onLoginSelected()}
            Boton1.setOnClickListener{viewmodelregister.onLoginSelected()}
            botonregistro.setOnClickListener {
                it.dismissKeyboard()
                viewmodelregister.onRegisterSelected(
                    UserRegister(
                        email = binding.campoemail.text.toString(),
                        password = binding.campocontrasena.text.toString(),
                        passwordConfirmation = binding.campoconfirmarcontra.text.toString()
                    )
                )
            }
        }
    }

    private fun initObservers(){
        viewmodelregister.navigateToLogin.observe(this, Observer{
            it.getContentIfNotHandled()?.let {
                goToLogin()
            }
        })
        viewmodelregister.navegateToHome.observe(this){
            it.getContentIfNotHandled()?.let {
                goToHome()
            }
        }
        lifecycleScope.launchWhenStarted {
            viewmodelregister.viewState.collect(){viewState ->
                updateUI(viewState)
            }
        }
        viewmodelregister.showErrorDialog.observe(this){ showError ->
            if (showError) showErrorDialog()
        }
    }

    private fun showErrorDialog() {
        ErrorDialog.create(
            title = getString(R.string.signin_error_dialog_title),
            description = getString(R.string.signin_error_dialog_body),
            positiveAction = ErrorDialog.Action(getString(R.string.signin_error_dialog_positive_action)) {
                it.dismiss()
            }
        ).show(dialogLauncher, this)
    }

    private fun updateUI(viewState: RegisterViewState ){
        with(binding){
            binding.campoemail.error=
                if (viewState.isValidEmail) null else getString(R.string.signin_error_mail)
            binding.campocontrasena.error=
                if (viewState.isValidPassword)null else getString(R.string.signin_error_password)
            binding.campoconfirmarcontra.error=
                if (viewState.isValidPassword)null else getString(R.string.signin_error_password)
        }
    }

    private fun goToLogin(){
        startActivity(MainActivity.create(this))
    }

    private fun onFieldChanged(hasFocus: Boolean=false){
        if (!hasFocus){
            viewmodelregister.onFieldsChanged(
                UserRegister(
                    email = binding.campoemail.text.toString(),
                    password = binding.campoemail.text.toString(),
                    passwordConfirmation = binding.campoemail.text.toString()
                )
            )
        }
    }

    private fun goToHome(){
        startActivity(HomeActivity.create(this))
    }

}