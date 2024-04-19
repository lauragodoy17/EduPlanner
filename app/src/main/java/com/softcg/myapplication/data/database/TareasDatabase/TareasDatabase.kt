package com.softcg.myapplication.data.database.TareasDatabase

import androidx.room.Database
import androidx.room.RoomDatabase
import com.softcg.myapplication.data.database.dao.TareasDao
import com.softcg.myapplication.data.database.entities.TareaEntity

@Database(entities = [TareaEntity::class], version = 1)
abstract class TareasDatabase :RoomDatabase(){

    abstract fun getTareasDao():TareasDao
}