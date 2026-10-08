package com.eltavine.sereingram.features.timestamps

import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.MessageHooks
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import org.telegram.messenger.LocaleController
import org.telegram.messenger.MessageObject
import org.telegram.messenger.R
import java.time.Instant
import java.time.ZoneId

/** Writes the date beside the time of older messages, after NagramX's request. */
object TimestampsFeature : SereinModule, SettingsContributor {
    override val id: String = "timestamps"

    override val options: List<Option<*>> = TimestampOptions.all

    override fun start(context: ModuleContext) {
        val options = context.options
        MessageHooks.timeDecorators.install { _, message, time -> dated(options, message as MessageObject, time) }
        // Asked whenever a message draws its time.
        val hideReadChecks = options.cached(TimestampOptions.hideReadChecks)
        MessageHooks.readReceiptPolicies.install { _, _ -> hideReadChecks.value }
    }

    private fun dated(options: Options, message: MessageObject, time: String): String {
        val sent = message.messageOwner?.date?.takeIf { it > 0 } ?: return time
        if (!options.get(TimestampOptions.showDate) || hasOwnDate(message)) {
            return time
        }
        val millis = sent * 1000L
        val locale = LocaleController.getInstance()
        val date = when (dateShown(Instant.ofEpochMilli(millis), Instant.now(), ZoneId.systemDefault())) {
            DateShown.NONE -> return time
            DateShown.DAY_AND_MONTH -> locale.formatterDayMonth.format(millis)
            DateShown.FULL -> locale.formatterYear.format(millis)
        }
        return withDate(time, locale.formatterDay.format(millis), date)
    }

    // Telegram already writes a date for these, and not the one they arrived on: deleted messages in the
    // admin log and previews of reposts get the day they were sent, messages forwarded to Saved Messages
    // the day they were first sent or saved.
    private fun hasOwnDate(message: MessageObject): Boolean {
        val forwarded = message.messageOwner?.fwd_from
        return message.realDate != 0 || message.isRepostPreview ||
            message.isSaved && forwarded != null && (forwarded.date != 0 || forwarded.saved_date != 0)
    }

    override val settingsIcon: Int = R.drawable.msg_calendar2

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_timestamps_title,
        listOf(
            SettingsSection(
                rows = listOf(SettingsRow.Toggle(TimestampOptions.showDate, R.string.serein_timestamps_show_date)),
                note = R.string.serein_timestamps_show_date_note,
            ),
            SettingsSection(
                rows = listOf(SettingsRow.Toggle(TimestampOptions.hideReadChecks, R.string.serein_timestamps_hide_read)),
                note = R.string.serein_timestamps_hide_read_note,
            ),
        ),
    )
}
