package de.badaix.snapcast.data.model

import kotlinx.serialization.Serializable

/**
 * RPC version information
 */
@Serializable
data class RpcVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
)

