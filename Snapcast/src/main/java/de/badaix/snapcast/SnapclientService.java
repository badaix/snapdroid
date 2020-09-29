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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.UUID;

import de.badaix.snapcast.utils.Settings;

import static android.os.PowerManager.FULL_WAKE_LOCK;
import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

/**
 * Created by johannes on 01.01.16.
 */

public class SnapclientService extends Service {

    private static final String TAG = "Service";

    public static final String EXTRA_HOST = "EXTRA_HOST";
    public static final String EXTRA_PORT = "EXTRA_PORT";
    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String NOTIFICATION_CHANNEL_ID = "de.badaix.snapcast.snapclientservice.defaultchannel";

    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";
    private static String uniqueID = null;

    private final IBinder mBinder = new LocalBinder();
    private java.lang.Process process = null;
    private PowerManager.WakeLock wakeLock = null;
    private WifiManager.WifiLock wifiWakeLock = null;
    private Thread reader = null;
    private boolean running = false;
    private SnapclientListener listener = null;
    private boolean logReceived;
    private Handler restartHandler = new Handler();
    private Runnable restartRunnable = null;
    private String host = null;
    private int port = 0;

    public boolean isRunning() {
        return running;
    }

    public void setListener(SnapclientListener listener) {
        this.listener = listener;
    }

    public void initChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                "Snapclient service",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Snapcast player service");
        notificationManager.createNotificationChannel(channel);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initChannels(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null)
            return START_NOT_STICKY;

