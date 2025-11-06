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
package de.badaix.snapcast.utils

import android.annotation.TargetApi
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.DiscoveryListener
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.MulticastLock
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.InetAddress
import javax.inject.Inject

/**
 * Created by johannes on 19.01.16.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
class NsdHelper @Inject constructor(@ApplicationContext private var mContext: Context) {
    private var serviceName: String? = null
    private var serviceType: String? = null
    private val mPort = 0
    private val mHost: InetAddress? = null
    private var mNsdManager: NsdManager? = null
    private var mDiscoveryListener: DiscoveryListener? = null
    private var mResolveListener: NsdManager.ResolveListener? = null
    private var listener: NsdHelperListener? = null
    private var multicastLock: MulticastLock? = null

    fun startListening(serviceType: String?, serviceName: String, listener: NsdHelperListener?) {
        stopListening()
        this.listener = listener
        this.serviceName = serviceName
        this.serviceType = serviceType

        // https://stackoverflow.com/questions/53615125/nsdmanager-discovery-does-not-work-on-android-9
        val wifi = mContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
        if (wifi != null) {
            multicastLock = wifi.createMulticastLock("multicastLock")
            multicastLock!!.setReferenceCounted(true)
            multicastLock!!.acquire()
        }

        initializeResolveListener()
        initializeDiscoveryListener()
        mNsdManager = mContext.getSystemService(Context.NSD_SERVICE) as NsdManager
        mNsdManager!!.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener)
        //        mNsdManager.discoverServices("_snapcast-jsonrpc._tcp.", NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    fun stopListening() {
        if (mDiscoveryListener != null) {
            mNsdManager!!.stopServiceDiscovery(mDiscoveryListener)
            mDiscoveryListener = null
        }
        if (multicastLock != null) {
            multicastLock!!.release()
            multicastLock = null
        }
    }

    private fun initializeResolveListener() {
        mResolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.d(TAG, "Resolve failed")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val info = serviceInfo
                Log.i(
                    TAG,
                    "onServiceResolved, canonical host name: " + info.host
                        .getCanonicalHostName()
                )
                // sometimes it returns an IPv6 address...
                // if (!info.host.getCanonicalHostName().contains(":"))
                listener!!.onResolved(this@NsdHelper, serviceInfo)
            }
        }
    }

    private fun initializeDiscoveryListener() {
        mDiscoveryListener = object : DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Discovery failed, error code: " + errorCode)
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Stopping discovery failed, error code: " + errorCode)
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                Log.i(TAG, "Discovery started, service tyoe: " + serviceType)
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Log.i(TAG, "Discovery stopped, service type: " + serviceType)
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                val info = serviceInfo
                Log.d(TAG, "Service found: " + info.getServiceName())
                if (info.getServiceName().startsWith(serviceName!!)) {
                    try {
                        mNsdManager!!.resolveService(info, mResolveListener)
                    } catch (e: IllegalArgumentException) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                val info = serviceInfo
                Log.d(TAG, "Service lost: " + info.getServiceName())
            }
        }
    }

    interface NsdHelperListener {
        fun onResolved(nsdHelper: NsdHelper?, serviceInfo: NsdServiceInfo?)
    }

    companion object {
        private const val TAG = "NsdHelper"
    }
}
