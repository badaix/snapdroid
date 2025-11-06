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

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import de.badaix.snapcast.utils.NsdHelper
import de.badaix.snapcast.utils.NsdHelper.NsdHelperListener
import de.badaix.snapcast.utils.Settings
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Created by johannes on 21.02.16.
 */
@AndroidEntryPoint
class ServerDialogFragment : DialogFragment(), View.OnClickListener {
    @Inject
    lateinit var nsdHelper: NsdHelper

    @Inject
    lateinit var settings: Settings

    private var btnScan: Button? = null
    private var editHost: EditText? = null
    private var editStreamPort: EditText? = null
    private var editControlPort: EditText? = null
    private var checkBoxAutoStart: CheckBox? = null
    private var checkBoxResample: CheckBox? = null
    private var spinnerAudioEngine: Spinner? = null
    private var host: String? = ""
    private var streamPort = 1704
    private var controlPort = 1705
    private var autoStart = false
    private var listener: ServerDialogListener? = null

    fun setListener(listener: ServerDialogListener?) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.d(TAG, "onCreateDialog")
        val builder = AlertDialog.Builder(requireContext())
        // Get the layout inflater
        val inflater = requireActivity().layoutInflater
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        val view = inflater.inflate(R.layout.dialog_server, null)
        btnScan = view.findViewById<Button>(R.id.btn_scan)
        btnScan!!.setOnClickListener(this)

        editHost = view.findViewById<EditText>(R.id.host)
        editStreamPort = view.findViewById<EditText>(R.id.stream_port)
        editControlPort = view.findViewById<EditText>(R.id.control_port)
        checkBoxAutoStart = view.findViewById<CheckBox>(R.id.checkBoxAutoStart)

        spinnerAudioEngine = view.findViewById<Spinner>(R.id.audio_engine)
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.audio_engine_array, android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAudioEngine!!.setAdapter(adapter)
        checkBoxResample = view.findViewById<CheckBox>(R.id.checkBoxResample)

        update()

        builder.setView(view) // Add action buttons
            .setPositiveButton(android.R.string.ok) { dialog, id -> // sign in the user ...
                host = editHost!!.text.toString()
                try {
                    streamPort = editStreamPort!!.text.toString().toInt()
                    controlPort = editControlPort!!.text.toString().toInt()
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                }
                if (listener != null) {
                    listener!!.onHostChanged(host, streamPort, controlPort)
                    listener!!.onAutoStartChanged(checkBoxAutoStart!!.isChecked)
                }
                settings.setAudioEngine(
                    spinnerAudioEngine!!.selectedItem.toString(),
                    checkBoxResample!!.isChecked
                )
            }
            .setNegativeButton(android.R.string.cancel, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, id: Int) {
                    this@ServerDialogFragment.getDialog()!!.cancel()
                }
            })
            .setTitle(R.string.settings)
            .setCancelable(false)
        return builder.create()
    }

    override fun onClick(v: View?) {
        nsdHelper.startListening("_snapcast._tcp.", "Snapcast", object : NsdHelperListener {
            override fun onResolved(
                nsdHelper: NsdHelper?,
                serviceInfo: NsdServiceInfo?
            ) {
                if (serviceInfo == null) return
                Log.d(TAG, "onResolved: " + serviceInfo.host.getCanonicalHostName())
                setHost(
                    serviceInfo.host.getCanonicalHostName(),
                    serviceInfo.port,
                    serviceInfo.port + 1
                )
            }
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(TAG, "onAttach")

        if (context is Activity) {
            update()
        }
    }

    private fun update() {
        Log.d(TAG, "update")
        if (this.activity == null) return

        try {
            this.requireActivity().runOnUiThread {
                try {
                    editHost!!.setText(host)
                    editStreamPort!!.setText(streamPort.toString())
                    editControlPort!!.setText(controlPort.toString())
                    checkBoxAutoStart!!.setChecked(autoStart)
                    for (i in 0..<spinnerAudioEngine!!.count) {
                        if (spinnerAudioEngine!!.getItemAtPosition(i)
                                .toString() == settings.audioEngine
                        ) {
                            spinnerAudioEngine!!.setSelection(i)
                            break
                        }
                    }
                    checkBoxResample!!.setChecked(settings.doResample())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setHost(host: String?, streamPort: Int, controlPort: Int) {
        this.host = host
        this.streamPort = streamPort
        this.controlPort = controlPort
        update()
    }

    fun setAutoStart(autoStart: Boolean) {
        this.autoStart = autoStart
        update()
    }


    interface ServerDialogListener {
        fun onHostChanged(host: String?, streamPort: Int, controlPort: Int)

        fun onAutoStartChanged(autoStart: Boolean)
    }

    companion object {
        private const val TAG = "ServerDialog"
    }
}
