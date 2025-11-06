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
class StreamUri(json: JSONObject) : JsonSerialisable {
    var raw: String? = null
        private set
    var scheme: String? = null
    var host: String? = null
    var path: String? = null
    var fragment: String? = null
    var query: HashMap<String?, String?>? = null

    init {
        fromJson(json)
    }

    override fun fromJson(json: JSONObject?) {
        if (json == null) return
        try {
            if (json.has("raw")) raw = json.getString("raw")
            else if (json.has("uri")) raw = json.getString("uri")
            scheme = json.getString("scheme")
            host = json.getString("host")
            path = json.getString("path")
            fragment = json.getString("fragment")
            query = HashMap<String?, String?>()
            val jQuery = json.getJSONObject("query")
            for (i in 0..<jQuery.names().length()) query!!.put(
                jQuery.names().getString(i),
                jQuery.getString(jQuery.names().getString(i))
            )
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun toJson(): JSONObject {
        val json = JSONObject()
        try {
            json.put("raw", raw)
            json.put("scheme", scheme)
            json.put("host", host)
            json.put("path", path)
            json.put("fragment", fragment)
            val jQuery = JSONObject()
            for (entry in query!!.entries) jQuery.put(entry.key, entry.value)
            json.put("query", jQuery)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return json
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val stream = o as StreamUri

        if (raw != stream.raw) return false
        if (scheme != stream.scheme) return false
        if (host != stream.host) return false
        if (path != stream.path) return false
        if (fragment != stream.fragment) return false
        return query == stream.query
    }

    override fun hashCode(): Int {
        var result = if (raw != null) raw.hashCode() else 0
        result = 31 * result + (if (scheme != null) scheme.hashCode() else 0)
        result = 31 * result + (if (host != null) host.hashCode() else 0)
        result = 31 * result + (if (path != null) path.hashCode() else 0)
        result = 31 * result + (if (fragment != null) fragment.hashCode() else 0)
        result = 31 * result + (if (query != null) query.hashCode() else 0)
        return result
    }

    fun setUri(uri: String?) {
        this.raw = raw
    }

    val name: String?
        get() {
            if (query!!.containsKey("name")) return query!!.get("name")
            else return ""
        }

    override fun toString(): String {
        return toJson().toString()
    }
}
