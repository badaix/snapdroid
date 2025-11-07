package de.badaix.snapcast.data.repository

import de.badaix.snapcast.data.api.*
import de.badaix.snapcast.data.model.Client
import de.badaix.snapcast.data.model.Group
import de.badaix.snapcast.data.model.RpcVersion
import de.badaix.snapcast.data.model.Server
import de.badaix.snapcast.data.model.Stream
import de.badaix.snapcast.data.model.Volume
import de.badaix.snapcast.data.remote.SnapcastApiService
import de.badaix.snapcast.domain.repository.SnapcastRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository implementation for Snapcast JSON-RPC API
 * Following clean architecture principles - data layer
 */
@Singleton
class SnapcastRepositoryImpl @Inject constructor(
    private val apiService: SnapcastApiService
) : SnapcastRepository {

    private val notifications = apiService.observeNotifications()

    // ==================== Server Methods ====================

    override suspend fun getRpcVersion(): Result<RpcVersion> {
        Timber.d("getRpcVersion() called")
        return apiService.sendRequest<ServerGetRPCVersionParams, ServerGetRPCVersionResult>(
            method = "Server.GetRPCVersion",
            params = ServerGetRPCVersionParams
        ).map { it.toRpcVersion() }
            .also { result ->
                result.fold(
                    onSuccess = { Timber.d("getRpcVersion() succeeded: $it") },
                    onFailure = { Timber.e(it, "getRpcVersion() failed") }
                )
            }
    }

    override suspend fun getServerStatus(): Result<Server> {
        Timber.d("getServerStatus() called")
        return apiService.sendRequest<ServerGetStatusParams, ServerGetStatusResult>(
            method = "Server.GetStatus",
            params = ServerGetStatusParams
        ).map { it.server }
            .also { result ->
                result.fold(
                    onSuccess = { Timber.d("getServerStatus() succeeded: ${it.groups.size} groups, ${it.server.snapserver.version}") },
                    onFailure = { Timber.e(it, "getServerStatus() failed") }
                )
            }
    }

    override suspend fun deleteClient(clientId: String): Result<Server> {
        Timber.d("deleteClient() called with clientId: $clientId")
        return apiService.sendRequest<ServerDeleteClientParams, ServerDeleteClientResult>(
            method = "Server.DeleteClient",
            params = ServerDeleteClientParams(clientId)
        ).map { it.server }
            .also { result ->
                result.fold(
                    onSuccess = { Timber.i("deleteClient() succeeded for clientId: $clientId") },
                    onFailure = { Timber.e(it, "deleteClient() failed for clientId: $clientId") }
                )
            }
    }

    override fun observeServerUpdates(): Flow<Server> {
        Timber.d("observeServerUpdates() called")
        return notifications
            .filter { it.method == "Server.OnUpdate" }
            .map { notification ->
                val params = Json.decodeFromJsonElement<ServerOnUpdateParams>(
                    notification.params
                )
                Timber.d("Server.OnUpdate notification received: ${params.server.groups.size} groups")
                params.server
            }
    }

    // ==================== Client Methods ====================

    override suspend fun getClientStatus(clientId: String): Result<Client> {
        Timber.d("getClientStatus() called with clientId: $clientId")
        return apiService.sendRequest<ClientGetStatusParams, ClientGetStatusResult>(
            method = "Client.GetStatus",
            params = ClientGetStatusParams(clientId)
        ).map { it.client }
            .also { result ->
                result.fold(
                    onSuccess = { Timber.d("getClientStatus() succeeded for clientId: $clientId, name: ${it.config?.name}") },
                    onFailure = { Timber.e(it, "getClientStatus() failed for clientId: $clientId") }
                )
            }
    }

    override suspend fun setClientVolume(clientId: String, volume: Volume): Result<Volume> {
        Timber.d("setClientVolume() called with clientId: $clientId, volume: ${volume.percent}%")
        return apiService.sendRequest<ClientSetVolumeParams, ClientSetVolumeResult>(
            method = "Client.SetVolume",
            params = ClientSetVolumeParams(clientId, volume)
        ).map { it.volume }
            .also { result ->
                result.fold(
                    onSuccess = { Timber.i("setClientVolume() succeeded for clientId: $clientId, new volume: ${it.percent}%") },
                    onFailure = { Timber.e(it, "setClientVolume() failed for clientId: $clientId") }
                )
            }
    }

    override suspend fun setClientLatency(clientId: String, latency: Int): Result<Int> {
        Timber.d("setClientLatency() called with clientId: $clientId, latency: ${latency}ms")
        return apiService.sendRequest<ClientSetLatencyParams, ClientSetLatencyResult>(
            method = "Client.SetLatency",
            params = ClientSetLatencyParams(clientId, latency)
        ).map { it.latency }
            .also { result ->
                result.fold(
                    onSuccess = { Timber.i("setClientLatency() succeeded for clientId: $clientId, new latency: ${it}ms") },
                    onFailure = { Timber.e(it, "setClientLatency() failed for clientId: $clientId") }
                )
            }
    }

    override suspend fun setClientName(clientId: String, name: String): Result<String> {
        Timber.d("setClientName() called with clientId: $clientId, name: $name")
        return apiService.sendRequest<ClientSetNameParams, ClientSetNameResult>(
            method = "Client.SetName",
            params = ClientSetNameParams(clientId, name)
        ).map { it.name }
            .also { result ->
                result.fold(
                    onSuccess = { Timber.i("setClientName() succeeded for clientId: $clientId, new name: $it") },
                    onFailure = { Timber.e(it, "setClientName() failed for clientId: $clientId") }
                )
            }
    }

    override fun observeClientVolumeChanges(): Flow<Pair<String, Volume>> {
        Timber.d("observeClientVolumeChanges() called")
        return notifications
            .filter { it.method == "Client.OnVolumeChanged" }
            .map { notification ->
                val params = Json.decodeFromJsonElement<ClientOnVolumeChangedParams>(
                    notification.params
                )
                Timber.d("Client.OnVolumeChanged notification: clientId=${params.id}, volume=${params.volume.percent}%")
                params.id to params.volume
            }
    }

    override fun observeClientLatencyChanges(): Flow<Pair<String, Int>> {
        Timber.d("observeClientLatencyChanges() called")
        return notifications
            .filter { it.method == "Client.OnLatencyChanged" }
            .map { notification ->
                val params = Json.decodeFromJsonElement<ClientOnLatencyChangedParams>(
                    notification.params
                )
                Timber.d("Client.OnLatencyChanged notification: clientId=${params.id}, latency=${params.latency}ms")
                params.id to params.latency
            }
    }

    override fun observeClientNameChanges(): Flow<Pair<String, String>> {
        Timber.d("observeClientNameChanges() called")
        return notifications
            .filter { it.method == "Client.OnNameChanged" }
            .map { notification ->
                val params = Json.decodeFromJsonElement<ClientOnNameChangedParams>(
                    notification.params
                )
                Timber.d("Client.OnNameChanged notification: clientId=${params.id}, name=${params.name}")
                params.id to params.name
            }
    }

    override fun observeClientConnections(): Flow<Client> {
        Timber.d("observeClientConnections() called")
        return notifications
            .filter { it.method == "Client.OnConnect" }
            .map { notification ->
                val params = Json.decodeFromJsonElement<ClientOnConnectParams>(
                    notification.params
                )
                Timber.i("Client.OnConnect notification: clientId=${params.id}, name=${params.client.config.name}")
                params.client
            }
    }

    override fun observeClientDisconnections(): Flow<Client> {
        Timber.d("observeClientDisconnections() called")
        return notifications
            .filter { it.method == "Client.OnDisconnect" }
            .map { notification ->
                val params = Json.decodeFromJsonElement<ClientOnDisconnectParams>(
                    notification.params
                )
                Timber.i("Client.OnDisconnect notification: clientId=${params.id}, name=${params.client.config.name}")
                params.client
            }
    }

    // ==================== Group Methods ====================

    override suspend fun getGroupStatus(groupId: String): Result<Group> {
        Timber.d("getGroupStatus() called with groupId: $groupId")
        return apiService.sendRequest<GroupGetStatusParams, GroupGetStatusResult>(
            method = "Group.GetStatus",
            params = GroupGetStatusParams(groupId)
        ).map { it.group }
            .also { result ->
                result.fold(
                    onSuccess = { Timber.d("getGroupStatus() succeeded for groupId: $groupId, name: ${it.name}, ${it.clients.size} clients") },
                    onFailure = { Timber.e(it, "getGroupStatus() failed for groupId: $groupId") }
                )
            }
    }

    override suspend fun setGroupMute(groupId: String, mute: Boolean): Result<Boolean> {
        Timber.d("setGroupMute() called with groupId: $groupId, mute: $mute")
        return apiService.sendRequest<GroupSetMuteParams, GroupSetMuteResult>(
            method = "Group.SetMute",
            params = GroupSetMuteParams(groupId, mute)
        ).map { it.mute }
            .also { result ->
                result.fold(
                    onSuccess = { Timber.i("setGroupMute() succeeded for groupId: $groupId, mute: $it") },
                    onFailure = { Timber.e(it, "setGroupMute() failed for groupId: $groupId") }
                )
            }
    }

    override suspend fun setGroupStream(groupId: String, streamId: String): Result<String> {
        Timber.d("setGroupStream() called with groupId: $groupId, streamId: $streamId")
        return apiService.sendRequest<GroupSetStreamParams, GroupSetStreamResult>(
            method = "Group.SetStream",
            params = GroupSetStreamParams(groupId, streamId)
        ).map { it.streamId }
            .also { result ->
                result.fold(
                    onSuccess = { Timber.i("setGroupStream() succeeded for groupId: $groupId, streamId: $it") },
                    onFailure = { Timber.e(it, "setGroupStream() failed for groupId: $groupId") }
                )
            }
    }

    override suspend fun setGroupClients(groupId: String, clientIds: List<String>): Result<Server> {
        Timber.d("setGroupClients() called with groupId: $groupId, clientIds: $clientIds")
        return apiService.sendRequest<GroupSetClientsParams, GroupSetClientsResult>(
            method = "Group.SetClients",
            params = GroupSetClientsParams(clientIds, groupId)
        ).map { it.server }
            .also { result ->
                result.fold(
                    onSuccess = { Timber.i("setGroupClients() succeeded for groupId: $groupId, ${clientIds.size} clients") },
                    onFailure = { Timber.e(it, "setGroupClients() failed for groupId: $groupId") }
                )
            }
    }

    override suspend fun setGroupName(groupId: String, name: String): Result<String> {
        Timber.d("setGroupName() called with groupId: $groupId, name: $name")
        return apiService.sendRequest<GroupSetNameParams, GroupSetNameResult>(
            method = "Group.SetName",
            params = GroupSetNameParams(groupId, name)
        ).map { it.name }
            .also { result ->
                result.fold(
                    onSuccess = { Timber.i("setGroupName() succeeded for groupId: $groupId, new name: $it") },
                    onFailure = { Timber.e(it, "setGroupName() failed for groupId: $groupId") }
                )
            }
    }

    override fun observeGroupMuteChanges(): Flow<Pair<String, Boolean>> {
        Timber.d("observeGroupMuteChanges() called")
        return notifications
            .filter { it.method == "Group.OnMute" }
            .map { notification ->
                val params = Json.decodeFromJsonElement<GroupOnMuteParams>(
                    notification.params
                )
                Timber.d("Group.OnMute notification: groupId=${params.id}, mute=${params.mute}")
                params.id to params.mute
            }
    }

    override fun observeGroupStreamChanges(): Flow<Pair<String, String>> {
        Timber.d("observeGroupStreamChanges() called")
        return notifications
            .filter { it.method == "Group.OnStreamChanged" }
            .map { notification ->
                val params = Json.decodeFromJsonElement<GroupOnStreamChangedParams>(
                    notification.params
                )
                Timber.d("Group.OnStreamChanged notification: groupId=${params.id}, streamId=${params.streamId}")
                params.id to params.streamId
            }
    }

    override fun observeGroupNameChanges(): Flow<Pair<String, String>> {
        Timber.d("observeGroupNameChanges() called")
        return notifications
            .filter { it.method == "Group.OnNameChanged" }
            .map { notification ->
                val params = Json.decodeFromJsonElement<GroupOnNameChangedParams>(
                    notification.params
                )
                Timber.d("Group.OnNameChanged notification: groupId=${params.id}, name=${params.name}")
                params.id to params.name
            }
    }

    // ==================== Stream Methods ====================

    override suspend fun controlStream(
        streamId: String,
        command: String,
        params: Map<String, String>
    ): Result<String> {
        Timber.d("controlStream() called with streamId: $streamId, command: $command, params: $params")
        return apiService.sendRequest<StreamControlParams, StreamControlResult>(
            method = "Stream.Control",
            params = StreamControlParams(streamId, command, params)
        ).map { it.result }
            .also { result ->
                result.fold(
                    onSuccess = { Timber.i("controlStream() succeeded for streamId: $streamId, command: $command") },
                    onFailure = { Timber.e(it, "controlStream() failed for streamId: $streamId, command: $command") }
                )
            }
    }

    override suspend fun setStreamProperty(
        streamId: String,
        property: String,
        value: String
    ): Result<String> {
        Timber.d("setStreamProperty() called with streamId: $streamId, property: $property, value: $value")
        return apiService.sendRequest<StreamSetPropertyParams, StreamSetPropertyResult>(
            method = "Stream.SetProperty",
            params = StreamSetPropertyParams(streamId, property, value)
        ).map { it.result }
            .also { result ->
                result.fold(
                    onSuccess = { Timber.i("setStreamProperty() succeeded for streamId: $streamId, property: $property") },
                    onFailure = { Timber.e(it, "setStreamProperty() failed for streamId: $streamId, property: $property") }
                )
            }
    }

    override suspend fun addStream(streamUri: String): Result<String> {
        Timber.d("addStream() called with streamUri: $streamUri")
        return apiService.sendRequest<StreamAddStreamParams, StreamAddStreamResult>(
            method = "Stream.AddStream",
            params = StreamAddStreamParams(streamUri)
        ).map { it.streamId }
            .also { result ->
                result.fold(
                    onSuccess = { Timber.i("addStream() succeeded, streamUri: $streamUri, streamId: $it") },
                    onFailure = { Timber.e(it, "addStream() failed for streamUri: $streamUri") }
                )
            }
    }

    override suspend fun removeStream(streamId: String): Result<String> {
        Timber.d("removeStream() called with streamId: $streamId")
        return apiService.sendRequest<StreamRemoveStreamParams, StreamRemoveStreamResult>(
            method = "Stream.RemoveStream",
            params = StreamRemoveStreamParams(streamId)
        ).map { it.streamId }
            .also { result ->
                result.fold(
                    onSuccess = { Timber.i("removeStream() succeeded for streamId: $it") },
                    onFailure = { Timber.e(it, "removeStream() failed for streamId: $streamId") }
                )
            }
    }

    override fun observeStreamUpdates(): Flow<Stream> {
        Timber.d("observeStreamUpdates() called")
        return notifications
            .filter { it.method == "Stream.OnUpdate" }
            .map { notification ->
                val params = Json.decodeFromJsonElement<StreamOnUpdateParams>(
                    notification.params
                )
                Timber.d("Stream.OnUpdate notification: streamId=${params.id}, uri=${params.stream.uri?.raw}")
                params.stream
            }
    }

    override fun observeStreamProperties(): Flow<Pair<String, Map<String, String>>> {
        Timber.d("observeStreamProperties() called")
        return notifications
            .filter { it.method == "Stream.OnProperties" }
            .map { notification ->
                val params = Json.decodeFromJsonElement<StreamOnPropertiesParams>(
                    notification.params
                )
                Timber.d("Stream.OnProperties notification: streamId=${params.id}, properties: ${params.metadata.keys}")
                params.id to params.metadata
            }
    }

    // ==================== Connection Management ====================

    override suspend fun connect(host: String, port: Int) {
        Timber.d("connect() called with host: $host, port: $port")
        try {
            // Update connection settings (will disconnect if already connected to different server)
            apiService.updateConnection(host, port)
            // Connect to the server
            apiService.connectSocket()
            Timber.i("Repository connected successfully to $host:$port")
        } catch (e: Exception) {
            Timber.e(e, "Repository connection failed to $host:$port")
            throw e
        }
    }

    override suspend fun disconnect() {
        Timber.d("disconnect() called")
        apiService.disconnectSocket()
        Timber.i("Repository disconnected")
    }

    override fun isConnected(): Boolean {
        val connected = apiService.isConnected
        Timber.v("isConnected() called, returning: $connected")
        return connected
    }
}

// Extension function to map Result types
private fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> {
    return fold(
        onSuccess = { Result.success(transform(it)) },
        onFailure = { Result.failure(it) }
    )
}

