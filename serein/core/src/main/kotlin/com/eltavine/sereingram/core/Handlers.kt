package com.eltavine.sereingram.core

/**
 * The handlers installed for one hook. Hooks read the list on hot paths, so
 * reads take no lock; installing and removing copy the list.
 */
public class Handlers<H : Any> {
    @Volatile
    private var installed: List<H> = emptyList()

    public val all: List<H>
        get() = installed

    public val isEmpty: Boolean
        get() = installed.isEmpty()

    /** Adds [handler] after the ones already installed; closing the result removes this installation once. */
    public fun install(handler: H): AutoCloseable {
        synchronized(this) {
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
