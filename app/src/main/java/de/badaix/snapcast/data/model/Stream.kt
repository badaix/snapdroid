package de.badaix.snapcast.data.model

import kotlinx.serialization.Serializable

/**
 * Stream entity
 */
@Serializable
data class Stream(
    val id: String,
    val status: String,
    val uri: StreamUri
)

