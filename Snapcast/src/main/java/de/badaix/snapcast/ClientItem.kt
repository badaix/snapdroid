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

import android.content.Context
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import de.badaix.snapcast.control.json.Client

class ClientItem @JvmOverloads constructor(context: Context, client: Client) :
    LinearLayout(context), OnSeekBarChangeListener, View.OnClickListener,
    PopupMenu.OnMenuItemClickListener {
    private val title: TextView
    private val volumeSeekBar: SeekBar
    private val ibMute: ImageButton
    private val ibOverflow: ImageButton
    var client: Client? = null
        private set
    private var listener: ClientItemListener? = null

    init {
        val vi = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        vi.inflate(R.layout.client_item, this)
        title = findViewById<TextView>(R.id.title)
        volumeSeekBar = findViewById<SeekBar>(R.id.volumeSeekBar)
        ibMute = findViewById<ImageButton>(R.id.ibMute)
        ibMute.setImageResource(R.drawable.volume_up_24px)
        ibMute.setOnClickListener(this)
        ibOverflow = findViewById<ImageButton>(R.id.ibOverflow)
        ibOverflow.setOnClickListener(this)
        volumeSeekBar.setOnSeekBarChangeListener(this)
        setClient(client)
    }

    fun update() {
        //Log.d(TAG, "update: " + client.visibleName + ", connected: " + client.isConnected);
        if (client!!.visibleName != title.getText()) title.setText(client!!.visibleName)
        title.setEnabled(client!!.isConnected)
        volumeSeekBar.setProgress(client!!.config!!.volume!!.percent)
        if (client!!.config!!.volume!!
                .isMuted
        ) ibMute.setImageResource(R.drawable.volume_off_24px)
        else ibMute.setImageResource(R.drawable.volume_up_24px)
    }

    fun setClient(client: Client) {
        this.client = client
        update()
    }

    fun setListener(listener: ClientItemListener?) {
        this.listener = listener
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (fromUser && (listener != null)) {
            val volume = client!!.config!!.volume!!
            volume!!.percent = progress
            listener!!.onVolumeChanged(this, volume!!.percent, volume.isMuted)
        }
    }

    override fun onClick(v: View) {
        if (v === ibMute) {
            val volume = client!!.config!!.volume
            volume!!.isMuted = !volume!!.isMuted
            update()
            listener!!.onVolumeChanged(this, volume.percent, volume.isMuted)
        } else if (v === ibOverflow) {
            val popup = PopupMenu(v.context, v)
            popup.getMenu().add(Menu.NONE, R.id.menu_details, 0, R.string.menu_details)
            if (!client!!.isConnected) popup.menu
                .add(Menu.NONE, R.id.menu_delete, 1, R.string.menu_delete)
            popup.setOnMenuItemClickListener(this)
            popup.show()
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        val id = item.getItemId()

        if (id == R.id.menu_details) {
            listener!!.onPropertiesClicked(this)
            return true
        }

        if (id == R.id.menu_delete) {
            listener!!.onDeleteClicked(this)
            return true
        }

        return false
    }

    interface ClientItemListener {
        fun onVolumeChanged(clientItem: ClientItem?, percent: Int, mute: Boolean)

        fun onDeleteClicked(clientItem: ClientItem?)

        fun onPropertiesClicked(clientItem: ClientItem?)
    }

    companion object {
        private const val TAG = "ClientItem"
    }
}
