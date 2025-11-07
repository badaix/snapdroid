package de.badaix.snapcast.data.api

import de.badaix.snapcast.data.model.Client
import de.badaix.snapcast.data.model.LastSeen
import de.badaix.snapcast.data.model.Volume
import kotlinx.serialization.Serializable

// ==================== Client.GetStatus ====================

/**
 * Client.GetStatus request parameters
 */
@Serializable
data class ClientGetStatusParams(
    val id: String
)

/**
 * Client.GetStatus response result
 */
@Serializable
data class ClientGetStatusResult(
    val client: Client
)

// ==================== Client.SetVolume ====================

/**
 * Client.SetVolume request parameters
 */
@Serializable
data class ClientSetVolumeParams(
    val id: String,
    val volume: Volume
)

/**
 * Client.SetVolume response result
 */
@Serializable
data class ClientSetVolumeResult(
    val volume: Volume
)

/**
 * Client.SetVolume notification parameters
 */
@Serializable
data class ClientOnVolumeChangedParams(
    val id: String,
    val volume: Volume
)

// ==================== Client.SetLatency ====================

/**
 * Client.SetLatency request parameters
 */
@Serializable
data class ClientSetLatencyParams(
    val id: String,
    val latency: Int
)

/**
 * Client.SetLatency response result
 */
@Serializable
data class ClientSetLatencyResult(
    val latency: Int
)

/**
 * Client.SetLatency notification parameters
 */
@Serializable
data class ClientOnLatencyChangedParams(
    val id: String,
    val latency: Int
)

// ==================== Client.SetName ====================

/**
 * Client.SetName request parameters
 */
@Serializable
data class ClientSetNameParams(
    val id: String,
    val name: String
)

/**
 * Client.SetName response result
 */
@Serializable
data class ClientSetNameResult(
    val name: String
)

/**
 * Client.SetName notification parameters
 */
@Serializable
data class ClientOnNameChangedParams(
    val id: String,
    val name: String
)

// ==================== Client Notifications ====================

/**
 * Client.OnConnect notification parameters
 */
@Serializable
data class ClientOnConnectParams(
    val client: Client,
    val id: String
)

/**
 * Client.OnDisconnect notification parameters
 */
@Serializable
data class ClientOnDisconnectParams(
    val client: Client,
    val id: String
)

