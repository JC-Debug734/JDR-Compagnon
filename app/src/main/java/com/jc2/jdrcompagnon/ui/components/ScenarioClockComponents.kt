package com.jc2.jdrcompagnon.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Cyclone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SevereCold
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.network.PlayerConnectionState
import com.jc2.jdrcompagnon.network.SessionRole
import com.jc2.jdrcompagnon.ui.CalendarConfig
import com.jc2.jdrcompagnon.ui.ClimatMois
import com.jc2.jdrcompagnon.ui.DayPeriod
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.JourFerie
import com.jc2.jdrcompagnon.ui.ScenarioClockState
import com.jc2.jdrcompagnon.ui.ScenarioTemperature
import com.jc2.jdrcompagnon.ui.ScenarioWeather
import com.jc2.jdrcompagnon.ui.WeatherSoundManager
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import kotlin.math.roundToInt

/** Icône (soleil / lune / crépuscule) cohérente avec l'heure de scénario en cours. */
fun dayPeriodIcon(period: DayPeriod): ImageVector = when (period) {
    DayPeriod.JOUR -> Icons.Default.WbSunny
    DayPeriod.NUIT -> Icons.Default.NightsStay
    DayPeriod.AUBE, DayPeriod.CREPUSCULE -> Icons.Default.WbTwilight
}

fun weatherIcon(weather: ScenarioWeather): ImageVector = when (weather) {
    ScenarioWeather.CLAIR -> Icons.Default.WbSunny
    ScenarioWeather.NUAGEUX -> Icons.Default.Cloud
    ScenarioWeather.PLUIE -> Icons.Default.Grain
    ScenarioWeather.ORAGE -> Icons.Default.Bolt
    ScenarioWeather.NEIGE -> Icons.Default.AcUnit
    ScenarioWeather.BROUILLARD -> Icons.Default.Air
    ScenarioWeather.FORTE_NEIGE -> Icons.Default.SevereCold
    ScenarioWeather.VENT_FORT -> Icons.Default.Cyclone
    ScenarioWeather.SEC -> Icons.Default.Whatshot
}

fun weatherLabel(weather: ScenarioWeather): String = when (weather) {
    ScenarioWeather.CLAIR -> "Ciel dégagé"
    ScenarioWeather.NUAGEUX -> "Nuageux"
    ScenarioWeather.PLUIE -> "Pluie"
    ScenarioWeather.ORAGE -> "Orage"
    ScenarioWeather.NEIGE -> "Neige"
    ScenarioWeather.BROUILLARD -> "Brouillard"
    ScenarioWeather.FORTE_NEIGE -> "Forte neige"
    ScenarioWeather.VENT_FORT -> "Vent fort"
    ScenarioWeather.SEC -> "Temps sec"
}

/** Couleur de la température : bleu glacé → rouge brûlant. */
fun temperatureColor(temperature: ScenarioTemperature): Color = when (temperature) {
    ScenarioTemperature.FROID_INTENSE -> Color(0xFF81D4FA)
    ScenarioTemperature.FROID -> Color(0xFFB3E5FC)
    ScenarioTemperature.FRAIS -> Color(0xFFE0F2F1)
    ScenarioTemperature.DOUX -> Color.White
    ScenarioTemperature.CHAUD -> Color(0xFFFFCC80)
    ScenarioTemperature.FORTE_CHALEUR -> Color(0xFFFF8A65)
}

/**
 * Ligne compacte affichée dans les menus latéraux MJ et Joueur (Mjdrawer.kt / Joueurdrawer.kt) :
 * icône jour/nuit cohérente avec l'heure de scénario, heure, météo et température en direct,
 * saison et jour férié du calendrier. N'affiche rien tant que la fonction n'est pas activée par
 * le MJ (ScenarioClockState.state.enabled). [onClick] (côté MJ) ouvre [ReglageHorlogeDialog].
 */
