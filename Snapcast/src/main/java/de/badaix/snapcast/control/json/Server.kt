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

import org.json.JSONException
import org.json.JSONObject

/**
 * Created by johannes on 02.03.16.
 */
class Server(json: JSONObject) : JsonSerialisable {
    var host: Host? = null
        private set
    private var snapserver: Snapserver? = null

    init {
        fromJson(json)
    }

    override fun fromJson(json: JSONObject?) {
        if (json == null) return
        try {
            if (json.has("host") && json.get("host") !is String) host =
                Host(json.getJSONObject("host"))
            else {
                host = Host()
                host!!.name = json.getString("host")
            }

            if (json.has("snapserver")) snapserver = Snapserver(json.getJSONObject("snapserver"))
            else {
                snapserver = Snapserver()
                snapserver!!.version = json.getString("version")
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun toJson(): JSONObject {
        val json = JSONObject()
        try {
            json.put("host", host!!.toJson())
            json.put("snapserver", snapserver!!.toJson())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return json
    }

    fun getSnapserver(): Snapcast? {
        return snapserver
    }

    override fun toString(): String {
        return toJson().toString()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val server = o as Server

        if (host != server.host) return false
        return snapserver == server.snapserver
    }

    override fun hashCode(): Int {
        var result = if (host != null) host.hashCode() else 0
        result = 31 * result + (if (snapserver != null) snapserver.hashCode() else 0)
        return result
    }
}

