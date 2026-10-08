package com.eltavine.sereingram.adapters.prefs

import android.content.Context
import com.eltavine.sereingram.ports.LocalNameStore

/** Local names in SharedPreferences `serein_local_names_<account>`, keyed by Telegram's peer id. */
internal class PreferencesLocalNameStore(context: Context, account: Int) : LocalNameStore {
    private val preferences = context.getSharedPreferences("serein_local_names_$account", Context.MODE_PRIVATE)

    override fun all(): Map<Long, String> = preferences.all.mapNotNull { (key, value) ->
        val peerId = key.toLongOrNull() ?: return@mapNotNull null
        (value as? String)?.let { peerId to it }
    }.toMap()

    override fun set(peerId: Long, name: String?) {
        val editor = preferences.edit()
        if (name.isNullOrBlank()) editor.remove(peerId.toString()) else editor.putString(peerId.toString(), name.trim())
        editor.apply()
    }

    override fun clearAll() {
        preferences.edit().clear().commit()
    }
}
