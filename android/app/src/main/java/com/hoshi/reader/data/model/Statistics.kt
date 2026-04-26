package com.hoshi.reader.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Statistics(
    val dateKey: String,
    val charactersRead: Int = 0,
    val readingTimeSeconds: Double = 0.0,
    val minSpeed: Double = 0.0,
    val maxSpeed: Double = 0.0,
    val lastSpeed: Double = 0.0
)
