package com.jc2.jdrcompagnon.feature_combat.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jc2.jdrcompagnon.feature_combat.presentation.NiveauBannieres
import com.jc2.jdrcompagnon.feature_combat.presentation.ReglagesCombatStore
import com.jc2.jdrcompagnon.feature_combat.presentation.TypeRecit
import com.jc2.jdrcompagnon.network.RecitData
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Réglages d'affichage du combat mémorisés d'une session à l'autre : récit déplié en entier ou
 * non, détail technique (dés, DD) affiché sous le récit côté MJ.
 */
object PreferencesCombat {
    private const val PREFS = "combat"
    private var prefs: android.content.SharedPreferences? = null

    private val _recitComplet = MutableStateFlow(false)
    val recitComplet: StateFlow<Boolean> = _recitComplet.asStateFlow()
    private val _journalTechnique = MutableStateFlow(false)
    val journalTechnique: StateFlow<Boolean> = _journalTechnique.asStateFlow()
    // Vue détaillée du combat (boss, récit, ordre du round) sous le tour en cours ; sinon seulement l'essentiel.
    private val _vueDetaillee = MutableStateFlow(false)
    val vueDetaillee: StateFlow<Boolean> = _vueDetaillee.asStateFlow()

    // Joueur, hors de son tour : dés et aptitudes (réactions…) dépliés sous la scène.
    private val _outilsSpectateur = MutableStateFlow(false)
    val outilsSpectateur: StateFlow<Boolean> = _outilsSpectateur.asStateFlow()

    fun basculerOutilsSpectateur() {
        _outilsSpectateur.value = !_outilsSpectateur.value
        prefs?.edit()?.putBoolean("outils_spectateur", _outilsSpectateur.value)?.apply()
    }

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).also {
            _outilsSpectateur.value = it.getBoolean("outils_spectateur", false)
            _recitComplet.value = it.getBoolean("recit_complet", false)
            _journalTechnique.value = it.getBoolean("journal_technique", false)
            _vueDetaillee.value = it.getBoolean("vue_detaillee", false)
        }
        SonsCombat.init(context)
    }

    fun basculerRecitComplet() {
        _recitComplet.value = !_recitComplet.value
        prefs?.edit()?.putBoolean("recit_complet", _recitComplet.value)?.apply()
    }

    fun basculerVueDetaillee() {
        _vueDetaillee.value = !_vueDetaillee.value
        prefs?.edit()?.putBoolean("vue_detaillee", _vueDetaillee.value)?.apply()
    }

    fun basculerJournalTechnique() {
        _journalTechnique.value = !_journalTechnique.value
        prefs?.edit()?.putBoolean("journal_technique", _journalTechnique.value)?.apply()
    }
}

internal fun typeDe(r: RecitData): TypeRecit = runCatching { TypeRecit.valueOf(r.type) }.getOrDefault(TypeRecit.INFO)

/** Couleur d'une ligne du récit selon ce qu'elle raconte. */
internal fun couleurRecit(type: TypeRecit): Color = when (type) {
    TypeRecit.CRITIQUE -> Color(0xFFFFD54F)
    TypeRecit.TOUCHE, TypeRecit.DEGATS -> Color(0xFFFF8A80)
    TypeRecit.RATE -> Color(0xFFB0BEC5)
    TypeRecit.KO, TypeRecit.MORT -> Color(0xFFEF5350)
    TypeRecit.SOIN -> Color(0xFF81C784)
    TypeRecit.ETAT -> Color(0xFFCE93D8)
    TypeRecit.SAUVEGARDE -> Color(0xFF90CAF9)
    TypeRecit.BOSS -> Color(0xFFFFAB40)
    TypeRecit.VICTOIRE, TypeRecit.ROUND -> ForcedDarkPalette.AccentGold
    TypeRecit.DEPLACEMENT -> Color(0xFF80DEEA)
    else -> Color.White
}

/**
 * Le récit du combat, dans l'ordre où les choses se sont passées (à lire de haut en bas, le plus
 * récent en bas) : une ligne par événement, colorée selon ce qu'elle raconte ; les rounds font
 * office de séparateurs. La ligne qui vient d'arriver s'illumine.
 */
@Composable
fun FilRecit(lignes: List<RecitData>, nombre: Int, modifier: Modifier = Modifier) {
    val visibles = lignes.filter { it.texte.isNotBlank() }.takeLast(nombre)
    val plusRecent = lignes.lastOrNull()?.numero
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (visibles.isEmpty()) {
            Text("Le récit du combat s'écrira ici.", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = Color.White.copy(alpha = 0.7f))
        }
        visibles.forEach { r -> LigneRecit(r, nouvelle = r.numero == plusRecent) }
    }
}

