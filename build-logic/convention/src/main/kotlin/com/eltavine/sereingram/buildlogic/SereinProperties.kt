package com.eltavine.sereingram.buildlogic

import org.gradle.api.Project
import java.util.Base64
import java.util.Properties

/**
 * Build settings shared with the upstream script: the base64 `LOCAL_PROPERTIES`
 * variable used by CI, then `local.properties`, then the environment.
 */
internal class SereinProperties(private val project: Project) {
    private val local: Properties by lazy(::load)

    operator fun get(name: String): String? =
        local.getProperty(name)?.takeIf(String::isNotBlank)
            ?: project.providers.environmentVariable(name).orNull?.takeIf(String::isNotBlank)

    private fun load(): Properties {
        val properties = Properties()
        val encoded = project.providers.environmentVariable("LOCAL_PROPERTIES").orNull
        if (!encoded.isNullOrBlank()) {
            Base64.getMimeDecoder().decode(encoded.trim()).inputStream().use(properties::load)
        } else {
            val file = project.rootProject.file("local.properties")
            if (file.isFile) {
                file.inputStream().use(properties::load)
            }
        }
        return properties
    }
}
