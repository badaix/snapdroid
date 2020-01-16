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

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class AboutActivity extends AppCompatActivity {


    // Calculate the % of scroll progress in the actual web page content
    private float calculateProgression(WebView content) {
        float positionTopView = content.getTop();
        float contentHeight = content.getContentHeight();
        float currentScrollPosition = content.getScrollY();
        return (currentScrollPosition - positionTopView) / contentHeight;
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        WebView wv = findViewById(R.id.webView);
        OrientationChangeData objectToSave = new OrientationChangeData();
        objectToSave.mProgress = calculateProgression(wv);
        return objectToSave;
    }

    // Container class used to save data during the orientation change
    private final static class OrientationChangeData {
        private float mProgress;
    }

    private boolean mHasToRestoreState = false;
    private float mProgressToRestore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_about);
        } catch (Exception e) {

            Snackbar.make(findViewById(R.id.webView), this.getText(R.string.action_about_failed), Snackbar.LENGTH_LONG).show();
        }

        try {
            getSupportActionBar().setTitle(getString(R.string.about) + " Snapcast");
        }catch (NullPointerException e) {
            Snackbar.make(findViewById(R.id.webView), this.getText(R.string.action_nullpointer), Snackbar.LENGTH_LONG).show();
        }
        PackageInfo pInfo;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            try {
                getSupportActionBar().setSubtitle("v" + pInfo.versionName);
            }catch (NullPointerException e) {
                Snackbar.make(findViewById(R.id.webView), this.getText(R.string.action_nullpointer), Snackbar.LENGTH_LONG).show();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        WebView wv = findViewById(R.id.webView);
        wv.setWebViewClient(new MyWebViewClient());
        wv.loadUrl("file:///android_asset/" + this.getText(R.string.about_file));
        OrientationChangeData data = (OrientationChangeData) getLastCustomNonConfigurationInstance();
        if (data != null) {
            mHasToRestoreState = true;
            mProgressToRestore = data.mProgress;
        }
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            if (mHasToRestoreState) {
                mHasToRestoreState = false;
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        WebView wv = findViewById(R.id.webView);
                        float webviewsize = wv.getContentHeight() - wv.getTop();
                        if (webviewsize == 0){ wv.postDelayed(this, 10); return; }
                        float positionInWV = webviewsize * mProgressToRestore;
                        int positionY = Math.round(wv.getTop() + positionInWV);
                        wv.scrollTo(0, positionY);
                    }
                    // Delay the scrollTo to make it work
                });
            }
            super.onPageFinished(view, url);
        }
    }

}