        if (ACTION_STOP.equals(intent.getAction())) {
            stopService();
            return START_NOT_STICKY;
        } else if (ACTION_START.equals(intent.getAction())) {
            String host = intent.getStringExtra(EXTRA_HOST);
            int port = intent.getIntExtra(EXTRA_PORT, 1704);

            Intent stopIntent = new Intent(this, SnapclientService.class);
            stopIntent.setAction(ACTION_STOP);
            PendingIntent piStop = PendingIntent.getService(this, 0, stopIntent, 0);

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_media_play)
                            .setTicker(getText(R.string.ticker_text))
                            .setContentTitle(getText(R.string.notification_title))
                            .setContentText(getText(R.string.notification_text))
                            .setContentInfo(host)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(getText(R.string.notification_text)))
                            .addAction(R.drawable.ic_media_stop, getString(R.string.stop), piStop);

            Intent resultIntent = new Intent(this, MainActivity.class);

            // The stack builder object will contain an artificial back stack for the
            // started Activity.
            // This ensures that navigating backward from the Activity leads out of
            // your application to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(MainActivity.class);
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            builder.setContentIntent(resultPendingIntent);
            // mId allows you to update the notification later on.
            final Notification notification = builder.build();
            startForeground(123, notification);

            start(host, port);
            return START_STICKY;
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        stop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void stopPlayer() {
        Log.d(TAG, "stopPlayer");
        stopService();
    }

    private void stopService() {
        Log.d(TAG, "stopService");
        stop();
        stopForeground(true);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(123);
    }

    public synchronized static String getUniqueId(Context context) {
        if (uniqueID == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(
                    PREF_UNIQUE_ID, Context.MODE_PRIVATE);
            uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
            if (uniqueID == null) {
                uniqueID = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(PREF_UNIQUE_ID, uniqueID);
                editor.commit();
            }
        }
        return uniqueID;
    }

    private void startProcess() throws IOException {
        Log.d(TAG, "startProcess");
        String player = "oboe";
        String configuredEngine = Settings.getInstance(getApplicationContext()).getAudioEngine();
        if (configuredEngine.equals("OpenSL"))
            player = "opensl";
        else if (configuredEngine.equals("Oboe"))
            player = "oboe";
        else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                player = "opensl";
            else
                player = "oboe";
        }

        String rate = null;
        String fpb = null;
        String sampleformat = "*:16:*";
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        // boolean bta2dp = audioManager.isBluetoothA2dpOn();
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) && Settings.getInstance(getApplicationContext()).doResample()) {
            rate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
            fpb = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
            sampleformat = rate + ":16:*";
            // bta2dp = false;
            // for (AudioDeviceInfo deviceInfo : audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            //     if (deviceInfo.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
            //         bta2dp = true;
            //         break;
            //     }
            // }
        }
        Log.i(TAG, "Configured engine: " + configuredEngine + ", active engine: " + player + ", sampleformat: " + sampleformat);
        // Log.i(TAG, "Configured engine: " + configuredEngine + ", active engine: " + player + ", sampleformat: " + sampleformat + ", isBluetoothA2dpOn: " + bta2dp);

        ProcessBuilder pb = new ProcessBuilder()
                .command(this.getApplicationInfo().nativeLibraryDir + "/libsnapclient.so", "-h", host, "-p", Integer.toString(port), "--hostID", getUniqueId(this.getApplicationContext()), "--player", player, "--sampleformat", sampleformat, "--logfilter", "*:info,Stats:debug")
                .redirectErrorStream(true);
        Map<String, String> env = pb.environment();
        if (rate != null)
            env.put("SAMPLE_RATE", rate);
        if (fpb != null)
            env.put("FRAMES_PER_BUFFER", fpb);
        process = pb.start();

        Thread reader = new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                String line;
                try {
                    while ((line = bufferedReader.readLine()) != null) {
                        log(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        logReceived = false;
        reader.start();
    }


    private void start(String host, int port) {
        Log.d(TAG, "start host: " + host + ", port: " + port);
        try {
            //https://code.google.com/p/android/issues/detail?id=22763
            if (running)
                return;
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);

            UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
            if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
                Log.d(TAG, "Running on a TV Device");
                wakeLock = powerManager.newWakeLock(FULL_WAKE_LOCK, "snapcast:SnapcastFullWakeLock");
            } else {
                Log.d(TAG, "Running on a non-TV Device");
                wakeLock = powerManager.newWakeLock(PARTIAL_WAKE_LOCK, "snapcast:SnapcastPartialWakeLock");
            }

            wakeLock.acquire();

            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            wifiWakeLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "snapcast:SnapcastWifiWakeLock");
            wifiWakeLock.acquire();
            this.host = host;
            this.port = port;
            startProcess();
        } catch (Exception e) {
            e.printStackTrace();
            if (listener != null)
                listener.onError(this, e.getMessage(), e);
            stop();
        }
    }

    private void log(String msg) {
        if (!logReceived) {
            logReceived = true;
            running = true;
            if (listener != null)
                listener.onPlayerStart(this);
        }
        if (listener != null) {
            int idxSeverityOpen = msg.indexOf('[');
            int idxSeverityClose = msg.indexOf(']', idxSeverityOpen);
            int idxTagOpen = msg.indexOf('(', idxSeverityClose);
            int idxTagClose = msg.indexOf(')', idxTagOpen);
            if ((idxSeverityOpen > 0) && (idxSeverityClose > 0)) {
                String severity = msg.substring(idxSeverityOpen + 1, idxSeverityClose);
                String tag = "";
                if ((idxTagOpen > 0) && (idxTagClose > 0))
                    tag = msg.substring(idxTagOpen + 1, idxTagClose);
                String timestamp = msg.substring(0, idxSeverityOpen - 1);
                String message = msg.substring(Math.max(idxSeverityClose, idxTagClose) + 2);
                listener.onLog(SnapclientService.this, timestamp, severity, tag, message);

                if ((message.equals("Init start")) && (restartRunnable == null)) {
                    restartRunnable = new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "Restarting Snapclient");
                            stopProcess();
                            try {
                                startProcess();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    restartHandler.postDelayed(restartRunnable, 3000);
                } else if ((message.contains("Init failed")) && (restartHandler != null)) {
                    restartHandler.removeCallbacks(restartRunnable);
                    restartHandler.post(restartRunnable);
                } else if ((message.equals("Init done")) && (restartHandler != null)) {
                    restartHandler.removeCallbacks(restartRunnable);
                    restartRunnable = null;
                }
            }
        }
    }

    private void stopProcess() {
        Log.d(TAG, "stopProcess");
        try {
            if (reader != null)
                reader.interrupt();
            if (process != null)
                process.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stop() {
        Log.d(TAG, "stop");
        try {
            stopProcess();
            if ((wakeLock != null) && wakeLock.isHeld())
                wakeLock.release();
            if ((wifiWakeLock != null) && wifiWakeLock.isHeld())
                wifiWakeLock.release();
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
            running = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (listener != null)
            listener.onPlayerStop(this);
    }

    public interface SnapclientListener {
        void onPlayerStart(SnapclientService snapclientService);

        void onPlayerStop(SnapclientService snapclientService);

        void onLog(SnapclientService snapclientService, String timestamp, String logClass, String tag, String msg);

        void onError(SnapclientService snapclientService, String msg, Exception exception);
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        SnapclientService getService() {
            // Return this instance of LocalService so clients can call public methods
            return SnapclientService.this;
        }
    }

}




