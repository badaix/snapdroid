package de.badaix.snapcast;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlaybackSnapshotTest {
    private JSONObject server() throws Exception {
        return new JSONObject("{\"groups\":[{\"stream_id\":\"other\",\"clients\":[{\"id\":\"other-room\",\"connected\":true}]},{\"stream_id\":\"ours\",\"clients\":[{\"id\":\"clock\",\"connected\":true,\"config\":{\"volume\":{\"percent\":48,\"muted\":false}}}]}],\"streams\":[{\"id\":\"other\",\"status\":\"playing\"},{\"id\":\"ours\",\"status\":\"idle\"}]}");
    }

    @Test public void ignoresPlaybackInOtherRooms() throws Exception {
        PlaybackSnapshot snapshot = PlaybackSnapshot.select(server(), "clock");
        assertEquals("ours", snapshot.stream.getString("id"));
        assertFalse(snapshot.hasMedia());
        assertFalse(snapshot.playing());
    }

    @Test public void handlesRemovedAndDisconnectedClient() throws Exception {
        JSONObject server = server();
        assertNull(PlaybackSnapshot.select(server, "missing"));
        server.getJSONArray("groups").getJSONObject(1).getJSONArray("clients")
                .getJSONObject(0).put("connected", false);
        assertNull(PlaybackSnapshot.select(server, "clock"));
        assertNull(PlaybackSnapshot.select(new JSONObject(), "clock"));
    }

    @Test public void pauseMutesOnlyReceiverAndKeepsMediaAvailable() throws Exception {
        PlaybackSnapshot snapshot = PlaybackSnapshot.select(server(), "clock");
        snapshot.stream.put("status", "playing");
        assertTrue(snapshot.playing());
        snapshot.client.getJSONObject("config").getJSONObject("volume").put("muted", true);
        assertFalse(snapshot.playing());
        assertTrue(snapshot.hasMedia());
        assertEquals(48, snapshot.client.getJSONObject("config").getJSONObject("volume").getInt("percent"));
    }

    @Test public void respectsSourcePauseEvenWithBufferedAudio() throws Exception {
        PlaybackSnapshot snapshot = PlaybackSnapshot.select(server(), "clock");
        snapshot.stream.put("status", "playing").put("properties",
                new JSONObject().put("playbackStatus", "paused"));
        assertFalse(snapshot.playing());
        assertTrue(snapshot.hasMedia());
    }

    @Test public void stoppedSourceRemovesMediaEvenWithBufferedAudio() throws Exception {
        PlaybackSnapshot snapshot = PlaybackSnapshot.select(server(), "clock");
        snapshot.stream.put("status", "playing").put("properties",
                new JSONObject().put("playbackStatus", "stopped"));
        assertFalse(snapshot.playing());
        assertFalse(snapshot.hasMedia());
    }

    @Test public void groupMuteIsNotPresentedAsPlaying() throws Exception {
        JSONObject server = server();
        server.getJSONArray("groups").getJSONObject(1).put("muted", true);
        PlaybackSnapshot snapshot = PlaybackSnapshot.select(server, "clock");
        snapshot.stream.put("status", "playing");
        assertFalse(snapshot.playing());
    }
}
