package com.eltavine.sereingram.core

import java.util.concurrent.ConcurrentHashMap

/** Storage port for one scope, the device or one account; a missing key reads as null. */
public interface KeyValueStore {
    public fun getBoolean(key: String): Boolean?

    public fun getInt(key: String): Int?

    public fun getLong(key: String): Long?

    public fun getString(key: String): String?

    public fun putBoolean(key: String, value: Boolean)

    public fun putInt(key: String, value: Int)

    public fun putLong(key: String, value: Long)

    public fun putString(key: String, value: String)

    public fun remove(key: String)
}

/** A store that lives only as long as the process, for tests and transient state. */
public class MemoryKeyValueStore : KeyValueStore {
    private val values = ConcurrentHashMap<String, Any>()

    override fun getBoolean(key: String): Boolean? = values[key] as? Boolean

    override fun getInt(key: String): Int? = values[key] as? Int

    override fun getLong(key: String): Long? = values[key] as? Long

    override fun getString(key: String): String? = values[key] as? String

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }

    override fun putInt(key: String, value: Int) {
        values[key] = value
    }

    override fun putLong(key: String, value: Long) {
        values[key] = value
    }

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}
