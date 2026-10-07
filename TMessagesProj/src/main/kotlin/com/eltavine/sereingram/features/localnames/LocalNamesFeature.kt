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
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.Vector

/**
 * Names for people and chats that only this device shows, after NagramX's
 * local renaming: Telegram's users and chats get them as they are put into
 * memory, so every screen shows them without asking for them.
 */
class LocalNamesFeature(stores: (account: Int) -> LocalNameStore) : SereinModule, SettingsContributor {
    override val id: String = "local_names"

    override val options: List<Option<*>> = emptyList()

    internal val names = LocalNames(stores)
    internal val originals = OriginalNames()

    override fun start(context: ModuleContext) {
        PeerHooks.userRewriters.install { account, user -> rename(account, user as TLRPC.User) }
        PeerHooks.chatRewriters.install { account, chat -> rename(account, chat as TLRPC.Chat) }
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
        if (user.first_name != local || !user.last_name.isNullOrEmpty()) {
            originals.remember(account, user.id, listOfNotNull(user.first_name, user.last_name).filter { it.isNotBlank() }.joinToString(" "))
            user.first_name = local
            user.last_name = ""
        }
    }

    private fun rename(account: Int, chat: TLRPC.Chat) {
        val local = names.of(account, -chat.id) ?: return
        if (chat.title != local) {
            originals.remember(account, -chat.id, chat.title.orEmpty())
            chat.title = local
        }
    }

    /** Sets or removes a local name and shows the result at once; Telegram's name comes back from its servers. */
    internal fun set(account: Int, peerId: Long, name: String?) {
        val controller = MessagesController.getInstance(account)
        val kept = names.set(account, peerId, name)
        if (peerId > 0) {
            val user = controller.getUser(peerId) ?: return
            if (kept != null) {
                rename(account, user)
            } else {
                originals.of(account, peerId)?.let { user.first_name = it; user.last_name = "" }
                refetchUser(account, peerId)
            }
        } else {
            val chat = controller.getChat(-peerId) ?: return
            if (kept != null) {
                rename(account, chat)
            } else {
                originals.of(account, peerId)?.let { chat.title = it }
                controller.loadFullChat(-peerId, 0, true)
            }
        }
        if (kept == null) {
            originals.forget(account, peerId)
        }
        refresh(account)
        AndroidUtilities.runOnUIThread({ refresh(account) }, REFETCH_DELAY)
    }

    // Telegram may have saved the renamed user to its database, so the fresh one is saved too.
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
