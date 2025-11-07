package de.badaix.snapcast.data.model

import kotlinx.serialization.Serializable

/**
 * Stream URI with parsed components
 */
@Serializable
data class StreamUri(
    val fragment: String,
    val host: String,
    val path: String,
    val query: Map<String, String>,
    val raw: String,
    val scheme: String
)

