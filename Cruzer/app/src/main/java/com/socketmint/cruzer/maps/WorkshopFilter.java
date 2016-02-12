package com.socketmint.cruzer.maps;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.socketmint.cruzer.R;
import com.socketmint.cruzer.drawer.DrawerFragment;
import com.socketmint.cruzer.main.History;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.Login;

public class WorkshopFilter extends AppCompatActivity implements View.OnClickListener, Toolbar.OnMenuItemClickListener {
    private static final String TAG = "WorkshopFilter";

    private AppCompatImageView imageVehicleTypeTW, imageVehicleTypeFW, imageOfferingService, imageOfferingTyre, imageOfferingBattery, imageOfferingAccessories, imageOfferingBeautification, imageOfferingPuncture;

    private Login login = new Login();

    private String vehicleType, offeringType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workshop_filter);

        login.initInstance(this);

        initializeViews();
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
                startActivity(new Intent(WorkshopFilter.this, WorkshopLocator.class).putExtra(Constants.Bundle.OFFERING_FILTER, offeringType).putExtra(Constants.Bundle.VEHICLE_TYPE_FILTER, vehicleType));
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
