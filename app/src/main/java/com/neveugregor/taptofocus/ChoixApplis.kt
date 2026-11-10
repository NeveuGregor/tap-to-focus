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
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.neveugregor.taptofocus.ui.FormeControle
import com.neveugregor.taptofocus.ui.Mono
import com.neveugregor.taptofocus.ui.TexteDiscret
import com.neveugregor.taptofocus.ui.TexteSecondaire
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Appli(val packageName: String, val nom: String, val icone: ImageBitmap)

/**
 * Applis visibles dans le tiroir d'applis, triées par nom.
 * Visibilité permise par le <queries> MAIN/LAUNCHER du manifeste (pas besoin de QUERY_ALL_PACKAGES).
 * Lent (chargement des icônes) : à appeler hors du thread principal.
 */
fun chargerApplis(context: Context): List<Appli> {
    val pm = context.packageManager
    val lanceur = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(lanceur, 0)
        .distinctBy { it.activityInfo.packageName }
        // On ne propose pas de bloquer tap-to-focus lui-même
        .filter { it.activityInfo.packageName != context.packageName }
        .map {
            Appli(
                packageName = it.activityInfo.packageName,
                nom = it.loadLabel(pm).toString(),
                icone = it.loadIcon(pm).toBitmap(96, 96).asImageBitmap(),
            )
        }
        .sortedBy { it.nom.lowercase() }
}

@Composable
fun ChoixApplis(
    selection: Set<String>,
    basculer: (String) -> Unit,
    retour: () -> Unit,
) {
    val context = LocalContext.current
    // null tant que la liste n'est pas chargée
    val applis by produceState<List<Appli>?>(null) {
        value = withContext(Dispatchers.IO) { chargerApplis(context) }
    }
    var recherche by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.safeDrawingPadding().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Décalage de -12 : la flèche s'aligne sur les icônes de la liste malgré la marge interne du bouton
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.offset(x = (-12).dp),
        ) {
            IconButton(onClick = retour) {
                Icon(painterResource(R.drawable.ic_retour), contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.choose_apps), style = MaterialTheme.typography.titleLarge)
        }
        OutlinedTextField(
            value = recherche,
            onValueChange = { recherche = it },
            placeholder = { Text(stringResource(R.string.search), color = TexteDiscret) },
            singleLine = true,
            shape = FormeControle,
            modifier = Modifier.fillMaxWidth(),
        )

        val liste = applis
        if (liste == null) {
            Text(stringResource(R.string.loading_apps), color = TexteSecondaire)
        } else {
            val filtrees = liste.filter { it.nom.contains(recherche, ignoreCase = true) }
            // Deux sections : les applis déjà bloquées en haut, les autres en dessous
            val (bloquees, autres) = filtrees.partition { it.packageName in selection }
            LazyColumn {
                if (bloquees.isNotEmpty()) {
                    item(key = "entete_bloquees") { Entete(stringResource(R.string.blocked_header), bloquees.size) }
                    items(bloquees, key = { it.packageName }) { appli ->
                        LigneAppli(appli, cochee = true, basculer, Modifier.animateItem())
                    }
                }
                item(key = "entete_autres") { Entete(stringResource(R.string.other_apps), autres.size) }
                items(autres, key = { it.packageName }) { appli ->
                    LigneAppli(appli, cochee = false, basculer, Modifier.animateItem())
                }
            }
        }
    }
}

// En-tête de section : libellé discret et compte en mono, plus près de ses lignes que de la section du dessus
@Composable
private fun Entete(titre: String, nombre: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
    ) {
        Text(titre, color = TexteSecondaire, style = MaterialTheme.typography.labelLarge)
        Text("$nombre", color = TexteDiscret, fontFamily = Mono, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun LigneAppli(appli: Appli, cochee: Boolean, basculer: (String) -> Unit, modifier: Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { basculer(appli.packageName) }
            .padding(vertical = 8.dp),
    ) {
        // Le nom à côté suffit aux lecteurs d'écran
        Image(appli.icone, contentDescription = null, modifier = Modifier.size(40.dp))
        Text(appli.nom, modifier = Modifier.weight(1f))
        // onCheckedChange null : c'est la ligne entière qui est cliquable
        Checkbox(checked = cochee, onCheckedChange = null)
    }
}
