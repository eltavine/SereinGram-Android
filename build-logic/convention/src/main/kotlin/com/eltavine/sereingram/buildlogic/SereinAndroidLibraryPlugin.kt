package com.eltavine.sereingram.buildlogic

import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * A SereinGram module that needs the Android framework but not Telegram, such
 * as a storage adapter. Its unit tests run on the JVM under Robolectric.
 */
class SereinAndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("org.jetbrains.kotlin.android")
            checkKotlinVersion()
            extensions.configure<LibraryAndroidComponentsExtension> { checkAndroidGradlePluginVersion(pluginVersion) }
            extensions.configure<LibraryExtension> {
                namespace = sereinNamespace
                compileSdk = AppLevels.COMPILE_SDK
                defaultConfig.minSdk = AppLevels.MIN_SDK
                compileOptions.sourceCompatibility = JavaVersion.VERSION_11
                compileOptions.targetCompatibility = JavaVersion.VERSION_11
                testOptions.unitTests.isIncludeAndroidResources = true
            }
            extensions.configure<KotlinAndroidProjectExtension> {
                explicitApi()
                compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
            }
            dependencies.add("testImplementation", libs.library("junit4"))
            dependencies.add("testImplementation", libs.library("robolectric"))
            dependencies.add("testImplementation", libs.library("androidx-test-core"))
            dependencies.add("testImplementation", "org.jetbrains.kotlin:kotlin-test-junit")
        }
    }
}
