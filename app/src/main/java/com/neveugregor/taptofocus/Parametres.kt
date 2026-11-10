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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.neveugregor.taptofocus.ui.Mono
import com.neveugregor.taptofocus.ui.TexteDiscret
import com.neveugregor.taptofocus.ui.TexteSecondaire
import kotlinx.coroutines.launch

const val CONTACT = "contact.neveugregor@proton.me"
const val DEPOT = "https://github.com/NeveuGregor/tap-to-focus"

private const val TEXTE_CONFIDENTIALITE =
    "Tap to Focus ne collecte et n'envoie aucune donnée : l'appli n'a pas accès à Internet. " +
        "Ses réglages (tags, applis bloquées, sons et image choisis) restent sur ce téléphone.\n\n" +
        "Le service d'accessibilité sert uniquement à savoir quelle appli passe au premier plan, " +
        "pour afficher l'écran de blocage. Il ne lit pas le contenu des écrans.\n\n" +
        "Logiciel libre sous licence CeCILL-B, fourni tel quel, sans garantie. " +
        "Les sons et images que tu ajoutes restent sous ta responsabilité."

private const val TEXTE_CREDITS =
    "Code : © 2026 Neveu Grégor, licence CeCILL-B.\n\n" +
        "Polices : IBM Plex Sans (© IBM Corp.) et JetBrains Mono (© JetBrains), " +
        "SIL Open Font License 1.1.\n\n" +
        "Icônes : Lucide (lucide.dev), licence ISC.\n\n" +
        "Sons et images : aucun n'est fourni, ceux que tu choisis restent sur ton téléphone."

/**
 * Écran ouvert par la roue crantée : médias (figés comme les autres réglages en mode travail)
 * et « à propos », toujours lisible.
 */
@Composable
fun Parametres(prefs: Preferences, verrouille: Boolean, retour: () -> Unit) {
    val context = LocalContext.current
    val portee = rememberCoroutineScope()
    var texteOuvert by remember { mutableStateOf<Pair<String, String>?>(null) }

    fun enregistrer(cle: Preferences.Key<String>, uri: Uri?) {
        portee.launch {
            context.reglages.edit {
                if (it.verrouille(System.currentTimeMillis())) return@edit
                if (uri == null) it.remove(cle) else it[cle] = uri.toString()
            }
        }
    }

    // Sans permission persistante, l'URI devient illisible au redémarrage du téléphone
    @Composable
    fun selecteur(cle: Preferences.Key<String>) = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        // shortcut: l'ancienne permission n'est pas rendue (plafond système de 512), à libérer si on dépasse quelques médias
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        enregistrer(cle, uri)
    }
    val sonScan = selecteur(CLE_SON_SCAN)
    val sonBlocage = selecteur(CLE_SON_BLOCAGE)
    val image = selecteur(CLE_IMAGE_BLOCAGE)

    @Composable
    fun LigneMedia(libelle: String, cle: Preferences.Key<String>, vide: String = "Aucun", choisir: () -> Unit) {
        val uri = prefs[cle]
        val nom = remember(uri) { uri?.let { nomFichier(context, it) } }
        LigneReglage(libelle, onClick = if (verrouille) null else choisir) {
            Text(
                nom ?: vide,
                color = TexteSecondaire,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
            if (nom != null && !verrouille) TextButton(onClick = { enregistrer(cle, null) }) { Text("Retirer", color = TexteDiscret) }
        }
    }

    Column(
        modifier = Modifier
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        // Même en-tête que le choix des applis ; -12 : la flèche s'aligne malgré la marge interne du bouton
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.offset(x = (-12).dp),
        ) {
            IconButton(onClick = retour) {
                Icon(painterResource(R.drawable.ic_retour), contentDescription = "Retour")
            }
            Text("Réglages", style = MaterialTheme.typography.titleLarge)
        }

        Section("Sons et image")
        if (verrouille) {
            Text(
                "Verrouillés jusqu'au prochain scan",
                color = TexteDiscret,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        LigneMedia("Son au scan", CLE_SON_SCAN) { sonScan.launch(arrayOf("audio/*")) }
        LigneMedia("Son de blocage", CLE_SON_BLOCAGE) { sonBlocage.launch(arrayOf("audio/*")) }
        LigneMedia("Image de blocage", CLE_IMAGE_BLOCAGE, "Aucune") { image.launch(arrayOf("image/*")) }

        Section("À propos")
        LigneReglage("Version") {
            Text(version(context), fontFamily = Mono, color = TexteSecondaire)
        }
        LigneReglage("Confidentialité et conditions", onClick = { texteOuvert = "Confidentialité et conditions" to TEXTE_CONFIDENTIALITE }) {}
        LigneReglage("Contact", onClick = { ouvrir(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$CONTACT"))) }) {
            Text("Écrire", color = TexteSecondaire, style = MaterialTheme.typography.bodyMedium)
        }
        LigneReglage("Code source", onClick = { ouvrir(context, Intent(Intent.ACTION_VIEW, Uri.parse(DEPOT))) }) {
            Text("GitHub", color = TexteSecondaire, style = MaterialTheme.typography.bodyMedium)
        }
        LigneReglage("Crédits et licences", onClick = { texteOuvert = "Crédits et licences" to TEXTE_CREDITS }) {}
    }

    texteOuvert?.let { (titre, texte) ->
        AlertDialog(
            onDismissRequest = { texteOuvert = null },
            title = { Text(titre) },
            text = { Text(texte, modifier = Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { texteOuvert = null }) { Text("Fermer") } },
        )
    }
}

// Titre de section, discret, comme les en-têtes du choix des applis
@Composable
private fun Section(titre: String) {
    Text(
        titre,
        color = TexteSecondaire,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 32.dp, bottom = 8.dp),
    )
}

/** Nom affichable d'un fichier choisi ; null s'il n'est plus lisible (supprimé, permission perdue). */
private fun nomFichier(context: Context, uri: String): String? = try {
    context.contentResolver.query(Uri.parse(uri), arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { if (it.moveToFirst()) it.getString(0) else null }
} catch (e: Exception) {
    null
}

private fun version(context: Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"

// Pas d'appli mail ou de navigateur : on ne plante pas pour autant
private fun ouvrir(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Rien à faire : l'adresse et le lien restent affichés
    }
}
