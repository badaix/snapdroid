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

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import de.badaix.snapcast.GroupItem.GroupItemListener
import de.badaix.snapcast.control.json.Group
import de.badaix.snapcast.control.json.ServerStatus
import de.badaix.snapcast.utils.Settings
import javax.inject.Inject

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [GroupItem.GroupItemListener] interface
 * to handle interaction events.
 */
@AndroidEntryPoint
class GroupListFragment : Fragment() {
    @Inject
    lateinit var settings: Settings

    private var groupItemListener: GroupItemListener? = null
    private var groupAdapter: GroupAdapter? = null
    private var serverStatus: ServerStatus? = null
    private var hideOffline = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        Log.d(TAG, "onCreateView: " + this)
        val view = inflater.inflate(R.layout.fragment_group_list, container, false)
        val lvGroup = view.findViewById<ListView>(R.id.lvGroup)
        groupAdapter = GroupAdapter(requireContext(), groupItemListener)
        groupAdapter!!.setHideOffline(hideOffline)
        groupAdapter!!.updateServer(serverStatus)
        lvGroup.setAdapter(groupAdapter)
        updateGui()
        return view
    }


    private fun updateGui() {
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is GroupItemListener) {
            groupItemListener = context as GroupItemListener
        } else {
            throw RuntimeException(
                context
                    .toString() + " must implement GroupItemListener"
            )
        }
        updateGui()
    }

    override fun onDetach() {
        super.onDetach()
        groupItemListener = null
    }

    fun updateServer(serverStatus: ServerStatus?) {
        this.serverStatus = serverStatus
        if (groupAdapter != null) groupAdapter!!.updateServer(serverStatus)
    }

    fun setHideOffline(hide: Boolean) {
        this.hideOffline = hide
        if (groupAdapter != null) groupAdapter!!.setHideOffline(hideOffline)
    }

    inner class GroupAdapter internal constructor(
        private val context: Context,
        private val listener: GroupItemListener?
    ) : ArrayAdapter<Group>(
        context, 0
    ) {
        private var hideOffline = false
        private var serverStatus = ServerStatus()


        override fun getView(
            position: Int, convertView: View?,
            parent: ViewGroup
        ): View {
            val group = getItem(position)!!
            val groupItem: GroupItem

            if (convertView != null) {
                groupItem = convertView as GroupItem
                groupItem.setGroup(group)
            } else {
                groupItem = GroupItem(context, serverStatus, group)
            }
            groupItem.setHideOffline(hideOffline)
            groupItem.setListener(listener)
            return groupItem
        }

        fun updateServer(serverStatus: ServerStatus?) {
            if (serverStatus != null) {
                this@GroupAdapter.serverStatus = serverStatus
                update()
            }
        }


        fun update() {
            val activity = getActivity()
            if (activity == null) return
            activity.runOnUiThread {
                clear()
                for (group in this@GroupAdapter.serverStatus.groups) {
                    if (group.clients.isEmpty()) continue

                    var onlineCount = 0
                    var count = 0
                    for (client in group.clients) {
                        if (client.isDeleted) continue
                        if (client.isConnected)  // && client.config!!.stream.equals(GroupListFragment.this.stream.id))
                            onlineCount++
                        count++
                    }

                    if ((onlineCount > 0) || (!hideOffline && (count > 0))) add(group)
                }

                if (getActivity() != null) notifyDataSetChanged()
            }
        }

        fun setHideOffline(hideOffline: Boolean) {
            if (this.hideOffline == hideOffline) return
            this.hideOffline = hideOffline
            update()
        }
    }


    companion object {
        private const val TAG = "GroupList"
    }
}
