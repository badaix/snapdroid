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

import org.json.JSONException
import org.json.JSONObject

/**
 * Created by johannes on 19.02.17.
 */
internal class RPCRequest {
    constructor(json: JSONObject) {
        fromJson(json)
    }

    constructor(json: String) : this(JSONObject(json))

    constructor(method: String?, id: Long, params: JSONObject?) {
        this.method = method
        this.id = id
        this.params = params
    }

    override fun toString(): String {
        return toJson().toString()
    }

    fun toJson(): JSONObject? {
        val request = JSONObject()
        try {
            request.put("jsonrpc", "2.0")
            request.put("method", method)
            request.put("id", id)
            if (params != null) request.put("params", params)
            return request
        } catch (e: JSONException) {
            e.printStackTrace()
            return null
        }
    }

    @Throws(JSONException::class)
    fun fromJson(json: JSONObject) {
        id = json.getLong("id")
        method = json.getString("method")
        if (json.has("params")) params = json.getJSONObject("params")
        else params = null
    }

    var method: String? = null
    var id: Long = 0
    var params: JSONObject? = null
}


internal class RPCNotification {
    constructor(json: JSONObject) {
        fromJson(json)
    }

    constructor(json: String) : this(JSONObject(json))

    constructor(method: String?, id: Long, params: JSONObject?) {
        this.method = method
        this.params = params
    }

    override fun toString(): String {
        return toJson().toString()
    }

    fun toJson(): JSONObject? {
        val request = JSONObject()
        try {
            request.put("jsonrpc", "2.0")
            request.put("method", method)
            if (params != null) request.put("params", params)
            return request
        } catch (e: JSONException) {
            e.printStackTrace()
            return null
        }
    }

    @Throws(JSONException::class)
    fun fromJson(json: JSONObject) {
        method = json.getString("method")
        if (json.has("params")) params = json.getJSONObject("params")
        else params = null
    }

    var method: String? = null
    var params: JSONObject? = null
}


internal class RPCResponse(json: JSONObject) {
    constructor(json: String) : this(JSONObject(json))

    override fun toString(): String {
        return toJson().toString()
    }

    fun toJson(): JSONObject? {
        val response = JSONObject()
        try {
            response.put("jsonrpc", "2.0")
            if (error != null) response.put("error", error)
            else if (result != null) response.put("result", result)
            else throw JSONException("error and result are null")

            response.put("id", id)
            return response
        } catch (e: JSONException) {
            e.printStackTrace()
            return null
        }
    }

    @Throws(JSONException::class)
    fun fromJson(json: JSONObject) {
        id = json.getLong("id")
        if (json.has("error")) {
            error = json.getJSONObject("error")
            result = null
        } else if (json.has("result")) {
            result = json.getJSONObject("result")
            error = null
        } else throw JSONException("error and result are null")
    }

    var id: Long = 0
    var result: JSONObject? = null
    var error: JSONObject? = null

    init {
        fromJson(json)
    }
}

