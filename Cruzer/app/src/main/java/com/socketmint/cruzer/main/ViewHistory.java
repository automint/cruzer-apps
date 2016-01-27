package com.socketmint.cruzer.main;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.*;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.socketmint.cruzer.CruzerApp;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.crud.ChoiceDialog;
import com.socketmint.cruzer.crud.Create;
import com.socketmint.cruzer.crud.CrudChoices;
import com.socketmint.cruzer.crud.Retrieve;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.dataholder.Vehicle;
import com.socketmint.cruzer.drawer.DrawerFragment;
import com.socketmint.cruzer.history.Adapter;
import com.socketmint.cruzer.manage.Choices;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.LocData;
import com.socketmint.cruzer.manage.Login;
import com.socketmint.cruzer.manage.gcm.RegistrationService;
import com.socketmint.cruzer.startup.LoginDialog;
import com.socketmint.cruzer.manage.sync.ManualSync;

public class ViewHistory extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "ViewHistory";
    private static final String ACTION_VEHICLE_LIST = "Vehicle List";
    private static final String ACTION_VEHICLE_NAME = "View Vehicle from History";
    private static final String ACTION_MANUAL_SYNC_OFFLINE = "Manual Sync Offline";
    private static final String ACTION_MANUAL_SYNC_ONLINE = "Manual Sync Online";
    private static final String ACTION_CREATE_REFUEL = "Add Refuel";
    private static final String ACTION_CREATE_SERVICE = "Add Service";
    private static final String ACTION_VIEW_REFUEL = "View Refuel";
    private static final String ACTION_VIEW_SERVICE = "View Service";

    private AppCompatTextView toolbarTitle, textOptionRefuel, textOptionService;
    private AppCompatImageView iconOptionRefuel, iconOptionService;
    private LinearLayoutCompat layoutOptionRefuel, layoutOptionService;
    private ViewPager pager;
    private Adapter adapter;
    private DrawerFragment drawerFragment;
    private ChoiceDialog choiceDialog;
    private LoginDialog loginDialog = LoginDialog.getInstance();

    private DatabaseHelper databaseHelper;
    private String vId;
    private int choice;
    private Vehicle vehicle;
    private Login login = Login.getInstance();

    private ManualSync manualSync = ManualSync.getInstance();
    public static Account account;

    private BroadcastReceiver broadcastReceiver;
    private Tracker analyticsTracker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.layout_view_history);

            analyticsTracker = ((CruzerApp) getApplication()).getAnalyticsTracker();
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    LocData locData = new LocData();
                    locData.cruzerInstance(getApplicationContext());
                    Log.d("gcm", "token sent = " + locData.gcmSentStatus());
                }
            };

            if (checkPlayServices()) {
                Intent gcmIntent = new Intent(this, RegistrationService.class);
                startService(gcmIntent);
            }

            databaseHelper = new DatabaseHelper(getApplicationContext());
            choiceDialog = new ChoiceDialog(this);
            login.initInstance(this);
            loginDialog.initInstance(this);

            if (databaseHelper.vehicleCount() == 0) {
                startActivity(new Intent(ViewHistory.this, Create.class).putExtra(Constants.Bundle.FORM_TYPE, CrudChoices.VEHICLE));
                finish();
                return;
            }

            vId = getIntent().getStringExtra(Constants.Bundle.VEHICLE_ID);
            choice = getIntent().getIntExtra(Constants.Bundle.PAGE_CHOICE, 0);

            if (login.login() > Login.LoginType.TRIAL)
                sync();

            initializeViews();
        } catch (Exception e) {
            e.printStackTrace();
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setMessage(getString(R.string.message_init_fail))
                    .setPositiveButton(getString(R.string.label_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            login.initInstance(ViewHistory.this);
                            login.logout();
                        }
                    })
                    .show();
        }
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(Constants.Bundle.PAGE_CHOICE, choice);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        choice = savedInstanceState.getInt(Constants.Bundle.PAGE_CHOICE, Choices.EXPENSES.REFUEL.ordinal());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (login.login() > Login.LoginType.TRIAL)
            androidSync();
    }

    @Override
    public void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(Constants.Gcm.ACTION_GCM_REG_COMPLETE));

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                vId = (vId != null) ? vId : "0";
                vehicle = databaseHelper.vehicle(vId);
                vehicle = (vehicle != null) ? vehicle : databaseHelper.firstVehicle();
                vId = (vId.equals("all") ? vId : vehicle.getId());
                return null;
            }

            @Override
            public void onPostExecute(Void result) {
                drawerFragment.changeVehicleCount();
                drawerFragment.changeUserName();

                adapter.setVehicleId(vId);
                pager.setAdapter(adapter);

                setContent();
            }
        }.execute();
    }

    private void initializeViews() {
        drawerFragment = (DrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        drawerFragment.setUp(R.id.navigation_drawer, (Toolbar) findViewById(R.id.toolbar), (DrawerLayout) findViewById(R.id.drawer_layout));

        toolbarTitle = (AppCompatTextView) findViewById(R.id.toolbar_title);
        textOptionRefuel = (AppCompatTextView) findViewById(R.id.text_option_refuel);
        textOptionService = (AppCompatTextView) findViewById(R.id.text_option_service);
        iconOptionRefuel = (AppCompatImageView) findViewById(R.id.icon_option_refuel);
        iconOptionService = (AppCompatImageView) findViewById(R.id.icon_option_service);
        layoutOptionRefuel = (LinearLayoutCompat) findViewById(R.id.layout_option_refuel);
        layoutOptionService = (LinearLayoutCompat) findViewById(R.id.layout_option_service);
        pager = (ViewPager) findViewById(R.id.pager_view_history);

        adapter = new Adapter(getSupportFragmentManager());
        pager.addOnPageChangeListener(pagerChangeListener);

        layoutOptionRefuel.setOnClickListener(this);
        layoutOptionService.setOnClickListener(this);
        toolbarTitle.setOnClickListener(this);
        findViewById(R.id.button_sync).setOnClickListener(this);
        findViewById(R.id.button_vehicle_list).setOnClickListener(this);
        findViewById(R.id.button_create_record).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.layout_option_refuel:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_VIEW_REFUEL).build());
                choice = Choices.EXPENSES.REFUEL.ordinal();
                setContent();
                break;
            case R.id.layout_option_service:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_VIEW_SERVICE).build());
                choice = Choices.EXPENSES.SERVICE.ordinal();
                setContent();
                break;
            case R.id.button_vehicle_list:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_VEHICLE_LIST).build());
                choiceDialog.chooseVehicle();
                break;
            case R.id.toolbar_title:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_VEHICLE_NAME).build());
                startActivity(new Intent(ViewHistory.this, Retrieve.class).putExtra(Constants.Bundle.ID, vehicle.getId()).putExtra(Constants.Bundle.PAGE_CHOICE, CrudChoices.VEHICLE));
                break;
            case R.id.button_create_record:
                if (vId.equals("all")) {
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.message_select_vehicle), Snackbar.LENGTH_SHORT).show();
                    return;
                }
                int choice;
                String CLICKED_CHOICE;
                switch (Choices.EXPENSES.values()[pager.getCurrentItem()]) {
                    case REFUEL: choice = CrudChoices.REFUEL; CLICKED_CHOICE = ACTION_CREATE_REFUEL; break;
                    case SERVICE: choice = CrudChoices.SERVICE; CLICKED_CHOICE = ACTION_CREATE_SERVICE; break;
                    default: choice = 0; CLICKED_CHOICE = "Error Occurred"; break;
                }
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(CLICKED_CHOICE).build());
                startActivity(new Intent(ViewHistory.this, Create.class).putExtra(Constants.Bundle.FORM_TYPE, choice).putExtra(Constants.Bundle.VEHICLE_ID, vId));
