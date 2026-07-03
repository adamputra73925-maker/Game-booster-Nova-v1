package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.AppListScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BoosterViewModel

/**
 * Navigation screen routes.
 */
enum class Screen {
    DASHBOARD,
    APP_LIST,
    STATS
}

class MainActivity : ComponentActivity() {

    private val viewModel: BoosterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge support
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    BoxModifier(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            Screen.DASHBOARD -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToAppList = { currentScreen = Screen.APP_LIST },
                                onNavigateToStats = { currentScreen = Screen.STATS }
                            )
                            Screen.APP_LIST -> AppListScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = Screen.DASHBOARD }
                            )
                            Screen.STATS -> StatsScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = Screen.DASHBOARD }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh values when returning to the app
        viewModel.checkPermissions()
        viewModel.checkServiceStatus()
        viewModel.refreshRamUsage()
    }
}

@Composable
fun BoxModifier(modifier: Modifier, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        content()
    }
}
