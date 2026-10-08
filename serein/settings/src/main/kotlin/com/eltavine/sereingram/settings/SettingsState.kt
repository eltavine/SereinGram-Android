package com.eltavine.sereingram.settings

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.OptionScope
import com.eltavine.sereingram.core.Options

/**
 * The options as a page shows them: device options, and the account options of
 * [account], the account the page was opened in.
 */
public class SettingsState(public val options: Options, public val account: Int) {
    public operator fun <T : Any> get(option: Option<T>): T = options.get(option, accountOf(option))

    public operator fun <T : Any> set(option: Option<T>, value: T) {
        options.set(option, value, accountOf(option))
    }

    public fun reset(option: Option<*>) {
        options.reset(option, accountOf(option))
    }

    public fun isModified(option: Option<*>): Boolean = options.isModified(option, accountOf(option))

    /** The account [option] is stored for: [account] for account options, none for device options. */
    public fun accountOf(option: Option<*>): Int = if (option.scope == OptionScope.ACCOUNT) account else Options.NO_ACCOUNT
}
