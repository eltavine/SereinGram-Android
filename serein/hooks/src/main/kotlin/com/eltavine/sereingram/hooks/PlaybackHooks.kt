package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** How Telegram plays voice and video messages, and videos in its media viewer. */
public object PlaybackHooks {
    public fun interface VoiceQueuePolicy {
        public fun stopsAfterEachVoice(): Boolean
    }

    public fun interface SeekPolicy {
        /** How far a double tap beside a playing video jumps, or null to leave it to Telegram. */
        public fun doubleTapSeekMillis(): Long?
    }

    /** What Telegram's media viewer jumps by. */
    public const val TELEGRAM_SEEK_MILLIS: Long = 10_000

    public val voiceQueuePolicies: Handlers<VoiceQueuePolicy> = Handlers()
    public val seekPolicies: Handlers<SeekPolicy> = Handlers()

    /** Whether playback ends with the voice or video message that was started instead of going on to the next. */
    @JvmStatic
    public fun stopsAfterEachVoice(): Boolean =
        voiceQueuePolicies.all.any { policy -> Faults.guard("voice queue policy", fallback = false) { policy.stopsAfterEachVoice() } }

    /** The first jump a policy asks for, kept positive, or [TELEGRAM_SEEK_MILLIS]. */
    @JvmStatic
    public fun doubleTapSeekMillis(): Long =
        seekPolicies.all.firstNotNullOfOrNull { policy ->
            Faults.guard("seek policy", fallback = null) { policy.doubleTapSeekMillis()?.takeIf { it > 0 } }
        } ?: TELEGRAM_SEEK_MILLIS
}
