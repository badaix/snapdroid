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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Vector;

import de.badaix.snapcast.control.json.Client;
import de.badaix.snapcast.control.json.Group;
import de.badaix.snapcast.control.json.ServerStatus;
import de.badaix.snapcast.control.json.Stream;
import de.badaix.snapcast.control.json.Volume;

/**
 * Created by johannes on 04.12.16.
 */


public class GroupItem extends LinearLayout implements SeekBar.OnSeekBarChangeListener, View.OnClickListener, ClientItem.ClientItemListener, View.OnTouchListener, View.OnFocusChangeListener {

    private static final String TAG = "GroupItem";

    //    private TextView title;
    private final SeekBar volumeSeekBar;
    private final ImageButton ibMute;
    private final ImageButton ibSettings;
    private final LinearLayout llClient;
    private Group group;
    private final ServerStatus server;
    private TextView tvStreamName = null;
    private GroupItemListener listener = null;
    private final LinearLayout llVolume;
    private boolean hideOffline = false;
    private Vector<ClientItem> clientItems = null;
    private Vector<Integer> clientVolumes = null;
    private int groupVolume = 0;

    public GroupItem(Context context, ServerStatus server, Group group) {
        super(context);
        LayoutInflater vi = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        vi.inflate(R.layout.group_item, this);
//        title = (TextView) findViewById(R.id.title);
        volumeSeekBar = findViewById(R.id.volumeSeekBar);
        ibMute = findViewById(R.id.ibMute);
        ibMute.setImageResource(R.drawable.volume_up_24px);
        ibMute.setOnClickListener(this);
        ibSettings = findViewById(R.id.ibSettings);
        ibSettings.setOnClickListener(this);
        llVolume = findViewById(R.id.llVolume);
        llVolume.setVisibility(GONE);
        llClient = findViewById(R.id.llClient);
        llClient.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        tvStreamName = findViewById(R.id.tvStreamName);
        tvStreamName.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                streamChoiceDialog();
            }
        });
        volumeSeekBar.setOnSeekBarChangeListener(this);
        volumeSeekBar.setOnTouchListener(this);
        volumeSeekBar.setOnFocusChangeListener(this);
        this.server = server;
        clientItems = new Vector<>();
        clientVolumes = new Vector<>();
        setGroup(group);
    }

    private void update() {
//        title.setText(group.getName());
        llClient.removeAllViews();
        clientItems.clear();
        for (Client client : group.getClients()) {
            if ((client == null) || client.isDeleted() || (hideOffline && !client.isConnected()))
                continue;

            ClientItem clientItem = new ClientItem(this.getContext(), client);
            clientItem.setListener(this);
            clientItems.add(clientItem);
            llClient.addView(clientItem);
        }

        if (group.isMuted())
            ibMute.setImageResource(R.drawable.volume_off_24px);
        else
            ibMute.setImageResource(R.drawable.volume_up_24px);

        if ((clientItems.size() >= 2) || ((clientItems.size() == 1) && group.isMuted()))
            llVolume.setVisibility(VISIBLE);
        else
            llVolume.setVisibility(GONE);
        updateVolume();

        Stream stream = server.getStream(group.getStreamId());
        if ((tvStreamName == null) || (stream == null))
            return;
        tvStreamName.setText(stream.getName());
/*        String codec = stream.getUri().getQuery().get("codec");
        if (codec.contains(":"))
            codec = codec.split(":")[0];
        tvStreamState.setText(stream.getUri().getQuery().get("sampleformat") + " - " + codec + " - " + stream.getStatus().toString());

        title.setEnabled(group.isConnected());
        volumeSeekBar.setProgress(group.getConfig().getVolume().getPercent());
        if (client.getConfig().getVolume().isMuted())
            ibMute.setImageResource(R.drawable.ic_mute_icon);
        else
            ibMute.setImageResource(R.drawable.ic_speaker_icon);
*/
    }

    private void updateVolume() {
        double meanVolume = 0;
        for (ClientItem c : clientItems) {
            meanVolume += c.getClient().getConfig().getVolume().getPercent();
        }
        meanVolume /= clientItems.size();
        volumeSeekBar.setProgress((int) (Math.ceil(meanVolume)));
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(final Group group) {
        this.group = group;
        update();
    }

    public void setListener(GroupItemListener listener) {
        this.listener = listener;
    }

    public void setHideOffline(boolean hideOffline) {
        if (this.hideOffline == hideOffline)
            return;
        this.hideOffline = hideOffline;
        update();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser)
            return;

        int delta = progress - groupVolume;
        if (delta == 0)
            return;

        double ratio;
        if (delta < 0)
            ratio = (double) (groupVolume - progress) / (double) groupVolume;
        else
            ratio = (double) (progress - groupVolume) / (double) (100 - groupVolume);

        for (int i = 0; i < clientItems.size(); ++i) {
            ClientItem clientItem = clientItems.get(i);
            int clientVolume = clientVolumes.get(i);
            int newVolume = clientVolume;
            if (delta < 0)
                newVolume -= ratio * clientVolume;
            else
                newVolume += ratio * (100 - clientVolume);
            Volume volume = clientItem.getClient().getConfig().getVolume();
            volume.setPercent(newVolume);
            clientItem.update();
        }
        if (listener != null)
            listener.onGroupVolumeChanged(this);
    }

    private void updateClientVolumes() {
        clientVolumes.clear();
        for (int i = 0; i < clientItems.size(); ++i)
            clientVolumes.add(clientItems.get(i).getClient().getConfig().getVolume().getPercent());
        groupVolume = volumeSeekBar.getProgress();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            updateClientVolumes();
            Log.d(TAG, "onTouch: " + groupVolume);
        }
        return false;
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        Log.d(TAG, "onFocusChange hasFocus: " + hasFocus);
        if (hasFocus) {
            updateClientVolumes();
            Log.d(TAG, "onFocusChange: " + groupVolume);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == ibMute) {
            group.setMuted(!group.isMuted());
            update();
            listener.onMute(this, group.isMuted());
        } else if (v == ibSettings) {
            listener.onPropertiesClicked(this);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onVolumeChanged(ClientItem clientItem, int percent, boolean mute) {
        if (listener != null)
            listener.onVolumeChanged(this, clientItem, percent, mute);
        updateVolume();
    }

    @Override
    public void onDeleteClicked(ClientItem clientItem) {
        if (listener != null)
            listener.onDeleteClicked(this, clientItem);
    }

    @Override
    public void onPropertiesClicked(ClientItem clientItem) {
        if (listener != null)
            listener.onClientPropertiesClicked(this, clientItem);
    }

    private void streamChoiceDialog() {
        final ArrayList<CharSequence> streamNames = new ArrayList<>();
        final ArrayList<String> streamIds = new ArrayList<>();
        int checkedStream = -1, index = 0;
        for (Stream stream : server.getStreams()) {
            streamNames.add(stream.getName());
            streamIds.add(stream.getId());
            if (group.getStreamId().equals(stream.getId())) checkedStream = index;
            index++;
        }
        final CharSequence[] streamNamesArr = streamNames.toArray(new CharSequence[]{});
        final String[] streamIdsArr = streamIds.toArray(new String[]{});
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
        alertDialog.setTitle(getContext().getString(R.string.client_stream));
        alertDialog.setCancelable(true);
        alertDialog.setSingleChoiceItems(streamNamesArr, checkedStream, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newStreamId = streamIdsArr[which];
                if (!newStreamId.equals(group.getStreamId())) {
                    listener.onGroupStreamChanged(group, newStreamId);
                }
                dialog.cancel();
            }
        });
        AlertDialog alert = alertDialog.create();
        alert.setCanceledOnTouchOutside(true);
        alert.show();
    }

    public interface GroupItemListener {
        void onGroupVolumeChanged(GroupItem group);

        void onMute(GroupItem group, boolean mute);

        void onVolumeChanged(GroupItem group, ClientItem clientItem, int percent, boolean mute);

        void onDeleteClicked(GroupItem group, ClientItem clientItem);

        void onClientPropertiesClicked(GroupItem group, ClientItem clientItem);

        void onPropertiesClicked(GroupItem group);

        void onGroupStreamChanged(Group group, String streamId);
    }

}
