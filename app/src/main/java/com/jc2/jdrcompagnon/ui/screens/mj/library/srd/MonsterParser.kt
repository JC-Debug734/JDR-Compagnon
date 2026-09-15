package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

/**
 * Parser pour le fichier `monster_srd521.md` (SRD 5.2.1 FR), format structuré
 * "base de données" (comme [EquipmentParser]/[SpellParser]).
 *
 * Chaque monstre est un bloc de la forme :
 *
 *   ### Nom du monstre
 *   Catégorie: Dragons rouges
 *   Type: Dragon (Chromatique)
 *   Taille: TG
 *   Alignement: Chaotique Mauvais
 *   CA: 19 Initiative +12 (22)
 *   Pv: 256 (19d12 + 133)
 *   Vitesse: 12 m, escalade 12 m, vol 24 m
 *   Caractéristiques: For 27 +8 +8 Dex 10 +0 +6 Con 25 +7 +7 / Int 16 +3 +3 Sag 13 +1 +7 Cha 23 +6 +6
 *   Compétences: Discrétion +6, Perception +13        (optionnel)
 *   Vulnérabilités: ...                                (optionnel)
 *   Résistances: ...                                   (optionnel)
 *   Immunités: ...                                     (optionnel)
 *   Équipement: ...                                     (optionnel)
 *   Sens: Vision aveugle 18 m, Vision dans le noir 36 m ; Perception passive 23
 *   Langues: commun, draconique
 *   FP: 17 (18 000 PX, ou 20 000 dans son antre ; BM +6)
 *
 *   ## Traits
 *   Nom du trait. Description...
 *
 *   ## Actions
 *   ...
 *
 * [SrdEntry.category] contient le groupe thématique du monstre ("Dragons rouges",
 * "Gobelins", "Non classé"...), comme l'ancien format à sections `### Catégorie`.
 * [SrdEntry.rawMarkdown] commence par un résumé en gras du profil (dont une ligne
 * **FP :** exploitée par [monsterChallenge]/[monsterChallengeLabel] pour le filtre par
 * niveau de défi), suivi des sections Traits/Actions/Actions Bonus/Réactions/Actions
 * Légendaires telles qu'extraites du SRD.
 *
 * Le fichier contient 318 profils (monstres et PNJ génériques : ce SRD n'a pas de
 * chapitre PNJ séparé, ils sont mêlés aux monstres comme dans le livre officiel).
 */
object MonsterParser {

    private val fieldLineRegex = Regex("""^([\p{L}][\p{L} '’-]*):\s*(.*)$""")

    /**
     * Parse le contenu markdown du fichier en une liste d'entrées SRD, triées par nom.
     */
    fun parse(rawMarkdown: String): List<SrdEntry> {
        val blocks = rawMarkdown.split(Regex("""(?m)^### """)).drop(1)
        val entries = mutableListOf<SrdEntry>()

        for (block in blocks) {
            val lines = block.lines()
            val name = lines.firstOrNull()?.trim().orEmpty()
            if (name.isBlank()) continue

            val fields = mutableMapOf<String, String>()
            var i = 1
            while (i < lines.size) {
                val match = fieldLineRegex.find(lines[i])
                if (match != null) {
                    fields[match.groupValues[1].trim()] = match.groupValues[2].trim()
                    i++
                } else {
                    break
                }
            }
            while (i < lines.size && lines[i].isBlank()) i++
            val description = lines.drop(i).joinToString("\n").trim()

            val categorie = fields["Catégorie"].orEmpty()
            val type = fields["Type"].orEmpty()
            val taille = fields["Taille"].orEmpty()
            val alignement = fields["Alignement"].orEmpty()
            val ca = fields["CA"].orEmpty()
            val pv = fields["Pv"].orEmpty()
            val vitesse = fields["Vitesse"].orEmpty()
            val caracteristiques = fields["Caractéristiques"].orEmpty()
            val competences = fields["Compétences"].orEmpty()
            val vulnerabilites = fields["Vulnérabilités"].orEmpty()
            val resistances = fields["Résistances"].orEmpty()
            val immunites = fields["Immunités"].orEmpty()
            val equipement = fields["Équipement"].orEmpty()
            val sens = fields["Sens"].orEmpty()
            val langues = fields["Langues"].orEmpty()
            val fp = fields["FP"].orEmpty()

            val rawContent = buildString {
                val typeLine = listOfNotNull(
                    type.ifBlank { null },
                    taille.ifBlank { null }?.let { "taille $it" },
                    alignement.ifBlank { null },
                ).joinToString(", ")
                if (typeLine.isNotBlank()) {
                    appendLine("*$typeLine*")
                    appendLine()
                }
                if (ca.isNotBlank()) appendLine("**CA :** $ca")
                if (pv.isNotBlank()) appendLine("**Pv :** $pv")
                if (vitesse.isNotBlank()) appendLine("**Vitesse :** $vitesse")
                if (caracteristiques.isNotBlank()) appendLine("**Caractéristiques :** $caracteristiques")
                if (competences.isNotBlank()) appendLine("**Compétences :** $competences")
                if (vulnerabilites.isNotBlank()) appendLine("**Vulnérabilités :** $vulnerabilites")
                if (resistances.isNotBlank()) appendLine("**Résistances :** $resistances")
                if (immunites.isNotBlank()) appendLine("**Immunités :** $immunites")
                if (equipement.isNotBlank()) appendLine("**Équipement :** $equipement")
                if (sens.isNotBlank()) appendLine("**Sens :** $sens")
                if (langues.isNotBlank()) appendLine("**Langues :** $langues")
                if (fp.isNotBlank()) appendLine("**FP :** $fp")
                if (isNotEmpty()) appendLine()
                append(description)
            }.trim()

            entries.add(
                SrdEntry(
                    name = name,
                    category = categorie,
                    rawMarkdown = rawContent,
                )
            )
        }

        return entries.sortedBy { it.name.lowercase() }
    }
}