package com.rudderstack.sampleapp.navigation

sealed class Routes(val route: String) {
    object MainScreen : Routes(route = "mainScreen")
    object ScreenOne : Routes(route = "screenOne")
    object ScreenTwo : Routes(route = "screenTwo")
}
