package com.eltavine.sereingram.features.links

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.eltavine.sereingram.core.MemoryKeyValueStore
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Options
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import tw.nekomimi.nekogram.NekoConfig

// Goes through Telegram's own check, so it fails if an upstream merge drops the hook.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LinksFeatureTest {
    private var skipsConfirming = false

    @Before
    fun setUp() {
        ApplicationLoader.applicationContext = ApplicationProvider.getApplicationContext()
        skipsConfirming = NekoConfig.skipOpenLinkConfirm.Bool()
        if (!started) {
            LinksFeature.start(ModuleContext(options) { _, _ -> })
            started = true
        }
    }

    @After
    fun tearDown() {
        NekoConfig.skipOpenLinkConfirm.changed(skipsConfirming)
        options.reset(LinkOptions.confirmAll)
    }

    @Test
    fun telegramAsksBeforeOpeningAnyLinkOnceTheUserWantsIt() {
        assertFalse(AndroidUtilities.shouldShowUrlInAlert("https://example.com/page"))
        options.set(LinkOptions.confirmAll, true)
        assertTrue(AndroidUtilities.shouldShowUrlInAlert("https://example.com/page"))
    }

    @Test
    fun nagramSkippingTelegramsQuestionsDoesNotSkipSereinGrams() {
        NekoConfig.skipOpenLinkConfirm.changed(true)
        assertFalse(AndroidUtilities.shouldShowUrlInAlert("https://xn--e1afmkfd.xn--p1ai/"))
        options.set(LinkOptions.confirmAll, true)
        assertTrue(AndroidUtilities.shouldShowUrlInAlert("https://example.com/page"))
    }

    // Modules install their handlers for good, so the feature starts once for all tests.
    private companion object {
        val store = MemoryKeyValueStore()
        val options = Options { _, _ -> store }
        var started = false
    }
}
