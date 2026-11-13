package com.example.propelassignment.data

data class Kitchen(
    val id: String,
    val name: String,
    val cuisine: String,
    val pricePerTiffin: Int,
    val foodType: String,
    val rating: Double,
    val weeklyMenu: List<MenuDay>
)

data class MenuDay(val day: String, val lunch: String, val dinner: String)
