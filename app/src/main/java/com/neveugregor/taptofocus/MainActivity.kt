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

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.lifecycleScope
import com.neveugregor.taptofocus.ui.Accent
import com.neveugregor.taptofocus.ui.DessinTag
import com.neveugregor.taptofocus.ui.FormeControle
import com.neveugregor.taptofocus.ui.Mono
import com.neveugregor.taptofocus.ui.TexteDiscret
import com.neveugregor.taptofocus.ui.TexteSecondaire
import com.neveugregor.taptofocus.ui.TapToFocusTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Écran d'accueil. Reçoit les tags de deux façons :
 * - appli ouverte : mode lecteur, tous les tags arrivent dans traiterTag, sans bip système ;
 * - appli fermée : un tag écrit par l'appli (MIME + AAR) la lance via NDEF_DISCOVERED.
 */
class MainActivity : ComponentActivity() {

    // États observés par Compose : l'écran se redessine quand ils changent
    private var nfcActif by mutableStateOf(false)
    private var serviceActif by mutableStateOf(false)
    private var associationEnCours by mutableStateOf(false)
    private var message by mutableStateOf<String?>(null)
    private var choixApplisOuvert by mutableStateOf(false)
    private var parametresOuvert by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barres système sombres quel que soit le thème du téléphone
        enableEdgeToEdge(SystemBarStyle.dark(AndroidColor.TRANSPARENT), SystemBarStyle.dark(AndroidColor.TRANSPARENT))
        // null si le téléphone n'a pas de puce NFC
        val nfcPresent = NfcAdapter.getDefaultAdapter(this) != null

        // Lancée par un scan depuis l'écran d'accueil ; savedInstanceState non null = simple rotation, déjà traité
        if (savedInstanceState == null) intent.tag()?.let(::traiterTag)

        setContent {
            TapToFocusTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val prefs = reglages.data.collectAsState(initial = emptyPreferences()).value
                    val finDeblocage = prefs[CLE_FIN_DEBLOCAGE] ?: 0L
                    // Horloge à la seconde, seulement pendant un déblocage : compte à rebours et reverrouillage
                    val maintenant by produceState(System.currentTimeMillis(), finDeblocage) {
                        while (value < finDeblocage) {
                            delay(1000)
                            value = System.currentTimeMillis()
                        }
                    }
                    val verrouille = prefs.verrouille(maintenant)
                    // Fin du déblocage : on referme ce qui modifie les réglages
                    LaunchedEffect(verrouille) {
                        if (verrouille) {
                            choixApplisOuvert = false
                            associationEnCours = false
                        }
                    }
                    if (choixApplisOuvert && !verrouille) {
                        BackHandler { choixApplisOuvert = false }
                        ChoixApplis(
                            selection = prefs[CLE_APPLIS_BLOQUEES] ?: emptySet(),
                            basculer = ::basculerAppli,
                            retour = { choixApplisOuvert = false },
                        )
                        return@Surface
                    }
                    if (parametresOuvert) {
                        BackHandler { parametresOuvert = false }
                        Parametres(prefs, verrouille, retour = { parametresOuvert = false })
                        return@Surface
                    }
                    Accueil(
                        prefs = prefs,
                        maintenant = maintenant,
                        nfcPresent = nfcPresent,
                        nfcActif = nfcActif,
                        serviceActif = serviceActif,
                        associationEnCours = associationEnCours,
                        message = message,
                        ouvrirAccessibilite = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                        ouvrirReglagesNfc = { startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                        ouvrirChoixApplis = { choixApplisOuvert = true },
                        ouvrirParametres = { parametresOuvert = true },
                        basculerAssociation = {
                            associationEnCours = !associationEnCours
                            message = null
                        },
                        choisirDuree = ::choisirDuree,
                        retirerTag = ::oublierTag,
                        quitterModeTravail = ::quitter,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Relus à chaque retour, notamment après un passage par les réglages
        serviceActif = BlocageService.estActif(this)
        nfcActif = NfcAdapter.getDefaultAdapter(this)?.isEnabled == true
        activerReceptionTags(::traiterTag)
    }

    override fun onPause() {
        super.onPause()
        desactiverReceptionTags()
    }

    // Android 17 / One UI 9 : dans onResume, le système ne nous voit pas encore au premier plan
    // et refuse en silence (« Caller not in foreground »). Au focus de la fenêtre, c'est sûr.
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) activerReceptionTags(::traiterTag)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.tag()?.let(::traiterTag)
    }

    // Ajoute ou retire une appli de la liste noire, enregistré aussitôt
    private fun basculerAppli(packageName: String) {
        lifecycleScope.launch {
            reglages.edit {
                if (it.verrouille(System.currentTimeMillis())) return@edit
                val actuelles = it[CLE_APPLIS_BLOQUEES] ?: emptySet()
                it[CLE_APPLIS_BLOQUEES] = if (packageName in actuelles) actuelles - packageName else actuelles + packageName
            }
        }
    }

    private fun choisirDuree(minutes: Int) {
        if (minutes !in 1..DUREE_MAX) return
        lifecycleScope.launch {
            reglages.edit { if (!it.verrouille(System.currentTimeMillis())) it[CLE_DUREE_MINUTES] = minutes }
        }
    }

