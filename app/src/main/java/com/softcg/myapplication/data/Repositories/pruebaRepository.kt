package com.softcg.myapplication.data.Repositories

import com.softcg.myapplication.data.database.dao.PruebaDao
import com.softcg.myapplication.data.database.dao.TareasDao
import com.softcg.myapplication.data.database.entities.TareaEntity
import javax.inject.Inject

class pruebaRepository @Inject constructor(private val pruebaDao: PruebaDao) {

    suspend fun getAllPrueba():List<TareaEntity>{
        return pruebaDao.getAllPrueba()
    }
}