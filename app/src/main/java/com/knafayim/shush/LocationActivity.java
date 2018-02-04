package com.knafayim.shush;

import android.Manifest;
import android.app.ActionBar;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.knafayim.shush.geofence.GeofenceService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;

public class LocationActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        com.google.android.gms.location.LocationListener, GoogleApiClient.OnConnectionFailedListener {

    ArrayAdapter adapter;
    ArrayList<String> locationNameArrayList;
    ListView locationListView;
    EditText newLocationEditText;
    SQLiteDatabase userLocations;
    boolean locationFlag;
    boolean locationExists;
    String locationString;
    ArrayList<String> locationAddressArrayList;
    String addressString;
    private Button useMap;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private int counter = 0;
    private Button getCurrentLocation;
    private AlertDialog.Builder alertDialog;


    @Override
    protected void onResume() {
        super.onResume();
        locationFlag = false;
        locationNameArrayList = new ArrayList<>();
        adapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, locationNameArrayList);
        locationAddressArrayList = new ArrayList<>();

        userLocations = this.openOrCreateDatabase("Locations", MODE_PRIVATE, null);
        userLocations.execSQL("CREATE TABLE IF NOT EXISTS locations (name VARCHAR, address VARCHAR, latitude FLOAT, longitude FLOAT)");

        Cursor c = userLocations.rawQuery("SELECT * FROM locations", null);
        int nameIndex;

        c.moveToFirst();
        if (c.getCount() > 0) {
            try {
                while (c != null) {
                    nameIndex = c.getColumnIndex("name");
                    String name = c.getString(nameIndex);
                    locationNameArrayList.add(name);
                    c.moveToNext();
                }
            } catch (Exception e) {

            }
        }
        c.close();

        if (locationNameArrayList.isEmpty()) {
            locationNameArrayList.add("You haven't added any locations yet");
        }

        Collections.sort(locationNameArrayList, String.CASE_INSENSITIVE_ORDER);

