package com.hoshi.reader.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Highlight(
    val id: String,
    val color: String,
    val start: Int,
    val offset: Int,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
