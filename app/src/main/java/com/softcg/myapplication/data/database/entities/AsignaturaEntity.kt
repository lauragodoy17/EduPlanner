package com.softcg.myapplication.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "asignaturas_table")
data class AsignaturaEntity (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="id")val id: Int?=null,
    @ColumnInfo(name="nombre")val nombre:String,
    @ColumnInfo(name = "tutor")val tutor:String
)