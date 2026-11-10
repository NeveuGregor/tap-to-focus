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
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first

/**
 * Joue le son choisi dans les réglages (clé CLE_SON_*), sauf aucun choisi ou téléphone en silencieux
 * ou vibreur (pas de « ah ah ah » en réunion). Fichier supprimé ou permission perdue : silence.
 * Contexte de l'appli : le son continue même si l'écran qui l'a lancé se ferme. Libéré à la fin.
 */
suspend fun Context.jouer(cle: Preferences.Key<String>) {
    val son = reglages.data.first()[cle] ?: return
    val audio = getSystemService(AudioManager::class.java)
    if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
    val attributs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    // create() renvoie null si le fichier est illisible
    MediaPlayer.create(applicationContext, Uri.parse(son), null, attributs, audio.generateAudioSessionId())?.apply {
        setOnCompletionListener { it.release() }
        start()
    }
}
