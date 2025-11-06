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
open class Snapcast : JsonSerialisable {
    var name: String? = ""
    var version: String? = ""
    var protocolVersion: Int = 1

    constructor()

    constructor(json: JSONObject?) {
        fromJson(json)
    }

    override fun fromJson(json: JSONObject?) {
        if (json == null) return
        try {
            name = json.getString("name")
            version = json.getString("version")
            protocolVersion = json.getInt("protocolVersion")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun toJson(): JSONObject {
        val json = JSONObject()
        try {
            json.put("name", name)
            json.put("version", version)
            json.put("protocolVersion", protocolVersion)
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

        val that = o as Snapcast

        if (name != that.name) return false
        if (version != that.version) return false
        return (protocolVersion == that.protocolVersion)
    }

    override fun hashCode(): Int {
        var result = if (name != null) name.hashCode() else 0
        result = 31 * result + (if (version != null) version.hashCode() else 0)
        result = 31 * result + protocolVersion
        return result
    }
}

