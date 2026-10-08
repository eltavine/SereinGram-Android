package com.eltavine.sereingram.features.localnames

import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.ChatMenuHooks
import com.eltavine.sereingram.hooks.PeerHooks
import com.eltavine.sereingram.ports.LocalNameStore
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.MessagesController
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.messenger.Utilities
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.SerializedData
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.Vector

/**
 * Names for people and chats that only this device shows, after NagramX's
 * local renaming: Telegram's users and chats get them as they are put into
 * memory, so every screen shows them without asking for them. Whatever leaves
 * the device or goes into Telegram's database gets Telegram's names back.
 */
class LocalNamesFeature(stores: (account: Int) -> LocalNameStore) : SereinModule, SettingsContributor {
    override val id: String = "local_names"

    override val options: List<Option<*>> = emptyList()

    internal val names = LocalNames(stores)
    internal val originals = OriginalNames()

    internal val users = object : PeerHooks.UserRewriter {
        override fun beforePut(account: Int, user: Any) = rename(account, user as TLRPC.User)

        override fun original(account: Int, user: Any): Any {
            val shown = user as TLRPC.User
            val name = originals.behind(account, shown.id, nameOf(shown)) ?: return user
            return shown.copied { TLRPC.User.TLdeserialize(it, it.readInt32(true), true) }.apply {
                first_name = name.first
                last_name = name.last
            }
        }
    }

    internal val chats = object : PeerHooks.ChatRewriter {
        override fun beforePut(account: Int, chat: Any) = rename(account, chat as TLRPC.Chat)

        override fun original(account: Int, chat: Any): Any {
            val shown = chat as TLRPC.Chat
            val name = originals.behind(account, -shown.id, PeerName(shown.title.orEmpty())) ?: return chat
            return shown.copied { TLRPC.Chat.TLdeserialize(it, it.readInt32(true), true) }.apply { title = name.first }
        }
    }

    override fun start(context: ModuleContext) {
        PeerHooks.userRewriters.install(users)
        PeerHooks.chatRewriters.install(chats)
        ChatMenuHooks.entries.install(LocalNameEntry(this))
        // Reads the names before Telegram puts its first users, which can happen on the UI thread.
        Utilities.globalQueue.postRunnable {
            for (account in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
                names.of(account, 0)
            }
        }
    }

    private fun rename(account: Int, user: TLRPC.User) {
        val local = names.of(account, user.id) ?: return
        val shown = originals.replace(account, user.id, nameOf(user), local) ?: return
        user.first_name = shown.first
        user.last_name = shown.last
    }

    private fun rename(account: Int, chat: TLRPC.Chat) {
        val local = names.of(account, -chat.id) ?: return
        val shown = originals.replace(account, -chat.id, PeerName(chat.title.orEmpty()), local) ?: return
        chat.title = shown.first
    }

    /** Sets or removes a local name and shows the result at once, with Telegram's name back when it is removed. */
    internal fun set(account: Int, peerId: Long, name: String?) {
        val controller = MessagesController.getInstance(account)
        val kept = names.set(account, peerId, name)
        val original = originals.of(account, peerId)
        if (kept == null) {
            originals.forget(account, peerId)
        }
        if (peerId > 0) {
            controller.getUser(peerId)?.let { user ->
                when {
                    kept != null -> rename(account, user)
                    original != null -> {
                        user.first_name = original.first
                        user.last_name = original.last
                    }
                    else -> refetchUser(account, peerId)
                }
            }
        } else {
            controller.getChat(-peerId)?.let { chat ->
                when {
                    kept != null -> rename(account, chat)
                    original != null -> chat.title = original.first
                    else -> controller.loadFullChat(-peerId, 0, true)
                }
            }
        }
        refresh(account)
        AndroidUtilities.runOnUIThread({ refresh(account) }, REFETCH_DELAY)
    }

    // Telegram's name is unknown when the user only ever showed the local name, so it is fetched again.
    private fun refetchUser(account: Int, userId: Long) {
        val controller = MessagesController.getInstance(account)
        val input = controller.getInputUser(userId) ?: return
        val request = TLRPC.TL_users_getUsers().apply { id.add(input) }
        ConnectionsManager.getInstance(account).sendRequest(request) { response, _ ->
            val users = (response as? Vector<*>)?.objects?.filterIsInstance<TLRPC.User>().orEmpty()
            if (users.isNotEmpty()) {
                AndroidUtilities.runOnUIThread {
                    controller.putUsers(ArrayList(users), false)
                    MessagesStorage.getInstance(account).putUsersAndChats(users, null, false, true)
                    refresh(account)
                }
            }
        }
    }

    private fun refresh(account: Int) {
        val mask = MessagesController.UPDATE_MASK_NAME or MessagesController.UPDATE_MASK_CHAT_NAME
        NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.updateInterfaces, mask)
    }

    override val settingsIcon: Int = R.drawable.msg_edit

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_local_names_title,
        listOf(
            SettingsSection(
                rows = listOf(SettingsRow.Screen(R.string.serein_local_names_all, { LocalNamesActivity(this) })),
                note = R.string.serein_local_names_note,
            ),
        ),
    )

    private companion object {
        const val REFETCH_DELAY = 2_000L
    }
}

private fun nameOf(user: TLRPC.User) = PeerName(user.first_name.orEmpty(), user.last_name.orEmpty())

/** A copy of a Telegram object, written and read the way Telegram stores one. */
private fun <T : TLObject> T.copied(read: (SerializedData) -> T): T {
    val written = SerializedData()
    val bytes = try {
        serializeToStream(written)
        written.toByteArray()
    } finally {
        written.cleanup()
    }
    val reader = SerializedData(bytes)
    return try {
        read(reader)
    } finally {
        reader.cleanup()
    }
}
