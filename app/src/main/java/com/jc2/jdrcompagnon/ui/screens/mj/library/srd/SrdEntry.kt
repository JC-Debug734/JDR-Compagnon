package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

/**
 * Représente une entrée du SRD (System Reference Document).
 * Utilisée pour les monstres, les sorts, et tout contenu markdown indexé.
 */
data class SrdEntry(
    val name: String,
    val rawMarkdown: String,
    val category: String = "",
    val firstLetter: Char = name.firstOrNull()?.uppercaseChar() ?: 'A',
)