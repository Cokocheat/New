package com.example.game.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.game.viewmodel.AppScreen
import com.example.game.viewmodel.GameViewModel

@Composable
fun WormsZoneApp(viewModel: GameViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A0E17)
    ) {
        Crossfade(targetState = currentScreen, label = "screenTransition") { screen ->
            when (screen) {
                AppScreen.MAIN_MENU -> MainMenuScreen(viewModel = viewModel)
                AppScreen.PLAYING -> GamePlayScreen(viewModel = viewModel)
                AppScreen.SHOP -> ShopScreen(viewModel = viewModel)
                AppScreen.LEADERBOARD -> LeaderboardScreen(viewModel = viewModel)
                AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
