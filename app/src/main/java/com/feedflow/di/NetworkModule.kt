package com.feedflow.di

import com.feedflow.data.remote.WebViewCookieJar
import com.feedflow.data.remote.api.DiscourseApi
import com.feedflow.data.remote.api.HackerNewsApi
import com.feedflow.util.BrowserCompat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", BrowserCompat.USER_AGENT)
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    @Provides
    @Singleton
    @Named("HackerNews")
    fun provideHackerNewsRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://hacker-news.firebaseio.com/v0/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideHackerNewsApi(@Named("HackerNews") retrofit: Retrofit): HackerNewsApi {
        return retrofit.create(HackerNewsApi::class.java)
    }

    @Provides
    @Singleton
    @Named("Discourse")
    fun provideDiscourseRetrofit(client: OkHttpClient): Retrofit {
        val discourseClient = client.newBuilder()
            .cookieJar(WebViewCookieJar())
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                // Add browser-like headers so Cloudflare doesn't flag us
                for ((name, value) in BrowserCompat.BROWSER_HEADERS) {
                    requestBuilder.header(name, value)
                }
                requestBuilder.header("Referer", "https://linux.do/")
                requestBuilder.header("Origin", "https://linux.do")
                chain.proceed(requestBuilder.build())
            }
            .addInterceptor { chain ->
                // Detect Cloudflare challenge pages and throw a clear error
                val response = chain.proceed(chain.request())
                if (response.code in listOf(403, 503)) {
                    val body = response.peekBody(4096).string()
                    if (body.contains("cf-challenge") || body.contains("cloudflare")) {
                        response.close()
                        throw IOException(
                            "Cloudflare challenge detected. Please open Linux.do in the login browser first."
                        )
                    }
                }
                response
            }
            .build()

        return Retrofit.Builder()
            .baseUrl("https://linux.do/")
            .client(discourseClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideDiscourseApi(@Named("Discourse") retrofit: Retrofit): DiscourseApi {
        return retrofit.create(DiscourseApi::class.java)
    }
}
