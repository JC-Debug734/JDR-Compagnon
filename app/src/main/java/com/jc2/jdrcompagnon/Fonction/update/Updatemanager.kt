package com.jc2.jdrcompagnon.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jc2.jdrcompagnon.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

/** État affiché par l'UI concernant les mises à jour de l'application. */
sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class UpdateAvailable(val release: ReleaseInfo) : UpdateUiState
}

/**
 * Source unique de vérité pour les mises à jour : lit version.txt (GitHub),
 * compare à la version installée, et ouvre la page Drive dans le navigateur
 * pour un téléchargement + installation entièrement manuels par
 * l'utilisateur. L'app ne télécharge jamais l'APK elle-même — ça évite
 * tout risque de blocage lié au téléchargement d'un gros fichier Drive.
 */
object UpdateManager {
    private const val PERIODIC_WORK_NAME = "update_check_worker"

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    // Dernier numéro de version distant connu (même si pas plus récent que
    // la version installée) — pour affichage informatif dans l'UI.
    private val _lastRemoteVersion = MutableStateFlow<String?>(null)
    val lastRemoteVersion: StateFlow<String?> = _lastRemoteVersion.asStateFlow()

    /**
     * Vérifie version.properties. Renvoie true si une version plus récente
     * existe (état passé à UpdateAvailable), false sinon.
     */
    suspend fun checkForUpdate(context: Context): Boolean {
        if (_state.value !is UpdateUiState.Idle) return _state.value !is UpdateUiState.Idle

        _state.value = UpdateUiState.Checking
        val release = UpdateChecker.getLatestRelease()
        if (release != null) {
            _lastRemoteVersion.value = release.versionName
        }
        return if (release != null && UpdateChecker.isNewer(release.versionName, BuildConfig.VERSION_NAME)) {
            _state.value = UpdateUiState.UpdateAvailable(release)
            true
        } else {
            _state.value = UpdateUiState.Idle
            false
        }
    }

    /** Ouvre la page Drive dans le navigateur pour un téléchargement manuel. */
    fun openDownloadPage(context: Context, release: ReleaseInfo) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(release.driveViewUrl))
        context.startActivity(intent)
        _state.value = UpdateUiState.Idle
    }

    /** Utilisé par UpdateCheckWorker (arrière-plan) : renvoie la release si plus récente, sinon null. */
    suspend fun checkInBackground(context: Context): ReleaseInfo? {
        val release = UpdateChecker.getLatestRelease() ?: return null
        _lastRemoteVersion.value = release.versionName
        return if (UpdateChecker.isNewer(release.versionName, BuildConfig.VERSION_NAME)) release else null
    }

    /** Planifie le check périodique en arrière-plan (toutes les 6h, réseau requis). */
    fun schedulePeriodicChecks(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}