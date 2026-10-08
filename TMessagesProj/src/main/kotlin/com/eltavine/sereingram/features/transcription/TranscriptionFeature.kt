package com.eltavine.sereingram.features.transcription

import android.widget.Toast
import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.TranscriptionHooks
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
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

/** Transcribes voice and video messages with a speech-to-text service of the user's choice. */
object TranscriptionFeature : SereinModule, SettingsContributor {
    override val id: String = "transcription"

    override val options: List<Option<*>> = TranscriptionOptions.all

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client by lazy {
        TranscriptionClient(HttpClient(OkHttp) { install(HttpTimeout) { requestTimeoutMillis = 120_000 } })
    }

    override fun start(context: ModuleContext) {
        TranscriptionHooks.providers.install(Provider(context.options))
    }

    private class Provider(private val options: Options) : TranscriptionHooks.Provider {
        // Account, chat and message of each transcription under way.
        private val transcribing: MutableSet<Triple<Int, Long, Int>> = ConcurrentHashMap.newKeySet()

        // Runs whenever a message is drawn, so the cheap checks come first.
        override fun offers(account: Int, message: Any): Boolean {
            val shown = message as MessageObject
            return options.get(TranscriptionOptions.enabled) && (shown.isVoice || shown.isRoundVideo) && shown.isSent &&
                !shown.isQuickReply && !shown.isRepostPreview && MessageObject.getMedia(shown.messageOwner) !is TLRPC.TL_messageMediaWebPage &&
                shown.messageOwner?.media?.ttl_seconds == 0 && !DialogObject.isEncryptedDialog(shown.dialogId) && config(options).isUsable
        }

        override fun isTranscribing(message: Any): Boolean = key(message as MessageObject) in transcribing

        override fun tap(account: Int, message: Any, open: Boolean, button: Any): Boolean {
            val shown = message as MessageObject
            val config = config(options)
            if (!config.isUsable || open || !shown.messageOwner?.voiceTranscription.isNullOrEmpty()) {
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
            scope.launch {
                val result = runCatching { client.transcribe(config, file) }
                AndroidUtilities.runOnUIThread {
                    transcribing.remove(key)
                    result.onSuccess { show(shown, it) }.onFailure { error ->
                        Faults.report("transcription", error)
                        toast(LocaleController.formatString(R.string.serein_transcription_failed, error.message.orEmpty()))
                        // With an id, Telegram finds the message among those shown now and draws it anew.
                        NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.voiceTranscriptionUpdate, shown, 0L, null)
                    }
                }
            }
            return true
        }

        private fun key(message: MessageObject) = Triple(message.currentAccount, message.dialogId, message.id)
    }

    // Telegram's own transcriptions end the same way, which redraws the message.
    private fun show(message: MessageObject, text: String) {
        message.messageOwner.voiceTranscription = text
        message.messageOwner.voiceTranscriptionOpen = true
        TranscribeButton.openVideoTranscription(message)
        TranscribeButton.finishTranscription(message, 0, text)
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

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_transcription_title,
        listOf(
            SettingsSection(
                rows = listOf(SettingsRow.Toggle(TranscriptionOptions.enabled, R.string.serein_transcription_enabled)),
                note = R.string.serein_transcription_note,
            ),
            SettingsSection(
                header = R.string.serein_transcription_service,
                rows = listOf(
                    SettingsRow.Text(TranscriptionOptions.baseUrl, R.string.serein_transcription_base_url, R.string.serein_transcription_base_url_hint),
                    SettingsRow.Text(TranscriptionOptions.apiKey, R.string.serein_transcription_api_key, R.string.serein_transcription_api_key_hint),
                    SettingsRow.Text(TranscriptionOptions.model, R.string.serein_transcription_model, R.string.serein_transcription_model_hint),
                    SettingsRow.Text(TranscriptionOptions.language, R.string.serein_transcription_language, R.string.serein_transcription_language_hint),
                ),
                note = R.string.serein_transcription_service_note,
            ),
        ),
    )
}
