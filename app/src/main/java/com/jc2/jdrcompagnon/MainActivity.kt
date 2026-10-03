package com.jc2.jdrcompagnon

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.MusicManager
import com.jc2.jdrcompagnon.ui.ScenarioClockState
import com.jc2.jdrcompagnon.ui.WeatherSoundManager
import com.jc2.jdrcompagnon.ui.navigation.JdrNavGraph
import com.jc2.jdrcompagnon.ui.WorldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.jc2.jdrcompagnon.ui.theme.JdrCompagnonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Empêche le téléphone de se mettre en veille pendant l'utilisation de l'app
        // (session de jeu potentiellement longue, écran hébergeant le serveur réseau...).
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Initialize GameState with SharedPreferences
        GameState.init(this)
        GameState.loadMusicSettings()
        MusicManager.setVolume(GameState.musicSettings.value.volume)

        setContent {
            JdrCompagnonTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Fond forcé, quel que soit le thème sélectionné
                    // (clair/sombre/monde) : le thème ne doit jamais influencer
                    // ce fond. Remplace l'ancien fond uni ForcedDarkPalette.Background
                    // par une image, appliquée une seule fois ici pour toute l'app.
                    Image(
                        painter = painterResource(id = R.drawable.fond_ecran),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    AppEntryPoint()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ScenarioClockState.refreshAmbientSound()
    }

    override fun onStop() {
        super.onStop()
        MusicManager.pause()
        WeatherSoundManager.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        MusicManager.release()
        WeatherSoundManager.stop()
    }
}

@Composable
fun AppEntryPoint() {
    val currentWorld by GameState.currentWorld.collectAsState()
    // Plus de choix d'univers imposé au premier lancement : Donjons & Dragons par défaut (aussi
    // quand l'univers courant a été supprimé), on arrive directement sur le choix du pseudo /
    // du rôle. L'univers reste modifiable depuis le bouton "Univers".
    val defaultWorldName = stringResource(R.string.world_dnd_label)
    val defaultWorldDescription = stringResource(R.string.world_dnd_description)
    LaunchedEffect(currentWorld) {
        if (currentWorld == null) {
            GameState.selectWorld(
                WorldState(id = "donjon_et_dragon", name = defaultWorldName, description = defaultWorldDescription)
            )
        }
    }
    JdrNavGraph()
}