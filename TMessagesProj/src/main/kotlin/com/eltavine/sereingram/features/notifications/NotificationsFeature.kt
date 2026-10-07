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

/** Lets notifications show their messages while a passcode locks the app, after NagramX's request. */
object NotificationsFeature : SereinModule, SettingsContributor {
    override val id: String = "notifications"

    override val options: List<Option<*>> = NotificationOptions.all

    override fun start(context: ModuleContext) {
        val options = context.options
        NotificationHooks.lockedContentPolicies.install { options.get(NotificationOptions.contentWhenLocked) }
    }

    override val settingsIcon: Int = R.drawable.msg_notifications

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_notifications_title,
        listOf(
            SettingsSection(
                rows = listOf(SettingsRow.Toggle(NotificationOptions.contentWhenLocked, R.string.serein_notifications_content_when_locked)),
                note = R.string.serein_notifications_content_when_locked_note,
            ),
        ),
    )
}
