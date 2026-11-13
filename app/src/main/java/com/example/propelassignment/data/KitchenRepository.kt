package com.example.propelassignment.data

import android.content.Context
import org.json.JSONArray

class KitchenRepository(private val context: Context) {
    fun loadKitchens(): List<Kitchen> {
        val raw = context.assets.open("kitchens.json").bufferedReader().use { it.readText() }
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
                    MenuDay(menu.getString("day"), menu.getString("lunch"), menu.getString("dinner"))
                }
            )
        }
    }
}
