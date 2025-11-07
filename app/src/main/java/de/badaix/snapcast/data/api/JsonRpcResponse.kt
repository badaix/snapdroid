package de.badaix.snapcast.data.api

import kotlinx.serialization.Serializable

/**
 * JSON-RPC 2.0 response
 * @param id Response identifier matching the request
 * @param result Result object (null if error occurred)
 * @param error Error object (null if successful)
 */
@Serializable
data class JsonRpcResponse<T>(
    val id: JsonRpcId?,
    val jsonrpc: String = "2.0",
    val result: T? = null,
    val error: JsonRpcError? = null
)

