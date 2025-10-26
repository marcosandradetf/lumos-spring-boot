package com.lumos.lumosspring.config

import com.lumos.lumosspring.authentication.model.TenantEntity
import com.lumos.lumosspring.util.Utils
import org.springframework.stereotype.Component
import org.springframework.context.event.EventListener
import org.springframework.data.relational.core.mapping.event.BeforeSaveEvent

@Component
class TenantEventListener {

    @EventListener
    fun handleBeforeSave(event: BeforeSaveEvent<TenantEntity>) {
        val entity = event.entity
        // Set o tenantId before to save
        entity.tenantId = Utils.getCurrentTenantId()
    }
}
