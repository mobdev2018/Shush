package com.knafayim.shush;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.knafayim.shush.geofence.GeofenceService;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by s9iper1 on 12/20/17.
 */

public class BootStarter extends BroadcastReceiver {

    private SQLiteDatabase userLocations;

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        userLocations = context.openOrCreateDatabase("Locations", MODE_PRIVATE, null);
        if (AppGlobals.isServiceActive() && AppGlobals.shouldStartAfterBoot()) {
            Cursor c = userLocations.rawQuery("SELECT * FROM locations", null);
            if (c.getCount() > 0) {
                context.startService(new Intent(context, GeofenceService.class));
            }

        }
    }
}
