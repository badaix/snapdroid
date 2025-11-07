package de.badaix.snapcast.data.model

import kotlinx.serialization.Serializable

/**
 * Last seen timestamp
 */
@Serializable
data class LastSeen(
    val sec: Long,
    val usec: Long
)

