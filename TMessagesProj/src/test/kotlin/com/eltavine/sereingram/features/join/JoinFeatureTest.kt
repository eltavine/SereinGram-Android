package com.eltavine.sereingram.features.join

import org.junit.Assert.*
import org.junit.Test
import org.telegram.tgnet.TLRPC

class JoinFeatureTest {
    @Test fun handlesBothJoinMethodsWithoutTreatingOtherFailuresAsMembershipLimits() {
        val publicJoin = TLRPC.TL_channels_joinChannel()
        val inviteJoin = TLRPC.TL_messages_importChatInvite()
        assertTrue(JoinFeature.isJoinLimit(publicJoin, "CHANNELS_TOO_MUCH"))
        assertTrue(JoinFeature.isJoinLimit(inviteJoin, "CHANNELS_TOO_MUCH"))
        assertFalse(JoinFeature.isJoinLimit(inviteJoin, "INVITE_HASH_EXPIRED"))
        assertFalse(JoinFeature.isJoinLimit(publicJoin, "FLOOD_WAIT_120"))
        assertFalse(JoinFeature.isJoinLimit(TLRPC.TL_channels_inviteToChannel(), "USER_CHANNELS_TOO_MUCH"))
        assertFalse(JoinFeature.isJoinLimit(TLRPC.TL_channels_createChannel(), "CHANNELS_TOO_MUCH"))
    }
}
