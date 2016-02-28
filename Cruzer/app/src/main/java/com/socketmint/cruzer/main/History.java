package com.socketmint.cruzer.main;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.ListViewCompat;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.socketmint.cruzer.CruzerApp;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.crud.ChoiceDialog;
import com.socketmint.cruzer.crud.create.Create;
import com.socketmint.cruzer.crud.retrieve.Retrieve;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.dataholder.PUC;
import com.socketmint.cruzer.dataholder.User;
import com.socketmint.cruzer.dataholder.expense.Refuel;
import com.socketmint.cruzer.dataholder.expense.service.Service;
import com.socketmint.cruzer.dataholder.insurance.Insurance;
import com.socketmint.cruzer.dataholder.insurance.InsuranceCompany;
import com.socketmint.cruzer.dataholder.location.City;
import com.socketmint.cruzer.dataholder.location.Country;
import com.socketmint.cruzer.dataholder.vehicle.Vehicle;
import com.socketmint.cruzer.dataholder.workshop.Workshop;
import com.socketmint.cruzer.drawer.DrawerFragment;
import com.socketmint.cruzer.manage.Choices;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.LocData;
import com.socketmint.cruzer.manage.Login;
import com.socketmint.cruzer.manage.gcm.RegistrationService;
import com.socketmint.cruzer.manage.sync.ManualSync;
import com.socketmint.cruzer.maps.WorkshopFilter;
import com.socketmint.cruzer.ui.UiElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class History extends AppCompatActivity implements View.OnClickListener, Toolbar.OnMenuItemClickListener {
    private static final String TAG = "History ";
    private static final String ACTION_ADD_FAB = "Add Fab";
    private static final String ACTION_ADD_REFUEL = "Add Refuel";
    private static final String ACTION_ADD_SERVICE = "Add Service";
    private static final String ACTION_ADD_PUC = "Add PUC";
    private static final String ACTION_ADD_INSURANCE = "Add Insurance";
    private static final String ACTION_VEHICLE_LIST = "Vehicle List";

    private FloatingActionButton fabAdd, fabRefuel, fabService, fabInsurance, fabPUC;
    private Animation animRotateForward, animRotateBackward, animFadeOut, animFadeIn;
    private Animation animRefuelOut, animServiceOut, animInsuranceOut, animPUCOut, animRefuelIn, animServiceIn, animInsuranceIn, animPUCIn;
    private Animation animCardRefuelOut, animCardServiceOut, animCardInsuranceOut, animCardPUCOut, animCardRefuelIn, animCardServiceIn, animCardInsuranceIn, animCardPUCIn;
    private CardView cardAddService, cardAddRefuel, cardAddInsurance, cardAddPUC;
    private AppCompatTextView toolbarTitle;
    private LinearLayoutCompat layoutAddActive;
    private DrawerFragment drawerFragment;

    private Adapter adapter;
    private List<Holder> holders = new ArrayList<>();
    private boolean isFabOpen = false, getLocation = false;
    private String vehicleId, nation, locality;
    private Vehicle vehicle;

    private DatabaseHelper databaseHelper;
    private UiElement uiElement;
    private ChoiceDialog choiceDialog;
    private Login login = Login.getInstance();
    private LocData locData = new LocData();
    private ManualSync manualSync;

    private BroadcastReceiver gcmBroadcast;
    private Tracker analyticsTracker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        getLocation = false;
        analyticsTracker = ((CruzerApp) getApplication()).getAnalyticsTracker();
        initializeBroadcasts();

        if (checkPlayServices()) {
            Intent gcmIntent = new Intent(this, RegistrationService.class);
            startService(gcmIntent);
        }

        databaseHelper = new DatabaseHelper(getApplicationContext());
        uiElement = new UiElement(this);
        choiceDialog = new ChoiceDialog(this);
        login.initInstance(this);
        manualSync = new ManualSync(this);
        locData.cruzerInstance(this);

        if (databaseHelper.vehicleCount() == 0) {
            startActivity(new Intent(History.this, Create.class).putExtra(Constants.Bundle.PAGE_CHOICE, Choices.VEHICLE));
            finish();
            return;
        }

        vehicleId = getIntent().getStringExtra(Constants.Bundle.VEHICLE_ID);
        adjustVehicleId();

        if (login.login() > Login.LoginType.TRIAL)
            sync();

        initializeViews();
        initializeAssets();
    }

    @Override
    public void onStart() {
        super.onStart();
        analyticsTracker.setScreenName(TAG);
        analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());

        addData();

        getGeoLocation();
    }

    private void getGeoLocation() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    if (getLocation)
                        return null;
                    LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                    Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    Log.d(TAG, "Last Known Location = " + location.getLatitude() + ", " + location.getLongitude());
                    Geocoder geocoder = new Geocoder(History.this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    locality = addresses.get(0).getLocality();
                    String subLocality = addresses.get(0).getSubLocality();
                    nation = addresses.get(0).getCountryName();
                    Log.d(TAG, "Geo Address : " + addresses.get(0).toString());
                    Log.d(TAG, "Locality = " + locality + " | Sub Locality = " + subLocality + " | countryName = " + nation);
                } catch (SecurityException | IOException | NullPointerException e) { Log.e(TAG, "can not get location"); }
                return null;
            }
            @Override
            public void onPostExecute(Void result) {
                Bundle syncBundle = new Bundle();
                User user = databaseHelper.user();
                City city = databaseHelper.city(Collections.singletonList(DatabaseSchema.Cities.COLUMN_ID), new String[]{user.getCityId()});
                if (city != null) {
                    Country country = databaseHelper.country(Collections.singletonList(DatabaseSchema.Countries.COLUMN_ID), new String[]{city.getCountryId()});
                    if (country != null) {
                        locality = city.city;
                        nation = country.country;
                    }
                }
                syncBundle.putString(Constants.Bundle.CITY, locality);
                syncBundle.putString(Constants.Bundle.COUNTRY, nation);
                if (login.login() > Login.LoginType.TRIAL)
                    manualSync.syncEverything(syncBundle);
            }
        }.execute();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(gcmBroadcast);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(gcmBroadcast, new IntentFilter(Constants.IntentFilters.GCM));
        drawerFragment.changeVehicleCount();
        drawerFragment.changeUserName();
    }

    private void initializeBroadcasts() {
        gcmBroadcast = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getBooleanExtra(Constants.Gcm.MESSAGE_TOKEN_SENT, false))
                    Log.d(TAG, "gcm token sent to server");
                if (intent.getBooleanExtra(Constants.Gcm.MESSAGE_UPDATE, false))
                    addData();
            }
        };
    }

    private void adjustVehicleId() {
        vehicleId = (vehicleId != null) ? vehicleId : "all";
        vehicle = databaseHelper.vehicle(vehicleId);
        vehicle = (vehicle != null) ? vehicle : databaseHelper.firstVehicle();
        vehicleId = (vehicleId.equals("all") ? vehicleId : vehicle.getId());
    }

    private void initializeViews() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_history);
        toolbar.setOnMenuItemClickListener(this);

        drawerFragment = (DrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        drawerFragment.setUp(R.id.navigation_drawer, toolbar, (DrawerLayout) findViewById(R.id.drawer_layout));

        fabAdd = (FloatingActionButton) findViewById(R.id.fab_add);
        fabRefuel = (FloatingActionButton) findViewById(R.id.fab_add_refuel);
        fabService = (FloatingActionButton) findViewById(R.id.fab_add_service);
        fabInsurance = (FloatingActionButton) findViewById(R.id.fab_add_insurance);
        fabPUC = (FloatingActionButton) findViewById(R.id.fab_add_puc);
        toolbarTitle = (AppCompatTextView) findViewById(R.id.toolbar_title);
        cardAddRefuel = (CardView) findViewById(R.id.card_add_refuel);
        cardAddService = (CardView) findViewById(R.id.card_add_service);
        cardAddInsurance = (CardView) findViewById(R.id.card_add_insurance);
        cardAddPUC = (CardView) findViewById(R.id.card_add_puc);
        layoutAddActive = (LinearLayoutCompat) findViewById(R.id.layout_add_active);

        adapter = new Adapter();
        ((ListViewCompat) findViewById(R.id.list_history)).setAdapter(adapter);

        fabAdd.setOnClickListener(this);
        fabRefuel.setOnClickListener(this);
        fabService.setOnClickListener(this);
        fabInsurance.setOnClickListener(this);
        fabPUC.setOnClickListener(this);
        toolbarTitle.setOnClickListener(this);
    }

    private void initializeAssets() {
        isFabOpen = false;
        animRotateForward = AnimationUtils.loadAnimation(this, R.anim.rotate_forward);
        animRotateBackward = AnimationUtils.loadAnimation(this, R.anim.rotate_backward);
        animFadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        animFadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);

        animCardRefuelOut = AnimationUtils.loadAnimation(this, R.anim.zoom_out);
        animCardServiceOut = AnimationUtils.loadAnimation(this, R.anim.zoom_out);
        animCardInsuranceOut = AnimationUtils.loadAnimation(this, R.anim.zoom_out);
        animCardPUCOut = AnimationUtils.loadAnimation(this, R.anim.zoom_out);
        animCardRefuelOut.setStartOffset(Constants.AnimationStartOffset.REFUEL_EXIT);
        animCardServiceOut.setStartOffset(Constants.AnimationStartOffset.SERVICE_EXIT);
        animCardInsuranceOut.setStartOffset(Constants.AnimationStartOffset.INSURANCE_EXIT);
        animCardPUCOut.setStartOffset(Constants.AnimationStartOffset.PUC_EXIT);

        animCardRefuelIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
        animCardServiceIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
        animCardInsuranceIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
        animCardPUCIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
        animCardRefuelIn.setStartOffset(Constants.AnimationStartOffset.REFUEL_ENTRY);
        animCardServiceIn.setStartOffset(Constants.AnimationStartOffset.SERVICE_ENTRY);
        animCardInsuranceIn.setStartOffset(Constants.AnimationStartOffset.INSURANCE_ENTRY);
        animCardPUCIn.setStartOffset(Constants.AnimationStartOffset.PUC_ENTRY);

        animRefuelIn = AnimationUtils.loadAnimation(this, R.anim.slide_from_right);
        animServiceIn = AnimationUtils.loadAnimation(this, R.anim.slide_from_right);
        animInsuranceIn = AnimationUtils.loadAnimation(this, R.anim.slide_from_right);
        animPUCIn = AnimationUtils.loadAnimation(this, R.anim.slide_from_right);
        animRefuelIn.setStartOffset(Constants.AnimationStartOffset.REFUEL_ENTRY);
        animServiceIn.setStartOffset(Constants.AnimationStartOffset.SERVICE_ENTRY);
        animInsuranceIn.setStartOffset(Constants.AnimationStartOffset.INSURANCE_ENTRY);
        animPUCIn.setStartOffset(Constants.AnimationStartOffset.PUC_ENTRY);

        animRefuelOut = AnimationUtils.loadAnimation(this, R.anim.slide_to_right);
        animServiceOut = AnimationUtils.loadAnimation(this, R.anim.slide_to_right);
        animInsuranceOut = AnimationUtils.loadAnimation(this, R.anim.slide_to_right);
        animPUCOut = AnimationUtils.loadAnimation(this, R.anim.slide_to_right);
        animRefuelOut.setStartOffset(Constants.AnimationStartOffset.REFUEL_EXIT);
        animServiceOut.setStartOffset(Constants.AnimationStartOffset.SERVICE_EXIT);
        animInsuranceOut.setStartOffset(Constants.AnimationStartOffset.INSURANCE_EXIT);
        animPUCOut.setStartOffset(Constants.AnimationStartOffset.PUC_EXIT);
    }

    private void addData() {
        if (vehicleId.equals("all"))
            toolbarTitle.setText(R.string.text_all_vehicles);
        else {
            String string;
            if (vehicle.name != null) {
                if (vehicle.name.isEmpty())
                    string = (vehicle.model != null) ? vehicle.model.name : vehicle.reg;
                else
                    string = vehicle.name;
            } else
                string = (vehicle.model != null) ? vehicle.model.name : vehicle.reg;
            toolbarTitle.setText(string);
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                List<Refuel> refuels = (vehicleId.equals("all")) ? databaseHelper.refuels() : databaseHelper.refuels(Collections.singletonList(DatabaseSchema.COLUMN_VEHICLE_ID), new String[]{vehicleId});
                refuels = (refuels == null) ? new ArrayList<Refuel>() : refuels;
                Collections.sort(refuels, new Comparator<Refuel>() {
                    @Override
                    public int compare(Refuel lhs, Refuel rhs) {
                        return rhs.date.compareTo(lhs.date);
                    }
                });
                holders.clear();
                for (Refuel item : refuels) {
                    holders.add(new Holder(Choices.REFUEL, item));
                }
                List<Service> services = (vehicleId.equals("all")) ? databaseHelper.services() : databaseHelper.services(Collections.singletonList(DatabaseSchema.COLUMN_VEHICLE_ID), new String[]{vehicleId});
                services = (services == null) ? new ArrayList<Service>() : services;
                Collections.sort(services, new Comparator<Service>() {
                    @Override
                    public int compare(Service lhs, Service rhs) {
                        return rhs.date.compareTo(lhs.date);
                    }
                });
                for (Service item : services) {
                    holders.add(new Holder(Choices.SERVICE, item));
                }
                List<Insurance> insurances = (vehicleId.equals("all")) ? databaseHelper.insurances() : databaseHelper.insurances(Collections.singletonList(DatabaseSchema.COLUMN_VEHICLE_ID), new String[]{vehicleId});
                insurances = (insurances == null) ? new ArrayList<Insurance>() : insurances;
                Collections.sort(insurances, new Comparator<Insurance>() {
                    @Override
                    public int compare(Insurance lhs, Insurance rhs) {
                        return rhs.startDate.compareTo(lhs.startDate);
                    }
                });
                for (Insurance item : insurances) {
                    holders.add(new Holder(Choices.INSURANCE, item));
                }
                List<PUC> pucList = (vehicleId.equals("all")) ? databaseHelper.pucList() : databaseHelper.pucList(Collections.singletonList(DatabaseSchema.COLUMN_VEHICLE_ID), new String[]{vehicleId});
                pucList = (pucList == null) ? new ArrayList<PUC>() : pucList;
                Collections.sort(pucList, new Comparator<PUC>() {
                    @Override
                    public int compare(PUC lhs, PUC rhs) {
                        return rhs.startDate.compareTo(lhs.startDate);
                    }
                });
                for (PUC item : pucList) {
                    holders.add(new Holder(Choices.PUC, item));
                }
                Log.d(TAG, "pucList.size : " + pucList.size() + " | holders.size : " + holders.size());
                Collections.sort(holders, new Comparator<Holder>() {
                    @Override
                    public int compare(Holder lhs, Holder rhs) {
                        String lhsDate, rhsDate;
                        switch (lhs.type) {
                            case Choices.REFUEL:
                                lhsDate = ((Refuel) lhs.object).date;
                                break;
                            case Choices.SERVICE:
                                lhsDate = ((Service) lhs.object).date;
                                break;
                            case Choices.INSURANCE:
                                lhsDate = ((Insurance) lhs.object).startDate;
                                break;
                            case Choices.PUC:
                                lhsDate = ((PUC) lhs.object).startDate;
                                break;
                            default:
                                return 0;
                        }
                        switch (rhs.type) {
                            case Choices.REFUEL:
                                rhsDate = ((Refuel) rhs.object).date;
                                break;
                            case Choices.SERVICE:
                                rhsDate = ((Service) rhs.object).date;
                                break;
                            case Choices.INSURANCE:
                                rhsDate = ((Insurance) rhs.object).startDate;
                                break;
                            case Choices.PUC:
                                rhsDate = ((PUC) rhs.object).startDate;
                                break;
                            default:
                                return 0;
                        }
                        return rhsDate.compareTo(lhsDate);
                    }
                });
                return null;
            }
            @Override
            protected void onPostExecute(Void result) {
                adapter.animateTo(holders);
            }
        }.execute();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab_add:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_ADD_FAB).build());
                animateFab();
                break;
            case R.id.fab_add_refuel:
                animateFab();
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_ADD_REFUEL).build());
                startActivity(new Intent(History.this, Create.class).putExtra(Constants.Bundle.PAGE_CHOICE, Choices.REFUEL).putExtra(Constants.Bundle.VEHICLE_ID, vehicleId));
                break;
            case R.id.fab_add_service:
                animateFab();
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_ADD_SERVICE).build());
                startActivity(new Intent(History.this, Create.class).putExtra(Constants.Bundle.PAGE_CHOICE, Choices.SERVICE).putExtra(Constants.Bundle.VEHICLE_ID, vehicleId));
                break;
            case R.id.fab_add_insurance:
                animateFab();
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_ADD_INSURANCE).build());
                startActivity(new Intent(History.this, Create.class).putExtra(Constants.Bundle.PAGE_CHOICE, Choices.INSURANCE).putExtra(Constants.Bundle.VEHICLE_ID, vehicleId));
                break;
            case R.id.fab_add_puc:
                animateFab();
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_ADD_PUC).build());
                startActivity(new Intent(History.this, Create.class).putExtra(Constants.Bundle.PAGE_CHOICE, Choices.PUC).putExtra(Constants.Bundle.VEHICLE_ID, vehicleId));
                break;
            case R.id.toolbar_title:
                if (!vehicleId.equals("all"))
                    startActivity(new Intent(History.this, Retrieve.class).putExtra(Constants.Bundle.PAGE_CHOICE, Choices.VEHICLE).putExtra(Constants.Bundle.ID, vehicleId));
                break;
        }
    }

    private void animateFab() {
        if(isFabOpen) {
            layoutAddActive.startAnimation(animFadeOut);
            fabService.startAnimation(animServiceOut);
            fabRefuel.startAnimation(animRefuelOut);
            fabInsurance.startAnimation(animInsuranceOut);
            fabPUC.startAnimation(animPUCOut);
            cardAddService.startAnimation(animCardServiceOut);
            cardAddRefuel.startAnimation(animCardRefuelOut);
            cardAddInsurance.startAnimation(animCardInsuranceOut);
            cardAddPUC.startAnimation(animCardPUCOut);
            fabAdd.startAnimation(animRotateBackward);
            isFabOpen = false;
        } else {
            layoutAddActive.startAnimation(animFadeIn);
            fabService.startAnimation(animServiceIn);
            fabRefuel.startAnimation(animRefuelIn);
            fabInsurance.startAnimation(animInsuranceIn);
            fabPUC.startAnimation(animPUCIn);
            cardAddService.startAnimation(animCardServiceIn);
            cardAddRefuel.startAnimation(animCardRefuelIn);
            cardAddInsurance.startAnimation(animCardInsuranceIn);
            cardAddPUC.startAnimation(animCardPUCIn);
            fabAdd.startAnimation(animRotateForward);
            isFabOpen = true;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_history:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(TAG + ACTION_VEHICLE_LIST).build());
                choiceDialog.chooseVehicle();
                break;
            case R.id.item_locate:
                startActivity(new Intent(History.this, WorkshopFilter.class));
                finish();
                break;
        }
        return false;
    }

    private class Adapter extends BaseAdapter implements View.OnClickListener {
        private List<Holder> holderList = new ArrayList<>();

        public void animateTo(List<Holder> items) {
            applyAndAnimateRemovals(items);
            applyAndAnimateAdditions(items);
            applyAndAnimateMovedItems(items);
        }

        private void applyAndAnimateRemovals(List<Holder> newItems) {
            for (int i = holderList.size() - 1; i >= 0; i--) {
                final Holder model = holderList.get(i);
                if (!newItems.contains(model)) {
                    removeItem(i);
                }
            }
        }

        private void applyAndAnimateAdditions(List<Holder> newItems) {
            for (int i = 0, count = newItems.size(); i < count; i++) {
                final Holder model = newItems.get(i);
                if (!holderList.contains(model)) {
                    addItem(i, model);
                }
            }
        }

        private void applyAndAnimateMovedItems(List<Holder> newItems) {
            for (int toPosition = newItems.size() - 1; toPosition >= 0; toPosition--) {
                final Holder model = newItems.get(toPosition);
                final int fromPosition = holderList.indexOf(model);
                if (fromPosition >= 0 && fromPosition != toPosition) {
                    moveItem(fromPosition, toPosition);
                }
            }
        }

        public Holder removeItem(int position) {
            final Holder model = holderList.remove(position);
            notifyDataSetChanged();
            return model;
        }

        public void addItem(int position, Holder item) {
            holderList.add(position, item);
            notifyDataSetChanged();
        }

        public void moveItem(int fromPosition, int toPosition) {
            final Holder model = holderList.remove(fromPosition);
            holderList.add(toPosition, model);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return holderList.size();
        }

        @Override
        public Object getItem(int position) {
            return holderList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return holderList.indexOf(holderList.get(position));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder = holderList.get(position);
            View view = (convertView == null) ? ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.item_history, parent, false) : convertView;

            String title, date, amount, odo;
            int icon;
            Vehicle currentVehicle;

            switch (holder.type) {
                case Choices.REFUEL:
                    Refuel refuel = (Refuel) holder.object;
                    date = uiElement.cardDate(refuel.date);
                    odo = refuel.odo.isEmpty() ? "" : getString(R.string.text_odometer, refuel.odo);
                    title = getString(R.string.title_refuel);
                    amount = refuel.cost.isEmpty() ? "" : getString(R.string.text_amount, refuel.cost);
                    icon = R.drawable.ic_refuel_card;
                    currentVehicle = databaseHelper.vehicle(refuel.getVehicleId());
                    break;
                case Choices.SERVICE:
                    Service service = (Service) holder.object;
                    date = uiElement.cardDate(service.date);
                    odo = service.odo.isEmpty() ? "" : getString(R.string.text_odometer, service.odo);
                    title = getString(R.string.title_service);
                    amount = service.cost.isEmpty() ? "" : getString(R.string.text_amount, service.cost);
                    icon = R.drawable.ic_service_card;
                    currentVehicle = databaseHelper.vehicle(service.getVehicleId());
                    break;
                case Choices.INSURANCE:
                    Insurance insurance = (Insurance) holder.object;
                    date = uiElement.cardDate(insurance.startDate);
                    InsuranceCompany company = databaseHelper.insuranceCompany(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{insurance.getCompanyId()});
                    odo = (company == null) ? "" : company.company;
                    title = getString(R.string.title_insurance);
                    amount = insurance.premium.isEmpty() ? "" : getString(R.string.text_amount, insurance.premium);
                    icon = R.drawable.ic_service_card;
                    currentVehicle = databaseHelper.vehicle(insurance.getVehicleId());
                    break;
                case Choices.PUC:
                    PUC puc = (PUC) holder.object;
                    date = uiElement.cardDate(puc.startDate);
                    Workshop workshop = databaseHelper.workshop(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{puc.getWorkshopId()});
                    odo = (workshop == null) ? "" : workshop.name;
                    title = getString(R.string.title_puc);
                    amount = puc.fees.isEmpty() ? "" : getString(R.string.text_amount, puc.fees);
                    icon = R.drawable.ic_service_card;
                    currentVehicle = databaseHelper.vehicle(puc.getVehicleId());
                    break;
                default:
                    return view;
            }

            AppCompatImageView road = (AppCompatImageView) view.findViewById(R.id.icon_history_road);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) road.getLayoutParams();
            final float scale = getApplicationContext().getResources().getDisplayMetrics().density;
            if (position == 0)

                layoutParams.setMargins(0, (int) (24f * scale + 0.5f), 0, 0);
            else if (position == holderList.size()-1) {
                layoutParams.setMargins(0, 0, 0, (int) (24f * scale + 0.5f));
                view.findViewById(R.id.view_separator).setVisibility(View.GONE);
            } else
                layoutParams.setMargins(0, 0, 0, 0);
            road.setVisibility((holderList.size() == 1) ? View.GONE : View.VISIBLE);
            road.setLayoutParams(layoutParams);

            AppCompatTextView textDate = (AppCompatTextView) view.findViewById(R.id.text_date);
            AppCompatTextView textVehicleName = (AppCompatTextView) view.findViewById(R.id.text_vehicle_name);
            AppCompatTextView textAmount = (AppCompatTextView) view.findViewById(R.id.text_amount);
            AppCompatTextView textOdo = (AppCompatTextView) view.findViewById(R.id.text_odo);
            AppCompatTextView textTitle = (AppCompatTextView) view.findViewById(R.id.text_title);

            ((AppCompatImageView) view.findViewById(R.id.icon_history_type)).setImageResource(icon);
            textDate.setText(date);
            textVehicleName.setText(vehicleName(currentVehicle));
            textAmount.setText(amount);
            textOdo.setText(Html.fromHtml(odo));
            textTitle.setText(title);

            textDate.setVisibility((date == null || date.isEmpty()) ? View.GONE : View.VISIBLE);
            textAmount.setVisibility(amount.isEmpty() ? View.GONE : View.VISIBLE);
            textOdo.setVisibility(odo.isEmpty() ? View.GONE : View.VISIBLE);
            textTitle.setVisibility(title.isEmpty() ? View.GONE : View.VISIBLE);

            view.setTag(holder);
            view.setOnClickListener(this);
            return view;
        }

        @Override
        public void onClick(View v) {
            Holder holder = (Holder) v.getTag();
            String targetId;
            switch (holder.type) {
                case Choices.REFUEL:
                    targetId = ((Refuel) holder.object).getId();
                    break;
                case Choices.SERVICE:
                    targetId = ((Service) holder.object).getId();
                    break;
                default:
                    targetId = "";
                    break;
            }
            startActivity(new Intent(History.this, Retrieve.class).putExtra(Constants.Bundle.PAGE_CHOICE, holder.type).putExtra(Constants.Bundle.ID, targetId));
        }

        private SpannableString vehicleName(Vehicle vehicle) {
            if (vehicle.name == null) {
                if (vehicle.model != null) {
                    String t = vehicle.model.name + ", " + vehicle.model.manu.name;
                    SpannableString title = new SpannableString(t);
                    title.setSpan(new RelativeSizeSpan(0.7f), vehicle.model.name.length(), title.length(), 0);
                    return title;
                } else
                    return (new SpannableString(vehicle.reg));
            } else {
                if (vehicle.name.isEmpty()) {
                    if (vehicle.model != null) {
                        String t = vehicle.model.name + ", " + vehicle.model.manu.name;
                        SpannableString title = new SpannableString(t);
                        title.setSpan(new RelativeSizeSpan(0.7f), vehicle.model.name.length(), title.length(), 0);
                        return title;
                    } else
                        return (new SpannableString(vehicle.reg));
                } else
                    return (new SpannableString(vehicle.name));
            }
        }
    }

    private class Holder {
        private int type;
        public Object object;

        public Holder(int type, Object object) {
            this.type = type;
            this.object = object;
        }
    }

    public void sync() {
        createSyncAccount();
    }

    private void createSyncAccount() {
        Account newAccount = new Account(getString(R.string.sync_account_name), getString(R.string.sync_account_type));
        AccountManager accountManager = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
            Log.e("account", "created");
            try {
                ContentResolver.setSyncAutomatically(newAccount, getString(R.string.sync_account_authority), true);
                ContentResolver.addPeriodicSync(
                        newAccount,
                        getString(R.string.sync_account_authority),
                        Bundle.EMPTY,
                        Constants.Sync.SYNC_INTERVAL);
                Log.e("sync", "interval and automatic");
            } catch (Exception e) { e.printStackTrace(); }
        } else
            Log.e("account", "exists");
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, Constants.GooglePlay.RESOLUTION_REQUEST)
                        .show();
            } else
                Log.e("Google Play", "(GCM) This device is not supported.");
            return false;
        }
        return true;
    }


    private boolean exit = false;
    @Override
    public void onBackPressed() {
        if (isFabOpen)
            animateFab();
        else {
            if (exit) {
                android.os.Process.killProcess(android.os.Process.myPid());
            } else {
                final Snackbar exitBar = Snackbar.make(History.this.findViewById(android.R.id.content), getString(R.string.message_back_exit), Snackbar.LENGTH_SHORT);
                exitBar.show();
                exit = true;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        exit = false;
                        exitBar.dismiss();
                    }
                }, 3 * 1000);

            }
        }
    }
}
