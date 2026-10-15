package com.jc2.jdrcompagnon.feature_combat.ui

import android.content.Context
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatEnCours
import com.jc2.jdrcompagnon.feature_combat.presentation.PhaseCombat
import com.jc2.jdrcompagnon.feature_combat.presentation.ReglagesCombatStore
import com.jc2.jdrcompagnon.ui.MusicManager
import com.jc2.jdrcompagnon.ui.availableLoopTracks

/**
 * Musique du combat (réglages « Musique de combat » / « Musique de boss ») sur l'appareil du MJ :
 * la piste de combat part au round 1, celle du boss dès qu'un boss est en jeu, et la piste
 * d'avant le combat reprend à la fin ([terminer]). Rien ne se relance si le MJ a coupé la musique
 * lui-même.
 */
object MusiqueCombat {
    private var appContext: Context? = null
    private var combatId: String? = null
    private var pisteAvant: String? = null
    private var bossLance = false
    private var aChange = false

    fun suivre(context: Context, etat: CombatEnCours?) {
        appContext = context.applicationContext
        if (etat == null || etat.phase != PhaseCombat.EN_COURS) return
        val r = ReglagesCombatStore.courant
        if (combatId != etat.id) {
            combatId = etat.id
            pisteAvant = MusicManager.currentTrack.value
            bossLance = false
            aChange = false
            r.musiqueCombat?.let { jouer(it) }
        }
        if (!bossLance && etat.boss.isNotEmpty()) {
            bossLance = true
            r.musiqueBoss?.let { jouer(it) }
        }
    }

    private fun jouer(pisteId: String) {
        val context = appContext ?: return
        if (MusicManager.mutedByUser.value) return
        val piste = availableLoopTracks.firstOrNull { it.id == pisteId } ?: return
        MusicManager.play(context, piste)
        aChange = true
    }

    /** Le combat est fermé : la piste d'avant reprend (si la musique a été changée pour le combat). */
    fun terminer() {
        val context = appContext ?: return
        if (combatId == null) return
        combatId = null
        if (!aChange || !ReglagesCombatStore.courant.reprendreMusique || MusicManager.mutedByUser.value) return
        val avant = pisteAvant?.let { nom -> availableLoopTracks.firstOrNull { it.displayName == nom } }
        if (avant != null) MusicManager.play(context, avant) else MusicManager.stop()
    }
}
