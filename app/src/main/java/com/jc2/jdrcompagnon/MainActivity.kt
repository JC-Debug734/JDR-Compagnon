package com.jc2.jdrcompagnon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.MusicManager
import com.jc2.jdrcompagnon.ui.navigation.JdrNavGraph
import com.jc2.jdrcompagnon.ui.navigation.Route
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import com.jc2.jdrcompagnon.ui.theme.JdrCompagnonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize GameState with SharedPreferences
        GameState.init(this)
        GameState.loadMusicSettings()
        MusicManager.setVolume(GameState.musicSettings.value.volume)

        setContent {
            JdrCompagnonTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    // Fond forcé, quel que soit le thème sélectionné
                    // (clair/sombre/monde) : le thème ne doit jamais influencer
                    // cette couleur.
                    color = ForcedDarkPalette.Background,
                ) {
                    AppEntryPoint()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        MusicManager.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        MusicManager.release()
    }
}

@Composable
fun AppEntryPoint() {
    val currentWorld by GameState.currentWorld.collectAsState()
    val isFirstLaunch = remember { (!GameState.isWorldSelected()) && (currentWorld == null) }

    if (isFirstLaunch) {
        // Premier lancement : forcer la sélection du monde
        JdrNavGraph(overrideStartDestination = Route.FirstLaunchWorldSelection.path)
    } else {
        JdrNavGraph()
    }
}