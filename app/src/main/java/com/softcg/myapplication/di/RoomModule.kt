package com.softcg.myapplication.di

import android.content.Context
import androidx.room.Room
import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.data.Repositories.CalificacionesRespository
import com.softcg.myapplication.data.Repositories.EventosRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.data.Repositories.pruebaRepository
import com.softcg.myapplication.data.database.TareasDatabase.TareasDatabase
import com.softcg.myapplication.data.database.dao.AsignaturasDao
import com.softcg.myapplication.data.database.dao.CalificacionesDao
import com.softcg.myapplication.data.database.dao.EventosDao
import com.softcg.myapplication.data.database.dao.PruebaDao
import com.softcg.myapplication.data.database.dao.TareasDao
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models.Calificacion
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {
    private const val TAREAS_DATABASE_NAME="tareas_database"
    @Singleton
    @Provides
    fun provideRoom(@ApplicationContext context: Context)=
        Room.databaseBuilder(context, TareasDatabase::class.java, TAREAS_DATABASE_NAME).fallbackToDestructiveMigration().allowMainThreadQueries().build()

    @Singleton
    @Provides
    fun provideTareasDao(db:TareasDatabase)=db.getTareasDao()

    @Singleton
    @Provides
    fun provideTareasRepository(dao:TareasDao):TareasRepository = TareasRepository(dao)


    @Singleton
    @Provides
    fun providePruebaDao(db:TareasDatabase)=db.getPruebaDao()

    @Singleton
    @Provides
    fun providePruebaRepository(dao: PruebaDao):pruebaRepository = pruebaRepository(dao)

    @Singleton
    @Provides
    fun provideEventosDao(db:TareasDatabase)=db.getEventosDao()

    @Singleton
    @Provides
    fun provideEventosRepository(dao: EventosDao):EventosRepository = EventosRepository(dao)

    @Singleton
    @Provides
    fun provideAsignaturassDao(db:TareasDatabase)=db.getAsignaturasDao()

    @Singleton
    @Provides
    fun provideAsignaturasRepository(dao: AsignaturasDao):AsignaturasRepository = AsignaturasRepository(dao)

    @Singleton
    @Provides
    fun provideCalificacionesDao(db:TareasDatabase)=db.getCalificacionesDao()

    @Singleton
    @Provides
    fun provideCalificacionesRepository(dao: CalificacionesDao):CalificacionesRespository = CalificacionesRespository(dao)

}