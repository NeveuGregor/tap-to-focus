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

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Surveille l'appli au premier plan et affiche l'écran de blocage quand une appli
 * de la liste noire passe devant, en mode travail et hors déblocage temporaire.
 * Logs : adb logcat -s TapToFocus
 */
class BlocageService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private val verification = Runnable { verifier() }

    // Copie en mémoire des réglages, tenue à jour depuis DataStore
    private var etat: Preferences = emptyPreferences()

    // Dernière appli réellement au premier plan (hors volet, clavier, écran de blocage)
    private var premierPlan: String? = null

    // Jamais bloqués, même s'ils sont cochés : sinon plus aucune issue
    private lateinit var jamaisBloques: Set<String>

    override fun onServiceConnected() {
        super.onServiceConnected()
        jamaisBloques = setOfNotNull(
            packageName,
            // Réglages : seule sortie possible tant que le mode travail n'a pas de fin
            "com.android.settings",
            // Lanceur par défaut : le bloquer ferait boucler Retour → accueil → blocage
            packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
                ?.activityInfo?.packageName,
        )
        Log.d(TAG, "Service connecté, jamais bloqués : $jamaisBloques")

        scope.launch {
            reglages.data.collect {
                etat = it
                programmerFinDeblocage()
                verifier()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Filtré par le XML de config, mais on revérifie : d'autres types peuvent arriver
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val paquet = event.packageName?.toString() ?: return
        val classe = event.className?.toString()
        Log.d(TAG, "Premier plan : $paquet ($classe)")

        // Fenêtres posées par-dessus l'appli courante sans la remplacer : on garde l'appli d'en dessous,
        // pour que la vérification de fin de déblocage vise la bonne appli
        if (paquet == "com.android.systemui" ||
            classe == "android.inputmethodservice.SoftInputWindow" ||
            classe == BlockActivity::class.java.name
        ) return

        premierPlan = paquet
        verifier()
    }

    /** Affiche l'écran de blocage si l'appli au premier plan doit l'être maintenant. */
    private fun verifier() {
        val paquet = premierPlan ?: return
        if (!doitBloquer(paquet)) return
        Log.d(TAG, "Blocage de $paquet")
        // NEW_TASK obligatoire depuis un service ; singleTask absorbe les demandes en double
        startActivity(Intent(this, BlockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun doitBloquer(paquet: String): Boolean =
        etat.verrouille(System.currentTimeMillis()) &&
            paquet in (etat[CLE_APPLIS_BLOQUEES] ?: emptySet()) &&
            paquet !in jamaisBloques

    /**
     * Revérifie à la fin du déblocage, même si l'appli bloquée reste ouverte sans changer de fenêtre.
     * shortcut: postDelayed ne compte pas le temps de veille profonde ; écran allumé ça tient,
     * écran éteint le blocage revient au prochain changement de fenêtre. AlarmManager si besoin.
     */
    private fun programmerFinDeblocage() {
        handler.removeCallbacks(verification)
        val reste = (etat[CLE_FIN_DEBLOCAGE] ?: 0L) - System.currentTimeMillis()
        // Petite marge : on revérifie juste après l'heure de fin, pas juste avant
        if (reste > 0) handler.postDelayed(verification, reste + 500)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacks(verification)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val TAG = "TapToFocus"

        /** Vrai si l'utilisateur a activé le service dans les réglages d'accessibilité. */
        fun estActif(context: Context): Boolean {
            val moi = ComponentName(context, BlocageService::class.java)
            val actifs = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                ?: return false
            // Liste « paquet/classe » séparée par « : », classe parfois abrégée en « .Classe »
            return actifs.split(':').any { ComponentName.unflattenFromString(it) == moi }
        }
    }
}
