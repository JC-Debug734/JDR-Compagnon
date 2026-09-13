package com.jc2.jdrcompagnon.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Vérifie périodiquement (voir UpdateManager.schedulePeriodicChecks) si
 * version.txt indique une nouvelle version, et notifie l'utilisateur le cas
 * échéant. Ne télécharge rien — la notification ouvre juste la page Drive
 * pour un téléchargement manuel.
 */
class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val release = UpdateManager.checkInBackground(applicationContext) ?: return Result.success()
        UpdateNotifications.showUpdateAvailableNotification(applicationContext, release)
        return Result.success()
    }
}