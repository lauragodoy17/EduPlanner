package com.softcg.myapplication.di

import com.softcg.myapplication.BuildConfig
import com.softcg.myapplication.data.network.EncuestaSheetsService
import com.softcg.myapplication.data.preferencias.PreferenciasEncuesta
import com.softcg.myapplication.domain.encuesta.AlmacenEncuesta
import com.softcg.myapplication.domain.encuesta.EnviadorEncuesta
import com.softcg.myapplication.domain.encuesta.VersionApp
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class EncuestaModule {

    @Binds
    abstract fun bindEnviadorEncuesta(servicio: EncuestaSheetsService): EnviadorEncuesta

    @Binds
    abstract fun bindAlmacenEncuesta(preferencias: PreferenciasEncuesta): AlmacenEncuesta

    companion object {
        @Provides
        fun provideVersionApp(): VersionApp = VersionApp("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
    }
}
