package com.eltavine.sereingram.features.history

import android.content.DialogInterface
import android.content.Intent
import android.view.View
import android.widget.TextView
import androidx.core.content.FileProvider
import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.hooks.ChatMenuHooks
import com.eltavine.sereingram.ports.HistoryRecord
import com.eltavine.sereingram.ports.HistoryStore
import com.eltavine.sereingram.support.Chats
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.LocaleController
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.messenger.Utilities
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.ChatActivity
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import org.telegram.ui.Components.UniversalFragment
import java.io.File
import java.time.ZoneId

/** "Deleted messages" in the menu of a chat, after NagramX's items to view and clear them. */
internal class DeletedMessagesEntry(
    private val options: Options,
    private val stores: (account: Int) -> HistoryStore,
    private val backups: MediaBackups,
) : ChatMenuHooks.Entry {
    override val id: Int = ChatMenuHooks.FIRST_ID + 3

    override val icon: Int = R.drawable.msg_delete

    override fun title(account: Int, dialogId: Long): CharSequence = getString(R.string.serein_history_deleted_list)

    override fun isShown(account: Int, dialogId: Long): Boolean = options.get(HistoryOptions.saveDeleted, account)

    override fun onSelected(account: Int, dialogId: Long, chat: Any) {
        val list = DeletedMessagesActivity(stores(account), dialogId, chat as? ChatActivity) { backups.forgetChat(account, dialogId) }
        (chat as BaseFragment).presentFragment(list)
    }
}

/** The deleted messages kept of one chat, most recent first, and a way to drop them with their media. */
internal class DeletedMessagesActivity(
    private val store: HistoryStore,
    private val dialogId: Long,
    private val chat: ChatActivity?,
    private val forgetMedia: () -> Unit,
) : UniversalFragment() {
    private var records: List<HistoryRecord>? = null

    override fun onFragmentCreate(): Boolean {
        reload()
        return super.onFragmentCreate()
    }

    private fun reload() {
        Utilities.globalQueue.postRunnable {
            val loaded = Faults.guard("deleted messages", fallback = emptyList()) { store.deleted(dialogId, LIMIT) }
            AndroidUtilities.runOnUIThread {
                records = loaded
                listView?.adapter?.update(true)
            }
        }
    }

    // Opened from the list of all chats, the chat's name says more than the list's.
    override fun getTitle(): CharSequence =
        if (chat == null) Chats.name(currentAccount, dialogId) else getString(R.string.serein_history_deleted_list)

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        val shown = records ?: return
        if (shown.isEmpty()) {
            items.add(UItem.asShadow(getString(R.string.serein_history_deleted_list_empty)))
            return
        }
        shown.forEachIndexed { index, record ->
            val text = record.text.ifEmpty { getString(R.string.serein_history_version_no_text) }
            items.add(UItem.asButton(index + 1, text, LocaleController.formatShortDateTime(record.recordedAt / 1000)))
        }
        items.add(UItem.asShadow(getString(R.string.serein_history_deleted_list_note)))
        items.add(UItem.asButton(EXPORT, getString(R.string.serein_history_deleted_export)))
        items.add(UItem.asButton(CLEAR, getString(R.string.serein_history_deleted_clear)).red())
        items.add(UItem.asShadow(null))
    }

    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) {
        when (item.id) {
            EXPORT -> return export()
            CLEAR -> return confirmClear()
        }
        val record = records?.getOrNull(item.id - 1) ?: return
        val origin = chat
        if (origin != null && origin.dialogId == dialogId) {
            finishFragment()
            origin.scrollToMessageId(record.messageId, 0, true, 0, true, 0)
        } else {
            presentFragment(Chats.screen(dialogId, record.messageId))
        }
    }

    override fun onLongClick(item: UItem, view: View, position: Int, x: Float, y: Float): Boolean = false

    /** Shares every kept deleted message of the chat as a text file, after NagramX's export request. */
    private fun export() {
        val context = parentActivity ?: return
        val account = currentAccount
        Utilities.globalQueue.postRunnable {
            val file = Faults.guard("deleted messages export", fallback = null) {
                val chatName = Chats.name(account, dialogId)
                val words = TranscriptWords(
                    title = LocaleController.formatString(R.string.serein_history_deleted_export_title, chatName),
                    deleted = getString(R.string.serein_history_deleted_mark),
                    noText = getString(R.string.serein_history_version_no_text),
                )
                val text = transcript(store.deleted(dialogId, EXPORT_LIMIT), words, { Chats.name(account, it) }, ZoneId.systemDefault())
                File(context.cacheDir, "media/serein_deleted_$dialogId.txt").apply {
                    parentFile?.mkdirs()
                    writeText(text)
                }
            } ?: return@postRunnable
            AndroidUtilities.runOnUIThread {
                val uri = FileProvider.getUriForFile(context, ApplicationLoader.getApplicationId() + ".provider", file)
                val share = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(Intent.createChooser(share, getString(R.string.serein_history_deleted_export)))
            }
        }
    }

    private fun confirmClear() {
        val context = parentActivity ?: return
        val dialog = AlertDialog.Builder(context, resourceProvider)
            .setTitle(getString(R.string.serein_history_deleted_clear))
            .setMessage(getString(R.string.serein_history_deleted_clear_note))
            .setPositiveButton(getString(R.string.serein_history_deleted_clear_confirm)) { _, _ ->
                Utilities.globalQueue.postRunnable {
                    Faults.guard("deleted messages", fallback = Unit) {
                        store.clear(dialogId)
                        forgetMedia()
                    }
                    reload()
                }
            }
            .setNegativeButton(getString(R.string.Cancel), null)
            .create()
        showDialog(dialog)
        (dialog.getButton(DialogInterface.BUTTON_POSITIVE) as? TextView)?.setTextColor(Theme.getColor(Theme.key_text_RedBold))
    }

    private companion object {
        const val LIMIT = 500
        const val EXPORT_LIMIT = 100_000
        const val EXPORT = 100_000
        const val CLEAR = 100_001
    }
}
