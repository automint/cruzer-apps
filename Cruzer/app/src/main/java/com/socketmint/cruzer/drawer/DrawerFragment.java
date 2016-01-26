package com.socketmint.cruzer.drawer;

import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.ListViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.GoogleApiClient;
import com.socketmint.cruzer.CruzerApp;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.crud.CrudChoices;
import com.socketmint.cruzer.crud.Retrieve;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.dataholder.User;
import com.socketmint.cruzer.main.ViewHistory;
import com.socketmint.cruzer.main.ViewVehicle;
import com.socketmint.cruzer.manage.Choices;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.Login;
import com.socketmint.cruzer.maps.WorkshopLocator;
import com.socketmint.cruzer.startup.LoginDialog;
import com.socketmint.cruzer.ui.UserInterface;

import java.util.ArrayList;
import java.util.List;

public class DrawerFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, View.OnClickListener {
    private static final String TAG = "NavigationDrawer";
    private static final String ACTION_USER_DETAILS = "View User Details";
    private static final String ACTION_VIEW_HISTORY = "View History";
    private static final String ACTION_VIEW_VEHICLE = "View Vehicles";
    private static final String ACTION_LOCATOR = "Workshop Locator";
    private static final String ACTION_LOGOUT = "Logout";
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;
    private ListViewCompat mainList, settingsList;
    private View fragmentContainerView;
    private AppCompatTextView userName;
    private LoginDialog loginDialog = LoginDialog.getInstance();
    protected UserInterface userInterface = UserInterface.getInstance();

    private int currentSelectPosition = 0;
    protected Login login = Login.getInstance();
    private DatabaseHelper databaseHelper;
    protected List<DrawerData> drawerDataList;
    protected List<DrawerData> drawerSettingsList = new ArrayList<>();

    private GoogleApiClient googleApiClient;
    private Tracker analyticsTracker;

    public DrawerFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        analyticsTracker = ((CruzerApp) getActivity().getApplication()).getAnalyticsTracker();

