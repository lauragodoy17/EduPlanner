package com.softcg.myapplication.di

import com.softcg.myapplication.core.Reloj
import com.softcg.myapplication.core.RelojSistema
import com.softcg.myapplication.data.network.FirebaseSesionUsuario
import com.softcg.myapplication.data.network.SesionUsuario
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class GamificacionModule {

    @Binds
    abstract fun bindReloj(reloj: RelojSistema): Reloj

    @Binds
    abstract fun bindSesionUsuario(sesion: FirebaseSesionUsuario): SesionUsuario
}
