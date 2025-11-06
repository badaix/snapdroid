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

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Collections

/**
 * Created by johannes on 06.01.16.
 */
class ServerStatus : JsonSerialisable {
    val groups: ArrayList<Group> = ArrayList()
    val streams: ArrayList<Stream> = ArrayList()
    private var server: Server? = null

    constructor(json: JSONObject) {
        fromJson(json)
    }

    constructor()

    override fun fromJson(json: JSONObject?) {
        if (json == null) return
        try {
            clear()
            server = Server(json.getJSONObject("server"))
            val jStreams = json.getJSONArray("streams")
            for (i in 0..<jStreams.length()) streams.add(Stream(jStreams.getJSONObject(i)))
            val jGroups = json.getJSONArray("groups")
            for (i in 0..<jGroups.length()) groups.add(Group(jGroups.getJSONObject(i)))
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        sort()
    }

    override fun toJson(): JSONObject {
        val json = JSONObject()
        try {
            json.put("server", server!!.toJson())
            json.put("groups", this.jsonGroups)
            json.put("streams", this.jsonStreams)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return json
    }

    fun sort() {
        for (group in groups) group.sort()

        Collections.sort<Group?>(groups)
    }

    override fun toString(): String {
        return toJson().toString()
    }

    fun clear() {
        groups.clear()
        streams.clear()
    }

    fun removeClient(client: Client?): Boolean {
        if (client == null) return false

        for (group in groups) {
            if (group.getClient(client.id) != null) {
                group.removeClient(client.id)
                return true
            }
        }
        return false
    }

    fun updateClient(client: Client?): Boolean {
        if (client == null) return false

        for (group in groups) {
            if (group.getClient(client.id) != null) {
                group.updateClient(client)
                return true
            }
        }
        return false
    }

    fun updateGroup(group: Group?): Boolean {
        if (group == null) return false

        for (i in groups.indices) {
            val g = groups.get(i)
            if (g == null) continue

            if (group.id == g.id) {
                if (g == group) return false
                groups.set(i, group)
                return true
            }
        }
        return false
    }

    fun updateStream(stream: Stream?): Boolean {
        if (stream == null) return false

        for (i in streams.indices) {
            val s = streams.get(i)
            if (s == null) continue

            if (stream.id == s.id) {
                if (s == stream) return false
                streams.set(i, stream)
                return true
            }
        }
        streams.add(stream)
        return true
    }

    fun getClient(id: String?): Client? {
        for (group in groups) {
            val client = group.getClient(id)
            if (client != null) return client
        }
        return null
    }

    fun getStream(id: String?): Stream? {
        for (s in streams) if ((s != null) && (s.id == id)) return s
        return null
    }

    fun getGroup(id: String?): Group? {
        for (g in groups) if ((g != null) && (g.id == id)) return g
        return null
    }

    val jsonStreams: JSONArray
        get() {
            val jsonArray = JSONArray()
            for (stream in streams) jsonArray.put(stream!!.toJson())
            return jsonArray
        }

    val jsonGroups: JSONArray
        get() {
            val jsonArray = JSONArray()
            for (group in groups) jsonArray.put(group.toJson())
            return jsonArray
        }
}
