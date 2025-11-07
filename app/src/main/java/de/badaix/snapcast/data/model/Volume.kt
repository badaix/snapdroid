package de.badaix.snapcast.data.model

import kotlinx.serialization.Serializable

/**
 * Volume configuration for a client
 */
@Serializable
data class Volume(
    val muted: Boolean,
    val percent: Int
)

