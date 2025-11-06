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
class Stream(json: JSONObject) : JsonSerialisable {
    var uri: StreamUri? = null
    var id: String? = null
    var status: Status? = null

    init {
        fromJson(json)
    }

    override fun fromJson(json: JSONObject?) {
        if (json == null) return
        try {
            if (json.has("uri") && (json.get("uri") is JSONObject)) {
                uri = StreamUri(json.getJSONObject("uri"))
                id = json.getString("id")
                status = Status.Companion.fromString(json.getString("status"))
            } else {
                uri = StreamUri(json)
                id = json.getString("id")
                status = Status.unknown
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun toJson(): JSONObject {
        val json = JSONObject()
        try {
            json.put("uri", uri!!.toJson())
            json.put("id", id)
            json.put("status", status)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return json
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val stream = o as Stream

        if (uri != stream.uri) return false
        if (id != stream.id) return false
        return status == stream.status
    }

    override fun hashCode(): Int {
        var result = if (uri != null) uri.hashCode() else 0
        result = 31 * result + (if (id != null) id.hashCode() else 0)
        result = 31 * result + (if (status != null) status.hashCode() else 0)
        return result
    }

    val name: String?
        get() = uri!!.name

    override fun toString(): String {
        return toJson().toString()
    }

    enum class Status(status: String) {
        unknown("unknown"),
        idle("idle"),
        playing("playing"),
        disabled("disabled");

        private val status: String?

        init {
            this.status = status
        }

        companion object {
            fun fromString(status: String?): Status? {
                if (status != null) {
                    for (s in entries) {
                        if (status.equals(s.status, ignoreCase = true)) {
                            return s
                        }
                    }
                }
                return null
            }
        }
    }
}
