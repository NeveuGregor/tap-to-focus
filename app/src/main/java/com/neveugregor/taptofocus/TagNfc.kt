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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import androidx.core.content.IntentCompat

// Type MIME propre à l'appli : doit correspondre au filtre NDEF_DISCOVERED du manifeste
const val TYPE_MIME = "application/vnd.neveugregor.taptofocus"

/** UID du tag en hexadécimal, octets séparés par « : ». */
fun Tag.uid(): String = id.joinToString(":") { "%02X".format(it) }

/**
 * Écrit sur le tag : un enregistrement au type MIME de l'appli, puis un AAR.
 * Le MIME en premier déclenche NDEF_DISCOVERED ; l'AAR garantit que c'est
 * cette appli qui s'ouvre, même si une autre déclarait le même type.
 * Bloquant (entrées/sorties NFC) : à appeler hors du thread principal.
 */
fun Tag.ecrireNdef(context: Context) {
    val message = NdefMessage(
        NdefRecord.createMime(TYPE_MIME, "tap-to-focus".toByteArray()),
        NdefRecord.createApplicationRecord(context.packageName),
    )
    val ndef = Ndef.get(this)
    if (ndef != null) {
        ndef.use {
            it.connect()
            check(it.isWritable) { context.getString(R.string.tag_read_only) }
            it.writeNdefMessage(message)
        }
    } else {
        // Tag jamais formaté en NDEF : on le formate en y écrivant le message
        val formatable = NdefFormatable.get(this) ?: error(context.getString(R.string.tag_not_ndef))
        formatable.use {
            it.connect()
            it.format(message)
        }
    }
}

/** Tag NFC transporté par l'intent (scan), ou null. */
fun Intent.tag(): Tag? = IntentCompat.getParcelableExtra(this, NfcAdapter.EXTRA_TAG, Tag::class.java)

/**
 * Mode lecteur : tant que l'activité est au premier plan, tous les tags lui arrivent dans
 * [recu], sur le thread principal, sans le bip système (c'est l'appli qui joue son son).
 * Appli fermée, le scan passe encore par NDEF_DISCOVERED : là, le bip système reste.
 * À appeler dans onResume.
 */
fun Activity.activerReceptionTags(recu: (Tag) -> Unit) {
    val adapter = NfcAdapter.getDefaultAdapter(this) ?: return
    // Pas de FLAG_READER_SKIP_NDEF_CHECK : sans lui, Ndef.get() marche encore pour l'association
    val types = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
        NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or
        NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
    // Le rappel arrive sur un thread du système : on repasse sur le thread principal
    adapter.enableReaderMode(this, { tag -> runOnUiThread { recu(tag) } }, types, null)
}

/** À appeler dans onPause. */
fun Activity.desactiverReceptionTags() {
    NfcAdapter.getDefaultAdapter(this)?.disableReaderMode(this)
}
