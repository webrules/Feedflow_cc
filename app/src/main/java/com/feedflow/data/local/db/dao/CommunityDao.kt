package com.feedflow.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.feedflow.data.local.db.entity.CommunityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommunityDao {
    @Query("SELECT * FROM communities WHERE serviceId = :serviceId")
    fun getCommunitiesByService(serviceId: String): Flow<List<CommunityEntity>>

    @Query("SELECT * FROM communities WHERE serviceId = :serviceId")
    suspend fun getCommunitiesByServiceOnce(serviceId: String): List<CommunityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommunities(communities: List<CommunityEntity>)

    @Query("DELETE FROM communities WHERE serviceId = :serviceId")
    suspend fun deleteCommunities(serviceId: String)

    @Query("DELETE FROM communities")
    suspend fun deleteAll()
}
