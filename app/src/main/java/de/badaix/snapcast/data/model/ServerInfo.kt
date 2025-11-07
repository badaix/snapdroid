package de.badaix.snapcast.data.model

import kotlinx.serialization.Serializable

/**
 * Server information
 */
@Serializable
data class ServerInfo(
    val host: Host,
    val snapserver: Snapserver
)

