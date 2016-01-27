package com.socketmint.cruzer.main;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.*;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.socketmint.cruzer.CruzerApp;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.crud.Create;
import com.socketmint.cruzer.crud.CrudChoices;
import com.socketmint.cruzer.crud.Retrieve;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.dataholder.Vehicle;
import com.socketmint.cruzer.drawer.DrawerFragment;
import com.socketmint.cruzer.manage.Choices;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.Login;

import java.util.ArrayList;
import java.util.List;

public class ViewVehicle extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "ViewVehicle";
    private static final String ACTION_VEHICLE_ADD = "Add Vehicle";
    private static final String ACTION_VEHICLE_HISTORY = "Vehicle History";
    private static final String ACTION_VEHICLE_VIEW = "View Vehicle";

    private DrawerFragment drawerFragment;
    private Adapter adapter;

    private Login login = Login.getInstance();
    private DatabaseHelper databaseHelper;
    private List<Vehicle> vehicles;

    private Tracker analyticsTracker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.layout_view_vehicle);

            analyticsTracker = ((CruzerApp) getApplication()).getAnalyticsTracker();

            login.initInstance(this);
            databaseHelper = new DatabaseHelper(getApplicationContext());

            if (databaseHelper.vehicleCount() == 0) {
                startActivity(new Intent(ViewVehicle.this, Create.class).putExtra(Constants.Bundle.FORM_TYPE, CrudChoices.VEHICLE));
                finish();
                return;
            }

            initializeViews();
        } catch (Exception e) {
            e.printStackTrace();
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setMessage(getString(R.string.message_init_fail))
                    .setPositiveButton(getString(R.string.label_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            login.initInstance(ViewVehicle.this);
                            login.logout();
                        }
                    })
                    .show();
        }
    }

    private void androidSync() {
        try {
            ContentResolver.requestSync(ViewHistory.account, getString(R.string.sync_account_authority), new Bundle());
            Log.e(TAG, "sync requested");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void onStart() {
        super.onStart();
        analyticsTracker.setScreenName(TAG);
        analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());
        if (login.login() > Login.LoginType.TRIAL)
            androidSync();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (databaseHelper.vehicleCount() == 0) {
            startActivity(new Intent(ViewVehicle.this, Create.class).putExtra(Constants.Bundle.FORM_TYPE, CrudChoices.VEHICLE));
            finish();
            return;
        }

        addData();
        drawerFragment.changeVehicleCount();
        drawerFragment.changeUserName();
    }

    private void addData() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    vehicles = databaseHelper.vehicles();
                } catch (Exception e) {
                    e.printStackTrace();

                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.message_no_vehicles), Snackbar.LENGTH_INDEFINITE)
                            .setAction(getString(R.string.label_add), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    startActivity(new Intent(ViewVehicle.this, Create.class).putExtra(Constants.Bundle.FORM_TYPE, CrudChoices.VEHICLE));
                                    finish();
                                }
                            }).show();
                }
                return null;
            }
            @Override
            protected void onPostExecute(Void result) {
                adapter.animateTo(vehicles);
                super.onPostExecute(result);
            }
        }.execute();
    }

    private void initializeViews() {
        drawerFragment = (DrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        drawerFragment.setUp(R.id.navigation_drawer, (Toolbar) findViewById(R.id.toolbar), (DrawerLayout) findViewById(R.id.drawer_layout));

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view_vehicle);

        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new Adapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        findViewById(R.id.fab_add).setOnClickListener(this);
        SpannableString title;
        String yv = "Your Vehicle";
        title = new SpannableString(yv.concat("(s)"));
        title.setSpan(new RelativeSizeSpan(1.5f), 0, yv.length(), 0);
        ((AppCompatTextView) findViewById(R.id.toolbar_title)).setText(title);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab_add:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_VEHICLE_ADD).build());
