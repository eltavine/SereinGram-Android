package com.eltavine.sereingram.features.services

import com.eltavine.sereingram.hooks.UpstreamService
import kotlin.test.Test
import kotlin.test.assertTrue

class NagramServicesPolicyTest {
    @Test
    fun refusesEveryNagramService() {
        assertTrue(UpstreamService.entries.none(NagramServicesPolicy::allow))
    }
}
