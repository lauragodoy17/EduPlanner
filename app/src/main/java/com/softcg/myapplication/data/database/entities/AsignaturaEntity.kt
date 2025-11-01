package com.softcg.myapplication.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "asignaturas_table")
data class AsignaturaEntity (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="id")val id: Int?=null,
    @ColumnInfo(name="nombre")val nombre:String,
    @ColumnInfo(name = "tutor")val tutor:String,
    @ColumnInfo(name = "Duracion")val duracion:Int,
    @ColumnInfo(name = "hora")val hora:String,
    @ColumnInfo(name = "lunes")val lunes:Boolean,
    @ColumnInfo(name = "martes")val martes:Boolean,
    @ColumnInfo(name = "miercoles")val miercoles:Boolean,
    @ColumnInfo(name = "jueves")val jueves:Boolean,
    @ColumnInfo(name = "viernes")val viernes:Boolean,
    @ColumnInfo(name = "sabado")val sabado:Boolean,
    @ColumnInfo(name = "domingo")val domingo:Boolean,
    @ColumnInfo(name = "tipo_calificacion")val tipoCalificacion:String = ""
)