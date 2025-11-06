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
class Time_t : JsonSerialisable {
    var sec: Long = 0
    var usec: Long = 0

    constructor()

    constructor(json: JSONObject) {
        fromJson(json)
    }

    constructor(sec: Long, usec: Long) {
        this.sec = sec
        this.usec = usec
    }

    override fun fromJson(json: JSONObject?) {
        if (json == null) return
        try {
            sec = json.getLong("sec")
            usec = json.getLong("usec")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun toJson(): JSONObject {
        val json = JSONObject()
        try {
            json.put("sec", sec)
            json.put("usec", usec)
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

        val time_t = o as Time_t

        if (sec != time_t.sec) return false
        return usec == time_t.usec
    }

    override fun hashCode(): Int {
        var result = sec.toInt()
        result = (31 * result + usec).toInt()
        return result
    }
}
