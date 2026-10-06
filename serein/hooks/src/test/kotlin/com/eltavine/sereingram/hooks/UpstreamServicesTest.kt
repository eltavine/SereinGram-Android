package com.eltavine.sereingram.hooks

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpstreamServicesTest {
    @Test
    fun everythingIsAllowedWithoutAPolicy() {
        assertTrue(UpstreamServices.allowCrashReports())
        assertTrue(UpstreamServices.allowUpdateCheck())
    }

    @Test
    fun anyPolicyCanRefuseAService() {
        val allowAll = UpstreamServices.policies.install { true }
        val refuseUpdates = UpstreamServices.policies.install { it != UpstreamService.UPDATE_CHECK }
        try {
            assertTrue(UpstreamServices.allowCrashReports())
            assertFalse(UpstreamServices.allowUpdateCheck())
        } finally {
            refuseUpdates.close()
            allowAll.close()
        }
        assertTrue(UpstreamServices.allowUpdateCheck())
    }
}
