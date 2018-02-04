package com.knafayim.shush.geofence;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.knafayim.shush.AppGlobals;
import com.knafayim.shush.MainActivity;
import com.knafayim.shush.R;
import com.knafayim.shush.ReceiveTransitionsIntentService;

import java.util.ArrayList;


public class GeofenceService extends Service implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status>,
        LocationListener {

    protected ArrayList<Geofence> mGeofenceList;
    SharedPreferences mSharedPreferences;
    GoogleApiClient mGoogleApiClient;
    private PendingIntent mGeofencePendingIntent;
    private SQLiteDatabase userLocations;
    private LocationRequest mLocationRequest;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mGeofenceList = new ArrayList<>();
        mGeofencePendingIntent = null;
        userLocations = this.openOrCreateDatabase("Locations", MODE_PRIVATE, null);
        buildGoogleApiClient();
        notificationManager =(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (AppGlobals.isModeChangedByUs()) {
            AppGlobals.ringerModeChangedByUs(true);
        } else {
            AppGlobals.ringerModeChangedByUs(false);
        }
        populateGeofenceList();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.shush_icon)
                .setContentTitle("Shush app enabled.")
                .setContentIntent(pendingIntent).build();
        startForeground(121212, notification);
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mGoogleApiClient.connect();
            }
        }, 2000);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, "GoogleApiClient Not Connected", Toast.LENGTH_SHORT).show();
        } else {
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    getGeofencePendingIntent()
            ).setResultCallback(this);
        }
        mGoogleApiClient.disconnect();
        notificationManager.cancel(1001);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i("API", "Connected");
        long INTERVAL = 0;
        long FASTEST_INTERVAL = 0;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.GeofencingApi.addGeofences(
                mGoogleApiClient,
                getGeofencingRequest(),
                getGeofencePendingIntent()
        ).setResultCallback(this);
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onResult(Status status) {
        Log.i("GeoFence", "onResult status" + status);
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, ReceiveTransitionsIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void populateGeofenceList() {
        Cursor c = userLocations.rawQuery("SELECT * FROM locations", null);

        Log.i("TAG", "count  " + c.getCount());
            try {
                float radius =  (float) (AppGlobals.getFenceRadius()/3.2808);
                Log.i("TAG", "radius " + AppGlobals.getFenceRadius());
                Log.i("TAG", "radius " + radius);
                while (c.moveToNext()) {
                    int nameIndex = c.getColumnIndex("name");
                    String name = c.getString(nameIndex);
                    int latitude = c.getColumnIndex("latitude");
                    float lat  = c.getFloat(latitude);
                    int longitude = c.getColumnIndex("longitude");
                    float lng  = c.getFloat(longitude);
                    mGeofenceList.add(new Geofence.Builder()
                            .setRequestId(name)
                            .setCircularRegion(
                                    lat,
                                    lng, radius

                            )
                            .setExpirationDuration(Geofence.NEVER_EXPIRE)
                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER
                            | Geofence.GEOFENCE_TRANSITION_EXIT)
                            .build());
                    Log.i("TAG", "latlng "+ lat +"," + lng);
                }
            } catch( Exception e){

            }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onLocationChanged(Location location) {
    }
}