package com.eltavine.sereingram.features.join

import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account

/** Read only the current authorization's API ID; never retain session hashes, IPs or other devices. */
internal class JoinSessionDiagnostics(
    private val account: Int,
    private val owner: Long,
    private val currentOwner: () -> Long = { UserConfig.getInstance(account).clientUserId },
    private val send: (TLObject, (TLObject?, TLRPC.TL_error?) -> Unit) -> Unit = { request, done ->
        ConnectionsManager.getInstance(account).sendRequest(request, { response, error ->
            AndroidUtilities.runOnUIThread { done(response, error) }
        }, ConnectionsManager.RequestFlagFailOnServerErrors)
    },
) {
    fun run(done: (Int?) -> Unit) {
        if (currentOwner() != owner) return
        send(TL_account.getAuthorizations()) { response, error ->
            if (currentOwner() != owner) return@send
            val session = if (error == null) {
                (response as? TL_account.authorizations)?.authorizations?.filter { it.current }?.singleOrNull()
            } else null
            done(session?.api_id?.takeIf { it > 0 })
        }
    }
}
