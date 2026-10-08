package com.eltavine.sereingram.settings.ui

import com.eltavine.sereingram.core.Option
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.Components.BulletinFactory
import tw.nekomimi.nekogram.helpers.AppRestartHelper
import java.util.concurrent.ConcurrentHashMap

/**
 * Options that take effect once the app starts again, with the value each had when it
 * started, so that a restart is offered only while it would change something. They are
 * told apart by option alone, as befits device options, which such options are.
 */
internal object Restarts {
    private val atStart = ConcurrentHashMap<Option<*>, Any>()

    /** Notes [before], which [option] held until now, as its value at the start, unless it changed before. */
    fun changing(option: Option<*>, before: Any) {
        atStart.putIfAbsent(option, before)
    }

    /** Whether the app runs with another value of [option] than [now]. */
    fun pending(option: Option<*>, now: Any): Boolean = atStart[option]?.let { it != now } == true

    /** Says [text] with a button that restarts the app. */
    fun offer(fragment: BaseFragment, text: CharSequence = getString(R.string.serein_settings_restart_needed), icon: Int = R.raw.info) {
        BulletinFactory.of(fragment)
            .createSimpleBulletin(icon, text, getString(R.string.serein_settings_restart)) { AppRestartHelper.triggerRebirth() }
            .show()
    }
}
