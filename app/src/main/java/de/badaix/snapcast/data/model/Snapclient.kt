package de.badaix.snapcast.data.model

import kotlinx.serialization.Serializable

/**
 * Snapclient information
 */
@Serializable
data class Snapclient(
    val name: String,
    val protocolVersion: Int,
    val version: String
)

