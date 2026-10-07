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

// Goes through Telegram's own check, so it fails if an upstream merge drops the hook.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LinksFeatureTest {
    private val store = MemoryKeyValueStore()
    private val options = Options { _, _ -> store }

    @Before
    fun setUp() {
        ApplicationLoader.applicationContext = ApplicationProvider.getApplicationContext()
        LinksFeature.start(ModuleContext(options) { _, _ -> })
    }

    @After
    fun tearDown() {
        options.reset(LinkOptions.confirmAll)
    }

    @Test
    fun telegramAsksBeforeOpeningAnyLinkOnceTheUserWantsIt() {
        assertFalse(AndroidUtilities.shouldShowUrlInAlert("https://example.com/page"))
        options.set(LinkOptions.confirmAll, true)
        assertTrue(AndroidUtilities.shouldShowUrlInAlert("https://example.com/page"))
    }
}
