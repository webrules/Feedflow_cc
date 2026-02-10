package com.feedflow.di

import com.feedflow.data.local.encryption.EncryptionHelper
import com.feedflow.data.local.preferences.PreferencesManager
import com.feedflow.data.remote.api.DiscourseApi
import com.feedflow.data.remote.api.HackerNewsApi
import com.feedflow.domain.parser.RSSParser
import com.feedflow.domain.service.DiscourseService
import com.feedflow.domain.service.ForumService
import com.feedflow.domain.service.FourD4YService
import com.feedflow.domain.service.GeminiService
import com.feedflow.domain.service.HackerNewsService
import com.feedflow.domain.service.RSSService
import com.feedflow.domain.service.V2EXService
import com.feedflow.domain.service.ZhihuService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    @Named("RSS")
    fun provideRSSService(
        client: OkHttpClient,
        preferencesManager: PreferencesManager,
        rssParser: RSSParser
    ): ForumService = RSSService(client, preferencesManager, rssParser)

    @Provides
    @Singleton
    @Named("HackerNews")
    fun provideHackerNewsService(
        api: HackerNewsApi
    ): ForumService = HackerNewsService(api)

    @Provides
    @Singleton
    @Named("Discourse")
    fun provideDiscourseService(
        api: DiscourseApi
    ): ForumService = DiscourseService(api)

    @Provides
    @Singleton
    @Named("V2EX")
    fun provideV2EXService(
        client: OkHttpClient,
        encryptionHelper: EncryptionHelper
    ): ForumService = V2EXService(client, encryptionHelper)

    @Provides
    @Singleton
    @Named("4D4Y")
    fun provideFourD4YService(
        client: OkHttpClient,
        encryptionHelper: EncryptionHelper
    ): ForumService = FourD4YService(client, encryptionHelper)

    @Provides
    @Singleton
    @Named("Zhihu")
    fun provideZhihuService(
        client: OkHttpClient,
        preferencesManager: PreferencesManager,
        encryptionHelper: EncryptionHelper
    ): ForumService = ZhihuService(client, preferencesManager, encryptionHelper)

    @Provides
    @Singleton
    fun provideGeminiService(
        preferencesManager: PreferencesManager
    ): GeminiService = GeminiService(preferencesManager)

    @Provides
    @Singleton
    fun provideForumServiceMap(
        @Named("RSS") rssService: ForumService,
        @Named("HackerNews") hackerNewsService: ForumService,
        @Named("Discourse") discourseService: ForumService,
        @Named("V2EX") v2exService: ForumService,
        @Named("4D4Y") fourD4YService: ForumService,
        @Named("Zhihu") zhihuService: ForumService
    ): Map<String, ForumService> = mapOf(
        "rss" to rssService,
        "hackernews" to hackerNewsService,
        "linux_do" to discourseService,
        "v2ex" to v2exService,
        "4d4y" to fourD4YService,
        "zhihu" to zhihuService
    )
}
