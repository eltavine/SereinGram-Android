package com.eltavine.sereingram.app

import android.app.Application
import com.eltavine.sereingram.adapters.prefs.PreferencesStores
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.ModuleRegistry
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.features.declutter.DeclutterFeature
import com.eltavine.sereingram.features.services.ServicesModule
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import org.telegram.messenger.FileLog
import org.telegram.messenger.R

/**
 * Composition root, started once per process from `ApplicationLoader.onCreate`.
 * The module list is the only place that knows every feature.
 */
object SereinApp {
    private val modules = ModuleRegistry(
        listOf(
            ServicesModule,
            DeclutterFeature,
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
        installSettingsEntry(options, listOf(featuresSection()))
    }

    private fun featuresSection() = SettingsSection(
        header = R.string.serein_settings_features,
        rows = modules.modules.filterIsInstance<SettingsContributor>().map {
            SettingsRow.Subpage(it.settingsPage, it.settingsIcon)
        },
    )
}