//                startActivity(new Intent(ViewHistory.this, com.socketmint.cruzer.crud.create.Create.class).putExtra(Constants.Bundle.PAGE_CHOICE, Choices.PAGE_EXPENSE));
                break;
            case R.id.button_sync:
                if (!login.isNetworkAvailable()) {
                    analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_MANUAL_SYNC_OFFLINE).build());
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.message_network_unavailable), Snackbar.LENGTH_SHORT).show();
                    return;
                }
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_MANUAL_SYNC_ONLINE).build());
                manualSync.initInstance(ViewHistory.this);
                manualSync.performSync();
                break;
        }
    }

    private void setContent() {
        textOptionRefuel.setText(R.string.label_refuel);
        textOptionService.setText(R.string.label_service);
        iconOptionRefuel.setImageResource(R.drawable.refuel_icon1);
        iconOptionService.setImageResource(R.drawable.service_icon1);

        if (vId.equals("all"))
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

        switch (Choices.EXPENSES.values()[choice]) {
            case REFUEL:
                refuel();
                break;
            case SERVICE:
                service();
                break;
        }

        pager.setCurrentItem(choice);
    }

    private void refuel() {
        iconOptionRefuel.setColorFilter(Color.argb(0, 0, 0, 0));
        iconOptionService.setColorFilter(ContextCompat.getColor(this, R.color.dark_v1));
        textOptionRefuel.setTextColor(ContextCompat.getColor(this, R.color.refuel_color));
        textOptionService.setTextColor(ContextCompat.getColor(this, R.color.dark_v1));
        layoutOptionRefuel.setAlpha(1f);
        layoutOptionService.setAlpha(0.5f);
    }

    private void service() {
        iconOptionService.setColorFilter(Color.argb(0, 0, 0, 0));
        iconOptionRefuel.setColorFilter(ContextCompat.getColor(this, R.color.dark_v1));
        textOptionService.setTextColor(ContextCompat.getColor(this, R.color.refuel_color));
        textOptionRefuel.setTextColor(ContextCompat.getColor(this, R.color.dark_v1));
        layoutOptionService.setAlpha(1f);
        layoutOptionRefuel.setAlpha(0.5f);
    }

    public void androidSync() {
        try {
            ContentResolver.requestSync(account, getString(R.string.sync_account_authority), new Bundle());
            Log.e(TAG, "sync requested");
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void sync() {
        account = createSyncAccount();
    }

    private Account createSyncAccount() {
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
        return newAccount;
    }

    private boolean exit = false;

    @Override
    public void onBackPressed() {
        if (exit) {
            android.os.Process.killProcess(android.os.Process.myPid());
        } else {
            Snackbar.make(ViewHistory.this.findViewById(android.R.id.content), getString(R.string.message_back_exit), Snackbar.LENGTH_SHORT).show();
            exit = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    exit = false;
                }
            }, 3 * 1000);

        }
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

    private int previousState;
    private ViewPager.OnPageChangeListener pagerChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            choice = position;
            switch (Choices.EXPENSES.values()[choice]) {
                case REFUEL:
                    refuel();
                    break;
                case SERVICE:
                    service();
                    break;
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            String CHOICE_SELECTED;
            switch (Choices.EXPENSES.values()[choice]) {
                case REFUEL: CHOICE_SELECTED = ACTION_VIEW_REFUEL; break;
                case SERVICE: CHOICE_SELECTED = ACTION_VIEW_SERVICE; break;
                default: CHOICE_SELECTED = "Error Occurred"; break;
            }
            if (previousState == ViewPager.SCROLL_STATE_DRAGGING && state == ViewPager.SCROLL_STATE_SETTLING)
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_PAGER).setAction(CHOICE_SELECTED).build());
            previousState = state;
        }
    };
}
