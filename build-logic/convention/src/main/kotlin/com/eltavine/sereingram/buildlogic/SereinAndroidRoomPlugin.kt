package com.eltavine.sereingram.buildlogic

import androidx.room.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Room for a `serein.android.library` module. The schema of every database
 * version is exported to `schemas/` and committed, so a change to it shows up
 * in review and can be migrated from. Room's Gradle plugin exports each
 * variant on its own and copies the result, so variants never race for a file.
 */
class SereinAndroidRoomPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("androidx.room")
            extensions.configure<RoomExtension> {
                schemaDirectory(layout.projectDirectory.dir("schemas").asFile.path)
                generateKotlin = true
            }
            dependencies.add("implementation", libs.library("room-runtime"))
            dependencies.add("ksp", libs.library("room-compiler"))
        }
    }
}
