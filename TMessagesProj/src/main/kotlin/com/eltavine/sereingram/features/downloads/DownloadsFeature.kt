package com.eltavine.sereingram.features.downloads

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.DownloadHooks
import com.eltavine.sereingram.settings.SettingsCategory
import com.eltavine.sereingram.settings.SettingsCondition
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import com.eltavine.sereingram.settings.SettingsTint
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.DialogObject
import org.telegram.messenger.FileLoader
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.MessageObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.Components.BulletinFactory
import org.telegram.ui.LaunchActivity
import org.telegram.ui.ChatActivity
import org.telegram.ui.Cells.ChatMessageCell
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Publishes completed manual downloads through a user-granted document tree. */
class DownloadsFeature(private val application: Application) : SereinModule, SettingsContributor {
    override val id: String = "downloads"
    override val options: List<Option<*>> = DownloadOptions.all
    private lateinit var preferences: Options
    private lateinit var notices: DownloadNotices
    private val io by lazy { Executors.newSingleThreadExecutor { Thread(it, "serein-downloads") } }
    private val exports by lazy { DirectoryExports(application) }

    override fun start(context: ModuleContext) {
        preferences = context.options
        notices = DownloadNotices(application)
        val wasEnabled = AtomicBoolean(preferences.get(DownloadOptions.enabled))
        preferences.addListener { option, _ ->
            if (option == DownloadOptions.enabled) {
                val enabled = preferences.get(DownloadOptions.enabled)
                if (!wasEnabled.getAndSet(enabled) && enabled) notices.enabled()
            }
        }
        DownloadHooks.completedDownloads.install { account, parent, file ->
            val message = parent as? MessageObject ?: return@install
            if (!eligible(message) || !preferences.get(DownloadOptions.enabled)) return@install
            val tree = preferences.get(DownloadOptions.directory).takeIf(String::isNotEmpty) ?: return@install
            val document = message.document ?: return@install
            val identity = "${UserConfig.getInstance(account).clientUserId}:${document.dc_id}:${document.id}"
            val name = FileLoader.getDocumentFileName(document).ifBlank { file.name }
            val mime = document.mime_type?.takeIf { it.contains('/') } ?: "application/octet-stream"
            io.execute {
                // Switching off or choosing another directory also cancels queued exports.
                if (!preferences.get(DownloadOptions.enabled) || preferences.get(DownloadOptions.directory) != tree) return@execute
                runCatching { exports.save(tree, identity, file, name, mime) }
                    .onSuccess {
                        preferences.reset(DownloadOptions.error)
                        notices.completed(account, tree)
                        AndroidUtilities.runOnUIThread {
                            if (!file.exists()) {
                                clearCachedState(message)
                                val page = LaunchActivity.getLastFragmentIncludeMainTabs() as? ChatActivity
                                val list = page?.takeIf { it.currentAccount == account && it.dialogId == message.dialogId }?.getChatListView()
                                if (list != null) {
                                    for (index in 0 until list.childCount) {
                                        val cell = list.getChildAt(index) as? ChatMessageCell ?: continue
                                        val shown = cell.messageObject ?: continue
                                        if (shown.id == message.id && shown.dialogId == message.dialogId) {
                                            clearCachedState(shown)
                                            cell.updateButtonState(false, true, true)
                                        }
                                    }
                                }
                                NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.onDownloadingFilesChanged)
                            }
                        }
                    }
                    .onFailure { error ->
                        Faults.report("external download", error)
                        preferences.set(DownloadOptions.error, tree)
                        AndroidUtilities.runOnUIThread {
                            LaunchActivity.getLastFragment()?.let { page ->
                                BulletinFactory.of(page).createSimpleBulletin(R.raw.error, getString(R.string.serein_downloads_failed)).show()
                            }
                        }
                    }
            }
        }
    }

    override val settingsIcon: Int = R.drawable.msg_download
    override val settingsCategory: SettingsCategory = SettingsCategory.MEDIA
    override val settingsTint: SettingsTint = SettingsTint.BROWN
    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_downloads_title,
        summary = R.string.serein_downloads_summary,
        status = { state ->
            when {
                state[DownloadOptions.error].isNotEmpty() ||
                    (state[DownloadOptions.enabled] && state[DownloadOptions.directory].isEmpty()) -> getString(R.string.serein_downloads_attention)
                state[DownloadOptions.enabled] -> getString(R.string.serein_downloads_on)
                else -> getString(R.string.serein_downloads_off)
            }
        },
        sections = listOf(
            SettingsSection(
                rows = listOf(
                    SettingsRow.PickDirectory(
                        R.string.serein_downloads_directory,
                        picked = { host, uri, flags -> choose(host as BaseFragment, Uri.parse(uri), flags) },
                        value = { state -> directoryLabel(state[DownloadOptions.directory]) },
                        summary = R.string.serein_downloads_directory_info,
                        icon = R.drawable.msg_folders,
                    ),
                    SettingsRow.Toggle(
                        DownloadOptions.enabled,
                        R.string.serein_downloads_enabled,
                        summary = R.string.serein_downloads_enabled_info,
                        enabledWhen = SettingsCondition { it[DownloadOptions.directory].isNotEmpty() },
                    ),
                    SettingsRow.Action(
                        R.string.serein_downloads_reset,
                        run = { reset() },
                        summary = R.string.serein_downloads_reset_info,
                        shownWhen = SettingsCondition { it[DownloadOptions.directory].isNotEmpty() },
                    ),
                ),
                note = R.string.serein_downloads_note,
            ),
            SettingsSection(
                rows = listOf(
                    SettingsRow.Action(
                        R.string.serein_downloads_attention,
                        run = { host ->
                            BulletinFactory.of(host as BaseFragment)
                                .createSimpleBulletin(R.raw.error, getString(R.string.serein_downloads_failed)).show()
                        },
                        summary = R.string.serein_downloads_failed,
                    ),
                ),
                shownWhen = SettingsCondition { it[DownloadOptions.error].isNotEmpty() },
            ),
        ),
    )

    private fun choose(page: BaseFragment, uri: Uri, flags: Int) {
        io.execute {
            val before = preferences.get(DownloadOptions.directory)
            val alreadyEnabled = preferences.get(DownloadOptions.enabled)
            val result = runCatching {
                require(DocumentsContract.isTreeUri(uri))
                val access = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                require(access == (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
                application.contentResolver.takePersistableUriPermission(uri, access)
                try {
                    val directory = DocumentFile.fromTreeUri(application, uri)
                    require(directory != null && directory.isDirectory && directory.canRead() && directory.canWrite())
                } catch (error: Exception) {
                    if (before != uri.toString()) release(uri)
                    throw error
                }
                preferences.set(DownloadOptions.directory, uri.toString())
                preferences.set(DownloadOptions.enabled, true)
                preferences.reset(DownloadOptions.error)
                if (before.isNotEmpty() && before != uri.toString()) release(Uri.parse(before))
            }
            AndroidUtilities.runOnUIThread {
                // Enabling already schedules the explanatory dialog through the options listener.
                if (result.isSuccess && !alreadyEnabled) return@runOnUIThread
                val text = getString(if (result.isSuccess) R.string.serein_downloads_selected else R.string.serein_downloads_invalid_directory)
                BulletinFactory.of(page).createSimpleBulletin(if (result.isSuccess) R.raw.done else R.raw.error, text).show()
            }
            result.exceptionOrNull()?.let { Faults.report("download directory", it) }
        }
    }

    private fun reset() {
        preferences.set(DownloadOptions.enabled, false)
        val before = preferences.get(DownloadOptions.directory)
        preferences.reset(DownloadOptions.directory)
        preferences.reset(DownloadOptions.error)
        // Let a copy already using this grant finish before releasing it.
        if (before.isNotEmpty()) io.execute { release(Uri.parse(before)) }
    }

    private fun release(uri: Uri) {
        runCatching {
            application.contentResolver.releasePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
    }

    internal companion object {
        // Refresh the download button without rebinding images and immediately auto-downloading
        // the file we just removed. An explicit tap enables downloading again in Telegram's cell.
        private fun clearCachedState(message: MessageObject) {
            message.checkMediaExistance()
            message.loadedFileSize = 0
            message.putInDownloadsStore = false
            message.loadingCancelled = true
        }

        fun eligible(message: MessageObject): Boolean {
            val owner = message.messageOwner ?: return false
            return message.putInDownloadsStore && !DialogObject.isEncryptedDialog(message.dialogId) &&
                owner.ttl == 0 && (MessageObject.getMedia(owner)?.ttl_seconds ?: 0) == 0 &&
                !owner.noforwards && !MessagesController.getInstance(message.currentAccount).isPeerNoForwards(message.dialogId) &&
                !message.isAnyKindOfSticker
        }

        fun directoryLabel(tree: String): String = if (tree.isEmpty()) {
            getString(R.string.serein_downloads_no_directory)
        } else {
            runCatching {
                DocumentsContract.getTreeDocumentId(Uri.parse(tree)).removePrefix("primary:").replace(':', '/').ifEmpty { tree }
            }.getOrDefault(tree)
        }
    }
}
