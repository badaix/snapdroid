package de.badaix.snapcast.data.model

import kotlinx.serialization.Serializable

// Client.GetStatus
@Serializable
data class ClientGetStatusRequest(
    val id: String
)

@Serializable
data class ClientGetStatusResponse(
    val client: Client
)

// Client.SetVolume
@Serializable
data class ClientSetVolumeRequest(
    val id: String,
    val volume: Volume
)

@Serializable
data class ClientSetVolumeResponse(
    val volume: Volume
)

@Serializable
data class ClientOnVolumeChangedNotification(
    val id: String,
    val volume: Volume
)

// Client.SetLatency
@Serializable
data class ClientSetLatencyRequest(
    val id: String,
    val latency: Int
)

@Serializable
data class ClientSetLatencyResponse(
    val latency: Int
)

@Serializable
data class ClientOnLatencyChangedNotification(
    val id: String,
    val latency: Int
)

// Client.SetName
@Serializable
data class ClientSetNameRequest(
    val id: String,
    val name: String
)

@Serializable
data class ClientSetNameResponse(
    val name: String
)

@Serializable
data class ClientOnNameChangedNotification(
    val id: String,
    val name: String
)

// Client.OnConnect
@Serializable
data class ClientOnConnectNotification(
    val client: Client,
    val id: String
)

// Client.OnDisconnect
@Serializable
data class ClientOnDisconnectNotification(
    val client: Client,
    val id: String
)

