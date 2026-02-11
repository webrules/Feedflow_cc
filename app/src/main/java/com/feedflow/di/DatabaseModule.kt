package com.feedflow.di

import android.content.Context
import androidx.room.Room
import com.feedflow.data.local.db.FeedflowDatabase
import com.feedflow.data.local.db.dao.AiSummaryDao
import com.feedflow.data.local.db.dao.BookmarkDao
import com.feedflow.data.local.db.dao.CacheDao
import com.feedflow.data.local.db.dao.CommunityDao
import com.feedflow.data.local.db.dao.CoverSummaryDao
import com.feedflow.data.local.db.dao.SettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FeedflowDatabase {
        return Room.databaseBuilder(
            context,
            FeedflowDatabase::class.java,
            FeedflowDatabase.DATABASE_NAME
        )
            .addMigrations(FeedflowDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideCommunityDao(database: FeedflowDatabase): CommunityDao {
        return database.communityDao()
    }

    @Provides
    fun provideSettingsDao(database: FeedflowDatabase): SettingsDao {
        return database.settingsDao()
    }

    @Provides
    fun provideAiSummaryDao(database: FeedflowDatabase): AiSummaryDao {
        return database.aiSummaryDao()
    }

    @Provides
    fun provideCacheDao(database: FeedflowDatabase): CacheDao {
        return database.cacheDao()
    }

    @Provides
    fun provideBookmarkDao(database: FeedflowDatabase): BookmarkDao {
        return database.bookmarkDao()
    }

    @Provides
    fun provideCoverSummaryDao(database: FeedflowDatabase): CoverSummaryDao {
        return database.coverSummaryDao()
    }
}
