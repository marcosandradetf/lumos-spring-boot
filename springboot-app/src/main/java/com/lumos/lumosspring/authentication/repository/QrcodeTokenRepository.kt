package com.lumos.lumosspring.authentication.repository

import com.lumos.lumosspring.authentication.model.QrcodeToken
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*


@Repository
public interface QrcodeTokenRepository : CrudRepository<QrcodeToken, UUID> {
    @Modifying
    @Query("DELETE FROM qrcode_token WHERE user_id = :userId")
    fun deleteAllByUserId(userId: UUID)

}