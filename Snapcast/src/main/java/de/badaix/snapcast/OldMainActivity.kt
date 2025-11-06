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

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import de.badaix.snapcast.GroupItem.GroupItemListener
import de.badaix.snapcast.ServerDialogFragment.ServerDialogListener
import de.badaix.snapcast.SnapclientService.LocalBinder
import de.badaix.snapcast.SnapclientService.SnapclientListener
import de.badaix.snapcast.control.RemoteControl
import de.badaix.snapcast.control.RemoteControl.RPCEvent
import de.badaix.snapcast.control.RemoteControl.RemoteControlListener
import de.badaix.snapcast.control.json.Client
import de.badaix.snapcast.control.json.Group
import de.badaix.snapcast.control.json.ServerStatus
import de.badaix.snapcast.control.json.Stream
import de.badaix.snapcast.control.json.Volume
import de.badaix.snapcast.utils.NsdHelper
import de.badaix.snapcast.utils.Settings
import org.json.JSONException
import org.json.JSONObject
import java.net.UnknownHostException
import javax.inject.Inject

@AndroidEntryPoint
class OldMainActivity : AppCompatActivity(), ServerDialogListener, GroupItemListener,
    SnapclientListener, RemoteControlListener, NsdHelper.NsdHelperListener {

    @Inject
    lateinit var nsdHelper: NsdHelper

    @Inject
    lateinit var settings: Settings

    var bound: Boolean = false
    private var miStartStop: MenuItem? = null
    private var miSettings: MenuItem? = null

    //    private MenuItem miRefresh = null;
    private var host = ""
    private var port = 1704
    private var controlPort = 1705
    private var remoteControl: RemoteControl? = null
    private var serverStatus: ServerStatus? = null
    private var snapclientService: SnapclientService? = null
    private var groupListFragment: GroupListFragment? = null
    private var warningSamplerateSnackbar: Snackbar? = null
    private var deleteSnackbar: Snackbar? = null
    private var coordinatorLayout: CoordinatorLayout? = null
    private var btnConnect: Button? = null
    private var batchActive = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT)
                .show()
        } else {
            Toast.makeText(
                this,
                "Snapdroid can't post notifications without POST_NOTIFICATIONS permission",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API Level > 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName?,
            service: IBinder?
        ) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as LocalBinder
            snapclientService = binder.service
            snapclientService!!.setListener(this@OldMainActivity)
            bound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName?) {
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        coordinatorLayout = findViewById<CoordinatorLayout>(R.id.myCoordinatorLayout)
        val toolbar = findViewById<Toolbar?>(R.id.toolbar)
        setSupportActionBar(toolbar)

        btnConnect = findViewById<Button?>(R.id.btnConnect)
        btnConnect!!.setVisibility(View.GONE)

        groupListFragment =
            getSupportFragmentManager().findFragmentById(R.id.groupListFragment) as GroupListFragment?
        groupListFragment!!.setHideOffline(settings.getBoolean("hide_offline", false))

        setActionbarSubtitle("Host: no Snapserver found")
        serverStatus = ServerStatus()
        askNotificationPermission()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_snapcast, menu)
        miStartStop = menu.findItem(R.id.action_play_stop)
        miSettings = menu.findItem(R.id.action_settings)
        val isChecked = settings.getBoolean("hide_offline", false)
        val menuItem = menu.findItem(R.id.action_hide_offline)
        menuItem.setChecked(isChecked)
        if (remoteControl != null) {
            updateMenuItems(remoteControl!!.isConnected)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.getItemId()

        if (id == R.id.action_settings) {
            val serverDialogFragment = ServerDialogFragment()
            serverDialogFragment.setHost(
                settings.host,
                settings.streamPort,
                settings.controlPort
            )
            serverDialogFragment.setAutoStart(settings.isAutostart)
            serverDialogFragment.setListener(this)
            serverDialogFragment.show(getSupportFragmentManager(), "serverDialogFragment")
            return true
        } else if (id == R.id.action_play_stop) {
            if (bound && snapclientService!!.isRunning) {
                stopSnapclient()
            } else {
                item.setEnabled(false)
                startSnapclient()
            }
            return true
        } else if (id == R.id.action_hide_offline) {
            item.setChecked(!item.isChecked())
            settings.put("hide_offline", item.isChecked())
            groupListFragment!!.setHideOffline(item.isChecked())
            return true
        } else if (id == R.id.action_refresh) {
            if (host.trim { it <= ' ' }.isEmpty()) {
                showWarning(getString(R.string.host_empty))
            } else {
                startRemoteControl()
            }
            remoteControl!!.serverStatus
        } else if (id == R.id.action_about) {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }

        return super.onOptionsItemSelected(item)
    }

    private fun updateStartStopMenuItem() {
        this@OldMainActivity.runOnUiThread(object : Runnable {
            override fun run() {
                if (bound && snapclientService!!.isRunning) {
                    Log.d(TAG, "updateStartStopMenuItem: ic_media_stop")
                    miStartStop!!.setIcon(R.drawable.ic_media_stop)
                } else {
                    Log.d(TAG, "updateStartStopMenuItem: ic_media_play")
                    miStartStop!!.setIcon(R.drawable.ic_media_play)
                }
                if (miStartStop != null) {
                    miStartStop!!.setEnabled(true)
                }
            }
        })
    }

    private fun startSnapclient() {
        if (TextUtils.isEmpty(host)) return

        val i = Intent(this, SnapclientService::class.java)
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        i.putExtra(SnapclientService.EXTRA_HOST, host)
        i.putExtra(SnapclientService.EXTRA_PORT, port)
        i.setAction(SnapclientService.ACTION_START)

        startService(i)
    }

    private fun stopSnapclient() {
        if (bound) snapclientService!!.stopPlayer()
        //        stopService(new Intent(this, SnapclientService.class));
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }


    private fun startRemoteControl() {
        if (remoteControl == null) remoteControl = RemoteControl(this)
        if (!host.isEmpty()) remoteControl!!.connect(host, controlPort)
    }

    private fun stopRemoteControl() {
        if ((remoteControl != null) && (remoteControl!!.isConnected)) remoteControl!!.disconnect()
        remoteControl = null
    }


    public override fun onResume() {
        super.onResume()
        startRemoteControl()
    }

    public override fun onStart() {
        super.onStart()

        if (TextUtils.isEmpty(settings.host)) {
            nsdHelper.startListening("_snapcast._tcp.", SERVICE_NAME, this)
        } else {
            setHost(
                settings.host!!,
                settings.streamPort,
                settings.controlPort
            )
        }

        val intent = Intent(this, SnapclientService::class.java)
        bindService(intent, mConnection, BIND_AUTO_CREATE)
    }

    public override fun onDestroy() {
        stopRemoteControl()
        super.onDestroy()
    }

    public override fun onStop() {
        super.onStop()

        nsdHelper.stopListening()
        // Unbind from the service
        if (bound) {
            unbindService(mConnection)
            bound = false
        }
    }

    override fun onPlayerStart(snapclientService: SnapclientService?) {
        Log.d(TAG, "onPlayerStart")
        updateStartStopMenuItem()
    }

    override fun onPlayerStop(snapclientService: SnapclientService?) {
        Log.d(TAG, "onPlayerStop")
        updateStartStopMenuItem()
        if (warningSamplerateSnackbar != null) warningSamplerateSnackbar!!.dismiss()
    }

    override fun onLog(
        snapclientService: SnapclientService?,
        timestamp: String?,
        logClass: String?,
        tag: String?,
        msg: String?
    ) {
        if (msg == null) return
        Log.d(TAG, "[$logClass] ($tag) $msg")
        if ("Info" != logClass || "Controller" != tag) {
            if ("err" == logClass || "Emerg" == logClass || "Alert" == logClass || "Crit" == logClass || "Err" == logClass || "Error" == logClass) {
                showWarning(msg)
            }
        }
    }

    private fun showWarning(msg: String) {
        if (warningSamplerateSnackbar != null) warningSamplerateSnackbar!!.dismiss()
        warningSamplerateSnackbar = Snackbar.make(
            coordinatorLayout!!,
            msg, Snackbar.LENGTH_LONG
        )
        warningSamplerateSnackbar!!.show()
    }

    override fun onError(
        snapclientService: SnapclientService?,
        msg: String?,
        exception: Exception?
    ) {
        updateStartStopMenuItem()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_CANCELED) {
            return
        }
        if (requestCode == CLIENT_PROPERTIES_REQUEST) {
            if (data == null) return
            val client: Client?
            try {
                client = Client(JSONObject(data.getStringExtra("client")))
            } catch (e: JSONException) {
                e.printStackTrace()
                return
            }

            val clientOriginal: Client?
            try {
                clientOriginal = Client(JSONObject(data.getStringExtra("clientOriginal")))
            } catch (e: JSONException) {
                e.printStackTrace()
                return
            }
            Log.d(
                TAG,
                "new name: " + client.config!!
                    .name + ", old name: " + clientOriginal.config!!.name
            )
            if (client.config!!.name != clientOriginal.config!!
                    .name
            ) remoteControl!!.setName(client, client.config!!.name)
            Log.d(
                TAG,
                "new latency: " + client.config!!
                    .latency + ", old latency: " + clientOriginal.config!!.latency
            )
            if (client.config!!.latency != clientOriginal.config!!
                    .latency
            ) remoteControl!!.setLatency(client, client.config!!.latency)
            if (serverStatus != null) {
                serverStatus!!.updateClient(client)
                groupListFragment!!.updateServer(serverStatus)
            }
        } else if (requestCode == GROUP_PROPERTIES_REQUEST) {
            if (data == null) return
            val groupId = data.getStringExtra("group")
            val changed = false
            if (data.hasExtra("clients")) {
                val clients = data.getStringArrayListExtra("clients")
                if (clients == null) return
                remoteControl!!.setClients(groupId, clients)
            }
            if (data.hasExtra("stream")) {
                val streamId = data.getStringExtra("stream")
                remoteControl!!.setStream(groupId, streamId)
                onStreamChanged(RPCEvent.response, groupId, streamId)
            }
        }
    }


    private fun setActionbarSubtitle(subtitle: String?) {
        this@OldMainActivity.runOnUiThread {
            val actionBar = supportActionBar
            actionBar?.subtitle = subtitle
        }
    }

    private fun setHost(host: String, streamPort: Int, controlPort: Int) {
        if (TextUtils.isEmpty(host)) return
        this.host = host
        this.port = streamPort
        this.controlPort = controlPort
        settings.setHost(host, streamPort, controlPort)
    }

    fun updateMenuItems(connected: Boolean) {
        this.runOnUiThread {
            if (connected) {
                if (miSettings != null) miSettings!!.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                if (miStartStop != null) miStartStop!!.isVisible = true
            } else {
                if (miSettings != null) miSettings!!.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                if (miStartStop != null) miStartStop!!.isVisible = false
            }
        }
    }


    override fun onResolved(nsdHelper: NsdHelper?, serviceInfo: NsdServiceInfo?) {
        if (serviceInfo == null) return
        Log.d(TAG, "resolved: $serviceInfo")
        setHost(
            serviceInfo.host.getCanonicalHostName(),
            serviceInfo.port,
            serviceInfo.port + 1
        )
        startRemoteControl()
        nsdHelper!!.stopListening()
    }


    override fun onGroupVolumeChanged(groupItem: GroupItem?) {
        if (groupItem == null) return
        remoteControl!!.setGroupVolume(groupItem.group!!)
    }

    override fun onMute(groupItem: GroupItem?, mute: Boolean) {
        if (groupItem == null) return
        remoteControl!!.setGroupMuted(groupItem.group!!, mute)
    }

    override fun onGroupStreamChanged(group: Group?, streamId: String?) {
        if (group == null) return
        remoteControl!!.setStream(group, streamId)
        onStreamChanged(RPCEvent.response, group.id, streamId)
    }

    override fun onVolumeChanged(
        groupItem: GroupItem?,
        clientItem: ClientItem?,
        percent: Int,
        mute: Boolean
    ) {
        if (clientItem == null) return
        remoteControl!!.setVolume(clientItem.client!!, percent, mute)
    }

    override fun onDeleteClicked(groupItem: GroupItem?, clientItem: ClientItem?) {
        if (clientItem == null) return
        if ((deleteSnackbar != null) && deleteSnackbar!!.isShown()) {
            deleteSnackbar!!.dismiss()
        }

        val client = clientItem.client!!
        client.isDeleted = true

        serverStatus!!.updateClient(client)
        groupListFragment!!.updateServer(serverStatus)
        deleteSnackbar = Snackbar.make(
            coordinatorLayout!!,
            getString(R.string.client_deleted, client.visibleName),
            Snackbar.LENGTH_SHORT
        )
        deleteSnackbar!!.setAction(R.string.undo_string, object : View.OnClickListener {
            override fun onClick(v: View?) {
                client.isDeleted = false
                serverStatus!!.updateClient(client)
                groupListFragment!!.updateServer(serverStatus)
            }
        })
        deleteSnackbar!!.addCallback(object : Snackbar.Callback() {
            var dismissed: Boolean = false

            override fun onDismissed(snackbar: Snackbar?, event: Int) {
                super.onDismissed(snackbar, event)
                if ((event != DISMISS_EVENT_ACTION) && !dismissed) {
                    remoteControl!!.delete(client)
                    serverStatus!!.removeClient(client)
                    dismissed = true
                }
            }
        })
        deleteSnackbar!!.show()
    }

    override fun onClientPropertiesClicked(groupItem: GroupItem?, clientItem: ClientItem?) {
        if (clientItem == null) return
        val intent = Intent(this, ClientSettingsActivity::class.java)
        intent.putExtra("client", clientItem.client!!.toJson().toString())
        intent.setFlags(0)
        startActivityForResult(intent, CLIENT_PROPERTIES_REQUEST)
    }

    override fun onPropertiesClicked(groupItem: GroupItem?) {
        if (groupItem == null) return
        val intent = Intent(this, GroupSettingsActivity::class.java)
        intent.putExtra("serverStatus", serverStatus!!.toJson().toString())
        intent.putExtra("group", groupItem.group!!.toJson().toString())
        intent.setFlags(0)
        startActivityForResult(intent, GROUP_PROPERTIES_REQUEST)
    }

    override fun onConnected(remoteControl: RemoteControl?) {
        if (remoteControl == null) return
        setActionbarSubtitle(remoteControl.host)
        remoteControl.serverStatus
        updateMenuItems(true)
    }

    override fun onConnecting(remoteControl: RemoteControl?) {
        if (remoteControl == null) return
        setActionbarSubtitle("connecting: " + remoteControl.host)
    }

    override fun onDisconnected(remoteControl: RemoteControl?, e: Exception?) {
        Log.d(TAG, "onDisconnected")
        serverStatus = ServerStatus()
        groupListFragment!!.updateServer(serverStatus)
        if (e != null) {
            if (e is UnknownHostException) setActionbarSubtitle("error: unknown host")
            else setActionbarSubtitle("error: " + e.message)
        } else {
            setActionbarSubtitle("not connected")
        }
        updateMenuItems(false)
    }


    override fun onBatchStart() {
        batchActive = true
    }


    override fun onBatchEnd() {
        batchActive = false
        groupListFragment!!.updateServer(serverStatus)
    }

    override fun onConnect(client: Client?) {
        if (client == null) return
        serverStatus!!.getClient(client.id)
        client.isConnected = true
        serverStatus!!.updateClient(client)
        groupListFragment!!.updateServer(serverStatus)
    }

    override fun onDisconnect(clientId: String?) {
        val client = serverStatus!!.getClient(clientId)
        if (client == null) {
            remoteControl!!.serverStatus
            return
        }
        client.isConnected = false
        serverStatus!!.updateClient(client)
        groupListFragment!!.updateServer(serverStatus)
    }

    override fun onUpdate(client: Client?) {
        serverStatus!!.updateClient(client)
        groupListFragment!!.updateServer(serverStatus)
    }

    override fun onVolumeChanged(event: RPCEvent?, clientId: String?, volume: Volume?) {
        if (event == RPCEvent.response) return
        val client = serverStatus!!.getClient(clientId)
        if (client == null) {
            remoteControl!!.serverStatus
            return
        }
        client.setVolume(volume)
        if (!batchActive) groupListFragment!!.updateServer(serverStatus)
    }

    override fun onLatencyChanged(event: RPCEvent?, clientId: String?, latency: Long) {
        val client = serverStatus!!.getClient(clientId)
        if (client == null) {
            remoteControl!!.serverStatus
            return
        }
        client.config!!.latency = latency.toInt()
        groupListFragment!!.updateServer(serverStatus)
    }

    override fun onNameChanged(event: RPCEvent?, clientId: String?, name: String?) {
        val client = serverStatus!!.getClient(clientId)
        if (client == null) {
            remoteControl!!.serverStatus
            return
        }
        client.config!!.name = name
        groupListFragment!!.updateServer(serverStatus)
    }

    override fun onUpdate(group: Group?) {
        serverStatus!!.updateGroup(group)
        groupListFragment!!.updateServer(serverStatus)
    }

    override fun onMute(event: RPCEvent?, groupId: String?, mute: Boolean) {
        val g = serverStatus!!.getGroup(groupId)
        if (g == null) {
            remoteControl!!.serverStatus
            return
        }
        g.isMuted = mute
        serverStatus!!.updateGroup(g)
        groupListFragment!!.updateServer(serverStatus)
    }

    override fun onStreamChanged(event: RPCEvent?, groupId: String?, streamId: String?) {
        val g = serverStatus!!.getGroup(groupId)
        if (g == null) {
            remoteControl!!.serverStatus
            return
        }
        g.streamId = streamId
        serverStatus!!.updateGroup(g)
        groupListFragment!!.updateServer(serverStatus)
    }

    override fun onUpdate(server: ServerStatus?) {
        this.serverStatus = server
        groupListFragment!!.updateServer(serverStatus)
    }

    override fun onUpdate(streamId: String?, stream: Stream?) {
        serverStatus!!.updateStream(stream)
        groupListFragment!!.updateServer(serverStatus)
    }

    override fun onHostChanged(
        host: String?,
        streamPort: Int,
        controlPort: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun onAutoStartChanged(autoStart: Boolean) {
        TODO("Not yet implemented")
    }

    companion object {
        const val CLIENT_PROPERTIES_REQUEST: Int = 1
        const val GROUP_PROPERTIES_REQUEST: Int = 2
        private const val TAG = "Main"
        private const val SERVICE_NAME = "Snapcast" // #2";
    }
}
