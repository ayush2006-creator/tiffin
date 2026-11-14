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
    val remainingBookings: Int,
    val kitchenId: String?,
    val kitchenName: String?,
    val validUntilTimestamp: Long?
)

data class BookedDabba(
    val kitchenId: String,
    val kitchenName: String,
    val pricePerTiffin: Int,
    val bookedAt: Long
)

data class MealPreferences(
    val extraChapatis: Boolean = false,
    val gheeFree: Boolean = false,
    val lessOil: Boolean = false
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

    fun purchaseState(): PurchaseState = kitchenSubscriptions().lastOrNull() ?: emptyPurchaseState()

    fun purchaseStateFor(kitchenId: String): PurchaseState =
        kitchenSubscriptions().firstOrNull { it.kitchenId == kitchenId } ?: emptyPurchaseState()

    fun activeSubscriptions(): List<PurchaseState> = kitchenSubscriptions()

    fun isAppSubscribed(): Boolean = preferences.getBoolean(IS_PAID, false)

    /** Records the one-time app membership when it is bought before any kitchen plan. */
    fun completeAppSubscription(timestamp: Long): Boolean = preferences.edit()
        .putBoolean(IS_PAID, true)
        .putLong(PURCHASE_TIMESTAMP, timestamp)
        .commit()

    /** Meal customisation belongs to a kitchen, never to the whole application. */
    fun mealPreferences(kitchenId: String) = MealPreferences(
        extraChapatis = preferences.getBoolean(preferenceKey(EXTRA_CHAPATIS, kitchenId), false),
        gheeFree = preferences.getBoolean(preferenceKey(GHEE_FREE, kitchenId), false),
        lessOil = preferences.getBoolean(preferenceKey(LESS_OIL, kitchenId), false)
    )

    fun saveMealPreferences(kitchenId: String, value: MealPreferences): Boolean = preferences.edit()
        .putBoolean(preferenceKey(EXTRA_CHAPATIS, kitchenId), value.extraChapatis)
        .putBoolean(preferenceKey(GHEE_FREE, kitchenId), value.gheeFree)
        .putBoolean(preferenceKey(LESS_OIL, kitchenId), value.lessOil)
        .commit()

    /** Persists both purchase values atomically before the UI shows success. */
    fun completePurchase(timestamp: Long, plan: SubscriptionPlan, kitchen: Kitchen): Boolean {
        val subscriptions = kitchenSubscriptions().filterNot { it.kitchenId == kitchen.id } + PurchaseState(
            isPaid = true,
            purchaseTimestamp = timestamp,
            plan = plan,
            remainingBookings = plan.mealCredits,
            kitchenId = kitchen.id,
            kitchenName = kitchen.name,
            validUntilTimestamp = timestamp + plan.mealCredits * MILLIS_PER_DAY
        )
        return preferences.edit()
            .putBoolean(IS_PAID, true)
            .putLong(PURCHASE_TIMESTAMP, timestamp)
            .putString(PLAN, plan.name)
            .putInt(REMAINING_BOOKINGS, plan.mealCredits)
            .putString(PURCHASED_KITCHEN_ID, kitchen.id)
            .putString(PURCHASED_KITCHEN_NAME, kitchen.name)
            .putLong(VALID_UNTIL_TIMESTAMP, timestamp + plan.mealCredits * MILLIS_PER_DAY)
            .putString(SUBSCRIPTIONS, subscriptionsToJson(subscriptions).toString())
            .commit()
    }

    /** Adds prepaid dabba credits and extends the existing kitchen's plan immediately. */
    fun extendPlan(kitchenId: String, plan: SubscriptionPlan): Boolean {
        val now = System.currentTimeMillis()
        val current = purchaseStateFor(kitchenId)
        if (!current.isPaid) return false
        val newEnd = maxOf(now, current.validUntilTimestamp ?: now) + plan.mealCredits * MILLIS_PER_DAY
        val subscriptions = kitchenSubscriptions().map { state ->
            if (state.kitchenId == kitchenId) state.copy(plan = plan, remainingBookings = state.remainingBookings + plan.mealCredits, validUntilTimestamp = newEnd) else state
        }
        return preferences.edit()
            .putString(PLAN, plan.name)
            .putInt(REMAINING_BOOKINGS, preferences.getInt(REMAINING_BOOKINGS, 0) + plan.mealCredits)
            .putLong(VALID_UNTIL_TIMESTAMP, newEnd)
            .putString(SUBSCRIPTIONS, subscriptionsToJson(subscriptions).toString())
            .commit()
    }

    /** Reserves one prepaid tiffin and durably decrements the member's plan balance. */
    fun bookDabba(kitchen: Kitchen): BookedDabba? {
        val subscription = purchaseStateFor(kitchen.id)
        val available = subscription.remainingBookings
        if (!subscription.isPaid || available <= 0) return null
        val booking = BookedDabba(kitchen.id, kitchen.name, kitchen.pricePerTiffin, System.currentTimeMillis())
        val records = JSONArray(preferences.getString(BOOKED_DABBAS, "[]") ?: "[]")
        records.put(JSONObject().apply {
            put("kitchenId", booking.kitchenId)
            put("kitchenName", booking.kitchenName)
            put("pricePerTiffin", booking.pricePerTiffin)
            put("bookedAt", booking.bookedAt)
        })
        val subscriptions = kitchenSubscriptions().map { if (it.kitchenId == kitchen.id) it.copy(remainingBookings = available - 1) else it }
        val saved = preferences.edit()
            .putInt(REMAINING_BOOKINGS, available - 1)
            .putString(BOOKED_DABBAS, records.toString())
            .putString(SUBSCRIPTIONS, subscriptionsToJson(subscriptions).toString())
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

    private fun kitchenSubscriptions(): List<PurchaseState> = runCatching {
        val records = JSONArray(preferences.getString(SUBSCRIPTIONS, "[]") ?: "[]")
        List(records.length()) { index -> records.getJSONObject(index).toPurchaseState() }
    }.getOrDefault(emptyList()).ifEmpty {
        // Preserve subscriptions made before per-kitchen support was added.
        val legacyKitchen = preferences.getString(PURCHASED_KITCHEN_ID, null)
        if (preferences.getBoolean(IS_PAID, false) && legacyKitchen != null) listOf(
            PurchaseState(true, preferences.getLong(PURCHASE_TIMESTAMP, -1L), preferences.getString(PLAN, null)?.let { runCatching { SubscriptionPlan.valueOf(it) }.getOrNull() }, preferences.getInt(REMAINING_BOOKINGS, 0), legacyKitchen, preferences.getString(PURCHASED_KITCHEN_NAME, null), preferences.getLong(VALID_UNTIL_TIMESTAMP, -1L))
        ) else emptyList()
    }

    private fun emptyPurchaseState() = PurchaseState(false, null, null, 0, null, null, null)

    private fun preferenceKey(base: String, kitchenId: String) = "${base}_$kitchenId"

    private fun subscriptionsToJson(subscriptions: List<PurchaseState>) = JSONArray().apply {
        subscriptions.forEach { state -> put(JSONObject().apply { put("kitchenId", state.kitchenId); put("kitchenName", state.kitchenName); put("plan", state.plan?.name); put("purchaseTimestamp", state.purchaseTimestamp); put("remainingBookings", state.remainingBookings); put("validUntilTimestamp", state.validUntilTimestamp) }) }
    }

    private fun JSONObject.toPurchaseState() = PurchaseState(true, optLong("purchaseTimestamp").takeIf { it > 0 }, optString("plan").let { runCatching { SubscriptionPlan.valueOf(it) }.getOrNull() }, optInt("remainingBookings"), optString("kitchenId", null), optString("kitchenName", null), optLong("validUntilTimestamp").takeIf { it > 0 })

    private companion object {
        const val LAUNCH_COUNT = "launch_count"
        const val IS_PAID = "is_paid"
        const val PURCHASE_TIMESTAMP = "purchase_timestamp"
        const val PLAN = "subscription_plan"
        const val REMAINING_BOOKINGS = "remaining_bookings"
        const val BOOKED_DABBAS = "booked_dabbas"
        const val PURCHASED_KITCHEN_ID = "purchased_kitchen_id"
        const val PURCHASED_KITCHEN_NAME = "purchased_kitchen_name"
        const val VALID_UNTIL_TIMESTAMP = "valid_until_timestamp"
        const val SUBSCRIPTIONS = "kitchen_subscriptions"
        const val EXTRA_CHAPATIS = "extra_chapatis"
        const val GHEE_FREE = "ghee_free"
        const val LESS_OIL = "less_oil"
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    }

    /** Fast process-lifetime cache requested for launch handling; never used as durable storage. */
    private object MobileMemory {
        @Volatile var launchCount: Int? = null
    }
}
