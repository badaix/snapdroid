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
package de.badaix.snapcast

import android.os.Bundle
import android.preference.EditTextPreference
import android.preference.Preference
import android.preference.Preference.OnPreferenceChangeListener
import android.preference.PreferenceFragment
import android.text.TextUtils
import android.text.format.DateUtils
import de.badaix.snapcast.control.json.Client
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.min

/**
 * Created by johannes on 11.01.16.
 */
class ClientSettingsFragment : PreferenceFragment() {
    var client: Client? = null
        private set
    var originalClientInfo: Client? = null
        private set
    private var prefName: EditTextPreference? = null
    private var prefLatency: EditTextPreference? = null
    private var prefMac: Preference? = null
    private var prefId: Preference? = null
    private var prefIp: Preference? = null
    private var prefHost: Preference? = null
    private var prefOS: Preference? = null
    private var prefVersion: Preference? = null
    private var prefLastSeen: Preference? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = arguments
        try {
            client = Client(JSONObject(bundle.getString("client")))
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        this.originalClientInfo = Client(client!!.toJson())

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.client_preferences)
        prefName = findPreference("pref_client_name") as EditTextPreference
        prefName!!.onPreferenceChangeListener =
            OnPreferenceChangeListener { preference, newValue ->
                prefName!!.summary = newValue as String?
                client!!.setName(newValue)
                true
            }

        prefMac = findPreference("pref_client_mac")
        prefId = findPreference("pref_client_id")
        prefIp = findPreference("pref_client_ip")
        prefHost = findPreference("pref_client_host")
        prefOS = findPreference("pref_client_os")
        prefVersion = findPreference("pref_client_version")
        prefLastSeen = findPreference("pref_client_last_seen")
        prefLatency = findPreference("pref_client_latency") as EditTextPreference
        prefLatency!!.onPreferenceChangeListener =
            OnPreferenceChangeListener { preference, newValue ->
                var latency = newValue as String?
                if (TextUtils.isEmpty(latency)) latency = "0"
                prefLatency!!.summary = latency + "ms"
                client!!.config!!.latency = latency!!.toInt()
                true
            }
        update()
    }

    fun update() {
        if (client == null) return
        prefName!!.summary = client!!.config!!.name
        prefName!!.setText(client!!.config!!.name)
        prefMac!!.summary = client!!.host!!.mac
        prefId!!.summary = client!!.id
        prefIp!!.summary = client!!.host!!.ip
        prefHost!!.summary = client!!.host!!.name
        prefOS!!.summary = client!!.host!!.os + "@" + client!!.host!!.arch
        prefVersion!!.summary = client!!.getSnapclient()!!.version
        var lastSeen = getText(R.string.online).toString()
        if (!client!!.isConnected) {
            val lastSeenTs = min(client!!.getLastSeen().sec * 1000, System.currentTimeMillis())
            lastSeen = DateUtils.getRelativeTimeSpanString(
                lastSeenTs,
                System.currentTimeMillis(),
                DateUtils.SECOND_IN_MILLIS
            ).toString()
        }
        prefLastSeen!!.summary = lastSeen
        prefLatency!!.summary = client!!.config!!.latency.toString() + "ms"
        prefLatency!!.setText(client!!.config!!.latency.toString() + "")
    }
}
