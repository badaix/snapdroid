package de.badaix.snapcast.control.json;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

public class StreamProperties implements JsonSerialisable {
    private boolean canControl = false;
    private boolean canGoNext = false;
    private boolean canGoPrevious = false;
    private boolean canPlay = false;
    private boolean canPause = false;
    private boolean canSeek = false;

    private String playbackStatus = "unknown";
    private double position = 0;
    private int volume = -1;
    private boolean shuffle = false;
    private boolean mute = false;
    private String loopStatus = "none";
    private double rate = 1.0;

    private String title = "";
    private ArrayList<String> artist = new ArrayList<>();
    private String album = "";
    private ArrayList<String> albumArtist = new ArrayList<>();
    private String artUrl = "";
    private String artDataBase64 = "";
    private String artDataExtension = "";
    private double duration = 0;
    private String url = "";
    private int trackNumber = -1;
    private int discNumber = -1;
    private ArrayList<String> genre = new ArrayList<>();

    private Bitmap cachedArtBitmap = null;
    private boolean artBitmapDecoded = false;

    private JSONObject rawJson = null;

    public StreamProperties() {
    }

    public StreamProperties(JSONObject json) {
        fromJson(json);
    }

    @Override
    public void fromJson(JSONObject json) {
        rawJson = json;

        canControl = json.optBoolean("canControl", false);
        canGoNext = json.optBoolean("canGoNext", false);
        canGoPrevious = json.optBoolean("canGoPrevious", false);
        canPlay = json.optBoolean("canPlay", false);
        canPause = json.optBoolean("canPause", false);
        canSeek = json.optBoolean("canSeek", false);

        playbackStatus = json.optString("playbackStatus", "unknown");
        position = json.optDouble("position", 0);
        volume = json.optInt("volume", -1);
        shuffle = json.optBoolean("shuffle", false);
        mute = json.optBoolean("mute", false);
        loopStatus = json.optString("loopStatus", "none");
        rate = json.optDouble("rate", 1.0);

        JSONObject metadata = json.optJSONObject("metadata");
        if (metadata != null) {
            parseMetadata(metadata);
        }
    }

    private void parseMetadata(JSONObject metadata) {
        title = metadata.optString("title", "");
        album = metadata.optString("album", "");
        artUrl = metadata.optString("artUrl", "");
        url = metadata.optString("url", "");
        duration = metadata.optDouble("duration", 0);
        trackNumber = metadata.optInt("trackNumber", -1);
        discNumber = metadata.optInt("discNumber", -1);

        artist = parseStringArray(metadata, "artist");
        albumArtist = parseStringArray(metadata, "albumArtist");
        genre = parseStringArray(metadata, "genre");

        JSONObject artData = metadata.optJSONObject("artData");
        if (artData != null) {
            artDataBase64 = artData.optString("data", "");
            artDataExtension = artData.optString("extension", "");
            cachedArtBitmap = null;
            artBitmapDecoded = false;
        }
    }

