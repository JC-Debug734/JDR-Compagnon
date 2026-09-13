package com.jc2.jdrcompagnon.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jc2.jdrcompagnon.R

/**
 * Service de premier plan minimal : garde le processus actif pendant qu'une
 * partie réseau local est en cours (hébergée ou rejointe), pour que la
 * connexion socket survive au passage de l'app en arrière-plan. Ne fait
 * aucune logique réseau lui-même — NetworkSessionManager s'en charge.
 */
class LanConnectionService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val message = intent?.getStringExtra(EXTRA_MESSAGE) ?: "Partie en réseau local active"
        startForeground(NOTIFICATION_ID, buildNotification(message))
        return START_STICKY
    }

    private fun buildNotification(message: String): Notification {
        createChannelIfNeeded()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JDR Compagnon")
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Session réseau local",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "lan_session"
        private const val NOTIFICATION_ID = 4821
        private const val EXTRA_MESSAGE = "message"

        fun ensureStarted(context: Context, message: String) {
            val intent = Intent(context, LanConnectionService::class.java)
                .putExtra(EXTRA_MESSAGE, message)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /** À appeler après tout arrêt/déconnexion : coupe le service si plus rien n'est actif. */
        fun stopIfIdle(context: Context) {
            val role = NetworkSessionManager.role.value
            val playerState = NetworkSessionManager.playerState.value
            val stillActive = role == SessionRole.HOST ||
                    (role == SessionRole.PLAYER && playerState != PlayerConnectionState.DISCONNECTED)
            if (!stillActive) {
                context.stopService(Intent(context, LanConnectionService::class.java))
            }
        }
    }
}