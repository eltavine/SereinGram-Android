package com.eltavine.sereingram.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * A SereinGram module that knows nothing of Android or Telegram: plain Kotlin
 * with an explicit public API, Java 11 bytecode like the app, and JUnit
 * Platform tests that run on the JVM.
 */
class SereinJvmLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            checkKotlinVersion()
            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }
            extensions.configure<KotlinJvmProjectExtension> {
                explicitApi()
                compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
                // Against Java 11's API rather than the build JDK's, which Android lacks.
                compilerOptions.freeCompilerArgs.add("-Xjdk-release=11")
            }
            dependencies.add("testImplementation", dependencies.platform(libs.library("junit-bom")))
            dependencies.add("testImplementation", "org.jetbrains.kotlin:kotlin-test")
            dependencies.add("testRuntimeOnly", libs.library("junit-platformLauncher"))
            tasks.withType<Test>().configureEach { useJUnitPlatform() }
        }
    }
}
