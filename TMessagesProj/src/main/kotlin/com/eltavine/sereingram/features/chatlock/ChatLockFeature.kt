package com.eltavine.sereingram.features.chatlock

import android.os.SystemClock
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.eltavine.sereingram.core.DialogIds
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.ChatMenuHooks
import com.eltavine.sereingram.hooks.DialogsHooks
import com.eltavine.sereingram.hooks.NavigationHooks
import com.eltavine.sereingram.hooks.NotificationHooks
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.DialogObject
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.MessagesController
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ChatActivity
import org.telegram.ui.DialogsActivity
import org.telegram.ui.LaunchActivity
import org.telegram.ui.ProfileActivity
import org.telegram.ui.TopicsFragment
import java.util.Collections
import java.util.WeakHashMap

/**
 * Chats, the archive and secret chats that ask for the device's fingerprint,
 * face or screen lock before they open, after Cherrygram's and OctoGram's
 * locks and Swiftgram issue 56. Telegram opens screens through calls that ask
 * [NavigationHooks] first, and its notifications ask [NotificationHooks].
 */
object ChatLockFeature : SereinModule, SettingsContributor {
    override val id: String = "chat_lock"

    override val options: List<Option<*>> = ChatLockOptions.all

    private val window = UnlockWindow { SystemClock.elapsedRealtime() }
    private val passes: MutableSet<Any> = Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap()))

    override fun start(context: ModuleContext) {
        val options = context.options
        NavigationHooks.guards.install { screen, preview, retry -> allows(options, screen as BaseFragment, preview, retry) }
        NavigationHooks.restoreGuards.install { screen -> allowsRestoring(options, screen as BaseFragment) }
        ChatMenuHooks.entries.install(LockEntry(options))
        DialogsHooks.previewReplacers.install { account, dialogId -> lockedPreview(options, account, dialogId) }
        // Notifications are built off the main thread, where the folder of a chat cannot be read safely,
        // and Telegram moves unmuted chats out of the archive when they get a message anyway.
        NotificationHooks.contentPolicies.install { account, dialogId -> isLocked(chat(account, dialogId, archived = false), settings(options, account)) }
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) = window.close()
            },
        )
    }

    // The chat list would otherwise show what a locked chat keeps behind its lock.
    private fun lockedPreview(options: Options, account: Int, dialogId: Long): CharSequence? {
        val target = chat(account, dialogId, archived = isArchived(account, dialogId))
        return getString(R.string.serein_lock_preview).takeIf { isLocked(target, settings(options, account)) && !window.isOpen() }
    }

    private fun chat(account: Int, dialogId: Long, archived: Boolean) =
        LockTarget.Chat(dialogId, secret = DialogObject.isEncryptedDialog(dialogId), archived = archived)

    // Telegram keeps its dialogs for the main thread, which is where screens open and the chat list draws.
    private fun isArchived(account: Int, dialogId: Long): Boolean =
        MessagesController.getInstance(account).dialogs_dict.get(dialogId)?.folder_id == 1

    private fun allows(options: Options, screen: BaseFragment, preview: Boolean, retry: Runnable): Boolean {
        if (passes.remove(screen)) {
            return true
        }
        val target = targetOf(screen) ?: return true
        if (!isLocked(target, settings(options, screen.currentAccount)) || window.isOpen()) {
            return true
        }
        if (preview) {
            return false
        }
        unlocking {
            passes.add(screen)
            retry.run()
        }
        return false
    }

    // Nothing is asked while Telegram recreates its screens, and which chats are archived is not known yet.
    private fun allowsRestoring(options: Options, screen: BaseFragment): Boolean {
        val target = targetOf(screen) ?: return true
        val settings = settings(options, screen.currentAccount)
        val restored = if (target is LockTarget.Chat && settings.lockArchive) LockTarget.Chat(target.dialogId, target.secret, archived = true) else target
        return window.isOpen() || !isLocked(restored, settings)
    }

    /**
     * Runs [lift], which takes a lock away, once the owner of the device has shown themselves, as
     * opening a locked chat asks them to; within the moments after an unlock it runs at once.
     */
    internal fun liftingLock(lift: () -> Unit) {
        if (window.isOpen()) lift() else unlocking(lift)
    }

    // Asks for the device's credential in the app's main screen, the only place it can be asked.
    private fun unlocking(then: () -> Unit) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val activity = LaunchActivity.instance
        if (activity == null) {
            Toast.makeText(ApplicationLoader.applicationContext, R.string.serein_lock_open_in_app, Toast.LENGTH_LONG).show()
            return
        }
        // Without a screen lock nothing can be asked, so locks let everyone through.
        if (BiometricManager.from(activity).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(activity, R.string.serein_lock_no_screen_lock, Toast.LENGTH_LONG).show()
            then()
            return
        }
        authenticate(activity, authenticators) {
            window.unlock()
            then()
        }
    }

    private fun authenticate(activity: FragmentActivity, authenticators: Int, onSuccess: () -> Unit) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        }
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.serein_lock_prompt))
                .setAllowedAuthenticators(authenticators)
                .build(),
        )
    }

    private fun targetOf(screen: BaseFragment): LockTarget? {
        val arguments = screen.arguments ?: return null
        return when (screen) {
            is DialogsActivity -> LockTarget.Archive.takeIf { arguments.getInt("folderId", 0) == 1 }
            is ChatActivity, is ProfileActivity, is TopicsFragment -> {
                val encrypted = arguments.getInt("enc_id", 0)
                val userId = arguments.getLong("user_id", 0)
                val chatId = arguments.getLong("chat_id", 0)
                val dialogId = when {
                    encrypted != 0 -> DialogObject.makeEncryptedDialogId(encrypted.toLong())
                    userId != 0L -> userId
                    chatId != 0L -> -chatId
                    else -> return null
                }
                chat(screen.currentAccount, dialogId, archived = isArchived(screen.currentAccount, dialogId))
            }
            else -> null
        }
    }

    internal fun settings(options: Options, account: Int) = LockSettings(
        lockedChats = DialogIds.parse(options.get(ChatLockOptions.lockedChats, account)),
        lockArchive = options.get(ChatLockOptions.lockArchive, account),
        lockSecretChats = options.get(ChatLockOptions.lockSecretChats, account),
    )

    // Locking needs nothing; turning a lock off is what someone holding an unlocked phone would do.
    private fun guardLock(on: Boolean, change: () -> Unit) = if (on) change() else liftingLock(change)

    override val settingsIcon: Int = R.drawable.msg_secret

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_lock_title,
        listOf(
            SettingsSection(
                rows = listOf(
                    SettingsRow.Toggle(ChatLockOptions.lockArchive, R.string.serein_lock_archive, ::guardLock),
                    SettingsRow.Toggle(ChatLockOptions.lockSecretChats, R.string.serein_lock_secret, ::guardLock),
                    SettingsRow.Screen(R.string.serein_lock_chats, { options -> LockedChatsActivity(options) }),
                ),
                note = R.string.serein_lock_note,
            ),
        ),
    )
}

