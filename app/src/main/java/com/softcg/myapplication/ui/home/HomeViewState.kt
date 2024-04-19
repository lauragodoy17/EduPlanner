package com.softcg.myapplication.ui.home

import com.softcg.myapplication.ui.tarea.model.Tarea

data class HomeViewState (
    val tareas:List<Tarea> = emptyList()
)