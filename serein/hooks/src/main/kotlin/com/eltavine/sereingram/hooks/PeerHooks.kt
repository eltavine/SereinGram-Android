package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/**
 * Users and chats as they enter Telegram's memory, where every screen reads
 * them from. The objects are Telegram's; rewriters may change them.
 */
public object PeerHooks {
    public fun interface UserRewriter {
        public fun beforePut(account: Int, user: Any)
    }

    public fun interface ChatRewriter {
        public fun beforePut(account: Int, chat: Any)
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
}
