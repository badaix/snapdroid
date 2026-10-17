package de.badaix.snapcast;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/** Service-owned media session and reconnecting Snapserver control connection. */
final class MediaSessionBridge {
    private static final String CHANNEL = "snapcast.playback";
    private static final int NOTIFICATION = 124;
    private final SnapclientService service;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final MediaSession session;
    private final NotificationManager notifications;
    private final BlockingQueue<String> commands = new LinkedBlockingQueue<>();
    private final Runnable stopPlayer;
    private volatile Socket socket;
    private volatile int generation;
    private PlaybackSnapshot current;
    private Thread worker;
    private boolean shown;
    private String lastPresentation = "";
    private final PendingIntent openApp;

    MediaSessionBridge(SnapclientService service, Runnable stopPlayer) {
        this.service = service;
        this.stopPlayer = stopPlayer;
        notifications = (NotificationManager) service.getSystemService(service.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "Snapcast playback",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setSound(null, null);
            notifications.createNotificationChannel(channel);
        }
        openApp = PendingIntent.getActivity(service, 0,
                new Intent(service, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        session = new MediaSession(service, "Snapcast");
        session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setSessionActivity(openApp);
        session.setPlaybackToLocal(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { control("play", "canPlay"); }
            @Override public void onPause() { control("pause", "canPause"); }
            @Override public void onStop() { stopPlayer.run(); }
            @Override public void onSkipToNext() { control("next", "canGoNext"); }
            @Override public void onSkipToPrevious() { control("previous", "canGoPrevious"); }
        }, main);
    }

    void start(String host, int port, String clientId) {
        stop();
        final int run = ++generation;
        worker = new Thread(() -> connect(host, port, clientId, run), "Snapcast-media");
        worker.start();
    }

    private void connect(String host, int port, String clientId, int run) {
        while (generation == run) {
            try (Socket connection = new Socket()) {
                synchronized (this) {
                    if (generation != run) return;
                    socket = connection;
                }
                connection.connect(new InetSocketAddress(host, port), 5000);
                connection.setSoTimeout(500);
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        connection.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(connection.getOutputStream(), true);
                boolean refresh = true;
                long lastRefresh = 0;
                long id = 0;
                while (generation == run) {
                    String command;
                    while ((command = commands.poll()) != null) { writer.println(command); refresh = true; }
                    if (refresh && System.currentTimeMillis() - lastRefresh >= 500) {
                        writer.println(new JSONObject().put("jsonrpc", "2.0").put("id", ++id)
                                .put("method", "Server.GetStatus"));
                        lastRefresh = System.currentTimeMillis();
                        refresh = false;
                    }
                    try {
                        String line = reader.readLine();
                        if (line == null) break;
                        if (line.trim().isEmpty()) continue;
                        JSONArray messages = line.trim().startsWith("[") ? new JSONArray(line)
                                : new JSONArray().put(new JSONObject(line));
                        for (int i = 0; i < messages.length(); i++) {
                            JSONObject message = messages.getJSONObject(i);
                            JSONObject result = message.optJSONObject("result");
                            if (result != null && result.has("server")) {
                                PlaybackSnapshot snapshot = PlaybackSnapshot.select(result.getJSONObject("server"), clientId);
                                main.post(() -> { if (generation == run) present(snapshot); });
                            } else if (message.has("method") || message.has("error") || result != null) {
                                refresh = true;
                                if (message.has("error")) Log.w("Snapcast-media", "Control request failed: " + message.opt("error"));
                            }
                        }
                    } catch (SocketTimeoutException ignored) { }
                }
            } catch (Exception error) {
                if (generation == run) Log.d("Snapcast-media", "Control connection unavailable", error);
            } finally {
                if (generation == run) {
                    socket = null;
                    commands.clear();
                    main.post(() -> { if (generation == run) present(null); });
                }
            }
            try { Thread.sleep(3000); } catch (InterruptedException ignored) { return; }
        }
    }

    private void control(String command, String capability) {
        if (current == null) return;
        try {
            JSONObject properties = current.properties();
            JSONObject params = new JSONObject();
            String method;
            if (properties.optBoolean("canControl") && properties.optBoolean(capability)) {
                method = "Stream.Control";
                params.put("id", current.stream.getString("id")).put("command", command);
            } else if (("play".equals(command) || "pause".equals(command))
                    && !properties.optBoolean("canControl")) {
                // Raw PCM sources cannot pause upstream: pause only this receiver.
                method = "Client.SetVolume";
                JSONObject volume = new JSONObject(current.client.getJSONObject("config")
                        .getJSONObject("volume").toString());
                volume.put("muted", "pause".equals(command));
                params.put("id", current.client.getString("id")).put("volume", volume);
            } else return;
            commands.offer(new JSONObject().put("jsonrpc", "2.0").put("id", "media")
                    .put("method", method).put("params", params).toString());
        } catch (Exception error) { Log.w("Snapcast-media", "Cannot control stream", error); }
    }

    // MediaStyle + a valid MediaSession token is exempt from POST_NOTIFICATIONS.
    // Verified on API 33 with no permission: only the media card appears.
    @SuppressLint("NotificationPermission")
    private void present(PlaybackSnapshot snapshot) {
        current = snapshot;
        if (snapshot == null || !snapshot.hasMedia()) {
            session.setActive(false);
            session.setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_STOPPED, 0, 0).build());
            notifications.cancel(NOTIFICATION);
            shown = false;
            lastPresentation = "";
            return;
        }
        JSONObject properties = snapshot.properties();
        JSONObject metadata = properties.optJSONObject("metadata");
        String title = metadata == null ? "Snapcast" : metadata.optString("title", "Snapcast");
        String artist = "";
        if (metadata != null) {
            Object artists = metadata.opt("artist");
            if (artists instanceof JSONArray) {
                JSONArray array = (JSONArray) artists;
                for (int i = 0; i < array.length(); i++) artist += (i == 0 ? "" : ", ") + array.optString(i);
            } else artist = metadata.optString("artist");
        }
        boolean playing = snapshot.playing();
        long actions = PlaybackState.ACTION_STOP;
        boolean remote = properties.optBoolean("canControl");
        if (!remote || properties.optBoolean("canPlay")) actions |= PlaybackState.ACTION_PLAY;
        if (!remote || properties.optBoolean("canPause")) actions |= PlaybackState.ACTION_PAUSE;
        if ((actions & (PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE)) != 0)
            actions |= PlaybackState.ACTION_PLAY_PAUSE;
        if (remote && properties.optBoolean("canGoNext")) actions |= PlaybackState.ACTION_SKIP_TO_NEXT;
        if (remote && properties.optBoolean("canGoPrevious")) actions |= PlaybackState.ACTION_SKIP_TO_PREVIOUS;
        String presentation = snapshot.stream.optString("id") + title + artist + playing + actions;
        if (shown && presentation.equals(lastPresentation)) return;
        lastPresentation = presentation;
        session.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist).build());
        session.setPlaybackState(new PlaybackState.Builder().setActions(actions)
                .setState(playing ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED,
                        PlaybackState.PLAYBACK_POSITION_UNKNOWN, playing ? 1 : 0).build());
        session.setActive(true);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(service, CHANNEL) : new Notification.Builder(service);
        builder.setSmallIcon(R.drawable.ic_media_play).setContentTitle(title).setContentText(artist)
                .setContentIntent(openApp).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PUBLIC)
                .setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()));
        notifications.notify(NOTIFICATION, builder.build());
        shown = true;
    }

    synchronized void stop() {
        ++generation;
        Socket connection = socket;
        socket = null;
        try { if (connection != null) connection.close(); } catch (Exception ignored) { }
        if (worker != null) worker.interrupt();
        commands.clear();
        present(null);
    }

    void release() { stop(); session.release(); }
}
