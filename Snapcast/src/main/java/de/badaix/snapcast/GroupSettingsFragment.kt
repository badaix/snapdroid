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
import android.preference.CheckBoxPreference
import android.preference.ListPreference
import android.preference.Preference
import android.preference.Preference.OnPreferenceChangeListener
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import de.badaix.snapcast.control.json.Group
import de.badaix.snapcast.control.json.ServerStatus
import org.json.JSONException
import org.json.JSONObject
import java.util.Collections

/**
 * Created by johannes on 06.12.16.
 */
class GroupSettingsFragment : PreferenceFragment() {
    //private static final String TAG = "GroupSettingsFragment";
    private var prefStreams: ListPreference? = null
    var group: Group? = null
        private set
    private var serverStatus: ServerStatus? = null
    private var prefCatClients: PreferenceCategory? = null

    private var oldClients = ""
    private var oldStream = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.group_preferences)
        val screen = this.getPreferenceScreen()

        prefStreams = findPreference("pref_stream") as ListPreference

        val bundle = getArguments()
        try {
            group = Group(JSONObject(bundle.getString("group")))
            serverStatus = ServerStatus(JSONObject(bundle.getString("serverStatus")))
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val streams = serverStatus!!.streams
        val streamNames = arrayOfNulls<CharSequence>(streams.size)
        val streamIds = arrayOfNulls<CharSequence>(streams.size)
        for (i in streams.indices) {
            streamNames[i] = streams.get(i)!!.name
            streamIds[i] = streams.get(i)!!.id
        }

        prefStreams!!.setEntries(streamNames)
        prefStreams!!.setEntryValues(streamIds)

        for (i in streams.indices) {
            if (streamIds[i] == group!!.streamId) {
                prefStreams!!.setTitle(streamNames[i])
                prefStreams!!.setValueIndex(i)
                oldStream = prefStreams!!.getValue()
                break
            }
        }

        prefStreams!!.setOnPreferenceChangeListener(object : OnPreferenceChangeListener {
            override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
                for (i in streams.indices) {
                    if (streamIds[i] == newValue) {
                        prefStreams!!.setTitle(streamNames[i])
                        //                        client.config!!.setStream(streamIds[i].toString());
                        prefStreams!!.setValueIndex(i)
                        break
                    }
                }

                return false
            }
        })


        prefCatClients = findPreference("pref_cat_clients") as PreferenceCategory?
        val allClients = java.util.ArrayList<CheckBoxPreference?>()
        for (g in serverStatus!!.groups) {
            for (client in g.clients) {
                val checkBoxPref = CheckBoxPreference(screen.getContext())
                checkBoxPref.setKey(client.id)
                checkBoxPref.setTitle(client.visibleName)
                checkBoxPref.setChecked(g.id == group!!.id)
                checkBoxPref.setPersistent(false)
                allClients.add(checkBoxPref)
            }
        }
        Collections.sort<CheckBoxPreference?>(allClients, object : Comparator<CheckBoxPreference?> {
            override fun compare(lhs: CheckBoxPreference?, rhs: CheckBoxPreference?): Int {
                if (lhs == null || rhs == null) return 0
                return lhs.getTitle().toString()
                    .compareTo(rhs.getTitle().toString(), ignoreCase = true)
            }
        })
        for (pref in allClients) prefCatClients!!.addPreference(pref)

        oldClients = this.clients.toString()
    }

    val clients: ArrayList<String?>
        get() {
            val clients =
                java.util.ArrayList<String?>()
            for (i in 0..<prefCatClients!!.getPreferenceCount()) {
                val checkBoxPref =
                    prefCatClients!!.getPreference(i) as CheckBoxPreference
                if (checkBoxPref.isChecked()) clients.add(checkBoxPref.getKey())
            }
            return clients
        }


    fun didStreamChange(): Boolean {
        return oldStream != prefStreams!!.getValue()
    }

    fun didClientsChange(): Boolean {
        return !oldClients.equals(this.clients.toString(), ignoreCase = true)
    }

    val stream: String
        get() = prefStreams!!.getValue()
}