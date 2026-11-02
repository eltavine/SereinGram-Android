package com.eltavine.sereingram.features.downloads

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

class FileExportTest {
    private class Directory : ExportDirectory {
        val files = linkedMapOf("report.pdf" to "existing".toByteArray())
        var failWrite = false
        var failClose = false
        override fun names(): Set<String> = files.keys
        override fun create(name: String, mimeType: String): ExportDocument {
            check(name !in files)
            files[name] = byteArrayOf()
            return object : ExportDocument {
                override val uri: String = "content://downloads/$name"
                override fun open(): OutputStream = object : ByteArrayOutputStream() {
                    override fun write(bytes: ByteArray, offset: Int, count: Int) {
                        if (failWrite) throw IOException("Disk full")
                        super.write(bytes, offset, count)
                    }
                    override fun close() {
                        if (failClose) throw IOException("Provider disconnected")
                        files[name] = toByteArray()
                    }
                }
                override fun delete() { files.remove(name) }
            }
        }
    }

    @Test
    fun copiesBytesWithoutOverwritingAFileWithTheSameName() {
        val directory = Directory()
        val content = ByteArray(200_000) { (it % 251).toByte() }
        val uri = exportFile(directory, "report.pdf", "application/pdf") { content.inputStream() }
        assertEquals("content://downloads/report (1).pdf", uri)
        assertContentEquals(content, directory.files["report (1).pdf"])
        assertEquals("existing", directory.files.getValue("report.pdf").toString(Charsets.UTF_8))
    }

    @Test
    fun aReadFailureDeletesOnlyTheNewDocument() {
        val directory = Directory()
        assertFailsWith<IOException> {
            exportFile(directory, "report.pdf", "application/pdf") { throw IOException("Source disappeared") }
        }
        assertEquals(setOf("report.pdf"), directory.files.keys)
    }

    @Test
    fun writeAndCloseFailuresRemoveThePartialDocument() {
        listOf(true, false).forEach { writing ->
            val directory = Directory().apply { failWrite = writing; failClose = !writing }
            assertFailsWith<IOException> {
                exportFile(directory, "report.pdf", "application/pdf") { "data".byteInputStream() }
            }
            assertEquals(setOf("report.pdf"), directory.files.keys)
        }
    }

    @Test
    fun namesCannotEscapeTheDirectoryAndKeepValidLeadingDots() {
        assertEquals(".._.._file.txt", safeName("../..\\file.txt"))
        assertEquals("download", safeName(".."))
        assertEquals(".env", safeName(".env"))
        assertEquals(".gitignore", safeName(".gitignore"))
        assertEquals("a_b.txt", safeName("a\u0000b.txt"))
        assertEquals("照片.jpg", safeName(" 照片.jpg "))
        assertEquals("archive.tar (2).gz", availableName("archive.tar.gz", setOf("archive.tar.gz", "archive.tar (1).gz")))
        assertEquals("README (1)", availableName("README", setOf("README")))
    }

    @Test
    fun directoryGrantsAndErrorsAreNotPortableSettings() {
        assertFalse(DownloadOptions.directory.backedUp)
        assertFalse(DownloadOptions.error.backedUp)
        assertFalse(DownloadOptions.enabled.default)
    }

    @Test
    fun longUnicodeNamesKeepTheirExtensionAndFitTheFilesystem() {
        listOf("文".repeat(100), "😀".repeat(100)).forEach { stem ->
            val name = safeName("$stem.pdf")
            assertTrue(name.endsWith(".pdf"))
            assertTrue(name.toByteArray(Charsets.UTF_8).size <= 180)
            assertEquals(name, name.toByteArray(Charsets.UTF_8).toString(Charsets.UTF_8))
        }
    }
}
