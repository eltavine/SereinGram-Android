package com.eltavine.sereingram.features.downloads

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.Bundle
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.system.Os
import android.system.OsConstants
import android.system.StructStat
import android.system.ErrnoException
import androidx.test.core.app.ApplicationProvider
import androidx.documentfile.provider.DocumentFile
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadows.ShadowContentResolver
import java.io.File
import java.io.FileNotFoundException
import java.io.FileDescriptor
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.util.IdentityHashMap

/** Real document-tree operations against a provider, without assuming a filesystem path for a URI. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28, 34],
    shadows = [DirectoryExportsTest.ProviderCalls::class, DirectoryExportsTest.HostFileStats::class])
class DirectoryExportsTest {
    @get:Rule val folder = TemporaryFolder()
    private lateinit var context: Context
    private lateinit var provider: FolderProvider
    private lateinit var source: File
    private var access = true
    private val tree = DocumentsContract.buildTreeDocumentUri(AUTHORITY, "root").toString()

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val cache = folder.newFolder("app-cache")
        // Robolectric does not model the system URI-permission service. Model just the picker grant;
        // queries, creation, streams and deletion still go through the actual document provider.
        context = object : ContextWrapper(application) {
            override fun getCacheDir(): File = cache
            override fun checkCallingOrSelfUriPermission(uri: android.net.Uri, modeFlags: Int): Int =
                if (uri.authority == AUTHORITY && access) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        }
        context.getSharedPreferences("serein_download_exports", Context.MODE_PRIVATE).edit().clear().commit()
        provider = FolderProvider().apply {
            directory = folder.newFolder("destination")
            attachInfo(this@DirectoryExportsTest.context, ProviderInfo().apply {
                authority = AUTHORITY
                exported = true
                grantUriPermissions = true
                readPermission = Manifest.permission.MANAGE_DOCUMENTS
                writePermission = Manifest.permission.MANAGE_DOCUMENTS
                applicationInfo = this@DirectoryExportsTest.context.applicationInfo
            })
        }
        ShadowContentResolver.registerProviderInternal(AUTHORITY, provider)
        val root = DocumentFile.fromTreeUri(context, android.net.Uri.parse(tree))!!
        context.contentResolver.query(root.uri, null, null, null, null)!!.use { cursor ->
            assertTrue("Provider returned no root", cursor.moveToFirst())
            assertEquals(DocumentsContract.Document.MIME_TYPE_DIR,
                cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)))
        }
        assertTrue("Provider root ${root.uri}: type=${root.type}, write=${root.canWrite()}, permission=${context.checkCallingOrSelfUriPermission(root.uri, 2)}", root.isDirectory)
        source = File(cache, "cached.bin").apply { writeBytes(ByteArray(100_000) { (it % 249).toByte() }) }
        HostFileStats.source = source
        HostFileStats.descriptors.clear()
    }

    @Test
    fun aVerifiedExportRemovesThePrivateCopyAndARepeatDownloadDoesNotMakeAnotherExport() {
        val exports = DirectoryExports(context)
        val bytes = source.readBytes()
        val first = exports.save(tree, "user:dc:document", source, "report.pdf", "application/pdf")
        assertFalse("The verified app copy must be removed", source.exists())
        source.writeBytes(bytes)
        assertEquals(first, DirectoryExports(context).save(tree, "user:dc:document", source, "report.pdf", "application/pdf"))
        assertFalse(source.exists())
        assertEquals(listOf("report.pdf"), provider.directory.list()!!.toList())
        assertArrayEquals(bytes, File(provider.directory, "report.pdf").readBytes())
    }

    @Test
    fun unrelatedDownloadsWithTheSameNameDoNotOverwriteEachOther() {
        val exports = DirectoryExports(context)
        exports.save(tree, "user:dc:first", source, "report.pdf", "application/pdf")
        source.writeText("another document")
        exports.save(tree, "user:dc:second", source, "report.pdf", "application/pdf")
        assertEquals(100_000L, File(provider.directory, "report.pdf").length())
        assertEquals("another document", File(provider.directory, "report (1).pdf").readText())
    }

    @Test
    fun deletingAnExportAllowsTheSameDocumentToBeSavedAgain() {
        val exports = DirectoryExports(context)
        val bytes = source.readBytes()
        exports.save(tree, "document", source, "report.pdf", "application/pdf")
        File(provider.directory, "report.pdf").delete()
        source.writeBytes(bytes)
        exports.save(tree, "document", source, "report.pdf", "application/pdf")
        assertArrayEquals(bytes, File(provider.directory, "report.pdf").readBytes())
        assertFalse(source.exists())
    }

    @Test
    fun anUnavailableProviderLeavesTheCacheAndNoPartialFile() {
        provider.failOpen = true
        val bytes = source.readBytes()
        assertThrows(Exception::class.java) {
            DirectoryExports(context).save(tree, "document", source, "report.pdf", "application/pdf")
        }
        assertTrue(source.exists())
        assertTrue(provider.directory.list()!!.isEmpty())
        provider.failOpen = false
        DirectoryExports(context).save(tree, "document", source, "report.pdf", "application/pdf")
        assertArrayEquals(bytes, File(provider.directory, "report.pdf").readBytes())
        assertFalse(source.exists())
    }

    @Test
    fun anInterruptedCopyIsRemovedBeforeRetrying() {
        File(provider.directory, "partial.pdf").writeText("partial")
        val partial = DocumentsContract.buildDocumentUriUsingTree(android.net.Uri.parse(tree), "partial.pdf")
        context.getSharedPreferences("serein_download_exports", Context.MODE_PRIVATE)
            .edit().putString("pending", partial.toString()).commit()
        DirectoryExports(context).save(tree, "document", source, "report.pdf", "application/pdf")
        assertEquals(listOf("report.pdf"), provider.directory.list()!!.toList())
    }

    @Test
    fun aRevokedGrantLeavesTheSourceAndDestinationUntouched() {
        access = false
        assertThrows(java.io.IOException::class.java) {
            DirectoryExports(context).save(tree, "document", source, "report.pdf", "application/pdf")
        }
        assertEquals(100_000L, source.length())
        assertTrue(provider.directory.list()!!.isEmpty())
    }

    @Test
    fun sameSizeCorruptionInAPreviousExportDoesNotCountAsAnIdenticalCopy() {
        val bytes = source.readBytes()
        val exports = DirectoryExports(context)
        exports.save(tree, "document", source, "report.pdf", "application/pdf")
        File(provider.directory, "report.pdf").writeBytes(ByteArray(bytes.size) { 42 })
        source.writeBytes(bytes)
        exports.save(tree, "document", source, "report.pdf", "application/pdf")
        assertArrayEquals(bytes, File(provider.directory, "report (1).pdf").readBytes())
        assertEquals(42.toByte(), File(provider.directory, "report.pdf").readBytes()[0])
        assertFalse(source.exists())
    }

    @Test
    fun anExportThatCannotBeReadBackNeverCausesDeletion() {
        val bytes = source.readBytes()
        provider.failRead = true
        assertThrows(Exception::class.java) {
            DirectoryExports(context).save(tree, "document", source, "report.pdf", "application/pdf")
        }
        assertArrayEquals(bytes, source.readBytes())
        provider.failRead = false
        DirectoryExports(context).save(tree, "document", source, "report.pdf", "application/pdf")
        assertArrayEquals(bytes, File(provider.directory, "report.pdf").readBytes())
        assertFalse(source.exists())
    }

    @Test
    fun aCorruptNewExportKeepsTheOriginalUntilASuccessfulRetry() {
        val bytes = source.readBytes()
        provider.onRead = { it.writeBytes(ByteArray(bytes.size) { 42 }) }
        assertThrows(java.io.IOException::class.java) {
            DirectoryExports(context).save(tree, "document", source, "report.pdf", "application/pdf")
        }
        assertArrayEquals(bytes, source.readBytes())
        provider.onRead = {}
        DirectoryExports(context).save(tree, "document", source, "report.pdf", "application/pdf")
        assertArrayEquals(bytes, File(provider.directory, "report.pdf").readBytes())
        assertFalse(source.exists())
    }

    @Test
    fun changingTheSourceDuringVerificationNeverDeletesTheChangedFile() {
        provider.onRead = {
            source.writeText("new contents")
        }
        assertThrows(java.io.IOException::class.java) {
            DirectoryExports(context).save(tree, "document", source, "report.pdf", "application/pdf")
        }
        assertEquals("new contents", source.readText())
    }

    @Test
    fun changingTheSourceAfterVerificationNeverDeletesTheChangedFile() {
        File(provider.directory, "report.pdf").writeBytes(source.readBytes())
        val uri = DocumentsContract.buildDocumentUriUsingTree(Uri.parse(tree), "report.pdf")
        val verified = VerifiedDownload.compare(context, source, uri)!!
        assertTrue(source.delete())
        source.writeText("replacement file")
        assertThrows(java.io.IOException::class.java) { verified.removePrivateCopy(context) }
        assertEquals("replacement file", source.readText())
    }

    @Test
    fun losingTheDestinationAfterVerificationKeepsTheSource() {
        val destination = File(provider.directory, "report.pdf").apply { writeBytes(source.readBytes()) }
        val uri = DocumentsContract.buildDocumentUriUsingTree(Uri.parse(tree), "report.pdf")
        val verified = VerifiedDownload.compare(context, source, uri)!!
        assertTrue(destination.delete())
        assertThrows(java.io.IOException::class.java) { verified.removePrivateCopy(context) }
        assertEquals(100_000L, source.length())
    }

    @Test
    fun publicSourcesAndPathsOutsideTheAppAreNeverDeleted() {
        val public = folder.newFile("sent-by-user.pdf").apply { writeText("user file") }
        HostFileStats.source = public
        DirectoryExports(context).save(tree, "document", public, "report.pdf", "application/pdf")
        assertEquals("user file", public.readText())
        assertEquals("user file", File(provider.directory, "report.pdf").readText())
    }

    @Test
    fun aProviderAliasOfTheSourceIsNeverDeleted() {
        // The user may have selected the app's own folder on an older Android version.
        provider.directory = source.parentFile!!
        val uri = DocumentsContract.buildDocumentUriUsingTree(Uri.parse(tree), source.name)
        val verified = VerifiedDownload.compare(context, source, uri)!!
        verified.removePrivateCopy(context)
        assertEquals(100_000L, source.length())
    }

    class FolderProvider : DocumentsProvider() {
        lateinit var directory: File
        var failOpen = false
        var failRead = false
        var onRead: (File) -> Unit = {}
        override fun onCreate(): Boolean = true
        override fun queryRoots(projection: Array<out String>?): Cursor = MatrixCursor(arrayOf(DocumentsContract.Root.COLUMN_ROOT_ID))

        private fun cursor(projection: Array<out String>?): MatrixCursor = MatrixCursor(projection ?: arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE,
        ))

        private fun add(cursor: MatrixCursor, id: String) {
            val file = if (id == "root") directory else File(directory, id)
            if (!file.exists()) return
            cursor.newRow().apply {
                cursor.columnNames.forEach { column ->
                    add(column, when (column) {
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID -> id
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME -> file.name
                        DocumentsContract.Document.COLUMN_MIME_TYPE -> if (id == "root") DocumentsContract.Document.MIME_TYPE_DIR else "application/octet-stream"
                        DocumentsContract.Document.COLUMN_FLAGS -> DocumentsContract.Document.FLAG_SUPPORTS_WRITE or
                            DocumentsContract.Document.FLAG_SUPPORTS_DELETE or DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE
                        DocumentsContract.Document.COLUMN_SIZE -> file.length()
                        else -> null
                    })
                }
            }
        }

        override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor = cursor(projection).apply { add(this, documentId) }
        override fun queryChildDocuments(parentDocumentId: String, projection: Array<out String>?, sortOrder: String?): Cursor =
            cursor(projection).apply { directory.list()!!.forEach { add(this, it) } }
        override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean = parentDocumentId == "root"
        override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String {
            check(parentDocumentId == "root")
            check(File(directory, displayName).createNewFile())
            return displayName
        }
        override fun deleteDocument(documentId: String) { check(File(directory, documentId).delete()) }
        override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
            if (failOpen) throw FileNotFoundException("Storage disconnected")
            if (mode == "r") {
                if (failRead) throw FileNotFoundException("Read permission lost")
                onRead(File(directory, documentId))
            }
            val file = File(directory, documentId)
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode)).also {
                HostFileStats.descriptors[it.fileDescriptor] = file
            }
        }
    }

    /** Robolectric returns zero-filled fstat values on Windows. Supply host metadata and file IDs
     * (canonical paths where the host has no ID); associate descriptors with their provider file. */
    @Implements(Os::class)
    class HostFileStats {
        companion object {
            lateinit var source: File
            val descriptors = IdentityHashMap<FileDescriptor, File>()

            @JvmStatic @Implementation
            fun fstat(fd: FileDescriptor): StructStat = stat((descriptors[fd] ?: source).path)

            @JvmStatic @Implementation
            fun stat(path: String): StructStat {
                if (!File(path).exists()) throw ErrnoException("stat", OsConstants.ENOENT)
                val attributes = Files.readAttributes(File(path).toPath(), BasicFileAttributes::class.java)
                val mode = if (attributes.isDirectory) OsConstants.S_IFDIR else OsConstants.S_IFREG
                val identity = attributes.fileKey() ?: File(path).canonicalPath
                return StructStat(1, identity.hashCode().toLong(), mode or 0x180,
                    1, 0, 0, 0, attributes.size(), 0, attributes.lastModifiedTime().toMillis(),
                    attributes.creationTime().toMillis(), 4096, 1)
            }
        }
    }

    /** ContentResolver adapts legacy calls before IPC; Robolectric calls the provider directly. */
    @Implements(DocumentsProvider::class)
    class ProviderCalls {
        @RealObject private lateinit var provider: DocumentsProvider

        @Implementation(minSdk = 26)
        protected fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? =
            provider.query(uri, projection, Bundle(), null)

        @Implementation
        protected fun call(method: String, arg: String?, extras: Bundle?): Bundle {
            val uri = extras!!.getParcelable<Uri>("uri")!!
            val id = DocumentsContract.getDocumentId(uri)
            return when (method) {
                "android:createDocument" -> {
                    val created = provider.createDocument(id,
                        extras.getString(DocumentsContract.Document.COLUMN_MIME_TYPE)!!,
                        extras.getString(DocumentsContract.Document.COLUMN_DISPLAY_NAME)!!)
                    Bundle().apply { putParcelable("uri", DocumentsContract.buildDocumentUriUsingTree(uri, created)) }
                }
                "android:deleteDocument" -> {
                    provider.deleteDocument(id)
                    Bundle()
                }
                else -> error("Unexpected document operation: $method")
            }
        }
    }

    private companion object {
        const val AUTHORITY = "com.eltavine.sereingram.test.downloads"
    }
}
