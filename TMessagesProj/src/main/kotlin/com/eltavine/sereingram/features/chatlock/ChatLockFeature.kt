package com.eltavine.sereingram.features.chatlock

import android.os.SystemClock
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.eltavine.sereingram.core.DialogIds
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.ChatMenuHooks
import com.eltavine.sereingram.hooks.NavigationHooks
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import org.telegram.messenger.DialogObject
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.INavigationLayout
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
 * locks and Swiftgram issue 56. Telegram opens every screen through one call,
 * which asks [NavigationHooks] first.
 */
object ChatLockFeature : SereinModule, SettingsContributor {
    override val id: String = "chat_lock"

    override val options: List<Option<*>> = ChatLockOptions.all

    private val window = UnlockWindow { SystemClock.elapsedRealtime() }
    private val passes: MutableSet<Any> = Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap()))

    override fun start(context: ModuleContext) {
        val options = context.options
        NavigationHooks.guards.install { layout, screen, params -> allows(options, layout, screen as BaseFragment, params) }
        ChatMenuHooks.entries.install(LockEntry(options))
    }

    private fun allows(options: Options, layout: Any, screen: BaseFragment, params: Any): Boolean {
        if (passes.remove(screen)) {
            return true
        }
        val target = targetOf(screen) ?: return true
        if (!isLocked(target, settings(options, screen.currentAccount)) || window.isOpen()) {
            return true
        }
        val navigation = params as INavigationLayout.NavigationParams
        if (navigation.preview) {
            return false
        }
        val activity = LaunchActivity.instance ?: return true
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (BiometricManager.from(activity).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(activity, R.string.serein_lock_no_screen_lock, Toast.LENGTH_LONG).show()
            return true
        }
        authenticate(activity, authenticators) {
            window.unlock()
            passes.add(screen)
            (layout as INavigationLayout).presentFragment(navigation)
        }
        return false
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
                when {
                    encrypted != 0 -> LockTarget.Chat(DialogObject.makeEncryptedDialogId(encrypted.toLong()), secret = true)
                    userId != 0L -> LockTarget.Chat(userId, secret = false)
                    chatId != 0L -> LockTarget.Chat(-chatId, secret = false)
                    else -> null
                }
            }
            else -> null
        }
    }

    internal fun settings(options: Options, account: Int) = LockSettings(
        lockedChats = DialogIds.parse(options.get(ChatLockOptions.lockedChats, account)),
        lockArchive = options.get(ChatLockOptions.lockArchive, account),
        lockSecretChats = options.get(ChatLockOptions.lockSecretChats, account),
    )

    override val settingsIcon: Int = R.drawable.msg_secret

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_lock_title,
        listOf(
            SettingsSection(
                rows = listOf(
                    SettingsRow.Toggle(ChatLockOptions.lockArchive, R.string.serein_lock_archive),
                    SettingsRow.Toggle(ChatLockOptions.lockSecretChats, R.string.serein_lock_secret),
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
        options.set(ChatLockOptions.lockedChats, DialogIds.with(options.get(ChatLockOptions.lockedChats, account), dialogId, lock), account)
        val fragment = chat as BaseFragment
        val text = getString(if (lock) R.string.serein_chat_locked else R.string.serein_chat_unlocked)
        Toast.makeText(fragment.parentActivity ?: return, text, Toast.LENGTH_SHORT).show()
    }

    private fun locked(account: Int, dialogId: Long) =
        dialogId in DialogIds.parse(options.get(ChatLockOptions.lockedChats, account))
}
