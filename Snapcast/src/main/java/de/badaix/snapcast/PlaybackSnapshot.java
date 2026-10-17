package de.badaix.snapcast;

import org.json.JSONArray;
import org.json.JSONObject;

/** Select only the stream belonging to this device, never another room's stream. */
final class PlaybackSnapshot {
    final JSONObject stream;
    final JSONObject client;
    final boolean groupMuted;

    private PlaybackSnapshot(JSONObject stream, JSONObject client, boolean groupMuted) {
        this.stream = stream;
        this.client = client;
        this.groupMuted = groupMuted;
    }

    static PlaybackSnapshot select(JSONObject server, String clientId) {
        JSONArray groups = server.optJSONArray("groups");
        JSONArray streams = server.optJSONArray("streams");
        if (groups == null || streams == null) return null;
        for (int i = 0; i < groups.length(); i++) {
            JSONObject group = groups.optJSONObject(i);
            if (group == null) continue;
            JSONArray clients = group.optJSONArray("clients");
            if (clients == null) continue;
            for (int j = 0; j < clients.length(); j++) {
                JSONObject client = clients.optJSONObject(j);
                if (client == null || !clientId.equals(client.optString("id"))
                        || !client.optBoolean("connected")) continue;
                for (int k = 0; k < streams.length(); k++) {
                    JSONObject stream = streams.optJSONObject(k);
                    if (stream != null && group.optString("stream_id").equals(stream.optString("id")))
                        return new PlaybackSnapshot(stream, client, group.optBoolean("muted"));
                }
            }
        }
        return null;
    }

    JSONObject properties() {
        JSONObject properties = stream.optJSONObject("properties");
        return properties == null ? new JSONObject() : properties;
    }

    boolean muted() {
        JSONObject config = client.optJSONObject("config");
        JSONObject volume = config == null ? null : config.optJSONObject("volume");
        return groupMuted || (volume != null && volume.optBoolean("muted"));
    }

    boolean playing() {
        String playback = properties().optString("playbackStatus");
        return !muted() && (playback.isEmpty() ? "playing".equals(stream.optString("status"))
                : "playing".equalsIgnoreCase(playback));
    }

    boolean hasMedia() {
        String playback = properties().optString("playbackStatus");
        return playback.isEmpty() ? "playing".equals(stream.optString("status"))
                : "playing".equalsIgnoreCase(playback) || "paused".equalsIgnoreCase(playback);
    }
}