    private fun traiterTag(tag: Tag) {
        val uid = tag.uid()
        lifecycleScope.launch {
            if (associationEnCours) {
                try {
                    withContext(Dispatchers.IO) { tag.ecrireNdef(packageName) }
                    // Mémorisé seulement si l'écriture a réussi : sinon le tag ne lancerait pas l'appli
                    var ajoute = false
                    reglages.edit { ajoute = ajouterTag(it, uid, System.currentTimeMillis()) }
                    associationEnCours = false
                    if (ajoute) jouer(CLE_SON_SCAN)
                    message = if (ajoute) "Tag associé." else "Réglages verrouillés : scanne d'abord un de tes tags."
                } catch (e: Exception) {
                    // Tag retiré trop tôt, verrouillé, non NDEF… on reste en association pour réessayer
                    message = "Écriture impossible (${e.message ?: e.javaClass.simpleName}). Garde le tag contre le téléphone et réessaie."
                }
            } else {
                message = scannerTag(uid).texte()
            }
        }
    }

    private fun oublierTag(uid: String) {
        lifecycleScope.launch { reglages.edit { retirerTag(it, uid, System.currentTimeMillis()) } }
    }

    private fun quitter() {
        lifecycleScope.launch {
            var fait = false
            reglages.edit { fait = quitterModeTravail(it, System.currentTimeMillis()) }
            message = if (fait) "Mode travail désactivé." else null
        }
    }
}

