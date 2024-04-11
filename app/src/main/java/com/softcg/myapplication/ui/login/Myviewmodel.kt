package com.softcg.myapplication.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.softcg.myapplication.core.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MiViewModel @Inject constructor() :ViewModel() {
    private val _navigateToRegister=MutableLiveData<Event<Boolean>>()
    val navigateToRegister: LiveData<Event<Boolean>>
        get() = _navigateToRegister

    fun onRegisterSelected(){
        _navigateToRegister.value=Event(true)
    }
}