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

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "tap-to-focus"
include(":app")
