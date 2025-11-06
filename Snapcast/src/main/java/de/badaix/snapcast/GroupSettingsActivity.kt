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

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

/**
 * Created by johannes on 06.12.16.
 */
class GroupSettingsActivity : AppCompatActivity() {
    private var groupSettingsFragment: GroupSettingsFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        groupSettingsFragment = GroupSettingsFragment()
        groupSettingsFragment!!.setArguments(getIntent().getExtras())
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
            .replace(android.R.id.content, groupSettingsFragment).commit()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent()
                intent.putExtra("group", groupSettingsFragment!!.group!!.id)
                if (groupSettingsFragment!!.didStreamChange()) intent.putExtra(
                    "stream",
                    groupSettingsFragment!!.stream
                )
                if (groupSettingsFragment!!.didClientsChange()) intent.putStringArrayListExtra(
                    "clients",
                    groupSettingsFragment!!.clients
                )
                setResult(RESULT_OK, intent)
                finish()
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed()
            return true
        }
        return false
    }
}

