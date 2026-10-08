package com.eltavine.sereingram.buildlogic

import com.android.build.api.AndroidPluginVersion
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

/** Fails when the root build applies another Android Gradle Plugin than the one SereinGram's catalog pins. */
internal fun Project.checkAndroidGradlePluginVersion(applied: AndroidPluginVersion) {
    val pinned = libs.findVersion("androidGradlePlugin").get().requiredVersion
    val version = "${applied.major}.${applied.minor}.${applied.micro}"
    if (version != pinned) {
        throw GradleException(
            "The build applies the Android Gradle Plugin $version but gradle/libs.versions.toml pins $pinned; " +
                "update the catalog to the version in the root build.gradle.",
        )
    }
}

/** The app's SDK levels, set in TMessagesProj/build.gradle, which SereinGram's Android modules share. */
internal object AppLevels {
    const val COMPILE_SDK = 36
    const val MIN_SDK = 21
}

/** `:serein:adapters:room` becomes `com.eltavine.sereingram.adapters.room`. */
internal val Project.sereinNamespace: String
    get() = "com.eltavine.sereingram." + path.removePrefix(":serein:").replace(':', '.').replace('-', '_')
