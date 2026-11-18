package com.softcg.myapplication.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Lo que solo el estudiante puede decir: interés en una asignatura, dificultad de una tarea o tema
 * y el método con el que estudia. [origen] identifica la pregunta ("TAREA:12", "INTERES:3:2026-11"):
 * responder otra vez la misma pregunta reemplaza la respuesta, nunca la duplica.
 */
@Entity(tableName = "percepciones", primaryKeys = ["uid", "tipo", "origen"])
data class PercepcionEntity(
    @ColumnInfo(name = "uid") val uid: String,
    @ColumnInfo(name = "tipo") val tipo: String,
    @ColumnInfo(name = "origen") val origen: String,
    @ColumnInfo(name = "asignatura_id") val asignaturaId: Int,
    @ColumnInfo(name = "tema") val tema: String?,
    // INTERES y DIFICULTAD: 0-10
    @ColumnInfo(name = "valor") val valor: Float?,
    // METODO: nombre del método del catálogo
    @ColumnInfo(name = "texto") val texto: String?,
    @ColumnInfo(name = "fecha_millis") val fechaMillis: Long
)
