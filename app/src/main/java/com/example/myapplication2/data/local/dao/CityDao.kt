package com.example.myapplication2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication2.data.local.entity.CityEntity


@Dao
interface CityDao {
    @Query("SELECT * FROM cities WHERE name LIKE :query || '%' COLLATE NOCASE LIMIT 20")
    suspend fun searchCities(query: String): List<CityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cities: List<CityEntity>)

    @Query("SELECT COUNT(*) FROM cities")
    suspend fun countCities(): Int   // Add this!
}
