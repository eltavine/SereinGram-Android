package com.eltavine.sereingram.features.history

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.ChatMenuHooks
import com.eltavine.sereingram.hooks.HistoryHooks
import com.eltavine.sereingram.hooks.MessageHooks
import com.eltavine.sereingram.hooks.MessageMenuHooks
import com.eltavine.sereingram.ports.HistoryStore
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import java.io.File
import java.util.concurrent.Executors

/**
 * Saves deleted messages and earlier versions of edited ones, after AyuGram's
 * message history; copies of their media go under [mediaRoot].
 */
class HistoryFeature(
    private val mediaRoot: File,
    private val stores: (account: Int) -> HistoryStore,
) : SereinModule, SettingsContributor {
    override val id: String = "history"

    override val options: List<Option<*>> = HistoryOptions.all

    private val writer = Executors.newSingleThreadExecutor { Thread(it, "serein-history") }
    private val kept = MessageMap<Long>()
    private val revised = MessageSet()
    private val deletedByUser = MessageSet()
    private lateinit var mediaBackups: MediaBackups

    override fun start(context: ModuleContext) {
        val backups = MediaBackups(mediaRoot, context.options)
        mediaBackups = backups
        val recorder = HistoryRecorder(context.options, stores, writer, revised, deletedByUser, backups)
        HistoryHooks.userDeletionListeners.install(recorder::beforeUserDeletes)
        HistoryHooks.deletionListeners.install(recorder::beforeDeleted)
        HistoryHooks.editListeners.install(recorder::beforeEdited)
        val restorer = HistoryRestorer(context.options, stores, kept, revised, backups)
        HistoryHooks.loadListeners.install(restorer::afterLoaded)
        HistoryHooks.chatKeepers.install(KeptInChat(context.options, kept, deletedByUser))
        HistoryHooks.fileKeepers.install { account, _, _ -> context.options.get(HistoryOptions.saveDeleted, account) }
        MessageHooks.timeDecorators.install(restorer::decorateTime)
        MessageMenuHooks.entries.install(EditHistoryEntry(stores, revised))
        MessageMenuHooks.entries.install(DeletedAtEntry(kept))
        ChatMenuHooks.entries.install(DeletedMessagesEntry(context.options, stores, backups))
    }

    override fun forgetAccount(account: Int) {
        kept.forget(account)
        revised.forget(account)
        deletedByUser.forget(account)
        writer.execute {
            Faults.guard("history clear", fallback = Unit) { stores(account).clearAll() }
            Faults.guard("media backups clear", fallback = Unit) { mediaBackups.forgetAccount(account) }
        }
    }

    override val settingsIcon: Int = R.drawable.msg_recent

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_history_title,
        listOf(
            SettingsSection(
                header = R.string.serein_history_save,
                rows = listOf(
                    SettingsRow.Toggle(HistoryOptions.saveDeleted, R.string.serein_history_save_deleted),
                    SettingsRow.Toggle(HistoryOptions.saveEdits, R.string.serein_history_save_edits),
                    SettingsRow.Toggle(HistoryOptions.saveInBotChats, R.string.serein_history_save_in_bots),
                ),
                note = R.string.serein_history_save_note,
            ),
            SettingsSection(
                rows = listOf(
                    SettingsRow.Screen(R.string.serein_history_deleted_all, {
                        val account = UserConfig.selectedAccount
                        DeletedChatsActivity(stores(account)) { dialogId -> mediaBackups.forgetChat(account, dialogId) }
                    }),
                ),
            ),
            SettingsSection(
                header = R.string.serein_history_media,
                rows = listOf(
                    SettingsRow.Toggle(HistoryOptions.backupMedia, R.string.serein_history_backup_media),
                    SettingsRow.Toggle(HistoryOptions.backupInPrivateChats, R.string.serein_history_backup_private),
                    SettingsRow.Toggle(HistoryOptions.backupInGroups, R.string.serein_history_backup_groups),
                    SettingsRow.Toggle(HistoryOptions.backupInChannels, R.string.serein_history_backup_channels),
                ),
                note = R.string.serein_history_backup_note,
            ),
            SettingsSection(
                header = R.string.serein_history_appearance,
                rows = listOf(
                    SettingsRow.Text(HistoryOptions.deletedMark, R.string.serein_history_deleted_mark_title, R.string.serein_history_deleted_mark),
                ),
                note = R.string.serein_history_deleted_mark_note,
            ),
        ),
    )
}