@Composable
private fun LigneRecit(r: RecitData, nouvelle: Boolean) {
    val type = typeDe(r)
    if (type == TypeRecit.ROUND) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = ForcedDarkPalette.AccentGold.copy(alpha = 0.5f))
            Text("  ⚔ ${r.texte}  ", fontWeight = FontWeight.Bold, color = ForcedDarkPalette.AccentGold, style = MaterialTheme.typography.labelLarge)
            HorizontalDivider(modifier = Modifier.weight(1f), color = ForcedDarkPalette.AccentGold.copy(alpha = 0.5f))
        }
        return
    }
    // Ligne qui vient d'arriver : halo de sa couleur qui s'estompe.
    val halo = remember(r.numero) { Animatable(if (nouvelle) 0.35f else 0f) }
    LaunchedEffect(r.numero) { if (nouvelle) halo.animateTo(0f, tween(1600)) }
    val couleur = couleurRecit(type)
    val discret = type == TypeRecit.TOUR || type == TypeRecit.INFO
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(couleur.copy(alpha = halo.value), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(type.icone, modifier = Modifier.width(22.dp), style = MaterialTheme.typography.bodyMedium)
        Text(
            r.texte,
            style = if (discret) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            fontWeight = when (type) {
                TypeRecit.CRITIQUE, TypeRecit.KO, TypeRecit.MORT, TypeRecit.VICTOIRE -> FontWeight.Bold
                else -> FontWeight.Normal
            },
            fontStyle = if (type == TypeRecit.BOSS) FontStyle.Italic else FontStyle.Normal,
            color = if (discret) Color.White.copy(alpha = 0.75f) else if (type == TypeRecit.TOUCHE || type == TypeRecit.DEGATS) Color.White else couleur,
        )
    }
}

/**
 * Habille un écran de combat : sons des nouvelles lignes du récit, bannière plein écran pour les
 * moments forts (nouveau round, coup critique, chute, victoire) et pour « À toi ! » quand vient
 * le tour de [monCombattantId] (avec une vibration).
 */
@Composable
fun EffetsRecit(
    lignes: List<RecitData>,
    actifId: String?,
    monCombattantId: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    var dernierVu by remember { mutableStateOf<Int?>(null) }
    var banniere by remember { mutableStateOf<Pair<String, Color>?>(null) }
    var numeroBanniere by remember { mutableStateOf(0) }
    val haptique = LocalHapticFeedback.current
    val dernier = lignes.lastOrNull()?.numero ?: 0

    LaunchedEffect(dernier) {
        val vu = dernierVu
        dernierVu = dernier
        // Premier affichage : on ne rejoue pas ce qui s'est passé avant l'ouverture de l'écran.
        if (vu == null) return@LaunchedEffect
        val nouvelles = lignes.filter { it.numero > vu }
        val fortes = when (ReglagesCombatStore.courant.bannieres) {
            NiveauBannieres.TOUTES -> setOf(TypeRecit.ROUND, TypeRecit.VICTOIRE, TypeRecit.CRITIQUE, TypeRecit.MORT)
            NiveauBannieres.ESSENTIELLES -> setOf(TypeRecit.VICTOIRE, TypeRecit.MORT)
            NiveauBannieres.AUCUNE -> emptySet()
        }
        nouvelles.lastOrNull { typeDe(it) in fortes }?.let { forte ->
            val type = typeDe(forte)
            banniere = when (type) {
                TypeRecit.CRITIQUE -> "💥 COUP CRITIQUE !" to couleurRecit(type)
                TypeRecit.MORT -> "💀 ${forte.texte}" to couleurRecit(type)
                else -> forte.texte to couleurRecit(type)
            }
            numeroBanniere++
        }
        // Un son par nature d'événement (trois attaques touchées : un seul coup), légèrement décalés.
        nouvelles.map { typeDe(it) }.filter { SonsCombat.sonore(it) }.distinct().takeLast(3).forEach {
            SonsCombat.jouer(it)
            delay(180)
        }
    }

    // C'est le tour du joueur : bannière, son et vibration.
    LaunchedEffect(actifId, monCombattantId) {
        if (monCombattantId != null && actifId == monCombattantId) {
            val reglages = ReglagesCombatStore.courant
            if (reglages.bannieres != NiveauBannieres.AUCUNE) {
                banniere = "⚔ À toi de jouer !" to ForcedDarkPalette.AccentGold
                numeroBanniere++
            }
            if (reglages.vibrations) haptique.performHapticFeedback(HapticFeedbackType.LongPress)
            SonsCombat.jouer(TypeRecit.TOUR, monTour = true)
        }
    }
    LaunchedEffect(numeroBanniere) {
        if (banniere == null) return@LaunchedEffect
        delay(1800)
        banniere = null
    }

    Box(modifier = modifier) {
        content()
        AnimatedVisibility(
            visible = banniere != null,
            enter = fadeIn(tween(200)) + scaleIn(tween(300), initialScale = 0.7f),
            exit = fadeOut(tween(400)) + scaleOut(tween(400), targetScale = 1.1f),
            modifier = Modifier.align(Alignment.Center).padding(24.dp),
        ) {
            val (texte, couleur) = banniere ?: ("" to Color.White)
            Box(
                modifier = Modifier
                    .background(Color(0xE6101018), RoundedCornerShape(16.dp))
                    .padding(horizontal = 28.dp, vertical = 18.dp),
            ) {
                Text(texte, color = couleur, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            }
        }
    }
}
