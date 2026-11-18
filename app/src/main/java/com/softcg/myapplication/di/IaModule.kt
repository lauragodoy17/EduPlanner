package com.softcg.myapplication.di

import com.softcg.myapplication.data.network.ClienteHttp
import com.softcg.myapplication.data.network.ClienteHttpUrlConnection
import com.softcg.myapplication.data.network.GeminiRecomendaciones
import com.softcg.myapplication.data.network.VerificadorFuentesWeb
import com.softcg.myapplication.domain.ia.GeneradorRecomendaciones
import com.softcg.myapplication.domain.ia.VerificadorFuentes
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class IaModule {

    @Binds
    abstract fun bindGeneradorRecomendaciones(gemini: GeminiRecomendaciones): GeneradorRecomendaciones

    @Binds
    abstract fun bindVerificadorFuentes(verificador: VerificadorFuentesWeb): VerificadorFuentes

    @Binds
    abstract fun bindClienteHttp(cliente: ClienteHttpUrlConnection): ClienteHttp
}
