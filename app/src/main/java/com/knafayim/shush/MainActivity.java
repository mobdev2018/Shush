package com.knafayim.shush;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.knafayim.shush.geofence.GeofenceService;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    private SwitchCompat fenceSwitch;
    private SQLiteDatabase userLocations;
    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.abs_layout);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && !notificationManager.isNotificationPolicyAccessGranted()) {

            Intent intent = new Intent(
                    android.provider.Settings
                            .ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
        }
        fenceSwitch = (SwitchCompat) findViewById(R.id.service_switch);
        userLocations = this.openOrCreateDatabase("Locations", MODE_PRIVATE, null);
        userLocations.execSQL("CREATE TABLE IF NOT EXISTS locations (name VARCHAR, address VARCHAR, latitude FLOAT, longitude FLOAT)");
        fenceSwitch.setChecked(AppGlobals.isServiceActive());
        fenceSwitch.setOnCheckedChangeListener(this);
        if (AppGlobals.isServiceActive()) {
            if (locationEnabled()) {
                Cursor c = userLocations.rawQuery("SELECT * FROM locations", null);
                if (c.getCount() > 0 && !permissionRequired()) {
                    fenceSwitch.setText("App Enabled");
                    fenceSwitch.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    startService(new Intent(getApplicationContext(), GeofenceService.class));
                }
            } else {
                dialogForLocationEnableManually(this);
            }
        } else {
            fenceSwitch.setText("App Disabled");
            fenceSwitch.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
        permissionRequired();
    }

    public static boolean locationEnabled() {
        LocationManager lm = (LocationManager) AppGlobals.getContext()
                .getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }
        return gps_enabled || network_enabled;
    }

    public static void dialogForLocationEnableManually(final Activity activity) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
        dialog.setMessage("Location is not enabled");
        dialog.setPositiveButton("Turn on", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                // TODO Auto-generated method stub
                Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                activity.startActivityForResult(myIntent, AppGlobals.LOCATION_ENABLE);
            }
        });
        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                // TODO Auto-generated method stub
                paramDialogInterface.dismiss();

            }
        });
        dialog.show();
    }

    private boolean permissionRequired() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //ask for permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 0 || resultCode != RESULT_OK) {
            Toast.makeText(getApplicationContext(), "Please allow this app to change 'do not disturb' settings",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "permission granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void goToLocationActivity(View view) {
        Intent locationIntent = new Intent(MainActivity.this, LocationActivity.class);
        startActivity(locationIntent);
    }

    public void goToVolumeSettings(View view) {
        Intent volumeIntent = new Intent(MainActivity.this, VolumeSettingsActivity.class);
        startActivity(volumeIntent);
    }

    public void goToAbout(View view) {
        Intent aboutIntent = new Intent(MainActivity.this, AboutActivity.class);
        startActivity(aboutIntent);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (b) {
            Cursor c = userLocations.rawQuery("SELECT * FROM locations", null);
            if (c.getCount() > 0 && !permissionRequired()) {
                if (locationEnabled()) {
                    AppGlobals.setServiceState(true);
                    fenceSwitch.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    startService(new Intent(getApplicationContext(), GeofenceService.class));
                    fenceSwitch.setText("App Enabled");
                } else {
                    dialogForLocationEnableManually(this);
                    fenceSwitch.setChecked(false);
                    fenceSwitch.setTextColor(getResources().getColor(android.R.color.darker_gray));

                }
            } else {
                fenceSwitch.setChecked(false);
                fenceSwitch.setText("App Disabled");
                fenceSwitch.setTextColor(getResources().getColor(android.R.color.darker_gray));
                Toast.makeText(this, "please add location", Toast.LENGTH_SHORT).show();
            }
        } else {
            fenceSwitch.setTextColor(getResources().getColor(android.R.color.darker_gray));
            stopService(new Intent(getApplicationContext(), GeofenceService.class));
            AppGlobals.setServiceState(false);
            fenceSwitch.setText("App Disabled");
            NotificationManager notificationManager = (NotificationManager)
                    getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(1001);
            if (AppGlobals.getExitMode() == 0 && AppGlobals.isModeChangedByUs()) {
                    audioManager.setRingerMode(AppGlobals.getPreviousRingerState());
            }
        }
    }
}
