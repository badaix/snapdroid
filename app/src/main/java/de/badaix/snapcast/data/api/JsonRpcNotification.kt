package de.badaix.snapcast.data.api

import kotlinx.serialization.Serializable

/**
 * JSON-RPC 2.0 notification (request without id)
 * @param method Method name
 * @param params Method parameters
 */
@Serializable
data class JsonRpcNotification<T>(
    val jsonrpc: String = "2.0",
    val method: String,
    val params: T
)

