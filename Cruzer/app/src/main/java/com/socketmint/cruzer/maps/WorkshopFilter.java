package com.socketmint.cruzer.maps;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;

import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.dataholder.location.City;
import com.socketmint.cruzer.dataholder.User;
import com.socketmint.cruzer.drawer.DrawerFragment;
import com.socketmint.cruzer.main.History;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.Login;
import com.socketmint.cruzer.manage.sync.ManualSync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class WorkshopFilter extends AppCompatActivity implements View.OnClickListener, Toolbar.OnMenuItemClickListener {
    private static final String TAG = "WorkshopFilter";

    private AppCompatImageView imageVehicleTypeTW, imageVehicleTypeFW, imageOfferingService, imageOfferingTyre, imageOfferingBattery, imageOfferingAccessories, imageOfferingBeautification, imageOfferingPuncture;
    private AppCompatSpinner citySpinner;
    private ProgressDialog progressDialog;

    private Login login = new Login();
    private DatabaseHelper databaseHelper;
    private ManualSync manualSync;

    private List<City> cities = new ArrayList<>();
    private List<String> cityList = new ArrayList<>();
    private String vehicleType, offeringType, nation, locality;
    private boolean getLocation = false;

    private BroadcastReceiver getCityBroadcast;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workshop_filter);

        getLocation = false;
        initializeBroadcasts();
        login.initInstance(this);
        manualSync = new ManualSync(this);
        databaseHelper = new DatabaseHelper(getApplicationContext());

        progressDialog = new ProgressDialog(this);
        initializeViews();
    }

    @Override
    public void onStart() {
        super.onStart();

        // get location
        new AsyncTask<Void, Void, Void>() {
            @Override
            public void onPreExecute() {
                if (progressDialog != null) {
                    progressDialog.setMessage(getString(R.string.message_wait_task_pending));
                    progressDialog.show();
                }
            }
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    if (getLocation)
                        return null;
                    LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                    Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    Log.d(TAG, "Last Known Location = " + location.getLatitude() + ", " + location.getLongitude());
                    Geocoder geocoder = new Geocoder(WorkshopFilter.this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    locality = addresses.get(0).getLocality();
                    String subLocality = addresses.get(0).getSubLocality();
                    nation = addresses.get(0).getCountryName();
                    Log.d(TAG, "Geo Address : " + addresses.get(0).toString());
                    Log.d(TAG, "Locality = " + locality + " | Sub Locality = " + subLocality + " | countryName = " + nation);
                } catch (SecurityException | IOException | NullPointerException e) { e.printStackTrace(); }
                return null;
            }
            @Override
            public void onPostExecute(Void result) {
                Bundle syncBundle = new Bundle();
                syncBundle.putString(Constants.Bundle.CITY, locality);
                syncBundle.putString(Constants.Bundle.COUNTRY, nation);
                if (login.login() > Login.LoginType.TRIAL)
                    manualSync.syncEverything(syncBundle);
            }
        }.execute();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(getCityBroadcast);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(getCityBroadcast, new IntentFilter(Constants.IntentFilters.CITY));
    }

    private void initializeBroadcasts() {
        getCityBroadcast = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (progressDialog != null)
                    progressDialog.dismiss();
                setSpinnerContent();
            }
        };
    }

    private void initializeViews() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_workshop_filter);
        toolbar.setOnMenuItemClickListener(this);
        DrawerFragment drawerFragment = (DrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        drawerFragment.setUp(R.id.navigation_drawer, toolbar, (DrawerLayout) findViewById(R.id.drawer_layout));

        imageVehicleTypeTW = (AppCompatImageView) findViewById(R.id.image_filter_vehicle_two_wheeler);
        imageVehicleTypeFW = (AppCompatImageView) findViewById(R.id.image_filter_vehicle_four_wheeler);
        imageOfferingService = (AppCompatImageView) findViewById(R.id.image_filter_offering_service);
        imageOfferingTyre = (AppCompatImageView) findViewById(R.id.image_filter_offering_tyre);
        imageOfferingBattery = (AppCompatImageView) findViewById(R.id.image_filter_offering_batteries);
        imageOfferingAccessories = (AppCompatImageView) findViewById(R.id.image_filter_offering_accessories);
        imageOfferingBeautification = (AppCompatImageView) findViewById(R.id.image_filter_offering_beautification);
        imageOfferingPuncture = (AppCompatImageView) findViewById(R.id.image_filter_offering_puncture);
        citySpinner = (AppCompatSpinner) findViewById(R.id.spinner_city_name);

        setSpinnerContent();

        findViewById(R.id.layout_filter_vehicle_two_wheeler).setOnClickListener(this);
        findViewById(R.id.layout_filter_vehicle_four_wheeler).setOnClickListener(this);
        findViewById(R.id.layout_filter_offering_service).setOnClickListener(this);
        findViewById(R.id.layout_filter_offering_tyres).setOnClickListener(this);
        findViewById(R.id.layout_filter_offering_batteries).setOnClickListener(this);
        findViewById(R.id.layout_filter_offering_accessories).setOnClickListener(this);
        findViewById(R.id.layout_filter_offering_beautification).setOnClickListener(this);
        findViewById(R.id.layout_filter_offering_puncture).setOnClickListener(this);
        findViewById(R.id.button_filter_workshops).setOnClickListener(this);
    }

    private void setSpinnerContent() {
        User user = databaseHelper.user();
        City city = databaseHelper.city(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{user.getCityId()});
        cities = databaseHelper.cities();
        cityList.clear();
        for (City item : cities) {
            cityList.add(item.city);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, R.id.text_spinner_item, cityList);
        citySpinner.setAdapter(adapter);
        if (city != null) {
            int index = cityList.indexOf(city.city);
            citySpinner.setSelection(index);
        }
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(WorkshopFilter.this, History.class));
        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.layout_filter_vehicle_two_wheeler:
                selectVehicleType(VehicleTypeFilter.TWO_WHEELER);
                break;
            case R.id.layout_filter_vehicle_four_wheeler:
                selectVehicleType(VehicleTypeFilter.FOUR_WHEELER);
                break;
            case R.id.layout_filter_offering_service:
                selectOffering(OfferingFilter.SERVICE);
                break;
            case R.id.layout_filter_offering_tyres:
                selectOffering(OfferingFilter.TYRE);
                break;
            case R.id.layout_filter_offering_batteries:
                selectOffering(OfferingFilter.BATTERIES);
                break;
            case R.id.layout_filter_offering_accessories:
                selectOffering(OfferingFilter.ACCESSORIES);
                break;
            case R.id.layout_filter_offering_beautification:
                selectOffering(OfferingFilter.BEAUTIFICATION);
                break;
            case R.id.layout_filter_offering_puncture:
                selectOffering(OfferingFilter.PUNCTURE);
                break;
            case R.id.button_filter_workshops:
                if (!login.isNetworkAvailable()) {
                    final Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), R.string.message_volley_error_response, Snackbar.LENGTH_INDEFINITE);
                    snackbar.setAction(R.string.label_enable, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                            snackbar.dismiss();
                        }
                    });
                    snackbar.show();
                    return;
                }
                int position = citySpinner.getSelectedItemPosition();
                startActivity(new Intent(WorkshopFilter.this, WorkshopLocator.class).putExtra(Constants.Bundle.OFFERING_FILTER, offeringType).putExtra(Constants.Bundle.VEHICLE_TYPE_FILTER, vehicleType).putExtra(Constants.Bundle.CITY, cities.get(position).getId()));
                break;
        }
    }

    private void selectVehicleType(String choice) {
        switch (choice) {
            case VehicleTypeFilter.TWO_WHEELER:
                if (vehicleType != null && vehicleType.equals(VehicleTypeFilter.TWO_WHEELER)) {
                    imageVehicleTypeTW.setImageResource(R.drawable.ic_2_wheelers_disabled);
                    vehicleType = null;
                    break;
                }
                imageVehicleTypeTW.setImageResource(R.drawable.ic_2_wheelers_enabled);
                imageVehicleTypeFW.setImageResource(R.drawable.ic_4_wheelers_disabled);
                vehicleType = VehicleTypeFilter.TWO_WHEELER;
                break;
            case VehicleTypeFilter.FOUR_WHEELER:
                if (vehicleType != null &&vehicleType.equals(VehicleTypeFilter.FOUR_WHEELER)) {
                    imageVehicleTypeFW.setImageResource(R.drawable.ic_4_wheelers_disabled);
                    vehicleType = null;
                    break;
                }
                imageVehicleTypeTW.setImageResource(R.drawable.ic_2_wheelers_disabled);
                imageVehicleTypeFW.setImageResource(R.drawable.ic_4_wheelers_enabled);
                vehicleType = VehicleTypeFilter.FOUR_WHEELER;
                break;
        }
    }

    private void selectOffering(String choice) {
        switch (choice) {
            case OfferingFilter.SERVICE:
                if (offeringType != null && offeringType.equals(OfferingFilter.SERVICE)) {
                    imageOfferingService.setImageResource(R.drawable.ic_service_disabled);
                    offeringType = null;
                    break;
                }
                imageOfferingService.setImageResource(R.drawable.ic_service_enabled);
                imageOfferingBattery.setImageResource(R.drawable.ic_batter_disabled);
                imageOfferingTyre.setImageResource(R.drawable.ic_tyres_disabled);
                imageOfferingAccessories.setImageResource(R.drawable.ic_accessories_disabled);
                imageOfferingBeautification.setImageResource(R.drawable.ic_beautification_disabled);
                imageOfferingPuncture.setImageResource(R.drawable.ic_puncture_disabled);
                offeringType = OfferingFilter.SERVICE;
                break;
            case OfferingFilter.TYRE:
                if (offeringType != null && offeringType.equals(OfferingFilter.TYRE)) {
                    imageOfferingTyre.setImageResource(R.drawable.ic_tyres_disabled);
                    offeringType = null;
                    break;
                }
                imageOfferingService.setImageResource(R.drawable.ic_service_disabled);
                imageOfferingBattery.setImageResource(R.drawable.ic_batter_disabled);
                imageOfferingTyre.setImageResource(R.drawable.ic_tyres_enabled);
                imageOfferingAccessories.setImageResource(R.drawable.ic_accessories_disabled);
                imageOfferingBeautification.setImageResource(R.drawable.ic_beautification_disabled);
                imageOfferingPuncture.setImageResource(R.drawable.ic_puncture_disabled);
                offeringType = OfferingFilter.TYRE;
                break;
            case OfferingFilter.BATTERIES:
                if (offeringType != null && offeringType.equals(OfferingFilter.BATTERIES)) {
                    imageOfferingBattery.setImageResource(R.drawable.ic_batter_disabled);
                    offeringType = null;
                    break;
                }
                imageOfferingService.setImageResource(R.drawable.ic_service_disabled);
                imageOfferingBattery.setImageResource(R.drawable.ic_batter_enabled);
                imageOfferingTyre.setImageResource(R.drawable.ic_tyres_disabled);
                imageOfferingAccessories.setImageResource(R.drawable.ic_accessories_disabled);
                imageOfferingBeautification.setImageResource(R.drawable.ic_beautification_disabled);
                imageOfferingPuncture.setImageResource(R.drawable.ic_puncture_disabled);
                offeringType = OfferingFilter.BATTERIES;
                break;
            case OfferingFilter.ACCESSORIES:
                if (offeringType != null && offeringType.equals(OfferingFilter.ACCESSORIES)) {
                    imageOfferingAccessories.setImageResource(R.drawable.ic_accessories_disabled);
                    offeringType = null;
                    break;
                }
                imageOfferingService.setImageResource(R.drawable.ic_service_disabled);
                imageOfferingBattery.setImageResource(R.drawable.ic_batter_disabled);
                imageOfferingTyre.setImageResource(R.drawable.ic_tyres_disabled);
                imageOfferingAccessories.setImageResource(R.drawable.ic_accessories_enabled);
                imageOfferingBeautification.setImageResource(R.drawable.ic_beautification_disabled);
                imageOfferingPuncture.setImageResource(R.drawable.ic_puncture_disabled);
                offeringType = OfferingFilter.ACCESSORIES;
                break;
            case OfferingFilter.BEAUTIFICATION:
                if (offeringType != null && offeringType.equals(OfferingFilter.BEAUTIFICATION)) {
                    imageOfferingBeautification.setImageResource(R.drawable.ic_beautification_disabled);
                    offeringType = null;
                    break;
                }
                imageOfferingService.setImageResource(R.drawable.ic_service_disabled);
                imageOfferingBattery.setImageResource(R.drawable.ic_batter_disabled);
                imageOfferingTyre.setImageResource(R.drawable.ic_tyres_disabled);
                imageOfferingAccessories.setImageResource(R.drawable.ic_accessories_disabled);
                imageOfferingBeautification.setImageResource(R.drawable.ic_beautification_enabled);
                imageOfferingPuncture.setImageResource(R.drawable.ic_puncture_disabled);
                offeringType = OfferingFilter.BEAUTIFICATION;
                break;
            case OfferingFilter.PUNCTURE:
                if (offeringType != null && offeringType.equals(OfferingFilter.PUNCTURE)) {
                    imageOfferingPuncture.setImageResource(R.drawable.ic_puncture_disabled);
                    offeringType = null;
                    break;
                }
                imageOfferingService.setImageResource(R.drawable.ic_service_disabled);
                imageOfferingBattery.setImageResource(R.drawable.ic_batter_disabled);
                imageOfferingTyre.setImageResource(R.drawable.ic_tyres_disabled);
                imageOfferingAccessories.setImageResource(R.drawable.ic_accessories_disabled);
                imageOfferingBeautification.setImageResource(R.drawable.ic_beautification_disabled);
                imageOfferingPuncture.setImageResource(R.drawable.ic_puncture_enabled);
                offeringType = OfferingFilter.PUNCTURE;
                break;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_locate:
                if (!login.isNetworkAvailable()) {
                    final Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), R.string.message_volley_error_response, Snackbar.LENGTH_INDEFINITE);
                    snackbar.setAction(R.string.label_enable, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                            snackbar.dismiss();
                        }
                    });
                    snackbar.show();
                    return false;
                }
                startActivity(new Intent(WorkshopFilter.this, WorkshopLocator.class).putExtra(Constants.Bundle.OFFERING_FILTER, offeringType).putExtra(Constants.Bundle.VEHICLE_TYPE_FILTER, vehicleType));
                break;
        }
        return false;
    }

    public abstract class VehicleTypeFilter {
        public static final String TWO_WHEELER = "2";
        public static final String FOUR_WHEELER = "4";
    }

    public abstract class OfferingFilter {
        public static final String SERVICE = "1";
        public static final String TYRE = "2";
        public static final String BATTERIES = "3";
        public static final String ACCESSORIES = "4";
        public static final String BEAUTIFICATION = "5";
        public static final String PUNCTURE = "6";
    }
}
