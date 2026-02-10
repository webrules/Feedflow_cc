package com.feedflow.di

import android.content.Context
import com.feedflow.data.local.encryption.EncryptionHelper
import com.feedflow.data.local.preferences.PreferencesManager
import com.feedflow.domain.parser.OPMLParser
import com.feedflow.domain.parser.RSSParser
import com.feedflow.util.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideEncryptionHelper(@ApplicationContext context: Context): EncryptionHelper {
        return EncryptionHelper(context)
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitor(context)
    }

    @Provides
    @Singleton
    fun provideRSSParser(): RSSParser {
        return RSSParser()
    }

    @Provides
    @Singleton
    fun provideOPMLParser(): OPMLParser {
        return OPMLParser()
    }
}
