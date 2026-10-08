package com.eltavine.sereingram.features.history

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Options
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ChatObject
import org.telegram.messenger.DialogObject
import org.telegram.messenger.FileLoader
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.tgnet.TLRPC
import java.io.File
import java.util.concurrent.Executor

/**
 * Copies of the photos and files of kept deleted messages under [root], one
 * folder per account, so that they outlive the cleaning of Telegram's cache.
 * Copies back into the cache happen on [io]; [cachePath] is where Telegram
 * keeps a message's media, [kindOf] the kind of a chat, and [restored] tells
 * Telegram that a message's media is there again.
 */
internal class MediaBackups(
    private val root: File,
    private val options: Options,
    private val io: Executor,
    private val cachePath: (account: Int, message: TLRPC.Message) -> File? = { account, message ->
        FileLoader.getInstance(account).getPathToMessage(message)
    },
    private val kindOf: (account: Int, dialogId: Long) -> ChatKind? = ::telegramChatKind,
    private val restored: (account: Int, message: TLRPC.Message, file: File) -> Unit = ::announce,
) {
    /** Copies the media of [message], which Telegram leaves in its cache once it is deleted. */
    fun backUp(account: Int, dialogId: Long, message: TLRPC.Message) {
        val source = cached(account, message) ?: return
        if (!policy(account).backsUp(kindOf(account, dialogId), source.length())) {
            return
        }
        val target = file(account, dialogId, message.id)
        if (!target.exists()) {
            copyWhole(source, target)
        }
    }

    /**
     * Puts the copy back where Telegram looks for the media, when its own one is
     * gone. The copy happens later on [io], since files may be large and history
     * loads on a queue all of Telegram's storage waits for.
     */
    fun restore(account: Int, dialogId: Long, message: TLRPC.Message) {
        if (!hasMedia(message)) {
            return
        }
        io.execute {
            Faults.guard("history media restore", fallback = Unit) {
                val target = cachePath(account, message) ?: return@guard
                val backup = file(account, dialogId, message.id)
                if (!target.exists() && backup.exists()) {
                    copyWhole(backup, target)
                    restored(account, message, target)
                }
            }
        }
    }

    fun forget(account: Int, dialogId: Long, messageIds: Collection<Int>) {
        messageIds.forEach { file(account, dialogId, it).delete() }
    }

    fun forgetChat(account: Int, dialogId: Long) {
        folder(account).listFiles { file -> isBackupOf(file.name, dialogId) }?.forEach(File::delete)
    }

    fun forgetAccount(account: Int) {
        folder(account).deleteRecursively()
    }

    private fun policy(account: Int) = MediaBackupPolicy(
        enabled = options.get(HistoryOptions.backupMedia, account),
        kinds = buildSet {
            if (options.get(HistoryOptions.backupInPrivateChats, account)) add(ChatKind.PRIVATE)
            if (options.get(HistoryOptions.backupInGroups, account)) add(ChatKind.GROUP)
            if (options.get(HistoryOptions.backupInChannels, account)) add(ChatKind.CHANNEL)
        },
    )

    private fun cached(account: Int, message: TLRPC.Message): File? =
        if (hasMedia(message)) cachePath(account, message)?.takeIf(File::isFile) else null

    // Link previews and the like are not the message's own media.
    private fun hasMedia(message: TLRPC.Message): Boolean =
        message.media is TLRPC.TL_messageMediaPhoto || message.media is TLRPC.TL_messageMediaDocument

    // A copy cut short leaves nothing behind that looks whole, so it is made again next time.
    private fun copyWhole(source: File, target: File) {
        val partial = File(target.path + ".part")
        source.copyTo(partial, overwrite = true)
        if (!partial.renameTo(target)) {
            partial.delete()
        }
    }

    private fun folder(account: Int) = File(root, account.toString())

    private fun file(account: Int, dialogId: Long, messageId: Int) = File(folder(account), backupName(dialogId, messageId))
}

private fun telegramChatKind(account: Int, dialogId: Long): ChatKind? = when {
    DialogObject.isEncryptedDialog(dialogId) -> null
    dialogId > 0 -> ChatKind.PRIVATE
    ChatObject.isChannelAndNotMegaGroup(MessagesController.getInstance(account).getChat(-dialogId)) -> ChatKind.CHANNEL
    else -> ChatKind.GROUP
}

// What Telegram announces when a download ends, so the message shows its media.
private fun announce(account: Int, message: TLRPC.Message, file: File) {
    val name = FileLoader.getMessageFileName(message)
    AndroidUtilities.runOnUIThread {
        NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.fileLoaded, name, file)
    }
}
