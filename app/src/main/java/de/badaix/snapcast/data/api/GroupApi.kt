package de.badaix.snapcast.data.api

import de.badaix.snapcast.data.model.Group
import de.badaix.snapcast.data.model.Server
import kotlinx.serialization.Serializable

// ==================== Group.GetStatus ====================

/**
 * Group.GetStatus request parameters
 */
@Serializable
data class GroupGetStatusParams(
    val id: String
)

/**
 * Group.GetStatus response result
 */
@Serializable
data class GroupGetStatusResult(
    val group: Group
)

// ==================== Group.SetMute ====================

/**
 * Group.SetMute request parameters
 */
@Serializable
data class GroupSetMuteParams(
    val id: String,
    val mute: Boolean
)

/**
 * Group.SetMute response result
 */
@Serializable
data class GroupSetMuteResult(
    val mute: Boolean
)

/**
 * Group.SetMute notification parameters
 */
@Serializable
data class GroupOnMuteParams(
    val id: String,
    val mute: Boolean
)

// ==================== Group.SetStream ====================

/**
 * Group.SetStream request parameters
 */
@Serializable
data class GroupSetStreamParams(
    val id: String,
    @kotlinx.serialization.SerialName("stream_id")
    val streamId: String
)

/**
 * Group.SetStream response result
 */
@Serializable
data class GroupSetStreamResult(
    @kotlinx.serialization.SerialName("stream_id")
    val streamId: String
)

/**
 * Group.SetStream notification parameters
 */
@Serializable
data class GroupOnStreamChangedParams(
    val id: String,
    @kotlinx.serialization.SerialName("stream_id")
    val streamId: String
)

// ==================== Group.SetClients ====================

/**
 * Group.SetClients request parameters
 */
@Serializable
data class GroupSetClientsParams(
    val clients: List<String>,
    val id: String
)

/**
 * Group.SetClients response result
 */
@Serializable
data class GroupSetClientsResult(
    val server: Server
)

// ==================== Group.SetName ====================

/**
 * Group.SetName request parameters
 */
@Serializable
data class GroupSetNameParams(
    val id: String,
    val name: String
)

/**
 * Group.SetName response result
 */
@Serializable
data class GroupSetNameResult(
    val name: String
)

/**
 * Group.SetName notification parameters
 */
@Serializable
data class GroupOnNameChangedParams(
    val id: String,
    val name: String
)

