package com.eltavine.sereingram.features.sending

import com.eltavine.sereingram.core.MemoryKeyValueStore
import com.eltavine.sereingram.core.Options
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SendingFeatureTest {
    private val store = MemoryKeyValueStore()
    private val options = Options { _, _ -> store }
    private var nagramAsked = 0

    @Test
    fun whoeverHadSereinGramAskBeforeStickersHasNagramAskOnce() {
        store.putBoolean(STICKER_KEY, true)
        SendingFeature.handStickersToNagram(options) { nagramAsked++ }
        assertEquals(1, nagramAsked)
        assertNull(store.getBoolean(STICKER_KEY))
        SendingFeature.handStickersToNagram(options) { nagramAsked++ }
        assertEquals(1, nagramAsked)
    }

    @Test
    fun nagramsSettingIsLeftAsItIsOtherwise() {
        SendingFeature.handStickersToNagram(options) { nagramAsked++ }
        assertEquals(0, nagramAsked)
    }

    private companion object {
        // Where releases before Nagram's own setting stored SereinGram's; it must not change.
        const val STICKER_KEY = "sending_ask_before_sticker"
    }
}