/** "Lock chat" or "Unlock chat" in the menu of a chat. */
private class LockEntry(private val options: Options) : ChatMenuHooks.Entry {
    override val id: Int = ChatMenuHooks.FIRST_ID + 5

    override val icon: Int = R.drawable.msg_secret

    override fun isShown(account: Int, dialogId: Long): Boolean = !DialogObject.isEncryptedDialog(dialogId)

    override fun title(account: Int, dialogId: Long): CharSequence =
        getString(if (locked(account, dialogId)) R.string.serein_unlock_chat else R.string.serein_lock_chat)

    override fun onSelected(account: Int, dialogId: Long, chat: Any) {
        val lock = !locked(account, dialogId)
        val change: () -> Unit = {
            options.set(ChatLockOptions.lockedChats, DialogIds.with(options.get(ChatLockOptions.lockedChats, account), dialogId, lock), account)
            val text = getString(if (lock) R.string.serein_chat_locked else R.string.serein_chat_unlocked)
            (chat as BaseFragment).parentActivity?.let { Toast.makeText(it, text, Toast.LENGTH_SHORT).show() }
        }
        if (lock) change() else ChatLockFeature.liftingLock(change)
    }

    private fun locked(account: Int, dialogId: Long) =
        dialogId in DialogIds.parse(options.get(ChatLockOptions.lockedChats, account))
}
