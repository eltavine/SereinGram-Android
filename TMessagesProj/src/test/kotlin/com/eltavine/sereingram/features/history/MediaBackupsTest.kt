package com.eltavine.sereingram.features.history

import android.app.Application
import com.eltavine.sereingram.core.MemoryKeyValueStore
import com.eltavine.sereingram.core.Options
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.telegram.tgnet.TLRPC
import java.io.File
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MediaBackupsTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val options = Options { _, _ -> MemoryKeyValueStore() }
    private val restored = mutableListOf<String>()
    private val cache by lazy { folder.newFolder("cache") }
    private val backups by lazy {
        MediaBackups(
            folder.newFolder("backups"),
            options,
            io = Executor(Runnable::run),
            cachePath = { _, message -> File(cache, "${message.id}.jpg") },
            kindOf = { _, dialogId -> if (dialogId > 0) ChatKind.PRIVATE else ChatKind.CHANNEL },
            restored = { _, message, file -> restored += "${message.id}:${file.name}" },
        )
    }

    private fun photo(id: Int) = TLRPC.TL_message().apply {
        this.id = id
        media = TLRPC.TL_messageMediaPhoto()
    }

    private fun cached(id: Int, content: String = "photo $id") = File(cache, "$id.jpg").apply { writeText(content) }

    @Test
    fun aBackedUpPhotoComesBackAfterTheCacheLosesIt() {
        val original = cached(5)
        backups.backUp(0, 42, photo(5))
        original.delete()
        backups.restore(0, 42, photo(5))
        assertEquals("photo 5", original.readText())
        assertEquals("Telegram hears that the media is back", listOf("5:5.jpg"), restored)
        backups.restore(0, 42, photo(5))
        assertEquals("media Telegram still has is left alone", listOf("5:5.jpg"), restored)
        assertFalse(File(cache, "5.jpg.part").exists())
    }

    @Test
    fun channelsAndMessagesWithoutMediaAreNotBackedUp() {
        val inChannel = cached(6)
        backups.backUp(0, -100, photo(6))
        inChannel.delete()
        backups.restore(0, -100, photo(6))
        assertFalse(inChannel.exists())
        val text = TLRPC.TL_message().apply { id = 7 }
        cached(7)
        backups.backUp(0, 42, text)
        File(cache, "7.jpg").delete()
        backups.restore(0, 42, photo(7))
        assertFalse(File(cache, "7.jpg").exists())
    }

    @Test
    fun forgottenBackupsAreGone() {
        cached(8)
        cached(9)
        backups.backUp(0, 42, photo(8))
        backups.backUp(0, 42, photo(9))
        backups.forget(0, 42, listOf(8))
        backups.forgetChat(1, 42)
        File(cache, "8.jpg").delete()
        File(cache, "9.jpg").delete()
        backups.restore(0, 42, photo(8))
        backups.restore(0, 42, photo(9))
        assertFalse(File(cache, "8.jpg").exists())
        assertTrue(File(cache, "9.jpg").exists())
        backups.forgetChat(0, 42)
        File(cache, "9.jpg").delete()
        backups.restore(0, 42, photo(9))
        assertFalse(File(cache, "9.jpg").exists())
    }
}
