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
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

// Fichier de préférences unique de l'appli, partagé par les activités et le service
val Context.reglages by preferencesDataStore(name = "reglages")

// Ancien format, un seul tag (« 04:A1:… ») : lu par tags() puis effacé à la première modification
val CLE_TAG_UID = stringPreferencesKey("tag_uid")

// Packages des applis à bloquer en mode travail
val CLE_APPLIS_BLOQUEES = stringSetPreferencesKey("applis_bloquees")

// Mode travail actif (blocage) ; absent = mode libre
val CLE_MODE_TRAVAIL = booleanPreferencesKey("mode_travail")

// Fin du déblocage temporaire, en millisecondes depuis l'epoch
val CLE_FIN_DEBLOCAGE = longPreferencesKey("fin_deblocage")

// Durée d'un déblocage, en minutes
val CLE_DUREE_MINUTES = intPreferencesKey("duree_minutes")
const val DUREE_PAR_DEFAUT = 5

// UID des tags associés (plusieurs : bureau, porte-clés, chez quelqu'un…)
val CLE_TAGS = stringSetPreferencesKey("tags")

/** Tags associés ; repli sur l'ancien tag unique tant que la liste n'a jamais été écrite. */
fun Preferences.tags(): Set<String> = this[CLE_TAGS] ?: setOfNotNull(this[CLE_TAG_UID])

// Durées proposées en un appui ; toute autre se saisit, dans la limite de DUREE_MAX
val DUREES_RAPIDES = listOf(1, 5, 15)
const val DUREE_MAX = 240

/** Minutes saisies, ou null si vide, non numérique ou hors 1..DUREE_MAX. */
fun dureeSaisie(texte: String): Int? = texte.toIntOrNull()?.takeIf { it in 1..DUREE_MAX }

// Médias choisis sur le téléphone (URI content:// à permission persistante) ; absent = aucun
val CLE_SON_SCAN = stringPreferencesKey("son_scan")
val CLE_SON_BLOCAGE = stringPreferencesKey("son_blocage")
val CLE_IMAGE_BLOCAGE = stringPreferencesKey("image_blocage")
