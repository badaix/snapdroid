package de.badaix.snapcast.data.remote

import de.badaix.snapcast.data.api.JsonRpcId
import de.badaix.snapcast.data.api.JsonRpcNotification
import de.badaix.snapcast.data.api.JsonRpcRequest
import de.badaix.snapcast.data.api.JsonRpcResponse
import timber.log.Timber
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.coroutineContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

/**
 * API service for communicating with Snapcast JSON-RPC API
 * Uses raw TCP socket connection with newline-delimited JSON (ndjson)
 * Supports changing host and port at runtime for connecting to different servers
 */
class SnapcastApiService(
    host: String = "localhost",
    port: Int = 1705
) {
    val json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    var host: String = host
        private set
    var port: Int = port
        private set
    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    var writer: PrintWriter? = null
    private var readJob: Job? = null
    var requestIdCounter = 0
    val pendingRequests = mutableMapOf<Int, CompletableDeferred<JsonRpcResponse<JsonObject>>>()
    private val notificationChannel = Channel<JsonRpcNotification<JsonObject>>(Channel.UNLIMITED)

    val isConnected: Boolean
        get() = socket != null && socket?.isConnected == true

    /**
     * Update the host and port for the connection
     * If already connected, this will disconnect first
     */
    suspend fun updateConnection(host: String, port: Int) {
        withContext(Dispatchers.IO) {
            val wasConnected = isConnected
            val hostChanged = this@SnapcastApiService.host != host
            val portChanged = this@SnapcastApiService.port != port
            
            if (wasConnected && (hostChanged || portChanged)) {
                Timber.d("Host or port changed, disconnecting from current server")
                disconnectSocket()
            }
            
            this@SnapcastApiService.host = host
            this@SnapcastApiService.port = port
            Timber.d("Connection settings updated: $host:$port")
        }
    }

    /**
     * Connect TCP socket for receiving notifications
     * If already connected, will disconnect first before connecting to new server
     */
    suspend fun connectSocket() {
        withContext(Dispatchers.IO) {
            try {
                // Disconnect if already connected
                if (isConnected) {
                    Timber.d("Already connected, disconnecting first")
                    disconnectSocket()
                }
                
                Timber.d("Connecting TCP socket to: $host:$port")
                socket = Socket(host, port)
                socket!!.tcpNoDelay = true
                reader = BufferedReader(InputStreamReader(socket?.getInputStream()))
                writer = PrintWriter(socket!!.getOutputStream(), true)
                
                // Start reading messages in background
                readJob = scope.launch {
                    readMessages()
                }
                
                Timber.i("TCP socket connected successfully to $host:$port")
            } catch (e: Exception) {
                Timber.e(e, "Failed to connect TCP socket to $host:$port")
                disconnectSocket()
                throw SnapcastApiException("Failed to connect TCP socket: ${e.message}", e)
            }
        }
    }

    /**
     * Disconnect TCP socket
     */
    suspend fun disconnectSocket() {
        withContext(Dispatchers.IO) {
            Timber.d("Disconnecting TCP socket")
            readJob?.cancel()
            readJob = null
            try {
                reader?.close()
                writer?.close()
                socket?.close()
            } catch (e: Exception) {
                Timber.w(e, "Error closing socket resources")
            }
            reader = null
            writer = null
            socket = null
            // Cancel all pending requests
            pendingRequests.values.forEach { it.cancel() }
            pendingRequests.clear()
            Timber.i("TCP socket disconnected")
        }
    }

    /**
     * Read messages from TCP socket (newline-delimited JSON)
     */
    private suspend fun readMessages() {
        withContext(Dispatchers.IO) {
            val bufferedReader = reader ?: return@withContext
            try {
                while (coroutineContext.isActive && socket?.isConnected == true) {
                    val line = bufferedReader.readLine() ?: break
                    if (line.isBlank()) continue
                    
                    try {
                        processMessage(line)
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to process message: $line")
                    }
                }
            } catch (e: Exception) {
                if (coroutineContext.isActive) {
                    Timber.e(e, "Error reading from TCP socket, connection may be closed")
                }
            }
        }
    }

    /**
     * Process a received JSON message (response or notification)
     */
    private suspend fun processMessage(line: String) {
        val jsonElement = json.parseToJsonElement(line)
        val jsonObject = jsonElement.jsonObject

        // Check if it's a response to a pending request
        if (jsonObject.containsKey("id") && (jsonObject.containsKey("result") || jsonObject.containsKey("error"))) {
            val idElement = jsonObject["id"]
            if (idElement != null) {
                val id = when {
                    idElement.jsonPrimitive.isString -> idElement.jsonPrimitive.content.toIntOrNull()
                    idElement.jsonPrimitive.intOrNull != null -> idElement.jsonPrimitive.intOrNull
                    else -> null
                }
                id?.let {
                    Timber.v("Received JSON-RPC response [id=$it]")
                    pendingRequests.remove(it)?.complete(
                        json.decodeFromJsonElement<JsonRpcResponse<JsonObject>>(jsonElement)
                    )
                    return
                }
            }
        }

        // Otherwise, it's a notification (has method and params, but no id)
        if (jsonObject.containsKey("method") && jsonObject.containsKey("params") && !jsonObject.containsKey("id")) {
            val notification = json.decodeFromJsonElement<JsonRpcNotification<JsonObject>>(jsonElement)
            Timber.d("Received JSON-RPC notification: ${notification.method}")
            // Send notification to channel for observeNotifications flow
            notificationChannel.trySend(notification)
        }
    }

    /**
     * Send JSON-RPC request
     */
    suspend inline fun <T, reified R> sendRequest(
        method: String,
        params: T?,
    ): Result<R> {
        return withContext(Dispatchers.IO) {
            try {
                val currentWriter = writer ?: run {
                    Timber.e("TCP socket not connected, cannot send request: $method")
                    throw SnapcastApiException("TCP socket not connected")
                }
                
                val id = JsonRpcId.NumberId(++requestIdCounter)
                val request = JsonRpcRequest(id, "2.0", method, params)

                Timber.d("Sending JSON-RPC request [id=$requestIdCounter]: $method with params: $params")

                val deferred = CompletableDeferred<JsonRpcResponse<JsonObject>>()
                pendingRequests[id.value] = deferred

                // Serialize request and append newline (ndjson format)
                val requestJson = json.encodeToString(request)
                currentWriter.println(requestJson)
                currentWriter.flush()

                val response = deferred.await()
                pendingRequests.remove(id.value)

                if (response.error != null) {
                    Timber.e("JSON-RPC error [id=$requestIdCounter, method=$method]: ${response.error.message} (code: ${response.error.code})")
                    Result.failure(
                        SnapcastApiException(
                            "JSON-RPC Error: ${response.error.message}",
                            code = response.error.code
                        )
                    )
                } else {
                    val result = json.decodeFromJsonElement<R>(response.result!!)
                    Timber.d("JSON-RPC request successful [id=$requestIdCounter, method=$method]")
                    Result.success(result)
                }
            } catch (e: Exception) {
                Timber.e(e, "TCP socket request failed [method=$method]")
                Result.failure(SnapcastApiException("TCP socket request failed: ${e.message}", e))
            }
        }
    }

    /**
     * Observe TCP socket notifications
     */
    fun observeNotifications(): Flow<JsonRpcNotification<JsonObject>> = flow {
        if (socket == null || socket?.isConnected != true) {
            Timber.w("TCP socket is not connected, cannot observe notifications")
            return@flow
        }
        Timber.d("Starting to observe TCP socket notifications")
        try {
            while (coroutineContext.isActive && socket?.isConnected == true) {
                val notification = notificationChannel.receive()
                emit(notification)
            }
        } catch (e: Exception) {
            if (coroutineContext.isActive) {
                Timber.e(e, "TCP socket notification observer error, connection may be closed")
            }
        } finally {
            Timber.d("Stopped observing TCP socket notifications")
        }
    }

    /**
     * Close TCP socket connection
     */
    suspend fun close() {
        Timber.d("Closing TCP socket connection")
        disconnectSocket()
        notificationChannel.close()
        scope.cancel()
        Timber.i("TCP socket connection closed")
    }
}

/**
 * Custom exception for Snapcast API errors
 */
class SnapcastApiException(
    message: String,
    cause: Throwable? = null,
    val code: Int? = null
) : Exception(message, cause)

