package com.eltavine.sereingram.features.backup

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.LocaleController.formatString
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.messenger.Utilities
import org.telegram.ui.ActionBar.BaseFragment
import okio.buffer
import okio.source
import org.telegram.ui.Components.BulletinFactory
import java.io.File

/** SereinGram's settings of the device and the current account as a file to keep, and back. */
object BackupSettings {
    fun section(options: Options, all: List<Option<*>>): SettingsSection = SettingsSection(
        header = R.string.serein_backup_header,
        rows = listOf(
            SettingsRow.Action(R.string.serein_backup_export) { page -> export(page, options, all) },
            SettingsRow.PickFile(R.string.serein_backup_import, MIME_TYPES) { page, uri -> import(page, options, all, uri) },
        ),
        note = R.string.serein_backup_note,
    )

    private fun export(page: BaseFragment, options: Options, all: List<Option<*>>) {
        val context = page.parentActivity ?: return
        val account = page.currentAccount
        Utilities.globalQueue.postRunnable {
            val file = Faults.guard("settings backup", fallback = null) {
                File(context.cacheDir, "media/sereingram-settings.json").apply {
                    parentFile?.mkdirs()
                    writeText(SettingsBackup.write(options, all, account))
                }
            } ?: return@postRunnable
            AndroidUtilities.runOnUIThread {
                val uri = FileProvider.getUriForFile(context, ApplicationLoader.getApplicationId() + ".provider", file)
                val share = Intent(Intent.ACTION_SEND)
                    .setType("application/json")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(Intent.createChooser(share, getString(R.string.serein_backup_export)))
            }
        }
    }

    private fun import(page: BaseFragment, options: Options, all: List<Option<*>>, uri: Uri) {
        val context = page.parentActivity ?: return
        val account = page.currentAccount
        Utilities.globalQueue.postRunnable {
            val result = runCatching {
                val document = context.contentResolver.openInputStream(uri)?.source()?.buffer()?.use { file ->
                    if (file.request(MAX_BYTES + 1L)) null else file.readUtf8()
                }
                if (document == null) Restore.NotABackup else SettingsBackup.restore(options, all, account, document)
            }
            AndroidUtilities.runOnUIThread {
                val message = result.fold(
                    onSuccess = { restore ->
                        when (restore) {
                            is Restore.Done -> formatString(R.string.serein_backup_restored, restore.changed, restore.skipped)
                            is Restore.NotABackup -> getString(R.string.serein_backup_not_a_backup)
                            is Restore.TooNew -> getString(R.string.serein_backup_too_new)
                        }
                    },
                    onFailure = { error ->
                        Faults.report("settings backup", error)
                        formatString(R.string.serein_backup_failed, error.message.orEmpty())
                    },
                )
                val done = result.getOrNull() is Restore.Done
                BulletinFactory.of(page).createSimpleBulletin(if (done) R.raw.done else R.raw.error, message).show()
            }
        }
    }

    // Pickers label JSON files differently, and some apps share them as plain text.
    private val MIME_TYPES = listOf("application/json", "text/plain", "application/octet-stream")

    // A backup takes a few kilobytes; anything far larger is some other file.
    private const val MAX_BYTES = 1 shl 20
}
