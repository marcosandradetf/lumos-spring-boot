package com.lumos.lumosspring.remoteconfig

import com.lumos.lumosspring.remoteconfig.model.RemoteConfigEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository


@Repository
interface RemoteConfigRepository : CrudRepository<RemoteConfigEntity, Long> {
    fun findByAppIdAndPlatformAndActiveTrue(appId: String, platform: String): RemoteConfigEntity?
}