//                startActivity(new Intent(ViewVehicle.this, Create.class).putExtra(Constants.Bundle.FORM_TYPE, CrudChoices.VEHICLE));
                startActivity(new Intent(ViewVehicle.this, com.socketmint.cruzer.crud.create.Create.class).putExtra(Constants.Bundle.PAGE_CHOICE, Choices.PAGE_VEHICLE));
                break;
        }
    }

    public class Adapter extends RecyclerView.Adapter<Holder> implements View.OnClickListener {
        private List<Vehicle> vehicles = new ArrayList<>();

        public Adapter() { }

        public void animateTo(List<Vehicle> items) {
            applyAndAnimateRemovals(items);
            applyAndAnimateAdditions(items);
            applyAndAnimateMovedItems(items);
        }

        private void applyAndAnimateRemovals(List<Vehicle> newItems) {
            for (int i = vehicles.size() - 1; i >= 0; i--) {
                final Vehicle model = vehicles.get(i);
                if (!newItems.contains(model)) {
                    removeItem(i);
                }
            }
        }

        private void applyAndAnimateAdditions(List<Vehicle> newItems) {
            for (int i = 0, count = newItems.size(); i < count; i++) {
                final Vehicle model = newItems.get(i);
                if (!vehicles.contains(model)) {
                    addItem(i, model);
                }
            }
        }

        private void applyAndAnimateMovedItems(List<Vehicle> newItems) {
            for (int toPosition = newItems.size() - 1; toPosition >= 0; toPosition--) {
                final Vehicle model = newItems.get(toPosition);
                final int fromPosition = vehicles.indexOf(model);
                if (fromPosition >= 0 && fromPosition != toPosition) {
                    moveItem(fromPosition, toPosition);
                }
            }
        }

        public Vehicle removeItem(int position) {
            final Vehicle model = vehicles.remove(position);
            notifyItemRemoved(position);
            return model;
        }

        public void addItem(int position, Vehicle item) {
            vehicles.add(position, item);
            notifyItemInserted(position);
        }

        public void moveItem(int fromPosition, int toPosition) {
            final Vehicle model = vehicles.remove(fromPosition);
            vehicles.add(toPosition, model);
            notifyItemMoved(fromPosition, toPosition);
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_vehicle, parent, false);

            final Holder holder = new Holder(view);

            holder.itemView.setTag(holder);
            holder.itemView.setOnClickListener(this);

            return holder;
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            final Vehicle vehicle = vehicles.get(position);

            holder.txtVehicleName.setText(vehicleName(vehicle));
            holder.btnHistory.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_VEHICLE_HISTORY).build());
                    startActivity(new Intent(ViewVehicle.this, ViewHistory.class).putExtra(Constants.Bundle.VEHICLE_ID, vehicle.getId()));
                }
            });
        }

        @Override
        public int getItemCount() {
            return vehicles.size();
        }

        @Override
        public void onClick(View v) {
            final Holder holder = (Holder) v.getTag();
            analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_VEHICLE_VIEW).build());
            startActivity(new Intent(ViewVehicle.this, Retrieve.class).putExtra(Constants.Bundle.ID, vehicles.get(holder.getAdapterPosition()).getId()).putExtra(Constants.Bundle.PAGE_CHOICE, CrudChoices.VEHICLE));
        }

        /**
         * It returns vehicle name in relative spannable format.
         * If vehicle has a name assigned to it
         *      ? return name
         *      : if vehicle has model assigned to it
         *          ? return model name followed by manufacturer name in small text
         *          : return registration number
         * @param vehicle as current vehicle
         * @return identifier of given vehicle
         */

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

    public class Holder extends RecyclerView.ViewHolder {
        private AppCompatTextView txtVehicleName;
        private AppCompatImageButton btnHistory;
        public Holder(View itemView) {
            super(itemView);
            txtVehicleName = (AppCompatTextView) itemView.findViewById(R.id.text_vehicle_name);
            btnHistory = (AppCompatImageButton) itemView.findViewById(R.id.button_view_history);
        }
    }

    private boolean exit = false;

    @Override
    public void onBackPressed() {
        if (exit) {
            android.os.Process.killProcess(android.os.Process.myPid());
        } else {
            Snackbar.make(ViewVehicle.this.findViewById(android.R.id.content), getString(R.string.message_back_exit), Snackbar.LENGTH_SHORT).show();
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
