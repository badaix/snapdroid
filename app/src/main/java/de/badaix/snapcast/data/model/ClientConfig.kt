package de.badaix.snapcast.data.model

import kotlinx.serialization.Serializable

/**
 * Client configuration
 */
@Serializable
data class ClientConfig(
    val instance: Int,
    val latency: Int,
    val name: String,
    val volume: Volume
)

