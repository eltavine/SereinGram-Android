package com.eltavine.sereingram.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).get()

/** Fails when the root build applies another Kotlin than the one SereinGram's catalog pins. */
internal fun Project.checkKotlinVersion() {
    val pinned = libs.findVersion("kotlin").get().requiredVersion
    val applied = getKotlinPluginVersion()
    if (applied != pinned) {
        throw GradleException(
            "The build applies Kotlin $applied but gradle/libs.versions.toml pins $pinned; " +
                "update the catalog to the version in the root build.gradle.",
        )
    }
}

/** `:serein:adapters:room` becomes `com.eltavine.sereingram.adapters.room`. */
internal val Project.sereinNamespace: String
    get() = "com.eltavine.sereingram." + path.removePrefix(":serein:").replace(':', '.').replace('-', '_')
