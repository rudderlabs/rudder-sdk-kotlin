package com.rudderstack.sampleapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rudderstack.sampleapp.mainscreen.MainScreen
import com.rudderstack.sampleapp.mainscreen.MainViewModel
import com.rudderstack.sampleapp.screenone.ScreenOne
import com.rudderstack.sampleapp.screentwo.ScreenTwo

@Composable
fun AppNavigationGraph(
    viewModel: MainViewModel,
    navController: NavHostController,
    startDestination: String = Routes.MainScreen.route,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Routes.MainScreen.route) {
            MainScreen(
                viewModel = viewModel,
                onNavigateToScreenOne = { navController.navigate(Routes.ScreenOne.route) }
            )
        }
        composable(route = Routes.ScreenOne.route) {
            ScreenOne(
                viewModel = viewModel,
                onNavigateToScreenTwo = { navController.navigate(Routes.ScreenTwo.route) }
            )
        }
        composable(route = Routes.ScreenTwo.route) {
            ScreenTwo(
                viewModel = viewModel,
                onNavigateToScreenOne = { navController.navigate(Routes.ScreenOne.route) }
            )
        }
    }
}