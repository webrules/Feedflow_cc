package com.feedflow.data.repository

import com.feedflow.data.local.db.dao.CommunityDao
import com.feedflow.data.local.db.entity.CommunityEntity
import com.feedflow.data.model.Community
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRepository @Inject constructor(
    private val communityDao: CommunityDao
) {
    fun getCommunitiesByService(serviceId: String): Flow<List<Community>> {
        return communityDao.getCommunitiesByService(serviceId).map { entities ->
            entities.map { it.toCommunity() }
        }
    }

    suspend fun getCommunitiesByServiceOnce(serviceId: String): List<Community> {
        return communityDao.getCommunitiesByServiceOnce(serviceId).map { it.toCommunity() }
    }

    suspend fun saveCommunities(communities: List<Community>, serviceId: String) {
        val entities = communities.map { CommunityEntity.fromCommunity(it, serviceId) }
        communityDao.deleteCommunities(serviceId)
        communityDao.insertCommunities(entities)
    }

    suspend fun deleteCommunities(serviceId: String) {
        communityDao.deleteCommunities(serviceId)
    }

    suspend fun deleteAll() {
        communityDao.deleteAll()
    }
}