        locationListView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.abs_layout);

        locationListView = (ListView) findViewById(R.id.locationListView);
        useMap = (Button) findViewById(R.id.user_map);
        useMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), MapsActivity.class));
            }
        });
        getCurrentLocation = (Button) findViewById(R.id.useCurrentLocationButton);
        getCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getCurrentLocation();
            }
        });
        newLocationEditText = (EditText) findViewById(R.id.addNewLocationEditText);
        locationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                Log.i("TAG", "click ");
                final String currName = locationNameArrayList.get(position);
                if (!locationNameArrayList.get(0).equals("You haven't added any locations yet")) {
                    alertDialog = new AlertDialog.Builder(LocationActivity.this);
                    alertDialog.setTitle("Edit location");
                    alertDialog.setMessage("To rename location, type location name and press save.\n\n" + "Clicking remove will delete location.\n");

                    final EditText input = new EditText(LocationActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT |
                            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                    StringBuilder builder = new StringBuilder();
                    String[] strArray = currName.split(" ");
                    for (String s : strArray) {
                        String cap = s.substring(0, 1).toUpperCase() + s.substring(1);
                        builder.append(cap + " ");
                    }
                    input.setText(builder.toString());
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT);
                    input.setLayoutParams(lp);
                    input.selectAll();
                    alertDialog.setView(input);
                    alertDialog.setPositiveButton("Save", null);
                    alertDialog.setNegativeButton("Remove", null);

                    final AlertDialog alert = alertDialog.create();
                    alert.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialogInterface) {
                            Button button = ((AlertDialog) alert)
                                    .getButton(AlertDialog.BUTTON_POSITIVE);

                            button.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (!input.getText().toString().equals("")) {
                                        userLocations = getApplicationContext().openOrCreateDatabase("Locations", MODE_PRIVATE, null);
                                        Log.i("TAG", " list name " + locationNameArrayList.get(position));
                                        Log.i("TAG", " changed name " + input.getText().toString());
                                        if (!locationNameArrayList.get(position).equals(input.getText().toString())) {
                                            if (!locationNameArrayList.contains(input.getText().toString())) {
                                                String query = "UPDATE locations SET name= ?"
                                                        + "  WHERE name = ?";
                                                userLocations.execSQL(query
                                                        , new String[]{input.getText().toString(), currName});
                                                locationNameArrayList.set(position, input.getText().toString());
                                                Collections.sort(locationNameArrayList, String.CASE_INSENSITIVE_ORDER);
                                                adapter.notifyDataSetChanged();
                                                getWindow().setSoftInputMode(
                                                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                                                );
                                                alert.dismiss();
                                            } else {
                                                Toast.makeText(getApplicationContext(), "Name already exist", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    } else {
                                        Toast.makeText(getApplicationContext(), "Name can't be empty", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });

                            Button negButton = ((AlertDialog) alert).getButton(AlertDialog.BUTTON_NEGATIVE);
                            negButton.setOnClickListener(new View.OnClickListener() {

                                @Override
                                public void onClick(View view) {
                                    // TODO Do something
                                    alert.dismiss();
                                    userLocations = getApplicationContext().openOrCreateDatabase("Locations", MODE_PRIVATE, null);
                                    userLocations.execSQL("DELETE FROM locations WHERE name = '" + currName.replace("\'", "''") + "'");
                                    locationNameArrayList.remove(position);
                                    if (locationNameArrayList.isEmpty()) {
                                        locationNameArrayList.add("You haven't added any locations yet");
                                    }
                                    adapter.notifyDataSetChanged();
                                    getWindow().setSoftInputMode(
                                            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                                    );
                                    if (AppGlobals.getExitMode() == 0 && AppGlobals.isModeChangedByUs()) {
                                        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                                        audioManager.setRingerMode(AppGlobals.getPreviousRingerState());
                                    }
                                    stopService(new Intent(getApplicationContext(), GeofenceService.class));
                                    new android.os.Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (AppGlobals.isServiceActive()) {
                                                Cursor c = userLocations.rawQuery("SELECT * FROM locations", null);
                                                int nameIndex;
                                                c.moveToFirst();
                                                if (c.getCount() > 0) {
                                                    startService(new Intent(getApplicationContext(), GeofenceService.class));
                                                } else {
                                                    AppGlobals.setServiceState(false);
                                                }
                                            }
                                        }
                                    }, 1000);

                                }
                            });

                        }
                    });
                    alert.getWindow().setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    alert.show();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        userLocations.close();
    }

    public void addNewLocation(View view) {
        String address = newLocationEditText.getText().toString();
        if (!address.equals("")) {
            getLocationFromAddress();
        } else {
            Toast.makeText(getApplicationContext(), "Please specify new address", Toast.LENGTH_LONG).show();
        }
    }

    private void getCurrentLocation() {
        buildGoogleApiClient();
        mGoogleApiClient.connect();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
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
        Log.d("TAG", "Location changed called" + "Lat " + location.getLatitude() + ", Lng " + location.getLongitude());
        if (counter >= 1) {
            useCurrentLocation(location);
            stopLocationUpdate();
            counter = 0;
        }
        counter++;
    }

    private void stopLocationUpdate() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public void useCurrentLocation(Location location) {
        try {
            String link = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + location.getLatitude() + "," +
                    location.getLongitude() + "&key=AIzaSyAzRGYfdXmofJ37yZ1mvAAPRY2TvVNcc60";
            GetAddressDownloadTask getLocation = new GetAddressDownloadTask();
            getLocation.execute(link);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Location unknown", Toast.LENGTH_LONG).show();
        }
    }

    public void getLocationFromAddress() {
        try {
            try {
                locationString = URLEncoder.encode(newLocationEditText.getText().toString(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            if (locationString != "") {
                locationExists = false;
                userLocations = this.openOrCreateDatabase("Locations", MODE_PRIVATE, null);
                userLocations.execSQL("CREATE TABLE IF NOT EXISTS locations (name VARCHAR, address VARCHAR, latitude FLOAT, longitude FLOAT)");


                for (String name : locationNameArrayList) {
                    if (name.equals(locationString)) {
                        locationExists = true;
                    }
                }
                if (!locationExists) {
                    String link = "https://maps.googleapis.com/maps/api/geocode/json?address=" + locationString + "&key=AIzaSyAzRGYfdXmofJ37yZ1mvAAPRY2TvVNcc60";

                    GetLocationDownloadTask getLocation = new GetLocationDownloadTask();
                    getLocation.execute(link);
                } else {
                    Toast.makeText(getApplicationContext(), "Location already exists", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "Type in the city!", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Location unknown", Toast.LENGTH_LONG).show();
        }
    }

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
                while (data != -1) {
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
                if (c.getCount() > 0) {
                    try {
                        while (c != null) {
                            addressIndex = c.getColumnIndex("address");
                            String address = c.getString(addressIndex);
                            locationAddressArrayList.add(address);
                            c.moveToNext();
                        }
                    } catch (Exception e) {

                    }
                }

                c.close();


                JSONObject locationObject = new JSONObject(result);
                JSONArray jsonArray = locationObject.getJSONArray("results");
                JSONObject geometryArray = jsonArray.getJSONObject(0).getJSONObject("geometry");
                addressString = jsonArray.getJSONObject(0).getString("formatted_address");

                if (addressString.indexOf(',') != -1) {
                    addressString = addressString.substring(0, addressString.indexOf(','));
                }

                for (String address : locationAddressArrayList) {
                    if (address.indexOf(',') != -1) {
                        address = address.substring(0, address.indexOf(','));
                    }
                    if (address.equals(addressString)) {
                        locationExists = true;
                    }
                }
                final JSONObject locationGeo = geometryArray.getJSONObject("location");

                if (!locationExists) {
                    new AlertDialog.Builder(LocationActivity.this)
                            .setTitle("New location")
                            .setMessage("Do you want to use this location: " + addressString)
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

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
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        if (locationNameArrayList.get(0).equals("You haven't added any locations yet")) {
                                            locationNameArrayList.remove(0);
                                        }
                                        locationNameArrayList.add(addressString);
                                        Collections.sort(locationNameArrayList, String.CASE_INSENSITIVE_ORDER);
                                        newLocationEditText.setText("");
                                        adapter.notifyDataSetChanged();
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
                } else {
                    Toast.makeText(getApplicationContext(), "Location already exists", Toast.LENGTH_LONG).show();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
    }

    public class GetLocationDownloadTask extends AsyncTask<String, Void, String> {

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
                while (data != -1) {
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

            if (result != null) {
                try {
                    locationExists = false;
                    Cursor c = userLocations.rawQuery("SELECT * FROM locations", null);
                    int addressIndex;

                    locationAddressArrayList.clear();

                    c.moveToFirst();
                    if (c.getCount() > 0) {
                        try {
                            while (c != null) {
                                addressIndex = c.getColumnIndex("address");
                                String address = c.getString(addressIndex);
                                locationAddressArrayList.add(address);
                                c.moveToNext();
                            }
                        } catch (Exception e) {

                        }
                    }
                    c.close();
                    JSONObject locationObject = new JSONObject(result);
                    JSONArray jsonArray = locationObject.getJSONArray("results");
                    JSONObject geometryArray = jsonArray.getJSONObject(0).getJSONObject("geometry");
                    addressString = jsonArray.getJSONObject(0).getString("formatted_address");
                    final JSONObject locationGeo = geometryArray.getJSONObject("location");

                    if (addressString.indexOf(',') != -1) {
                        addressString = addressString.substring(0, addressString.indexOf(','));
                    }

                    for (String address : locationAddressArrayList) {
                        if (address.indexOf(',') != -1) {
                            address = address.substring(0, address.indexOf(','));
                        }
                        if (address.equals(addressString)) {
                            locationExists = true;
                        }
                    }

                    if (!locationExists) {
                        new AlertDialog.Builder(LocationActivity.this)
                                .setTitle("New location")
                                .setMessage("Do you want to use this location: " + addressString)
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

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
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            if (locationNameArrayList.get(0).equals("You haven't added any locations yet")) {
                                                locationNameArrayList.remove(0);
                                            }
                                            locationNameArrayList.add(addressString);
                                            Collections.sort(locationNameArrayList, String.CASE_INSENSITIVE_ORDER);
                                            newLocationEditText.setText("");
                                            adapter.notifyDataSetChanged();
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
                    } else {
                        Toast.makeText(getApplicationContext(), "Location already exists", Toast.LENGTH_LONG).show();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}
