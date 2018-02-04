package com.knafayim.shush;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.knafayim.shush.geofence.GeofenceService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener, GoogleApiClient.ConnectionCallbacks,
        LocationListener, GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnCameraChangeListener {

    private GoogleMap mMap;
    private SQLiteDatabase userLocations;
    boolean locationExists;
    private ArrayList<String> locationAddressArrayList;
    private String addressString;
    private ArrayList<String> locationNameArrayList;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private boolean zoom = true;
    private Location currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.abs_action_bar);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        userLocations = this.openOrCreateDatabase("Locations", MODE_PRIVATE, null);
        locationNameArrayList = new ArrayList<>();
        locationAddressArrayList = new ArrayList<>();
        buildGoogleApiClient();
        mGoogleApiClient.connect();
        Button useCurrentButton = (Button) findViewById(R.id.get_current);
        useCurrentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String link = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" +
                        currentLocation.getLatitude() + "," +
                        currentLocation.getLongitude() + "&key=AIzaSyAzRGYfdXmofJ37yZ1mvAAPRY2TvVNcc60";
                GetAddressDownloadTask getLocation = new GetAddressDownloadTask();
                getLocation.execute(link);
            }
        });
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnCameraChangeListener(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
        Cursor c = userLocations.rawQuery("SELECT * FROM locations", null);
        int nameIndex;
        c.moveToFirst();
        if(c.getCount() > 0) {
            try {
                while (c != null) {
                    nameIndex = c.getColumnIndex("name");
                    String name = c.getString(nameIndex);
                    int latitude = c.getColumnIndex("latitude");
                    float lat  = c.getFloat(latitude);
                    int longitude = c.getColumnIndex("longitude");
                    float lng  = c.getFloat(longitude);
                    LatLng location = new LatLng(lat, lng);
                    drawMarkerWithCircle(location, name);
                    c.moveToNext();
                }
            } catch( Exception e){

            }
        }
        c.close();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stopLocationUpdate();
    }

    private void drawMarkerWithCircle(LatLng position, String name){
        double radiusInMeters = AppGlobals.getFenceRadius()/3.2808;
        int strokeColor = Color.parseColor("#4890c8");
        int shadeColor = Color.parseColor("#334890c8");
        CircleOptions circleOptions = new CircleOptions().center(position).radius(radiusInMeters).fillColor(shadeColor).strokeColor(strokeColor).strokeWidth(8);
        Circle mCircle = mMap.addCircle(circleOptions);
        MarkerOptions markerOptions = new MarkerOptions().position(position).title(name);
        Marker mMarker = mMap.addMarker(markerOptions);
        mCircle.setCenter(position);
        mMarker.setPosition(position);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        try {
            String link = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + latLng.latitude + "," +
                    latLng.longitude + "&key=AIzaSyAzRGYfdXmofJ37yZ1mvAAPRY2TvVNcc60";
            GetAddressDownloadTask getLocation = new GetAddressDownloadTask();
            getLocation.execute(link);
        } catch(Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Location unknown", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    public void startLocationUpdates() {
        long INTERVAL = 0;
        long FASTEST_INTERVAL = 0;
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (zoom) {
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),
                    location.getLongitude()), 14);
            mMap.animateCamera(cameraUpdate);
        }
        currentLocation = location;
//        Log.d("TAG", "Location changed called" + "Lat " + location.getLatitude() + ", Lng "+ location.getLongitude());
    }

    private void stopLocationUpdate() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        Log.i("TAG", "camera "+ cameraPosition.zoom);
        if (cameraPosition.zoom > 14) {
            zoom = false;
        } else if (cameraPosition.zoom < 3) {
            zoom = true;
        } else {
            zoom = false;
        }
    }

    @SuppressLint("StaticFieldLeak")
    public class GetAddressDownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection;
            try {
                url = new URL(strings[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream is = urlConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(is);

                int data = inputStreamReader.read();
                while(data != -1){
                    char curr = (char) data;
                    result += curr;
                    data = inputStreamReader.read();
                }
                return result;

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            try {
                locationExists = false;
                Cursor c = userLocations.rawQuery("SELECT * FROM locations", null);
                int addressIndex;
                locationAddressArrayList.clear();

                c.moveToFirst();
                if(c.getCount() > 0) {
                    try {
                        while (c != null) {
                            addressIndex = c.getColumnIndex("address");
                            String address = c.getString(addressIndex);
                            locationAddressArrayList.add(address);
                            c.moveToNext();
                        }
                    } catch( Exception e){

                    }
                }
                c.close();


                JSONObject locationObject = new JSONObject(result);
                JSONArray jsonArray = locationObject.getJSONArray("results");
                JSONObject geometryArray = jsonArray.getJSONObject(0).getJSONObject("geometry");
                addressString = jsonArray.getJSONObject(0).getString("formatted_address");

                if(addressString.indexOf(',') != -1){
                    addressString = addressString.substring(0, addressString.indexOf(','));
                }

                for(String address: locationAddressArrayList){
                    if(address.indexOf(',') != -1){
                        address = address.substring(0, address.indexOf(','));
                    }
                    if(address.equals(addressString)){
                        locationExists = true;
                    }
                }
                final JSONObject locationGeo = geometryArray.getJSONObject("location");

                if(!locationExists) {
                    new AlertDialog.Builder(MapsActivity.this)
                            .setTitle("New location")
                            .setMessage("Do you want to use this location: " + addressString)
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();

                                }
                            })
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    locationExists = false;
                                    for (String name : locationNameArrayList) {
                                        if (name.equals(addressString)) {
                                            locationExists = true;
                                        }
                                    }
                                    if (!locationExists) {
                                        userLocations = getApplicationContext().openOrCreateDatabase("Locations", MODE_PRIVATE, null);
                                        try {
                                            userLocations.execSQL("INSERT INTO locations (name, address, latitude, longitude) " +
                                                    "VALUES ('" + addressString + "', '" + addressString + "', " +
                                                    locationGeo.get("lat") + ", " + locationGeo.get("lng") + ")");
                                            drawMarkerWithCircle(new LatLng(Double.parseDouble(
                                                    String.valueOf(locationGeo.get("lat"))),
                                                    Double.parseDouble(String.valueOf(locationGeo.get("lng")))), addressString);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        locationNameArrayList.add(addressString);
                                        Collections.sort(locationNameArrayList, String.CASE_INSENSITIVE_ORDER);

//                                        newLocationEditText.setText("");
//                                        adapter.notifyDataSetChanged();
                                        stopService(new Intent(getApplicationContext(), GeofenceService.class));
                                        new android.os.Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                             if (AppGlobals.isServiceActive()) {
                                                 startService(new Intent(getApplicationContext(), GeofenceService.class));
                                             }
                                            }
                                        }, 1000);
                                    } else {
                                        Toast.makeText(getApplicationContext(), "Location already exists", Toast.LENGTH_LONG).show();
                                    }
                                }
                            })
                            .show();
                } else{
                    Toast.makeText(getApplicationContext(), "Location already exists", Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
