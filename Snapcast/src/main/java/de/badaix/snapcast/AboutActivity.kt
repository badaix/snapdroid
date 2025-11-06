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

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        try {
            supportActionBar!!.title = getString(R.string.about) + " Snapcast"
        } catch (e: Exception) {
            Snackbar.make(
                findViewById(R.id.webView),
                getText(R.string.action_bar_failed),
                Snackbar.LENGTH_LONG
            ).show()
        }
        val pInfo: PackageInfo
        try {
            pInfo = packageManager.getPackageInfo(packageName, 0)
            supportActionBar!!.subtitle = "v" + pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        val wv = findViewById<WebView>(R.id.webView)
        wv.loadUrl("file:///android_asset/" + this.getText(R.string.about_file))
    }
}
