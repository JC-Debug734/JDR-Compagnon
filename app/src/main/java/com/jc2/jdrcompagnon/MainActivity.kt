package com.jc2.jdrcompagnon

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
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

/** Bleu nuit de la barre du bas (IconesAsset.FondIcones), en ARGB pour la barre de navigation système. */
private const val BLEU_BARRE_DU_BAS = 0xFF111D30.toInt()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barres système intégrées à l'appli, quel que soit le thème du téléphone : sans style
        // imposé, Android choisit selon le mode clair/sombre du système (barre de navigation
        // blanche sur un Samsung en mode clair). Icônes claires, barre d'état transparente,
        // barre de navigation du bleu nuit de la barre du bas (transparente en navigation par gestes).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(BLEU_BARRE_DU_BAS),
        )
        // Pas de voile de contraste ajouté par le système par-dessus la barre de navigation.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        // Empêche le téléphone de se mettre en veille pendant l'utilisation de l'app
        // (session de jeu potentiellement longue, écran hébergeant le serveur réseau...).
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Initialize GameState with SharedPreferences
        GameState.init(this)
        // Carnet du MJ (secrets, fils en suspens, préparation de séance), par monde.
        com.jc2.jdrcompagnon.feature_preparation.data.CarnetMj.init(this)
        com.jc2.jdrcompagnon.feature_faction.data.FactionsStore.init(this)
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