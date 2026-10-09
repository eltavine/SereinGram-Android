package com.eltavine.sereingram.app

import android.content.Context
import android.content.res.Configuration
import com.eltavine.sereingram.hooks.LocaleHooks
import org.telegram.messenger.LocaleController
import java.util.Locale

/**
 * Lets Telegram show SereinGram's strings, as the app does when it starts, until closed.
 * Telegram keeps the packs it loaded for a locale, perhaps before SereinGram's were there,
 * so a switch to another locale and back makes it load them again.
 */
internal fun showSereinStrings(context: Context): AutoCloseable {
    val installed = LocaleHooks.stringPacks.install(SereinStringPacks)
    val controller = LocaleController.getInstance()
    val configuration = context.resources.configuration
    controller.onDeviceConfigurationChange(Configuration(configuration).apply { setLocale(Locale.ROOT) })
    controller.onDeviceConfigurationChange(configuration)
    return installed
}
