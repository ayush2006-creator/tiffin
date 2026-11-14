package com.example.propelassignment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.propelassignment.analytics.Analytics
import com.example.propelassignment.data.Kitchen
import com.example.propelassignment.data.KitchenRepository
import com.example.propelassignment.data.SubscriptionPlan
import com.example.propelassignment.persistence.AppPreferences
import com.example.propelassignment.persistence.PurchaseState
import com.example.propelassignment.persistence.MealPreferences
import com.example.propelassignment.persistence.BookedDabba
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AppDestination {
    data object KitchenList : AppDestination
    data class KitchenDetail(val kitchen: Kitchen) : AppDestination
    data class Paywall(val kitchen: Kitchen, val plan: SubscriptionPlan) : AppDestination
    data object AppSubscriptionPaywall : AppDestination
    data object BookedDabbas : AppDestination
}

/** A row shown by the single reusable paywall. The caller decides which charges apply. */
data class PaywallCharge(
    val label: String,
    val amount: Int,
    val detail: String
)

sealed interface KitchenLoadState {
    data object Loading : KitchenLoadState
    data class Content(val kitchens: List<Kitchen>) : KitchenLoadState
    data object Empty : KitchenLoadState
    data object Error : KitchenLoadState
}

data class TiffinUiState(
    val kitchenLoadState: KitchenLoadState = KitchenLoadState.Loading,
    val destination: AppDestination = AppDestination.KitchenList,
    val purchaseState: PurchaseState = PurchaseState(isPaid = false, purchaseTimestamp = null, plan = null, remainingBookings = 0, kitchenId = null, kitchenName = null, validUntilTimestamp = null),
    val selectedPlansByKitchen: Map<String, SubscriptionPlan> = emptyMap(),
    val bookedDabbas: List<BookedDabba> = emptyList(),
    val subscriptions: List<PurchaseState> = emptyList(),
    val isAppSubscribed: Boolean = false,
    val mealPreferences: MealPreferences = MealPreferences(),
    val selectedMenuDay: Int = 0,
    val purchaseSaveError: Boolean = false
)

