package com.drinkwater

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.drinkwater.data.db.SettingsDataStore
import com.drinkwater.ui.components.PermissionGuideScreen
import com.drinkwater.ui.home.HomeScreen
import com.drinkwater.ui.monitor.MonitorScreen
import com.drinkwater.ui.settings.SettingsScreen
import com.drinkwater.ui.theme.DrinkWaterTheme
import com.drinkwater.ui.vocabulary.VocabularyScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DrinkWaterTheme {
                val settings = remember { SettingsDataStore(this@MainActivity) }
                val isFirstLaunch by settings.isFirstLaunch.collectAsState(initial = true)
                var guideCompleted by remember { mutableStateOf(false) }

                if (isFirstLaunch && !guideCompleted) {
                    PermissionGuideScreen(onComplete = { guideCompleted = true })
                } else {
                    MainScreen()
                }
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: @Composable () -> Unit) {
    data object Home : Screen("home", "首页", { Icon(Icons.Default.Home, contentDescription = "首页") })
    data object Monitor : Screen("monitor", "监控", { Icon(Icons.Default.Visibility, contentDescription = "监控") })
    data object Vocabulary : Screen("vocabulary", "生词本", { Icon(Icons.Default.MenuBook, contentDescription = "生词本") })
    data object Settings : Screen("settings", "设置", { Icon(Icons.Default.Settings, contentDescription = "设置") })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val screens = listOf(Screen.Home, Screen.Monitor, Screen.Vocabulary, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { screen.icon() },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Monitor.route) { MonitorScreen() }
            composable(Screen.Vocabulary.route) { VocabularyScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
