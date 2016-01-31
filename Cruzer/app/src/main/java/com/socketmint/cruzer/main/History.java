package com.socketmint.cruzer.main;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.ListViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.crud.ChoiceDialog;
import com.socketmint.cruzer.crud.CrudChoices;
import com.socketmint.cruzer.crud.create.Create;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.dataholder.Refuel;
import com.socketmint.cruzer.dataholder.Vehicle;
import com.socketmint.cruzer.drawer.DrawerFragment;
import com.socketmint.cruzer.manage.Choices;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.LocData;
import com.socketmint.cruzer.manage.Login;
import com.socketmint.cruzer.manage.gcm.RegistrationService;
import com.socketmint.cruzer.ui.UiElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class History extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "History";

    private FloatingActionButton fabAdd, fabRefuel, fabService;
    private Animation animZoomIn, animZoomOut, animRotateForward, animRotateBackward, animSlideFromRight, animSlideToRight, animFadeOut, animFadeIn;
    private CardView cardAddService, cardAddRefuel;
    private AppCompatTextView toolbarTitle;
    private LinearLayoutCompat layoutAddActive;
    private DrawerFragment drawerFragment;

    private Adapter adapter;
    private List<Refuel> refuels = new ArrayList<>();
    private List<Holder> holders = new ArrayList<>();
    private boolean isFabOpen = false;
    private String vehicleId;
    private Vehicle vehicle;

    private DatabaseHelper databaseHelper;
    private UiElement uiElement;
    private ChoiceDialog choiceDialog;
    private Login login = Login.getInstance();
    public static Account account;

    private BroadcastReceiver broadcastReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

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
        uiElement = new UiElement(this);
        choiceDialog = new ChoiceDialog(this);
        login.initInstance(this);

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
        if (login.login() > Login.LoginType.TRIAL)
            androidSync();
        addData();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(Constants.Gcm.ACTION_GCM_REG_COMPLETE));
        drawerFragment.changeVehicleCount();
        drawerFragment.changeUserName();
    }

    private void adjustVehicleId() {
        vehicleId = (vehicleId != null) ? vehicleId : "0";
        vehicle = databaseHelper.vehicle(vehicleId);
        vehicle = (vehicle != null) ? vehicle : databaseHelper.firstVehicle();
        vehicleId = (vehicleId.equals("all") ? vehicleId : vehicle.getId());
    }

    private void initializeViews() {
        drawerFragment = (DrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        drawerFragment.setUp(R.id.navigation_drawer, (Toolbar) findViewById(R.id.toolbar), (DrawerLayout) findViewById(R.id.drawer_layout));

        fabAdd = (FloatingActionButton) findViewById(R.id.fab_add);
        fabRefuel = (FloatingActionButton) findViewById(R.id.fab_add_refuel);
        fabService = (FloatingActionButton) findViewById(R.id.fab_add_service);
        toolbarTitle = (AppCompatTextView) findViewById(R.id.toolbar_title);
        cardAddRefuel = (CardView) findViewById(R.id.card_add_refuel);
        cardAddService = (CardView) findViewById(R.id.card_add_service);
        layoutAddActive = (LinearLayoutCompat) findViewById(R.id.layout_add_active);

        adapter = new Adapter();
        ((ListViewCompat) findViewById(R.id.list_history)).setAdapter(adapter);

        fabAdd.setOnClickListener(this);
        fabRefuel.setOnClickListener(this);
        fabService.setOnClickListener(this);
        findViewById(R.id.button_vehicle_list).setOnClickListener(this);
    }

    private void initializeAssets() {
        isFabOpen = false;
        animZoomIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
        animZoomOut = AnimationUtils.loadAnimation(this, R.anim.zoom_out);
        animRotateForward = AnimationUtils.loadAnimation(this, R.anim.rotate_forward);
        animRotateBackward = AnimationUtils.loadAnimation(this, R.anim.rotate_backward);
        animSlideFromRight = AnimationUtils.loadAnimation(this, R.anim.slide_from_right);
        animSlideToRight = AnimationUtils.loadAnimation(this, R.anim.slide_to_right);
        animFadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        animFadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
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
                refuels = (vehicleId.equals("all")) ? databaseHelper.refuels() : databaseHelper.refuels(Collections.singletonList(DatabaseSchema.COLUMN_VEHICLE_ID), new String[]{vehicleId});
                Collections.sort(refuels, new Comparator<Refuel>() {
                    @Override
                    public int compare(Refuel lhs, Refuel rhs) {
                        return rhs.date.compareTo(lhs.date);
                    }
                });
                holders.clear();
                for (Refuel item : refuels) {
                    holders.add(new Holder(item.getId(), CrudChoices.REFUEL, databaseHelper.vehicle(item.getVehicleId()).reg, item.date, item.cost));
                }
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
                animateFab();
                break;
            case R.id.fab_add_refuel:
                animateFab();
                startActivity(new Intent(History.this, Create.class).putExtra(Constants.Bundle.PAGE_CHOICE, Choices.REFUEL).putExtra(Constants.Bundle.VEHICLE_ID, vehicleId));
                break;
            case R.id.fab_add_service:
                animateFab();
                startActivity(new Intent(History.this, Create.class).putExtra(Constants.Bundle.PAGE_CHOICE, Choices.SERVICE).putExtra(Constants.Bundle.VEHICLE_ID, vehicleId));
                break;
            case R.id.button_vehicle_list:
                choiceDialog.chooseVehicle();
                break;
        }
    }

    private void animateFab() {
        if(isFabOpen) {
            layoutAddActive.startAnimation(animFadeOut);
            fabService.startAnimation(animSlideToRight);
            fabRefuel.startAnimation(animSlideToRight);
            cardAddService.startAnimation(animZoomOut);
            cardAddRefuel.startAnimation(animZoomOut);
            fabAdd.startAnimation(animRotateBackward);
            fabRefuel.setClickable(false);
            fabService.setClickable(false);
            isFabOpen = false;
        } else {
            layoutAddActive.startAnimation(animFadeIn);
            fabService.startAnimation(animSlideFromRight);
            fabRefuel.startAnimation(animSlideFromRight);
            cardAddService.startAnimation(animZoomIn);
            cardAddRefuel.startAnimation(animZoomIn);
            fabAdd.startAnimation(animRotateForward);
            fabRefuel.setClickable(true);
            fabService.setClickable(true);
            isFabOpen = true;
        }
    }

    private class Adapter extends BaseAdapter {
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
            ((AppCompatTextView) view.findViewById(R.id.text_date)).setText(uiElement.date(holder.date));
            ((AppCompatTextView) view.findViewById(R.id.text_vehicle_name)).setText(holder.vehicleName);
            ((AppCompatTextView) view.findViewById(R.id.text_amount)).setText(holder.cost);
            return view;
        }
    }

    private class Holder {
        private int type;
        private String id;
        public String vehicleName, date, cost;

        public Holder(String id, int type, String vehicleName, String date, String cost) {
            this.id = id;
            this.type = type;
            this.vehicleName = vehicleName;
            this.date = date;
            this.cost = cost;
        }

        public String getId() {
            return id;
        }

        public int getType() {
            return type;
        }
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
                Snackbar.make(History.this.findViewById(android.R.id.content), getString(R.string.message_back_exit), Snackbar.LENGTH_SHORT).show();
                exit = true;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        exit = false;
                    }
                }, 3 * 1000);

            }
        }
    }
}
