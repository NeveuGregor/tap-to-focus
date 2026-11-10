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

import androidx.datastore.preferences.core.mutablePreferencesOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanTest {

    private val uid = "04:A1:B2:C3:D4:E5:F6"
    private val maintenant = 1_000_000L

    @Test
    fun `tag inconnu ne change rien`() {
        val prefs = mutablePreferencesOf(CLE_TAGS to setOf(uid))
        assertEquals(ResultatScan.NonReconnu, appliquerScan(prefs, "04:00", maintenant))
        assertNull(prefs[CLE_MODE_TRAVAIL])
    }

    @Test
    fun `sans tag associe tout scan est inconnu`() {
        assertEquals(ResultatScan.NonReconnu, appliquerScan(mutablePreferencesOf(), uid, maintenant))
    }

    @Test
    fun `mode libre passe en mode travail sans deblocage`() {
        val prefs = mutablePreferencesOf(CLE_TAGS to setOf(uid), CLE_FIN_DEBLOCAGE to maintenant + 1)
        assertEquals(ResultatScan.ModeTravail, appliquerScan(prefs, uid, maintenant))
        assertTrue(prefs[CLE_MODE_TRAVAIL] == true)
        assertNull(prefs[CLE_FIN_DEBLOCAGE])
    }

    @Test
    fun `mode travail debloque pour la duree choisie`() {
        val prefs = mutablePreferencesOf(CLE_TAGS to setOf(uid), CLE_MODE_TRAVAIL to true, CLE_DUREE_MINUTES to 1)
        val attendu = maintenant + 60_000L
        assertEquals(ResultatScan.Debloque(attendu), appliquerScan(prefs, uid, maintenant))
        assertEquals(attendu, prefs[CLE_FIN_DEBLOCAGE])
        // Le mode travail reste actif : le blocage reviendra à l'heure de fin
        assertTrue(prefs[CLE_MODE_TRAVAIL] == true)
    }

    @Test
    fun `duree par defaut cinq minutes`() {
        val prefs = mutablePreferencesOf(CLE_TAGS to setOf(uid), CLE_MODE_TRAVAIL to true)
        assertEquals(ResultatScan.Debloque(maintenant + 5 * 60_000L), appliquerScan(prefs, uid, maintenant))
    }

    @Test
    fun `ancien tag unique toujours reconnu`() {
        val prefs = mutablePreferencesOf(CLE_TAG_UID to uid)
        assertEquals(ResultatScan.ModeTravail, appliquerScan(prefs, uid, maintenant))
    }

    @Test
    fun `n importe quel tag de la liste fonctionne`() {
        val prefs = mutablePreferencesOf(CLE_TAGS to setOf(uid, "04:BB"))
        assertEquals(ResultatScan.ModeTravail, appliquerScan(prefs, "04:BB", maintenant))
    }

    @Test
    fun `verrouille seulement en mode travail hors deblocage`() {
        assertFalse(mutablePreferencesOf().verrouille(maintenant))
        assertTrue(mutablePreferencesOf(CLE_MODE_TRAVAIL to true).verrouille(maintenant))
        assertFalse(mutablePreferencesOf(CLE_MODE_TRAVAIL to true, CLE_FIN_DEBLOCAGE to maintenant + 1).verrouille(maintenant))
        assertTrue(mutablePreferencesOf(CLE_MODE_TRAVAIL to true, CLE_FIN_DEBLOCAGE to maintenant).verrouille(maintenant))
    }

    @Test
    fun `ajout de tag migre l ancien format`() {
        val prefs = mutablePreferencesOf(CLE_TAG_UID to uid)
        assertTrue(ajouterTag(prefs, "04:BB", maintenant))
        assertEquals(setOf(uid, "04:BB"), prefs[CLE_TAGS])
        assertNull(prefs[CLE_TAG_UID])
    }

    @Test
    fun `ajout et retrait refuses quand verrouille`() {
        val prefs = mutablePreferencesOf(CLE_TAGS to setOf(uid), CLE_MODE_TRAVAIL to true)
        assertFalse(ajouterTag(prefs, "04:BB", maintenant))
        assertFalse(retirerTag(prefs, uid, maintenant))
        assertEquals(setOf(uid), prefs[CLE_TAGS])
    }

    @Test
    fun `retirer le dernier tag repasse en mode libre`() {
        val prefs = mutablePreferencesOf(CLE_TAGS to setOf(uid), CLE_MODE_TRAVAIL to true, CLE_FIN_DEBLOCAGE to maintenant + 1)
        assertTrue(retirerTag(prefs, uid, maintenant))
        assertEquals(emptySet<String>(), prefs.tags())
        assertNull(prefs[CLE_MODE_TRAVAIL])
    }

    @Test
    fun `quitter le mode travail seulement pendant un deblocage`() {
        val bloque = mutablePreferencesOf(CLE_MODE_TRAVAIL to true)
        assertFalse(quitterModeTravail(bloque, maintenant))
        assertTrue(bloque[CLE_MODE_TRAVAIL] == true)

        val debloque = mutablePreferencesOf(CLE_MODE_TRAVAIL to true, CLE_FIN_DEBLOCAGE to maintenant + 1)
        assertTrue(quitterModeTravail(debloque, maintenant))
        assertNull(debloque[CLE_MODE_TRAVAIL])
        assertNull(debloque[CLE_FIN_DEBLOCAGE])
    }

    @Test
    fun `duree saisie bornee de 1 a DUREE_MAX`() {
        assertEquals(30, dureeSaisie("30"))
        assertEquals(DUREE_MAX, dureeSaisie("$DUREE_MAX"))
        assertNull(dureeSaisie(""))
        assertNull(dureeSaisie("0"))
        assertNull(dureeSaisie("${DUREE_MAX + 1}"))
    }
}
