package com.example.propelassignment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.propelassignment.analytics.LogcatAnalytics
import com.example.propelassignment.data.KitchenRepository
import com.example.propelassignment.persistence.AppPreferences
import com.example.propelassignment.ui.TiffinApp
import com.example.propelassignment.ui.TiffinViewModel
import com.example.propelassignment.ui.theme.PropelAssignmentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences = AppPreferences(this)
        val launchCount = preferences.registerGenuineLaunch()
        val viewModel = ViewModelProvider(
            this,
            TiffinViewModel.Factory(KitchenRepository(this), preferences, LogcatAnalytics(), launchCount)
        )[TiffinViewModel::class.java]
        setContent {
            PropelAssignmentTheme {
                TiffinApp(viewModel)
            }
        }
    }
}
