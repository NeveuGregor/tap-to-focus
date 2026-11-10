/**
 * This file is part of tap-to-focus.
 *
 * tap-to-focus - Bloque des applis choisies jusqu'au scan d'un tag NFC
 *
 * @author    Neveu Grégor <contact.neveugregor@proton.me>
 * @copyright 2026 Neveu Grégor
 * @license   CeCILL-B Free Software License Agreement
 *
 * Governed by the CeCILL-B license under French law — http://www.cecill.info
 */

package com.neveugregor.taptofocus.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neveugregor.taptofocus.R

// Palette « Ambre », rampe « Ink » sombre. Thème sombre uniquement pour l'instant.
val Accent = Color(0xFFF2A33A)
private val Fond = Color(0xFF0B0D14)
val Carte = Color(0xFF11141C)
val Releve = Color(0xFF1A1E28)
val TextePrimaire = Color(0xFFEEF0F5)
val TexteSecondaire = Color(0xFFA3A9B8)
val TexteDiscret = Color(0xFF7A8296)

// Polices embarquées (pas de téléchargement réseau) ; licences dans licences-polices/
private val Plex = FontFamily(
    Font(R.font.plex_sans_regular, FontWeight.Normal),
    Font(R.font.plex_sans_medium, FontWeight.Medium),
)
val Mono = FontFamily(Font(R.font.jetbrains_mono_regular, FontWeight.Normal))

// Contrôles (boutons, champs) : rayon 10, pas de pilule
val FormeControle = RoundedCornerShape(10.dp)

// Toutes les couleurs sont fixées : sinon Material 3 remplit les trous avec son violet par défaut
private val Couleurs = darkColorScheme(
    primary = Accent,
    onPrimary = Fond,
    primaryContainer = Releve,
    onPrimaryContainer = TextePrimaire,
    secondary = TexteSecondaire,
    onSecondary = Fond,
    secondaryContainer = Releve,
    onSecondaryContainer = TextePrimaire,
    tertiary = TexteSecondaire,
    onTertiary = Fond,
    tertiaryContainer = Releve,
    onTertiaryContainer = TextePrimaire,
    background = Fond,
    onBackground = TextePrimaire,
    surface = Fond,
    onSurface = TextePrimaire,
    surfaceVariant = Releve,
    onSurfaceVariant = TexteSecondaire,
    surfaceTint = Color.Transparent,
    surfaceBright = Releve,
    surfaceDim = Fond,
    surfaceContainerLowest = Fond,
    surfaceContainerLow = Carte,
    surfaceContainer = Carte,
    surfaceContainerHigh = Releve,
    surfaceContainerHighest = Releve,
    inverseSurface = TextePrimaire,
    inverseOnSurface = Fond,
    inversePrimary = Color(0xFFA35A00),
    // Bordures : texte primaire à 16 % (contrôles) et 8 % (séparateurs)
    outline = TextePrimaire.copy(alpha = 0.16f),
    outlineVariant = TextePrimaire.copy(alpha = 0.08f),
    error = Color(0xFFE5484D),
    onError = TextePrimaire,
    scrim = Color.Black,
)

// Échelle Material conservée, police Plex partout, graisses 400 et 500 seulement
private val Typo = Typography().run {
    fun TextStyle.plex() = copy(fontFamily = Plex)
    copy(
        displayLarge = displayLarge.plex(), displayMedium = displayMedium.plex(), displaySmall = displaySmall.plex(),
        headlineLarge = headlineLarge.plex(), headlineMedium = headlineMedium.plex(), headlineSmall = headlineSmall.plex(),
        titleLarge = titleLarge.plex(), titleMedium = titleMedium.plex(), titleSmall = titleSmall.plex(),
        bodyLarge = bodyLarge.plex(), bodyMedium = bodyMedium.plex(), bodySmall = bodySmall.plex(),
        labelLarge = labelLarge.plex(), labelMedium = labelMedium.plex(), labelSmall = labelSmall.plex(),
    )
}

@Composable
fun TapToFocusTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Couleurs, typography = Typo, content = content)
}
