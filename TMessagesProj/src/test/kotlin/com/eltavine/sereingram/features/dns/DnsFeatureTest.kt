package com.eltavine.sereingram.features.dns

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.eltavine.sereingram.core.MemoryKeyValueStore
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.OptionScope
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.hooks.NetworkHooks
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.telegram.messenger.ApplicationLoader

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DnsFeatureTest {
    @Before
    fun setUp() {
        ApplicationLoader.applicationContext = ApplicationProvider.getApplicationContext()
        if (!started) {
            DnsFeature.start(ModuleContext(options) { _, _ -> })
            started = true
        }
    }

    @After
    fun tearDown() {
        DnsOptions.all.forEach { options.reset(it) }
    }

    @Test
    fun byDefaultTelegramAndNagramResolveAsTheyDo() {
        assertNull(NetworkHooks.resolve("localhost"))
    }

    @Test
    fun theSystemResolvesWhenItIsChosen() {
        options.set(DnsOptions.mode, DnsMode.SYSTEM.code)
        assertTrue(NetworkHooks.resolve("localhost")!!.all { it.isLoopbackAddress })
    }

    @Test
    fun theSystemAnswersWhatACustomServerCannot() {
        options.set(DnsOptions.mode, DnsMode.CUSTOM.code)
        options.set(DnsOptions.customServer, "https://127.0.0.1:9/dns-query")
        assertTrue(NetworkHooks.resolve("localhost")!!.all { it.isLoopbackAddress })
    }

    // Modules install their handlers for good, so the feature starts once for all tests.
    private companion object {
        val stores = HashMap<Pair<OptionScope, Int>, MemoryKeyValueStore>()
        val options = Options { scope, account -> stores.getOrPut(scope to account) { MemoryKeyValueStore() } }
        var started = false
    }
}
