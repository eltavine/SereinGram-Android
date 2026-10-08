package com.eltavine.sereingram.buildlogic

import org.gradle.api.Project
import java.io.File
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

    /**
     * The release keystore: `KEYSTORE_FILE`, a path from the repository's root,
     * or `KEYSTORE_BASE64`, the keystore itself for CI secrets. Null leaves the
     * upstream script's choice.
     */
    fun keystore(): File? {
        get("KEYSTORE_FILE")?.let { return project.rootProject.file(it) }
        val encoded = get("KEYSTORE_BASE64") ?: return null
        return project.layout.buildDirectory.file("serein/release.keystore").get().asFile.apply {
            parentFile.mkdirs()
            writeBytes(Base64.getMimeDecoder().decode(encoded.trim()))
        }
    }

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
