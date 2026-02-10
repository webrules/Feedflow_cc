package com.feedflow.data.local.db.entity

import androidx.room.Entity
import com.feedflow.data.model.Community

@Entity(
    tableName = "communities",
    primaryKeys = ["id", "serviceId"]
)
data class CommunityEntity(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val activeToday: Int,
    val onlineNow: Int,
    val serviceId: String
) {
    fun toCommunity(): Community = Community(
        id = id,
        name = name,
        description = description,
        category = category,
        activeToday = activeToday,
        onlineNow = onlineNow
    )

    companion object {
        fun fromCommunity(community: Community, serviceId: String): CommunityEntity =
            CommunityEntity(
                id = community.id,
                name = community.name,
                description = community.description,
                category = community.category,
                activeToday = community.activeToday,
                onlineNow = community.onlineNow,
                serviceId = serviceId
            )
    }
}
