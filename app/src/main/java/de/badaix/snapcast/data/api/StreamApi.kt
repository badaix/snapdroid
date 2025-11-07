package de.badaix.snapcast.data.api

import de.badaix.snapcast.data.model.Stream
import kotlinx.serialization.Serializable

// ==================== Stream.Control ====================

/**
 * Stream.Control request parameters
 */
@Serializable
data class StreamControlParams(
    val id: String,
    val command: String,
    val params: Map<String, String> = emptyMap()
)

/**
 * Stream.Control response result (success)
 */
@Serializable
data class StreamControlResult(
    val result: String = "ok"
)

// ==================== Stream.SetProperty ====================

/**
 * Stream.SetProperty request parameters
 */
@Serializable
data class StreamSetPropertyParams(
    val id: String,
    val property: String,
    val value: String
)

/**
 * Stream.SetProperty response result (success)
 */
@Serializable
data class StreamSetPropertyResult(
    val result: String = "ok"
)

// ==================== Stream.AddStream ====================

/**
 * Stream.AddStream request parameters
 */
@Serializable
data class StreamAddStreamParams(
    @kotlinx.serialization.SerialName("streamUri")
    val streamUri: String
)

/**
 * Stream.AddStream response result
 */
@Serializable
data class StreamAddStreamResult(
    @kotlinx.serialization.SerialName("stream_id")
    val streamId: String
)

// ==================== Stream.RemoveStream ====================

/**
 * Stream.RemoveStream request parameters
 */
@Serializable
data class StreamRemoveStreamParams(
    val id: String
)

/**
 * Stream.RemoveStream response result
 */
@Serializable
data class StreamRemoveStreamResult(
    @kotlinx.serialization.SerialName("stream_id")
    val streamId: String
)

// ==================== Stream Notifications ====================

/**
 * Stream.OnUpdate notification parameters
 */
@Serializable
data class StreamOnUpdateParams(
    val id: String,
    val stream: Stream
)

/**
 * Stream.OnProperties notification parameters
 */
@Serializable
data class StreamOnPropertiesParams(
    val id: String,
    val metadata: Map<String, String> = emptyMap()
)

