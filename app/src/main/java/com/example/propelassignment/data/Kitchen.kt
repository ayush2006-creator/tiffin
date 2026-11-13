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

data class MenuDay(
    val day: String,
    val breakfast: String,
    val lunch: String,
    val dinner: String
)

enum class SubscriptionPlan(val label: String, val mealCredits: Int) {
    SIX_DAY("6 Days Plan", 6),
    TWENTY_SIX_DAY("26 Days Plan", 26)
}
