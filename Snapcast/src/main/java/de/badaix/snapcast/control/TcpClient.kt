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
package de.badaix.snapcast.control

import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetAddress
import java.net.Socket
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Created by johannes on 06.01.16.
 */
class TcpClient(listener: TcpClientListener?) {
    private var mServerMessage: String? = null

    // sends message received notifications
    private var mMessageListener: TcpClientListener? = null

    // while this is true, the server will continue running
    private var mRun = false

    // used to send messages
    private var mBufferOut: PrintWriter? = null

    // used to read messages from the server
    private var mBufferIn: BufferedReader? = null
    private var readerThread: Thread? = null
    private var socket: Socket? = null
    private val messages: BlockingQueue<String?> = LinkedBlockingQueue<String?>()

    /**
     * Constructor of the class. OnMessagedReceived listens for the messages
     * received from server
     */
    init {
        mMessageListener = listener
    }

    val isConnected: Boolean
        get() {
            if (socket == null) return false
            return socket!!.isConnected
        }

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    fun sendMessage(message: String?) {
        messages.offer(message)
    }

    /**
     * Close the connection and release the members
     */
    fun stop() {
        mRun = false

        if (mBufferOut != null) {
            mBufferOut!!.flush()
            mBufferOut!!.close()
        }

        //        mMessageListener = null;
        readerThread!!.interrupt()
        try {
            readerThread!!.join(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        mBufferIn = null
        mBufferOut = null
        mServerMessage = null
    }

    fun start(host: String?, port: Int) {
        val readerRunnable = ReaderRunnable(host, port)
        readerThread = Thread(readerRunnable)
        readerThread!!.start()
    }

    // Declare the interface. The method messageReceived(String message) will
    // must be implemented in the MyActivity
    // class at on asynckTask doInBackground
    interface TcpClientListener {
        fun onMessageReceived(tcpClient: TcpClient?, message: String?)

        fun onConnecting(tcpClient: TcpClient?)

        fun onConnected(tcpClient: TcpClient?)

        fun onDisconnected(tcpClient: TcpClient?, e: Exception?)
    }

    private inner class ReaderRunnable(private val host: String?, private val port: Int) :
        Runnable {
        override fun run() {
            mRun = true
            var exception: Exception? = null
            var writerThread: Thread? = null

            try {
                if (mMessageListener != null) mMessageListener!!.onConnecting(this@TcpClient)

                // here you must put your computer's IP address.
                val serverAddr = InetAddress.getByName(host)

                Log.d(TAG, "Connecting to " + serverAddr.getCanonicalHostName() + ":" + port)

                // create a socket to make the connection with the server
                socket = Socket(serverAddr, port)


                // sends the message to the server
                mBufferOut = PrintWriter(
                    BufferedWriter(
                        OutputStreamWriter(socket!!.getOutputStream())
                    ), true
                )

                // receives the message which the server sends back
                mBufferIn = BufferedReader(
                    InputStreamReader(
                        socket!!.getInputStream()
                    )
                )

                if (mMessageListener != null) mMessageListener!!.onConnected(this@TcpClient)

                writerThread = Thread {
                    while (mRun) {
                        try {
                            val message = messages.poll(50, TimeUnit.MILLISECONDS)
                            if ((message != null) && (mBufferOut != null)) {
                                Log.d(TAG, "Sending: " + message)
                                mBufferOut!!.println(message + "\r\n")
                                mBufferOut!!.flush()
                            }
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                }
                writerThread!!.start()

                // in this while the client listens for the messages sent by the
                // server
                while (mRun) {
                    mServerMessage = mBufferIn!!.readLine()

                    if (mServerMessage != null) {
                        Log.d(TAG, "Received Message: '" + mServerMessage + "'")
                        if (mMessageListener != null) {
                            mMessageListener!!.onMessageReceived(this@TcpClient, mServerMessage)
                        }
                    } else {
                        break
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error", e)
                exception = e
            } finally {
                // the socket must be closed. It is not possible to reconnect to
                // this socket
                // after it is closed, which means a new socket instance has to
                // be created.
                mRun = false
                if (writerThread != null) {
                    try {
                        writerThread.interrupt()
                        writerThread.join(100)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (socket != null) {
                    try {
                        socket!!.close()
                    } catch (ignored: Exception) {
                    }
                }
                socket = null
                if (mMessageListener != null) mMessageListener!!.onDisconnected(
                    this@TcpClient,
                    exception
                )
            }
        }
    }

    companion object {
        private const val TAG = "TCP"
    }
}

