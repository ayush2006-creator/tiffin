package com.example.propelassignment.persistence

import android.content.Context

data class PurchaseState(val isPaid: Boolean, val purchaseTimestamp: Long?)

class AppPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("tiffin_preferences", Context.MODE_PRIVATE)

    /**
     * Called only from Activity.onCreate. The process-memory cache survives rotation but not a
     * process kill, so a restored process correctly counts as a new genuine application launch.
     */
    fun registerGenuineLaunch(): Int {
        synchronized(MobileMemory) {
            MobileMemory.launchCount?.let { return it }
            val count = preferences.getInt(LAUNCH_COUNT, 0) + 1
            // commit() intentionally completes the durable write before we report a launch.
            // The in-memory value is only a fast cache; disk remains authoritative after a kill.
            if (preferences.edit().putInt(LAUNCH_COUNT, count).commit()) {
                MobileMemory.launchCount = count
                return count
            }
            return preferences.getInt(LAUNCH_COUNT, 0)
        }
    }

    fun purchaseState() = PurchaseState(
        isPaid = preferences.getBoolean(IS_PAID, false),
        purchaseTimestamp = preferences.getLong(PURCHASE_TIMESTAMP, -1L).takeIf { it > 0L }
    )

    /** Persists both purchase values atomically before the UI shows success. */
    fun completePurchase(timestamp: Long): Boolean {
        return preferences.edit()
            .putBoolean(IS_PAID, true)
            .putLong(PURCHASE_TIMESTAMP, timestamp)
            .commit()
    }

    private companion object {
        const val LAUNCH_COUNT = "launch_count"
        const val IS_PAID = "is_paid"
        const val PURCHASE_TIMESTAMP = "purchase_timestamp"
    }

    /** Fast process-lifetime cache requested for launch handling; never used as durable storage. */
    private object MobileMemory {
        @Volatile var launchCount: Int? = null
    }
}
