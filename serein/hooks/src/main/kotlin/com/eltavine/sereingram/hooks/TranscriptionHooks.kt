package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** Turning voice and video messages into text, besides Telegram Premium's own way. */
public object TranscriptionHooks {
    public interface Provider {
        /** Whether [message], Telegram's message object, gets a transcribe button from this provider. */
        public fun offers(account: Int, message: Any): Boolean

        /**
         * Handles a tap on the transcribe button of [message], whose text shows when
         * [open]; [button] is Telegram's button. False leaves the tap to Telegram.
         */
        public fun tap(account: Int, message: Any, open: Boolean, button: Any): Boolean
    }

    public val providers: Handlers<Provider> = Handlers()

    @JvmStatic
    public fun offersTranscription(account: Int, message: Any?): Boolean =
        message != null && providers.all.any { provider ->
            Faults.guard("transcription provider", fallback = false) { provider.offers(account, message) }
        }

    @JvmStatic
    public fun handlesTap(account: Int, message: Any?, open: Boolean, button: Any): Boolean =
        message != null && providers.all.any { provider ->
            Faults.guard("transcription provider", fallback = false) { provider.tap(account, message, open, button) }
        }
}
