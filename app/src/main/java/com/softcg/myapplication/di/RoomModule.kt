package com.softcg.myapplication.di

import android.content.Context
import androidx.room.Room
import com.softcg.myapplication.data.database.TareasDatabase.MIGRATION_13_14
import com.softcg.myapplication.data.database.TareasDatabase.MIGRATION_14_15
import com.softcg.myapplication.data.database.TareasDatabase.MIGRATION_15_16
import com.softcg.myapplication.data.database.TareasDatabase.MIGRATION_16_17
import com.softcg.myapplication.data.database.TareasDatabase.TareasDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Base de datos y DAOs. Los repositorios tienen @Inject constructor y no necesitan módulo. */
@Module
@InstallIn(SingletonComponent::class)
object RoomModule {
    private const val TAREAS_DATABASE_NAME="tareas_database"
    @Singleton
    @Provides
    fun provideRoom(@ApplicationContext context: Context)=
        Room.databaseBuilder(context, TareasDatabase::class.java, TAREAS_DATABASE_NAME)
            .addMigrations(MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17)
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()

    @Singleton
    @Provides
    fun provideGamificacionDao(db:TareasDatabase)=db.getGamificacionDao()

    @Singleton
    @Provides
    fun provideTareasDao(db:TareasDatabase)=db.getTareasDao()

    @Singleton
    @Provides
    fun provideEventosDao(db:TareasDatabase)=db.getEventosDao()

    @Singleton
    @Provides
    fun provideAsignaturassDao(db:TareasDatabase)=db.getAsignaturasDao()

    @Singleton
    @Provides
    fun provideCalificacionesDao(db:TareasDatabase)=db.getCalificacionesDao()

    @Singleton
    @Provides
    fun providePercepcionesDao(db:TareasDatabase)=db.getPercepcionesDao()
}
