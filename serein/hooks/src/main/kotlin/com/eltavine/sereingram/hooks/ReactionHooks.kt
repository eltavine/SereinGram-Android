package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** The reactions Telegram offers above a message's menu. */
public object ReactionHooks {
    public fun interface Arranger {
        /** The positions of [reactions], Telegram's, in the order to offer them; positions left out keep their order after. */
        public fun arrange(account: Int, reactions: List<Any>): List<Int>
    }

    public val arrangers: Handlers<Arranger> = Handlers()

    /** [reactions] in the order the installed arrangers ask for, each one once. */
    @JvmStatic
    public fun <T : Any> arrange(account: Int, reactions: List<T>): List<T> =
        arrangers.all.fold(reactions) { current, arranger ->
            Faults.guard("reaction arranger", fallback = current) {
                val first = arranger.arrange(account, current).distinct().filter { it in current.indices }
                first.map(current::get) + current.filterIndexed { index, _ -> index !in first }
            }
        }
}
