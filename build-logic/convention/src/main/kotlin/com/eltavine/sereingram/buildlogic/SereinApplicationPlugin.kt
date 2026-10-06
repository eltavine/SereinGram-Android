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
 * script stays as Nagram ships it.
 */
class SereinApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.withPlugin("com.android.application") {
            val properties = SereinProperties(target)
            target.extensions.configure<ApplicationAndroidComponentsExtension> {
                finalizeDsl { android ->
                    android.defaultConfig.applicationId = APPLICATION_ID
                    val sha1 = signingCertificateSha1(properties["SEREIN_SIGNING_SHA1"])
                    android.defaultConfig.externalNativeBuild.cmake.arguments +=
                        "-DSEREIN_SIGNING_SHA1=$sha1"
                    // Build type resources take precedence over src/main, which upstream owns.
                    android.buildTypes.forEach { buildType ->
                        android.sourceSets.getByName(buildType.name).res.srcDir(OVERLAY_RES)
                    }
                    val release = android.signingConfigs.findByName("release")
                    if (release?.storePassword.isNullOrEmpty() || release?.keyAlias.isNullOrEmpty()) {
                        target.logger.warn(
                            "SereinGram: no release signing key is configured " +
                                "(KEYSTORE_PASS, ALIAS_NAME, ALIAS_PASS), signing with the debug key.",
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
