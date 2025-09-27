package com.softcg.myapplication.ui.Inicio.Models

import java.util.Date

data class DateFilterItem(
    val date: Date,
    val dayNumber: String,
    val dayName: String,
    val isSelected: Boolean = false,
    val isToday: Boolean = false
)