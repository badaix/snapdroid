package de.badaix.snapcast.data.model

import kotlinx.serialization.Serializable

/**
 * Host information
 */
@Serializable
data class Host(
    val arch: String,
    val ip: String,
    val mac: String,
    val name: String,
    val os: String
)

