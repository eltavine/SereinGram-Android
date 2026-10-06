package com.eltavine.sereingram.buildlogic

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
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
                    val sha1 = signingCertificateSha1(properties["SEREIN_SIGNING_SHA1"])
                    android.defaultConfig.externalNativeBuild.cmake.arguments +=
                        "-DSEREIN_SIGNING_SHA1=$sha1"
                }
            }
        }
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
