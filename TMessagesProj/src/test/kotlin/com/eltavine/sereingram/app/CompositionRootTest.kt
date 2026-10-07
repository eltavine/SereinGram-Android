package com.eltavine.sereingram.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.eltavine.sereingram.core.KeyValueStore
import com.eltavine.sereingram.core.MemoryKeyValueStore
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.OptionScope
import com.eltavine.sereingram.core.Options
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.telegram.messenger.ApplicationLoader

/**
 * The real module list builds, which checks module ids and option keys, and every
 * module starts without a fault, which checks for clashing menu and settings ids.
 */
@RunWith(RobolectricTestRunner::class)
// An SDK level of its own gives this test its own sandbox, so the handlers it installs stay out of the other tests.
@Config(application = Application::class, sdk = [34])
class CompositionRootTest {
    @Test
    fun everyModuleStartsWithoutAFault() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        ApplicationLoader.applicationContext = application
        val stores = HashMap<Pair<OptionScope, Int>, KeyValueStore>()
        val options = Options { scope, account -> stores.getOrPut(scope to account) { MemoryKeyValueStore() } }
        val faults = ArrayList<String>()
        val modules = SereinApp.modules(application)
        modules.start(ModuleContext(options) { message, error -> faults += "$message: $error" })
        installSettingsEntry(options, emptyList())
        assertEquals(emptyList<String>(), faults)
    }
}
