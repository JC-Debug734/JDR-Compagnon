package com.jc2.jdrcompagnon.feature_carte.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jc2.jdrcompagnon.feature_carte.presentation.VoyageSession

/**
 * Côté MJ, quel que soit l'écran : une fois le voyage parti (tout le monde prêt au départ, voir
 * VoyageSession), chaque étape — les haltes de fin de journée puis l'arrivée — fait avancer
 * l'horloge et l'icône du groupe, propose un événement de voyage puis l'écran de repos (le repos
 * long est proposé aux joueurs connectés, qui choisissent leur repos). Après l'arrivée, le voyage
 * est terminé.
 */
@Composable
fun VoyageMjOverlay() {
    val voyage by VoyageSession.etat.collectAsState()
    val progression by VoyageSession.progression.collectAsState()
    val v = voyage?.takeIf { it.enRoute } ?: return
    val etape = progression.etape.coerceAtMost(VoyageSession.nombreEtapes(v) - 1)

    LaunchedEffect(v.id, etape) { VoyageSession.entrerEtape(v, etape) }

    if (!progression.repos) {
        EvenementTrajetDialog(
            campagneId = v.campagneId,
            contexte = "Voyage vers ${v.arriveeNom} · ${VoyageSession.libelleEtape(v, etape)} " +
                "(étape ${etape + 1}/${VoyageSession.nombreEtapes(v)})",
            onDismiss = { VoyageSession.versRepos() },
        )
    } else {
        ReposGroupeDialog(
            lieu = VoyageSession.lieuEtape(v, etape),
            onDismiss = { VoyageSession.etapeSuivante() },
        )
    }
}