/** Owns UI state through rotation and moves all storage and JSON work out of composables. */
class TiffinViewModel(
    private val repository: KitchenRepository,
    private val preferences: AppPreferences,
    private val analytics: Analytics,
    launchCount: Int
) : ViewModel() {
    private val persistedPurchase = preferences.purchaseState()
    private val _uiState = MutableStateFlow(
        TiffinUiState(
            purchaseState = persistedPurchase,
            bookedDabbas = preferences.bookedDabbas(),
            subscriptions = preferences.activeSubscriptions(),
            isAppSubscribed = preferences.isAppSubscribed(),
            destination = if (launchCount == THIRD_LAUNCH && !preferences.isAppSubscribed()) {
                AppDestination.AppSubscriptionPaywall
            } else {
                AppDestination.KitchenList
            }
        )
    )
    val uiState: StateFlow<TiffinUiState> = _uiState.asStateFlow()

    init {
        analytics.track("app_opened")
        loadKitchens()
    }

    fun loadKitchens() {
        _uiState.update { it.copy(kitchenLoadState = KitchenLoadState.Loading) }
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { repository.loadKitchens() }
            _uiState.update { current ->
                current.copy(kitchenLoadState = result.fold(
                    onSuccess = { kitchens -> if (kitchens.isEmpty()) KitchenLoadState.Empty else KitchenLoadState.Content(kitchens) },
                    onFailure = { KitchenLoadState.Error }
                ))
            }
        }
    }

    fun openKitchen(kitchen: Kitchen) {
        analytics.track("kitchen_viewed", mapOf("kitchen_id" to kitchen.id))
        val kitchenPurchase = preferences.purchaseStateFor(kitchen.id)
        _uiState.update { current ->
            current.copy(
                destination = AppDestination.KitchenDetail(kitchen),
                purchaseState = kitchenPurchase,
                selectedPlansByKitchen = current.selectedPlansByKitchen + (kitchen.id to (current.selectedPlansByKitchen[kitchen.id] ?: kitchenPurchase.plan ?: SubscriptionPlan.SIX_DAY)),
                mealPreferences = preferences.mealPreferences(kitchen.id),
                selectedMenuDay = 0
            )
        }
    }

    fun selectMenuDay(index: Int) {
        _uiState.update { it.copy(selectedMenuDay = index.coerceIn(0, 6)) }
    }

    fun onSubscribe(kitchen: Kitchen) {
        analytics.track("subscribe_clicked", mapOf("kitchen_id" to kitchen.id))
        val plan = _uiState.value.selectedPlansByKitchen[kitchen.id] ?: SubscriptionPlan.SIX_DAY
        _uiState.update { it.copy(destination = AppDestination.Paywall(kitchen, plan), purchaseSaveError = false) }
    }

    fun selectPlan(kitchenId: String, plan: SubscriptionPlan) {
        _uiState.update { it.copy(selectedPlansByKitchen = it.selectedPlansByKitchen + (kitchenId to plan)) }
    }

    fun goBack() {
        val destination = _uiState.value.destination
        _uiState.update {
            it.copy(destination = when (destination) {
                is AppDestination.Paywall -> AppDestination.KitchenDetail(destination.kitchen)
                AppDestination.AppSubscriptionPaywall -> AppDestination.KitchenList
                AppDestination.BookedDabbas -> AppDestination.KitchenList
                else -> AppDestination.KitchenList
            })
        }
    }

    fun completePurchase() {
        val timestamp = System.currentTimeMillis()
        if (_uiState.value.destination == AppDestination.AppSubscriptionPaywall) {
            val saved = preferences.completeAppSubscription(timestamp)
            if (saved) {
                analytics.track("purchase_completed", mapOf("price" to "1", "product" to "app_subscription"))
                _uiState.update { it.copy(isAppSubscribed = true, purchaseSaveError = false, destination = AppDestination.KitchenList) }
            } else {
                _uiState.update { it.copy(purchaseSaveError = true) }
            }
            return
        }
        val paywall = _uiState.value.destination as? AppDestination.Paywall
        if (paywall == null) {
            _uiState.update { it.copy(purchaseSaveError = true) }
            return
        }
        val returnKitchen = paywall.kitchen
        val plan = paywall.plan
        val payableTotal = returnKitchen.pricePerTiffin * plan.mealCredits + if (preferences.isAppSubscribed()) 0 else 1
        val existingPlan = preferences.purchaseStateFor(returnKitchen.id)
        val saved = when {
            existingPlan?.isPaid == true -> preferences.extendPlan(returnKitchen.id, plan)
            else -> preferences.completePurchase(timestamp, plan, returnKitchen)
        }
        if (saved) {
            val state = preferences.purchaseStateFor(returnKitchen.id)
            analytics.track("purchase_completed", mapOf("price" to payableTotal.toString(), "kitchen_id" to returnKitchen.id))
            _uiState.update { it.copy(purchaseState = state, subscriptions = preferences.activeSubscriptions(), isAppSubscribed = preferences.isAppSubscribed(), purchaseSaveError = false, destination = AppDestination.KitchenDetail(returnKitchen)) }
        } else {
            _uiState.update { it.copy(purchaseSaveError = true) }
        }
    }

    fun startExtension(kitchen: Kitchen, plan: SubscriptionPlan) {
        _uiState.update { it.copy(selectedPlansByKitchen = it.selectedPlansByKitchen + (kitchen.id to plan), destination = AppDestination.Paywall(kitchen, plan), purchaseSaveError = false) }
    }

    fun bookDabba(kitchen: Kitchen) {
        if (preferences.bookDabba(kitchen) != null) {
            _uiState.update { it.copy(purchaseState = preferences.purchaseStateFor(kitchen.id), bookedDabbas = preferences.bookedDabbas(), subscriptions = preferences.activeSubscriptions(), destination = AppDestination.BookedDabbas) }
        }
    }

    fun openBookedDabbas() {
        _uiState.update { it.copy(bookedDabbas = preferences.bookedDabbas(), subscriptions = preferences.activeSubscriptions(), destination = AppDestination.BookedDabbas) }
    }

    fun updateMealPreferences(kitchenId: String, transform: (MealPreferences) -> MealPreferences) {
        val updated = transform(_uiState.value.mealPreferences)
        if (preferences.saveMealPreferences(kitchenId, updated)) _uiState.update { it.copy(mealPreferences = updated) }
    }

    class Factory(
        private val repository: KitchenRepository,
        private val preferences: AppPreferences,
        private val analytics: Analytics,
        private val launchCount: Int
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TiffinViewModel::class.java))
            return TiffinViewModel(repository, preferences, analytics, launchCount) as T
        }
    }

    private companion object { const val THIRD_LAUNCH = 3 }
}