@Composable
fun ScenarioClockRow(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val clock by ScenarioClockState.state.collectAsState()
    val calendars by ScenarioClockState.calendars.collectAsState()
    val activeCalendarId by ScenarioClockState.activeCalendarId.collectAsState()
    val remoteCalendar by ScenarioClockState.remoteCalendar.collectAsState()
    val role by NetworkSessionManager.role.collectAsState()
    val playerState by NetworkSessionManager.playerState.collectAsState()
    if (!clock.enabled) return

    val activeCalendar = remember(calendars, activeCalendarId, remoteCalendar) { ScenarioClockState.activeCalendar() }
    val hour = ScenarioClockState.hourOfDay(clock.scenarioMinutes)
    val period = ScenarioClockState.dayPeriod(hour)
    val saison = ScenarioClockState.climatCourant(clock.scenarioMinutes, activeCalendar)?.saison?.takeIf { it.isNotBlank() }
    val ferie = ScenarioClockState.jourFerie(clock.scenarioMinutes, activeCalendar)

    // Date complète du calendrier actif, puis l'heure avec secondes (police à chasse fixe : sa
    // largeur ne bouge pas à chaque seconde), météo en dessous.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(4.dp)
                else Modifier
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = dayPeriodIcon(period),
                contentDescription = null,
                tint = ForcedDarkPalette.AccentGold,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ScenarioClockState.formattedCalendarDay(clock.scenarioMinutes, activeCalendar) +
                        (saison?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ForcedDarkPalette.Content,
                )
                Text(
                    text = ScenarioClockState.formattedTime(clock.scenarioMinutes, clock.scenarioSeconds),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            if (onClick != null) {
                Icon(Icons.Default.Tune, contentDescription = "Régler l'heure, la date et la météo", tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(18.dp))
            }
        }
        ferie?.let {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Celebration, contentDescription = null, tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(18.dp))
                Text(text = it.nom, style = MaterialTheme.typography.bodySmall, color = ForcedDarkPalette.AccentGold, fontWeight = FontWeight.Bold)
            }
        }
        // Joueur connecté au MJ : état de la synchronisation de l'heure et du calendrier.
        if (role == SessionRole.PLAYER) {
            val (syncLabel, syncColor) = when (playerState) {
                PlayerConnectionState.CONNECTED ->
                    (if (remoteCalendar != null) "Synchronisé avec le MJ · ${remoteCalendar?.nom}" else "Synchronisé avec le MJ") to Color(0xFF7BC67B)
                PlayerConnectionState.CONNECTING, PlayerConnectionState.RECONNECTING ->
                    "Synchronisation en cours…" to ForcedDarkPalette.AccentGold
                PlayerConnectionState.DISCONNECTED -> "Non synchronisé (hors ligne)" to Color(0xFFE57373)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Sync, contentDescription = null, tint = syncColor, modifier = Modifier.size(16.dp))
                Text(text = syncLabel, style = MaterialTheme.typography.labelSmall, color = syncColor)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = weatherIcon(clock.weather),
                contentDescription = null,
                tint = ForcedDarkPalette.Content,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = weatherLabel(clock.weather),
                style = MaterialTheme.typography.bodySmall,
                color = ForcedDarkPalette.Content,
            )
            Icon(
                imageVector = Icons.Default.Thermostat,
                contentDescription = null,
                tint = temperatureColor(clock.temperature),
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = clock.temperature.label,
                style = MaterialTheme.typography.bodySmall,
                color = temperatureColor(clock.temperature),
            )
        }
    }
}

/**
 * Heure de scénario compacte affichée sous le titre du scénario en cours de lecture (côté MJ).
 * Un clic met en pause / relance le défilement automatique de l'heure
 * ([ScenarioClockState.setAutoAdvanceEnabled]), rediffusé aux joueurs comme depuis l'écran
 * Horloge & météo. N'affiche rien tant que l'horloge n'est pas activée.
 */
