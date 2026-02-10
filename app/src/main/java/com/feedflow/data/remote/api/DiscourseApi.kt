package com.feedflow.data.remote.api

import com.feedflow.data.remote.dto.DiscourseResponse
import com.feedflow.data.remote.dto.DiscourseTopicListResponse
import com.feedflow.data.remote.dto.DiscourseTopicResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DiscourseApi {
    @GET("categories.json")
    suspend fun getCategories(): DiscourseResponse

    @GET("c/{category}.json")
    suspend fun getCategoryTopics(
        @Path("category") category: String,
        @Query("page") page: Int = 0
    ): DiscourseTopicListResponse

    @GET("t/{topicId}.json")
    suspend fun getTopic(
        @Path("topicId") topicId: Int,
        @Query("page") page: Int = 1
    ): DiscourseTopicResponse
}
