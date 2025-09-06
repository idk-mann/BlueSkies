package com.example.myapplication2.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "cities",
    indices = [Index(value = ["name"])]
)
data class CityEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val state: String?,
    val country: String,
    val lat: Double,
    val lon: Double
)
