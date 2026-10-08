package com.eltavine.sereingram.buildlogic

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ResValue
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * SereinGram's settings for the upstream app module. They are applied in
 * `finalizeDsl`, after `TMessagesProj/build.gradle` has run, so the upstream
 * script stays as Nagram ships it. The app also depends on every module
 * under `serein/`.
 */
class SereinApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.withPlugin("com.android.application") {
            target.rootProject.subprojects
                .filter { it.path.startsWith(":serein:") && it.file("build.gradle.kts").isFile }
                .forEach { target.dependencies.add("implementation", it) }
            target.dependencies.add("implementation", target.libs.library("androidx-lifecycle-process"))
            target.dependencies.add("implementation", target.libs.library("okio"))
            target.dependencies.add("testImplementation", target.libs.library("archunit"))
            target.dependencies.add("testImplementation", target.libs.library("ktor-client-mock"))
            val properties = SereinProperties(target)
            target.extensions.configure<ApplicationAndroidComponentsExtension> {
                target.checkAndroidGradlePluginVersion(pluginVersion)
                finalizeDsl { android ->
                    if (android.compileSdk != AppLevels.COMPILE_SDK || android.defaultConfig.minSdk != AppLevels.MIN_SDK) {
                        throw GradleException(
                            "TMessagesProj builds against SDK ${android.compileSdk} for SDK ${android.defaultConfig.minSdk} and up; " +
                                "update AppLevels in build-logic to match, which SereinGram's Android modules use.",
                        )
                    }
                    android.defaultConfig.applicationId = APPLICATION_ID
                    val sha1 = signingCertificateSha1(properties["SEREIN_SIGNING_SHA1"])
                    android.defaultConfig.externalNativeBuild.cmake.arguments +=
                        "-DSEREIN_SIGNING_SHA1=$sha1"
                    // Build type sources take precedence over src/main, which upstream owns.
                    android.buildTypes.forEach { buildType ->
                        val sourceSet = android.sourceSets.getByName(buildType.name)
                        sourceSet.res.srcDir(OVERLAY_RES)
                        sourceSet.manifest.srcFile(OVERLAY_MANIFEST)
                    }
                    val release = android.signingConfigs.findByName("release")
                    properties.keystore()?.let { release?.storeFile = it }
                    val complete = release != null &&
                        !release.storePassword.isNullOrEmpty() &&
                        !release.keyAlias.isNullOrEmpty() &&
                        !release.keyPassword.isNullOrEmpty() &&
                        release.storeFile?.isFile == true
                    if (!complete) {
                        target.logger.warn(
                            "SereinGram: no complete release signing key is configured " +
                                "(KEYSTORE_FILE or KEYSTORE_BASE64, KEYSTORE_PASS, ALIAS_NAME, ALIAS_PASS), " +
                                "signing with the debug key.",
                        )
                        val debug = android.signingConfigs.getByName("debug")
                        android.buildTypes.forEach { it.signingConfig = debug }
                    }
                }
                // Account type and intent targets in res/xml must follow the final id.
                onVariants { variant ->
                    variant.resValues.put(
                        variant.makeResValueKey("string", "serein_application_id"),
                        variant.applicationId.map { ResValue(it) },
                    )
                }
            }
        }
    }

    private companion object {
        const val APPLICATION_ID = "com.eltavine.sereingram"
        const val OVERLAY_RES = "src/serein/res"
        const val OVERLAY_MANIFEST = "src/serein/AndroidManifest.xml"
    }
}

internal fun signingCertificateSha1(value: String?): String {
    if (value.isNullOrBlank()) {
        return ""
    }
    val hex = value.replace(":", "").trim().uppercase()
    if (hex.length != 40 || !hex.all { it in '0'..'9' || it in 'A'..'F' }) {
        throw GradleException(
            "SEREIN_SIGNING_SHA1 must be the SHA-1 of the APK signing certificate, " +
                "as printed by `keytool -list -v` (40 hex digits, colons allowed).",
        )
    }
    return hex
}
