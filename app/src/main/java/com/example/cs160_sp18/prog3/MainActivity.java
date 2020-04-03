package com.example.cs160_sp18.prog3;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private static final int REQUEST_CHECK_SETTINGS = 0x1;

    private RecyclerView recycle;
    private RecyclerView.Adapter adapter;
    private ArrayList<BearCard> cards = new ArrayList<BearCard>();
    private FusedLocationProviderClient fusedLocationClient;
    private Location location;
    private SettingsClient settings;
    private LocationRequest locationRequest;
    private LocationSettingsRequest settingsRequest;
    private LocationCallback callback;

    private RelativeLayout layout;
    private Toolbar toolbar;
    private ImageView refresh;

    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent usernameIntent = getIntent();
        Bundle intentExtras = usernameIntent.getExtras();
        if (intentExtras != null) {
            username = (String) intentExtras.get("username");
        } else {
            Intent userIntent = new Intent(this, LoginActivity.class);
            this.startActivity(userIntent);
        }
        setTitle("Landmarks");
        layout = (RelativeLayout) findViewById(R.id.card_layout);
        recycle = (RecyclerView) findViewById(R.id.bears);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        refresh = (ImageView) findViewById(R.id.refresh);
        setSupportActionBar(toolbar);
        refresh.setClickable(true);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateLocation();
            }
        });
        toolbar.setTitle("Landmarks");
        recycle.setHasFixedSize(true);
        recycle.setLayoutManager(new LinearLayoutManager(this));
        requestPermissions();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        settings = LocationServices.getSettingsClient(this);

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();
        startLocationUpdates();

        updateLocation();
    }

    private boolean checkPermissions() {
        int locPermissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int intPermissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
        return locPermissionState == PackageManager.PERMISSION_GRANTED && intPermissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        if (!checkPermissions()) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}, REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void createLocationCallback() {
        callback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                location = locationResult.getLastLocation();
                makeCards();
                setAdapter();
            }
        };
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        settingsRequest = builder.build();
    }

    private void startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        settings.checkLocationSettings(settingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");

                        //noinspection MissingPermission
                        fusedLocationClient.requestLocationUpdates(locationRequest,
                                callback, Looper.myLooper());
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void updateLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            setLocation(location);
                            makeCards();
                            setAdapter();
                        } else {
                            Log.i("Null Location", "First location returned null");
                        }
                    }
                });
    }

    private void setLocation(Location loc) {
        location = loc;
    }

    private void makeCards() {
        String file = loadJSON(this);
        cards = new ArrayList<BearCard>();
        try {
            JSONArray json = new JSONArray(file);
            for (int i = 0; i < json.length(); i++) {
                JSONObject landmark = json.getJSONObject(i);
                String name = landmark.getString("landmark_name");
                String loc = landmark.getString("coordinates");
                String filename = landmark.getString("filename");
                Uri imageURI = Uri.parse("android.resource://" + this.getPackageName() + "/drawable/" + filename);
                String[] locs = loc.split(", ");
                double latitude = Location.convert(locs[0]);
                double longitude = Location.convert(locs[1]);
                Location bearLoc = new Location("");
                bearLoc.setLatitude(latitude);
                bearLoc.setLongitude(longitude);
                BearCard card = new BearCard(name, Math.round(location.distanceTo(bearLoc)), imageURI);
                cards.add(card);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // From https://stackoverflow.com/questions/13814503/reading-a-json-file-in-android
    public String loadJSON(Context context) {
        String json = null;
        try {
            InputStream is = context.getAssets().open("bear_statues.json");

            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();

            json = new String(buffer, "UTF-8");


        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;

    }

    private void setAdapter() {
        adapter = new CardAdapter(this, cards, username);
        recycle.setAdapter(adapter);
    }

}
