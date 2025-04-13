package io.otrade.data

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val poster: String?,
    val message: String,
)
