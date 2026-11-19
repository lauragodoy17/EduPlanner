package com.softcg.myapplication.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tareas_table", indices = [Index(value = ["id"], unique = true)])
data class TareaEntity (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="id")val id: Int? =null,
    @ColumnInfo(name="titulo")val titulo:String,
    @ColumnInfo(name = "descrip")val descrip:String,
    @ColumnInfo(name = "asignatura")val asignatura:String,
    @ColumnInfo(name= "fecha")val fecha:String,
    @ColumnInfo(name= "prioridad")val prioridad:Int,
    // Cuándo la marcó el usuario como entregada; null si sigue pendiente
    @ColumnInfo(name= "entregada_millis")val entregadaMillis:Long? = null,
    // Dueño (uid de Firebase). '' = creada antes de la versión 16: la adopta el primer usuario que abre la app
    @ColumnInfo(name = "uid", defaultValue = "")val uid:String = "",
    // Tema del catálogo del modelo predictivo (subject_topic); null si no se eligió
    @ColumnInfo(name = "tema")val tema:String? = null,
    // Asignatura por id: el nombre se conserva por compatibilidad con las pantallas existentes
    @ColumnInfo(name = "asignatura_id")val asignaturaId:Int? = null,
    // Hora límite "HH:mm" del día de entrega; null = hasta las 23:59 (ReglasEntrega.limite)
    @ColumnInfo(name = "hora_entrega")val horaEntrega:String? = null
)
