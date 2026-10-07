package com.eltavine.sereingram.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import java.io.File

/**
 * Includes every SereinGram module: each directory below `serein/` with a
 * `build.gradle.kts` becomes the project of the same path, for example
 * `serein/features/ghost` is `:serein:features:ghost`. `sereinCheck` runs
 * the checks of all of them.
 */
class SereinSettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        // The convention plugins pull in Gradle plugins from Google's repository, such as Room's.
        settings.pluginManagement.repositories {
            gradlePluginPortal()
            google()
            mavenCentral()
        }
        val modules = modulePaths(settings.settingsDir)
        modules.forEach(settings::include)
        settings.gradle.rootProject {
            tasks.register("sereinCheck") {
                group = "verification"
                description = "Runs the checks of every SereinGram module."
                dependsOn(modules.map { "$it:check" })
            }
        }
    }
}

internal fun modulePaths(root: File): List<String> {
    val modules = root.resolve("serein")
    if (!modules.isDirectory) {
        return emptyList()
    }
    return modules.walkTopDown()
        .onEnter { it.name != "build" && it.name != "src" && !it.name.startsWith(".") }
        .filter { it != modules && it.resolve("build.gradle.kts").isFile }
        .map { ":" + it.relativeTo(root).invariantSeparatorsPath.replace('/', ':') }
        .sorted()
        .toList()
}
