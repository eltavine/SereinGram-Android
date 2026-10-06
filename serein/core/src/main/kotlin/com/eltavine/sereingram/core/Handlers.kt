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

    /** Adds [handler] after the ones already installed; closing the result removes it. */
    public fun install(handler: H): AutoCloseable {
        synchronized(this) {
            installed = installed + handler
        }
        return AutoCloseable {
            synchronized(this) {
                installed = installed.filterNot { it === handler }
            }
        }
    }
}
