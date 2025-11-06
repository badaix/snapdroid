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
package de.badaix.snapcast.control.json

import android.text.TextUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Collections

/**
 * Created by johannes on 03.12.16.
 */
class Group : JsonSerialisable, Comparable<Group?> {
    var name: String? = ""
    var id: String? = ""
    var streamId: String? = ""
    var isMuted: Boolean = false
    val clients: ArrayList<Client> = ArrayList()

    constructor(json: JSONObject) {
        fromJson(json)
    }

    constructor()

    override fun fromJson(json: JSONObject?) {
        if (json == null) return
        try {
            clients.clear()
            name = json.getString("name")
            id = json.getString("id")
            streamId = json.getString("stream_id")
            this.isMuted = json.optBoolean("muted", false)
            val jClients = json.optJSONArray("clients")
            if (jClients != null) {
                for (i in 0..<jClients.length()) clients.add(Client(jClients.getJSONObject(i)))
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        sort()
    }

    override fun toJson(): JSONObject {
        val json = JSONObject()
        try {
            json.put("name", name)
            json.put("id", id)
            json.put("stream_id", streamId)
            json.put("muted", this.isMuted)
            json.put("clients", this.jsonClients)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return json
    }

    fun sort() {
        Collections.sort<Client?>(clients)
    }

    fun removeClient(id: String?): Boolean {
        if (TextUtils.isEmpty(id)) return false

        for (i in clients.indices) {
            if (clients.get(i)!!.id == id) {
                clients.removeAt(i)
                return true
            }
        }
        return false
    }


    fun updateClient(client: Client?): Boolean {
        if (client == null) return false

        for (i in clients.indices) {
            val c = clients[i]

            if (client.id == c.id) {
                if (client == c) return true
                clients[i] = client
                return true
            }
        }
        return false
    }


    fun getClient(id: String?): Client? {
        if (TextUtils.isEmpty(id)) return null

        for (c in clients) {
            if (c == null) continue

            if (id == c.id) return c
        }
        return null
    }

    val jsonClients: JSONArray
        get() {
            val jsonArray = JSONArray()
            for (client in clients) jsonArray.put(client!!.toJson())
            return jsonArray
        }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val group = o as Group

        if (name != group.name) return false
        if (id != group.id) return false
        if (this.isMuted != group.isMuted) return false
        if (streamId != group.streamId) return false
        return clients == group.clients
    }

    override fun hashCode(): Int {
        var result = if (name != null) name.hashCode() else 0
        result = 31 * result + (if (id != null) id.hashCode() else 0)
        result = 31 * result + (if (streamId != null) streamId.hashCode() else 0)
        result = 31 * result + (if (this.isMuted) 1 else 0)
        result = 31 * result + (if (clients != null) clients.hashCode() else 0)
        return result
    }

    override fun compareTo(other: Group?): Int {
        if (other == null) return 1
        if (this.clients.isEmpty()) return -1
        if (other.clients.isEmpty()) return 1
        return this.clients[0].compareTo(other.clients[0])
    }
}
