package com.jc2.jdrcompagnon.network

import android.content.Context
import android.net.Uri
import fi.iki.elonen.NanoHTTPD
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

/**
 * Retourne l'adresse IPv4 locale de l'appareil sur le réseau (Wi-Fi ou point
 * d'accès), pour affichage au MJ. Passe par NetworkInterface plutôt que
 * WifiManager.connectionInfo pour fonctionner aussi bien quand l'appareil
 * est client Wi-Fi que lorsqu'il agit lui-même comme point d'accès.
 */
internal fun getLocalIpAddress(): String? = try {
    Collections.list(NetworkInterface.getNetworkInterfaces())
        .asSequence()
        .flatMap { intf -> Collections.list(intf.inetAddresses).asSequence() }
        .filterIsInstance<Inet4Address>()
        .firstOrNull { !it.isLoopbackAddress }
        ?.hostAddress
} catch (e: Exception) {
    null
}

/**
 * Serveur HTTP embarqué démarré en même temps que le serveur de partie :
 * expose une page web (accessible depuis n'importe quel navigateur du même
 * Wi-Fi) sur laquelle le MJ peut pousser des photos ou documents à afficher
 * à toute la table, sans que les joueurs aient besoin d'installer quoi que
 * ce soit. La page affiche "Connexion réussie" puis se met à jour toute
 * seule (polling) à chaque envoi de fichier par le MJ.
 */
class MjWebServer(private val context: Context) : NanoHTTPD(0) {

    private data class MediaItem(val fileName: String, val displayName: String)

    /** État affiché en haut de la page (heure de fiction + météo), rafraîchi par NetworkSessionManager. */
    data class TableStatus(val time: String, val weatherKey: String, val weatherLabel: String)

    private val mediaDir: java.io.File = java.io.File(context.cacheDir, "mj_web_media").apply { mkdirs() }
    // Une seule photo affichée à la fois (pas une galerie qui s'accumule) : un nouvel envoi
    // remplace la précédente, à la fois sur la page et sur le disque.
    private var currentMedia: MediaItem? = null
    private var currentStatus: TableStatus? = null

    // Ordre des envois (photo ou carte d'exploration) : le dernier envoyé passe au premier plan.
    // Sans cela, une carte d'exploration restait par-dessus une photo envoyée ensuite, et
    // l'inverse une fois l'exploration retirée.
    private var sequenceAffichage = 0
    private var mediaSequence = 0
    private var explorationSequence = 0

    /** Copie le fichier pointé par [sourceUri] dans le dossier servi ; remplace la photo affichée. */
    fun addMedia(sourceUri: Uri, displayName: String): Boolean =
        addMedia(displayName) { context.contentResolver.openInputStream(sourceUri) }

    /** Variante pour une image embarquée dans les assets (ex. portrait PNJ de assets/dnd/PNJ). */
    fun addAssetMedia(assetPath: String, displayName: String): Boolean =
        addMedia(displayName) { context.assets.open(assetPath) }

