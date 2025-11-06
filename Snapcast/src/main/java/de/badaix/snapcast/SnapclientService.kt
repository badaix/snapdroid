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

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.UiModeManager
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WifiLock
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.os.Process
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import de.badaix.snapcast.utils.Settings
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.UUID
import kotlin.math.max
import androidx.core.content.edit

class SnapclientService : Service() {
    private val mBinder: IBinder = LocalBinder()
    private var process: java.lang.Process? = null
    private var wakeLock: WakeLock? = null
    private var wifiWakeLock: WifiLock? = null
    private val reader: Thread? = null
    var isRunning: Boolean = false
        private set
    private var listener: SnapclientListener? = null
    private var logReceived = false
    private val restartHandler = Handler()
    private var restartRunnable: Runnable? = null
    private var host: String? = null
    private var port = 0

    fun setListener(listener: SnapclientListener?) {
        this.listener = listener
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_START -> {
                val host = intent.getStringExtra(EXTRA_HOST)
                val port = intent.getIntExtra(EXTRA_PORT, DEFAULT_PORT)

                val stopIntent = Intent(this, SnapclientService::class.java)
                    .setAction(ACTION_STOP)
                val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

                val builder: NotificationCompat.Builder =
                    NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_media_play)
                        .setTicker(getText(R.string.ticker_text))
                        .setContentTitle(getText(R.string.notification_title))
                        .setContentText(getText(R.string.notification_text))
                        .setContentInfo(host)
                        .setStyle(
                            NotificationCompat.BigTextStyle()
                                .bigText(getText(R.string.notification_text))
                        )
                        .addAction(R.drawable.ic_media_stop, getString(R.string.stop), stopPendingIntent)

                val resultIntent = Intent(this, MainActivity::class.java)

                // The stack builder object will contain an artificial back stack for the
                // started Activity.
                // This ensures that navigating backward from the Activity leads out of
                // your application to the Home screen.
                val stackBuilder = TaskStackBuilder.create(this)
                // Adds the back stack for the Intent (but not the Intent itself)
                stackBuilder.addParentStack(MainActivity::class.java)
                // Adds the Intent that starts the Activity to the top of the stack
                stackBuilder.addNextIntent(resultIntent)
                val resultPendingIntent =
                    stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                builder.setContentIntent(resultPendingIntent)
                // mId allows you to update the notification later on.
                val notification = builder.build()
                startForeground(FOREGROUND_NOTIFICATION_ID, notification)

                start(host, port)
                START_STICKY
            }
            ACTION_STOP -> {
                stopService()
                START_NOT_STICKY
            }
            else -> START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        stop()
    }

    override fun onBind(intent: Intent?): IBinder {
        return mBinder
    }

    fun stopPlayer() {
        Log.d(TAG, "stopPlayer")
        stopService()
    }

    private fun stopService() {
        Log.d(TAG, "stopService")
        stop()
        stopForeground(true)
        val mNotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.cancel(FOREGROUND_NOTIFICATION_ID)
    }

    private fun startProcess() {
        Log.d(TAG, "startProcess")
        val configuredEngine = Settings.getInstance(this).audioEngine
        val player = when {
            configuredEngine == "OpenSL" -> "opensl"
            configuredEngine == "Oboe" -> "oboe"
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O -> "opensl"
            else -> "oboe"
        }

        var rate: String? = null
        var fpb: String? = null
        var sampleformat = "*:16:*"
        val audioManager = ContextCompat.getSystemService(this, AudioManager::class.java)
            ?: throw RuntimeException("AudioManager not available")
        // boolean bta2dp = audioManager.isBluetoothA2dpOn();
        if (Settings.getInstance(this).doResample()) {
            // Query the AudioManager for the native sample rate and let Snapclient to the
            // resampling using soxr's "high quality" configuration.
            // This will result in better latency estimation and thus better audio sync.
            rate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            fpb = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
            sampleformat = "$rate:*:*"
        }
        // bta2dp = false;
        // for (AudioDeviceInfo deviceInfo : audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
        //     if (deviceInfo.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
        //         bta2dp = true;
        //         break;
        //     }
        // }
        Log.i(
            TAG,
            "Configured engine: $configuredEngine, active engine: $player, sampleformat: $sampleformat"
        )

        val command = buildString {
            append("${applicationInfo.nativeLibraryDir}/libsnapclient.so ")
            append("-h $host ")
            append("-p $port ")
            append("--hostID ${getUniqueId()} ")
            append("--player $player ")
            append("--sampleformat $sampleformat ")
            append("--logfilter *:info,Stats:debug")
        }

        val pb = ProcessBuilder()
            .command(command)
            .redirectErrorStream(true)
        val env = pb.environment()
        if (rate != null) env.put("SAMPLE_RATE", rate)
        if (fpb != null) env.put("FRAMES_PER_BUFFER", fpb)
        process = pb.start()

        val readerThread = Thread {
            BufferedReader(InputStreamReader(process!!.inputStream)).use { reader ->
                var line: String?
                try {
                    while ((reader.readLine().also { line = it }) != null) {
                        log(line!!)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        logReceived = false
        readerThread.start()
    }

    private fun start(host: String?, port: Int) {
        Log.d(TAG, "start host: $host, port: $port")
        try {
            //https://code.google.com/p/android/issues/detail?id=22763
            if (this.isRunning) return
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

            val powerManager = ContextCompat.getSystemService(this, PowerManager::class.java)
                ?: throw RuntimeException("PowerManager not available")
            val uiModeManager = ContextCompat.getSystemService(this, UiModeManager::class.java)
                ?: throw RuntimeException("UiModeManager not available")
            val wifiManager = ContextCompat.getSystemService(this, WifiManager::class.java)
                ?: throw RuntimeException("WifiManager not available")

            val wakeLockLevel = if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
                Log.d(TAG, "Running on a TV Device")
                PowerManager.FULL_WAKE_LOCK
            } else {
                Log.d(TAG, "Running on a non-TV Device")
                PowerManager.PARTIAL_WAKE_LOCK
            }
            wakeLock = powerManager.newWakeLock(
                wakeLockLevel, "snapcast:SnapcastWakeLock"
            )
            wakeLock!!.acquire()

            val wifiLockType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            } else {
                WifiManager.WIFI_MODE_FULL_HIGH_PERF
            }
            wifiWakeLock = wifiManager.createWifiLock(
                wifiLockType,
                "snapcast:SnapcastWifiWakeLock"
            )
            wifiWakeLock!!.acquire()

            this.host = host
            this.port = port
            
            startProcess()
        } catch (e: Exception) {
            e.printStackTrace()
            listener?.onError(this, e.message, e)
            stop()
        }
    }

    fun getUniqueId(): String {
        if (uniqueId == null) {
            val sharedPrefs = getSharedPreferences(
                PREF_UNIQUE_ID, MODE_PRIVATE
            )
            uniqueId = sharedPrefs.getString(PREF_UNIQUE_ID, null)
            if (uniqueId == null) {
                uniqueId = UUID.randomUUID().toString()
                sharedPrefs.edit {
                    putString(PREF_UNIQUE_ID, uniqueId)
                }
            }
        }
        return uniqueId!!
    }

    private fun log(msg: String) {
        if (!logReceived) {
            logReceived = true
            this.isRunning = true
            if (listener != null) listener!!.onPlayerStart(this)
        }
        if (listener != null) {
            val idxSeverityOpen = msg.indexOf('[')
            val idxSeverityClose = msg.indexOf(']', idxSeverityOpen)
            val idxTagOpen = msg.indexOf('(', idxSeverityClose)
            val idxTagClose = msg.indexOf(')', idxTagOpen)
            if ((idxSeverityOpen > 0) && (idxSeverityClose > 0)) {
                val severity = msg.substring(idxSeverityOpen + 1, idxSeverityClose)
                var tag = ""
                if ((idxTagOpen > 0) && (idxTagClose > 0)) tag =
                    msg.substring(idxTagOpen + 1, idxTagClose)
                val timestamp = msg.substring(0, idxSeverityOpen - 1)
                val message = msg.substring(max(idxSeverityClose, idxTagClose) + 2)
                listener!!.onLog(this@SnapclientService, timestamp, severity, tag, message)

                if ((message == "Init start") && (restartRunnable == null)) {
                    restartRunnable = object : Runnable {
                        override fun run() {
                            Log.i(TAG, "Restarting Snapclient")
                            stopProcess()
                            try {
                                startProcess()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                    }
                    restartHandler.postDelayed(restartRunnable!!, 3000)
                } else if ((message.contains("Init failed")) && (restartHandler != null)) {
                    restartHandler.removeCallbacks(restartRunnable!!)
                    restartHandler.post(restartRunnable!!)
                } else if ((message == "Init done") && (restartHandler != null)) {
                    restartHandler.removeCallbacks(restartRunnable!!)
                    restartRunnable = null
                }
            }
        }
    }

    private fun stopProcess() {
        Log.d(TAG, "stopProcess")
        try {
            reader?.interrupt()
            process?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stop() {
        Log.d(TAG, "stop")
        try {
            stopProcess()
            wakeLock?.let { if (it.isHeld) it.release() }
            wifiWakeLock?.let { if (it.isHeld) it.release() }
            Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT)
            this.isRunning = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
        listener?.onPlayerStop(this)
    }

    interface SnapclientListener {
        fun onPlayerStart(snapclientService: SnapclientService?)

        fun onPlayerStop(snapclientService: SnapclientService?)

        fun onLog(
            snapclientService: SnapclientService?,
            timestamp: String?,
            logClass: String?,
            tag: String?,
            msg: String?
        )

        fun onError(snapclientService: SnapclientService?, msg: String?, exception: Exception?)
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        val service: SnapclientService
            get() =// Return this instance of LocalService so clients can call public methods
                this@SnapclientService
    }

    companion object {
        private const val TAG = "Service"

        const val EXTRA_HOST: String = "EXTRA_HOST"
        const val EXTRA_PORT: String = "EXTRA_PORT"
        const val ACTION_START: String = "ACTION_START"
        const val ACTION_STOP: String = "ACTION_STOP"
        const val NOTIFICATION_CHANNEL_ID: String =
            "de.badaix.snapcast.snapclientservice.defaultchannel"

        private const val PREF_UNIQUE_ID = "PREF_UNIQUE_ID"
        private var uniqueId: String? = null

        private const val DEFAULT_PORT = 1704

        private const val FOREGROUND_NOTIFICATION_ID = 1
    }
}




