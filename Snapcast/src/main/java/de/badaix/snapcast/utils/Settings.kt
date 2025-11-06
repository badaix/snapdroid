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
package de.badaix.snapcast.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.preference.PreferenceManager
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Created by johannes on 21.02.16.
 */
class Settings @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val resources: Resources
        get() = context.resources

    val prefs: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(context)

    fun put(key: String?, value: String?): Settings {
        prefs.edit {
            putString(key, value)
        }
        return this
    }

    fun put(key: String?, value: Boolean): Settings {
        this.prefs.edit {
            putBoolean(key, value)
        }
        return this
    }

    fun put(key: String?, value: Float): Settings {
        this.prefs.edit {
            putFloat(key, value)
        }
        return this
    }

    fun put(key: String?, value: Int): Settings {
        this.prefs.edit {
            putInt(key, value)
        }
        return this
    }

    fun put(key: String?, value: Long): Settings {
        this.prefs.edit {
            putLong(key, value)
        }
        return this
    }

    fun getString(key: String?, defaultValue: String?): String? {
        return this.prefs.getString(key, defaultValue)
    }

    fun getBoolean(key: String?, defaultValue: Boolean): Boolean {
        return this.prefs.getBoolean(key, defaultValue)
    }

    fun getFloat(key: String?, defaultValue: Float): Float {
        return this.prefs.getFloat(key, defaultValue)
    }

    fun getInt(key: String?, defaultValue: Int): Int {
        return this.prefs.getInt(key, defaultValue)
    }

    fun getLong(key: String?, defaultValue: Long): Long {
        return this.prefs.getLong(key, defaultValue)
    }

    val host: String?
        get() = getString("host", "")

    val streamPort: Int
        get() = getInt("streamPort", 1704)

    val controlPort: Int
        get() = getInt("controlPort", this.streamPort + 1)

    var isAutostart: Boolean
        get() = getBoolean("autoStart", false)
        set(autoStart) {
            put("autoStart", autoStart)
        }

    val audioEngine: String
        get() = "Auto" // getString("audioEngine", "Auto");

    fun doResample(): Boolean {
        return true // getBoolean("resample", true);
    }

    fun setAudioEngine(engine: String?, resample: Boolean) {
        put("audioEngine", engine)
        put("resample", resample)
    }

    fun setHost(host: String?, streamPort: Int, controlPort: Int) {
        put("host", host)
        put("streamPort", streamPort)
        put("controlPort", controlPort)
    }
}
