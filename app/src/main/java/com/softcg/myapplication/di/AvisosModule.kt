package com.softcg.myapplication.di

import com.softcg.myapplication.domain.notificaciones.ProgramadorAvisos
import com.softcg.myapplication.ui.notifications.ProgramadorAlarmas
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AvisosModule {

    /** El dominio planifica; AlarmManager es un detalle de Android. En los tests se sustituye. */
    @Binds
    abstract fun bindProgramadorAvisos(alarmas: ProgramadorAlarmas): ProgramadorAvisos
}
