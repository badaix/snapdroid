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

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.View.OnTouchListener
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import de.badaix.snapcast.ClientItem.ClientItemListener
import de.badaix.snapcast.control.json.Group
import de.badaix.snapcast.control.json.ServerStatus
import java.util.Vector
import kotlin.math.ceil

/**
 * Created by johannes on 04.12.16.
 */
class GroupItem(context: Context, server: ServerStatus, group: Group) : LinearLayout(context),
    OnSeekBarChangeListener, View.OnClickListener, ClientItemListener, OnTouchListener,
    OnFocusChangeListener {
    //    private TextView title;
    private val volumeSeekBar: SeekBar
    private val ibMute: ImageButton
    private val ibSettings: ImageButton
    private val llClient: LinearLayout
    var group: Group? = null
        private set
    private val server: ServerStatus
    private var tvStreamName: TextView? = null
    private var listener: GroupItemListener? = null
    private val llVolume: LinearLayout
    private var hideOffline = false
    private var clientItems: Vector<ClientItem>? = null
    private var clientVolumes: Vector<Int?>? = null
    private var groupVolume = 0

    init {
        val vi = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        vi.inflate(R.layout.group_item, this)
        //        title = (TextView) findViewById(R.id.title);
        volumeSeekBar = findViewById<SeekBar>(R.id.volumeSeekBar)
        ibMute = findViewById<ImageButton>(R.id.ibMute)
        ibMute.setImageResource(R.drawable.volume_up_24px)
        ibMute.setOnClickListener(this)
        ibSettings = findViewById<ImageButton>(R.id.ibSettings)
        ibSettings.setOnClickListener(this)
        llVolume = findViewById<LinearLayout>(R.id.llVolume)
        llVolume.setVisibility(GONE)
        llClient = findViewById<LinearLayout>(R.id.llClient)
        llClient.setLayoutParams(LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        tvStreamName = findViewById<TextView?>(R.id.tvStreamName)
        tvStreamName!!.setOnClickListener { streamChoiceDialog() }
        volumeSeekBar.setOnSeekBarChangeListener(this)
        volumeSeekBar.setOnTouchListener(this)
        volumeSeekBar.setOnFocusChangeListener(this)
        this.server = server
        clientItems = Vector<ClientItem>()
        clientVolumes = Vector<Int?>()
        setGroup(group)
    }

    private fun update() {
//        title.setText(group.name);
        llClient.removeAllViews()
        clientItems!!.clear()
        for (client in group!!.clients) {
            if ((client == null) || client.isDeleted || (hideOffline && !client.isConnected)) continue

            val clientItem = ClientItem(this.getContext(), client)
            clientItem.setListener(this)
            clientItems!!.add(clientItem)
            llClient.addView(clientItem)
        }

        if (group!!.isMuted) ibMute.setImageResource(R.drawable.volume_off_24px)
        else ibMute.setImageResource(R.drawable.volume_up_24px)

        if ((clientItems!!.size >= 2) || ((clientItems!!.size == 1) && group!!.isMuted)) llVolume.setVisibility(
            VISIBLE
        )
        else llVolume.setVisibility(GONE)
        updateVolume()

        val stream = server.getStream(group!!.streamId)
        if ((tvStreamName == null) || (stream == null)) return
        tvStreamName!!.setText(stream.name)
        /*        String codec = stream.getUri().getQuery().get("codec");
        if (codec.contains(":"))
            codec = codec.split(":")[0];
        tvStreamState.setText(stream.getUri().getQuery().get("sampleformat") + " - " + codec + " - " + stream.getStatus().toString());

        title.setEnabled(group.isConnected);
        volumeSeekBar.setProgress(group.config!!.volume.percent);
        if (client.config!!.volume.isMuted)
            ibMute.setImageResource(R.drawable.ic_mute_icon);
        else
            ibMute.setImageResource(R.drawable.ic_speaker_icon);
*/
    }

    private fun updateVolume() {
        var meanVolume = 0.0
        for (c in clientItems!!) {
            meanVolume += c.client!!.config!!.volume!!.percent.toDouble()
        }
        meanVolume /= clientItems!!.size.toDouble()
        volumeSeekBar.setProgress((ceil(meanVolume)).toInt())
    }

    fun setGroup(group: Group) {
        this.group = group
        update()
    }

    fun setListener(listener: GroupItemListener?) {
        this.listener = listener
    }

    fun setHideOffline(hideOffline: Boolean) {
        if (this.hideOffline == hideOffline) return
        this.hideOffline = hideOffline
        update()
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (!fromUser) return

        val delta = progress - groupVolume
        if (delta == 0) return

        val ratio: Double
        if (delta < 0) ratio = (groupVolume - progress).toDouble() / groupVolume.toDouble()
        else ratio = (progress - groupVolume).toDouble() / (100 - groupVolume).toDouble()

        for (i in clientItems!!.indices) {
            val clientItem = clientItems!!.get(i)
            val clientVolume = clientVolumes!!.get(i)!!
            var newVolume = clientVolume
            if (delta < 0) newVolume = (newVolume - ratio * clientVolume).toInt()
            else newVolume = (newVolume + ratio * (100 - clientVolume)).toInt()
            val volume = clientItem.client!!.config!!.volume
            volume!!.percent = newVolume
            clientItem.update()
        }
        if (listener != null) listener!!.onGroupVolumeChanged(this)
    }

    private fun updateClientVolumes() {
        clientVolumes!!.clear()
        for (i in clientItems!!.indices) clientVolumes!!.add(
            clientItems!!.get(i).client!!.config!!.volume!!.percent
        )
        groupVolume = volumeSeekBar.getProgress()
    }

    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            updateClientVolumes()
            Log.d(TAG, "onTouch: " + groupVolume)
        }
        return false
    }

    override fun onFocusChange(view: View?, hasFocus: Boolean) {
        Log.d(TAG, "onFocusChange hasFocus: " + hasFocus)
        if (hasFocus) {
            updateClientVolumes()
            Log.d(TAG, "onFocusChange: " + groupVolume)
        }
    }

    override fun onClick(v: View?) {
        if (v === ibMute) {
            group!!.isMuted = !group!!.isMuted
            update()
            listener!!.onMute(this, group!!.isMuted)
        } else if (v === ibSettings) {
            listener!!.onPropertiesClicked(this)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onVolumeChanged(clientItem: ClientItem?, percent: Int, mute: Boolean) {
        if (listener != null) listener!!.onVolumeChanged(this, clientItem, percent, mute)
        updateVolume()
    }

    override fun onDeleteClicked(clientItem: ClientItem?) {
        if (listener != null) listener!!.onDeleteClicked(this, clientItem)
    }

    override fun onPropertiesClicked(clientItem: ClientItem?) {
        if (listener != null) listener!!.onClientPropertiesClicked(this, clientItem)
    }

    private fun streamChoiceDialog() {
        val streamNames = ArrayList<CharSequence?>()
        val streamIds = ArrayList<String?>()
        var checkedStream = -1
        var index = 0
        for (stream in server.streams) {
            streamNames.add(stream.name)
            streamIds.add(stream.id)
            if (group!!.streamId == stream.id) checkedStream = index
            index++
        }
        val streamNamesArr = streamNames.toArray<CharSequence?>(arrayOf<CharSequence?>())
        val streamIdsArr = streamIds.toArray<String?>(arrayOf<String?>())
        val alertDialog = AlertDialog.Builder(getContext())
        alertDialog.setTitle(getContext().getString(R.string.client_stream))
        alertDialog.setCancelable(true)
        alertDialog.setSingleChoiceItems(
            streamNamesArr,
            checkedStream
        ) { dialog, which ->
            val newStreamId = streamIdsArr[which]
            if (newStreamId != group!!.streamId) {
                listener!!.onGroupStreamChanged(group, newStreamId)
            }
            dialog.cancel()
        }
        val alert = alertDialog.create()
        alert.setCanceledOnTouchOutside(true)
        alert.show()
    }

    interface GroupItemListener {
        fun onGroupVolumeChanged(group: GroupItem?)

        fun onMute(group: GroupItem?, mute: Boolean)

        fun onVolumeChanged(group: GroupItem?, clientItem: ClientItem?, percent: Int, mute: Boolean)

        fun onDeleteClicked(group: GroupItem?, clientItem: ClientItem?)

        fun onClientPropertiesClicked(group: GroupItem?, clientItem: ClientItem?)

        fun onPropertiesClicked(group: GroupItem?)

        fun onGroupStreamChanged(group: Group?, streamId: String?)
    }

    companion object {
        private const val TAG = "GroupItem"
    }
}
