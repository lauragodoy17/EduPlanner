package com.softcg.myapplication.di

import android.content.Context
import androidx.room.Room
import com.softcg.myapplication.data.database.TareasDatabase.TareasDatabase
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
        Room.databaseBuilder(context, TareasDatabase::class.java, TAREAS_DATABASE_NAME).build()

    @Singleton
    @Provides
    fun provideTareasDao(db:TareasDatabase)=db.getTareasDao()
}