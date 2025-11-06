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
 * Created by johannes on 06.01.16.
 */
class Client(json: JSONObject) : JsonSerialisable, Comparable<Client?> {
    var host: Host? = null
        private set
    private var snapclient: Snapclient? = null
    var config: ClientConfig? = null
        private set
    private var lastSeen: Time_t? = null
    var isConnected: Boolean = false
    var id: String? = null
        private set
    var isDeleted: Boolean = false

    init {
        fromJson(json)
    }

    override fun fromJson(json: JSONObject?) {
        if (json == null) return
        try {
            host = Host(json.getJSONObject("host"))
            snapclient = Snapclient(json.getJSONObject("snapclient"))
            config = ClientConfig(json.getJSONObject("config"))

            lastSeen = Time_t(json.getJSONObject("lastSeen"))
            this.isConnected = json.getBoolean("connected")
            this.id = json.optString("id", host!!.mac)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun toJson(): JSONObject {
        val json = JSONObject()
        try {
            json.put("host", host!!.toJson())
            json.put("snapclient", snapclient!!.toJson())
            json.put("config", config!!.toJson())
            json.put("lastSeen", lastSeen!!.toJson())
            json.put("connected", this.isConnected)
            json.put("id", this.id)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return json
    }

    fun getSnapclient(): Snapcast? {
        return snapclient
    }

    fun getLastSeen(): Time_t {
        return lastSeen!!
    }

    fun setVolume(volume: Volume?) {
        this.config!!.volume = volume
    }

    fun setName(name: String?) {
        this.config!!.name = name
    }

    val visibleName: String?
        get() {
            if ((config!!.name != null) && !config!!.name!!
                    .isEmpty()
            ) return config!!.name
            var name = host!!.name
            if (config!!.instance > 1) name += " #" + config!!.instance
            return name
        }

    override fun toString(): String {
        return toJson().toString()
    }


    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as Client

        if (host != that.host) return false
        if (snapclient != that.snapclient) return false
        if (config != that.config) return false
        if (this.isConnected != that.isConnected) return false
        if (this.id != that.id) return false
        return (this.isDeleted == that.isDeleted)
    }

    override fun hashCode(): Int {
        var result = if (host != null) host.hashCode() else 0
        result = 31 * result + (if (snapclient != null) snapclient.hashCode() else 0)
        result = 31 * result + (if (config != null) config!!.hashCode() else 0)
        result = 31 * result + (if (this.isConnected) 1 else 0)
        result = 31 * result + (if (this.id != null) id.hashCode() else 0)
        result = 31 * result + (if (this.isDeleted) 1 else 0)
        return result
    }

    override fun compareTo(another: Client?): Int {
        if (another == null) return 1
        return this.visibleName!!.compareTo(another.visibleName!!, ignoreCase = true)
    }
}

