package com.eltavine.sereingram.features.join

import org.junit.Assert.*
import org.junit.Test
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account

class JoinSessionDiagnosticsTest {
    private fun session(apiId: Int, isCurrent: Boolean) = TLRPC.TL_authorization().apply {
        api_id = apiId
        current = isCurrent
        hash = 987654321L
        ip = "192.0.2.1"
    }

    private fun response(vararg sessions: TLRPC.TL_authorization) = TL_account.authorizations().apply {
        authorizations.addAll(sessions)
    }

    private fun inspect(response: TLObject?, error: TLRPC.TL_error? = null): Int? {
        var called = false
        var result: Int? = null
        JoinSessionDiagnostics(2, 42L, { 42L }) { request, done ->
            assertTrue(request is TL_account.getAuthorizations)
            done(response, error)
        }.run { called = true; result = it }
        assertTrue(called)
        return result
    }

    @Test fun identifiesCurrentAuthorizationEvenWhenAnotherDeviceUsesTheDefaultApi() {
        assertEquals(123456, inspect(response(session(11535358, false), session(123456, true))))
    }

    @Test fun missingOrAmbiguousCurrentSessionIsUnknownRatherThanTheBuildDefault() {
        assertNull(inspect(response(session(11535358, false))))
        assertNull(inspect(response(session(123456, true), session(654321, true))))
        assertNull(inspect(response(session(0, true))))
        assertNull(inspect(TLRPC.TL_boolTrue()))
        assertNull(inspect(null))
    }

    @Test fun serverFailureDoesNotPresentAnApiAsVerified() {
        assertNull(inspect(response(session(123456, true)), TLRPC.TL_error().apply { text = "TIMEOUT" }))
    }

    @Test fun accountReplacementDiscardsAnInFlightResult() {
        var owner = 42L
        var deliver: ((TLObject?, TLRPC.TL_error?) -> Unit)? = null
        val diagnostics = JoinSessionDiagnostics(2, 42L, { owner }) { _, done -> deliver = done }
        diagnostics.run { fail("The old account must not update the new account's diagnostic") }
        owner = 43L
        deliver!!(response(session(123456, true)), null)
    }

    @Test fun replacedAccountDoesNotSendTheDiagnosticRequest() {
        JoinSessionDiagnostics(2, 42L, { 43L }) { _, _ -> fail("Wrong account") }
            .run { fail("Wrong account") }
    }
}
