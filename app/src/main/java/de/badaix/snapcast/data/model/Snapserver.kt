package de.badaix.snapcast.data.model

import kotlinx.serialization.Serializable

/**
 * Snapserver information
 */
@Serializable
data class Snapserver(
    val controlProtocolVersion: Int,
    val name: String,
    val protocolVersion: Int,
    val version: String
)

