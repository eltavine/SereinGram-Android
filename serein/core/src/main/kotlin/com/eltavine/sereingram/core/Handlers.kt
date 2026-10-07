package com.eltavine.sereingram.core

/**
 * The handlers installed for one hook. Hooks read the list on hot paths, so
 * reads take no lock; installing and removing copy the list. A [key], when
 * given, keeps two handlers that answer for the same thing, such as two menu
 * items with one id, from being installed together.
 */
public class Handlers<H : Any>(private val key: ((H) -> Any)? = null) {
    @Volatile
    private var installed: List<H> = emptyList()

    public val all: List<H>
        get() = installed

    public val isEmpty: Boolean
        get() = installed.isEmpty()

    /**
     * Adds [handler] after the ones already installed; closing the result removes this
     * installation once. Throws when another installed handler has the same [key].
     */
    public fun install(handler: H): AutoCloseable {
        synchronized(this) {
            key?.let { keyOf ->
                val taken = keyOf(handler)
                require(installed.none { keyOf(it) == taken }) { "a handler for $taken is already installed" }
            }
            installed = installed + handler
        }
        var removed = false
        return AutoCloseable {
            synchronized(this) {
                val index = installed.indexOfFirst { it === handler }
                if (!removed && index >= 0) {
                    removed = true
                    installed = installed.filterIndexed { position, _ -> position != index }
                }
            }
        }
    }
}
