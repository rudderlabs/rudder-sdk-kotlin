package com.rudderstack.sampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.rudderstack.sampleapp.analytics.RudderAnalyticsUtils.analytics
import com.rudderstack.sampleapp.mainscreen.MainViewModel
import com.rudderstack.sampleapp.navigation.AppNavHost
import com.rudderstack.sampleapp.ui.theme.RudderAndroidLibsTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()

            // Analytics tracking is initialized to track navigation events within the app.
            // Using a LaunchedEffect blocks helps us to avoid getting called multiple times due to recomposition.
            LaunchedEffect(Unit) {
                analytics.setNavigationDestinationsTracking(navController, this@MainActivity)
            }
            RudderAndroidLibsTheme {
                AppNavHost(viewModel = viewModel, navController = navController)
            }
        }
    }
}
