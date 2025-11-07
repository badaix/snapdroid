package de.badaix.snapcast.data.model

import kotlinx.serialization.Serializable

/**
 * Server entity containing groups, server info, and streams
 */
@Serializable
data class Server(
    val groups: List<Group>,
    val server: ServerInfo,
    val streams: List<Stream>
)

