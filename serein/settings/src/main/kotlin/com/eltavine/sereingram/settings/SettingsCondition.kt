package com.eltavine.sereingram.settings

import com.eltavine.sereingram.core.Option

/** Whether a row or a section applies, asked of the settings each time the page draws. */
public fun interface SettingsCondition {
    public fun holds(state: SettingsState): Boolean

    public infix fun and(other: SettingsCondition): SettingsCondition = SettingsCondition { holds(it) && other.holds(it) }

    public infix fun or(other: SettingsCondition): SettingsCondition = SettingsCondition { holds(it) || other.holds(it) }

    public operator fun not(): SettingsCondition = SettingsCondition { !holds(it) }

    public companion object {
        public val ALWAYS: SettingsCondition = SettingsCondition { true }

        /** While [option] is on. */
        public fun isOn(option: Option<Boolean>): SettingsCondition = SettingsCondition { it[option] }

        /** While [option] holds [value]. */
        public fun <T : Any> isSetTo(option: Option<T>, value: T): SettingsCondition = SettingsCondition { it[option] == value }
    }
}
