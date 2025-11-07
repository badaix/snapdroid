package de.badaix.snapcast.data.model

import kotlinx.serialization.Serializable

/**
 * Client entity
 */
@Serializable
data class Client(
    val config: ClientConfig,
    val connected: Boolean,
    val host: Host,
    val id: String,
    val lastSeen: LastSeen,
    val snapclient: Snapclient
)

