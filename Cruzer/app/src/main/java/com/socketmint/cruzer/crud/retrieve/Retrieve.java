package com.socketmint.cruzer.crud.retrieve;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.GoogleApiClient;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.crud.update.Update;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.main.Vehicles;
import com.socketmint.cruzer.manage.Choices;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.Login;

import java.util.Collections;

public class Retrieve extends AppCompatActivity implements View.OnClickListener, Toolbar.OnMenuItemClickListener, GoogleApiClient.ConnectionCallbacks {
    private static final String TAG = "Retrieve";
    private FloatingActionButton fabEdit;
    private Menu menu;

    private DatabaseHelper databaseHelper;
    private int choice;
    private String id;

    private Login login = Login.getInstance();
    private GoogleApiClient googleApiClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        choice = getIntent().getIntExtra(Constants.Bundle.PAGE_CHOICE, 0);
        chooseTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_retrieve);

        id = getIntent().getStringExtra(Constants.Bundle.ID);
        id = (id != null) ? id : "";
        if (choice == 0 || (id.isEmpty() && choice != Choices.USER)) {
            Snackbar.make(findViewById(android.R.id.content), R.string.message_activity_load_error, Snackbar.LENGTH_SHORT);
            onBackPressed();
        }

        login.initInstance(this);
        databaseHelper = new DatabaseHelper(getApplicationContext());

        initializeViews();
        replaceLayout();
    }

    private void initializeViews() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        toolbar.inflateMenu(R.menu.menu_retrieve);
        toolbar.setOnMenuItemClickListener(this);
        menu = toolbar.getMenu();
        fabEdit = (FloatingActionButton) findViewById(R.id.fab_edit);

        fabEdit.setOnClickListener(this);
        findViewById(R.id.fab_edit).setOnClickListener(this);
    }

    private void replaceLayout() {
        Fragment target;
        int title;
        switch (choice) {
            case Choices.WORKSHOP:
                title = R.string.title_workshop;
                target = Workshop.newInstance(id);
                workshopPermissions();
                break;
            case Choices.REFUEL:
                title = R.string.title_refuel;
                target = Refuel.newInstance(id);
                refuelPermissions();
                break;
            case Choices.SERVICE:
                title = R.string.title_service;
                target = Service.newInstance(id);
                servicePermissions();
                break;
            case Choices.VEHICLE:
                title = R.string.drawer_item_vehicle;
                target = Vehicle.newInstance(id);
                vehiclePermissions();
                break;
            case Choices.USER:
                title = R.string.title_user;
                target = User.newInstance();
                userPermissions();
                break;
            default:
                title = R.string.app_name;
                target = null;
                fabEdit.setVisibility(View.GONE);
                break;
        }
        ((AppCompatTextView) findViewById(R.id.toolbar_title)).setText(title);
        getSupportFragmentManager().beginTransaction().replace(R.id.frame_retrieve, target).commit();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab_edit:
                startActivity(new Intent(Retrieve.this, Update.class).putExtra(Constants.Bundle.PAGE_CHOICE, choice).putExtra(Constants.Bundle.ID, id));
        }
    }

    private void refuelPermissions() {
        fabEdit.setVisibility(View.VISIBLE);
        menu.findItem(R.id.item_delete).setVisible(true);
        menu.findItem(R.id.item_logout).setVisible(false);
    }

    private void userPermissions() {
        fabEdit.setVisibility(View.VISIBLE);
        menu.findItem(R.id.item_delete).setVisible(false);
        menu.findItem(R.id.item_logout).setVisible(true);
    }

    private void workshopPermissions() {
        fabEdit.setVisibility(View.GONE);
        menu.findItem(R.id.item_delete).setVisible(false);
        menu.findItem(R.id.item_logout).setVisible(false);
    }

    private void vehiclePermissions() {
        fabEdit.setVisibility(View.VISIBLE);
        menu.findItem(R.id.item_delete).setVisible(false);
        menu.findItem(R.id.item_logout).setVisible(false);
    }

    private void servicePermissions() {
        com.socketmint.cruzer.dataholder.Service service = databaseHelper.service(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{id});
        boolean allow = service.getUserId().equals(databaseHelper.user().getsId());
        fabEdit.setVisibility((allow) ? View.VISIBLE : View.GONE);
        menu.findItem(R.id.item_delete).setVisible(allow);
        menu.findItem(R.id.item_logout).setVisible(false);
    }

    private void chooseTheme() {
        int theme;
        switch (choice) {
            case Choices.VEHICLE:
                theme = R.style.AppTheme_Retrieve;
                break;
            case Choices.REFUEL:
                theme = R.style.AppTheme_Refuel;
                break;
            case Choices.SERVICE:
                theme = R.style.AppTheme_Service;
                break;
            default:
                theme = R.style.AppTheme_Retrieve;
                break;
        }
        setTheme(theme);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_delete:
                new AlertDialog.Builder(Retrieve.this)
                        .setMessage(getString(R.string.message_confirm_delete))
                        .setPositiveButton(R.string.label_yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (choice) {
                                    case Choices.VEHICLE:
                                        databaseHelper.deleteLocal(DatabaseSchema.Vehicles.TABLE_NAME, id);
                                        startActivity(new Intent(Retrieve.this, Vehicles.class));
                                        finish();
                                        return;
                                    case Choices.REFUEL:
                                        databaseHelper.deleteLocal(DatabaseSchema.Refuels.TABLE_NAME, id);
                                        break;
                                    case Choices.SERVICE:
                                        databaseHelper.deleteLocal(DatabaseSchema.Services.TABLE_NAME, id);
                                        break;
                                }
                                onBackPressed();
                            }
                        })
                        .setNegativeButton(R.string.label_no, null)
                        .show();
                break;
            case R.id.item_logout:
                new AlertDialog.Builder(Retrieve.this)
                        .setMessage(getString(R.string.message_confirm_logout))
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (login.login()) {
                                    case Login.LoginType.GOOGLE:
                                        googleApiClient = new GoogleApiClient.Builder(Retrieve.this)
                                                .addConnectionCallbacks(Retrieve.this)
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
        }
        return false;
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
