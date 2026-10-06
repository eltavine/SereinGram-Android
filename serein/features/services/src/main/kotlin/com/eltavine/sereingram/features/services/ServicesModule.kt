package com.eltavine.sereingram.features.services

import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.UpstreamService
import com.eltavine.sereingram.hooks.UpstreamServices

/**
 * Keeps the client off Nagram's own infrastructure: SereinGram's crashes are
 * not Nagram's to collect, and Nagram's APKs are not updates for SereinGram.
 */
public object ServicesModule : SereinModule {
    override val id: String = "services"

    override fun start(context: ModuleContext) {
        UpstreamServices.policies.install(NagramServicesPolicy)
    }
}

internal object NagramServicesPolicy : UpstreamServices.Policy {
    private val refused = setOf(UpstreamService.CRASH_REPORTS, UpstreamService.UPDATE_CHECK)

    override fun allow(service: UpstreamService): Boolean = service !in refused
}
