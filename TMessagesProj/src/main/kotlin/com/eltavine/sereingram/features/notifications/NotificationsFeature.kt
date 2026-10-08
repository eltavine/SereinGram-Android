package com.eltavine.sereingram.features.notifications

import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.NotificationHooks
import com.eltavine.sereingram.settings.SettingsCategory
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import com.eltavine.sereingram.settings.SettingsTint
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

    override val settingsCategory: SettingsCategory = SettingsCategory.CHATS

    override val settingsTint: SettingsTint = SettingsTint.RED

    override val settingsOrder: Int = 4

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_notifications_title,
        summary = R.string.serein_notifications_summary,
        sections = listOf(
            SettingsSection(
                rows = listOf(
                    SettingsRow.Toggle(
                        NotificationOptions.contentWhenLocked,
                        R.string.serein_notifications_content_when_locked,
                        summary = R.string.serein_notifications_content_when_locked_info,
                    ),
                ),
                note = R.string.serein_notifications_content_when_locked_note,
            ),
            SettingsSection(
                rows = listOf(
                    SettingsRow.Toggle(
                        NotificationOptions.plainAnswers,
                        R.string.serein_notifications_plain_answers,
                        summary = R.string.serein_notifications_plain_answers_info,
                    ),
                ),
                note = R.string.serein_notifications_plain_answers_note,
            ),
        ),
    )
}
