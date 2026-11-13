package com.example.propelassignment.persistence

import android.content.Context
import com.example.propelassignment.data.SubscriptionPlan
import com.example.propelassignment.data.Kitchen
import org.json.JSONArray
import org.json.JSONObject

data class PurchaseState(
    val isPaid: Boolean,
    val purchaseTimestamp: Long?,
    val plan: SubscriptionPlan?,
    val remainingBookings: Int
)

data class BookedDabba(
    val kitchenId: String,
    val kitchenName: String,
    val pricePerTiffin: Int,
    val bookedAt: Long
)

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
        purchaseTimestamp = preferences.getLong(PURCHASE_TIMESTAMP, -1L).takeIf { it > 0L },
        plan = preferences.getString(PLAN, null)?.let { runCatching { SubscriptionPlan.valueOf(it) }.getOrNull() },
        remainingBookings = preferences.getInt(REMAINING_BOOKINGS, 0)
    )

    /** Persists both purchase values atomically before the UI shows success. */
    fun completePurchase(timestamp: Long, plan: SubscriptionPlan): Boolean {
        return preferences.edit()
            .putBoolean(IS_PAID, true)
            .putLong(PURCHASE_TIMESTAMP, timestamp)
            .putString(PLAN, plan.name)
            .putInt(REMAINING_BOOKINGS, plan.mealCredits)
            .commit()
    }

    /** Reserves one prepaid tiffin and durably decrements the member's plan balance. */
    fun bookDabba(kitchen: Kitchen): BookedDabba? {
        val available = preferences.getInt(REMAINING_BOOKINGS, 0)
        if (!preferences.getBoolean(IS_PAID, false) || available <= 0) return null
        val booking = BookedDabba(kitchen.id, kitchen.name, kitchen.pricePerTiffin, System.currentTimeMillis())
        val records = JSONArray(preferences.getString(BOOKED_DABBAS, "[]") ?: "[]")
        records.put(JSONObject().apply {
            put("kitchenId", booking.kitchenId)
            put("kitchenName", booking.kitchenName)
            put("pricePerTiffin", booking.pricePerTiffin)
            put("bookedAt", booking.bookedAt)
        })
        val saved = preferences.edit()
            .putInt(REMAINING_BOOKINGS, available - 1)
            .putString(BOOKED_DABBAS, records.toString())
            .commit()
        return booking.takeIf { saved }
    }

    fun bookedDabbas(): List<BookedDabba> = runCatching {
        val records = JSONArray(preferences.getString(BOOKED_DABBAS, "[]") ?: "[]")
        List(records.length()) { index ->
            val item = records.getJSONObject(index)
            BookedDabba(item.getString("kitchenId"), item.getString("kitchenName"), item.getInt("pricePerTiffin"), item.getLong("bookedAt"))
        }.sortedByDescending { it.bookedAt }
    }.getOrDefault(emptyList())

    private companion object {
        const val LAUNCH_COUNT = "launch_count"
        const val IS_PAID = "is_paid"
        const val PURCHASE_TIMESTAMP = "purchase_timestamp"
        const val PLAN = "subscription_plan"
        const val REMAINING_BOOKINGS = "remaining_bookings"
        const val BOOKED_DABBAS = "booked_dabbas"
    }

    /** Fast process-lifetime cache requested for launch handling; never used as durable storage. */
    private object MobileMemory {
        @Volatile var launchCount: Int? = null
    }
}
