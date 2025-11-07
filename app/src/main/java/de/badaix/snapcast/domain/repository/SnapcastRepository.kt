package de.badaix.snapcast.domain.repository

import de.badaix.snapcast.data.model.Client
import de.badaix.snapcast.data.model.Group
import de.badaix.snapcast.data.model.RpcVersion
import de.badaix.snapcast.data.model.Server
import de.badaix.snapcast.data.model.Stream
import de.badaix.snapcast.data.model.Volume
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Snapcast JSON-RPC API
 * Following clean architecture principles - domain layer
 */
interface SnapcastRepository {
    
    // ==================== Server Methods ====================
    
    /**
     * Get RPC version
     */
    suspend fun getRpcVersion(): Result<RpcVersion>
    
    /**
     * Get server status
     */
    suspend fun getServerStatus(): Result<Server>
    
    /**
     * Delete a client
     */
    suspend fun deleteClient(clientId: String): Result<Server>
    
    /**
     * Observe server updates via TCP socket notifications
     */
    fun observeServerUpdates(): Flow<Server>
    
    // ==================== Client Methods ====================
    
    /**
     * Get client status
     */
    suspend fun getClientStatus(clientId: String): Result<Client>
    
    /**
     * Set client volume
     */
    suspend fun setClientVolume(clientId: String, volume: Volume): Result<Volume>
    
    /**
     * Set client latency
     */
    suspend fun setClientLatency(clientId: String, latency: Int): Result<Int>
    
    /**
     * Set client name
     */
    suspend fun setClientName(clientId: String, name: String): Result<String>
    
    /**
     * Observe client volume changes
     */
    fun observeClientVolumeChanges(): Flow<Pair<String, Volume>>
    
    /**
     * Observe client latency changes
     */
    fun observeClientLatencyChanges(): Flow<Pair<String, Int>>
    
    /**
     * Observe client name changes
     */
    fun observeClientNameChanges(): Flow<Pair<String, String>>
    
    /**
     * Observe client connections
     */
    fun observeClientConnections(): Flow<Client>
    
    /**
     * Observe client disconnections
     */
    fun observeClientDisconnections(): Flow<Client>
    
    // ==================== Group Methods ====================
    
    /**
     * Get group status
     */
    suspend fun getGroupStatus(groupId: String): Result<Group>
    
    /**
     * Set group mute state
     */
    suspend fun setGroupMute(groupId: String, mute: Boolean): Result<Boolean>
    
    /**
     * Set group stream
     */
    suspend fun setGroupStream(groupId: String, streamId: String): Result<String>
    
    /**
     * Set group clients
     */
    suspend fun setGroupClients(groupId: String, clientIds: List<String>): Result<Server>
    
    /**
     * Set group name
     */
    suspend fun setGroupName(groupId: String, name: String): Result<String>
    
    /**
     * Observe group mute changes
     */
    fun observeGroupMuteChanges(): Flow<Pair<String, Boolean>>
    
    /**
     * Observe group stream changes
     */
    fun observeGroupStreamChanges(): Flow<Pair<String, String>>
    
    /**
     * Observe group name changes
     */
    fun observeGroupNameChanges(): Flow<Pair<String, String>>
    
    // ==================== Stream Methods ====================
    
    /**
     * Control stream (play, pause, next, previous, seek, etc.)
     */
    suspend fun controlStream(streamId: String, command: String, params: Map<String, String> = emptyMap()): Result<String>
    
    /**
     * Set stream property
     */
    suspend fun setStreamProperty(streamId: String, property: String, value: String): Result<String>
    
    /**
     * Add stream
     */
    suspend fun addStream(streamUri: String): Result<String>
    
    /**
     * Remove stream
     */
    suspend fun removeStream(streamId: String): Result<String>
    
    /**
     * Observe stream updates
     */
    fun observeStreamUpdates(): Flow<Stream>
    
    /**
     * Observe stream properties
     */
    fun observeStreamProperties(): Flow<Pair<String, Map<String, String>>>
    
    // ==================== Connection Management ====================
    
    /**
     * Connect to Snapcast server
     * @param host The hostname or IP address of the Snapcast server
     * @param port The port number (default is 1705 for raw TCP socket)
     */
    suspend fun connect(host: String, port: Int = 1705)
    
    /**
     * Disconnect from Snapcast server
     */
    suspend fun disconnect()
    
    /**
     * Check if connected
     */
    fun isConnected(): Boolean
}

