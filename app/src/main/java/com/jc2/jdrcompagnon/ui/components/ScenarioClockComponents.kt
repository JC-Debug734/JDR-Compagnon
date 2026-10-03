package com.jc2.jdrcompagnon.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Sync
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.network.PlayerConnectionState
import com.jc2.jdrcompagnon.network.SessionRole
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.CalendarConfig
import com.jc2.jdrcompagnon.ui.DayPeriod
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ScenarioClockState
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
}

fun weatherLabel(weather: ScenarioWeather): String = when (weather) {
    ScenarioWeather.CLAIR -> "Ciel dégagé"
    ScenarioWeather.NUAGEUX -> "Nuageux"
    ScenarioWeather.PLUIE -> "Pluie"
    ScenarioWeather.ORAGE -> "Orage"
    ScenarioWeather.NEIGE -> "Neige"
    ScenarioWeather.BROUILLARD -> "Brouillard"
}

/**
 * Ligne compacte affichée dans les menus latéraux MJ et Joueur (Mjdrawer.kt / Joueurdrawer.kt) :
 * icône jour/nuit cohérente avec l'heure de scénario, heure, et icône météo en direct. N'affiche
 * rien tant que la fonction n'est pas activée par le MJ (ScenarioClockState.state.enabled).
 */
@Composable
fun ScenarioClockRow(modifier: Modifier = Modifier) {
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

    // Date complète du calendrier actif, puis l'heure avec secondes (police à chasse fixe : sa
    // largeur ne bouge pas à chaque seconde), météo en dessous.
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
            Column {
                Text(
                    text = ScenarioClockState.formattedCalendarDay(clock.scenarioMinutes, activeCalendar),
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
 * Écran de gestion de l'horloge de scénario, ouvert depuis le tableau de bord MJ
 * (MjHomeScreen), même famille que les autres outils MJ (Campagnes, Groupes, Musique...) —
 * une page dédiée plutôt qu'une fenêtre de dialogue, pour rester cohérent avec le reste.
 * Permet d'activer la fonction, le défilement automatique de l'heure et la météo automatique,
 * plus des réglages manuels (heure, météo) utilisables quand l'automatique est coupé. Chaque
 * changement est rediffusé en direct aux joueurs connectés (voir
 * NetworkSessionManager.broadcastTimeState) : couper le défilement ou la fonction ici l'arrête
 * immédiatement aussi côté joueurs.
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
        // Contenu posé sur une Surface opaque (comme les autres écrans MJ, ex. MusicScreen) :
        // sur le fond d'écran décoratif (Scaffold transparent), le texte par défaut n'était
        // pas assez contrasté pour rester lisible.
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(dayPeriodIcon(period), contentDescription = null, modifier = Modifier.size(28.dp))
                Icon(weatherIcon(clock.weather), contentDescription = null, modifier = Modifier.size(24.dp))
                Text(weatherLabel(clock.weather), style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = ScenarioClockState.formattedCalendarDate(clock.scenarioMinutes, activeCalendar, clock.scenarioSeconds),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
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
                label = "Météo automatique",
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

            if (clock.enabled) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(
                    text = "Réglage manuel de l'heure",
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { ScenarioClockState.advanceManually(-60) }) { Text("-1 h") }
                    OutlinedButton(onClick = { ScenarioClockState.advanceManually(-10) }) { Text("-10 min") }
                    OutlinedButton(onClick = { ScenarioClockState.advanceManually(10) }) { Text("+10 min") }
                    OutlinedButton(onClick = { ScenarioClockState.advanceManually(60) }) { Text("+1 h") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Réglage manuel du jour",
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { ScenarioClockState.advanceDays(-1) }) { Text("-1 jour") }
                    OutlinedButton(onClick = { ScenarioClockState.advanceDays(1) }) { Text("+1 jour") }
                }

                if (!clock.autoWeatherEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Météo manuelle",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ScenarioWeather.entries.forEach { weather ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(weatherIcon(weather), contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = weatherLabel(weather),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            if (clock.weather == weather) {
                                Text("Actuelle", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            } else {
                                TextButton(onClick = { ScenarioClockState.setWeather(weather) }) { Text("Choisir", color = Color.White) }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text(text = "Calendrier", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))

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
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val created = ScenarioClockState.addCalendar("Nouveau calendrier ${calendars.size + 1}")
                    ScenarioClockState.setActiveCalendarId(created.id)
                }) { Text("Nouveau calendrier") }
                if (calendars.none { it.nom == CalendarConfig.calendrierHarptos().nom }) {
                    OutlinedButton(onClick = {
                        val created = ScenarioClockState.addHarptosCalendar()
                        ScenarioClockState.setActiveCalendarId(created.id)
                    }) { Text("Calendrier D&D (Harpistes)") }
                }
                if (calendars.size > 1) {
                    OutlinedButton(onClick = { ScenarioClockState.deleteCalendar(activeCalendar.id) }) {
                        Text("Supprimer ce calendrier")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            DatePickerRow(
                config = activeCalendar,
                initialMinutes = clock.scenarioMinutes,
                onSetDate = { year, month, day -> ScenarioClockState.setCurrentDate(year, month, day) },
            )
            Spacer(modifier = Modifier.height(12.dp))
            CalendarConfigEditor(
                config = activeCalendar,
                onConfigChanged = { ScenarioClockState.updateCalendar(it) },
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
        }
        }
    }
}

/**
 * Sélecteur de date (année/mois/jour) réutilisé par l'Horloge et l'écran Campagne — un seul
 * calendrier global, partagé entre les deux outils (voir ScenarioClockState).
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
 * Édition des noms de mois/jours et du nombre de jours par mois — 12 mois et 7 jours de semaine
 * fixes (comme le calendrier grégorien), mais entièrement renommables et le nombre de jours par
 * mois ajustable, pour approcher un calendrier fantastique (ex. Calendrier des Harpistes).
 */
@Composable
fun CalendarConfigEditor(
    config: CalendarConfig,
    onConfigChanged: (CalendarConfig) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    TextButton(onClick = { expanded = !expanded }) {
        Text(if (expanded) "Masquer la configuration du calendrier" else "Configurer le calendrier", color = Color.White)
    }
    if (!expanded) return

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

        Text(text = "Mois (nom et nombre de jours)", style = MaterialTheme.typography.labelMedium)
        config.monthNames.forEachIndexed { index, name ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { newName ->
                        onConfigChanged(config.copy(monthNames = config.monthNames.toMutableList().apply { this[index] = newName }))
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = config.daysPerMonth.getOrElse(index) { 30 }.toString(),
                    onValueChange = { text ->
                        val days = text.filter(Char::isDigit).toIntOrNull()?.coerceIn(1, 99) ?: return@OutlinedTextField
                        onConfigChanged(config.copy(daysPerMonth = config.daysPerMonth.toMutableList().apply { this[index] = days }))
                    },
                    modifier = Modifier.width(70.dp),
                    singleLine = true,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "Jours de la semaine", style = MaterialTheme.typography.labelMedium)
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
