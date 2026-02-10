package com.feedflow.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.feedflow.data.local.db.dao.AiSummaryDao
import com.feedflow.data.local.db.dao.BookmarkDao
import com.feedflow.data.local.db.dao.CacheDao
import com.feedflow.data.local.db.dao.CommunityDao
import com.feedflow.data.local.db.dao.SettingsDao
import com.feedflow.data.local.db.entity.AiSummaryEntity
import com.feedflow.data.local.db.entity.BookmarkEntity
import com.feedflow.data.local.db.entity.CachedThreadEntity
import com.feedflow.data.local.db.entity.CachedTopicEntity
import com.feedflow.data.local.db.entity.CommunityEntity
import com.feedflow.data.local.db.entity.SettingEntity
import com.feedflow.data.local.db.entity.UrlBookmarkEntity

@Database(
    entities = [
        CommunityEntity::class,
        SettingEntity::class,
        AiSummaryEntity::class,
        CachedTopicEntity::class,
        CachedThreadEntity::class,
        BookmarkEntity::class,
        UrlBookmarkEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class FeedflowDatabase : RoomDatabase() {
    abstract fun communityDao(): CommunityDao
    abstract fun settingsDao(): SettingsDao
    abstract fun aiSummaryDao(): AiSummaryDao
    abstract fun cacheDao(): CacheDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        const val DATABASE_NAME = "feedflow.db"
    }
}
