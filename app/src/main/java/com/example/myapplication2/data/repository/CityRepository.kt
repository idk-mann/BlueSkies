package com.example.myapplication2.data.repository

import android.content.Context
import com.example.myapplication2.data.local.dao.CityDao
import com.example.myapplication2.data.local.entity.CityEntity
import com.example.myapplication2.model.City
import com.example.myapplication2.model.Coord
import com.example.myapplication2.data.local.database.AppDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CityRepository private constructor(
    private val cityDao: CityDao,
    private val appContext: Context
) {

    companion object {
        @Volatile
        private var INSTANCE: CityRepository? = null

        fun getInstance(context: Context): CityRepository {
            return INSTANCE ?: synchronized(this) {
                val database = AppDatabase.getDatabase(context)
                val instance = CityRepository(database.cityDao(), context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun searchCities(query: String): List<City> {
        return cityDao.searchCities(query).map { it.toDomain() }
    }

    suspend fun insertAll(cities: List<City>) {
        val entities = cities.map { it.toEntity() }
        cityDao.insertAll(entities)
    }

    suspend fun countCities(): Int {
        return cityDao.countCities()
    }

    suspend fun ensurePreloaded() {
        val count = cityDao.countCities()
        if (count == 0) {
            val cities = loadCityListFromJson()
            cityDao.insertAll(cities)
        }
    }

    private fun loadCityListFromJson(): List<CityEntity> {
        val inputStream = appContext.resources.openRawResource(
            com.example.myapplication2.R.raw.city_list
        )
        val json = inputStream.bufferedReader().use { it.readText() }

        val gson = Gson()
        val listType = object : TypeToken<List<City>>() {}.type
        val cityModels: List<City> = gson.fromJson(json, listType)

        return cityModels.map { it.toEntity() }
    }
}


fun CityEntity.toDomain(): City {
    return City(
        id = id,
        name = name,
        state = state,
        country = country,
        coord = Coord(lon = lon, lat = lat)
    )
}

fun City.toEntity(): CityEntity {
    return CityEntity(
        id = id,
        name = name,
        state = state,
        country = country,
        lat = coord.lat,
        lon = coord.lon
    )
}