        if (savedInstanceState != null)
            currentSelectPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);

        userInterface.changeActivity(getActivity());
        login.initInstance(getActivity());
        loginDialog.initInstance(getActivity());
        databaseHelper = new DatabaseHelper(getActivity().getApplicationContext());

        drawerDataList = new ArrayList<>(Choices.DRAWER_ITEMS.values().length);
        drawerSettingsList = new ArrayList<>(Choices.DRAWER_SETTINGS.values().length);
        drawerDataList.add(Choices.DRAWER_ITEMS.VIEW_HISTORY.ordinal(), new DrawerData(getString(R.string.drawer_item_history), R.drawable.expense_drawer_icon, 0));
        drawerDataList.add(Choices.DRAWER_ITEMS.VIEW_VEHICLE.ordinal(), new DrawerData(getString(R.string.drawer_item_vehicle), R.drawable.vehicle_drawer_icon, databaseHelper.vehicleCount()));
        drawerDataList.add(Choices.DRAWER_ITEMS.LOCATOR.ordinal(), new DrawerData(getString(R.string.drawer_item_locator), R.drawable.ic_location_searching_black_24dp, 0));
        drawerSettingsList.add(Choices.DRAWER_SETTINGS.LOGOUT.ordinal(), new DrawerData(getString(R.string.drawer_settings_logout), R.drawable.logout_drawer_icon, 0));
    }

    @Override
    public void onResume() {
        super.onResume();
        changeVehicleCount();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.drawerbar, container, false);

            mainList = (ListViewCompat) view.findViewById(R.id.drawer_list);
            settingsList = (ListViewCompat) view.findViewById(R.id.drawer_settings);
            userName = (AppCompatTextView) view.findViewById(R.id.text_drawer_username);
            AppCompatImageView cover = (AppCompatImageView) view.findViewById(R.id.image_drawer_cover);

            mainList.setAdapter(new DrawerAdapter(drawerDataList, getActivity()));
            settingsList.setAdapter(new DrawerAdapter(drawerSettingsList, getActivity()));

            cover.setAlpha(0.6f);
            userName.setTypeface(userInterface.font(UserInterface.font.copse));
            changeUserName();
            view.findViewById(R.id.drawer_profile_box).setOnClickListener(this);
            listeners();

            mainList.setItemChecked(currentSelectPosition, true);
            return view;
        } catch (Exception e) {
            e.printStackTrace();
            new AlertDialog.Builder(getActivity())
                    .setCancelable(false)
                    .setMessage(getString(R.string.message_init_fail))
                    .setPositiveButton(getString(R.string.label_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            login.initInstance(getActivity());
                            login.logout();
                        }
                    })
                    .show();
            return null;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.drawer_profile_box:
                if (login.login() == Login.LoginType.TRIAL) {
                    loginDialog.initInstance(getActivity());
                    loginDialog.show(true);
                } else {
                    analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_USER_DETAILS).build());
                    startActivity(new Intent(getActivity(), Retrieve.class).putExtra(Constants.Bundle.ID, databaseHelper.user().getId()).putExtra(Constants.Bundle.PAGE_CHOICE, CrudChoices.USER));
                }
                break;

        }
    }

    private void listeners() {
        mainList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });
        settingsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectSettings(position);
            }
        });
    }

    public void changeVehicleCount() {
        drawerDataList.get(Choices.DRAWER_ITEMS.VIEW_VEHICLE.ordinal()).titleCount = databaseHelper.vehicleCount();
    }

    public void changeUserName() {
        User user = databaseHelper.user();
        String name = getString(R.string.text_drawer_login);
        if (user != null) {
            if (!user.firstName.isEmpty() || !user.lastName.isEmpty())
                name = user.firstName + " " + user.lastName;
            else
                name = user.mobile;
        }
        userName.setText(name);
    }

    public void setUp(int fragmentId, Toolbar toolbar, DrawerLayout drawerLayout) {
        fragmentContainerView = getActivity().findViewById(fragmentId);
        this.drawerLayout = drawerLayout;
        this.drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        drawerToggle = new ActionBarDrawerToggle(getActivity(), this.drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                changeVehicleCount();
                if (!isAdded())
                    return;
                getActivity().invalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                changeVehicleCount();
                if (!isAdded())
                    return;
                getActivity().invalidateOptionsMenu();
            }
        };

        drawerLayout.post(new Runnable() {
            @Override
            public void run() {
                drawerToggle.syncState();
            }
        });

        drawerLayout.setDrawerListener(drawerToggle);
    }

    private void selectItem(int position) {
        currentSelectPosition = position;

        if (mainList != null) {
            mainList.setItemChecked(position, true);
        }

        if (drawerLayout != null) {
            drawerLayout.closeDrawer(fragmentContainerView);
        }

        Choices.DRAWER_ITEMS drawer_items = Choices.DRAWER_ITEMS.values()[position];
        switch (drawer_items) {
            case VIEW_HISTORY:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_VIEW_HISTORY).build());
                startActivity(new Intent(getActivity(), ViewHistory.class));
                getActivity().finish();
                break;
            case VIEW_VEHICLE:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_VIEW_VEHICLE).build());
                startActivity(new Intent(getActivity(), ViewVehicle.class));
                getActivity().finish();
                break;
            case LOCATOR:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_LOCATOR).build());
                startActivity(new Intent(getActivity(), WorkshopLocator.class));
                getActivity().finish();
                break;
        }
    }

    private void selectSettings(int position) {
        if (settingsList != null)
            settingsList.setItemChecked(position, true);

        if (drawerLayout != null)
            drawerLayout.closeDrawer(fragmentContainerView);

        Choices.DRAWER_SETTINGS settings = Choices.DRAWER_SETTINGS.values()[position];
        switch (settings) {
            case LOGOUT:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_LOGOUT).build());
                new AlertDialog.Builder(getActivity())
                        .setMessage(getString(R.string.message_confirm_logout))
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (login.login()) {
                                    case Login.LoginType.GOOGLE:
                                        googleApiClient = new GoogleApiClient.Builder(getActivity())
                                                .addConnectionCallbacks(DrawerFragment.this)
                                                .addApi(Auth.GOOGLE_SIGN_IN_API)
                                                .build();
                                        googleApiClient.connect();
                                        break;
                                }
                                login.logout();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
                break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, currentSelectPosition);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Auth.GoogleSignInApi.signOut(googleApiClient);
        Log.d(TAG, "googleApi - connected");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
}
