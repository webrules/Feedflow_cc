package com.feedflow.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Community(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val activeToday: Int = 0,
    val onlineNow: Int = 0
)
