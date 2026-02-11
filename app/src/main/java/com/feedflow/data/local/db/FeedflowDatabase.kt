package com.feedflow.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.feedflow.data.local.db.dao.AiSummaryDao
import com.feedflow.data.local.db.dao.BookmarkDao
import com.feedflow.data.local.db.dao.CacheDao
import com.feedflow.data.local.db.dao.CommunityDao
import com.feedflow.data.local.db.dao.CoverSummaryDao
import com.feedflow.data.local.db.dao.SettingsDao
import com.feedflow.data.local.db.entity.AiSummaryEntity
import com.feedflow.data.local.db.entity.BookmarkEntity
import com.feedflow.data.local.db.entity.CachedThreadEntity
import com.feedflow.data.local.db.entity.CachedTopicEntity
import com.feedflow.data.local.db.entity.CommunityEntity
import com.feedflow.data.local.db.entity.CoverSummaryEntity
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
        UrlBookmarkEntity::class,
        CoverSummaryEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class FeedflowDatabase : RoomDatabase() {
    abstract fun communityDao(): CommunityDao
    abstract fun settingsDao(): SettingsDao
    abstract fun aiSummaryDao(): AiSummaryDao
    abstract fun cacheDao(): CacheDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun coverSummaryDao(): CoverSummaryDao

    companion object {
        const val DATABASE_NAME = "feedflow.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS cover_summaries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        summary TEXT NOT NULL,
                        postsJson TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        hnCount INTEGER NOT NULL,
                        v2exCount INTEGER NOT NULL,
                        fourD4yCount INTEGER NOT NULL
                    )"""
                )
            }
        }
    }
}
