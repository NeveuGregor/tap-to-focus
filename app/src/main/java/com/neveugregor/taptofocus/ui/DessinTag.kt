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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Un tag NTAG rond vu de dessus : disque, spires de l'antenne, puce au centre.
 * - [allume] : spires ambre (applis bloquées), sinon éteintes ;
 * - [absent] : simple contour pointillé (aucun tag associé) ;
 * - [restant] : part restante du déblocage (0..1), dessinée en anneau autour du disque.
 */
@Composable
fun DessinTag(allume: Boolean, absent: Boolean, restant: Float?, modifier: Modifier = Modifier) {
    val eteint = TextePrimaire.copy(alpha = 0.14f)
    val spire by animateColorAsState(if (allume) Accent else eteint, tween(400), label = "spire")

    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2 * 0.86f
        val trait = 1.5f * density

        if (absent) {
            drawCircle(
                eteint, r, c,
                style = Stroke(trait, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6 * density, 6 * density))),
            )
            return@Canvas
        }

        // Le sticker
        drawCircle(Carte, r, c)
        drawCircle(TextePrimaire.copy(alpha = 0.08f), r, c, style = Stroke(trait))

        // L'antenne : 6 spires concentriques, plus marquées vers l'extérieur
        val spires = 6
        for (i in 0 until spires) {
            val ri = r * (0.58f + 0.06f * i)
            drawCircle(spire.copy(alpha = spire.alpha * (0.45f + 0.55f * i / (spires - 1))), ri, c, style = Stroke(trait))
        }
        // Pont de l'antenne vers la puce
        drawLine(spire, Offset(c.x, c.y - r * 0.58f), Offset(c.x, c.y - r * 0.16f), trait, StrokeCap.Round)

        // La puce
        val puce = r * 0.26f
        drawRoundRect(
            Releve, Offset(c.x - puce / 2, c.y - puce / 2), Size(puce, puce), CornerRadius(3 * density),
        )
        drawRoundRect(
            spire, Offset(c.x - puce / 2, c.y - puce / 2), Size(puce, puce), CornerRadius(3 * density),
            style = Stroke(trait),
        )

        // Le minuteur du déblocage : un anneau qui se vide dans le sens horaire
        if (restant != null) {
            val ra = size.minDimension / 2 * 0.97f
            drawCircle(eteint, ra, c, style = Stroke(3 * density))
            drawArc(
                Accent, -90f, 360f * restant.coerceIn(0f, 1f), false,
                Offset(c.x - ra, c.y - ra), Size(ra * 2, ra * 2),
                style = Stroke(3 * density, cap = StrokeCap.Round),
            )
        }
    }
}
