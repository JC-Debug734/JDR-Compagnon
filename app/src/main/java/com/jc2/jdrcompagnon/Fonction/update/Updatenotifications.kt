package com.jc2.jdrcompagnon.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.jc2.jdrcompagnon.R

/**
 * Notification affichée quand le worker en arrière-plan détecte une nouvelle
 * version. Tapoter la notification ouvre la page Drive dans le navigateur
 * pour un téléchargement manuel — l'app ne télécharge jamais l'APK elle-même.
 */
object UpdateNotifications {
    private const val CHANNEL_ID = "app_updates"
    private const val NOTIFICATION_ID = 1001

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mises à jour de l'application",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    fun showUpdateAvailableNotification(context: Context, release: ReleaseInfo) {
        ensureChannel(context)

        val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(release.driveViewUrl))
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Mise à jour disponible")
            .setContentText("Version ${release.versionName} disponible — appuie pour télécharger")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            context.getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
        }
    }
}