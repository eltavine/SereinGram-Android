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

/** Real document-tree operations against a provider, without assuming a filesystem path for a URI. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28, 34], shadows = [DirectoryExportsTest.ProviderCalls::class])
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
        // Robolectric does not model the system URI-permission service. Model just the picker grant;
        // queries, creation, streams and deletion still go through the actual document provider.
        context = object : ContextWrapper(application) {
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
        source = folder.newFile("cached.bin").apply { writeBytes(ByteArray(100_000) { (it % 249).toByte() }) }
    }

    @Test
    fun aCompletedCopySurvivesCacheDeletionAndIsNotExportedTwice() {
        val exports = DirectoryExports(context)
        val first = exports.copy(tree, "user:dc:document", source, "report.pdf", "application/pdf")
        assertEquals(first, DirectoryExports(context).copy(tree, "user:dc:document", source, "report.pdf", "application/pdf"))
        assertEquals(listOf("report.pdf"), provider.directory.list()!!.toList())
        val bytes = source.readBytes()
        source.delete()
        assertArrayEquals(bytes, File(provider.directory, "report.pdf").readBytes())
    }

    @Test
    fun unrelatedDownloadsWithTheSameNameDoNotOverwriteEachOther() {
        val exports = DirectoryExports(context)
        exports.copy(tree, "user:dc:first", source, "report.pdf", "application/pdf")
        source.writeText("another document")
        exports.copy(tree, "user:dc:second", source, "report.pdf", "application/pdf")
        assertEquals(100_000L, File(provider.directory, "report.pdf").length())
        assertEquals("another document", File(provider.directory, "report (1).pdf").readText())
    }

    @Test
    fun deletingAnExportAllowsTheSameDocumentToBeSavedAgain() {
        val exports = DirectoryExports(context)
        exports.copy(tree, "document", source, "report.pdf", "application/pdf")
        File(provider.directory, "report.pdf").delete()
        exports.copy(tree, "document", source, "report.pdf", "application/pdf")
        assertArrayEquals(source.readBytes(), File(provider.directory, "report.pdf").readBytes())
    }

    @Test
    fun anUnavailableProviderLeavesTheCacheAndNoPartialFile() {
        provider.failOpen = true
        assertThrows(Exception::class.java) {
            DirectoryExports(context).copy(tree, "document", source, "report.pdf", "application/pdf")
        }
        assertTrue(source.exists())
        assertTrue(provider.directory.list()!!.isEmpty())
        provider.failOpen = false
        DirectoryExports(context).copy(tree, "document", source, "report.pdf", "application/pdf")
        assertArrayEquals(source.readBytes(), File(provider.directory, "report.pdf").readBytes())
    }

    @Test
    fun anInterruptedCopyIsRemovedBeforeRetrying() {
        File(provider.directory, "partial.pdf").writeText("partial")
        val partial = DocumentsContract.buildDocumentUriUsingTree(android.net.Uri.parse(tree), "partial.pdf")
        context.getSharedPreferences("serein_download_exports", Context.MODE_PRIVATE)
            .edit().putString("pending", partial.toString()).commit()
        DirectoryExports(context).copy(tree, "document", source, "report.pdf", "application/pdf")
        assertEquals(listOf("report.pdf"), provider.directory.list()!!.toList())
    }

    @Test
    fun aRevokedGrantLeavesTheSourceAndDestinationUntouched() {
        access = false
        assertThrows(java.io.IOException::class.java) {
            DirectoryExports(context).copy(tree, "document", source, "report.pdf", "application/pdf")
        }
        assertEquals(100_000L, source.length())
        assertTrue(provider.directory.list()!!.isEmpty())
    }

    class FolderProvider : DocumentsProvider() {
        lateinit var directory: File
        var failOpen = false
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
            return ParcelFileDescriptor.open(File(directory, documentId), ParcelFileDescriptor.parseMode(mode))
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
