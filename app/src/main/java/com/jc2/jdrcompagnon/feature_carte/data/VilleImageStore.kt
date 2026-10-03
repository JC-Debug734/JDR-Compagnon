package com.jc2.jdrcompagnon.feature_carte.data

import android.content.Context
import android.net.Uri
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Image d'illustration d'une ville, choisie par le MJ : un fichier par ville, nommé d'après son
 * id (pas de colonne en base). Partagée avec les joueurs connectés (CarteSyncReseau) : chez eux,
 * le même fichier est écrit à la réception.
 */
object VilleImageStore {

    private val _version = MutableStateFlow(0)

    /** Change à chaque image ajoutée, remplacée ou retirée (rafraîchit l'écran et la diffusion). */
    val version: StateFlow<Int> = _version.asStateFlow()

    private fun dossier(context: Context): File =
        File(context.filesDir, "ville_images").apply { mkdirs() }

    fun fichier(context: Context, villeId: String): File = File(dossier(context), "$villeId.img")

    /** Identifie le contenu actuel de l'image (null = pas d'image). */
    fun signature(context: Context, villeId: String): String? =
        fichier(context, villeId).takeIf { it.exists() }?.let { "${it.length()}-${it.lastModified()}" }

    /** Remplace l'image de la ville par [uri] ; false en cas d'échec. */
    fun copier(context: Context, uri: Uri, villeId: String): Boolean = try {
        (context.contentResolver.openInputStream(uri)?.use { input ->
            fichier(context, villeId).outputStream().use { output -> input.copyTo(output) }
        } != null).also { if (it) _version.value++ }
    } catch (e: Exception) {
        false
    }

    /** Image reçue du MJ (joueur en partie réseau). */
    fun ecrire(context: Context, villeId: String, bytes: ByteArray) {
        fichier(context, villeId).writeBytes(bytes)
        _version.value++
    }

    fun supprimer(context: Context, villeId: String) {
        if (fichier(context, villeId).delete()) _version.value++
    }
}
