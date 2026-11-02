package com.eltavine.sereingram.features.downloads

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.security.MessageDigest

/** Accessed only on the feature's IO queue. The index keeps retries from creating extra copies. */
internal class DirectoryExports(private val context: Context) {
    private val index = context.getSharedPreferences("serein_download_exports", Context.MODE_PRIVATE)

    fun save(tree: String, identity: String, source: File, name: String, mimeType: String): String {
        val directory = DocumentFile.fromTreeUri(context, Uri.parse(tree))
            ?.takeIf { it.isDirectory && it.canWrite() } ?: throw IOException("Directory is not writable")
        val key = MessageDigest.getInstance("SHA-256").digest("$tree\n$identity".toByteArray())
            .joinToString("") { "%02x".format(it) }
        val previous = index.getString(key, null)?.let { DocumentFile.fromSingleUri(context, Uri.parse(it)) }
        if (previous != null && previous.exists()) {
            // A file with the same length may have been edited or corrupted since the last export.
            val verified = VerifiedDownload.compare(context, source, previous.uri)
            if (verified != null) {
                verified.removePrivateCopy(context)
                return previous.uri.toString()
            }
        }

        // An interrupted copy from a previous process is not a completed export.
        index.getString(PENDING, null)?.let { pending ->
            val partial = DocumentFile.fromSingleUri(context, Uri.parse(pending))
            if (partial?.exists() == true && !partial.delete()) throw IOException("Cannot remove interrupted download")
            index.edit().remove(PENDING).commit()
        }
        val target = object : ExportDirectory {
            override fun names(): Set<String> = directory.listFiles().mapNotNull { it.name }.toSet()

            override fun create(name: String, mimeType: String): ExportDocument {
                val file = directory.createFile(mimeType, name) ?: throw IOException("Cannot create download")
                if (!index.edit().putString(PENDING, file.uri.toString()).commit()) {
                    file.delete()
                    throw IOException("Cannot record download")
                }
                return object : ExportDocument {
                    override val uri: String = file.uri.toString()
                    override fun open(): OutputStream = context.contentResolver.openOutputStream(file.uri, "w")
                        ?: throw IOException("Cannot open download")
                    override fun delete() {
                        if (!file.delete()) throw IOException("Cannot remove incomplete download")
                        index.edit().remove(PENDING).commit()
                    }
                }
            }
        }
        val uri = exportFile(target, name, mimeType, source::inputStream)
        val verified = VerifiedDownload.compare(context, source, Uri.parse(uri))
            ?: throw IOException("Saved download differs from the app copy")
        // Commit before deleting the source: after a crash the verified export must never be treated
        // as an incomplete document and removed on the next run.
        if (!index.edit().putString(key, uri).remove(PENDING).commit()) throw IOException("Cannot record completed download")
        verified.removePrivateCopy(context)
        return uri
    }

    private companion object {
        const val PENDING = "pending"
    }
}
