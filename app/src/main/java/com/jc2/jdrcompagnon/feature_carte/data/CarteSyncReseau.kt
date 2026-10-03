package com.jc2.jdrcompagnon.feature_carte.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.jc2.jdrcompagnon.di.CarteDependencies
import com.jc2.jdrcompagnon.feature_carte.domain.model.CarteCampagne
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Boutique
import com.jc2.jdrcompagnon.feature_carte.domain.model.LieuNotable
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret
import com.jc2.jdrcompagnon.network.LieuJoueurData
import com.jc2.jdrcompagnon.network.VilleImageData
import com.jc2.jdrcompagnon.feature_carte.domain.model.TypePointInteret
import com.jc2.jdrcompagnon.network.BoutiqueJoueurData
import com.jc2.jdrcompagnon.network.ServiceJoueurData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.jc2.jdrcompagnon.network.CampagneJoueurData
import com.jc2.jdrcompagnon.network.CarteImageData
import com.jc2.jdrcompagnon.network.CarteJoueurData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Partage des cartes de campagne avec les joueurs connectés.
 *
 * Côté MJ : [etatCampagne] ne garde que les lieux révélés (PointInteret.visibleJoueurs) ; les
 * images de fond partent à part ([imageEncodee], réduites à 2048 px) et seulement quand elles
 * changent. Côté joueur : [appliquer] / [appliquerImage] recopient tout dans la base locale, pour
 * que l'écran de carte et la liste des villes fonctionnent comme hors réseau (en lecture seule).
 */
object CarteSyncReseau {

    /** Taille (dp) d'une case sans image de fond : même valeur que TAILLE_CASE_REFERENCE de l'écran. */
    private const val TAILLE_CASE_REFERENCE_DP = 36f
    private const val TAILLE_MAX_IMAGE_PX = 2048
    private const val TAILLE_MAX_IMAGE_VILLE_PX = 1280

    // ── Côté MJ ──

    fun signatureImage(context: Context, carte: CarteCampagne): String? =
        carte.imageFileName?.let { CarteImageStore.fichier(context, it) }?.takeIf { it.exists() }
            ?.let { "${it.length()}-${it.lastModified()}" }

    /** Cases sur la largeur de la carte telle que le MJ la voit (voir CarteGrillePrefs.casesEnLargeur). */
    fun casesEnLargeur(context: Context, carte: CarteCampagne): Float {
        val density = context.resources.displayMetrics.density
        val largeurImagePx = carte.imageFileName?.let { CarteImageStore.fichier(context, it) }?.takeIf { it.exists() }?.let { f ->
            BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { BitmapFactory.decodeFile(f.absolutePath, it) }.outWidth
        }?.takeIf { it > 0 }
        val largeurDp = largeurImagePx?.let { it / density } ?: (TAILLE_CASE_REFERENCE_DP * carte.largeurCases)
        return largeurDp / CarteGrillePrefs.tailleCaseDp(context, carte.id).coerceAtLeast(1)
    }

    /**
     * Boutiques des villes de [villes] (liées par Boutique.villeId ou PointInteret.boutiqueIds), vues
     * par les joueurs : services actifs seulement, sans stock ni caisse. Sert à la diffusion réseau
     * comme à l'affichage local des services du lieu (menu joueur).
     */
    fun boutiquesJoueur(villes: List<PointInteret>, boutiques: List<Boutique>): List<BoutiqueJoueurData> =
        villes.filter { it.type == TypePointInteret.VILLE }.flatMap { ville ->
            boutiques.filter { it.villeId == ville.id || it.id in ville.boutiqueIds }.map { b ->
                BoutiqueJoueurData(
                    id = b.id,
                    villeId = ville.id,
                    nom = b.nom,
                    type = b.type.label,
                    marchand = b.marchand.nom,
                    services = b.services.filter { it.actif }.map { s ->
                        ServiceJoueurData(s.nom, s.description, s.prixEnPo, s.quantiteDisponible)
                    },
                )
            }
        }.distinctBy { it.id }

    fun etatCampagne(
        context: Context,
        id: String,
        titre: String,
        cartes: List<CarteCampagne>,
        points: List<PointInteret>,
        boutiques: List<Boutique> = emptyList(),
        lieux: List<LieuNotable> = emptyList(),
    ) =
        CampagneJoueurData(
            id = id,
            titre = titre,
            cartes = cartes.map { c ->
                CarteJoueurData(
                    id = c.id,
                    nom = c.nom,
                    largeurCases = c.largeurCases,
                    hauteurCases = c.hauteurCases,
                    echelleKmParCase = c.echelleKmParCase,
                    casesEnLargeur = casesEnLargeur(context, c),
                    imageSignature = signatureImage(context, c),
                )
            },
            // Lieux cachés par le MJ : jamais transmis. Boutiques et scénarios liés restent côté MJ.
            points = points.filter { it.visibleJoueurs }.map { it.copy(boutiqueIds = emptyList(), scenarioIds = emptyList()) },
            // Les services, eux, sont partagés : les joueurs y accèdent depuis le lieu où se trouve le groupe.
            boutiques = boutiquesJoueur(points.filter { it.visibleJoueurs }, boutiques),
            lieux = lieux.filter { l -> points.any { it.id == l.villeId && it.visibleJoueurs } }
                .map { LieuJoueurData(it.id, it.villeId, it.nom, it.description) },
            // Images des villes révélées : envoyées à part (imageVilleEncodee), quand la signature change.
            imagesVilles = points.filter { it.visibleJoueurs && it.type == TypePointInteret.VILLE }
                .mapNotNull { v -> VilleImageStore.signature(context, v.id)?.let { v.id to it } }
                .toMap(),
        )

