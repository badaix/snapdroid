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

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import de.badaix.snapcast.utils.Settings;

/**
 * Created by johannes on 05.05.16.
 */
public class BroadcastReceiver extends android.content.BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            // Auto start snapclient on boot
            case "android.intent.action.BOOT_COMPLETED":
                if (Settings.getInstance(context).isAutostart()) {
                    startService(context, SnapclientService.ACTION_START);
                }
                break;

            // Control snapclient service via broadcast intents
            case "de.badaix.snapcast.START_SERVICE":
                startService(context, SnapclientService.ACTION_START);
                break;
            case "de.badaix.snapcast.STOP_SERVICE":
                startService(context, SnapclientService.ACTION_STOP);
                break;
        }
    }

    private void startService(Context context, String action) {
        Intent i = new Intent(context, SnapclientService.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        i.setAction(action);

        if (action == SnapclientService.ACTION_START) {
            String host = Settings.getInstance(context).getHost();
            int port = Settings.getInstance(context).getStreamPort();
            if (TextUtils.isEmpty(host))
                return;

            i.putExtra(SnapclientService.EXTRA_HOST, host);
            i.putExtra(SnapclientService.EXTRA_PORT, port);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(i);
        } else {
            context.startService(i);
        }
    }
}