    private ArrayList<String> parseStringArray(JSONObject json, String key) {
        ArrayList<String> result = new ArrayList<>();
        try {
            if (json.has(key)) {
                Object value = json.get(key);
                if (value instanceof JSONArray) {
                    JSONArray arr = (JSONArray) value;
                    for (int i = 0; i < arr.length(); i++) {
                        result.add(arr.getString(i));
                    }
                } else if (value instanceof String) {
                    result.add((String) value);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Merge incoming properties into this instance.
     * Only fields present in the incoming JSON are overwritten,
     * preserving previously known values (e.g. metadata survives
     * a capabilities-only update).
     */
    public void merge(StreamProperties other) {
        if (other.rawJson == null)
            return;

        JSONObject json = other.rawJson;
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            switch (key) {
                case "canControl": canControl = other.canControl; break;
                case "canGoNext": canGoNext = other.canGoNext; break;
                case "canGoPrevious": canGoPrevious = other.canGoPrevious; break;
                case "canPlay": canPlay = other.canPlay; break;
                case "canPause": canPause = other.canPause; break;
                case "canSeek": canSeek = other.canSeek; break;
                case "playbackStatus": playbackStatus = other.playbackStatus; break;
                case "position": position = other.position; break;
                case "volume": volume = other.volume; break;
                case "shuffle": shuffle = other.shuffle; break;
                case "mute": mute = other.mute; break;
                case "loopStatus": loopStatus = other.loopStatus; break;
                case "rate": rate = other.rate; break;
                case "metadata":
                    title = other.title;
                    artist = other.artist;
                    album = other.album;
                    albumArtist = other.albumArtist;
                    artUrl = other.artUrl;
                    artDataBase64 = other.artDataBase64;
                    artDataExtension = other.artDataExtension;
                    duration = other.duration;
                    url = other.url;
                    trackNumber = other.trackNumber;
                    discNumber = other.discNumber;
                    genre = other.genre;
                    cachedArtBitmap = null;
                    artBitmapDecoded = false;
                    break;
            }
        }
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("canControl", canControl);
            json.put("canGoNext", canGoNext);
            json.put("canGoPrevious", canGoPrevious);
            json.put("canPlay", canPlay);
            json.put("canPause", canPause);
            json.put("canSeek", canSeek);
            json.put("playbackStatus", playbackStatus);
            json.put("position", position);
            json.put("shuffle", shuffle);
            json.put("mute", mute);
            json.put("loopStatus", loopStatus);
            json.put("rate", rate);
            if (volume >= 0)
                json.put("volume", volume);

            JSONObject metadata = new JSONObject();
            if (!TextUtils.isEmpty(title)) metadata.put("title", title);
            if (!TextUtils.isEmpty(album)) metadata.put("album", album);
            if (!TextUtils.isEmpty(artUrl)) metadata.put("artUrl", artUrl);
            if (!TextUtils.isEmpty(url)) metadata.put("url", url);
            if (duration > 0) metadata.put("duration", duration);
            if (trackNumber >= 0) metadata.put("trackNumber", trackNumber);
            if (discNumber >= 0) metadata.put("discNumber", discNumber);
            if (!artist.isEmpty()) {
                JSONArray arr = new JSONArray();
                for (String a : artist) arr.put(a);
                metadata.put("artist", arr);
            }
            if (!albumArtist.isEmpty()) {
                JSONArray arr = new JSONArray();
                for (String a : albumArtist) arr.put(a);
                metadata.put("albumArtist", arr);
            }
            if (!genre.isEmpty()) {
                JSONArray arr = new JSONArray();
                for (String g : genre) arr.put(g);
                metadata.put("genre", arr);
            }
            if (!TextUtils.isEmpty(artDataBase64)) {
                JSONObject artData = new JSONObject();
                artData.put("data", artDataBase64);
                artData.put("extension", artDataExtension);
                metadata.put("artData", artData);
            }
            if (metadata.length() > 0)
                json.put("metadata", metadata);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public Bitmap getArtBitmap() {
        if (artBitmapDecoded)
            return cachedArtBitmap;
        artBitmapDecoded = true;
        if (TextUtils.isEmpty(artDataBase64)) {
            cachedArtBitmap = null;
            return null;
        }
        try {
            byte[] decoded = Base64.decode(artDataBase64, Base64.DEFAULT);
            cachedArtBitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
        } catch (Exception e) {
            e.printStackTrace();
            cachedArtBitmap = null;
        }
        return cachedArtBitmap;
    }

    public boolean hasMetadata() {
        return !TextUtils.isEmpty(title) || !artist.isEmpty() || !TextUtils.isEmpty(album);
    }

    public String getArtistString() {
        return TextUtils.join(", ", artist);
    }

    // Capability getters
    public boolean canControl() { return canControl; }
    public boolean canGoNext() { return canGoNext; }
    public boolean canGoPrevious() { return canGoPrevious; }
    public boolean canPlay() { return canPlay; }
    public boolean canPause() { return canPause; }
    public boolean canSeek() { return canSeek; }

    // State getters
    public String getPlaybackStatus() { return playbackStatus; }
    public boolean isPlaying() { return "playing".equalsIgnoreCase(playbackStatus); }
    public boolean isPaused() { return "paused".equalsIgnoreCase(playbackStatus); }
    public double getPosition() { return position; }
    public int getVolume() { return volume; }
    public boolean isShuffle() { return shuffle; }
    public boolean isMute() { return mute; }
    public String getLoopStatus() { return loopStatus; }
    public double getRate() { return rate; }

    // Metadata getters
    public String getTitle() { return title; }
    public ArrayList<String> getArtist() { return artist; }
    public String getAlbum() { return album; }
    public ArrayList<String> getAlbumArtist() { return albumArtist; }
    public String getArtUrl() { return artUrl; }
    public double getDuration() { return duration; }
    public String getUrl() { return url; }
    public int getTrackNumber() { return trackNumber; }
    public int getDiscNumber() { return discNumber; }
    public ArrayList<String> getGenre() { return genre; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StreamProperties that = (StreamProperties) o;
        return canControl == that.canControl &&
                canGoNext == that.canGoNext &&
                canGoPrevious == that.canGoPrevious &&
                canPlay == that.canPlay &&
                canPause == that.canPause &&
                canSeek == that.canSeek &&
                Objects.equals(playbackStatus, that.playbackStatus) &&
                Objects.equals(title, that.title) &&
                Objects.equals(artist, that.artist) &&
                Objects.equals(album, that.album) &&
                Objects.equals(artUrl, that.artUrl) &&
                Objects.equals(artDataBase64, that.artDataBase64);
    }

    @Override
    public int hashCode() {
        int result = (canControl ? 1 : 0);
        result = 31 * result + (playbackStatus != null ? playbackStatus.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (artist != null ? artist.hashCode() : 0);
        result = 31 * result + (album != null ? album.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
