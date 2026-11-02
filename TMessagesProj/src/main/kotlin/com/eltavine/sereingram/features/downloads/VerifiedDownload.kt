package com.eltavine.sereingram.features.downloads

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import android.system.StructStat
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.IOException

/** A completed source and the independent export that was read back byte for byte. */
internal class VerifiedDownload private constructor(
    private val source: File,
    private val snapshot: StructStat,
    private val modified: Long,
    private val independent: Boolean,
    private val destination: Uri,
) {
    /** Only the app's own file/cache directories may be cleaned, never a public or sent local file. */
    fun removePrivateCopy(context: Context) {
        val roots = listOf(context.cacheDir, context.filesDir) +
            context.externalCacheDirs.filterNotNull() + context.getExternalFilesDirs(null).filterNotNull()
        if (!independent || roots.none { source.path.startsWith(it.canonicalPath + File.separator) }) return
        val saved = DocumentFile.fromSingleUri(context, destination)
        if (saved == null || !saved.exists() || !saved.canRead()) throw IOException("Saved download is no longer accessible")
        if (source.lastModified() != modified || !unchanged(snapshot, Os.stat(source.path))) {
            throw IOException("Download changed after verification")
        }
        if (!source.delete()) throw IOException("Cannot remove verified app copy")
    }

    companion object {
        /** Null means different contents; IO and permission failures propagate and keep the source. */
        fun compare(context: Context, source: File, uri: Uri): VerifiedDownload? {
            val canonical = source.canonicalFile
            val modified = canonical.lastModified()
            var verified: VerifiedDownload? = null
            canonical.inputStream().use { original ->
                val before = Os.fstat(original.fd)
                if (!OsConstants.S_ISREG(before.st_mode)) throw IOException("Download is not a regular file")
                val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
                    ?: throw IOException("Cannot read saved download")
                ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { saved ->
                    val destination = Os.fstat(saved.fd)
                    if (sameContents(original, saved)) {
                        if (canonical.lastModified() != modified || !unchanged(before, Os.fstat(original.fd)) ||
                            !unchanged(before, Os.stat(canonical.path)) || !unchanged(destination, Os.fstat(saved.fd))) {
                            throw IOException("Download changed during verification")
                        }
                        verified = VerifiedDownload(canonical, before, modified,
                            before.st_dev != destination.st_dev || before.st_ino != destination.st_ino, uri)
                    }
                }
            }
            // Closing either stream may fail too; no deletion is permitted before both closes succeeded.
            return verified
        }

        private fun unchanged(before: StructStat, after: StructStat): Boolean =
            before.st_dev == after.st_dev && before.st_ino == after.st_ino &&
                before.st_size == after.st_size && before.st_mtime == after.st_mtime &&
                before.st_ctime == after.st_ctime
    }
}
