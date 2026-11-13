package com.example.propelassignment.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.propelassignment.R
import com.example.propelassignment.analytics.Analytics
import com.example.propelassignment.data.Kitchen
import com.example.propelassignment.data.KitchenRepository
import com.example.propelassignment.persistence.AppPreferences
import com.example.propelassignment.persistence.PurchaseState
import java.text.DateFormat
import java.util.Date

private sealed interface Screen { data object List : Screen; data class Detail(val kitchen: Kitchen) : Screen; data class Paywall(val kitchen: Kitchen?) : Screen }
private sealed interface KitchenLoadState { data object Loading : KitchenLoadState; data class Content(val kitchens: List<Kitchen>) : KitchenLoadState; data object Empty : KitchenLoadState; data class Error(val message: String) : KitchenLoadState }

@Composable
fun TiffinApp(repository: KitchenRepository, preferences: AppPreferences, analytics: Analytics, launchCount: Int) {
    var loadState by remember { mutableStateOf<KitchenLoadState>(KitchenLoadState.Loading) }
    var screen by remember { mutableStateOf<Screen>(Screen.List) }
    var purchaseState by remember { mutableStateOf(preferences.purchaseState()) }
    fun load() {
        loadState = KitchenLoadState.Loading
        try {
            val kitchens = repository.loadKitchens()
            loadState = if (kitchens.isEmpty()) KitchenLoadState.Empty else KitchenLoadState.Content(kitchens)
        } catch (exception: Exception) { loadState = KitchenLoadState.Error("We couldn't load the kitchen menu. Please try again.") }
    }
    LaunchedEffect(Unit) {
        load()
        analytics.track("app_opened")
        if (launchCount == 3 && !purchaseState.isPaid) screen = Screen.Paywall(null)
    }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val current = screen) {
            Screen.List -> KitchenList(loadState, onKitchen = { kitchen ->
                analytics.track("kitchen_viewed", mapOf("kitchen_id" to kitchen.id)); screen = Screen.Detail(kitchen)
            }, onRetry = ::load)
            is Screen.Detail -> DetailScreen(current.kitchen, purchaseState.isPaid, onBack = { screen = Screen.List }, onSubscribe = {
                analytics.track("subscribe_clicked", mapOf("kitchen_id" to current.kitchen.id)); if (!purchaseState.isPaid) screen = Screen.Paywall(current.kitchen)
            })
            is Screen.Paywall -> PaywallScreen(current.kitchen, onBack = { screen = if (current.kitchen == null) Screen.List else Screen.Detail(current.kitchen) }, onComplete = { timestamp ->
                preferences.completePurchase(timestamp); purchaseState = preferences.purchaseState(); analytics.track("purchase_completed", mapOf("price" to "1")); screen = Screen.List
            })
        }
    }
}

@Composable private fun KitchenList(state: KitchenLoadState, onKitchen: (Kitchen) -> Unit, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        AppHeader("Kadugudabba", "Home kitchens near you")
        when (state) {
            KitchenLoadState.Loading -> CenterMessage { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            KitchenLoadState.Empty -> CenterMessage { Text("No kitchens are available yet. Please check back shortly.") }
            is KitchenLoadState.Error -> CenterMessage { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(state.message); Spacer(Modifier.height(12.dp)); OutlinedButton(onClick = onRetry) { Text("Try again") } } }
            is KitchenLoadState.Content -> LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { WelcomeBanner() }
                item { Text("Home Kitchens Near You", style = MaterialTheme.typography.titleLarge) }
                items(state.kitchens, key = { it.id }) { KitchenCard(it) { onKitchen(it) } }
            }
        }
    }
}

@Composable private fun AppHeader(title: String, subtitle: String) = Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 20.dp, vertical = 16.dp)) {
    Text("▣  $title", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable private fun WelcomeBanner() = Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = RoundedCornerShape(16.dp)) {
    Column(Modifier.padding(18.dp)) { Text("FIRST DABBA GUARANTEE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary); Text("Try your first homestyle meal for ₹1", style = MaterialTheme.typography.titleLarge); Text("Authentic regional home kitchens delivered to your door.", style = MaterialTheme.typography.bodySmall) }
}

@Composable private fun KitchenCard(kitchen: Kitchen, onClick: () -> Unit) = Card(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
) {
    Column(Modifier.padding(16.dp)) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(12.dp))) {
            Image(
                painter = painterResource(mealImageFor(kitchen.id)),
                contentDescription = "${kitchen.name} meal",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            ) { Text("Homestyle ${kitchen.cuisine}", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall) }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) { Column(Modifier.weight(1f)) { Text(kitchen.name, style = MaterialTheme.typography.titleMedium); Text("${kitchen.cuisine} • ${kitchen.foodType}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text("₹${kitchen.pricePerTiffin}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(8.dp)); Row(verticalAlignment = Alignment.CenterVertically) { DietaryIndicator(kitchen.foodType); Spacer(Modifier.width(6.dp)); Text("★ ${kitchen.rating}  ·  Fresh batch daily", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary) }
        Spacer(Modifier.height(12.dp)); Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("View menu & subscribe") }
    }
}

@Composable private fun DietaryIndicator(foodType: String) {
    val vegetarian = foodType == "Vegetarian"
    val tint = if (vegetarian) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    Surface(Modifier.width(16.dp).height(16.dp).border(1.5.dp, tint, RoundedCornerShape(4.dp)), color = MaterialTheme.colorScheme.surface) {
        Text(if (vegetarian) "●" else "▲", color = tint, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 0.dp))
    }
}

