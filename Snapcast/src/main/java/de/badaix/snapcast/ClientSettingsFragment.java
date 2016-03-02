package de.badaix.snapcast;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.text.format.DateUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import de.badaix.snapcast.control.ClientInfo;
import de.badaix.snapcast.control.Stream;

/**
 * Created by johannes on 11.01.16.
 */
public class ClientSettingsFragment extends PreferenceFragment {
    private ClientInfo clientInfo = null;
    private ClientInfo clientInfoOriginal = null;
    private EditTextPreference prefName;
    private ListPreference prefStream;
    private EditTextPreference prefLatency;
    private Preference prefMac;
    private Preference prefIp;
    private Preference prefHost;
    private Preference prefVersion;
    private Preference prefLastSeen;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        try {
            clientInfo = new ClientInfo(new JSONObject(bundle.getString("client")));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        clientInfoOriginal = new ClientInfo(clientInfo.toJson());
        final ArrayList<Stream> streams = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(bundle.getString("streams"));
            for (int i = 0; i < jsonArray.length(); i++)
                streams.add(new Stream(jsonArray.getJSONObject(i)));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final CharSequence[] streamNames = new CharSequence[streams.size()];
        final CharSequence[] streamIds = new CharSequence[streams.size()];
        for (int i = 0; i < streams.size(); ++i) {
            streamNames[i] = streams.get(i).getName();
            streamIds[i] = streams.get(i).getId();
        }

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.client_preferences);
        prefName = (EditTextPreference) findPreference("pref_client_name");
        prefName.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                prefName.setSummary((String) newValue);
                clientInfo.setName((String) newValue);
                return true;
            }
        });
        prefStream = (ListPreference) findPreference("pref_client_stream");
        prefStream.setEntries(streamNames);
        prefStream.setEntryValues(streamIds);
        for (int i = 0; i < streams.size(); ++i) {
            if (streamIds[i].equals(clientInfo.getStream())) {
                prefStream.setSummary(streamNames[i]);
                prefStream.setValueIndex(i);
                break;
            }
        }
        prefStream.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                for (int i = 0; i < streams.size(); ++i) {
                    if (streamIds[i].equals(newValue)) {
                        prefStream.setSummary(streamNames[i]);
                        clientInfo.setStream(streamIds[i].toString());
                        prefStream.setValueIndex(i);
                        break;
                    }
                }

                return false;
            }
        });
        prefMac = (Preference) findPreference("pref_client_mac");
        prefIp = (Preference) findPreference("pref_client_ip");
        prefHost = (Preference) findPreference("pref_client_host");
        prefVersion = (Preference) findPreference("pref_client_version");
        prefLastSeen = (Preference) findPreference("pref_client_last_seen");
        prefLatency = (EditTextPreference) findPreference("pref_client_latency");
        prefLatency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String latency = (String) newValue;
                if (TextUtils.isEmpty(latency))
                    latency = "0";
                prefLatency.setSummary(latency + "ms");
                clientInfo.setLatency(Integer.parseInt(latency));
                return true;
            }
        });
        update();
    }

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public ClientInfo getOriginalClientInfo() {
        return clientInfoOriginal;
    }

    public void update() {
        if (clientInfo == null)
            return;
        prefName.setSummary(clientInfo.getName());
        prefName.setText(clientInfo.getName());
        prefMac.setSummary(clientInfo.getMac());
        prefIp.setSummary(clientInfo.getIp());
        prefHost.setSummary(clientInfo.getHost());
        prefVersion.setSummary(clientInfo.getVersion());
        String lastSeen = getText(R.string.online).toString();
        if (!clientInfo.isConnected()) {
            long lastSeenTs = Math.min(clientInfo.getLastSeen().getSec() * 1000, System.currentTimeMillis());
            lastSeen = DateUtils.getRelativeTimeSpanString(lastSeenTs, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS).toString();
        }
        prefLastSeen.setSummary(lastSeen);
        prefLatency.setSummary(clientInfo.getLatency() + "ms");
        prefLatency.setText(clientInfo.getLatency() + "");
    }
}
