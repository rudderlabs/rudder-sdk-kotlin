package com.rudderstack.sampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
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
            analytics.setNavigationDestinationsTracking(rememberNavController(), this@MainActivity)
            RudderAndroidLibsTheme {
                AppNavHost(viewModel = viewModel)
            }
        }
    }
}