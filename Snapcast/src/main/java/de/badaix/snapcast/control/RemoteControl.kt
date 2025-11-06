/*
 *     This file is part of snapcast
 *     Copyright (C) 2014-2018  Johannes Pohl
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.badaix.snapcast.control

import android.util.Log
import de.badaix.snapcast.control.TcpClient.TcpClientListener
import de.badaix.snapcast.control.json.Client
import de.badaix.snapcast.control.json.Group
import de.badaix.snapcast.control.json.ServerStatus
import de.badaix.snapcast.control.json.Stream
import de.badaix.snapcast.control.json.Volume
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Created by johannes on 13.01.16.
 */
class RemoteControl(private val listener: RemoteControlListener?) : TcpClientListener {
    enum class RPCEvent {
        response,
        notification
    }

    private var tcpClient: TcpClient? = null
    private var msgId: Long = 0
    var host: String? = null
        private set
    var port: Int = 0
        private set
    private val pendingRequests: HashMap<Long?, String?>

    init {
        pendingRequests = HashMap<Long?, String?>()
    }

    @Synchronized
    fun connect(host: String, port: Int) {
        if ((tcpClient != null) && tcpClient!!.isConnected) {
            if (this.host == host && (this.port == port)) return
            else disconnect()
        }
        this.host = host
        this.port = port

        tcpClient = TcpClient(this)
        tcpClient!!.start(host, port)
    }

    fun disconnect() {
        if ((tcpClient != null) && (tcpClient!!.isConnected)) tcpClient!!.stop()
        tcpClient = null
        pendingRequests.clear()
    }

    val isConnected: Boolean
        get() = ((tcpClient != null) && tcpClient!!.isConnected)