    fun imageEncodee(context: Context, carte: CarteCampagne): CarteImageData? {
        val signature = signatureImage(context, carte) ?: return null
        val fichier = CarteImageStore.fichier(context, carte.imageFileName ?: return null)
        return encoderJpeg(fichier, TAILLE_MAX_IMAGE_PX)?.let { CarteImageData(carte.id, signature, it) }
    }

    fun imageVilleEncodee(context: Context, villeId: String): VilleImageData? {
        val signature = VilleImageStore.signature(context, villeId) ?: return null
        return encoderJpeg(VilleImageStore.fichier(context, villeId), TAILLE_MAX_IMAGE_VILLE_PX)?.let { VilleImageData(villeId, signature, it) }
    }

    /** Image réduite à [tailleMax] px (plus grand côté), en JPEG encodé base64 ; null en cas d'échec. */
    private fun encoderJpeg(fichier: File, tailleMax: Int): String? = runCatching {
        val bornes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(fichier.absolutePath, bornes)
        var echantillon = 1
        while (maxOf(bornes.outWidth, bornes.outHeight) / echantillon > tailleMax) echantillon *= 2
        val bitmap = BitmapFactory.decodeFile(fichier.absolutePath, BitmapFactory.Options().apply { inSampleSize = echantillon })
            ?: return null
        val sortie = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, sortie)
        Base64.encodeToString(sortie.toByteArray(), Base64.NO_WRAP)
    }.getOrNull()

    // ── Côté joueur ──

    private val verrou = Mutex()
    private var derniereCampagneId: String? = null

    private val _boutiquesReseau = MutableStateFlow<List<BoutiqueJoueurData>>(emptyList())

    /** Boutiques (et services) des villes révélées, reçues du MJ ; vide hors partie réseau. */
    val boutiquesReseau: StateFlow<List<BoutiqueJoueurData>> = _boutiquesReseau.asStateFlow()

    fun oublierBoutiquesReseau() {
        _boutiquesReseau.value = emptyList()
    }

    /** Nom local de l'image reçue : change avec l'image, pour que l'écran la recharge. */
    private fun nomImage(carteId: String, signature: String) = "net_${carteId}_${signature.hashCode().toUInt()}.img"

    suspend fun appliquer(context: Context, data: CampagneJoueurData) = verrou.withLock {
        derniereCampagneId = data.id
        _boutiquesReseau.value = data.boutiques
        val repo = CarteDependencies.repository
        repo.observerCartes(data.id).first()
            .filter { locale -> data.cartes.none { it.id == locale.id } }
            .forEach { repo.supprimerCarte(it) }
        data.cartes.forEach { c ->
            CarteGrillePrefs.setCasesEnLargeur(context, c.id, c.casesEnLargeur)
            val image = c.imageSignature?.let { nomImage(c.id, it) }?.takeIf { CarteImageStore.fichier(context, it).exists() }
            repo.sauvegarderCarte(
                CarteCampagne(
                    campagneId = data.id,
                    largeurCases = c.largeurCases,
                    hauteurCases = c.hauteurCases,
                    echelleKmParCase = c.echelleKmParCase,
                    imageFileName = image,
                    id = c.id,
                    nom = c.nom,
                )
            )
        }
        // Un lieu que le MJ a caché (ou supprimé) disparaît aussi chez le joueur.
        repo.observerPoints(data.id).first()
            .filter { local -> data.points.none { it.id == local.id } }
            .forEach { repo.supprimerPoint(it.id) }
        // Points d'abord : les réenregistrer efface leurs lieux notables (cascade), remis ensuite.
        data.points.forEach { repo.sauvegarderPoint(it) }
        repo.observerLieuxNotablesCampagne(data.id).first()
            .filter { local -> data.lieux.none { it.id == local.id } }
            .forEach { repo.supprimerLieuNotable(it.id) }
        data.lieux.forEach { repo.sauvegarderLieuNotable(LieuNotable(it.id, it.villeId, it.nom, it.description)) }
        // Image retirée par le MJ (ou ville cachée) : retirée aussi chez le joueur.
        data.points.filter { it.type == TypePointInteret.VILLE && it.id !in data.imagesVilles }
            .forEach { VilleImageStore.supprimer(context, it.id) }
    }

    /** Image d'une ville reçue du MJ : remplace celle de l'appareil. */
    fun appliquerImageVille(context: Context, image: VilleImageData) {
        runCatching { VilleImageStore.ecrire(context, image.villeId, Base64.decode(image.base64, Base64.NO_WRAP)) }
    }

    suspend fun appliquerImage(context: Context, image: CarteImageData) = verrou.withLock {
        val nom = nomImage(image.carteId, image.signature)
        runCatching {
            CarteImageStore.fichier(context, nom).writeBytes(Base64.decode(image.base64, Base64.NO_WRAP))
        }.onFailure { return@withLock }
        // Anciennes versions de cette image : inutiles.
        CarteImageStore.fichier(context, nom).parentFile?.listFiles()
            ?.filter { it.name.startsWith("net_${image.carteId}_") && it.name != nom }
            ?.forEach(File::delete)
        val campagneId = derniereCampagneId ?: return@withLock
        val repo = CarteDependencies.repository
        repo.observerCartes(campagneId).first().firstOrNull { it.id == image.carteId }?.let { carte ->
            repo.sauvegarderCarte(carte.copy(imageFileName = nom))
        }
    }
}
