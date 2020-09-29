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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import de.badaix.snapcast.utils.NsdHelper;
import de.badaix.snapcast.utils.Settings;

/**
 * Created by johannes on 21.02.16.
 */
public class ServerDialogFragment extends DialogFragment implements View.OnClickListener {

    private static final String TAG = "ServerDialog";
    private Button btnScan;
    private EditText editHost;
    private EditText editStreamPort;
    private EditText editControlPort;
    private CheckBox checkBoxAutoStart;
    private CheckBox checkBoxResample;
    private Spinner spinnerAudioEngine;
    private String host = "";
    private int streamPort = 1704;
    private int controlPort = 1705;
    private boolean autoStart = false;
    private ServerDialogListener listener = null;

    public void setListener(ServerDialogListener listener) {
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(TAG, "onCreateDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View view = inflater.inflate(R.layout.dialog_server, null);
        btnScan = (Button) view.findViewById(R.id.btn_scan);
        btnScan.setOnClickListener(this);

        editHost = (EditText) view.findViewById(R.id.host);
        editStreamPort = (EditText) view.findViewById(R.id.stream_port);
        editControlPort = (EditText) view.findViewById(R.id.control_port);
        checkBoxAutoStart = (CheckBox) view.findViewById(R.id.checkBoxAutoStart);

        spinnerAudioEngine = (Spinner) view.findViewById(R.id.audio_engine);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.audio_engine_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAudioEngine.setAdapter(adapter);
        checkBoxResample = (CheckBox) view.findViewById(R.id.checkBoxResample);

        update();

        builder.setView(view)
                // Add action buttons
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // sign in the user ...
                        host = editHost.getText().toString();
                        try {
                            streamPort = Integer.parseInt(editStreamPort.getText().toString());
                            controlPort = Integer.parseInt(editControlPort.getText().toString());
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                        if (listener != null) {
                            listener.onHostChanged(host, streamPort, controlPort);
                            listener.onAutoStartChanged(checkBoxAutoStart.isChecked());
                        }
                        Settings.getInstance(getContext()).setAudioEngine(spinnerAudioEngine.getSelectedItem().toString(), checkBoxResample.isChecked());
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ServerDialogFragment.this.getDialog().cancel();
                    }
                })
                .setTitle(R.string.settings)
                .setCancelable(false);
        return builder.create();
    }

    @Override
    public void onClick(View v) {
        NsdHelper.getInstance(getContext()).startListening("_snapcast._tcp.", "Snapcast", new NsdHelper.NsdHelperListener() {
            @Override
            public void onResolved(NsdHelper nsdHelper, NsdServiceInfo serviceInfo) {
                Log.d(TAG, "onResolved: " + serviceInfo.getHost().getCanonicalHostName());
                setHost(serviceInfo.getHost().getCanonicalHostName(), serviceInfo.getPort(), serviceInfo.getPort() + 1);
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttach");

        if (context instanceof Activity) {
            update();
        }
    }

    private void update() {
        Log.d(TAG, "update");
        if (this.getActivity() == null)
            return;

        try {
            this.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        editHost.setText(host);
                        editStreamPort.setText(Integer.toString(streamPort));
                        editControlPort.setText(Integer.toString(controlPort));
                        checkBoxAutoStart.setChecked(autoStart);
                        for (int i = 0; i < spinnerAudioEngine.getCount(); ++i) {
                            if (spinnerAudioEngine.getItemAtPosition(i).toString().equals(Settings.getInstance(getContext()).getAudioEngine())) {
                                spinnerAudioEngine.setSelection(i);
                                break;
                            }
                        }
                        checkBoxResample.setChecked(Settings.getInstance(getContext()).doResample());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setHost(String host, int streamPort, int controlPort) {
        this.host = host;
        this.streamPort = streamPort;
        this.controlPort = controlPort;
        update();
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
        update();
    }


    public interface ServerDialogListener {
        void onHostChanged(String host, int streamPort, int controlPort);

        void onAutoStartChanged(boolean autoStart);
    }
}
