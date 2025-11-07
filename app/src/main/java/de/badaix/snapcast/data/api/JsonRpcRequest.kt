package de.badaix.snapcast.data.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * JSON-RPC 2.0 request
 * @param id Request identifier (can be String, Number, or null for notifications)
 * @param method Method name
 * @param params Method parameters (can be any serializable object)
 */
@Serializable
data class JsonRpcRequest<T>(
    val id: JsonRpcId?,
    val jsonrpc: String = "2.0",
    val method: String,
    val params: T? = null
)

/**
 * JSON-RPC ID can be String, Number, or null
 */
@Serializable(with = JsonRpcIdSerializer::class)
sealed class JsonRpcId {
    data class StringId(val value: String) : JsonRpcId()
    data class NumberId(val value: Int) : JsonRpcId()
    object NullId : JsonRpcId()
}

object JsonRpcIdSerializer : KSerializer<JsonRpcId> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("JsonRpcId")

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: JsonRpcId) {
        require(encoder is JsonEncoder) { "JsonRpcId can only be serialized with JsonEncoder" }
        val element: JsonElement = when (value) {
            is JsonRpcId.StringId -> JsonPrimitive(value.value)
            is JsonRpcId.NumberId -> JsonPrimitive(value.value)
            is JsonRpcId.NullId -> JsonPrimitive(null)
        }
        encoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): JsonRpcId {
        require(decoder is JsonDecoder) { "JsonRpcId can only be deserialized with JsonDecoder" }
        val element = decoder.decodeJsonElement()
        return when {
            element is JsonPrimitive && element.isString -> JsonRpcId.StringId(element.content)
            element is JsonPrimitive && element.intOrNull != null -> JsonRpcId.NumberId(element.intOrNull!!)
            else -> JsonRpcId.NullId
        }
    }
}

