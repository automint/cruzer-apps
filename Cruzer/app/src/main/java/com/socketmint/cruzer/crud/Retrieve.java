package com.socketmint.cruzer.crud;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.socketmint.cruzer.CruzerApp;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.dataholder.Problem;
import com.socketmint.cruzer.dataholder.Refuel;
import com.socketmint.cruzer.dataholder.Service;
import com.socketmint.cruzer.dataholder.User;
import com.socketmint.cruzer.dataholder.Vehicle;
import com.socketmint.cruzer.main.ViewVehicle;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.Login;
import com.socketmint.cruzer.ui.UserInterface;

import java.util.Arrays;
import java.util.List;

public class Retrieve extends AppCompatActivity {
    private static final String TAG = "Retrieve";
    private static final String SCREEN_USER = "UserDetails";
    private static final String SCREEN_VEHICLE = "VehicleDetails";
    private static final String SCREEN_REFUEL = "RefuelDetails";
    private static final String SCREEN_SERVICE = "ServiceDetails";
    private static final String ACTION_DELETE = " Delete";
    private static final String ACTION_EDIT = " Edit";
    private String SELECTED_CHOICE = "Error Occurred";

    private Toolbar toolbar;
    private AppCompatTextView txtRVehicle, txtRSubTitle, lblRField1, txtRField1, lblRField2, txtRField2, lblRField3, txtRField3, lblRField4, txtRField4, lblRField5, txtRField5, lblRField6, txtRField6, txtRetrieveType;
    private CardView layoutRField1, layoutRField2, layoutRField3, layoutRField4, layoutRField5, layoutRField6, layoutRMF;
    private AppCompatImageButton btnBack, btnDelete;
    private FloatingActionButton btnEdit;
    private UserInterface userInterface = UserInterface.getInstance();
    private DatabaseHelper databaseHelper;
    private String id;
    private int choice;
    private Login login = Login.getInstance();

