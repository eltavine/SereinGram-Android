package com.eltavine.sereingram.features.history

import com.eltavine.sereingram.core.Options
import org.telegram.messenger.ChatObject
import org.telegram.messenger.DialogObject
import org.telegram.messenger.FileLoader
import org.telegram.messenger.MessagesController
import org.telegram.tgnet.TLRPC
import java.io.File

/**
 * Copies of the photos and files of kept deleted messages under [root], one
 * folder per account, so that they outlive the cleaning of Telegram's cache.
 * [cachePath] is where Telegram keeps a message's media, [kindOf] the kind of a chat.
 */
internal class MediaBackups(
    private val root: File,
    private val options: Options,
    private val cachePath: (account: Int, message: TLRPC.Message) -> File? = { account, message ->
        FileLoader.getInstance(account).getPathToMessage(message)
    },
    private val kindOf: (account: Int, dialogId: Long) -> ChatKind? = ::telegramChatKind,
) {
    /** Copies the media of [message], which Telegram leaves in its cache once it is deleted. */
    fun backUp(account: Int, dialogId: Long, message: TLRPC.Message) {
        val source = cached(account, message) ?: return
        if (!policy(account).backsUp(kindOf(account, dialogId), source.length())) {
            return
        }
        val target = file(account, dialogId, message.id)
        if (!target.exists()) {
            source.copyTo(target, overwrite = true)
        }
    }

    /** Puts the copy back where Telegram looks for the media, when its own one is gone. */
    fun restore(account: Int, dialogId: Long, message: TLRPC.Message) {
        if (!hasMedia(message)) {
            return
        }
        val target = cachePath(account, message) ?: return
        val backup = file(account, dialogId, message.id)
        if (!target.exists() && backup.exists()) {
            backup.copyTo(target, overwrite = true)
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

    private fun folder(account: Int) = File(root, account.toString())

    private fun file(account: Int, dialogId: Long, messageId: Int) = File(folder(account), backupName(dialogId, messageId))
}

private fun telegramChatKind(account: Int, dialogId: Long): ChatKind? = when {
    DialogObject.isEncryptedDialog(dialogId) -> null
    dialogId > 0 -> ChatKind.PRIVATE
    ChatObject.isChannelAndNotMegaGroup(MessagesController.getInstance(account).getChat(-dialogId)) -> ChatKind.CHANNEL
    else -> ChatKind.GROUP
}
