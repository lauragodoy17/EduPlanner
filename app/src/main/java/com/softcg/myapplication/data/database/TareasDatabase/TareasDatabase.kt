package com.softcg.myapplication.data.database.TareasDatabase

import androidx.room.Database
import androidx.room.RoomDatabase
import com.softcg.myapplication.data.database.dao.AsignaturasDao
import com.softcg.myapplication.data.database.dao.EventosDao
import com.softcg.myapplication.data.database.dao.PruebaDao
import com.softcg.myapplication.data.database.dao.TareasDao
import com.softcg.myapplication.data.database.entities.AsignaturaEntity
import com.softcg.myapplication.data.database.entities.EventoEntity
import com.softcg.myapplication.data.database.entities.TareaEntity

@Database(entities = [TareaEntity::class, EventoEntity::class, AsignaturaEntity::class], version = 8, exportSchema = false)
abstract class TareasDatabase :RoomDatabase(){

    abstract fun getTareasDao():TareasDao

    abstract fun getPruebaDao():PruebaDao

    abstract fun getAsignaturasDao():AsignaturasDao

    abstract fun getEventosDao():EventosDao
}