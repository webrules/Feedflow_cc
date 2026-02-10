package com.feedflow.data.remote.api

import com.feedflow.data.remote.dto.HNItem
import retrofit2.http.GET
import retrofit2.http.Path

interface HackerNewsApi {
    @GET("topstories.json")
    suspend fun getTopStories(): List<Int>

    @GET("newstories.json")
    suspend fun getNewStories(): List<Int>

    @GET("beststories.json")
    suspend fun getBestStories(): List<Int>

    @GET("showstories.json")
    suspend fun getShowStories(): List<Int>

    @GET("askstories.json")
    suspend fun getAskStories(): List<Int>

    @GET("jobstories.json")
    suspend fun getJobStories(): List<Int>

    @GET("item/{id}.json")
    suspend fun getItem(@Path("id") id: Int): HNItem?
}