    @Synchronized
    private fun addMedia(displayName: String, open: () -> java.io.InputStream?): Boolean {
        return try {
            val safeName = "${System.currentTimeMillis()}_${displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")}"
            val dest = java.io.File(mediaDir, safeName)
            val copied = open()?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
            if (copied) {
                val previous = currentMedia
                currentMedia = MediaItem(safeName, displayName)
                mediaSequence = ++sequenceAffichage
                previous?.let { java.io.File(mediaDir, it.fileName).delete() }
            }
            copied
        } catch (e: Exception) {
            false
        }
    }

    /** Retire la photo déjà envoyée (nouvelle scène, table remise à zéro). */
    @Synchronized
    fun clearMedia() {
        currentMedia?.let { java.io.File(mediaDir, it.fileName).delete() }
        currentMedia = null
    }

    /** Met à jour l'heure/météo affichées en haut de la page (voir NetworkSessionManager). */
    @Synchronized
    fun updateStatus(time: String, weatherKey: String, weatherLabel: String) {
        currentStatus = TableStatus(time, weatherKey, weatherLabel)
    }

    /** Scénario en cours de lecture et image de sa scène courante (fond de la page table). */
    data class SceneInfo(val scenarioTitle: String, val sceneTitle: String?, val imageFile: java.io.File?)

    private var currentScene: SceneInfo? = null
    // Incrémentée à chaque changement : force le navigateur à recharger l'image (cache).
    private var sceneVersion = 0

    @Synchronized
    fun updateScene(info: SceneInfo?) {
        if (info == currentScene) return
        currentScene = info
        sceneVersion++
    }

    /**
     * Carte d'exploration (brouillard de guerre) : image découpée en [colonnes] x [lignes] cases,
     * seules les cases de [revelees] (index = ligne * colonnes + colonne) sont visibles des joueurs.
     */
    data class ExplorationInfo(
        val imageFile: java.io.File,
        val colonnes: Int,
        val lignes: Int,
        val revelees: Set<Int>,
        // Lieux montrés aux joueurs, dessinés par-dessus le brouillard.
        val lieux: List<LieuTable> = emptyList(),
        // Zone sur laquelle la table zoome (double appui du MJ), null = carte entière.
        val zoom: ZoomTable? = null,
    )

    /** Cadre de zoom en fractions de l'image (coin haut-gauche, largeur, hauteur). */
    data class ZoomTable(val x: Float, val y: Float, val largeur: Float, val hauteur: Float)

    /** Lieu visible des joueurs : centre de son icône en fractions de l'image ([fx], [fy]). */
    data class LieuTable(val nom: String, val fx: Float, val fy: Float, val emoji: String, val couleurArgb: Int?)

    // Orientation (degrés, par quarts de tour) choisie par le MJ sur l'écran de carte : appliquée
    // à toutes les images de la table (photo envoyée, carte d'exploration, fond de scène, épreuve).
    private var rotationTable = 0

    @Synchronized
    fun updateRotation(degres: Int) {
        rotationTable = ((degres % 360) + 360) % 360
    }

    private var currentExploration: ExplorationInfo? = null
    // Version de l'image (cache navigateur) et de l'état (redessin du brouillard seulement si changé).
    private var explorationImageVersion = 0
    private var explorationVersion = 0

    @Synchronized
    fun updateExploration(info: ExplorationInfo?) {
        if (info == currentExploration) return
        if (info?.imageFile != currentExploration?.imageFile) explorationImageVersion++
        // Le MJ agit sur la carte (envoi, case dévoilée, lieu montré) : elle repasse devant.
        if (info != null) explorationSequence = ++sequenceAffichage
        currentExploration = info
        explorationVersion++
    }

    @Synchronized
    private fun explorationJson(): String {
        val explo = currentExploration ?: return "null"
        if (!explo.imageFile.exists()) return "null"
        val total = explo.colonnes * explo.lignes
        val revelees = buildString(total) { for (i in 0 until total) append(if (i in explo.revelees) '1' else '0') }
        val lieux = explo.lieux.joinToString(",", "[", "]") { l ->
            val couleur = l.couleurArgb?.let { "#%06X".format(it and 0xFFFFFF) } ?: "#8D6E63"
            """{"nom":"${escapeJson(l.nom)}","x":${l.fx},"y":${l.fy},"emoji":"${escapeJson(l.emoji)}","color":"$couleur"}"""
        }
        val zoom = explo.zoom?.let { z -> """{"x":${z.x},"y":${z.y},"w":${z.largeur},"h":${z.hauteur}}""" } ?: "null"
        return """{"version":$explorationVersion,"image":"/exploration-image?v=$explorationImageVersion",""" +
            """"cols":${explo.colonnes},"rows":${explo.lignes},"revealed":"$revelees","lieux":$lieux,"zoom":$zoom,""" +
            """"masquee":${currentMedia != null && mediaSequence > explorationSequence}}"""
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        return when {
            uri == "/" || uri == "/index.html" ->
                newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", indexHtml())
            uri == "/api/media" ->
                newFixedLengthResponse(Response.Status.OK, "application/json", mediaJson())
            uri == "/api/status" ->
                newFixedLengthResponse(Response.Status.OK, "application/json", statusJson())
            uri == "/api/combat" ->
                newFixedLengthResponse(Response.Status.OK, "application/json", combatJson())
            uri == "/api/rotation" ->
                newFixedLengthResponse(Response.Status.OK, "application/json", synchronized(this) { "$rotationTable" })
            uri == "/api/scene" ->
                newFixedLengthResponse(Response.Status.OK, "application/json", sceneJson())
            uri == "/scene-image" -> serveSceneImage()
            uri == "/api/exploration" ->
                newFixedLengthResponse(Response.Status.OK, "application/json", explorationJson())
            uri == "/exploration-image" -> serveImageFile(synchronized(this) { currentExploration?.imageFile })
            uri == "/api/epreuve" ->
                newFixedLengthResponse(Response.Status.OK, "application/json", epreuveJson())
            uri == "/epreuve-image" -> serveEpreuveImage()
            uri.startsWith("/media/") -> serveMediaFile(uri.removePrefix("/media/"))
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Introuvable")
        }
    }

    @Synchronized
    private fun mediaJson(): String {
        val item = currentMedia ?: return "null"
        return """{"url":"/media/${escapeJson(item.fileName)}","type":"${if (isImage(item.fileName)) "image" else "document"}","name":"${escapeJson(item.displayName)}"}"""
    }

    @Synchronized
    private fun statusJson(): String {
        val status = currentStatus ?: return "null"
        return """{"time":"${escapeJson(status.time)}","weatherKey":"${escapeJson(status.weatherKey)}","weatherLabel":"${escapeJson(status.weatherLabel)}"}"""
    }

    /**
     * Fiche du combat en cours (CombatSession) pour la table : ordre d'initiative, tour actif,
     * PV exacts des personnages. Les PV des monstres restent secrets : seul un état de santé
     * approximatif est transmis (comme le décrirait le MJ).
     */
    private fun combatJson(): String {
        val combat = com.jc2.jdrcompagnon.feature_combat.presentation.CombatSession.etat.value ?: return "null"
        val actifId = combat.actif?.id
        val combattants = combat.combattants.joinToString(",") { c ->
            val etatSante = when {
                c.pv <= 0 -> if (c.estMonstre) "Vaincu" else "À terre"
                c.pv * 4 <= c.pvMax -> "Gravement blessé"
                c.pv * 2 <= c.pvMax -> "En sang"
                c.pv < c.pvMax -> "Blessé"
                else -> "Indemne"
            }
            val ratio = if (c.pvMax > 0) (c.pv.coerceAtLeast(0) * 100 / c.pvMax) else 0
            val pv = if (c.estMonstre) "null" else "\"${c.pv}/${c.pvMax}\""
            val conditions = c.conditions.joinToString(",") { "\"${escapeJson(it.label)}\"" }
            """{"nom":"${escapeJson(c.nom)}","monstre":${c.estMonstre},"initiative":${c.initiative ?: "null"},""" +
                """"pv":$pv,"ratio":$ratio,"sante":"${escapeJson(etatSante)}","actif":${c.id == actifId},"conditions":[$conditions]}"""
        }
        return """{"titre":"${escapeJson(combat.titre)}","round":${combat.round},"combattants":[$combattants]}"""
    }

    @Synchronized
    private fun sceneJson(): String {
        val scene = currentScene ?: return "null"
        val image = if (scene.imageFile?.exists() == true) "\"/scene-image?v=$sceneVersion\"" else "null"
        val sceneTitle = scene.sceneTitle?.let { "\"${escapeJson(it)}\"" } ?: "null"
        return """{"scenario":"${escapeJson(scene.scenarioTitle)}","scene":$sceneTitle,"image":$image}"""
    }

    private fun serveSceneImage(): Response = serveImageFile(synchronized(this) { currentScene?.imageFile })

    private fun serveImageFile(file: java.io.File?): Response {
        if (file == null || !file.exists()) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Introuvable")
        // Les images d'environnement sont stockées sans extension (.img) : type deviné d'après
        // les premiers octets du fichier.
        val header = ByteArray(12)
        val read = file.inputStream().use { it.read(header) }
        val mime = when {
            read >= 4 && header[0] == 0x89.toByte() && header[1] == 'P'.code.toByte() -> "image/png"
            read >= 2 && header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() -> "image/jpeg"
            read >= 3 && header[0] == 'G'.code.toByte() && header[1] == 'I'.code.toByte() -> "image/gif"
            read >= 12 && String(header, 8, 4, Charsets.US_ASCII) == "WEBP" -> "image/webp"
            else -> mimeTypeFor(file.name).takeIf { it.startsWith("image/") } ?: "image/jpeg"
        }
        return newFixedLengthResponse(Response.Status.OK, mime, file.inputStream(), file.length())
    }

    // Retours à la ligne échappés aussi : une description sur plusieurs lignes (épreuve) rendait
    // le JSON invalide.
    private fun escapeJson(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")

    /**
     * Épreuve de l'outil ÉPREUVES en cours (EpreuveOutilSession) : image en plein écran, compteur
     * de réussites et dernière complication. `numero` change à chaque complication tirée pour que
     * la page rejoue son animation.
     */
    private fun epreuveJson(): String {
        val etat = com.jc2.jdrcompagnon.feature_epreuve.presentation.EpreuveOutilSession.etat.value ?: return "null"
        val epreuve = etat.epreuve
        val image = epreuve.imageFileName
            ?.takeIf { com.jc2.jdrcompagnon.feature_epreuve.data.EpreuveImageStore.fichier(context, it).exists() }
            ?.let { "\"/epreuve-image?v=${escapeJson(it)}\"" } ?: "null"
        val complication = etat.derniereComplication?.let {
            """{"titre":"${escapeJson(it.titre)}","description":"${escapeJson(it.description)}","numero":${etat.numeroComplication}}"""
        } ?: "null"
        return """{"id":"${escapeJson(epreuve.id)}","nom":"${escapeJson(epreuve.nom)}","description":"${escapeJson(epreuve.description)}",""" +
            """"reussites":${etat.reussites},"requises":${epreuve.reussitesRequises},"echecs":${etat.echecs},""" +
            """"terminee":${etat.terminee},"reussie":${etat.reussie},"image":$image,"complication":$complication}"""
    }

    private fun serveEpreuveImage(): Response {
        val fileName = com.jc2.jdrcompagnon.feature_epreuve.presentation.EpreuveOutilSession.etat.value?.epreuve?.imageFileName
        return serveImageFile(fileName?.let { com.jc2.jdrcompagnon.feature_epreuve.data.EpreuveImageStore.fichier(context, it) })
    }

    private fun isImage(name: String): Boolean {
        val lower = name.lowercase()
        return listOf(".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp").any { lower.endsWith(it) }
    }

    private fun mimeTypeFor(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".gif") -> "image/gif"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".bmp") -> "image/bmp"
            lower.endsWith(".pdf") -> "application/pdf"
            else -> "application/octet-stream"
        }
    }

    private fun serveMediaFile(name: String): Response {
        val file = java.io.File(mediaDir, name)
        if (!file.exists()) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Introuvable")
        return newFixedLengthResponse(Response.Status.OK, mimeTypeFor(file.name), file.inputStream(), file.length())
    }

    private fun indexHtml(): String = """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Table de jeu</title>
        <style>
          html, body {
            margin: 0; padding: 0; height: 100%; overflow: hidden;
            background: #0c0c14; color: #f0f0f5; font-family: system-ui, sans-serif;
          }
          #topbar {
            position: fixed; top: 0; left: 0; right: 0; z-index: 10;
            display: flex; align-items: center; gap: 16px;
            padding: 10px 20px; background: rgba(10,10,16,0.55); backdrop-filter: blur(4px);
            font-size: 1.15rem;
          }
          #topbar .icon { font-size: 1.4rem; }
          #sceneLabel { margin-left: auto; font-weight: 700; text-align: right; }
          #sceneLabel .scene { font-weight: 400; color: #c8c8d8; }
          /* Fonds plein écran : centrés pour pouvoir pivoter (dimensions inversées à 90°/270°). */
          #sceneBg, #epreuveBg {
            position: fixed; left: 50%; top: 50%; width: 100vw; height: 100vh;
            transform: translate(-50%, -50%);
            background-size: cover; background-position: center; background-repeat: no-repeat;
          }
          #sceneBg { z-index: 0; transition: background-image 0.6s ease-in-out; }
          #stage { z-index: 1; }
          #stage {
            position: fixed; inset: 0; display: flex; align-items: center; justify-content: center;
          }
          /* Une photo affichée remplace l'image de scène en fond (sinon visible autour d'elle). */
          #stage.photo { background: #0c0c14; }
          #stage img {
            max-width: 100vw; max-height: 100vh; object-fit: contain; transition: transform .4s ease;
          }
          #stage a {
            display: inline-block; padding: 16px 24px; background: #2a2a38; border-radius: 8px;
            color: #f0f0f5; text-decoration: none; font-size: 1.2rem;
          }
          #empty { color: #888; font-size: 1.1rem; }
          #weatherFx { position: fixed; inset: 0; pointer-events: none; z-index: 5; overflow: hidden; }

          .drop {
            position: absolute; top: -5vh; width: 2px; height: 60px;
            background: linear-gradient(to bottom, rgba(180,200,255,0), rgba(180,200,255,0.55));
            animation: fall linear infinite;
          }
          .flake {
            position: absolute; top: -5vh; border-radius: 50%; background: rgba(255,255,255,0.85);
            animation: fallSlow linear infinite;
          }
          @keyframes fall { to { transform: translateY(110vh); } }
          @keyframes fallSlow { to { transform: translateY(110vh) translateX(20px); } }

          .fogLayer {
            position: absolute; inset: -10%; background: rgba(200,200,210,0.18);
            filter: blur(20px); animation: drift 18s ease-in-out infinite alternate;
          }
          @keyframes drift { from { transform: translateX(-5%); } to { transform: translateX(5%); } }

          .cloud {
            position: absolute; top: 8%; width: 220px; height: 60px; border-radius: 50%;
            background: rgba(255,255,255,0.12); filter: blur(6px);
            animation: cloudMove 40s linear infinite;
          }
          @keyframes cloudMove { from { transform: translateX(-30vw); } to { transform: translateX(130vw); } }

          .flash {
            position: absolute; inset: 0; background: rgba(255,255,255,0.9); opacity: 0;
            animation: strike 6s ease-in-out infinite;
          }
          @keyframes strike {
            0%, 92%, 100% { opacity: 0; }
            93% { opacity: 0.85; }
            94% { opacity: 0; }
            95% { opacity: 0.6; }
            96% { opacity: 0; }
          }

          #combat {
            position: fixed; z-index: 20; top: 56px; right: 16px; bottom: 16px;
            width: min(420px, 42vw); overflow-y: auto; display: none;
            background: rgba(12,12,20,0.82); backdrop-filter: blur(6px);
            border: 1px solid rgba(255,255,255,0.12); border-radius: 12px; padding: 14px 16px;
          }
          #combat h2 { margin: 0 0 2px; font-size: 1.3rem; }
          #combat .sub { color: #b8b8c8; font-size: 0.95rem; margin-bottom: 10px; }
          .fighter {
            display: flex; align-items: center; gap: 10px; padding: 8px 10px; margin-bottom: 6px;
            border-radius: 8px; background: rgba(255,255,255,0.05); border-left: 4px solid #64b5f6;
          }
          .fighter.monstre { border-left-color: #e57373; }
          .fighter.actif { background: rgba(255,215,120,0.18); outline: 2px solid #ffd778; }
          .fighter.out { opacity: 0.45; }
          .fighter .init { width: 2.2em; text-align: center; font-weight: 700; font-size: 1.2rem; }
          .fighter .body { flex: 1; min-width: 0; }
          .fighter .name { font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
          .fighter .meta { font-size: 0.85rem; color: #c8c8d8; }
          .bar { height: 6px; border-radius: 3px; background: rgba(255,255,255,0.12); margin-top: 4px; overflow: hidden; }
          .bar > div { height: 100%; }
          @media (max-width: 700px) {
            #combat { top: auto; left: 8px; right: 8px; bottom: 8px; width: auto; max-height: 55vh; }
          }

          #explo {
            position: fixed; inset: 0; z-index: 3; display: none;
            align-items: center; justify-content: center; background: #000; overflow: hidden;
          }
          #exploBox { position: relative; transition: transform .6s ease; }
          #exploBox img { position: absolute; inset: 0; width: 100%; height: 100%; }
          #exploBox canvas { position: absolute; inset: 0; width: 100%; height: 100%; }
          #exploLieux { position: absolute; inset: 0; pointer-events: none; }
          .lieu {
            position: absolute; transform: translate(-50%, -17px); transform-origin: 50% 17px;
            display: flex; flex-direction: column; align-items: center;
          }
          .lieu .pin {
            width: 34px; height: 34px; border-radius: 50%; border: 2px solid #fff;
            display: flex; align-items: center; justify-content: center; font-size: 18px;
            box-shadow: 0 2px 6px rgba(0,0,0,0.6);
          }
          .lieu .nom {
            margin-top: 2px; font-size: 0.85rem; padding: 1px 6px; border-radius: 4px;
            background: rgba(0,0,0,0.65); white-space: nowrap;
          }

          #epreuve { position: fixed; inset: 0; z-index: 4; display: none; overflow: hidden; background: #0c0c14; }
          #epreuve.sansImage { background: radial-gradient(circle at 50% 30%, #2b3550, #0c0c14 75%); }
          #epreuveBg { position: absolute; }
          #epreuveFlash {
            position: absolute; inset: 0; pointer-events: none; opacity: 0;
            background: radial-gradient(circle, rgba(229,57,53,0) 40%, rgba(229,57,53,0.55));
          }
          #epreuveFlash.on { animation: flashRouge 1.2s ease-out; }
          @keyframes flashRouge { 0% { opacity: 1; } 100% { opacity: 0; } }
          #epreuvePanel {
            position: absolute; left: 50%; bottom: 24px; transform: translateX(-50%);
            width: min(900px, calc(100vw - 32px)); box-sizing: border-box; max-height: 70vh; overflow-y: auto;
            background: rgba(12,12,20,0.82); backdrop-filter: blur(6px);
            border: 1px solid rgba(255,255,255,0.15); border-radius: 14px; padding: 16px 20px;
          }
          #epreuvePanel h2 { margin: 0 0 4px; font-size: 1.7rem; }
          #epreuvePanel .desc { color: #d8d8e4; font-style: italic; margin-bottom: 12px; white-space: pre-line; }
          .pips { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; }
          .pip { width: 28px; height: 28px; border-radius: 50%; border: 3px solid #d4af37; box-sizing: border-box; }
          .pip.on { background: #d4af37; box-shadow: 0 0 12px rgba(212,175,55,0.8); }
          .pipsLabel { margin-left: 8px; font-weight: 700; font-size: 1.15rem; }
          #epreuveComplication {
            margin-top: 14px; padding: 12px 14px; border-radius: 10px;
            background: rgba(198,40,40,0.35); border: 2px solid #e53935;
          }
          #epreuveComplication .titre { font-weight: 800; font-size: 1.3rem; }
          #epreuveComplication .effet { margin-top: 4px; white-space: pre-line; }
          #epreuveComplication.shake { animation: shake 0.6s; }
          @keyframes shake {
            0%, 100% { transform: translateX(0); }
            20%, 60% { transform: translateX(-10px); }
            40%, 80% { transform: translateX(10px); }
          }
          #epreuveIssue { margin-top: 14px; font-size: 1.6rem; font-weight: 900; text-align: center; }
          #epreuveIssue.reussie { color: #81c784; text-shadow: 0 0 14px rgba(129,199,132,0.7); }

          .sunGlow {
            position: absolute; top: -20%; right: -10%; width: 60vw; height: 60vw; border-radius: 50%;
            background: radial-gradient(circle, rgba(255,220,140,0.25), rgba(255,220,140,0) 70%);
          }
        </style>
        </head>
        <body>
        <div id="topbar">
          <span class="icon" id="weatherIcon">☀️</span>
          <span id="timeLabel">Connexion réussie</span>
          <span id="weatherLabel"></span>
          <span id="sceneLabel"></span>
        </div>
        <div id="sceneBg"></div>
        <div id="weatherFx"></div>
        <div id="stage">
          <div id="empty">En attente d'un envoi du MJ…</div>
        </div>
        <div id="explo"><div id="exploBox"><img id="exploImg" alt="Carte"><canvas id="exploFog"></canvas><div id="exploLieux"></div></div></div>
        <div id="epreuve">
          <div id="epreuveBg"></div>
          <div id="epreuveFlash"></div>
          <div id="epreuvePanel">
            <h2 id="epreuveNom"></h2>
            <div class="desc" id="epreuveDesc"></div>
            <div class="pips" id="epreuvePips"></div>
            <div id="epreuveComplication" style="display:none"><div class="titre"></div><div class="effet"></div></div>
            <div id="epreuveIssue" style="display:none"></div>
          </div>
        </div>
        <div id="combat"></div>
        <script>
        function esc(s) {
          return String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
        }

        async function refreshCombat() {
          const panel = document.getElementById('combat');
          try {
            const res = await fetch('/api/combat');
            const combat = await res.json();
            if (!combat) { panel.style.display = 'none'; return; }
            const actif = combat.combattants.find(c => c.actif);
            let html = '<h2>⚔ ' + esc(combat.titre) + '</h2><div class="sub">' +
              (combat.round > 0 ? 'Round ' + combat.round + (actif ? ' — au tour de <b>' + esc(actif.nom) + '</b>' : '')
                                : 'Préparation du combat…') + '</div>';
            for (const c of combat.combattants) {
              const out = c.ratio <= 0;
              const color = c.ratio > 50 ? '#66bb6a' : (c.ratio > 25 ? '#ffa726' : '#ef5350');
              const meta = (c.pv ? 'PV ' + esc(c.pv) : esc(c.sante)) +
                (c.conditions.length ? ' · ' + c.conditions.map(esc).join(', ') : '');
              html += '<div class="fighter' + (c.monstre ? ' monstre' : '') + (c.actif ? ' actif' : '') + (out ? ' out' : '') + '">' +
                '<div class="init">' + (c.initiative ?? '–') + '</div>' +
                '<div class="body"><div class="name">' + (c.actif ? '▶ ' : '') + esc(c.nom) + '</div>' +
                '<div class="meta">' + meta + '</div>' +
                '<div class="bar"><div style="width:' + Math.max(0, c.ratio) + '%;background:' + color + '"></div></div></div></div>';
            }
            panel.innerHTML = html;
            panel.style.display = 'block';
          } catch (e) {}
        }

        let currentWeatherKey = null;

        function renderWeatherFx(key) {
          const fx = document.getElementById('weatherFx');
          fx.innerHTML = '';
          if (key === 'pluie' || key === 'orage') {
            for (let i = 0; i < 60; i++) {
              const d = document.createElement('div');
              d.className = 'drop';
              d.style.left = (Math.random() * 100) + 'vw';
              d.style.animationDuration = (0.5 + Math.random() * 0.4) + 's';
              d.style.animationDelay = (Math.random() * 2) + 's';
              fx.appendChild(d);
            }
            if (key === 'orage') {
              const flash = document.createElement('div');
              flash.className = 'flash';
              fx.appendChild(flash);
            }
          } else if (key === 'neige') {
            for (let i = 0; i < 50; i++) {
              const f = document.createElement('div');
              f.className = 'flake';
              const size = 3 + Math.random() * 4;
              f.style.width = size + 'px';
              f.style.height = size + 'px';
              f.style.left = (Math.random() * 100) + 'vw';
              f.style.animationDuration = (4 + Math.random() * 4) + 's';
              f.style.animationDelay = (Math.random() * 4) + 's';
              fx.appendChild(f);
            }
          } else if (key === 'brouillard') {
            for (let i = 0; i < 3; i++) {
              const layer = document.createElement('div');
              layer.className = 'fogLayer';
              layer.style.animationDelay = (i * 3) + 's';
              fx.appendChild(layer);
            }
          } else if (key === 'nuageux') {
            for (let i = 0; i < 4; i++) {
              const c = document.createElement('div');
              c.className = 'cloud';
              c.style.top = (5 + i * 12) + '%';
              c.style.animationDuration = (30 + i * 8) + 's';
              c.style.animationDelay = (-i * 10) + 's';
              fx.appendChild(c);
            }
          } else if (key === 'clair') {
            const glow = document.createElement('div');
            glow.className = 'sunGlow';
            fx.appendChild(glow);
          }
        }

        const weatherIcons = { clair: '☀️', nuageux: '☁️', pluie: '🌧️', orage: '⛈️', neige: '❄️', brouillard: '🌫️', forte_neige: '🌨️', vent_fort: '🌬️', sec: '🏜️' };

        async function refreshStatus() {
          try {
            const res = await fetch('/api/status');
            const status = await res.json();
            if (!status) return;
            document.getElementById('timeLabel').textContent = status.time;
            document.getElementById('weatherLabel').textContent = status.weatherLabel;
            document.getElementById('weatherIcon').textContent = weatherIcons[status.weatherKey] || '☀️';
            if (status.weatherKey !== currentWeatherKey) {
              currentWeatherKey = status.weatherKey;
              renderWeatherFx(status.weatherKey);
            }
          } catch (e) {}
        }

        let hasSceneImage = false;
        let currentSceneImage = null;
        let currentMediaKey = null;

        // Orientation choisie par le MJ sur l'écran de carte (0, 90, 180, 270) : toutes les images
        // de la table pivotent ; à 90°/270° leurs dimensions sont inversées pour remplir l'écran.
        let tableRotation = 0;
        function quartTourne() { return tableRotation % 180 !== 0; }

        function applyRotation() {
          const r = tableRotation, q = quartTourne();
          for (const id of ['sceneBg', 'epreuveBg']) {
            const el = document.getElementById(id);
            el.style.width = q ? '100vh' : '100vw';
            el.style.height = q ? '100vw' : '100vh';
            el.style.transform = 'translate(-50%, -50%) rotate(' + r + 'deg)';
          }
          const img = document.querySelector('#stage img');
          if (img) {
            img.style.maxWidth = q ? '100vh' : '100vw';
            img.style.maxHeight = q ? '100vw' : '100vh';
            img.style.transform = r ? 'rotate(' + r + 'deg)' : '';
          }
          layoutExplo();
        }

        async function refreshRotation() {
          try {
            const res = await fetch('/api/rotation');
            const r = await res.json();
            if (typeof r === 'number' && r !== tableRotation) {
              tableRotation = r;
              applyRotation();
            }
          } catch (e) {}
        }

        async function refreshScene() {
          try {
            const res = await fetch('/api/scene');
            const scene = await res.json();
            const label = document.getElementById('sceneLabel');
            const bg = document.getElementById('sceneBg');
            if (!scene) {
              label.innerHTML = '';
              bg.style.backgroundImage = '';
              hasSceneImage = false;
              currentSceneImage = null;
              return;
            }
            label.innerHTML = '📜 ' + esc(scene.scenario) +
              (scene.scene ? ' <span class="scene">— ' + esc(scene.scene) + '</span>' : '');
            hasSceneImage = !!scene.image;
            if (scene.image !== currentSceneImage) {
              currentSceneImage = scene.image;
              bg.style.backgroundImage = scene.image ? "url('" + scene.image + "')" : '';
              refreshMedia();
            }
          } catch (e) {}
        }

        async function refreshMedia() {
          try {
            const res = await fetch('/api/media');
            const item = await res.json();
            const stage = document.getElementById('stage');
            const key = item ? item.url : null;
            // Même photo qu'au dernier passage : rien à refaire (évite de la recharger toutes les 3 s).
            if (item && key === currentMediaKey && stage.childElementCount) return;
            currentMediaKey = key;
            stage.innerHTML = '';
            stage.classList.toggle('photo', !!item && item.type === 'image');
            if (!item) {
              if (hasSceneImage) return;
              const empty = document.createElement('div');
              empty.id = 'empty';
              empty.textContent = "En attente d'un envoi du MJ…";
              stage.appendChild(empty);
              return;
            }
            if (item.type === 'image') {
              const img = document.createElement('img');
              img.src = item.url;
              img.alt = item.name;
              stage.appendChild(img);
              applyRotation();
            } else {
              const a = document.createElement('a');
              a.href = item.url;
              a.textContent = item.name;
              a.target = '_blank';
              stage.appendChild(a);
            }
          } catch (e) {}
        }

        // Exploration : carte recouverte de noir, sauf les cases révélées par le MJ.
        let explo = null;
        let exploDrawnKey = null;

        function layoutExplo() {
          const img = document.getElementById('exploImg');
          const box = document.getElementById('exploBox');
          if (!explo || !img.naturalWidth) return;
          const ratio = img.naturalWidth / img.naturalHeight;
          const top = document.getElementById('topbar').offsetHeight;
          // Carte pivotée d'un quart de tour : elle doit tenir dans l'écran une fois tournée.
          const availW = quartTourne() ? window.innerHeight - top : window.innerWidth;
          const availH = quartTourne() ? window.innerWidth : window.innerHeight - top;
          let w = availW, h = w / ratio;
          if (h > availH) { h = availH; w = h * ratio; }
          box.style.width = w + 'px';
          box.style.height = h + 'px';
          box.style.marginTop = top + 'px';
          applyZoom();
          drawFog(true);
          drawLieux();
        }

        // Zoom sur une zone révélée (double appui du MJ) : la zone est centrée et agrandie.
        function applyZoom() {
          const box = document.getElementById('exploBox');
          const z = explo && explo.zoom;
          const W = box.clientWidth, H = box.clientHeight;
          const rot = tableRotation ? 'rotate(' + tableRotation + 'deg) ' : '';
          if (!z || !W || !H) { box.style.transform = rot; return; }
          const top = document.getElementById('topbar').offsetHeight;
          const availW = quartTourne() ? window.innerHeight - top : window.innerWidth;
          const availH = quartTourne() ? window.innerWidth : window.innerHeight - top;
          const s = Math.max(1, Math.min(6, 0.9 * Math.min(availW / (z.w * W), availH / (z.h * H))));
          const dx = (z.x + z.w / 2 - 0.5) * W, dy = (z.y + z.h / 2 - 0.5) * H;
          box.style.transform = rot + 'scale(' + s + ') translate(' + (-dx) + 'px,' + (-dy) + 'px)';
        }

        function drawFog(force) {
          if (!explo) return;
          const box = document.getElementById('exploBox');
          const canvas = document.getElementById('exploFog');
          const w = box.clientWidth, h = box.clientHeight;
          if (!w || !h) return;
          const key = explo.version + ':' + w + 'x' + h;
          if (!force && key === exploDrawnKey) return;
          exploDrawnKey = key;
          const dpr = window.devicePixelRatio || 1;
          canvas.width = Math.round(w * dpr);
          canvas.height = Math.round(h * dpr);
          const ctx = canvas.getContext('2d');
          ctx.clearRect(0, 0, canvas.width, canvas.height);
          ctx.fillStyle = '#000';
          const cw = canvas.width / explo.cols, ch = canvas.height / explo.rows;
          for (let r = 0; r < explo.rows; r++) {
            // Cases masquées consécutives d'une ligne remplies d'un seul rectangle (grilles fines).
            let debut = -1;
            for (let c = 0; c <= explo.cols; c++) {
              const masquee = c < explo.cols && explo.revealed.charAt(r * explo.cols + c) !== '1';
              if (masquee && debut < 0) debut = c;
              if (!masquee && debut >= 0) {
                // Bords arrondis vers l'extérieur : pas de liseré entre deux cases masquées.
                const x0 = Math.floor(debut * cw), y0 = Math.floor(r * ch);
                ctx.fillRect(x0, y0, Math.ceil(c * cw) - x0, Math.ceil((r + 1) * ch) - y0);
                debut = -1;
              }
            }
          }
        }

        // Lieux rendus visibles par le MJ : icône et nom, par-dessus le brouillard.
        function drawLieux() {
          const layer = document.getElementById('exploLieux');
          if (!explo || !explo.lieux) { layer.innerHTML = ''; return; }
          let html = '';
          // Icônes et noms contre-tournés : ils restent lisibles quand la carte pivote.
          const contre = tableRotation ? ';transform:translate(-50%,-17px) rotate(' + (-tableRotation) + 'deg)' : '';
          for (const l of explo.lieux) {
            html += '<div class="lieu" style="left:' + (l.x * 100) + '%;top:' + (l.y * 100) + '%' + contre + '">' +
              '<div class="pin" style="background:' + esc(l.color) + '">' + esc(l.emoji) + '</div>' +
              '<div class="nom">' + esc(l.nom) + '</div></div>';
          }
          layer.innerHTML = html;
        }

        async function refreshExploration() {
          try {
            const res = await fetch('/api/exploration');
            const data = await res.json();
            const layer = document.getElementById('explo');
            if (!data) { explo = null; layer.style.display = 'none'; return; }
            const img = document.getElementById('exploImg');
            const imageChanged = !explo || explo.image !== data.image;
            const stateChanged = !explo || explo.version !== data.version;
            explo = data;
            // Une photo envoyée après la carte passe devant elle.
            layer.style.display = data.masquee ? 'none' : 'flex';
            if (imageChanged) {
              img.onload = layoutExplo;
              img.src = data.image;
            } else if (stateChanged) {
              drawFog(false);
            }
            if (stateChanged) { drawLieux(); applyZoom(); }
          } catch (e) {}
        }

        // Épreuve (outil ÉPREUVES) : image plein écran, réussites en pastilles, dernière complication.
        let epreuveImage = null;
        let epreuveNumero = null;

        async function refreshEpreuve() {
          try {
            const res = await fetch('/api/epreuve');
            const ep = await res.json();
            const layer = document.getElementById('epreuve');
            if (!ep) { layer.style.display = 'none'; epreuveImage = null; epreuveNumero = null; return; }
            layer.style.display = 'block';
            if (ep.image !== epreuveImage) {
              epreuveImage = ep.image;
              document.getElementById('epreuveBg').style.backgroundImage = ep.image ? "url('" + ep.image + "')" : '';
              layer.classList.toggle('sansImage', !ep.image);
            }
            document.getElementById('epreuveNom').textContent = ep.nom;
            const desc = document.getElementById('epreuveDesc');
            desc.textContent = ep.description;
            desc.style.display = ep.description ? 'block' : 'none';
            let pips = '';
            for (let i = 0; i < ep.requises; i++) pips += '<div class="pip' + (i < ep.reussites ? ' on' : '') + '"></div>';
            pips += '<span class="pipsLabel">' + ep.reussites + ' / ' + ep.requises + ' réussite' + (ep.requises > 1 ? 's' : '') + '</span>';
            document.getElementById('epreuvePips').innerHTML = pips;

            const comp = document.getElementById('epreuveComplication');
            if (ep.complication) {
              comp.querySelector('.titre').textContent = '⚠ ' + ep.complication.titre;
              comp.querySelector('.effet').textContent = ep.complication.description;
              comp.style.display = 'block';
              // Nouvelle complication : secousse + halo rouge (pas au premier chargement de la page).
              if (epreuveNumero !== null && ep.complication.numero !== epreuveNumero) {
                comp.classList.remove('shake'); void comp.offsetWidth; comp.classList.add('shake');
                const flash = document.getElementById('epreuveFlash');
                flash.classList.remove('on'); void flash.offsetWidth; flash.classList.add('on');
              }
              epreuveNumero = ep.complication.numero;
            } else {
              comp.style.display = 'none';
              if (epreuveNumero === null) epreuveNumero = 0;
            }

            const issue = document.getElementById('epreuveIssue');
            if (ep.terminee) {
              issue.textContent = ep.reussie ? '✔ Épreuve surmontée !' : 'Épreuve interrompue';
              issue.className = ep.reussie ? 'reussie' : '';
              issue.style.display = 'block';
            } else {
              issue.style.display = 'none';
            }
          } catch (e) {}
        }

        refreshEpreuve();
        setInterval(refreshEpreuve, 1000);

        window.addEventListener('resize', layoutExplo);
        refreshRotation();
        setInterval(refreshRotation, 1000);
        refreshExploration();
        setInterval(refreshExploration, 3000);

        refreshScene();
        setInterval(refreshScene, 3000);
        refreshStatus();
        refreshMedia();
        setInterval(refreshStatus, 3000);
        setInterval(refreshMedia, 3000);
        refreshCombat();
        setInterval(refreshCombat, 1500);
        </script>
        </body>
        </html>
    """.trimIndent()
}