@Composable
fun ScenarioClockTicker(modifier: Modifier = Modifier) {
    val clock by ScenarioClockState.state.collectAsState()
    if (!clock.enabled) return

    val period = ScenarioClockState.dayPeriod(ScenarioClockState.hourOfDay(clock.scenarioMinutes))
    val paused = !clock.autoAdvanceEnabled
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { ScenarioClockState.setAutoAdvanceEnabled(paused) }
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = dayPeriodIcon(period),
            contentDescription = null,
            tint = ForcedDarkPalette.AccentGold,
            modifier = Modifier.size(14.dp),
        )
        // Police à chasse fixe : la largeur ne change pas à chaque tic.
        Text(
            text = ScenarioClockState.formattedTime(clock.scenarioMinutes, clock.scenarioSeconds),
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            color = if (paused) Color.White.copy(alpha = 0.6f) else Color.White,
        )
        Icon(
            imageVector = if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
            contentDescription = if (paused) "Relancer l'heure" else "Mettre l'heure en pause",
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
    }
}

/**
 * Réglage rapide de l'horloge, ouvert d'un appui sur l'heure dans le menu latéral MJ : heure,
 * date et (si la météo automatique est coupée) météo et température. Ces réglages manuels ne sont
 * plus sur l'écran Horloge & météo, qui ne garde que l'activation des fonctions et le calendrier.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReglageHorlogeDialog(onDismiss: () -> Unit) {
    val clock by ScenarioClockState.state.collectAsState()
    val calendars by ScenarioClockState.calendars.collectAsState()
    val activeCalendarId by ScenarioClockState.activeCalendarId.collectAsState()
    val activeCalendar = remember(calendars, activeCalendarId) { ScenarioClockState.activeCalendar() }
    var heure by remember { mutableStateOf("%02d".format(ScenarioClockState.hourOfDay(clock.scenarioMinutes))) }
    var minute by remember { mutableStateOf("%02d".format(ScenarioClockState.minuteOfHour(clock.scenarioMinutes))) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Régler l'horloge", fontWeight = FontWeight.Bold) },
        text = {
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                Column(
                    modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        ScenarioClockState.formattedCalendarDate(clock.scenarioMinutes, activeCalendar, clock.scenarioSeconds),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    ToggleRow(
                        label = "Défilement automatique de l'heure",
                        checked = clock.autoAdvanceEnabled,
                        onCheckedChange = { ScenarioClockState.setAutoAdvanceEnabled(it) },
                    )

                    Text("Heure", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { ScenarioClockState.advanceManually(-60) }) { Text("-1 h") }
                        OutlinedButton(onClick = { ScenarioClockState.advanceManually(-10) }) { Text("-10 min") }
                        OutlinedButton(onClick = { ScenarioClockState.advanceManually(10) }) { Text("+10 min") }
                        OutlinedButton(onClick = { ScenarioClockState.advanceManually(60) }) { Text("+1 h") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = heure,
                            onValueChange = { heure = it.filter(Char::isDigit).take(2) },
                            label = { Text("h") },
                            modifier = Modifier.width(70.dp),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = minute,
                            onValueChange = { minute = it.filter(Char::isDigit).take(2) },
                            label = { Text("min") },
                            modifier = Modifier.width(70.dp),
                            singleLine = true,
                        )
                        OutlinedButton(onClick = {
                            ScenarioClockState.setTimeOfDay(heure.toIntOrNull() ?: 0, minute.toIntOrNull() ?: 0)
                        }) { Text("Régler") }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Jour", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { ScenarioClockState.advanceDays(-1) }) { Text("-1 jour") }
                        OutlinedButton(onClick = { ScenarioClockState.advanceDays(1) }) { Text("+1 jour") }
                    }
                    DatePickerRow(
                        config = activeCalendar,
                        initialMinutes = clock.scenarioMinutes,
                        onSetDate = { year, month, day -> ScenarioClockState.setCurrentDate(year, month, day) },
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Météo", style = MaterialTheme.typography.labelLarge)
                    if (clock.autoWeatherEnabled) {
                        Text(
                            "Météo automatique : ${weatherLabel(clock.weather)}, ${clock.temperature.label.lowercase()}. " +
                                "Coupez-la sur l'écran Horloge & météo pour la choisir ici.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    } else {
                        ChoixMeteo(
                            meteos = setOf(clock.weather),
                            onToggle = { ScenarioClockState.setWeather(it) },
                        )
                        Text("Température", style = MaterialTheme.typography.labelMedium)
                        ChoixTemperatures(
                            temperatures = setOf(clock.temperature),
                            onToggle = { ScenarioClockState.setTemperature(it) },
                        )
                    }
                    clock.temperature.regle?.let {
                        Text("⚠ $it", style = MaterialTheme.typography.bodySmall, color = temperatureColor(clock.temperature))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
    )
}

/** Puces de météo (sélection simple ou multiple selon l'appelant). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoixMeteo(meteos: Set<ScenarioWeather>, onToggle: (ScenarioWeather) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ScenarioWeather.entries.forEach { weather ->
            FilterChip(
                selected = weather in meteos,
                onClick = { onToggle(weather) },
                label = { Text(weatherLabel(weather)) },
                leadingIcon = { Icon(weatherIcon(weather), contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
        }
    }
}

/** Puces de température (sélection simple ou multiple selon l'appelant). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoixTemperatures(temperatures: Set<ScenarioTemperature>, onToggle: (ScenarioTemperature) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ScenarioTemperature.entries.forEach { temperature ->
            FilterChip(
                selected = temperature in temperatures,
                onClick = { onToggle(temperature) },
                label = { Text(temperature.label, color = temperatureColor(temperature)) },
            )
        }
    }
}

/**
 * Écran de gestion de l'horloge de scénario, ouvert depuis le tableau de bord MJ
 * (MjHomeScreen) : activation de la fonction, défilement automatique, météo automatique et
 * ambiance sonore dans une première carte, calendrier (mois, semaine, jours fériés, climat par
 * mois) dans une carte réductible séparée. Le réglage manuel de l'heure, de la date et de la
 * météo se fait d'un appui sur l'horloge du menu latéral MJ ([ReglageHorlogeDialog]). Chaque
 * changement est rediffusé en direct aux joueurs connectés (voir
 * NetworkSessionManager.broadcastTimeState).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioClockScreen(
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {
    val clock by ScenarioClockState.state.collectAsState()
    val calendars by ScenarioClockState.calendars.collectAsState()
    val activeCalendarId by ScenarioClockState.activeCalendarId.collectAsState()
    val activeCalendar = remember(calendars, activeCalendarId) { ScenarioClockState.activeCalendar() }
    val hour = ScenarioClockState.hourOfDay(clock.scenarioMinutes)
    val period = ScenarioClockState.dayPeriod(hour)

    val weatherSoundSettings by GameState.weatherSoundSettings.collectAsState()
    var weatherSoundVolume by remember { mutableStateOf(weatherSoundSettings.volume) }
    LaunchedEffect(weatherSoundSettings.volume) { weatherSoundVolume = weatherSoundSettings.volume }
    // Debounce persistance du volume (300ms), même principe que MusicScreen.
    LaunchedEffect(weatherSoundVolume) {
        kotlinx.coroutines.delay(300)
        if (weatherSoundVolume != weatherSoundSettings.volume) {
            GameState.saveWeatherSoundSettings(weatherSoundSettings.copy(volume = weatherSoundVolume))
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Horloge & météo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        CompositionLocalProvider(LocalContentColor provides Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Carte "Horloge" : fond translucide commun à l'app, texte blanc.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                contentColor = Color.White,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(dayPeriodIcon(period), contentDescription = null, modifier = Modifier.size(28.dp))
                        Icon(weatherIcon(clock.weather), contentDescription = null, modifier = Modifier.size(24.dp))
                        Text(weatherLabel(clock.weather), style = MaterialTheme.typography.bodyMedium)
                        Icon(Icons.Default.Thermostat, contentDescription = null, tint = temperatureColor(clock.temperature), modifier = Modifier.size(22.dp))
                        Text(clock.temperature.label, style = MaterialTheme.typography.bodyMedium, color = temperatureColor(clock.temperature))
                    }
                    Text(
                        text = ScenarioClockState.formattedCalendarDate(clock.scenarioMinutes, activeCalendar, clock.scenarioSeconds),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    ScenarioClockState.climatCourant(clock.scenarioMinutes, activeCalendar)?.saison?.takeIf { it.isNotBlank() }?.let {
                        Text("Saison : $it", style = MaterialTheme.typography.bodySmall)
                    }
                    ScenarioClockState.jourFerie(clock.scenarioMinutes, activeCalendar)?.let {
                        Text("🎉 ${it.nom}", style = MaterialTheme.typography.bodyMedium, color = ForcedDarkPalette.AccentGold, fontWeight = FontWeight.Bold)
                    }
                    clock.temperature.regle?.let {
                        Text("⚠ $it", style = MaterialTheme.typography.bodySmall, color = temperatureColor(clock.temperature))
                    }
                    Text(
                        "Heure, date et météo manuelle : touchez l'horloge dans le menu latéral.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    )

                    ToggleRow(
                        label = "Activer la fonction",
                        checked = clock.enabled,
                        onCheckedChange = { ScenarioClockState.setEnabled(it) },
                    )
                    ToggleRow(
                        label = "Défilement automatique de l'heure",
                        checked = clock.autoAdvanceEnabled,
                        enabled = clock.enabled,
                        onCheckedChange = { ScenarioClockState.setAutoAdvanceEnabled(it) },
                    )
                    ToggleRow(
                        label = "Météo automatique (selon le climat du mois)",
                        checked = clock.autoWeatherEnabled,
                        enabled = clock.enabled,
                        onCheckedChange = { ScenarioClockState.setAutoWeatherEnabled(it) },
                    )
                    ToggleRow(
                        label = "Effets sonores météo",
                        checked = weatherSoundSettings.enabled,
                        enabled = clock.enabled,
                        onCheckedChange = {
                            GameState.saveWeatherSoundSettings(weatherSoundSettings.copy(enabled = it))
                            ScenarioClockState.refreshAmbientSound()
                        },
                    )
                    if (weatherSoundSettings.enabled) {
                        ToggleRow(
                            label = "Scène en intérieur (pluie étouffée)",
                            checked = weatherSoundSettings.indoor,
                            enabled = clock.enabled,
                            onCheckedChange = {
                                GameState.saveWeatherSoundSettings(weatherSoundSettings.copy(indoor = it))
                                ScenarioClockState.refreshAmbientSound()
                            },
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Volume",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(60.dp),
                            )
                            Slider(
                                value = weatherSoundVolume,
                                onValueChange = {
                                    weatherSoundVolume = it
                                    WeatherSoundManager.setVolume(it)
                                },
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${(weatherSoundVolume * 100).roundToInt()} %",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(48.dp),
                            )
                        }
                    }
                }
            }

            // Carte "Calendrier", réductible (petite flèche) comme les autres sections de l'app.
            CollapsibleSectionCard(ownerKey = "horloge", title = "Calendrier") {
                CalendarSelector(calendars = calendars, activeCalendar = activeCalendar)
                Spacer(modifier = Modifier.height(4.dp))
                CalendarConfigEditor(
                    config = activeCalendar,
                    onConfigChanged = { ScenarioClockState.updateCalendar(it) },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
        }
    }
}

/** Choix du calendrier actif, création d'un calendrier vierge ou D&D, suppression. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CalendarSelector(calendars: List<CalendarConfig>, activeCalendar: CalendarConfig) {
    var calendarMenuExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = calendarMenuExpanded,
        onExpandedChange = { calendarMenuExpanded = !calendarMenuExpanded },
    ) {
        OutlinedTextField(
            value = activeCalendar.nom,
            onValueChange = {},
            readOnly = true,
            label = { Text("Calendrier actif") },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(calendarMenuExpanded) },
        )
        ExposedDropdownMenu(expanded = calendarMenuExpanded, onDismissRequest = { calendarMenuExpanded = false }) {
            calendars.forEach { calendar ->
                DropdownMenuItem(
                    text = { Text(calendar.nom) },
                    onClick = {
                        ScenarioClockState.setActiveCalendarId(calendar.id)
                        calendarMenuExpanded = false
                    },
                )
            }
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
            val created = ScenarioClockState.addCalendar("Nouveau calendrier ${calendars.size + 1}")
            ScenarioClockState.setActiveCalendarId(created.id)
        }) { Text("Nouveau calendrier", color = Color.White) }
        if (calendars.none { it.nom == CalendarConfig.calendrierHarptos().nom }) {
            OutlinedButton(onClick = {
                val created = ScenarioClockState.addHarptosCalendar()
                ScenarioClockState.setActiveCalendarId(created.id)
            }) { Text("Calendrier D&D (Harpistes)", color = Color.White) }
        }
        if (calendars.size > 1) {
            OutlinedButton(onClick = { ScenarioClockState.deleteCalendar(activeCalendar.id) }) {
                Text("Supprimer ce calendrier", color = Color.White)
            }
        }
    }
}

/**
 * Sélecteur de date (année/mois/jour) réutilisé par le réglage de l'horloge et l'écran Campagne —
 * un seul calendrier global, partagé entre les deux outils (voir ScenarioClockState).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerRow(
    config: CalendarConfig,
    initialMinutes: Long,
    onSetDate: (year: Int, month: Int, dayOfMonth: Int) -> Unit,
) {
    val (initialYear, initialMonth, initialDay) = remember(initialMinutes, config) {
        ScenarioClockState.currentDateParts(initialMinutes, config)
    }
    var year by rememberSaveable(initialYear) { mutableStateOf(initialYear.toString()) }
    var monthIndex by rememberSaveable(initialMonth) { mutableStateOf(initialMonth - 1) }
    var day by rememberSaveable(initialDay) { mutableStateOf(initialDay.toString()) }
    var monthMenuExpanded by remember { mutableStateOf(false) }

    Text(text = "Choisir une date", style = MaterialTheme.typography.labelLarge)
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = year,
            onValueChange = { year = it.filter(Char::isDigit) },
            label = { Text("Année") },
            modifier = Modifier.width(90.dp),
            singleLine = true,
        )
        ExposedDropdownMenuBox(
            expanded = monthMenuExpanded,
            onExpandedChange = { monthMenuExpanded = !monthMenuExpanded },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value = config.monthNames.getOrElse(monthIndex) { "Mois ${monthIndex + 1}" },
                onValueChange = {},
                readOnly = true,
                label = { Text("Mois") },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(monthMenuExpanded) },
            )
            ExposedDropdownMenu(expanded = monthMenuExpanded, onDismissRequest = { monthMenuExpanded = false }) {
                config.monthNames.forEachIndexed { index, name ->
                    DropdownMenuItem(text = { Text(name) }, onClick = { monthIndex = index; monthMenuExpanded = false })
                }
            }
        }
        OutlinedTextField(
            value = day,
            onValueChange = { day = it.filter(Char::isDigit) },
            label = { Text("Jour") },
            modifier = Modifier.width(80.dp),
            singleLine = true,
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(
        onClick = {
            val y = year.toIntOrNull() ?: initialYear
            val d = day.toIntOrNull() ?: 1
            onSetDate(y, monthIndex + 1, d)
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Définir cette date") }
}

/**
 * Édition complète du calendrier actif : nom, année de départ, mois (nombre, noms, jours, climat
 * et saison de chacun), jours de la semaine (nombre et noms) et jours fériés.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarConfigEditor(
    config: CalendarConfig,
    onConfigChanged: (CalendarConfig) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = config.nom,
            onValueChange = { onConfigChanged(config.copy(nom = it)) },
            label = { Text("Nom du calendrier") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = config.startYear.toString(),
            onValueChange = { text ->
                val newYear = text.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: config.startYear
                onConfigChanged(config.copy(startYear = newYear))
            },
            label = { Text("Année de départ") },
            modifier = Modifier.width(140.dp),
            singleLine = true,
        )

        // --- Mois ---
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        CompteurRow(
            label = "Mois dans l'année",
            valeur = config.monthNames.size,
            onMoins = { onConfigChanged(config.avecMoisRetire(config.monthNames.lastIndex)) },
            onPlus = { onConfigChanged(config.avecMoisAjoute()) },
        )
        Text(
            "Nom et nombre de jours de chaque mois ; la flèche ouvre son climat (saison, météos et températures possibles).",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f),
        )
        TextButton(onClick = {
            val n = config.monthNames.size
            // Répartition en 4 saisons égales, l'année commençant en hiver.
            var maj = config
            for (i in 0 until n) maj = maj.avecClimat(i, ClimatMois.saisons[(i * 4 / n).coerceIn(0, 3)])
            onConfigChanged(maj)
        }) { Text("Répartir les 4 saisons sur l'année", color = Color.White) }
        config.monthNames.forEachIndexed { index, name ->
            MoisEditor(
                index = index,
                config = config,
                name = name,
                onConfigChanged = onConfigChanged,
            )
        }

        // --- Semaine ---
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        CompteurRow(
            label = "Jours dans la semaine",
            valeur = config.dayNames.size,
            onMoins = { if (config.dayNames.size > 1) onConfigChanged(config.copy(dayNames = config.dayNames.dropLast(1))) },
            onPlus = { onConfigChanged(config.copy(dayNames = config.dayNames + "Jour ${config.dayNames.size + 1}")) },
        )
        config.dayNames.forEachIndexed { index, name ->
            OutlinedTextField(
                value = name,
                onValueChange = { newName ->
                    onConfigChanged(config.copy(dayNames = config.dayNames.toMutableList().apply { this[index] = newName }))
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        // --- Jours fériés ---
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Jours fériés et fêtes", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                onConfigChanged(config.copy(joursFeries = config.joursFeries + JourFerie("Nouvelle fête", 0, 1)))
            }) { Icon(Icons.Default.Add, contentDescription = "Ajouter un jour férié") }
        }
        if (config.joursFeries.isEmpty()) {
            Text("Aucun jour férié.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
        }
        config.joursFeries.forEachIndexed { index, ferie ->
            JourFerieEditor(
                ferie = ferie,
                config = config,
                onChange = { maj ->
                    onConfigChanged(config.copy(joursFeries = config.joursFeries.toMutableList().apply { this[index] = maj }))
                },
                onDelete = {
                    onConfigChanged(config.copy(joursFeries = config.joursFeries.filterIndexed { i, _ -> i != index }))
                },
            )
        }
    }
}

@Composable
private fun CompteurRow(label: String, valeur: Int, onMoins: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onMoins, enabled = valeur > 1) { Icon(Icons.Default.Remove, contentDescription = "Retirer") }
        Text("$valeur", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        IconButton(onClick = onPlus) { Icon(Icons.Default.Add, contentDescription = "Ajouter") }
    }
}

/** Ligne d'un mois : nom, nombre de jours, et climat dépliable. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoisEditor(
    index: Int,
    config: CalendarConfig,
    name: String,
    onConfigChanged: (CalendarConfig) -> Unit,
) {
    var climatOuvert by rememberSaveable(config.id, index) { mutableStateOf(false) }
    val climat = config.climat(index) ?: ClimatMois()
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = name,
                onValueChange = { newName ->
                    onConfigChanged(config.copy(monthNames = config.monthNames.toMutableList().apply { this[index] = newName }))
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                supportingText = climat.saison.takeIf { it.isNotBlank() }?.let { saison -> { Text(saison) } },
            )
            OutlinedTextField(
                value = config.daysPerMonth.getOrElse(index) { 30 }.toString(),
                onValueChange = { text ->
                    val days = text.filter(Char::isDigit).toIntOrNull()?.coerceIn(1, 99) ?: return@OutlinedTextField
                    onConfigChanged(config.copy(daysPerMonth = config.daysPerMonth.toMutableList().apply { this[index] = days }))
                },
                modifier = Modifier.width(64.dp),
                singleLine = true,
            )
            IconButton(onClick = { climatOuvert = !climatOuvert }) {
                Icon(
                    if (climatOuvert) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (climatOuvert) "Masquer le climat" else "Climat du mois",
                )
            }
        }
        if (climatOuvert) {
            Column(
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Saison", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ClimatMois.saisons.forEach { preset ->
                        FilterChip(
                            selected = climat.saison == preset.saison,
                            onClick = { onConfigChanged(config.avecClimat(index, preset)) },
                            label = { Text(preset.saison) },
                        )
                    }
                    FilterChip(
                        selected = climat == ClimatMois(),
                        onClick = { onConfigChanged(config.avecClimat(index, ClimatMois())) },
                        label = { Text("Libre") },
                    )
                }
                OutlinedTextField(
                    value = climat.saison,
                    onValueChange = { onConfigChanged(config.avecClimat(index, climat.copy(saison = it))) },
                    label = { Text("Nom de la saison") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Text("Météos possibles", style = MaterialTheme.typography.labelMedium)
                ChoixMeteo(
                    meteos = climat.meteos.toSet(),
                    onToggle = { meteo ->
                        val maj = if (meteo in climat.meteos) climat.meteos - meteo else climat.meteos + meteo
                        onConfigChanged(config.avecClimat(index, climat.copy(meteos = maj)))
                    },
                )
                Text("Températures possibles", style = MaterialTheme.typography.labelMedium)
                ChoixTemperatures(
                    temperatures = climat.temperatures.toSet(),
                    onToggle = { t ->
                        val maj = if (t in climat.temperatures) climat.temperatures - t else climat.temperatures + t
                        onConfigChanged(config.avecClimat(index, climat.copy(temperatures = maj)))
                    },
                )
                if (config.monthNames.size > 1) {
                    TextButton(onClick = { onConfigChanged(config.avecMoisRetire(index)) }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                        Text("  Supprimer ce mois", color = Color.White)
                    }
                }
            }
        }
    }
}

/** Un jour férié : nom, mois et jour du mois. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JourFerieEditor(
    ferie: JourFerie,
    config: CalendarConfig,
    onChange: (JourFerie) -> Unit,
    onDelete: () -> Unit,
) {
    var moisMenu by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = ferie.nom,
                onValueChange = { onChange(ferie.copy(nom = it)) },
                label = { Text("Nom") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Supprimer ce jour férié") }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(expanded = moisMenu, onExpandedChange = { moisMenu = !moisMenu }, modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = config.monthNames.getOrElse(ferie.mois) { "Mois ${ferie.mois + 1}" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Mois") },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(moisMenu) },
                )
                ExposedDropdownMenu(expanded = moisMenu, onDismissRequest = { moisMenu = false }) {
                    config.monthNames.forEachIndexed { index, name ->
                        DropdownMenuItem(text = { Text(name) }, onClick = { onChange(ferie.copy(mois = index)); moisMenu = false })
                    }
                }
            }
            OutlinedTextField(
                value = ferie.jour.toString(),
                onValueChange = { text ->
                    val jour = text.filter(Char::isDigit).toIntOrNull() ?: return@OutlinedTextField
                    onChange(ferie.copy(jour = jour.coerceIn(1, config.daysPerMonth.getOrElse(ferie.mois) { 99 })))
                },
                label = { Text("Jour") },
                modifier = Modifier.width(80.dp),
                singleLine = true,
            )
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
