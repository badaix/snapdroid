package de.badaix.snapcast.data.api

import kotlinx.serialization.Serializable

/**
 * JSON-RPC 2.0 error object
 */
@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String
)

