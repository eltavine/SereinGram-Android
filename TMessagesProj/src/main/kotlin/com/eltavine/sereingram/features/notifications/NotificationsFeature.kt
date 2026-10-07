package com.eltavine.sereingram.features.notifications

import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.NotificationHooks
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import org.telegram.messenger.R

/**
 * Lets notifications show their messages while a passcode locks the app, after NagramX's request,
 * and send answers typed into them as plain messages, after Cherrygram's.
 */
object NotificationsFeature : SereinModule, SettingsContributor {
    override val id: String = "notifications"

    override val options: List<Option<*>> = NotificationOptions.all

    override fun start(context: ModuleContext) {
        val options = context.options
        NotificationHooks.lockedContentPolicies.install { options.get(NotificationOptions.contentWhenLocked) }
        NotificationHooks.replyPolicies.install { options.get(NotificationOptions.plainAnswers) }
    }

    override val settingsIcon: Int = R.drawable.msg_notifications

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_notifications_title,
        listOf(
            SettingsSection(
                rows = listOf(SettingsRow.Toggle(NotificationOptions.contentWhenLocked, R.string.serein_notifications_content_when_locked)),
                note = R.string.serein_notifications_content_when_locked_note,
            ),
            SettingsSection(
                rows = listOf(SettingsRow.Toggle(NotificationOptions.plainAnswers, R.string.serein_notifications_plain_answers)),
                note = R.string.serein_notifications_plain_answers_note,
            ),
        ),
    )
}
