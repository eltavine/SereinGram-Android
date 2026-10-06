package com.eltavine.sereingram.buildlogic

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Room for a `serein.android.library` module. The schema of every database
 * version is exported to `schemas/` and committed, so a change to it shows up
 * in review and can be migrated from.
 */
class SereinAndroidRoomPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            extensions.configure<KspExtension> {
                arg("room.schemaLocation", layout.projectDirectory.dir("schemas").asFile.path)
                arg("room.generateKotlin", "true")
            }
            dependencies.add("implementation", libs.library("room-runtime"))
            dependencies.add("ksp", libs.library("room-compiler"))
        }
    }
}
