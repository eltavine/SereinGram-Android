package com.eltavine.sereingram.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import java.io.File

/**
 * Includes every SereinGram module: each directory below `serein/` with a
 * `build.gradle.kts` becomes the project of the same path, for example
 * `serein/features/ghost` is `:serein:features:ghost`.
 */
class SereinSettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        modulePaths(settings.settingsDir).forEach(settings::include)
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
