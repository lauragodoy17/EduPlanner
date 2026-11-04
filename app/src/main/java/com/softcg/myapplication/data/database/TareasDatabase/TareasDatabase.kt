package com.softcg.myapplication.data.database.TareasDatabase

import androidx.room.Database
import androidx.room.RoomDatabase
import com.softcg.myapplication.data.database.dao.AsignaturasDao
import com.softcg.myapplication.data.database.dao.CalificacionesDao
import com.softcg.myapplication.data.database.dao.EventosDao
import com.softcg.myapplication.data.database.dao.TareasDao
import com.softcg.myapplication.data.database.dao.GamificacionDao
import com.softcg.myapplication.data.database.entities.AsignaturaEntity
import com.softcg.myapplication.data.database.entities.AsistenciaEntity
import com.softcg.myapplication.data.database.entities.AvatarAparienciaEntity
import com.softcg.myapplication.data.database.entities.AvatarEquipadoEntity
import com.softcg.myapplication.data.database.entities.CalificacionEntity
import com.softcg.myapplication.data.database.entities.EventoEntity
import com.softcg.myapplication.data.database.entities.InventarioEntity
import com.softcg.myapplication.data.database.entities.MisionAsignadaEntity
import com.softcg.myapplication.data.database.entities.MovimientoEduCoinsEntity
import com.softcg.myapplication.data.database.entities.SesionEnfoqueEntity
import com.softcg.myapplication.data.database.entities.TareaEntity

@Database(
    entities = [
        TareaEntity::class, EventoEntity::class, AsignaturaEntity::class, CalificacionEntity::class,
        MovimientoEduCoinsEntity::class, AsistenciaEntity::class, InventarioEntity::class, AvatarEquipadoEntity::class,
        AvatarAparienciaEntity::class, MisionAsignadaEntity::class, SesionEnfoqueEntity::class
    ],
    version = 15,
    exportSchema = false
)
abstract class TareasDatabase :RoomDatabase(){

    abstract fun getGamificacionDao():GamificacionDao

    abstract fun getTareasDao():TareasDao

    abstract fun getAsignaturasDao():AsignaturasDao

    abstract fun getEventosDao():EventosDao

    abstract fun getCalificacionesDao():CalificacionesDao
}