package de.badaix.snapcast;

import android.app.PendingIntent;
import android.annotation.SuppressLint;
import androidx.annotation.RequiresApi;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.content.SharedPreferences;
import de.badaix.snapcast.utils.Settings;

/** The tile observes process state; it never stores an optimistic on/off value. */
@RequiresApi(24)
public class SnapcastTileService extends TileService {
    private final SharedPreferences.OnSharedPreferenceChangeListener changes =
            (prefs, key) -> { if ("playerRunning".equals(key)) update(); };

    @Override public void onStartListening() {
        Settings.getInstance(this).getPrefs().registerOnSharedPreferenceChangeListener(changes);
        update();
    }

    @Override public void onStopListening() {
        Settings.getInstance(this).getPrefs().unregisterOnSharedPreferenceChangeListener(changes);
    }

    private void update() {
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setLabel("Snapcast");
        tile.setState(SnapclientService.isClientRunning() ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    @SuppressLint("StartActivityAndCollapseDeprecated") // Intent overload is required below API 34.
    @Override public void onClick() {
        if (SnapclientService.isClientRunning()) {
            startService(new Intent(this, SnapclientService.class).setAction(SnapclientService.ACTION_STOP));
        } else {
            Settings settings = Settings.getInstance(this);
            if (settings.getHost().isEmpty()) {
                Intent activity = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (Build.VERSION.SDK_INT >= 34) {
                    startActivityAndCollapse(PendingIntent.getActivity(this, 0, activity, PendingIntent.FLAG_IMMUTABLE));
                } else {
                    startActivityAndCollapse(activity);
                }
                return;
            }
            Intent start = new Intent(this, SnapclientService.class)
                    .setAction(SnapclientService.ACTION_START)
                    .putExtra(SnapclientService.EXTRA_HOST, settings.getHost())
                    .putExtra(SnapclientService.EXTRA_PORT, settings.getStreamPort());
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(start);
            else startService(start);
        }
    }
}
