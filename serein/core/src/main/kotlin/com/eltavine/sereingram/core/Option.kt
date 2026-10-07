package com.eltavine.sereingram.core

/** Where an option is stored: once per device, or separately for each Telegram account. */
public enum class OptionScope { DEVICE, ACCOUNT }

/** The kind of value an option holds; each kind maps to one typed slot of a [KeyValueStore]. */
public sealed class OptionType<T : Any> {
    internal abstract fun read(store: KeyValueStore, key: String): T?

    internal abstract fun write(store: KeyValueStore, key: String, value: T)

    public data object Bool : OptionType<Boolean>() {
        override fun read(store: KeyValueStore, key: String): Boolean? = store.getBoolean(key)

        override fun write(store: KeyValueStore, key: String, value: Boolean): Unit = store.putBoolean(key, value)
    }

    public data object Int32 : OptionType<Int>() {
        override fun read(store: KeyValueStore, key: String): Int? = store.getInt(key)

        override fun write(store: KeyValueStore, key: String, value: Int): Unit = store.putInt(key, value)
    }

    public data object Int64 : OptionType<Long>() {
        override fun read(store: KeyValueStore, key: String): Long? = store.getLong(key)

        override fun write(store: KeyValueStore, key: String, value: Long): Unit = store.putLong(key, value)
    }

    public data object Text : OptionType<String>() {
        override fun read(store: KeyValueStore, key: String): String? = store.getString(key)

        override fun write(store: KeyValueStore, key: String, value: String): Unit = store.putString(key, value)
    }
}

/**
 * A setting declared by a module. The key is its storage key and never
 * changes once released; an unset value reads as [default]. A [secret], such
 * as an API key, is masked where it is shown and stays out of backups.
 */
public class Option<T : Any>(
    public val key: String,
    public val type: OptionType<T>,
    public val default: T,
    public val scope: OptionScope = OptionScope.DEVICE,
    public val secret: Boolean = false,
) {
    init {
        require(KEY.matches(key)) { "option key \"$key\" must be lower_snake_case" }
    }

    override fun toString(): String = "Option($key)"

    private companion object {
        val KEY = Regex("[a-z][a-z0-9]*(_[a-z0-9]+)*")
    }
}

public fun booleanOption(
    key: String,
    default: Boolean = false,
    scope: OptionScope = OptionScope.DEVICE,
    secret: Boolean = false,
): Option<Boolean> = Option(key, OptionType.Bool, default, scope, secret)

public fun intOption(
    key: String,
    default: Int = 0,
    scope: OptionScope = OptionScope.DEVICE,
    secret: Boolean = false,
): Option<Int> = Option(key, OptionType.Int32, default, scope, secret)

public fun longOption(
    key: String,
    default: Long = 0,
    scope: OptionScope = OptionScope.DEVICE,
    secret: Boolean = false,
): Option<Long> = Option(key, OptionType.Int64, default, scope, secret)

public fun textOption(
    key: String,
    default: String = "",
    scope: OptionScope = OptionScope.DEVICE,
    secret: Boolean = false,
): Option<String> = Option(key, OptionType.Text, default, scope, secret)
