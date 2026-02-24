package com.feedflow.data.remote.api

import com.feedflow.data.remote.dto.DiscoursePostResponse
import com.feedflow.data.remote.dto.DiscourseResponse
import com.feedflow.data.remote.dto.DiscourseTopicListResponse
import com.feedflow.data.remote.dto.DiscourseTopicResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
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

    @POST("posts.json")
    @FormUrlEncoded
    suspend fun createPost(
        @Field("topic_id") topicId: Int,
        @Field("raw") content: String
    ): DiscoursePostResponse

    @POST("posts.json")
    @FormUrlEncoded
    suspend fun createPostWithReplyTo(
        @Field("topic_id") topicId: Int,
        @Field("raw") content: String,
        @Field("reply_to_post_number") replyToPostNumber: Int
    ): DiscoursePostResponse

    @POST("posts.json")
    @FormUrlEncoded
    suspend fun createTopic(
        @Field("title") title: String,
        @Field("raw") content: String,
        @Field("category") category: String
    ): DiscoursePostResponse
}
