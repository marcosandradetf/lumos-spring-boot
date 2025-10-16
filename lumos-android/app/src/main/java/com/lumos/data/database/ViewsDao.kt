package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Query
import com.lumos.domain.model.InstallationView
import kotlinx.coroutines.flow.Flow

@Dao
interface ViewDao {
    @Query("""
        SELECT *
        FROM InstallationView
    """)
    fun getInstallationsHolder(): Flow<List<InstallationView>>


}


