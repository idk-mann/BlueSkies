package com.example.myapplication2.model

data class City(
    val id: Int,
    val name: String,
    val state: String?,
    val country: String,
    val coord: Coord
) {
    override fun toString(): String {
        val statePart = if (state.isNullOrBlank()) "" else ", $state"
        return "$name$statePart, $country"
    }
}

data class Coord(
    val lon: Double,
    val lat: Double
)
