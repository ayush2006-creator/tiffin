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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.propelassignment.R
import com.example.propelassignment.data.Kitchen
import java.text.DateFormat
import java.util.Date

@Composable
fun TiffinApp(viewModel: TiffinViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val current = uiState.destination) {
            AppDestination.KitchenList -> KitchenList(uiState.kitchenLoadState, viewModel::openKitchen, viewModel::loadKitchens)
            is AppDestination.KitchenDetail -> DetailScreen(
                kitchen = current.kitchen,
                isPaid = uiState.purchaseState.isPaid,
                selectedDay = uiState.selectedMenuDay,
                onBack = viewModel::goBack,
                onDaySelect = viewModel::selectMenuDay,
                onSubscribe = { viewModel.onSubscribe(current.kitchen) }
            )
            is AppDestination.Paywall -> PaywallScreen(current.kitchen, viewModel::goBack, viewModel::completePurchase, uiState.purchaseSaveError)
        }
    }
}

@Composable private fun KitchenList(state: KitchenLoadState, onKitchen: (Kitchen) -> Unit, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        AppHeader("Kadugudabba", "Home kitchens near you")
        when (state) {
            KitchenLoadState.Loading -> CenterMessage { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            KitchenLoadState.Empty -> CenterMessage { Text("No kitchens are available yet. Please check back shortly.") }
            KitchenLoadState.Error -> CenterMessage { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("We couldn't load the kitchen menu. Please try again."); Spacer(Modifier.height(12.dp)); OutlinedButton(onClick = onRetry) { Text("Try again") } } }
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

@Composable private fun DetailScreen(
    kitchen: Kitchen,
    isPaid: Boolean,
    selectedDay: Int,
    onBack: () -> Unit,
    onDaySelect: (Int) -> Unit,
    onSubscribe: () -> Unit
) = Column(Modifier.fillMaxSize()) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("‹", Modifier.clickable(onClick = onBack).padding(end = 12.dp), style = MaterialTheme.typography.headlineSmall)
        Text("▣", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(8.dp)); Text("Dabba Detail", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Text("●", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
    }
    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { KitchenHero(kitchen) }
        item { KitchenSummary(kitchen) }
        item { DeliveryTrustRow() }
        item {
            Text("Weekly Thali Schedule", style = MaterialTheme.typography.titleLarge)
            Text("Curated homestyle varieties rotated daily", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            DayPicker(kitchen.weeklyMenu.map { it.day }, selectedDay, onDaySelect)
        }
        item {
            val day = kitchen.weeklyMenu[selectedDay]
            MealTray("☀", "Lunch Dabba", "North-Indian Comfort Meal • 620 kcal", day.lunch, "Standard Thali")
        }
        item {
            val day = kitchen.weeklyMenu[selectedDay]
            MealTray("◔", "Dinner Dabba", "Light & High-Protein Night Meal • 540 kcal", day.dinner, "Wholesome")
        }
        item { CustomizationCard() }
        item { SubscriptionPlans() }
        item { TestimonialCard() }
        item { Spacer(Modifier.height(4.dp)) }
    }
    Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("₹1", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary); Text("First dabba trial • Cancel anytime", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Button(onClick = onSubscribe, enabled = !isPaid, modifier = Modifier.height(48.dp), shape = RoundedCornerShape(50)) { Text(if (isPaid) "Subscription active" else "Subscribe Plan  →") }
        }
    }
}

@Composable private fun KitchenHero(kitchen: Kitchen) = androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp))) {
    Image(painterResource(mealImageFor(kitchen.id)), "${kitchen.name} meal", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    val kitchenTag = if (kitchen.foodType == "Vegetarian") "●  Pure Veg Kitchen" else "●  Homestyle Kitchen"
    Surface(Modifier.align(Alignment.TopStart).padding(10.dp), shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.tertiaryContainer) { Text(kitchenTag, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer) }
    Surface(Modifier.align(Alignment.BottomStart).padding(10.dp), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .94f)) { Text("Starts at ₹${kitchen.pricePerTiffin} / dabba", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
}

@Composable private fun KitchenSummary(kitchen: Kitchen) = Column {
    Row(verticalAlignment = Alignment.Top) { Column(Modifier.weight(1f)) { Text(kitchen.name, style = MaterialTheme.typography.headlineSmall); Text("● Chef Sunita Sharma • 12 yrs cooking with love", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) { Text("★ ${kitchen.rating} (${(kitchen.rating * 70).toInt()})", Modifier.padding(horizontal = 8.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall) } }
}

@Composable private fun DeliveryTrustRow() = Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
    TrustMetric("♙", "100% Desi", "Pure homemade")
    TrustMetric("♧", "Zero Soda", "No preservatives")
    TrustMetric("✓", "FSSAI Certified", "#122318000492")
} }

@Composable private fun TrustMetric(icon: String, title: String, subtitle: String) = Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(icon, color = MaterialTheme.colorScheme.tertiary); Text(title, style = MaterialTheme.typography.labelSmall); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }

@Composable private fun DayPicker(days: List<String>, selectedDay: Int, onSelect: (Int) -> Unit) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    days.forEachIndexed { index, day ->
        val selected = index == selectedDay
        Surface(modifier = Modifier.weight(1f).clickable { onSelect(index) }, shape = RoundedCornerShape(8.dp), color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, shadowElevation = if (selected) 4.dp else 0.dp) {
            Column(Modifier.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(day.take(3).uppercase(), style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant); Text("${14 + index}", style = MaterialTheme.typography.labelLarge, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface); Text("•", style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable private fun MealTray(icon: String, title: String, subtitle: String, meal: String, tag: String) = Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) { Column(Modifier.padding(14.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) { Text(icon, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)) { Text("$title  ●", style = MaterialTheme.typography.titleMedium); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Text(tag, Modifier.padding(horizontal = 6.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall) } }
    Spacer(Modifier.height(10.dp)); Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { meal.split(",").forEach { item -> Text("◉  ${item.trim()}", style = MaterialTheme.typography.bodySmall) } } }
    Spacer(Modifier.height(8.dp)); Text("● 18g Protein    • 68g Carbs    ● 14g Good Fats", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
} }

@Composable private fun CustomizationCard() = Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.padding(14.dp)) { Row { Text("☷", color = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text("Daily Customization", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium); Text("Free Adjustments", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }; Text("Customize your tiffin every day up to 2 hours before kitchen prep begins.", style = MaterialTheme.typography.bodySmall); Spacer(Modifier.height(8.dp)); Text("✓ Less Oil & Spice     ✓ Diabetic Millet Base     ⊕ Extra 2 Rotis (+₹10)", style = MaterialTheme.typography.labelSmall) } }

@Composable private fun SubscriptionPlans() = Column { Row { Text("Subscription Plans", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge); Text("Pause or cancel anytime", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }; Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PlanCard("WEEKLY HABIT", "6 Days Plan", "₹510", "₹85/meal • Free delivery", false, Modifier.weight(1f)); PlanCard("MONTHLY VALUE", "26 Days Plan", "₹1,950", "₹75/meal • Save ₹260", true, Modifier.weight(1f)) } }

@Composable private fun PlanCard(label: String, title: String, price: String, copy: String, featured: Boolean, modifier: Modifier) = Card(modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = if (featured) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(12.dp)) { Text(label, style = MaterialTheme.typography.labelSmall, color = if (featured) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary); Spacer(Modifier.height(8.dp)); Text(title, style = MaterialTheme.typography.titleMedium); Text(if (featured) "Full monthly, worry-free" else "Not so fast meals", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(10.dp)); Text(price, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary); Text(copy, style = MaterialTheme.typography.labelSmall) } }

@Composable private fun TestimonialCard() = Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text("●", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.tertiary); Spacer(Modifier.width(8.dp)); Column { Text("★★★★★", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary); Text("Feels exactly like Maa's food in Whitefield PG!", style = MaterialTheme.typography.bodySmall); Text("— Aditya K., subscribed for 4 months", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

@Composable private fun PaywallScreen(kitchen: Kitchen?, onBack: () -> Unit, onComplete: () -> Unit, purchaseSaveError: Boolean) {
    val timestamp = remember { System.currentTimeMillis() }
    val date = remember(timestamp) { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp + 24 * 60 * 60 * 1000L)) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("‹", Modifier.clickable(onClick = onBack), style = MaterialTheme.typography.headlineSmall) }
        item { Text("Taste the difference. Just ₹1", style = MaterialTheme.typography.displaySmall) }
        item { Text(if (kitchen == null) "A weekly subscription to delicious local home food." else "Start your ${kitchen.name} subscription today.", style = MaterialTheme.typography.bodyLarge) }
        item { BillingTimeline(date) }
        if (purchaseSaveError) item { Text("We couldn't save your subscription. Please try again.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.padding(16.dp)) { Text("Simulated purchase", style = MaterialTheme.typography.titleMedium); Text("No payment SDK or real charge is used. This prototype only records your confirmation on this device.", style = MaterialTheme.typography.bodySmall) } } }
        item { Button(onClick = onComplete, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(50)) { Text("Start trial for ₹1") } }
        item { Text("By continuing, you agree to the disclosed billing schedule. You may cancel any time in this prototype.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable private fun BillingTimeline(futureDate: String) = Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Transparent billing timeline", style = MaterialTheme.typography.titleMedium); Row { Text("1", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Spacer(Modifier.width(12.dp)); Column { Text("Today (instant)", style = MaterialTheme.typography.titleMedium); Text("₹1 is charged now for your trial.", style = MaterialTheme.typography.bodyMedium) } }; Divider(); Row { Text("2", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Spacer(Modifier.width(12.dp)); Column { Text("In 24 hours", style = MaterialTheme.typography.titleMedium); Text("₹249 will be charged on $futureDate.", style = MaterialTheme.typography.bodyMedium) } } } }

@Composable private fun CenterMessage(content: @Composable () -> Unit) = androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { content() }
