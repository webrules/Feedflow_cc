package com.feedflow.data.remote.dto

import com.google.gson.annotations.SerializedName

data class HNItem(
    @SerializedName("id") val id: Int,
    @SerializedName("type") val type: String?,
    @SerializedName("by") val by: String?,
    @SerializedName("time") val time: Int,
    @SerializedName("text") val text: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("score") val score: Int?,
    @SerializedName("descendants") val descendants: Int?,
    @SerializedName("kids") val kids: List<Int>?
)
