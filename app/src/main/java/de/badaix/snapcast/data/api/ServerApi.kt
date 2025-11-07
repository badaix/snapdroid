package de.badaix.snapcast.data.api

import de.badaix.snapcast.data.model.RpcVersion
import de.badaix.snapcast.data.model.Server
import kotlinx.serialization.Serializable

// ==================== Server.GetRPCVersion ====================

/**
 * Server.GetRPCVersion request (no parameters)
 */
@Serializable
object ServerGetRPCVersionParams

/**
 * Server.GetRPCVersion response result
 */
@Serializable
data class ServerGetRPCVersionResult(
    val major: Int,
    val minor: Int,
    val patch: Int
) {
    fun toRpcVersion(): RpcVersion = RpcVersion(major, minor, patch)
}

// ==================== Server.GetStatus ====================

/**
 * Server.GetStatus request (no parameters)
 */
@Serializable
object ServerGetStatusParams

/**
 * Server.GetStatus response result
 */
@Serializable
data class ServerGetStatusResult(
    val server: Server
)

// ==================== Server.DeleteClient ====================

/**
 * Server.DeleteClient request parameters
 */
@Serializable
data class ServerDeleteClientParams(
    val id: String
)

/**
 * Server.DeleteClient response result
 */
@Serializable
data class ServerDeleteClientResult(
    val server: Server
)

// ==================== Server Notifications ====================

/**
 * Server.OnUpdate notification parameters
 */
@Serializable
data class ServerOnUpdateParams(
    val server: Server
)

