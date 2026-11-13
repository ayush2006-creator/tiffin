package com.example.propelassignment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.propelassignment.analytics.LogcatAnalytics
import com.example.propelassignment.data.KitchenRepository
import com.example.propelassignment.persistence.AppPreferences
import com.example.propelassignment.ui.TiffinApp
import com.example.propelassignment.ui.theme.PropelAssignmentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferences = AppPreferences(this)
        val launchCount = preferences.registerGenuineLaunch(savedInstanceState != null)
        setContent {
            PropelAssignmentTheme {
                TiffinApp(KitchenRepository(this), preferences, LogcatAnalytics(), launchCount)
            }
        }
    }
}