    override fun onMessageReceived(tcpClient: TcpClient?, message: String?) {
        if (message == null) return
        try {
            if (message.trim { it <= ' ' }.startsWith("[")) {
                val jsonArray = JSONArray(message)
                for (i in 0..<jsonArray.length()) {
                    val json = jsonArray.getJSONObject(i)
                    processJson(json)
                }
            } else {
                val json = JSONObject(message)
                processJson(json)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun processJson(json: JSONObject) {
//        Log.d(TAG, "Msg received: " + message);
        try {
            if (json.has("id")) {
                /** Response */
                val response = RPCResponse(json)
                //                Log.d(TAG, "ID: " + json.getString("id"));
                var request: RPCRequest? = null
                synchronized(pendingRequests) {
                    if (pendingRequests.containsKey(response.id)) {
                        request = RPCRequest(JSONObject(pendingRequests.get(response.id)))
                        Log.d(TAG, "Response to: " + request.method)
                        pendingRequests.remove(response.id)
                    }
                }

                if (listener == null) return

                if (json.has("error")) {
                    val error = json.getJSONObject("error")
                    Log.e(
                        TAG,
                        "error " + error.getInt("code") + ": " + error.getString("message") + "; data: " + error.getString(
                            "data"
                        )
                    )
                }

                if (request == null) {
                    Log.e(TAG, "request for id " + response.id + " not found")
                    return
                }

                val rpcEvent = RPCEvent.response
                /** Response to a "Object.GetStatus" message */
                if (request.method == "Client.GetStatus") {
                    listener.onUpdate(Client(response.result!!.getJSONObject("client")))
                } else if (request.method == "Client.SetVolume") {
                } else if (request.method == "Client.SetLatency") {
                } else if (request.method == "Client.SetName") {
                } else if (request.method == "Group.GetStatus") {
                    listener.onUpdate(Group(response.result!!.getJSONObject("group")))
                } else if (request.method == "Group.SetMute") {
//                    listener.onMute(rpcEvent, request.params!!.getString("id"), response.result!!.getBoolean("mute"));
                } else if (request.method == "Group.SetStream") {
                } else if (request.method == "Group.SetClients") {
                    listener.onUpdate(ServerStatus(response.result!!.getJSONObject("server")))
                } else if (request.method == "Server.GetStatus") {
                    listener.onUpdate(ServerStatus(response.result!!.getJSONObject("server")))
                } else if (request.method == "Server.DeleteClient") {
                    listener.onUpdate(ServerStatus(response.result!!.getJSONObject("server")))
                }
            } else {
                /** Notification */
                if (listener == null) return
                val rpcEvent = RPCEvent.notification
                val notification = RPCNotification(json)

                if (notification.method == "Client.OnConnect") {
                    listener.onConnect(Client(notification.params!!.getJSONObject("client")))
                } else if (notification.method == "Client.OnDisconnect") {
                    listener.onDisconnect(notification.params!!.getString("id"))
                } else if (notification.method == "Client.OnVolumeChanged") {
                    listener.onVolumeChanged(
                        rpcEvent,
                        notification.params!!.getString("id"),
                        Volume(notification.params!!.getJSONObject("volume"))
                    )
                } else if (notification.method == "Client.OnLatencyChanged") {
                    listener.onLatencyChanged(
                        rpcEvent,
                        notification.params!!.getString("id"),
                        notification.params!!.getInt("latency").toLong()
                    )
                } else if (notification.method == "Client.OnNameChanged") {
                    listener.onNameChanged(
                        rpcEvent,
                        notification.params!!.getString("id"),
                        notification.params!!.getString("name")
                    )
                } else if (notification.method == "Group.OnMute") {
                    listener.onMute(
                        rpcEvent,
                        notification.params!!.getString("id"),
                        notification.params!!.getBoolean("mute")
                    )
                } else if (notification.method == "Group.OnStreamChanged") {
                    listener.onStreamChanged(
                        rpcEvent,
                        notification.params!!.getString("id"),
                        notification.params!!.getString("stream_id")
                    )
                } else if (notification.method == "Stream.OnUpdate") {
                    listener.onUpdate(
                        notification.params!!.getString("id"),
                        Stream(notification.params!!.getJSONObject("stream"))
                    )
                } else if (notification.method == "Group.OnUpdate") {
                    listener.onUpdate(Group(notification.params!!.getJSONObject("group")))
                } else if (notification.method == "Server.OnUpdate") {
                    listener.onUpdate(ServerStatus(notification.params!!.getJSONObject("server")))
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun onConnecting(tcpClient: TcpClient?) {
        Log.d(TAG, "onConnecting")
        if (listener != null) listener.onConnecting(this)
    }

    override fun onConnected(tcpClient: TcpClient?) {
        Log.d(TAG, "onConnected")
        if (listener != null) listener.onConnected(this)
    }

    override fun onDisconnected(tcpClient: TcpClient?, e: Exception?) {
        Log.d(TAG, "onDisconnected")
        if (listener != null) listener.onDisconnected(this, e)
    }

    private fun jsonRequest(method: String?, params: JSONObject?): RPCRequest {
        val request = RPCRequest(method, msgId, params)
        synchronized(pendingRequests) {
            pendingRequests.put(msgId, request.toString())
        }
        msgId++
        return request
    }

    val serverStatus: Unit
        get() {
            val request = jsonRequest("Server.GetStatus", null)
            if (this.isConnected) tcpClient!!.sendMessage(request.toString())
        }

    fun setName(client: Client, name: String?) {
        try {
            val params = JSONObject()
            params!!.put("id", client.id)
            params!!.put("name", name)
            val request = jsonRequest("Client.SetName", params)
            if (this.isConnected) tcpClient!!.sendMessage(request.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun setLatency(client: Client, latency: Int) {
        try {
            val params = JSONObject()
            params!!.put("id", client.id)
            params!!.put("latency", latency)
            val request = jsonRequest("Client.SetLatency", params)
            if (this.isConnected) tcpClient!!.sendMessage(request.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun setStream(group: Group, id: String?) {
        setStream(group.id, id)
    }

    fun setClients(groupId: String?, clientIds: ArrayList<String?>) {
        try {
            val clients = JSONArray()
            for (clientId in clientIds) clients.put(clientId)
            val params = JSONObject()
            params!!.put("id", groupId)
            params!!.put("clients", clients)
            val request = jsonRequest("Group.SetClients", params)
            if (this.isConnected) tcpClient!!.sendMessage(request.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun setStream(groupId: String?, streamId: String?) {
        try {
            val params = JSONObject()
            params!!.put("id", groupId)
            params!!.put("stream_id", streamId)
            val request = jsonRequest("Group.SetStream", params)
            if (this.isConnected) tcpClient!!.sendMessage(request.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun setGroupMuted(group: Group, muted: Boolean) {
        try {
            val params = JSONObject()
            params!!.put("id", group.id)
            params!!.put("mute", muted)
            val request = jsonRequest("Group.SetMute", params)
            if (this.isConnected) tcpClient!!.sendMessage(request.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun setGroupVolume(group: Group) {
        try {
            val batch = JSONArray()
            for (client in group.clients) {
                val volume = client.config!!.volume!!
                val volumeRequest = getVolumeRequest(client, volume.percent, volume.isMuted)
                batch.put(volumeRequest.toJson())
            }
            if (this.isConnected) tcpClient!!.sendMessage(batch.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    @Throws(JSONException::class)
    private fun getVolumeRequest(client: Client, percent: Int, mute: Boolean): RPCRequest {
        val volume = Volume(percent, mute)
        val params = JSONObject()
        params!!.put("id", client.id)
        params!!.put("volume", volume.toJson())
        return jsonRequest("Client.SetVolume", params)
    }

    fun setVolume(client: Client, percent: Int, mute: Boolean) {
        try {
            if (this.isConnected) tcpClient!!.sendMessage(
                getVolumeRequest(
                    client,
                    percent,
                    mute
                ).toString()
            )
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun delete(client: Client) {
        try {
            val params = JSONObject()
            params!!.put("id", client.id)
            val request = jsonRequest("Server.DeleteClient", params)
            if (this.isConnected) tcpClient!!.sendMessage(request.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    enum class ClientEvent(text: String) {
        connected("Client.OnConnect"),
        disconnected("Client.OnDisconnect"),
        updated("Client.OnUpdate");

        //deleted("Client.OnDelete");
        val text: String?

        init {
            this.text = text
        }

        companion object {
            fun fromString(text: String?): ClientEvent {
                if (text != null) {
                    for (b in entries) {
                        if (text.equals(b.text, ignoreCase = true)) {
                            return b
                        }
                    }
                }
                throw IllegalArgumentException("No ClientEvent with text " + text + " found")
            }
        }
    }


    interface ClientListener {
        fun onConnect(client: Client?)

        fun onDisconnect(clientId: String?)

        fun onUpdate(client: Client?)

        fun onVolumeChanged(event: RPCEvent?, clientId: String?, volume: Volume?)

        fun onLatencyChanged(event: RPCEvent?, clientId: String?, latency: Long)

        fun onNameChanged(event: RPCEvent?, clientId: String?, name: String?)
    }

    interface GroupListener {
        fun onUpdate(group: Group?)

        fun onMute(event: RPCEvent?, groupId: String?, mute: Boolean)

        fun onStreamChanged(event: RPCEvent?, groupId: String?, streamId: String?)
    }

    interface StreamListener {
        fun onUpdate(streamId: String?, stream: Stream?)
    }

    interface ServerListener {
        fun onUpdate(server: ServerStatus?)
    }


    interface RemoteControlListener : ServerListener, StreamListener, GroupListener,
        ClientListener {
        fun onConnected(remoteControl: RemoteControl?)

        fun onConnecting(remoteControl: RemoteControl?)

        fun onDisconnected(remoteControl: RemoteControl?, e: Exception?)

        fun onBatchStart()

        fun onBatchEnd() /*
        void onClientEvent(RemoteControl remoteControl, RPCEvent rpcEvent, Client client, ClientEvent event);

        void onServerUpdate(RemoteControl remoteControl, RPCEvent rpcEvent, ServerStatus serverStatus);

        void onStreamUpdate(RemoteControl remoteControl, RPCEvent rpcEvent, Stream stream);

        void onGroupUpdate(RemoteControl remoteControl, RPCEvent rpcEvent, Group group);
*/
    }

    companion object {
        private const val TAG = "RC"
    }
}