private fun mealImageFor(kitchenId: String): Int = when (kitchenId) {
    "dakshin", "malabar" -> R.drawable.meal_south_indian
    "punjabi", "awadhi", "hyderabad" -> R.drawable.meal_punjabi
    "ma", "gujarati", "rajasthani" -> R.drawable.meal_healthy
    else -> R.drawable.meal_north_indian
}

@Composable private fun DetailScreen(kitchen: Kitchen, isPaid: Boolean, onBack: () -> Unit, onSubscribe: () -> Unit) = Column(Modifier.fillMaxSize()) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text("‹", Modifier.clickable(onClick = onBack).padding(end = 12.dp), style = MaterialTheme.typography.headlineSmall); Column { Text(kitchen.name, style = MaterialTheme.typography.titleLarge); Text("${kitchen.cuisine} • ★ ${kitchen.rating}", style = MaterialTheme.typography.bodySmall) } }
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.padding(16.dp)) { Text("${kitchen.foodType} kitchen", style = MaterialTheme.typography.labelLarge); Text("₹${kitchen.pricePerTiffin} / tiffin", style = MaterialTheme.typography.headlineSmall); Text("Wholesome meals prepared fresh every day.", style = MaterialTheme.typography.bodyMedium) } } }
        item { Text("Weekly Thali Schedule", style = MaterialTheme.typography.titleLarge) }
        items(kitchen.weeklyMenu) { day -> MenuDayCard(day.day, day.lunch, day.dinner) }
        item { Button(onClick = onSubscribe, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(50), enabled = !isPaid) { Text(if (isPaid) "Subscription active" else "Subscribe for ₹1 trial") } }
    }
}

@Composable private fun MenuDayCard(day: String, lunch: String, dinner: String) = Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.padding(14.dp)) { Text(day.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary); Text("Lunch  ·  $lunch", style = MaterialTheme.typography.bodyMedium); Spacer(Modifier.height(5.dp)); Text("Dinner  ·  $dinner", style = MaterialTheme.typography.bodyMedium) } }

@Composable private fun PaywallScreen(kitchen: Kitchen?, onBack: () -> Unit, onComplete: (Long) -> Unit) {
    val timestamp = remember { System.currentTimeMillis() }
    val date = remember(timestamp) { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp + 24 * 60 * 60 * 1000L)) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("‹", Modifier.clickable(onClick = onBack), style = MaterialTheme.typography.headlineSmall) }
        item { Text("Taste the difference. Just ₹1", style = MaterialTheme.typography.displaySmall) }
        item { Text(if (kitchen == null) "A weekly subscription to delicious local home food." else "Start your ${kitchen.name} subscription today.", style = MaterialTheme.typography.bodyLarge) }
        item { BillingTimeline(date) }
        item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.padding(16.dp)) { Text("Simulated purchase", style = MaterialTheme.typography.titleMedium); Text("No payment SDK or real charge is used. This prototype only records your confirmation on this device.", style = MaterialTheme.typography.bodySmall) } } }
        item { Button(onClick = { onComplete(timestamp) }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(50)) { Text("Start trial for ₹1") } }
        item { Text("By continuing, you agree to the disclosed billing schedule. You may cancel any time in this prototype.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable private fun BillingTimeline(futureDate: String) = Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Transparent billing timeline", style = MaterialTheme.typography.titleMedium); Row { Text("1", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Spacer(Modifier.width(12.dp)); Column { Text("Today (instant)", style = MaterialTheme.typography.titleMedium); Text("₹1 is charged now for your trial.", style = MaterialTheme.typography.bodyMedium) } }; Divider(); Row { Text("2", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Spacer(Modifier.width(12.dp)); Column { Text("In 24 hours", style = MaterialTheme.typography.titleMedium); Text("₹249 will be charged on $futureDate.", style = MaterialTheme.typography.bodyMedium) } } } }

@Composable private fun CenterMessage(content: @Composable () -> Unit) = androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { content() }
