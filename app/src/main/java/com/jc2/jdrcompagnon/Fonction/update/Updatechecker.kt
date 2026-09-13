package com.jc2.jdrcompagnon.update

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/** [downloadUrl] est l'URL à ouvrir dans le navigateur pour télécharger l'APK. */
data class ReleaseInfo(val versionName: String, val downloadUrl: String)

object UpdateChecker {
    // Le numéro de version vit directement dans app/build.gradle.kts (ligne
    // "// AUTO-VERSION: X.Y.Z"), réécrite automatiquement à chaque
    // compilation — un simple commit+push de build.gradle.kts suffit à
    // publier la nouvelle version, pas de fichier séparé à gérer.
    private const val VERSION_URL =
        "https://raw.githubusercontent.com/JC-Debug734/JDR-Compagnon/main/app/build.gradle.kts"

    private val VERSION_REGEX = Regex("""AUTO-VERSION: (\d+\.\d+\.\d+)""")

    // L'APK signé est committé directement dans le repo (app/release/app-release.apk),
    // remplacé à chaque nouvelle build — pas de service externe (Drive, etc.)
    // à gérer en plus. raw.githubusercontent.com sert le fichier brut sans
    // page d'avertissement intermédiaire, contrairement à Drive.
    private const val APK_URL =
        "https://raw.githubusercontent.com/JC-Debug734/JDR-Compagnon/main/app/release/app-release.apk"

    private const val TAG = "UpdateChecker"

    suspend fun getLatestRelease(): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val text = URL(VERSION_URL).readText()
            val versionName = VERSION_REGEX.find(text)?.groupValues?.get(1)
                ?: run {
                    Log.w(TAG, "Marqueur AUTO-VERSION introuvable dans build.gradle.kts")
                    return@withContext null
                }

            Log.d(TAG, "Version distante (build.gradle.kts) : $versionName")
            ReleaseInfo(versionName, APK_URL)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la lecture de build.gradle.kts", e)
            null
        }
    }

    fun isNewer(remote: String, current: String): Boolean {
        val r = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(r.size, c.size)) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv != cv) return rv > cv
        }
        return false
    }
}