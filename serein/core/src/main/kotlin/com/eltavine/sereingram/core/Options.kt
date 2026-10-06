package com.eltavine.sereingram.core

/**
 * Reads and writes option values. Device options ignore the account; account
 * options need one. Writing the default removes the stored value, so an option
 * that was never changed follows its default if a later release changes it.
 */
public class Options(private val stores: StoreProvider) {
    public fun interface StoreProvider {
        /** The store of [scope]; [account] is [NO_ACCOUNT] for device options. */
        public fun store(scope: OptionScope, account: Int): KeyValueStore
    }

    public fun interface Listener {
        public fun onChanged(option: Option<*>, account: Int)
    }

    private val listeners = Handlers<Listener>()

    public fun <T : Any> get(option: Option<T>, account: Int = NO_ACCOUNT): T =
        option.type.read(store(option, account), option.key) ?: option.default

    public fun <T : Any> set(option: Option<T>, value: T, account: Int = NO_ACCOUNT) {
        val store = store(option, account)
        if (value == option.default) {
            store.remove(option.key)
        } else {
            option.type.write(store, option.key, value)
        }
        notify(option, account)
    }

    public fun reset(option: Option<*>, account: Int = NO_ACCOUNT) {
        store(option, account).remove(option.key)
        notify(option, account)
    }

    public fun isModified(option: Option<*>, account: Int = NO_ACCOUNT): Boolean =
        option.type.read(store(option, account), option.key) != null

    /** [listener] runs on the thread that wrote the value. */
    public fun addListener(listener: Listener): AutoCloseable = listeners.install(listener)

    private fun store(option: Option<*>, account: Int): KeyValueStore = when (option.scope) {
        OptionScope.DEVICE -> stores.store(OptionScope.DEVICE, NO_ACCOUNT)
        OptionScope.ACCOUNT -> {
            require(account >= 0) { "$option is stored per account; pass the account" }
            stores.store(OptionScope.ACCOUNT, account)
        }
    }

    private fun notify(option: Option<*>, account: Int) {
        val target = if (option.scope == OptionScope.DEVICE) NO_ACCOUNT else account
        listeners.all.forEach { it.onChanged(option, target) }
    }

    public companion object {
        public const val NO_ACCOUNT: Int = -1
    }
}
