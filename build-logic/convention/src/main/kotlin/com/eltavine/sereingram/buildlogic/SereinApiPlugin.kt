package com.eltavine.sereingram.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * A module whose public API others build on: upstream code calls the hooks,
 * and features and adapters build on the core and the ports. Its API is kept
 * in `api/`, committed, and `check` fails when the code no longer matches it,
 * so every change to the API is made on purpose with `apiDump` and reviewed.
 */
class SereinApiPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("org.jetbrains.kotlinx.binary-compatibility-validator")
    }
}
