package com.jc2.jdrcompagnon.feature_boutique.data.mapper

import com.jc2.jdrcompagnon.feature_boutique.data.local.ArticleEnVenteEntity
import com.jc2.jdrcompagnon.feature_boutique.data.local.BoutiqueAvecDetails
import com.jc2.jdrcompagnon.feature_boutique.data.local.BoutiqueEntity
import com.jc2.jdrcompagnon.feature_boutique.data.local.EmployeEntity
import com.jc2.jdrcompagnon.feature_boutique.data.local.ServiceEntity
import com.jc2.jdrcompagnon.feature_boutique.domain.model.ArticleEnVente
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Boutique
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Employe
import com.jc2.jdrcompagnon.feature_boutique.domain.model.EquipementReference
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Marchand
import com.jc2.jdrcompagnon.feature_boutique.domain.model.RoleEmploye
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Service
import com.jc2.jdrcompagnon.feature_boutique.domain.model.StandingBoutique
import com.jc2.jdrcompagnon.feature_boutique.domain.model.TypeBoutique

fun BoutiqueAvecDetails.toDomain(): Boutique = Boutique(
    id = boutique.id,
    nom = boutique.nom,
    standing = StandingBoutique.valueOf(boutique.standing),
    marchand = Marchand(nom = boutique.nomMarchand, description = boutique.descriptionMarchand, trait = boutique.traitMarchand),
    employes = employes.map { it.toDomain() },
    inventaire = articles.map { it.toDomain() },
    type = TypeBoutique.valueOf(boutique.type),
    services = services.map { it.toDomain() },
    argentDisponibleEnPo = boutique.argentDisponibleEnPo,
    villeId = boutique.villeId
)

fun Boutique.toEntity(): BoutiqueEntity = BoutiqueEntity(
    id = id,
    nom = nom,
    standing = standing.name,
    nomMarchand = marchand.nom,
    descriptionMarchand = marchand.description,
    type = type.name,
    argentDisponibleEnPo = argentDisponibleEnPo,
    traitMarchand = marchand.trait,
    villeId = villeId
)

fun ArticleEnVenteEntity.toDomain(): ArticleEnVente = ArticleEnVente(
    equipement = EquipementReference(nom = equipementNom, coutBaseEnPo = coutBaseEnPo, type = equipementType),
    prixApplique = prixApplique,
    quantiteStock = quantiteStock,
    toujoursDisponible = toujoursDisponible
)

fun ArticleEnVente.toEntity(boutiqueId: String): ArticleEnVenteEntity = ArticleEnVenteEntity(
    boutiqueId = boutiqueId,
    equipementNom = equipement.nom,
    equipementType = equipement.type,
    coutBaseEnPo = equipement.coutBaseEnPo,
    prixApplique = prixApplique,
    quantiteStock = quantiteStock,
    toujoursDisponible = toujoursDisponible
)

fun EmployeEntity.toDomain(): Employe = Employe(nom = nom, role = RoleEmploye.valueOf(role), trait = trait)

fun Employe.toEntity(boutiqueId: String): EmployeEntity = EmployeEntity(
    boutiqueId = boutiqueId,
    nom = nom,
    role = role.name,
    trait = trait
)

fun ServiceEntity.toDomain(): Service = Service(
    nom = nom,
    description = description,
    prixEnPo = prixEnPo,
    quantiteDisponible = quantiteDisponible,
    actif = actif,
    personnalise = personnalise
)

fun Service.toEntity(boutiqueId: String): ServiceEntity = ServiceEntity(
    boutiqueId = boutiqueId,
    nom = nom,
    description = description,
    prixEnPo = prixEnPo,
    quantiteDisponible = quantiteDisponible,
    actif = actif,
    personnalise = personnalise
)
