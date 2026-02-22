package com.feedflow.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DiscourseResponse(
    @SerializedName("category_list") val categoryList: DiscourseCategoryList?
)

data class DiscourseCategoryList(
    @SerializedName("categories") val categories: List<DiscourseCategory>?
)

data class DiscourseCategory(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String,
    @SerializedName("description") val description: String?,
    @SerializedName("topic_count") val topicCount: Int?,
    @SerializedName("post_count") val postCount: Int?
)

data class DiscourseTopicListResponse(
    @SerializedName("topic_list") val topicList: DiscourseTopicList?
)

data class DiscourseTopicList(
    @SerializedName("topics") val topics: List<DiscourseTopic>?
)

data class DiscourseTopic(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("fancy_title") val fancyTitle: String?,
    @SerializedName("slug") val slug: String,
    @SerializedName("posts_count") val postsCount: Int,
    @SerializedName("reply_count") val replyCount: Int?,
    @SerializedName("like_count") val likeCount: Int?,
    @SerializedName("views") val views: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("bumped_at") val bumpedAt: String?,
    @SerializedName("category_id") val categoryId: Int?,
    @SerializedName("posters") val posters: List<DiscoursePoster>?
)

data class DiscoursePoster(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("description") val description: String?
)

data class DiscourseTopicResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("fancy_title") val fancyTitle: String?,
    @SerializedName("posts_count") val postsCount: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("views") val views: Int,
    @SerializedName("like_count") val likeCount: Int?,
    @SerializedName("category_id") val categoryId: Int?,
    @SerializedName("post_stream") val postStream: DiscoursePostStream?,
    @SerializedName("details") val details: DiscourseTopicDetails?
)

data class DiscoursePostStream(
    @SerializedName("posts") val posts: List<DiscoursePost>?
)

data class DiscoursePost(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int?,
    @SerializedName("username") val username: String,
    @SerializedName("avatar_template") val avatarTemplate: String,
    @SerializedName("cooked") val cooked: String, // HTML content
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("post_number") val postNumber: Int,
    @SerializedName("score") val score: Double?,
    @SerializedName("primary_group_name") val primaryGroupName: String?,
    @SerializedName("admin") val admin: Boolean?,
    @SerializedName("moderator") val moderator: Boolean?
)

data class DiscourseTopicDetails(
    @SerializedName("created_by") val createdBy: DiscourseUser?
)

data class DiscourseUser(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("avatar_template") val avatarTemplate: String?
)