    private Tracker analyticsTracker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_retrieve);

        analyticsTracker = ((CruzerApp) getApplication()).getAnalyticsTracker();

        login.initInstance(this);
        userInterface.changeActivity(this);
        databaseHelper = new DatabaseHelper(getApplicationContext());
        mapViews();
        setFonts();

        id = getIntent().getStringExtra(Constants.Bundle.ID);
        choice = getIntent().getIntExtra(Constants.Bundle.PAGE_CHOICE, 0);
        if (id == null || id.isEmpty() || choice == 0) {
            Snackbar.make(findViewById(android.R.id.content), R.string.message_activity_load_error, Snackbar.LENGTH_SHORT);
            onBackPressed();
        }
        hideAllLayouts();
        setContent();
        clickListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            setContent();
        } catch (Exception e) { Log.e(TAG, "can not set content"); }
    }

    private void setContent() {
        layoutRMF.setVisibility(View.VISIBLE);
        txtRVehicle.setVisibility(View.VISIBLE);
        switch (choice) {
            case CrudChoices.VEHICLE:
                SELECTED_CHOICE = SCREEN_VEHICLE;
                txtRetrieveType.setText(R.string.label_vehicle);
                vehicle();
                break;
            case CrudChoices.REFUEL:
                SELECTED_CHOICE = SCREEN_REFUEL;
                txtRetrieveType.setText(R.string.label_refuel);
                refuel();
                break;
            case CrudChoices.SERVICE:
                SELECTED_CHOICE = SCREEN_SERVICE;
                txtRetrieveType.setText(R.string.label_service);
                btnDelete.setVisibility(View.GONE);
                btnEdit.setVisibility(View.GONE);
                service();
                break;
            case CrudChoices.USER:
                SELECTED_CHOICE = SCREEN_USER;
                txtRetrieveType.setText(R.string.label_user);
                btnDelete.setVisibility(View.GONE);
                user();
                break;
        }
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(SELECTED_CHOICE + ACTION_EDIT).build());
                startActivity(new Intent(Retrieve.this, Update.class).putExtra(Constants.Bundle.ID, id).putExtra(Constants.Bundle.PAGE_CHOICE, choice));
            }
        });
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(SELECTED_CHOICE + ACTION_DELETE).build());
                new AlertDialog.Builder(Retrieve.this)
                        .setMessage(getString(R.string.message_confirm_delete))
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (choice) {
                                    case CrudChoices.VEHICLE:
//                                        databaseHelper.deleteLVehicle(id);                                [[ CHECK WITH LIVE UPDATE BEFORE DELETING ]]
                                        databaseHelper.deleteLocal(DatabaseSchema.Vehicles.TABLE_NAME, id);
                                        startActivity(new Intent(Retrieve.this, ViewVehicle.class));
                                        finish();
                                        return;
                                    case CrudChoices.REFUEL:
                                        databaseHelper.deleteLocal(DatabaseSchema.Refuels.TABLE_NAME, id);
                                        break;
                                    case CrudChoices.SERVICE:
                                        databaseHelper.deleteLocal(DatabaseSchema.Services.TABLE_NAME, id);
                                        break;
                                }
                                onBackPressed();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });
    }

    private void vehicle() {
        analyticsTracker.setScreenName(SCREEN_VEHICLE);
        analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());
        Vehicle vehicle = databaseHelper.vehicle(id);
        SpannableString vName = new SpannableString(vehicle.name);
        if (!vehicle.name.isEmpty())
            vName.setSpan(new RelativeSizeSpan(1.5f), 0, vehicle.name.length(), 0);
        else
            txtRVehicle.setVisibility(View.GONE);
        txtRVehicle.setText(vName);
        try {
            String subtitle = vehicle.model.name + ", " + vehicle.model.manu.name;
            SpannableString string = new SpannableString(subtitle);
            if (vehicle.name.isEmpty())
                string.setSpan(new RelativeSizeSpan(1.5f), 0, vehicle.model.name.length(), 0);
            txtRSubTitle.setText(string);
            txtRSubTitle.setVisibility(View.VISIBLE);
        } catch (Exception e) { Log.e(TAG, "can not set subtitle"); }
        if (vehicle.name.isEmpty() && vehicle.model == null)
            layoutRMF.setVisibility(View.GONE);
        lblRField1.setText(R.string.label_vehicle_reg);
        txtRField1.setText(vehicle.reg);
        if (!vehicle.reg.isEmpty())
            layoutRField1.setVisibility(View.VISIBLE);
    }

    private void user() {
        analyticsTracker.setScreenName(SCREEN_USER);
        analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());
        User user = databaseHelper.user();
        String name = user.firstName + " " + user.lastName;
        txtRVehicle.setText(name);
        if (user.firstName.isEmpty() && user.lastName.isEmpty())
            layoutRMF.setVisibility(View.GONE);
        lblRField1.setText(getString(R.string.label_phone_number));
        txtRField1.setText(user.mobile);
        if (!user.mobile.isEmpty())
            layoutRField1.setVisibility(View.VISIBLE);
        lblRField2.setText(getString(R.string.label_email));
        txtRField2.setText(user.email);
        if (!user.email.isEmpty())
            layoutRField2.setVisibility(View.VISIBLE);
    }

    private void refuel() {
        analyticsTracker.setScreenName(SCREEN_REFUEL);
        analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());
        Refuel refuel = databaseHelper.refuel(Arrays.asList(DatabaseSchema.COLUMN_ID), new String[]{id});
        Vehicle vehicle = databaseHelper.vehicle(refuel.getVehicleId());
        txtRVehicle.setText(vehicleName(vehicle));
        String rate = "Rs. " + refuel.rate + " /ltr";
        txtRSubTitle.setText(rate);
        lblRField1.setText(R.string.label_cost);
        String cost = "Rs. " + refuel.cost;
        txtRField1.setText(cost);
        lblRField2.setText(R.string.label_date);
        txtRField2.setText(userInterface.date(refuel.date));
        lblRField3.setText(R.string.label_refuel_volume);
        String volume = refuel.volume + " ltr";
        txtRField3.setText(volume);
        lblRField4.setText(R.string.label_odometer_reading);
        String odo = refuel.odo + " km.";
        txtRField4.setText(odo);
        if (!refuel.rate.isEmpty())
            txtRSubTitle.setVisibility(View.VISIBLE);
        if (!refuel.cost.isEmpty())
            layoutRField1.setVisibility(View.VISIBLE);
        if (!refuel.date.isEmpty())
            layoutRField2.setVisibility(View.VISIBLE);
        if (!refuel.volume.isEmpty())
            layoutRField3.setVisibility(View.VISIBLE);
        if (!refuel.odo.isEmpty())
            layoutRField4.setVisibility(View.VISIBLE);
    }

    private void service() {
        analyticsTracker.setScreenName(SCREEN_SERVICE);
        analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());
        Service service = databaseHelper.service(Arrays.asList(DatabaseSchema.COLUMN_ID), new String[]{id});
        Vehicle vehicle = databaseHelper.vehicle(service.getVehicleId());
        txtRVehicle.setText(vehicleName(vehicle));
        try {
            txtRSubTitle.setText(databaseHelper.workshop(Arrays.asList(DatabaseSchema.COLUMN_ID), new String[]{service.getWorkshopId()}).name);
            txtRSubTitle.setVisibility(View.VISIBLE);
        } catch (Exception e) { Log.e(TAG, "can not find workshop"); }
        lblRField1.setText(R.string.label_cost);
        String cost = "Rs. " + service.cost;
        txtRField1.setText(cost);
        lblRField2.setText(R.string.label_date);
        txtRField2.setText(userInterface.date(service.date));
        lblRField3.setText(R.string.label_odometer_reading);
        String odo = service.odo + " km.";
        txtRField3.setText(odo);
        lblRField5.setText(R.string.label_details);
        txtRField5.setText(service.details);
        txtRField5.setTextAppearance(this, android.R.style.TextAppearance_Small);
        setFonts();
        txtRField5.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
        lblRField6.setVisibility(View.GONE);
        String status;
        if (service.status != null) {
            switch (service.status) {
                case "4":
                    status = "Payment Due";
                    break;
                case "5":
                    status = "Bill Paid";
                    break;
                default:
                    status = "";
                    break;
            }
        } else
            status = "";
        txtRField6.setText(status);
        List<Problem> problemList = databaseHelper.problems(Arrays.asList(DatabaseSchema.Problems.COLUMN_SERVICE_ID), new String[]{service.getId()});
        if (problemList != null) {
            lblRField4.setText(R.string.label_problems);
            String problems = "";
            for (int i = 0; i < problemList.size(); i++) {
                Problem item = problemList.get(i);
                int lCost = 0;
                int pCost = 0;
                try {
                    lCost = Integer.parseInt(item.lCost);
                } catch (NumberFormatException e) { Log.d(TAG, "number format"); }
                try {
                    pCost = Integer.parseInt(item.pCost);
                } catch (NumberFormatException e) { Log.d(TAG, "number format"); }
                int total = lCost + pCost;
                problems = problems.concat((i + 1) + ". " + item.details + (!item.qty.isEmpty() ? " <small><i>(" + item.qty + ")</i></small>, " : ", ") +  "<small>Rs.</small>" + total + "<br>");
            }
            txtRField4.setText(Html.fromHtml(problems));
            if (!problemList.isEmpty())
                layoutRField4.setVisibility(View.VISIBLE);
        }
        if (!service.cost.isEmpty())
            layoutRField1.setVisibility(View.VISIBLE);
        if (!service.date.isEmpty())
            layoutRField2.setVisibility(View.VISIBLE);
        if (!service.odo.isEmpty())
            layoutRField3.setVisibility(View.VISIBLE);
        if (!service.details.isEmpty())
            layoutRField5.setVisibility(View.VISIBLE);
        if (!status.isEmpty())
            layoutRField6.setVisibility(View.VISIBLE);
    }

    private SpannableString vehicleName(Vehicle vehicle) {
        SpannableString title;
        if (vehicle.name == null) {
            if (vehicle.model != null) {
                String t = vehicle.model.name + ", " + vehicle.model.manu.name + "";
                title = new SpannableString(t);
                title.setSpan(new RelativeSizeSpan(1.5f), 0, vehicle.model.name.length(), 0);
            } else {
                title = new SpannableString(vehicle.reg);
                title.setSpan(new RelativeSizeSpan(1.5f), 0, vehicle.reg.length(), 0);
            }
        } else {
            if (vehicle.name.isEmpty()) {
                if (vehicle.model != null) {
                    String t = vehicle.model.name + ", " + vehicle.model.manu.name + "";
                    title = new SpannableString(t);
                    title.setSpan(new RelativeSizeSpan(1.5f), 0, vehicle.model.name.length(), 0);
                } else {
                    title = new SpannableString(vehicle.reg);
                    title.setSpan(new RelativeSizeSpan(1.5f), 0, vehicle.reg.length(), 0);
                }
            } else {
                title = new SpannableString(vehicle.name);
                title.setSpan(new RelativeSizeSpan(1.5f), 0, vehicle.name.length(), 0);
            }
        }
        return title;
    }

    private void hideAllLayouts() {
        txtRSubTitle.setVisibility(View.GONE);
        layoutRField1.setVisibility(View.GONE);
        layoutRField2.setVisibility(View.GONE);
        layoutRField3.setVisibility(View.GONE);
        layoutRField4.setVisibility(View.GONE);
        layoutRField5.setVisibility(View.GONE);
        layoutRField6.setVisibility(View.GONE);
    }

    private void mapViews() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        txtRVehicle = (AppCompatTextView) findViewById(R.id.txt_retrieve_vehicle);
        txtRSubTitle = (AppCompatTextView) findViewById(R.id.txt_retrieve_sub_title);
        lblRField1 = (AppCompatTextView) findViewById(R.id.lbl_retrieve_field_1);
        lblRField2 = (AppCompatTextView) findViewById(R.id.lbl_retrieve_field_2);
        lblRField3 = (AppCompatTextView) findViewById(R.id.lbl_retrieve_field_3);
        lblRField4 = (AppCompatTextView) findViewById(R.id.lbl_retrieve_field_4);
        lblRField5 = (AppCompatTextView) findViewById(R.id.lbl_retrieve_field_5);
        lblRField6 = (AppCompatTextView) findViewById(R.id.lbl_retrieve_field_6);
        txtRField1 = (AppCompatTextView) findViewById(R.id.txt_retrieve_field_1);
        txtRField2 = (AppCompatTextView) findViewById(R.id.txt_retrieve_field_2);
        txtRField3 = (AppCompatTextView) findViewById(R.id.txt_retrieve_field_3);
        txtRField4 = (AppCompatTextView) findViewById(R.id.txt_retrieve_field_4);
        txtRField5 = (AppCompatTextView) findViewById(R.id.txt_retrieve_field_5);
        txtRField6 = (AppCompatTextView) findViewById(R.id.txt_retrieve_field_6);
        txtRetrieveType = (AppCompatTextView) findViewById(R.id.txt_retrieve_type);
        layoutRField1 = (CardView) findViewById(R.id.layout_retrieve_field_1);
        layoutRField2 = (CardView) findViewById(R.id.layout_retrieve_field_2);
        layoutRField3 = (CardView) findViewById(R.id.layout_retrieve_field_3);
        layoutRField4 = (CardView) findViewById(R.id.layout_retrieve_field_4);
        layoutRField5 = (CardView) findViewById(R.id.layout_retrieve_field_5);
        layoutRField6 = (CardView) findViewById(R.id.layout_retrieve_field_6);
        layoutRMF = (CardView) findViewById(R.id.layout_retrieve_main);
        btnBack = (AppCompatImageButton) findViewById(R.id.button_back);
        btnEdit = (FloatingActionButton) findViewById(R.id.button_edit);
        btnDelete = (AppCompatImageButton) findViewById(R.id.button_delete);
    }

    private void setFonts() {
        txtRVehicle.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
        txtRSubTitle.setTypeface(userInterface.font(UserInterface.font.roboto_light));
        lblRField1.setTypeface(userInterface.font(UserInterface.font.roboto_light_italic));
        lblRField2.setTypeface(userInterface.font(UserInterface.font.roboto_light_italic));
        lblRField3.setTypeface(userInterface.font(UserInterface.font.roboto_light_italic));
        lblRField4.setTypeface(userInterface.font(UserInterface.font.roboto_light_italic));
        lblRField5.setTypeface(userInterface.font(UserInterface.font.roboto_light_italic));
        lblRField6.setTypeface(userInterface.font(UserInterface.font.roboto_light_italic));
        txtRField1.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
        txtRField2.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
        txtRField3.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
        txtRField4.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
        txtRField5.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
        txtRField6.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
        txtRetrieveType.setTypeface(userInterface.font(UserInterface.font.roboto_medium));
    }

    private void clickListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }
}
