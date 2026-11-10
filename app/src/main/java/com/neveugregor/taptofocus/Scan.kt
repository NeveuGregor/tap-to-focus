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

package com.neveugregor.taptofocus

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Ce qu'a provoqué un scan du tag (hors association). */
sealed interface ResultatScan {
    data object NonReconnu : ResultatScan
    data object ModeTravail : ResultatScan
    data class Debloque(val fin: Long) : ResultatScan
}

/**
 * Règle du mode bascule, sans Android : testée dans ScanTest.
 * - tag inconnu (absent de la liste) : rien ne change ;
 * - mode libre : passage en mode travail ;
 * - mode travail : déblocage temporaire (un nouveau scan le prolonge).
 */
fun appliquerScan(prefs: MutablePreferences, uid: String, maintenant: Long): ResultatScan {
    if (uid !in prefs.tags()) return ResultatScan.NonReconnu
    if (prefs[CLE_MODE_TRAVAIL] != true) {
        prefs[CLE_MODE_TRAVAIL] = true
        prefs.remove(CLE_FIN_DEBLOCAGE)
        return ResultatScan.ModeTravail
    }
    val fin = maintenant + (prefs[CLE_DUREE_MINUTES] ?: DUREE_PAR_DEFAUT) * 60_000L
    prefs[CLE_FIN_DEBLOCAGE] = fin
    return ResultatScan.Debloque(fin)
}

/** Applique le scan et l'enregistre ; le service est prévenu par le flux DataStore. */
suspend fun Context.scannerTag(uid: String): ResultatScan {
    var resultat: ResultatScan = ResultatScan.NonReconnu
    reglages.edit { resultat = appliquerScan(it, uid, System.currentTimeMillis()) }
    if (resultat != ResultatScan.NonReconnu) jouer(CLE_SON_SCAN)
    return resultat
}

fun ResultatScan.texte(): String = when (this) {
    ResultatScan.NonReconnu -> "Tag non reconnu."
    ResultatScan.ModeTravail -> "Mode travail activé."
    is ResultatScan.Debloque -> "Débloqué jusqu'à ${heure(fin)}."
}

/** « 14:05 » dans le fuseau du téléphone. */
fun heure(epochMillis: Long): String =
    DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

/** Réglages figés : mode travail hors déblocage. Seul un scan de tag les rouvre. */
fun Preferences.verrouille(maintenant: Long): Boolean =
    this[CLE_MODE_TRAVAIL] == true && maintenant >= (this[CLE_FIN_DEBLOCAGE] ?: 0L)

/** Ajoute un tag, sauf réglages verrouillés (sinon n'importe quel sticker vierge débloquerait). */
fun ajouterTag(prefs: MutablePreferences, uid: String, maintenant: Long): Boolean {
    if (prefs.verrouille(maintenant)) return false
    prefs[CLE_TAGS] = prefs.tags() + uid
    prefs.remove(CLE_TAG_UID)
    return true
}

/** Retire un tag, sauf réglages verrouillés. Plus aucun tag : retour en mode libre, sinon blocage sans clé. */
fun retirerTag(prefs: MutablePreferences, uid: String, maintenant: Long): Boolean {
    if (prefs.verrouille(maintenant)) return false
    val reste = prefs.tags() - uid
    prefs[CLE_TAGS] = reste
    prefs.remove(CLE_TAG_UID)
    if (reste.isEmpty()) {
        prefs.remove(CLE_MODE_TRAVAIL)
        prefs.remove(CLE_FIN_DEBLOCAGE)
    }
    return true
}

/** Sortie du mode travail : seulement pendant un déblocage, donc juste après un scan valide. */
fun quitterModeTravail(prefs: MutablePreferences, maintenant: Long): Boolean {
    if (prefs[CLE_MODE_TRAVAIL] != true || prefs.verrouille(maintenant)) return false
    prefs.remove(CLE_MODE_TRAVAIL)
    prefs.remove(CLE_FIN_DEBLOCAGE)
    return true
}
