package com.socketmint.cruzer.crud;

import android.Manifest;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.os.Process;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.ContentFrameLayout;
import android.support.v7.widget.LinearLayoutCompat;
import android.text.Editable;
import android.text.Html;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.socketmint.cruzer.CruzerApp;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.dataholder.Manu;
import com.socketmint.cruzer.dataholder.Model;
import com.socketmint.cruzer.dataholder.Workshop;
import com.socketmint.cruzer.main.ViewHistory;
import com.socketmint.cruzer.manage.Amazon;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.LocData;
import com.socketmint.cruzer.manage.Login;
import com.socketmint.cruzer.ui.UiElement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Create extends AppCompatActivity {
    private static final String TAG = "Create";
    private static final String SCREEN_VEHICLE = "AddVehicle";
    private static final String SCREEN_REFUEL = "AddRefuel";
    private static final String SCREEN_SERVICE = "AddService";
    private static final String ACTION_ADD = "Done";
    private static final String ACTION_DATE = " Date";
    private static final String ACTION_UPLOAD_PHOTO = "Upload Receipt";
    private static final String ACTION_EXISTING_PHOTO = "Existing Receipt";
    private static final String ACTION_FORM_EXPAND = " Expand Form";
    private static final String ACTION_FORM_COLLAPSE = " Collapse Form";
    private static final String ACTION_CHOICE_REFUEL = "Add Refuel";
    private static final String ACTION_CHOICE_SERVICE = "Add Service";
    private UiElement uiElement = UiElement.getInstance();
    private AppCompatTextView txtMainField, txtField1, txtField2, txtField3, txtField4, txtFirstVehicle, txtOption1, txtOption2;
    private AppCompatEditText editMainField, editField1, editField2, editField3, editField4;
    private AppCompatImageButton imgBtnField1, imgBtnField2, imgBtnField3, imgBtnField4;
    private LinearLayoutCompat layoutOptions, layoutOption1, layoutOption2,  linearField1, linearField2, linearField3, linearField4, layoutPhoto, linearProblemListing, layoutProblems;
    private AppCompatImageView imgOption1, imgOption2;
    private ContentFrameLayout imgBottom;
    private AppCompatButton btnFormControl, btnDone, btnAddProblem;
    private FloatingActionButton fabUploadPhoto;
    private int what;
    private DatabaseHelper databaseHelper;
    private ChoiceDialog choiceDialog = ChoiceDialog.getInstance();
    private LocData locData = new LocData();
    private Login login = Login.getInstance();
    private Amazon amazon = Amazon.getInstance();
    private InputFilter[] filterF1, filterF2, filterF3, filterF4, filterMF;
    private String vehicleId, fileName;
    private File imageFile;

    private Tracker analyticsTracker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_create);

        analyticsTracker = ((CruzerApp) getApplication()).getAnalyticsTracker();

        uiElement.changeActivity(this);
        databaseHelper = new DatabaseHelper(getApplicationContext());
        choiceDialog.initInstance(this);
        locData.formInstance(this);
        login.initInstance(this);
        amazon.initInstance(this);

        mapViews();

        try {
            what = getIntent().getIntExtra(Constants.Bundle.FORM_TYPE, 0);
        } catch (Exception e) {
            Snackbar.make(findViewById(android.R.id.content), R.string.message_activity_load_error, Snackbar.LENGTH_SHORT);
            onBackPressed();
        }

        locData.clearData();
        controlLayouts(CrudChoices.COLLAPSE_ALL);
        setFields(what);
    }

    private void mapViews() {
        txtMainField = (AppCompatTextView) findViewById(R.id.txt_main_field);
        txtField1 = (AppCompatTextView) findViewById(R.id.txt_field_1);
        txtField2 = (AppCompatTextView) findViewById(R.id.txt_field_2);
        txtField3 = (AppCompatTextView) findViewById(R.id.txt_field_3);
        txtField4 = (AppCompatTextView) findViewById(R.id.txt_field_4);
        txtOption1 = (AppCompatTextView) findViewById(R.id.txt_option_1);
        txtOption2 = (AppCompatTextView) findViewById(R.id.txt_option_2);
        txtFirstVehicle = (AppCompatTextView) findViewById(R.id.txt_first_vehicle);
        editMainField = (AppCompatEditText) findViewById(R.id.edit_main_field);
        editField1 = (AppCompatEditText) findViewById(R.id.edit_field_1);
        editField2 = (AppCompatEditText) findViewById(R.id.edit_field_2);
        editField3 = (AppCompatEditText) findViewById(R.id.edit_field_3);
        editField4 = (AppCompatEditText) findViewById(R.id.edit_field_4);
        linearField1 = (LinearLayoutCompat) findViewById(R.id.linear_field_1);
        linearField2 = (LinearLayoutCompat) findViewById(R.id.linear_field_2);
        linearField3 = (LinearLayoutCompat) findViewById(R.id.linear_field_3);
        linearField4 = (LinearLayoutCompat) findViewById(R.id.linear_field_4);
        linearProblemListing = (LinearLayoutCompat) findViewById(R.id.linear_problem_listing);
        layoutPhoto = (LinearLayoutCompat) findViewById(R.id.layout_upload_photo);
        layoutOptions = (LinearLayoutCompat) findViewById(R.id.create_layout_options);
        layoutOption1 = (LinearLayoutCompat) findViewById(R.id.create_option_1);
        layoutOption2 = (LinearLayoutCompat) findViewById(R.id.create_option_2);
        layoutProblems = (LinearLayoutCompat) findViewById(R.id.layout_problems);
        imgBtnField1 = (AppCompatImageButton) findViewById(R.id.field_1_list);
        imgBtnField2 = (AppCompatImageButton) findViewById(R.id.field_2_list);
        imgBtnField3 = (AppCompatImageButton) findViewById(R.id.field_3_list);
        imgBtnField4 = (AppCompatImageButton) findViewById(R.id.field_4_list);
        btnAddProblem = (AppCompatButton) findViewById(R.id.ib_add_problem);
        imgOption1 = (AppCompatImageView) findViewById(R.id.icn_option_1);
        imgOption2 = (AppCompatImageView) findViewById(R.id.icn_option_2);
        imgBottom = (ContentFrameLayout) findViewById(R.id.image_create_bottom);
        fabUploadPhoto = (FloatingActionButton) findViewById(R.id.fab_upload_photo);
        btnFormControl = (AppCompatButton) findViewById(R.id.button_form_control);
        btnDone = (AppCompatButton) findViewById(R.id.button_create_record);
    }

    private void hideDetailButtons() {
        imgBtnField1.setVisibility(View.GONE);
        imgBtnField2.setVisibility(View.GONE);
        imgBtnField3.setVisibility(View.GONE);
        imgBtnField4.setVisibility(View.GONE);
        editField1.setFocusable(true);
        editField2.setFocusable(true);
        editField3.setFocusable(true);
        editField4.setFocusable(true);
    }

    private void controlLayouts(int choice) {
        switch (choice) {
            case CrudChoices.COLLAPSE_ALL:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_FORM_COLLAPSE).build());
                linearField1.setVisibility(View.GONE);
                linearField2.setVisibility(View.GONE);
                linearField3.setVisibility(View.GONE);
                linearField4.setVisibility(View.GONE);
                linearProblemListing.setVisibility(View.GONE);
                imgBottom.setVisibility(View.VISIBLE);
                break;
            case CrudChoices.EXPAND_VEHICLE:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(SCREEN_VEHICLE + ACTION_FORM_EXPAND).build());
                linearField1.setVisibility(View.VISIBLE);
                linearField2.setVisibility(View.VISIBLE);
                linearField3.setVisibility(View.VISIBLE);
                linearField4.setVisibility(View.GONE);
                linearProblemListing.setVisibility(View.GONE);
                imgBottom.setVisibility(View.GONE);
                break;
            case CrudChoices.EXPAND_REFUEL:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(SCREEN_REFUEL + ACTION_FORM_EXPAND).build());
                linearField1.setVisibility(View.VISIBLE);
                linearField2.setVisibility(View.VISIBLE);
                linearField3.setVisibility(View.VISIBLE);
                linearField4.setVisibility(View.VISIBLE);
                linearProblemListing.setVisibility(View.GONE);
                imgBottom.setVisibility(View.GONE);
                break;
            case CrudChoices.EXPAND_SERVICE:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(SCREEN_SERVICE + ACTION_FORM_EXPAND).build());
                linearField1.setVisibility(View.VISIBLE);
                linearField2.setVisibility(View.GONE);
                linearField3.setVisibility(View.VISIBLE);
                linearField4.setVisibility(View.VISIBLE);
                linearProblemListing.setVisibility(View.GONE);
                imgBottom.setVisibility(View.GONE);
                break;
        }
    }

    private void setInputFilter() {
        filterF1 = new InputFilter[1];
        filterF2 = new InputFilter[1];
        filterF3 = new InputFilter[1];
        filterF4 = new InputFilter[1];
        filterMF= new InputFilter[1];
    }

    private void removeTextWatchers() {
        editField1.removeTextChangedListener(companyChange);
        editField2.removeTextChangedListener(field2Watcher);
        editField3.removeTextChangedListener(field3Watcher);
    }

    private void setFields(final int what) {
        layoutPhoto.setVisibility(View.GONE);
        hideDetailButtons();
        setInputFilter();
        removeTextWatchers();
        editField1.setFocusable(true);
        editField2.setFocusable(true);
        btnFormControl.setVisibility(View.VISIBLE);
        if (btnFormControl.getText().toString().equals(getString(R.string.label_collapse_form))) {
            switch (what) {
                case CrudChoices.VEHICLE:
                    controlLayouts(CrudChoices.EXPAND_VEHICLE);
                    break;
                case CrudChoices.REFUEL:
                    controlLayouts(CrudChoices.EXPAND_REFUEL);
                    break;
                case CrudChoices.SERVICE:
                    controlLayouts(CrudChoices.EXPAND_SERVICE);
                    break;
            }
        }
        txtFirstVehicle.setVisibility(View.GONE);
        editMainField.removeTextChangedListener(mainFieldWatcher);
        switch (what) {
            case CrudChoices.VEHICLE:
                vehicle();
                break;
            case CrudChoices.REFUEL:
                refuel();
                break;
            case CrudChoices.SERVICE:
                service();
                break;
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                btnFormControl.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (btnFormControl.getText().toString().equals(getString(R.string.label_collapse_form))) {
                            if (what == CrudChoices.SERVICE)
                                layoutPhoto.setVisibility(View.GONE);
                            else
                                layoutPhoto.setVisibility(View.GONE);
                            controlLayouts(CrudChoices.COLLAPSE_ALL);
                            btnFormControl.setText(R.string.label_expand_form);
                            return;
                        }
                        if (btnFormControl.getText().toString().equals(getString(R.string.label_expand_form))) {
                            layoutPhoto.setVisibility(View.GONE);
                            switch (what) {
                                case CrudChoices.VEHICLE:
                                    controlLayouts(CrudChoices.EXPAND_VEHICLE);
                                    break;
                                case CrudChoices.REFUEL:
                                    controlLayouts(CrudChoices.EXPAND_REFUEL);
                                    break;
                                case CrudChoices.SERVICE:
                                    controlLayouts(CrudChoices.EXPAND_SERVICE);
                                    break;
                            }
                            btnFormControl.setText(R.string.label_collapse_form);
                        }
                    }
                });
                layoutOption1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_CHOICE_REFUEL).build());
                        setFields(CrudChoices.REFUEL);
                    }
                });
                layoutOption2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_CHOICE_SERVICE).build());
                        setFields(CrudChoices.SERVICE);
                    }
                });
            }
        }, 1);
    }

    private void vehicle() {
        analyticsTracker.setScreenName(SCREEN_VEHICLE);
        analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());
        btnFormControl.setVisibility(View.GONE);
        controlLayouts(CrudChoices.EXPAND_VEHICLE);
        layoutOptions.setVisibility(View.GONE);
        txtFirstVehicle.setVisibility(View.VISIBLE);
        txtMainField.setText(Html.fromHtml(getString(R.string.label_vehicle_reg) + "<sup>*</sup>"));
        txtField1.setText(getString(R.string.label_vehicle_manu));
        txtField2.setText(getString(R.string.label_vehicle_model));
        txtField3.setText(R.string.label_vehicle_name);
        editField1.setFocusable(false);
        editField2.setFocusable(false);
        imgBtnField1.setVisibility(View.VISIBLE);
        imgBtnField2.setVisibility(View.VISIBLE);
        editField1.addTextChangedListener(companyChange);
        editField1.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editField2.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editField3.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editMainField.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        filterF3[0] = new InputFilter.LengthFilter(12);
        filterMF[0] = new InputFilter.LengthFilter(12);
        editMainField.setFilters(filterMF);
        editField3.setFilters(filterF3);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                imgBtnField1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        choiceDialog.setDialog(CrudChoices.MANU);
                        choiceDialog.show(editField1);
                    }
                });
                imgBtnField2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            List<Manu> manus = databaseHelper.manus();
                            for (Manu item : manus) {
                                if (item.name.equalsIgnoreCase(editField1.getText().toString())) {
                                    locData.storeManuId(item.getId());
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        choiceDialog.setDialog(CrudChoices.MODEL);
                        choiceDialog.show(editField2);
                    }
                });
                editField1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        choiceDialog.setDialog(CrudChoices.MANU);
                        choiceDialog.show(editField1);
                    }
                });
                editField2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            List<Manu> manus = databaseHelper.manus();
                            for (Manu item : manus) {
                                if (item.name.equalsIgnoreCase(editField1.getText().toString())) {
                                    locData.storeManuId(item.getId());
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        choiceDialog.setDialog(CrudChoices.MODEL);
                        choiceDialog.show(editField2);
                    }
                });
                btnDone.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (emptyField()) {
                            Snackbar.make(findViewById(android.R.id.content), R.string.message_fill_details, Snackbar.LENGTH_SHORT).show();
                            return;
                        }
                        analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(SCREEN_VEHICLE + ACTION_ADD).build());
                        if (!editMainField.getText().toString().matches(".*\\d+.*")) {
                            Snackbar.make(findViewById(android.R.id.content), getString(R.string.message_invalid_entry), Snackbar.LENGTH_SHORT).show();
                            return;
                        }
                        try {
                            locData.clearData();
                            List<Manu> manus = databaseHelper.manus();
                            for (Manu item : manus) {
                                if (item.name.equalsIgnoreCase(editField1.getText().toString())) {
                                    locData.storeManuId(item.getId());
                                    break;
                                }
                            }
                            List<Model> models = databaseHelper.models(Arrays.asList(DatabaseSchema.Models.COLUMN_MANU_ID), new String[]{locData.manuId()});
                            for (Model item : models) {
                                if (item.name.equalsIgnoreCase(editField2.getText().toString())) {
                                    locData.storeModelId(item.getId());
                                    break;
                                }
                            }
                            if (!locData.modelId().equals("0")) {
                                if (login.login() > Login.LoginType.TRIAL) {
                                    if (databaseHelper.addVehicle(editMainField.getText().toString(), editField3.getText().toString(), databaseHelper.user().getId(), locData.modelId())) {
                                        startActivity(new Intent(Create.this, ViewHistory.class));
                                        finish();
                                    }
                                } else {
                                    if (databaseHelper.addTrialVehicle(editMainField.getText().toString(), editField3.getText().toString(), locData.modelId())) {
                                        startActivity(new Intent(Create.this, ViewHistory.class));
                                        finish();
                                    }
                                }
                            } else {
                                if (login.login() > Login.LoginType.TRIAL) {
                                    if (databaseHelper.addVehicle(editMainField.getText().toString(), editField3.getText().toString(), databaseHelper.user().getId())) {
                                        startActivity(new Intent(Create.this, ViewHistory.class));
                                        finish();
                                    }
                                } else {
                                    if (databaseHelper.addTrialVehicle(editMainField.getText().toString(), editField3.getText().toString())) {
                                        startActivity(new Intent(Create.this, ViewHistory.class));
                                        finish();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }, 1);
    }

    private void defineLayoutOptions() {
        layoutOptions.setVisibility(View.VISIBLE);
        imgOption1.setImageResource(R.drawable.refuel_icon1);
        imgOption2.setImageResource(R.drawable.service_icon1);
        txtOption1.setText(R.string.label_refuel);
        txtOption2.setText(R.string.label_service);
    }

    private void refuel() {
        analyticsTracker.setScreenName(SCREEN_REFUEL);
        analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());
        defineLayoutOptions();
        imgOption1.setColorFilter(Color.argb(0, 0, 0, 0));
        imgOption2.setColorFilter(getResources().getColor(R.color.dark_v1));
        txtOption1.setTextColor(getResources().getColor(R.color.refuel_color));
        txtOption2.setTextColor(getResources().getColor(R.color.dark_v1));
        layoutOption1.setAlpha(1f);
        layoutOption2.setAlpha(0.5f);
        try {
            vehicleId = "";
            vehicleId = getIntent().getStringExtra(Constants.Bundle.VEHICLE_ID);
            if (vehicleId == null || vehicleId.isEmpty()) {
                Snackbar.make(findViewById(android.R.id.content), R.string.message_activity_load_error, Snackbar.LENGTH_SHORT).show();
                onBackPressed();
                return;
            }
            locData.storeVId(vehicleId);
        } catch (Exception e) { Snackbar.make(findViewById(android.R.id.content), R.string.message_activity_load_error, Snackbar.LENGTH_SHORT).show(); onBackPressed(); return; }
        hideDetailButtons();
        txtMainField.setText(Html.fromHtml(getString(R.string.label_cost) + "<sup>*</sup>"));
        txtField1.setText(R.string.label_date);
        txtField2.setText(R.string.label_refuel_volume);
        txtField3.setText(R.string.label_refuel_rate);
        txtField4.setText(R.string.label_odometer_reading);
        editMainField.setText("");
        editField1.setText(uiElement.currentDate());
        editField2.setText("");
        editField3.setText("");
        editField4.setText("");
        editField1.setFocusable(false);
        editMainField.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editField1.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE);
        editField2.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editField3.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editField4.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editField2.addTextChangedListener(field2Watcher);
        editField3.addTextChangedListener(field3Watcher);
        editMainField.addTextChangedListener(mainFieldWatcher);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                btnDone.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (emptyField()) {
                            Snackbar.make(findViewById(android.R.id.content), R.string.message_fill_details, Snackbar.LENGTH_SHORT).show();
                            return;
                        }
                        analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(SCREEN_REFUEL + ACTION_ADD).build());
                        if (databaseHelper.addRefuel(vehicleId, locData.longDate(), editField3.getText().toString(), editField2.getText().toString(), editMainField.getText().toString(), editField4.getText().toString()))
                            onBackPressed();
                    }
                });
                editField1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        uiElement.setDatePickerDialog(getString(R.string.label_date), editField1);
                        uiElement.showDatePickerDialog();
                        analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(SCREEN_REFUEL + ACTION_DATE).build());
                    }
                });
            }
        }, 1);
    }

    private void service() {
        analyticsTracker.setScreenName(SCREEN_SERVICE);
        analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());
        if (btnFormControl.getText().toString().equals(getString(R.string.label_expand_form))) {
            layoutPhoto.setVisibility(View.GONE);
        }
        vehicleId = "";
        defineLayoutOptions();
        try {
            vehicleId = getIntent().getStringExtra(Constants.Bundle.VEHICLE_ID);
            if (vehicleId == null || vehicleId.isEmpty()) {
                Snackbar.make(findViewById(android.R.id.content), R.string.message_activity_load_error, Snackbar.LENGTH_SHORT).show();
                onBackPressed();
                return;
            }
            locData.storeVId(vehicleId);
        } catch (Exception e) { Snackbar.make(findViewById(android.R.id.content), R.string.message_activity_load_error, Snackbar.LENGTH_SHORT).show(); onBackPressed(); return; }
        imgOption2.setColorFilter(Color.argb(0, 0, 0, 0));
        imgOption1.setColorFilter(getResources().getColor(R.color.dark_v1));
        txtOption2.setTextColor(getResources().getColor(R.color.service_color));
        txtOption1.setTextColor(getResources().getColor(R.color.dark_v1));
        layoutOption1.setAlpha(0.5f);
        layoutOption2.setAlpha(1f);
        txtMainField.setText(Html.fromHtml(getString(R.string.label_cost) + "<sup>*</sup>"));
        txtField1.setText(R.string.label_date);
        txtField2.setText(R.string.label_workshop);
        txtField3.setText(R.string.label_odometer_reading);
        txtField4.setText(R.string.label_details);
        editMainField.setText("");
        editField1.setText(uiElement.currentDate());
        editField2.setText("");
        editField3.setText("");
        editField4.setText("");
        imgBtnField2.setVisibility(View.VISIBLE);
        editField1.setFocusable(false);
        editMainField.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editField1.setInputType(InputType.TYPE_CLASS_DATETIME|InputType.TYPE_DATETIME_VARIATION_DATE);
        editField2.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editField3.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editField4.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                imgBtnField2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        choiceDialog.setDialog(CrudChoices.WORKSHOP);
                        choiceDialog.show(editField2);
                    }
                });
                editField1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        uiElement.setDatePickerDialog(getString(R.string.label_date), editField1);
                        uiElement.showDatePickerDialog();
                        analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(SCREEN_SERVICE + ACTION_ADD).build());
                    }
                });
                btnAddProblem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LinearLayoutCompat child = (LinearLayoutCompat) getLayoutInflater().inflate(R.layout.child_add_problem, layoutProblems, false);
                        AppCompatTextView textView = (AppCompatTextView) child.findViewById(R.id.text_problem_no);
                        textView.setText(String.valueOf(layoutProblems.getChildCount() + 1));
                        AppCompatSpinner spinner = (AppCompatSpinner) child.findViewById(R.id.spinner_problem_cost);
                        final List<String> cats = new ArrayList<>();
                        cats.add(getString(R.string.option_lcost));
                        cats.add(getString(R.string.option_pcost));
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, android.R.id.text1, cats);
                        spinner.setAdapter(adapter);
                        layoutProblems.addView(child);
                    }
                });
                fabUploadPhoto.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final CharSequence[] items = { "Take new photo", "Choose from existing" };
                        AlertDialog.Builder builder = new AlertDialog.Builder(Create.this);
                        final Snackbar bar = Snackbar.make(findViewById(android.R.id.content), getString(R.string.message_upload_aws_service), Snackbar.LENGTH_INDEFINITE);
                        bar.show();
                        builder.setTitle(getString(R.string.text_upload_dialog_title));
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (items[which].equals("Take new photo")) {
                                    bar.dismiss();
                                    if ((checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED)
                                            && (checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED)) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 19);
                                            return;
                                        }
                                        Snackbar.make(findViewById(android.R.id.content), getString(R.string.message_storage_access_denied), Snackbar.LENGTH_SHORT).show();
                                        return;
                                    }
                                    File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "/" + getString(R.string.directory_cruzer));
                                    if (!folder.exists()) {
                                        if (!folder.mkdir()) {
                                            Snackbar.make(findViewById(android.R.id.content), getString(R.string.message_storage_access_denied), Snackbar.LENGTH_SHORT).show();
                                            return;
                                        }
                                    }
                                    fileName = databaseHelper.user().getsId() + "-" + databaseHelper.vehicle(vehicleId).getsId() + "-" + System.currentTimeMillis();
                                    try {
                                        imageFile = File.createTempFile(fileName, ".jpg", folder);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Snackbar.make(findViewById(android.R.id.content), getString(R.string.message_storage_access_denied), Snackbar.LENGTH_SHORT).show();
                                        return;
                                    }
                                    analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_UPLOAD_PHOTO).build());
                                    startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile)), 24);
                                } else if (items[which].equals("Choose from existing")) {
                                    bar.dismiss();
                                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                    intent.setType("image/*");
                                    analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_EXISTING_PHOTO).build());
                                    startActivityForResult(Intent.createChooser(intent, "Select File"), 34);
                                } else {
                                    dialog.dismiss();
                                    bar.dismiss();
                                }
                            }
                        });
                        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                bar.dismiss();
                            }
                        });
                        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                bar.dismiss();
                            }
                        });
                        builder.show();
                    }
                });
                btnDone.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (emptyField()) {
                            Snackbar.make(findViewById(android.R.id.content), R.string.message_fill_details, Snackbar.LENGTH_SHORT).show();
                            return;
                        }
                        analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(SCREEN_SERVICE + ACTION_ADD).build());
                        try {
                            List<Workshop> workshops = databaseHelper.workshops();
                            boolean workshopPresent = false;
                            for (Workshop item : workshops) {
                                if (item.name.equals(editField2.getText().toString())) {
                                    workshopPresent = true;
                                    locData.storeWorkshopId(item.getId());
                                    break;
                                }
                            }
                            if (!workshopPresent && !editField2.getText().toString().isEmpty()) {
                                databaseHelper.addWorkshop(editField2.getText().toString());
                                locData.storeWorkshopId(databaseHelper.workshop(Arrays.asList(DatabaseSchema.Workshops.COLUMN_NAME), new String[]{editField2.getText().toString()}).getId());
                            }
                            String serviceId = databaseHelper.addService(vehicleId, locData.longDate(), locData.workshopId(), editMainField.getText().toString(), editField3.getText().toString(), editField4.getText().toString());
                            if (serviceId != null) {
                                for (int i = 0; i < layoutProblems.getChildCount(); i++) {
                                    LinearLayoutCompat layoutCompat = (LinearLayoutCompat) layoutProblems.getChildAt(i);
                                    LinearLayoutCompat ll1 = (LinearLayoutCompat) layoutCompat.getChildAt(1);
                                    AppCompatEditText details = (AppCompatEditText) ll1.getChildAt(0);
                                    LinearLayoutCompat layout = (LinearLayoutCompat) ll1.getChildAt(1);
                                    AppCompatEditText cost = (AppCompatEditText) layout.getChildAt(0);
                                    AppCompatSpinner spinner = (AppCompatSpinner) layout.getChildAt(1);
                                    if (spinner.getSelectedItem().equals(getString(R.string.option_lcost)))
                                        databaseHelper.addProblem(serviceId, details.getText().toString(), cost.getText().toString(), "");
                                    else
                                        databaseHelper.addProblem(serviceId, details.getText().toString(), "", cost.getText().toString());
                                }
                                onBackPressed();
                            }
                        } catch (Exception e) {
                            String serviceId = databaseHelper.addService(vehicleId, locData.longDate(), editMainField.getText().toString(), editField3.getText().toString(), editField4.getText().toString());
                            if (serviceId != null) {
                                for (int i = 0; i < layoutProblems.getChildCount(); i++) {
                                    LinearLayoutCompat layoutCompat = (LinearLayoutCompat) layoutProblems.getChildAt(i);
                                    AppCompatEditText details = (AppCompatEditText) layoutCompat.getChildAt(0);
                                    LinearLayoutCompat layout = (LinearLayoutCompat) layoutCompat.getChildAt(1);
                                    AppCompatEditText cost = (AppCompatEditText) layout.getChildAt(0);
                                    AppCompatSpinner spinner = (AppCompatSpinner) layout.getChildAt(1);
                                    if (spinner.getSelectedItem().equals(getString(R.string.option_lcost)))
                                        databaseHelper.addProblem(serviceId, details.getText().toString(), cost.getText().toString(), "");
                                    else
                                        databaseHelper.addProblem(serviceId, details.getText().toString(), "", cost.getText().toString());
                                }
                                onBackPressed();
                            }
                        }
                    }
                });
            }
        }, 1);
    }

    private boolean emptyField() {
        return (editMainField.getText().toString().isEmpty());
    }
    TextWatcher field2Watcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!editMainField.getText().toString().isEmpty() && !editField2.getText().toString().isEmpty() && editField2.isFocused()) {
                double amount = Double.parseDouble(editMainField.getText().toString());
                double volume = Double.parseDouble(editField2.getText().toString());
                double rate = amount / volume;
                editField3.setText(String.format("%.2f", rate));
            }
            if (editField2.getText().toString().isEmpty() && editField2.isFocused())
                editField3.setText("");
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    TextWatcher field3Watcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!editMainField.getText().toString().isEmpty() && !editField3.getText().toString().isEmpty() && editField3.isFocused()) {
                double amount = Double.parseDouble(editMainField.getText().toString());
                double rate = Double.parseDouble(editField3.getText().toString());
                double volume = amount / rate;
                editField2.setText(String.format("%.2f", volume));
            }
            if (editField3.getText().toString().isEmpty() && editField3.isFocused())
                editField2.setText("");
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    TextWatcher mainFieldWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!editField3.getText().toString().isEmpty() && !editMainField.getText().toString().isEmpty() && editMainField.isFocused()) {
                double amount = Double.parseDouble(editMainField.getText().toString());
                double rate = Double.parseDouble(editField3.getText().toString());
                double volume = amount / rate;
                editField2.setText(String.format("%.2f", volume));
            }
            if (editMainField.getText().toString().isEmpty() && editMainField.isFocused()) {
                editField2.setText("");
                editField3.setText("");
            }
            if (!editField2.getText().toString().isEmpty() && !editMainField.getText().toString().isEmpty() && editMainField.isFocused()) {
                double amount = Double.parseDouble(editMainField.getText().toString());
                double volume = Double.parseDouble(editField2.getText().toString());
                double rate = amount / volume;
                editField3.setText(String.format("%.2f", rate));
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };
    TextWatcher companyChange = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            editField2.setText("");
            locData.storeModelId("");
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    private boolean exit = false;

    @Override
    public void onBackPressed() {
        switch (what) {
            case CrudChoices.VEHICLE:
                if (databaseHelper.vehicleCount() == 0) {
                    if (exit)
                        login.logout();
                    else {
                        Snackbar.make(Create.this.findViewById(android.R.id.content), getString(R.string.message_back_logout), Snackbar.LENGTH_SHORT).show();
                        exit = true;
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                exit = false;
                            }
                        }, 3 * 1000);
                    }
                } else
                    super.onBackPressed();
                break;
            default:
                super.onBackPressed();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 24:
                    amazon.upload(fileName, imageFile);
                    onBackPressed();
                    break;
                case 34:
                    Uri selectedImageUri = data.getData();
                    String[] projection = {MediaStore.MediaColumns.DATA};
                    CursorLoader cursorLoader = new CursorLoader(this, selectedImageUri, projection, null, null, null);
                    Cursor cursor = cursorLoader.loadInBackground();
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                    cursor.moveToFirst();
                    String selectedImagePath = cursor.getString(columnIndex);
                    File file = new File(selectedImagePath);
                    String fileName = file.getName();
                    if (!fileName.startsWith(databaseHelper.user().getsId() + "-"))
                        fileName = databaseHelper.user().getsId() + "-" + databaseHelper.vehicle(vehicleId).getsId() + "-" + System.currentTimeMillis() + ".jpg";
                    amazon.upload(fileName, file);
                    onBackPressed();
                    break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 19:
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                        File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "/" + getString(R.string.directory_cruzer));
                        if (!folder.exists()) {
                            if (!folder.mkdir()) {
                                Snackbar.make(findViewById(android.R.id.content), getString(R.string.message_storage_access_denied), Snackbar.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        fileName = databaseHelper.user().getsId() + "-" + databaseHelper.vehicle(vehicleId).getsId() + "-" + System.currentTimeMillis();
                        try {
                            imageFile = File.createTempFile(fileName, ".jpg", folder);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Snackbar.make(findViewById(android.R.id.content), getString(R.string.message_storage_access_denied), Snackbar.LENGTH_SHORT).show();
                            return;
                        }
                        analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_UPLOAD_PHOTO).build());
                        startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile)), 24);
                    }
                }
                break;
        }
    }
}
