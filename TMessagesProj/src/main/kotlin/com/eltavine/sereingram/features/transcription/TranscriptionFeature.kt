package com.eltavine.sereingram.features.transcription

import android.widget.Toast
import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.TranscriptionHooks
import com.eltavine.sereingram.settings.SettingsCategory
import com.eltavine.sereingram.settings.SettingsCondition
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import com.eltavine.sereingram.settings.SettingsTint
import com.eltavine.sereingram.support.HttpDns
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.DialogObject
import org.telegram.messenger.FileLoader
import org.telegram.messenger.LocaleController
import org.telegram.messenger.MessageObject
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.R
import org.telegram.tgnet.TLRPC
import org.telegram.ui.Components.TranscribeButton
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** Transcribes voice and video messages with a speech-to-text service of the user's choice. */
object TranscriptionFeature : SereinModule, SettingsContributor {
    override val id: String = "transcription"

    override val options: List<Option<*>> = TranscriptionOptions.all

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client by lazy {
        TranscriptionClient(
            HttpClient(OkHttp) {
                install(HttpTimeout) { requestTimeoutMillis = 120_000 }
                engine { config { dns(HttpDns) } }
            },
        )
    }

    override fun start(context: ModuleContext) {
        TranscriptionHooks.providers.install(Provider(context.options))
    }

    private class Provider(private val options: Options) : TranscriptionHooks.Provider {
        // Account, chat and message of each transcription under way.
        private val transcribing: MutableSet<Triple<Int, Long, Int>> = ConcurrentHashMap.newKeySet()

        // Telegram finds the message a transcription is for by its id, or by message and chat when no message has
        // the id; Telegram's own ids are positive and most messages have none, so SereinGram's are negative.
        private val transcriptionIds = AtomicLong()

        // Runs whenever a message is drawn, so the cheap checks come first.
        override fun offers(account: Int, message: Any): Boolean {
            val shown = message as MessageObject
            return options.get(TranscriptionOptions.enabled) && (shown.isVoice || shown.isRoundVideo) && shown.isSent &&
                !shown.isQuickReply && !shown.isRepostPreview && MessageObject.getMedia(shown.messageOwner) !is TLRPC.TL_messageMediaWebPage &&
                shown.messageOwner?.media?.ttl_seconds == 0 && !DialogObject.isEncryptedDialog(shown.dialogId) && config(options).isUsable
        }

        override fun isTranscribing(message: Any): Boolean = key(message as MessageObject) in transcribing

        // Telegram's own button reaches this too, so messages SereinGram would not offer stay Telegram's.
        override fun tap(account: Int, message: Any, open: Boolean, button: Any): Boolean {
            val shown = message as MessageObject
            val config = config(options)
            if (!offers(account, message) || open || !shown.messageOwner?.voiceTranscription.isNullOrEmpty()) {
                return false
            }
            val key = key(shown)
            if (key in transcribing) {
                return true
            }
            val loader = FileLoader.getInstance(account)
            val file = loader.getPathToMessage(shown.messageOwner)
            if (file == null || !file.isFile) {
                loader.loadFile(shown.document, shown, FileLoader.PRIORITY_HIGH, 0)
                toast(LocaleController.getString(R.string.serein_transcription_downloading))
                return true
            }
            if (file.length() > MAX_UPLOAD_BYTES) {
                toast(LocaleController.getString(R.string.serein_transcription_too_long))
                return true
            }
            if (!transcribing.add(key)) {
                return true
            }
            (button as TranscribeButton).setLoading(true, true)
            val id = transcriptionIds.decrementAndGet()
            scope.launch {
                val result = runCatching { client.transcribe(config, file) }
                AndroidUtilities.runOnUIThread {
                    transcribing.remove(key)
                    result.onSuccess { show(shown, id, it) }.onFailure { error ->
                        Faults.report("transcription", error)
                        toast(LocaleController.formatString(R.string.serein_transcription_failed, error.message.orEmpty()))
                        NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.voiceTranscriptionUpdate, shown, id, null)
                    }
                }
            }
            return true
        }

        private fun key(message: MessageObject) = Triple(message.currentAccount, message.dialogId, message.id)
    }

    // Telegram's own transcriptions end the same way, which redraws the message.
    private fun show(message: MessageObject, id: Long, text: String) {
        message.messageOwner.voiceTranscription = text
        message.messageOwner.voiceTranscriptionOpen = true
        TranscribeButton.openVideoTranscription(message)
        TranscribeButton.finishTranscription(message, id, text)
    }

    private fun config(options: Options) = TranscriptionConfig(
        enabled = options.get(TranscriptionOptions.enabled),
        baseUrl = options.get(TranscriptionOptions.baseUrl),
        apiKey = options.get(TranscriptionOptions.apiKey),
        model = options.get(TranscriptionOptions.model),
        language = options.get(TranscriptionOptions.language),
    )

    private fun toast(text: String) {
        Toast.makeText(ApplicationLoader.applicationContext, text, Toast.LENGTH_LONG).show()
    }

    override val settingsIcon: Int = R.drawable.msg_photo_text_framed3

    override val settingsCategory: SettingsCategory = SettingsCategory.MEDIA

    override val settingsTint: SettingsTint = SettingsTint.BLUE

    override val settingsOrder: Int = 1

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_transcription_title,
        summary = R.string.serein_transcription_summary,
        status = { state ->
            val config = config(state.options)
            when {
                config.isUsable -> LocaleController.getString(R.string.serein_settings_on)
                config.enabled -> LocaleController.getString(R.string.serein_settings_not_set_up)
                else -> null
            }
        },
        sections = listOf(
            SettingsSection(
                rows = listOf(
                    SettingsRow.Toggle(TranscriptionOptions.enabled, R.string.serein_transcription_enabled, summary = R.string.serein_transcription_enabled_info),
                ),
                note = R.string.serein_transcription_note,
            ),
            SettingsSection(
                header = R.string.serein_transcription_service,
                rows = listOf(
                    SettingsRow.Text(
                        TranscriptionOptions.baseUrl,
                        R.string.serein_transcription_base_url,
                        placeholder = R.string.serein_transcription_base_url_hint,
                        summary = R.string.serein_transcription_base_url_info,
                        kind = SettingsRow.TextKind.URL,
                        check = { text -> R.string.serein_transcription_base_url_invalid.takeIf { endpoint(text) == null } },
                    ),
                    SettingsRow.Text(
                        TranscriptionOptions.apiKey,
                        R.string.serein_transcription_api_key,
                        placeholder = R.string.serein_transcription_api_key_hint,
                        summary = R.string.serein_transcription_api_key_info,
                        requiredWhen = SettingsCondition.isOn(TranscriptionOptions.enabled),
                    ),
                    SettingsRow.Text(
                        TranscriptionOptions.model,
                        R.string.serein_transcription_model,
                        placeholder = R.string.serein_transcription_model_hint,
                        summary = R.string.serein_transcription_model_info,
                    ),
                    SettingsRow.Text(
                        TranscriptionOptions.language,
                        R.string.serein_transcription_language,
                        placeholder = R.string.serein_transcription_language_hint,
                        summary = R.string.serein_transcription_language_info,
                        check = { text -> R.string.serein_transcription_language_invalid.takeUnless { isLanguageCode(text) } },
                    ),
                ),
                note = R.string.serein_transcription_service_note,
            ),
        ),
    )

    private fun endpoint(baseUrl: String): String? =
        TranscriptionConfig(enabled = true, baseUrl = baseUrl, apiKey = "", model = "", language = "").endpoint
}
