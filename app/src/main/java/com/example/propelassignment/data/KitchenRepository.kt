package com.example.propelassignment.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class KitchenRepository(private val context: Context) {
    fun loadKitchens(): List<Kitchen> {
        val raw = context.assets.open("kitchens.json").bufferedReader().use { it.readText() }
        val breakfasts = JSONObject(
            context.assets.open("breakfasts.json").bufferedReader().use { it.readText() }
        )
        val kitchens = JSONArray(raw)
        return List(kitchens.length()) { index ->
            val item = kitchens.getJSONObject(index)
            val weekly = item.getJSONArray("weeklyMenu")
            Kitchen(
                id = item.getString("id"), name = item.getString("name"),
                cuisine = item.getString("cuisine"), pricePerTiffin = item.getInt("pricePerTiffin"),
                foodType = item.getString("foodType"), rating = item.getDouble("rating"),
                weeklyMenu = List(weekly.length()) { day ->
                    val menu = weekly.getJSONObject(day)
                    MenuDay(
                        day = menu.getString("day"),
                        breakfast = breakfasts.getString(menu.getString("day")),
                        lunch = menu.getString("lunch"),
                        dinner = menu.getString("dinner")
                    )
                }
            )
        }
    }
}
