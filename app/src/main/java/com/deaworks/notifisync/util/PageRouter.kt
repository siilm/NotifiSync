package com.deaworks.notifisync.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.deaworks.notifisync.ui.pages.AccessControlPage
import com.deaworks.notifisync.ui.pages.HomePage
import com.deaworks.notifisync.ui.pages.LogsPage
import com.deaworks.notifisync.ui.pages.SettingsPage
import com.deaworks.notifisync.ui.pages.SolutionPage

@Composable
fun PageRouter(
    navController: NavHostController, modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController, startDestination = "home", modifier = modifier
    ) {
        composable("home") { HomePage() }
        composable("settings") { SettingsPage(navController) }
        composable("logs") {
            LogsPage()
        }

        composable("access_control") {
            AccessControlPage(
                onBack = { navController.popBackStack() },
            )
        }
        composable("solution_page") {
            SolutionPage(
                onBack = { navController.popBackStack() },
            )
        }
    }
}