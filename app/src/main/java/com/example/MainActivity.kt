package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.BingeRepository
import com.example.ui.screens.AddShowScreen
import com.example.ui.screens.BingeDashboardScreen
import com.example.ui.screens.EditShowScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ThemeAndAppearanceScreen
import com.example.ui.screens.ShowChecklistScreen
import com.example.ui.screens.SectionGridScreen
import com.example.ui.theme.BingeModeTheme
import com.example.ui.viewmodel.BingeViewModel
import com.example.ui.viewmodel.BingeViewModelFactory

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: BingeViewModel
    private var cancelReceiver: android.content.BroadcastReceiver? = null

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the intent so it can be read in setContent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        
        // Instantiate the local database repository
        val repository = BingeRepository(applicationContext)
        val viewModelFactory = BingeViewModelFactory(repository)
        viewModel = androidx.lifecycle.ViewModelProvider(this, viewModelFactory)[BingeViewModel::class.java]
        
        cancelReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
                if (intent.action == "com.example.ACTION_CANCEL_SIMULATION") {
                    viewModel.cancelSimulation()
                }
            }
        }
        val filter = android.content.IntentFilter("com.example.ACTION_CANCEL_SIMULATION")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(cancelReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(cancelReceiver, filter)
        }
        
        setContent {
            // Toast notification collector for settings, imports, saves
            val toastMessage by viewModel.toastMessage.collectAsState()
            val context = LocalContext.current
            
            LaunchedEffect(toastMessage) {
                toastMessage?.let {
                    android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                    viewModel.clearToast()
                }
            }

            val themeMode by viewModel.themeMode.collectAsState()
            val displayMode by viewModel.displayMode.collectAsState()
            val elementBorders by viewModel.elementBorders.collectAsState()

            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = when (displayMode) {
                "dark", "amoled" -> true
                "light" -> false
                else -> isSystemDark
            }
            val isAmoled = displayMode == "amoled"

            BingeModeTheme(
                darkTheme = darkTheme,
                themeMode = themeMode,
                isAmoled = isAmoled,
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
                    enterTransition = { androidx.compose.animation.slideInHorizontally(initialOffsetX = { 1000 }) + androidx.compose.animation.fadeIn() },
                    exitTransition = { androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -1000 }) + androidx.compose.animation.fadeOut() },
                    popEnterTransition = { androidx.compose.animation.slideInHorizontally(initialOffsetX = { -1000 }) + androidx.compose.animation.fadeIn() },
                    popExitTransition = { androidx.compose.animation.slideOutHorizontally(targetOffsetX = { 1000 }) + androidx.compose.animation.fadeOut() }
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
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cancelReceiver?.let { unregisterReceiver(it) }
    }
}