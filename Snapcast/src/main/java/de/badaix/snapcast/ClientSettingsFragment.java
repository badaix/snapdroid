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

package de.badaix.snapcast;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.text.format.DateUtils;

import org.json.JSONException;
import org.json.JSONObject;

import de.badaix.snapcast.control.json.Client;

/**
 * Created by johannes on 11.01.16.
 */
public class ClientSettingsFragment extends PreferenceFragment {
    private Client client = null;
    private Client clientOriginal = null;
    private EditTextPreference prefName;
    private EditTextPreference prefLatency;
    private Preference prefMac;
    private Preference prefId;
    private Preference prefIp;
    private Preference prefHost;
    private Preference prefOS;
    private Preference prefVersion;
    private Preference prefLastSeen;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        try {
            client = new Client(new JSONObject(bundle.getString("client")));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        clientOriginal = new Client(client.toJson());

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.client_preferences);
        prefName = (EditTextPreference) findPreference("pref_client_name");
        prefName.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                prefName.setSummary((String) newValue);
                client.setName((String) newValue);
                return true;
            }
        });

        prefMac = findPreference("pref_client_mac");
        prefId = findPreference("pref_client_id");
        prefIp = findPreference("pref_client_ip");
        prefHost = findPreference("pref_client_host");
        prefOS = findPreference("pref_client_os");
        prefVersion = findPreference("pref_client_version");
        prefLastSeen = findPreference("pref_client_last_seen");
        prefLatency = (EditTextPreference) findPreference("pref_client_latency");
        prefLatency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String latency = (String) newValue;
                if (TextUtils.isEmpty(latency))
                    latency = "0";
                prefLatency.setSummary(latency + "ms");
                client.getConfig().setLatency(Integer.parseInt(latency));
                return true;
            }
        });
        update();
    }

    public Client getClient() {
        return client;
    }

    public Client getOriginalClientInfo() {
        return clientOriginal;
    }

    public void update() {
        if (client == null)
            return;
        prefName.setSummary(client.getConfig().getName());
        prefName.setText(client.getConfig().getName());
        prefMac.setSummary(client.getHost().getMac());
        prefId.setSummary(client.getId());
        prefIp.setSummary(client.getHost().getIp());
        prefHost.setSummary(client.getHost().getName());
        prefOS.setSummary(client.getHost().getOs() + "@" + client.getHost().getArch());
        prefVersion.setSummary(client.getSnapclient().getVersion());
        String lastSeen = getText(R.string.online).toString();
        if (!client.isConnected()) {
            long lastSeenTs = Math.min(client.getLastSeen().getSec() * 1000, System.currentTimeMillis());
            lastSeen = DateUtils.getRelativeTimeSpanString(lastSeenTs, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS).toString();
        }
        prefLastSeen.setSummary(lastSeen);
        prefLatency.setSummary(client.getConfig().getLatency() + "ms");
        prefLatency.setText(client.getConfig().getLatency() + "");
    }
}
