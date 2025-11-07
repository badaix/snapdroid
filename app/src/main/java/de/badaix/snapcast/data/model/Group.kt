package de.badaix.snapcast.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Group entity
 */
@Serializable
data class Group(
    val clients: List<Client>,
    val id: String,
    val muted: Boolean,
    val name: String,
    @SerialName("stream_id")
    val streamId: String
)

