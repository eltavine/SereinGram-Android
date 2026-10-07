package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** How Telegram plays voice and video messages. */
public object PlaybackHooks {
    public fun interface VoiceQueuePolicy {
        public fun stopsAfterEachVoice(): Boolean
    }

    public val voiceQueuePolicies: Handlers<VoiceQueuePolicy> = Handlers()

    /** Whether playback ends with the voice or video message that was started instead of going on to the next. */
    @JvmStatic
    public fun stopsAfterEachVoice(): Boolean =
        voiceQueuePolicies.all.any { policy -> Faults.guard("voice queue policy", fallback = false) { policy.stopsAfterEachVoice() } }
}
