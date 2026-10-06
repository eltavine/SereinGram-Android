package com.eltavine.sereingram.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

/**
 * A SereinGram module that knows nothing of Android or Telegram: plain Kotlin
 * with an explicit public API, Java 11 bytecode like the app, and JUnit
 * Platform tests that run on the JVM.
 */
class SereinJvmLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            val pinned = libs.findVersion("kotlin").get().requiredVersion
            val applied = getKotlinPluginVersion()
            if (applied != pinned) {
                throw GradleException(
                    "The build applies Kotlin $applied but gradle/libs.versions.toml pins $pinned; " +
                        "update the catalog to the version in the root build.gradle.",
                )
            }
            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }
            extensions.configure<KotlinJvmProjectExtension> {
                explicitApi()
                compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
            }
            dependencies.add("testImplementation", dependencies.platform(libs.findLibrary("junit-bom").get()))
            dependencies.add("testImplementation", "org.jetbrains.kotlin:kotlin-test")
            dependencies.add("testRuntimeOnly", libs.findLibrary("junit-platformLauncher").get())
            tasks.withType<Test>().configureEach { useJUnitPlatform() }
        }
    }
}
