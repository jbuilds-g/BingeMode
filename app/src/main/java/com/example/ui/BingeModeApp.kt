package com.example.ui

import android.widget.Toast
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AddShowScreen
import com.example.ui.screens.BingeDashboardScreen
import com.example.ui.screens.EditShowScreen
import com.example.ui.screens.SectionGridScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShowChecklistScreen
import com.example.ui.screens.ThemeAndAppearanceScreen
import com.example.ui.theme.BingeModeTheme
import com.example.ui.viewmodel.BingeViewModel

@Composable
fun BingeModeApp(
    viewModel: BingeViewModel,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val context = LocalContext.current
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val displayMode by viewModel.displayMode.collectAsStateWithLifecycle()
    val elementBorders by viewModel.elementBorders.collectAsStateWithLifecycle()

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    val isSystemDark = isSystemInDarkTheme()
    val darkTheme = when (displayMode) {
        "dark", "amoled" -> true
        "light" -> false
        else -> isSystemDark
    }

    BingeModeTheme(
        darkTheme = darkTheme,
        themeMode = themeMode,
        isAmoled = displayMode == "amoled",
        elementBorders = elementBorders
    ) {
        val navController = rememberNavController()
        val currentIntent = (context as? android.app.Activity)?.intent
        val targetRoute = currentIntent?.getStringExtra("target_route")

        LaunchedEffect(targetRoute) {
            if (!targetRoute.isNullOrEmpty()) {
                navController.navigate(targetRoute) {
                    popUpTo("dashboard") { inclusive = false }
                }
                currentIntent.removeExtra("target_route")
            }
        }

        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = modifier,
            enterTransition = {
                slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn()
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -1000 }) + fadeOut()
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -1000 }) + fadeIn()
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { 1000 }) + fadeOut()
            }
        ) {
            composable("dashboard") {
                BingeDashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAddShow = { navController.navigate("add_show") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToChecklist = { id -> navController.navigate("checklist/$id") },
                    onNavigateToSectionGrid = { sectionId -> navController.navigate("section_grid/$sectionId") },
                    onNavigateToThemeAppearance = { navController.navigate("theme_appearance") }
                )
            }
            composable("add_show") {
                AddShowScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToThemeAppearance = { navController.navigate("theme_appearance") }
                )
            }
            composable("theme_appearance") {
                ThemeAndAppearanceScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("checklist/{showId}") { backStackEntry ->
                val showId = backStackEntry.arguments?.getString("showId")?.toIntOrNull() ?: 0
                ShowChecklistScreen(
                    showId = showId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEditShow = { id -> navController.navigate("edit_show/$id") }
                )
            }
            composable("edit_show/{showId}") { backStackEntry ->
                val showId = backStackEntry.arguments?.getString("showId")?.toIntOrNull() ?: 0
                EditShowScreen(
                    showId = showId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("section_grid/{sectionId}") { backStackEntry ->
                val sectionId = backStackEntry.arguments?.getString("sectionId") ?: ""
                SectionGridScreen(
                    sectionId = sectionId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
