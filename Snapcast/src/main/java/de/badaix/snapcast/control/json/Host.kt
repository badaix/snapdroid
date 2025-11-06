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
class Host : JsonSerialisable {
    var name: String? = ""
    var mac: String? = ""
    var os: String? = ""
    var arch: String? = ""
    var ip: String? = ""

    constructor(json: JSONObject) {
        fromJson(json)
    }

    constructor()

    override fun fromJson(json: JSONObject?) {
        if (json == null) return
        try {
            name = json.getString("name")
            mac = json.getString("mac")
            os = json.getString("os")
            arch = json.getString("arch")
            ip = json.getString("ip")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun toJson(): JSONObject {
        val json = JSONObject()
        try {
            json.put("name", name)
            json.put("mac", mac)
            json.put("os", os)
            json.put("arch", arch)
            json.put("ip", ip)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return json
    }

    override fun toString(): String {
        return toJson().toString()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as Host

        if (name != that.name) return false
        if (mac != that.mac) return false
        if (os != that.os) return false
        if (arch != that.arch) return false
        return ip == that.ip
    }

    override fun hashCode(): Int {
        var result = if (name != null) name.hashCode() else 0
        result = 31 * result + (if (mac != null) mac.hashCode() else 0)
        result = 31 * result + (if (os != null) os.hashCode() else 0)
        result = 31 * result + (if (arch != null) arch.hashCode() else 0)
        result = 31 * result + (if (ip != null) ip.hashCode() else 0)
        return result
    }
}

