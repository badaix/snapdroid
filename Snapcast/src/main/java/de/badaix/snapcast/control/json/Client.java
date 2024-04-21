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

package de.badaix.snapcast.control.json;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by johannes on 06.01.16.
 */
public class Client implements JsonSerialisable, Comparable<Client> {
    private Host host;
    private Snapclient snapclient;
    private ClientConfig config;
    private Time_t lastSeen;
    private boolean connected;
    private String clientId;
    private boolean deleted = false;

    public Client(JSONObject json) {
        fromJson(json);
    }

    @Override
    public void fromJson(JSONObject json) {
        try {
            host = new Host(json.getJSONObject("host"));
            snapclient = new Snapclient(json.getJSONObject("snapclient"));
            config = new ClientConfig(json.getJSONObject("config"));

            lastSeen = new Time_t(json.getJSONObject("lastSeen"));
            connected = json.getBoolean("connected");
            clientId = json.optString("id", host.mac);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("host", host.toJson());
            json.put("snapclient", snapclient.toJson());
            json.put("config", config.toJson());
            json.put("lastSeen", lastSeen.toJson());
            json.put("connected", connected);
            json.put("id", clientId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public Host getHost() {
        return host;
    }

    public Snapcast getSnapclient() {
        return snapclient;
    }

    public ClientConfig getConfig() {
        return config;
    }

    public Time_t getLastSeen() {
        return lastSeen;
    }

    public void setVolume(Volume volume) {
        this.config.setVolume(volume);
    }

    public void setName(String name) {
        this.config.setName(name);
    }

    public String getVisibleName() {
        if ((config.getName() != null) && !config.getName().isEmpty())
            return config.getName();
        String name = host.getName();
        if (config.getInstance() > 1)
            name += " #" + config.getInstance();
        return name;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getId() {
        return clientId;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Client that = (Client) o;

        if (host != null ? !host.equals(that.host) : that.host != null) return false;
        if (snapclient != null ? !snapclient.equals(that.snapclient) : that.snapclient != null)
            return false;
        if (config != null ? !config.equals(that.config) : that.config != null) return false;
        if (connected != that.connected) return false;
        if (clientId != null ? !clientId.equals(that.clientId) : that.clientId != null)
            return false;
        return (deleted == that.deleted);
    }

    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + (snapclient != null ? snapclient.hashCode() : 0);
        result = 31 * result + (config != null ? config.hashCode() : 0);
        result = 31 * result + (connected ? 1 : 0);
        result = 31 * result + (clientId != null ? clientId.hashCode() : 0);
        result = 31 * result + (deleted ? 1 : 0);
        return result;
    }

    @Override
    public int compareTo(Client another) {
        return getVisibleName().compareToIgnoreCase(another.getVisibleName());
    }
}

