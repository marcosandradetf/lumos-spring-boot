package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lumos.domain.model.OperationalUser
import com.lumos.domain.model.Team
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<OperationalUser>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeams(teams: List<Team>)

    @Query("SELECT * FROM OperationalUser")
    fun getUsersFlow(): Flow<List<OperationalUser>>

    @Query("SELECT * FROM Team")
    fun getTeamsFlow(): Flow<List<Team>>

}