@Composable
private fun Accueil(
    prefs: Preferences,
    maintenant: Long,
    nfcPresent: Boolean,
    nfcActif: Boolean,
    serviceActif: Boolean,
    associationEnCours: Boolean,
    message: String?,
    ouvrirAccessibilite: () -> Unit,
    ouvrirReglagesNfc: () -> Unit,
    ouvrirChoixApplis: () -> Unit,
    ouvrirParametres: () -> Unit,
    basculerAssociation: () -> Unit,
    choisirDuree: (Int) -> Unit,
    retirerTag: (String) -> Unit,
    quitterModeTravail: () -> Unit,
) {
    val modeTravail = prefs[CLE_MODE_TRAVAIL] == true
    val finDeblocage = prefs[CLE_FIN_DEBLOCAGE] ?: 0L
    val nbBloquees = prefs[CLE_APPLIS_BLOQUEES]?.size ?: 0
    val duree = prefs[CLE_DUREE_MINUTES] ?: DUREE_PAR_DEFAUT
    val tags = prefs.tags()
    val verrouille = prefs.verrouille(maintenant)
    var saisieDuree by remember { mutableStateOf(false) }
    val resteMs = if (modeTravail) finDeblocage - maintenant else 0L
    val ouvert = resteMs > 0
    val applis = if (nbBloquees == 1) "1 appli" else "$nbBloquees applis"

    // Ce que dit l'écran : une phrase d'état et une consigne, en mots simples
    val (etat, consigne) = when {
        tags.isEmpty() -> "Aucun tag associé" to "Associe un tag NFC pour commencer"
        nbBloquees == 0 -> "Aucune appli choisie" to "Choisis les applis à bloquer"
        ouvert -> "Ouvert encore ${compteARebours(resteMs)}" to "Ensuite, $applis se rebloquent"
        modeTravail -> "${applis.replaceFirstChar { it.uppercase() }} bloquée${if (nbBloquees > 1) "s" else ""}" to
            "Scanne ton tag pour les ouvrir $duree min"
        else -> "Rien n'est bloqué" to "Scanne ton tag pour bloquer $applis"
    }

    // Hauteur mini = l'écran : le tag occupe le haut, les réglages restent sous le pouce
    BoxWithConstraints(Modifier.safeDrawingPadding()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                // Signature : chevron de prompt dans l'accent, collé au nom ; roue crantée à droite
                // +12 : l'icône s'aligne sur la marge malgré la marge interne du bouton
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(">", color = Accent, style = MaterialTheme.typography.titleMedium)
                    Text("tap-to-focus", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = ouvrirParametres, modifier = Modifier.offset(x = 12.dp)) {
                        Icon(painterResource(R.drawable.ic_reglages), contentDescription = "Réglages", tint = TexteSecondaire)
                    }
                }

                // Sans le service d'accessibilité, aucun blocage n'est possible : on le dit avant tout le reste
                if (!serviceActif) {
                    Column(
                        modifier = Modifier.padding(top = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Le blocage est en pause : le service d'accessibilité est désactivé", color = MaterialTheme.colorScheme.error)
                        Text(
                            "Accessibilité → Applications installées → Tap to Focus. " +
                                "Interrupteur grisé : Infos de l'appli → ⋮ → Autoriser les paramètres restreints.",
                            color = TexteSecondaire,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(onClick = ouvrirAccessibilite, shape = FormeControle) { Text("Activer le service") }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DessinTag(
                        allume = modeTravail && !ouvert && nbBloquees > 0,
                        absent = tags.isEmpty(),
                        restant = if (ouvert) resteMs / (duree * 60_000f) else null,
                        modifier = Modifier.size(232.dp).padding(bottom = 24.dp),
                    )
                    Text(etat, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    when {
                        !nfcPresent -> Text("Ce téléphone n'a pas de NFC", color = MaterialTheme.colorScheme.error)
                        !nfcActif -> {
                            Text("Le NFC est désactivé", color = TexteSecondaire)
                            Button(onClick = ouvrirReglagesNfc, shape = FormeControle) { Text("Activer le NFC") }
                        }
                        associationEnCours -> Text(
                            "Approche le tag et garde-le contre le téléphone",
                            color = Accent,
                            textAlign = TextAlign.Center,
                        )
                        else -> Text(consigne, color = TexteSecondaire, textAlign = TextAlign.Center)
                    }
                    message?.let { Text(it, color = TexteDiscret, style = MaterialTheme.typography.bodyMedium) }
                    // Le déblocage prouve qu'on a un tag en main : c'est le seul moment où l'on peut sortir
                    if (modeTravail && ouvert) {
                        OutlinedButton(onClick = quitterModeTravail, shape = FormeControle, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Quitter le mode travail")
                        }
                    }
                    if (tags.isEmpty() && !associationEnCours && nfcActif) {
                        Button(onClick = basculerAssociation, shape = FormeControle, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Associer un tag")
                        }
                    }
                }
            }

            // Les réglages, séparés par des filets ; figés en mode travail hors déblocage
            Column(Modifier.padding(top = 32.dp)) {
                if (verrouille) {
                    Text(
                        "Réglages verrouillés jusqu'au prochain scan",
                        color = TexteDiscret,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                LigneReglage("Applis bloquées", onClick = if (verrouille) null else ouvrirChoixApplis) {
                    Text("$nbBloquees", fontFamily = Mono, color = TexteSecondaire)
                    if (!verrouille) Icon(painterResource(R.drawable.ic_suite), contentDescription = null, tint = TexteDiscret)
                }
                // Quatre puces ne tiennent pas à côté du libellé : elles passent dessous
                LigneReglage("Déblocage") {}
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    DUREES_RAPIDES.forEach { minutes ->
                        FilterChip(
                            selected = minutes == duree,
                            onClick = { choisirDuree(minutes) },
                            enabled = !verrouille,
                            label = { Text("$minutes min") },
                        )
                    }
                    val libre = duree !in DUREES_RAPIDES
                    FilterChip(
                        selected = libre,
                        onClick = { saisieDuree = true },
                        enabled = !verrouille,
                        label = { Text(if (libre) "$duree min" else "Autre") },
                    )
                }
                if (saisieDuree && !verrouille) {
                    SaisieDuree(
                        actuelle = duree,
                        valider = {
                            choisirDuree(it)
                            saisieDuree = false
                        },
                        annuler = { saisieDuree = false },
                    )
                }
                LigneReglage("Tags", onClick = if (nfcActif && !verrouille) basculerAssociation else null) {
                    Text(
                        when {
                            associationEnCours -> "Annuler"
                            verrouille -> "${tags.size}"
                            else -> "Ajouter"
                        },
                        fontFamily = if (verrouille) Mono else null,
                        color = if (associationEnCours) Accent else TexteSecondaire,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                // Un tag par ligne ; seul le bouton retire, pour qu'un effleurement de la ligne ne coûte rien
                tags.sorted().forEach { uid ->
                    LigneReglage(uid, police = Mono) {
                        if (!verrouille) TextButton(onClick = { retirerTag(uid) }) { Text("Retirer", color = TexteDiscret) }
                    }
                }
            }
        }
    }
}

/** « 0:42 », « 4:05 » */
private fun compteARebours(ms: Long): String {
    val s = (ms + 999) / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

/** Une ligne de réglage : libellé à gauche, valeur ou contrôle à droite, filet au-dessus. */
@Composable
fun LigneReglage(
    libelle: String,
    onClick: (() -> Unit)? = null,
    police: FontFamily? = null,
    valeur: @Composable RowScope.() -> Unit,
) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Text(libelle, fontFamily = police, maxLines = 1, modifier = Modifier.weight(1f))
        valeur()
    }
}

/** Saisie d'une durée libre, en minutes ; Valider reste grisé tant qu'elle sort de 1..DUREE_MAX. */
@Composable
private fun SaisieDuree(actuelle: Int, valider: (Int) -> Unit, annuler: () -> Unit) {
    var texte by remember { mutableStateOf("$actuelle") }
    val minutes = dureeSaisie(texte)
    AlertDialog(
        onDismissRequest = annuler,
        title = { Text("Durée du déblocage") },
        text = {
            OutlinedTextField(
                value = texte,
                onValueChange = { texte = it.filter(Char::isDigit).take(3) },
                suffix = { Text("min") },
                supportingText = { Text("De 1 à $DUREE_MAX min") },
                isError = minutes == null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = FormeControle,
            )
        },
        confirmButton = {
            TextButton(onClick = { minutes?.let(valider) }, enabled = minutes != null) { Text("Valider") }
        },
        dismissButton = { TextButton(onClick = annuler) { Text("Annuler") } },
    )
}
