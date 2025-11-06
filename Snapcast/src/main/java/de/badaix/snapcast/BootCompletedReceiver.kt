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
import de.badaix.snapcast.utils.Settings
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settings: Settings

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                if (settings.isAutostart) {
                    startSnapclient(context)
                }
            }
            ACTION_START_SERVICE -> startSnapclient(context)
            ACTION_STOP_SERVICE -> context.stopService(Intent(context, SnapclientService::class.java))
        }
    }

    private fun startSnapclient(context: Context) {
        val i = Intent(context, SnapclientService::class.java)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val host = settings.host
        val port = settings.streamPort
        i.putExtra(SnapclientService.EXTRA_HOST, host)
        i.putExtra(SnapclientService.EXTRA_PORT, port)
        i.action = SnapclientService.ACTION_START

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(i)
        } else {
            context.startService(i)
        }
    }

    companion object {
        const val ACTION_START_SERVICE = "de.badaix.snapcast.START_SERVICE"
        const val ACTION_STOP_SERVICE = "de.badaix.snapcast.STOP_SERVICE"
    }
}
