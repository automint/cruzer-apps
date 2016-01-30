package com.socketmint.cruzer.maps;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.*;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.socketmint.cruzer.CruzerApp;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.crud.CrudChoices;
import com.socketmint.cruzer.crud.retrieve.Retrieve;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.dataholder.User;
import com.socketmint.cruzer.dataholder.Workshop;
import com.socketmint.cruzer.drawer.DrawerFragment;
import com.socketmint.cruzer.main.ViewHistory;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.LocData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class WorkshopLocator extends FragmentActivity implements OnMapReadyCallback, LocationListener, GoogleMap.OnInfoWindowClickListener {
    private static final String TAG = "WorkshopLocator";

    private GoogleMap mMap;
    private DatabaseHelper databaseHelper;
    private Tracker analyticsTracker;

    private List<Workshop> workshops = new ArrayList<>();
    private LocData locData = new LocData();
    private RequestQueue requestQueue;

    private boolean reset = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workshop_locator);

        analyticsTracker = ((CruzerApp) getApplication()).getAnalyticsTracker();
        databaseHelper = new DatabaseHelper(getApplicationContext());
        requestQueue = Volley.newRequestQueue(this);
        locData.cruzerInstance(WorkshopLocator.this);

        DrawerFragment drawerFragment = (DrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        drawerFragment.setUp(R.id.navigation_drawer, (Toolbar) findViewById(R.id.toolbar), (DrawerLayout) findViewById(R.id.drawer_layout));

        fetchWorkshops();
    }

    private void initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        analyticsTracker.setScreenName(TAG);
        analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());
        Log.d(TAG, "analytics screen sent");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMap != null && reset) {
            currentLocation();
            reset = false;
        }
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

        if ((checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, android.os.Process.myPid(), android.os.Process.myUid()) != PackageManager.PERMISSION_GRANTED)
                && (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, android.os.Process.myPid(), android.os.Process.myUid()) != PackageManager.PERMISSION_GRANTED)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, Constants.RequestCodes.PERMISSION_MAPS_CURRENT_LOCATION);
                return;
            }
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.message_location_access_fail), Snackbar.LENGTH_SHORT).show();
            return;
        }
        mMap.setMyLocationEnabled(true);
        currentLocation();
        mMap.setOnInfoWindowClickListener(this);
    }

    private void currentLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, false);
        if ((checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, android.os.Process.myPid(), android.os.Process.myUid()) != PackageManager.PERMISSION_GRANTED)
                && (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, android.os.Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED)) {
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.message_location_access_fail), Snackbar.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "provider enabled : " + locationManager.isProviderEnabled(provider));
        Location location = locationManager.getLastKnownLocation(provider);
        Log.d(TAG, "location != null : " + (location != null));
        if (location != null)
            drawMarker(location);
        else {
            if (locationManager.isProviderEnabled(provider)) {
                Snackbar.make(findViewById(android.R.id.content), R.string.message_wait_task_pending, Snackbar.LENGTH_SHORT).show();
                locationManager.requestLocationUpdates(provider, 0, 0, this);
                return;
            }
            final Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), getString(R.string.message_gps_ask), Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction(R.string.label_enable, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackbar.dismiss();
                    reset = true;
                    startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }
            });
            snackbar.show();
        }
    }

    private void drawMarker(Location location){
        // Remove any existing markers on the map
        mMap.clear();
        LatLng currentPosition = new LatLng(location.getLatitude(),location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 15.4f));
        final Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), R.string.message_wait_task_pending, Snackbar.LENGTH_INDEFINITE);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                snackbar.show();
                workshops = databaseHelper.workshops();
                return null;
            }
            @Override
            public void onPostExecute(Void result) {
                snackbar.dismiss();
                int markerCount = 0;
                for (Workshop item : workshops) {
                    try {
                        LatLng workshopPosition = new LatLng(Double.parseDouble(item.latitude), Double.parseDouble(item.longitude));
                        if ((workshopPosition.latitude == 0) && (workshopPosition.longitude == 0))
                            continue;
                        Marker marker = mMap.addMarker(new MarkerOptions().position(workshopPosition)
                                .title(item.name)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                        marker.showInfoWindow();
                        markerCount++;
                    } catch (NumberFormatException e) { Log.d(TAG, "No position in workshop - " + item.name); }
                }
                if (markerCount == 0)
                    Snackbar.make(findViewById(android.R.id.content), R.string.message_locator_no_workshops, Snackbar.LENGTH_SHORT).show();
            }
        }.execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.RequestCodes.PERMISSION_MAPS_CURRENT_LOCATION:
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                        if ((checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, android.os.Process.myPid(), android.os.Process.myUid()) != PackageManager.PERMISSION_GRANTED)
                                && (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, android.os.Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED)) {
                            Snackbar.make(findViewById(android.R.id.content), getString(R.string.message_location_access_fail), Snackbar.LENGTH_SHORT).show();
                            onBackPressed();
                            return;
                        }
                        mMap.setMyLocationEnabled(true);
                        currentLocation();
                        mMap.setOnInfoWindowClickListener(this);
                    }
                }
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        drawMarker(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(WorkshopLocator.this, ViewHistory.class));
        finish();
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        List<Workshop> workshops = databaseHelper.workshops();
        for (Workshop item : workshops) {
            try {
                LatLng workshopPosition = new LatLng(Double.parseDouble(item.latitude), Double.parseDouble(item.longitude));
                if (marker.getPosition().equals(workshopPosition)) {
                    reset = false;
                    startActivity(new Intent(WorkshopLocator.this, Retrieve.class).putExtra(Constants.Bundle.PAGE_CHOICE, CrudChoices.WORKSHOP).putExtra(Constants.Bundle.ID, item.getId()));
                    break;
                }
            } catch (NumberFormatException e) {
                Log.d(TAG, "No position in workshop");
            }
        }
    }

    private void fetchWorkshops() {
        StringRequest request = new StringRequest(Request.Method.GET, Constants.Url.WORKSHOP, new Response.Listener<String>() {
            @Override
            public void onResponse(final String response) {
                Log.d(TAG, "workshop get = " + response);
                try {
                    JSONObject object = new JSONObject(response);
                    String message = object.optString(Constants.Json.MESSAGE);
                    if (message.equals(getString(R.string.error_auth_fail))) {
                        requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                            @Override
                            public boolean apply(Request<?> request) {
                                return true;
                            }
                        });
                        authenticate();
                        return;
                    }
                } catch (JSONException e) { Log.d(TAG, "workshop array"); }
                final ProgressDialog progressDialog = new ProgressDialog(WorkshopLocator.this);
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    public void onPreExecute() {
                        progressDialog.setMessage(getString(R.string.message_wait_task_pending));
                        progressDialog.show();
                    }
                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            JSONArray array = new JSONArray(response);
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject object = array.getJSONObject(i);
                                String sId = object.getString(DatabaseSchema.COLUMN_ID);
                                String name = object.optString(DatabaseSchema.Workshops.COLUMN_NAME);
                                String address = object.optString(DatabaseSchema.Workshops.COLUMN_ADDRESS);
                                String manager = object.optString(DatabaseSchema.Workshops.COLUMN_MANAGER);
                                String contact = object.optString(DatabaseSchema.Workshops.COLUMN_CONTACT);
                                String latitude = object.optString(DatabaseSchema.Workshops.COLUMN_LATITUDE);
                                String longitude = object.optString(DatabaseSchema.Workshops.COLUMN_LONGITUDE);
                                String city = object.optString(DatabaseSchema.Workshops.COLUMN_CITY);
                                String area = object.optString(DatabaseSchema.Workshops.COLUMN_AREA);
                                String offerings = object.optString(DatabaseSchema.Workshops.COLUMN_OFFERINGS);

                                Workshop workshop = databaseHelper.workshop(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{sId});
                                if (workshop != null) {
                                    databaseHelper.updateWorkshop(sId, name, address, manager, contact, latitude, longitude, city, area, offerings);
                                } else
                                    databaseHelper.addWorkshop(sId, name, address, manager, contact, latitude, longitude, city, area, offerings);
                            }
                        } catch (JSONException e) { Log.d(TAG, "can not parse in background"); }
                        return null;
                    }
                    @Override
                    public void onPostExecute(Void result) {
                        progressDialog.dismiss();
                        initMap();
                    }
                }.execute();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headerParams = new HashMap<>();
                headerParams.put(Constants.VolleyRequest.ACCESS_TOKEN, locData.token());
                return headerParams;
            }
        };
        requestQueue.add(request);
    }

    private void authenticate() {
        User user = databaseHelper.user();
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put(Constants.VolleyRequest.AUTH_MOBILE_PARAM, user.mobile);
        final String authParam = Jwts.builder().setClaims(claimsMap).setHeaderParam("typ", "JWT").setIssuedAt(Calendar.getInstance().getTime()).setExpiration(new Date(Calendar.getInstance().getTime().getTime() + 10*60000)).signWith(SignatureAlgorithm.HS256, Base64.encodeToString(getString(R.string.key_jwt).getBytes(), Base64.DEFAULT)).compact();
        StringRequest request = new StringRequest(Request.Method.POST, Constants.Url.OAUTH, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "auth = " + response);
                try {
                    JSONObject authResponse = new JSONObject(response);
                    boolean success = authResponse.getBoolean(Constants.Json.SUCCESS);
                    if (success) {
                        locData.storeToken(authResponse.getString(Constants.Json.TOKEN));
                        fetchWorkshops();
                    }
                } catch (JSONException e) { e.printStackTrace(); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headerParams = new HashMap<>();
                headerParams.put(Constants.VolleyRequest.AUTH_TOKEN, authParam);
                return headerParams;
            }
        };
        requestQueue.add(request);
    }
}
