package com.eltavine.sereingram.app

import android.app.Application
import com.eltavine.sereingram.adapters.prefs.PreferencesStores
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.ModuleRegistry
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.features.services.ServicesModule
import org.telegram.messenger.FileLog

/**
 * Composition root, started once per process from `ApplicationLoader.onCreate`.
 * The module list is the only place that knows every feature.
 */
object SereinApp {
    private val modules = ModuleRegistry(
        listOf(
            ServicesModule,
        ),
    )

    @JvmStatic
    lateinit var options: Options
        private set

    @JvmStatic
    fun onCreate(application: Application) {
        if (::options.isInitialized) {
            return
        }
        options = Options(PreferencesStores(application))
        modules.start(ModuleContext(options) { message, error -> FileLog.e(message, error) })
    }
}
