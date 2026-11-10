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

// Versions alignées sur Android Studio Otter 2025.2.1 (AGP 8.13 maximum)
plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
    // Doit avoir exactement la même version que Kotlin
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" apply false
}
