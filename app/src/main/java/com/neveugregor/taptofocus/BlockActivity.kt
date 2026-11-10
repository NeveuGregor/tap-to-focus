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
import android.graphics.Color as AndroidColor
import android.nfc.Tag
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.neveugregor.taptofocus.ui.Accent
import com.neveugregor.taptofocus.ui.DessinTag
import com.neveugregor.taptofocus.ui.TexteSecondaire
import com.neveugregor.taptofocus.ui.TapToFocusTheme
import kotlinx.coroutines.launch

/**
 * Écran de blocage, lancé par BlocageService par-dessus une appli bloquée.
 * Retour = écran d'accueil, jamais l'appli bloquée.
 * Le scan du tag se fait ici même : déblocage puis retour à l'appli d'en dessous.
 */
class BlockActivity : ComponentActivity() {

    // « Tag non reconnu », etc.
    private var message by mutableStateOf<String?>(null)

    // Le service relance l'écran à chaque événement de fenêtre : un seul rire à la fois
    private var dernierRire = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barres système sombres quel que soit le thème du téléphone
        enableEdgeToEdge(SystemBarStyle.dark(AndroidColor.TRANSPARENT), SystemBarStyle.dark(AndroidColor.TRANSPARENT))
        // savedInstanceState non null = simple rotation, pas une nouvelle apparition
        if (savedInstanceState == null) rire()
        setContent {
            BackHandler { allerAccueil() }
            TapToFocusTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val prefs = reglages.data.collectAsState(initial = null).value
                    EcranBlocage(message, prefs?.get(CLE_IMAGE_BLOCAGE))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
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
        // Sans tag : le service nous ramène devant une appli bloquée
        intent.tag()?.let(::traiterTag) ?: rire()
    }

    private fun traiterTag(tag: Tag) {
        lifecycleScope.launch {
            val resultat = scannerTag(tag.uid())
            // Débloqué : on s'efface, l'appli bloquée réapparaît en dessous
            if (resultat is ResultatScan.Debloque) finish() else message = resultat.texte(this@BlockActivity)
        }
    }

    private fun allerAccueil() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }

    private fun rire() {
        val maintenant = System.currentTimeMillis()
        if (maintenant - dernierRire < DUREE_RIRE_MS) return
        dernierRire = maintenant
        lifecycleScope.launch { jouer(CLE_SON_BLOCAGE) }
    }
}

// Anti-rafale : pas deux sons de blocage à moins de 2,5 s d'intervalle
private const val DUREE_RIRE_MS = 2_500L

@Composable
private fun EcranBlocage(message: String?, image: String?) {
    val context = LocalContext.current
    // Image choisie dans les réglages ; aucune, ou illisible : le dessin du tag allumé
    val dessin = remember(image) { image?.let { decoderImage(context, it) } }
    Column(
        modifier = Modifier.safeDrawingPadding().padding(24.dp).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // weight(fill = false) : l'image rétrécit si l'écran manque de hauteur
        if (dessin != null) {
            val ratio = dessin.intrinsicWidth.coerceAtLeast(1).toFloat() / dessin.intrinsicHeight.coerceAtLeast(1)
            ImageBlocage(dessin, Modifier.weight(1f, fill = false).aspectRatio(ratio))
        } else {
            DessinTag(allume = true, absent = false, restant = null, modifier = Modifier.size(232.dp))
        }
        Text(stringResource(R.string.scan_to_unlock), color = Accent, style = MaterialTheme.typography.titleMedium)
        message?.let { Text(it, color = TexteSecondaire) }
    }
}

/** Image fixe ou animée (GIF, WebP…) via ImageDecoder, sans bibliothèque ; null si illisible. */
private fun decoderImage(context: Context, uri: String): Drawable? = try {
    ImageDecoder.decodeDrawable(ImageDecoder.createSource(context.contentResolver, Uri.parse(uri))).apply {
        // Sinon une animation s'arrête après le nombre de boucles inscrit dans le fichier
        if (this is AnimatedImageDrawable) repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
    }
} catch (e: Exception) {
    // Fichier supprimé, permission perdue, format inconnu
    null
}

@Composable
private fun ImageBlocage(dessin: Drawable, modifier: Modifier) {
    DisposableEffect(dessin) {
        (dessin as? AnimatedImageDrawable)?.start()
        onDispose { (dessin as? AnimatedImageDrawable)?.stop() }
    }
    AndroidView(
        factory = {
            ImageView(it).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                // L'image est un détail visuel : le texte porte le message
                importantForAccessibility = ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        },
        // update : l'image peut changer sans que la vue soit recréée
        update = { it.setImageDrawable(dessin) },
        modifier = modifier,
    )
}
