package com.eltavine.sereingram.buildlogic

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ResValue
import io.sentry.android.gradle.extensions.SentryPluginExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

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
            target.dependencies.add("implementation", target.libs.library("okhttp-dnsoverhttps"))
            target.dependencies.add("testImplementation", target.libs.library("archunit"))
            target.dependencies.add("testImplementation", target.libs.library("ktor-client-mock"))
            // Robolectric loads all of the app's resources into each of its sandboxes, which outgrows the 512 MB a test JVM gets.
            target.tasks.withType<Test>().configureEach { maxHeapSize = "2g" }
            val properties = SereinProperties(target)
            target.pluginManager.withPlugin("io.sentry.android.gradle") {
                // SereinGram never starts Sentry, so its native crash handling and session replay only weigh.
                target.configurations.configureEach {
                    if (name.endsWith("RuntimeClasspath")) {
                        SENTRY_UNUSED.forEach { exclude(mapOf("group" to "io.sentry", "module" to it)) }
                    }
                }
            }
            target.extensions.configure<ApplicationAndroidComponentsExtension> {
                target.checkAndroidGradlePluginVersion(pluginVersion)
                finalizeDsl { android ->
                    target.pluginManager.withPlugin("io.sentry.android.gradle") { quietSentry(target) }
                    if (android.compileSdk != AppLevels.COMPILE_SDK || android.defaultConfig.minSdk != AppLevels.MIN_SDK) {
                        throw GradleException(
                            "TMessagesProj builds against SDK ${android.compileSdk} for SDK ${android.defaultConfig.minSdk} and up; " +
                                "update AppLevels in build-logic to match, which SereinGram's Android modules use.",
                        )
                    }
                    android.defaultConfig.applicationId = APPLICATION_ID
                    // Upstream reads these from a properties file only; CI passes them in the environment.
                    telegramApiId(properties["TELEGRAM_APP_ID"])?.let { android.defaultConfig.buildConfigField("int", "APP_ID", it) }
                    telegramApiHash(properties["TELEGRAM_APP_HASH"])?.let { android.defaultConfig.buildConfigField("String", "APP_HASH", "\"$it\"") }
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

    // After TMessagesProj/build.gradle set them, and before Sentry reads them for each variant.
    private fun quietSentry(project: Project) {
        project.extensions.configure<SentryPluginExtension> {
            telemetry.set(false)
            includeDependenciesReport.set(false)
            tracingInstrumentation.enabled.set(false)
        }
    }

    private companion object {
        val SENTRY_UNUSED = listOf("sentry-android-ndk", "sentry-native-ndk", "sentry-android-replay")
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

internal fun telegramApiId(value: String?): String? {
    val id = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
    if (id.toIntOrNull()?.takeIf { it > 0 } == null) {
        throw GradleException("TELEGRAM_APP_ID must be the api_id from my.telegram.org, a positive number.")
    }
    return id
}

internal fun telegramApiHash(value: String?): String? {
    val hash = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
    if (!Regex("[0-9a-f]{32}").matches(hash)) {
        throw GradleException("TELEGRAM_APP_HASH must be the api_hash from my.telegram.org, 32 hex digits.")
    }
    return hash
}
