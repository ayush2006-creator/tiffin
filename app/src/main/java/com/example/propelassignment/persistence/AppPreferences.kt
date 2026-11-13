package com.example.propelassignment.persistence

import android.content.Context

data class PurchaseState(val isPaid: Boolean, val purchaseTimestamp: Long?)

class AppPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("tiffin_preferences", Context.MODE_PRIVATE)

    /** Called only from Activity.onCreate: configuration changes retain the same instance state. */
    fun registerGenuineLaunch(isRecreating: Boolean): Int {
        if (isRecreating) return preferences.getInt(LAUNCH_COUNT, 0)
        val count = preferences.getInt(LAUNCH_COUNT, 0) + 1
        preferences.edit().putInt(LAUNCH_COUNT, count).apply()
        return count
    }

    fun purchaseState() = PurchaseState(
        isPaid = preferences.getBoolean(IS_PAID, false),
        purchaseTimestamp = preferences.getLong(PURCHASE_TIMESTAMP, -1L).takeIf { it > 0L }
    )

    fun completePurchase(timestamp: Long) {
        preferences.edit().putBoolean(IS_PAID, true).putLong(PURCHASE_TIMESTAMP, timestamp).apply()
    }

    private companion object {
        const val LAUNCH_COUNT = "launch_count"
        const val IS_PAID = "is_paid"
        const val PURCHASE_TIMESTAMP = "purchase_timestamp"
    }
}
