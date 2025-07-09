package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lumos.domain.model.MaterialStock
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterials(materials: List<MaterialStock>)

    @Query(
        """
        select * from material_stock order by stockAvailable
    """
    )
    fun getMaterialsFlow(): Flow<List<MaterialStock>>

}
