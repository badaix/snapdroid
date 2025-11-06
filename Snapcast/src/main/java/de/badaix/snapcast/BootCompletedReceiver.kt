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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.TextUtils
import de.badaix.snapcast.utils.Settings

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "android.intent.action.BOOT_COMPLETED" -> {
                if (Settings.getInstance(context).isAutostart) {
                    startService(context, SnapclientService.ACTION_START)
                }
            }

            "de.badaix.snapcast.START_SERVICE" -> {
                startService(
                    context,
                    SnapclientService.ACTION_START
                )
            }

            "de.badaix.snapcast.STOP_SERVICE" -> {
                startService(
                    context,
                    SnapclientService.ACTION_STOP
                )
            }
        }
    }

    private fun startService(context: Context, action: String?) {
        val intent = Intent(context, SnapclientService::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .setAction(action)

        if (action === SnapclientService.ACTION_START) {
            val host = Settings.getInstance(context).host
            val port = Settings.getInstance(context).streamPort
            if (TextUtils.isEmpty(host)) return

            intent.putExtra(SnapclientService.EXTRA_HOST, host)
            intent.putExtra(SnapclientService.EXTRA_PORT, port)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}

