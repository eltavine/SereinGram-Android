package com.eltavine.sereingram.adapters.prefs

import android.content.Context
import android.content.SharedPreferences
import com.eltavine.sereingram.core.KeyValueStore
import com.eltavine.sereingram.core.OptionScope
import com.eltavine.sereingram.core.Options
import java.util.concurrent.ConcurrentHashMap

/** Option storage in SharedPreferences: `serein` for the device, `serein_account<N>` per account. */
internal class PreferencesStores(private val context: Context) : Options.StoreProvider {
    private val stores = ConcurrentHashMap<Int, KeyValueStore>()

    override fun store(scope: OptionScope, account: Int): KeyValueStore = stores.getOrPut(account) {
        val name = if (scope == OptionScope.DEVICE) "serein" else "serein_account$account"
        PreferencesStore(context.getSharedPreferences(name, Context.MODE_PRIVATE))
    }
}

private class PreferencesStore(private val preferences: SharedPreferences) : KeyValueStore {
    override fun getBoolean(key: String): Boolean? = read(key) { preferences.getBoolean(key, false) }

    override fun getInt(key: String): Int? = read(key) { preferences.getInt(key, 0) }

    override fun getLong(key: String): Long? = read(key) { preferences.getLong(key, 0) }

    override fun getString(key: String): String? = read(key) { preferences.getString(key, null) }

    override fun putBoolean(key: String, value: Boolean) = preferences.edit().putBoolean(key, value).apply()

    override fun putInt(key: String, value: Int) = preferences.edit().putInt(key, value).apply()

    override fun putLong(key: String, value: Long) = preferences.edit().putLong(key, value).apply()

    override fun putString(key: String, value: String) = preferences.edit().putString(key, value).apply()

    override fun remove(key: String) = preferences.edit().remove(key).apply()

    // A value written under another type by an older release reads as unset.
    private inline fun <T> read(key: String, get: () -> T): T? = if (!preferences.contains(key)) {
        null
    } else {
        try {
            get()
        } catch (_: ClassCastException) {
            null
        }
    }
}
