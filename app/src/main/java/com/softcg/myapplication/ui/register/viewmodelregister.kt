package com.softcg.myapplication.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.softcg.myapplication.core.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class viewmodelregister @Inject constructor() : ViewModel() {
    private val _navigateToLogin= MutableLiveData<Event<Boolean>>()
    val navigateToLogin: LiveData<Event<Boolean>>
        get() = _navigateToLogin

    fun onLoginSelected(){
        _navigateToLogin.value= Event(true)
    }
}