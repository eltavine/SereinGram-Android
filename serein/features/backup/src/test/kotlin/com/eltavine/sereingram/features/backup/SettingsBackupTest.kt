package com.eltavine.sereingram.features.backup

import com.eltavine.sereingram.core.KeyValueStore
import com.eltavine.sereingram.core.MemoryKeyValueStore
import com.eltavine.sereingram.core.OptionScope
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.core.booleanOption
import com.eltavine.sereingram.core.intOption
import com.eltavine.sereingram.core.longOption
import com.eltavine.sereingram.core.textOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SettingsBackupTest {
    private val flag = booleanOption("some_flag")
    private val count = intOption("some_count", default = 3)
    private val since = longOption("some_since")
    private val mark = textOption("some_mark")
    private val perAccount = booleanOption("some_account_flag", scope = OptionScope.ACCOUNT)
    private val key = textOption("some_api_key", secret = true)
    private val address = textOption("some_api_address", backedUp = false)
    private val all = listOf(flag, count, since, mark, perAccount, key, address)

    private fun options(): Options {
        val stores = HashMap<Pair<OptionScope, Int>, KeyValueStore>()
        return Options { scope, account -> stores.getOrPut(scope to account) { MemoryKeyValueStore() } }
    }

    // A user may sit in another of the app's accounts on another phone.
    @Test
    fun aBackupBringsBackWhatWasChanged() {
        val source = options().apply {
            set(flag, true)
            set(count, 7)
            set(since, 1_700_000_000_000)
            set(mark, "🗑")
            set(perAccount, true, account = 2)
        }
        val document = SettingsBackup.write(source, all, account = 2, user = USER)
        val target = options().apply { set(count, 9) }
        val result = assertIs<Restore.Done>(SettingsBackup.restore(target, all, account = 0, user = USER, document))
        assertEquals(5, result.changed)
        assertEquals(0, result.skipped)
        assertFalse(result.fromAnotherUser)
        assertTrue(target.get(flag))
        assertEquals(7, target.get(count))
        assertEquals(1_700_000_000_000, target.get(since))
        assertEquals("🗑", target.get(mark))
        assertTrue(target.get(perAccount, account = 0))
    }

    @Test
    fun anotherUsersBackupLeavesTheSettingsOfTheAccountAsTheyAre() {
        val document = SettingsBackup.write(options().apply { set(flag, true) }, all, account = 0, user = USER)
        val target = options().apply {
            set(count, 9)
            set(perAccount, true, account = 0)
        }
        val result = assertIs<Restore.Done>(SettingsBackup.restore(target, all, account = 0, user = USER + 1, document))
        assertTrue(result.fromAnotherUser)
        assertEquals(2, result.changed)
        assertTrue(target.get(flag))
        assertEquals(3, target.get(count))
        assertTrue(target.get(perAccount, account = 0))
    }

    @Test
    fun backupsThatDoNotSayWhoseTheyAreGoToAnyAccount() {
        val document = """{"format":"sereingram-settings","version":1,"device":{},"account":{"some_account_flag":true}}"""
        val target = options()
        val result = assertIs<Restore.Done>(SettingsBackup.restore(target, all, account = 0, user = USER, document))
        assertFalse(result.fromAnotherUser)
        assertTrue(target.get(perAccount, account = 0))
    }

    @Test
    fun optionsLeftAtTheirDefaultsAreResetAndOthersAccountsUntouched() {
        val document = SettingsBackup.write(options(), all, account = 0, user = USER)
        val target = options().apply {
            set(flag, true)
            set(perAccount, true, account = 0)
            set(perAccount, true, account = 1)
        }
        assertIs<Restore.Done>(SettingsBackup.restore(target, all, account = 0, user = USER, document))
        assertFalse(target.isModified(flag))
        assertFalse(target.get(perAccount, account = 0))
        assertTrue(target.get(perAccount, account = 1))
    }

    @Test
    fun optionsAddedSinceABackupWasWrittenAreLeftAsTheyAre() {
        val olderRelease = listOf(flag, count)
        val document = SettingsBackup.write(options().apply { set(count, 7) }, olderRelease, account = 0, user = USER)
        val target = options().apply {
            set(flag, true)
            set(mark, "🗑")
            set(perAccount, true, account = 0)
        }
        val result = assertIs<Restore.Done>(SettingsBackup.restore(target, all, account = 0, user = USER, document))
        assertEquals(2, result.changed)
        assertFalse(target.get(flag))
        assertEquals(7, target.get(count))
        assertEquals("🗑", target.get(mark))
        assertTrue(target.get(perAccount, account = 0))
    }

    @Test
    fun secretsStayOutOfBackupsAndAreKeptOnRestore() {
        val source = options().apply { set(key, "sk-source") }
        val document = SettingsBackup.write(source, all, account = 0, user = USER)
        assertFalse("sk-source" in document)
        val target = options().apply { set(key, "sk-target") }
        SettingsBackup.restore(target, all, account = 0, user = USER, document)
        assertEquals("sk-target", target.get(key))
    }

    @Test
    fun optionsThatAreNotBackedUpStayOutAndAreKept() {
        val source = options().apply { set(address, "https://elsewhere.example") }
        val document = SettingsBackup.write(source, all, account = 0, user = USER)
        assertFalse("elsewhere" in document)
        val target = options().apply { set(address, "https://mine.example") }
        SettingsBackup.restore(target, all, account = 0, user = USER, document)
        assertEquals("https://mine.example", target.get(address))
    }

    @Test
    fun unknownKeysAndValuesOfTheWrongKindAreSkipped() {
        val document = """
            {"format":"sereingram-settings","version":1,
             "device":{"some_flag":"yes","some_count":5,"from_a_later_release":true},
             "account":{}}
        """.trimIndent()
        val target = options().apply { set(flag, true) }
        val result = assertIs<Restore.Done>(SettingsBackup.restore(target, all, account = 0, user = USER, document))
        assertEquals(2, result.skipped)
        assertTrue(target.get(flag))
        assertEquals(5, target.get(count))
    }

    @Test
    fun otherFilesAreRefused() {
        assertIs<Restore.NotABackup>(SettingsBackup.restore(options(), all, 0, USER, "not json"))
        assertIs<Restore.NotABackup>(SettingsBackup.restore(options(), all, 0, USER, """{"format":"nekox","version":1}"""))
        val later = """{"format":"sereingram-settings","version":3}"""
        assertEquals(3, assertIs<Restore.TooNew>(SettingsBackup.restore(options(), all, 0, USER, later)).version)
    }

    private companion object {
        const val USER = 777_000_123L
    }
}
