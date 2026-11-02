package com.eltavine.sereingram.features.downloads

import java.io.InputStream
import java.io.OutputStream

/** The storage provider creates a new document; existing files are never opened for writing. */
public interface ExportDirectory {
    public fun names(): Set<String>
    public fun create(name: String, mimeType: String): ExportDocument
}

public interface ExportDocument {
    public val uri: String
    public fun open(): OutputStream
    public fun delete()
}

/** Copies a completed download, removing the new document if reading, writing or closing fails. */
public fun exportFile(directory: ExportDirectory, name: String, mimeType: String, source: () -> InputStream): String {
    val document = directory.create(availableName(safeName(name), directory.names()), mimeType)
    try {
        source().use { input -> document.open().use { output -> input.copyTo(output) } }
        return document.uri
    } catch (error: Exception) {
        runCatching { document.delete() }.exceptionOrNull()?.let(error::addSuppressed)
        throw error
    }
}

/** A document's original name may contain path separators or controls. */
public fun safeName(name: String): String {
    val cleaned = name.map { if (it == '/' || it == '\\' || it.isISOControl()) '_' else it }
        .joinToString("").trim().ifBlank { "download" }
    if (cleaned.all { it == '.' }) return "download"
    val dot = cleaned.lastIndexOf('.').takeIf { it > 0 } ?: cleaned.length
    val extension = cleaned.substring(dot).takeIf { it.toByteArray(Charsets.UTF_8).size <= 32 }.orEmpty()
    var stem = cleaned.removeSuffix(extension)
    // Leave room for collision suffixes on filesystems whose limit is 255 bytes, also for CJK names.
    while ((stem + extension).toByteArray(Charsets.UTF_8).size > 180) {
        stem = stem.dropLast(Character.charCount(stem.codePointBefore(stem.length)))
    }
    return stem + extension
}

/** Keep the extension when two different documents share a name. */
public fun availableName(name: String, existing: Set<String>): String {
    if (name !in existing) return name
    val dot = name.lastIndexOf('.').takeIf { it > 0 } ?: name.length
    val stem = name.substring(0, dot)
    val extension = name.substring(dot)
    return generateSequence(1) { it + 1 }.map { "$stem ($it)$extension" }.first { it !in existing }
}
