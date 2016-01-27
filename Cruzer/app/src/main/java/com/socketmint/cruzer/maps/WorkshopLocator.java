package com.socketmint.cruzer.maps;

import android.Manifest;
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
import android.util.Log;
import android.view.View;

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
import com.socketmint.cruzer.dataholder.Workshop;
import com.socketmint.cruzer.drawer.DrawerFragment;
import com.socketmint.cruzer.main.ViewHistory;
import com.socketmint.cruzer.manage.Constants;

import java.util.List;

public class WorkshopLocator extends FragmentActivity implements OnMapReadyCallback, LocationListener, GoogleMap.OnInfoWindowClickListener {
    private static final String TAG = "WorkshopLocator";

    private GoogleMap mMap;
    private DatabaseHelper databaseHelper;
    private Tracker analyticsTracker;

    private boolean reset = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workshop_locator);

        analyticsTracker = ((CruzerApp) getApplication()).getAnalyticsTracker();
        databaseHelper = new DatabaseHelper(getApplicationContext());

        DrawerFragment drawerFragment = (DrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        drawerFragment.setUp(R.id.navigation_drawer, (Toolbar) findViewById(R.id.toolbar), (DrawerLayout) findViewById(R.id.drawer_layout));

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
        List<Workshop> workshops = databaseHelper.workshops();
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
}
