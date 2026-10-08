package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/**
 * Users and chats as they enter Telegram's memory, where every screen reads
 * them from. The objects are Telegram's; rewriters may change them, and say
 * what Telegram's servers know them as for whatever leaves the device or goes
 * into Telegram's database.
 */
public object PeerHooks {
    public interface UserRewriter {
        public fun beforePut(account: Int, user: Any)

        /** [user] as Telegram's servers know it: a copy if [beforePut] changed it, else [user] itself. */
        public fun original(account: Int, user: Any): Any
    }

    public interface ChatRewriter {
        public fun beforePut(account: Int, chat: Any)

        /** [chat] as Telegram's servers know it: a copy if [beforePut] changed it, else [chat] itself. */
        public fun original(account: Int, chat: Any): Any
    }

    public val userRewriters: Handlers<UserRewriter> = Handlers()
    public val chatRewriters: Handlers<ChatRewriter> = Handlers()

    @JvmStatic
    public fun beforeUserPut(account: Int, user: Any) {
        userRewriters.all.forEach { rewriter ->
            Faults.guard("user rewriter", fallback = Unit) { rewriter.beforePut(account, user) }
        }
    }

    @JvmStatic
    public fun beforeChatPut(account: Int, chat: Any) {
        chatRewriters.all.forEach { rewriter ->
            Faults.guard("chat rewriter", fallback = Unit) { rewriter.beforePut(account, chat) }
        }
    }

    /** [user] without what the rewriters changed, for whatever leaves the device or is stored. */
    @JvmStatic
    public fun originalUser(account: Int, user: Any?): Any? =
        user?.let { put ->
            userRewriters.all.asReversed().fold(put) { current, rewriter ->
                Faults.guard("user rewriter", fallback = current) { rewriter.original(account, current) }
            }
        }

    /** [chat] without what the rewriters changed, for whatever leaves the device or is stored. */
    @JvmStatic
    public fun originalChat(account: Int, chat: Any?): Any? =
        chat?.let { put ->
            chatRewriters.all.asReversed().fold(put) { current, rewriter ->
                Faults.guard("chat rewriter", fallback = current) { rewriter.original(account, current) }
            }
        }
}
