package com.socketmint.cruzer.crud;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutCompat;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.socketmint.cruzer.CruzerApp;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.dataholder.Manu;
import com.socketmint.cruzer.dataholder.Model;
import com.socketmint.cruzer.dataholder.Refuel;
import com.socketmint.cruzer.dataholder.Service;
import com.socketmint.cruzer.dataholder.User;
import com.socketmint.cruzer.dataholder.Vehicle;
import com.socketmint.cruzer.dataholder.Workshop;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.LocData;
import com.socketmint.cruzer.manage.Login;
import com.socketmint.cruzer.manage.sync.ManualSync;
import com.socketmint.cruzer.ui.UserInterface;

import java.util.Arrays;
import java.util.List;

public class Update extends AppCompatActivity {
    private static final String SCREEN_VEHICLE = "UpdateVehicle";
    private static final String SCREEN_REFUEL = "UpdateRefuel";
    private static final String SCREEN_SERVICE = "UpdateService";
    private static final String SCREEN_USER = "UpdateUser";
    private static final String ACTION_ADD = "Done";
    private static final String ACTION_DATE = " Date";
    private static final String ACTION_FORM_EXPAND = " Expand Form";
    private static final String ACTION_FORM_COLLAPSE = " Collapse Form";

    private UserInterface userInterface = UserInterface.getInstance();
    private AppCompatTextView txtMainField, txtField1, txtField2, txtField3, txtField4, txtRetrieveType;
    private AppCompatEditText editMainField, editField1, editField2, editField3, editField4;
    private AppCompatImageButton imgBtnField1, imgBtnField2, imgBtnField3, imgBtnField4;
    private LinearLayoutCompat linearField1, linearField2, linearField3, linearField4;
    private AppCompatButton btnFormControl, btnDone;
    private AppCompatImageButton btnBack;
    private String id;
    private int choice;
    private DatabaseHelper databaseHelper;
    private ChoiceDialog choiceDialog = ChoiceDialog.getInstance();
    private LocData locData = new LocData();
    private Login login = Login.getInstance();
    private ManualSync manualSync = ManualSync.getInstance();

    private Tracker analyticsTracker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_update);

        analyticsTracker = ((CruzerApp) getApplication()).getAnalyticsTracker();

        userInterface.changeActivity(this);
        databaseHelper = new DatabaseHelper(getApplicationContext());
        choiceDialog.initInstance(this);
        locData.formInstance(this);
        login.initInstance(this);

        mapViews();

        id = getIntent().getStringExtra(Constants.Bundle.ID);
        choice = getIntent().getIntExtra(Constants.Bundle.PAGE_CHOICE, 0);
        if (id == null || id.isEmpty() || choice == 0) {
            Snackbar.make(findViewById(android.R.id.content), R.string.message_activity_load_error, Snackbar.LENGTH_SHORT).show();
            onBackPressed();
        }

        locData.clearData();
        controlLayouts(CrudChoices.COLLAPSE_ALL);
        setFields(choice);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void setFields(final int what) {
        hideDetailButtons();
        editField1.setFocusable(true);
        editField2.setFocusable(true);
        btnFormControl.setVisibility(View.VISIBLE);
        editField1.removeTextChangedListener(companyChange);
        editField2.removeTextChangedListener(field2Watcher);
        editField3.removeTextChangedListener(field3Watcher);
        editMainField.removeTextChangedListener(mailFieldWatcher);
        switch (what) {
            case CrudChoices.VEHICLE:
                txtRetrieveType.setText(R.string.label_vehicle);
                vehicle();
                break;
            case CrudChoices.REFUEL:
                txtRetrieveType.setText(R.string.label_refuel);
                refuel();
                break;
            case CrudChoices.SERVICE:
                txtRetrieveType.setText(R.string.label_service);
                service();
                break;
            case CrudChoices.USER:
                txtRetrieveType.setText(getString(R.string.label_user));
                user();
                break;
        }
        btnFormControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnFormControl.getText().toString().equals(getString(R.string.label_collapse_form))) {
                    controlLayouts(CrudChoices.COLLAPSE_ALL);
                    btnFormControl.setText(R.string.label_expand_form);
                    return;
                }
                if (btnFormControl.getText().toString().equals(getString(R.string.label_expand_form))) {
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
    }

    private void vehicle() {
        analyticsTracker.setScreenName(SCREEN_VEHICLE);
        analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());
        btnFormControl.setVisibility(View.GONE);
        controlLayouts(CrudChoices.EXPAND_VEHICLE);
        Vehicle vehicle = databaseHelper.vehicle(id);
        txtMainField.setText(Html.fromHtml(getString(R.string.label_vehicle_reg) + "<sup>*</sup>"));
        txtField1.setText(getString(R.string.label_vehicle_manu));
        txtField2.setText(getString(R.string.label_vehicle_model));
        txtField3.setText(R.string.label_vehicle_name);
        editField1.addTextChangedListener(companyChange);
        editField1.setFocusable(false);
        editField2.setFocusable(false);
        imgBtnField1.setVisibility(View.VISIBLE);
        imgBtnField2.setVisibility(View.VISIBLE);
        editField1.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editField2.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editField3.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editMainField.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        editField3.setText(vehicle.name);
        try {
            editField1.setText(vehicle.model.manu.name);
            editField2.setText(vehicle.model.name);
        } catch (Exception e) { editField1.setText(""); editField2.setText(""); }
        editMainField.setText(vehicle.reg);
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
                        for (Manu item : databaseHelper.manus()) {
                            if (item.name.equalsIgnoreCase(editField1.getText().toString())) {
                                locData.storeManuId(item.getId());
                                break;
                            }
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
                        for (Manu item : databaseHelper.manus()) {
                            if (item.name.equalsIgnoreCase(editField1.getText().toString())) {
                                locData.storeManuId(item.getId());
                                break;
                            }
                        }
                        choiceDialog.setDialog(CrudChoices.MODEL);
                        choiceDialog.show(editField2);
                    }
                });
                btnDone.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (editMainField.getText().toString().isEmpty() || editField1.getText().toString().isEmpty() || editField2.getText().toString().isEmpty()) {
                            Snackbar.make(Update.this.findViewById(android.R.id.content), R.string.message_fill_details, Snackbar.LENGTH_SHORT).show();
                            return;
                        }
                        analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(SCREEN_VEHICLE + ACTION_ADD).build());
                        try {
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
                            if (databaseHelper.updateVehicle(id, editMainField.getText().toString(), editField3.getText().toString(), locData.modelId()))
                                onBackPressed();
                        } catch (Exception e) {
                            if (databaseHelper.updateVehicle(id, editMainField.getText().toString(), editField3.getText().toString()))
                                onBackPressed();
                        }
                    }
                });
            }
        }, 1);
    }

    private void refuel() {
        analyticsTracker.setScreenName(SCREEN_REFUEL);
        analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());
        Refuel refuel = databaseHelper.refuel(Arrays.asList(DatabaseSchema.COLUMN_ID), new String[]{id});
        try {
            String vehicleId = refuel.getVehicleId();
            locData.storeVId(vehicleId);
        } catch (Exception e) { Snackbar.make(findViewById(android.R.id.content), R.string.message_activity_load_error, Snackbar.LENGTH_SHORT).show(); onBackPressed(); return; }
        txtMainField.setText(Html.fromHtml(getString(R.string.label_cost) + "<sup>*</sup>"));
        txtField1.setText(R.string.label_date);
        txtField2.setText(R.string.label_refuel_volume);
        txtField3.setText(R.string.label_refuel_rate);
        txtField4.setText(R.string.label_odometer_reading);
        editMainField.setText(refuel.cost);
        editField1.setText(userInterface.date(refuel.date));
        editField2.setText(refuel.volume);
        editField3.setText(refuel.rate);
        editField4.setText(refuel.odo);
        editField1.setFocusable(false);
        editMainField.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editField1.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE);
        editField2.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editField3.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editField4.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editField1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(SCREEN_REFUEL + ACTION_DATE).build());
                userInterface.setDatePickerDialog(getString(R.string.label_date), editField1);
                userInterface.showDatePickerDialog();
            }
        });
        editField2.addTextChangedListener(field2Watcher);
        editField3.addTextChangedListener(field3Watcher);
        editMainField.addTextChangedListener(mailFieldWatcher);
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (emptyField()) {
                    analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(SCREEN_REFUEL + ACTION_ADD).build());
                    Snackbar.make(findViewById(android.R.id.content), R.string.message_fill_details, Snackbar.LENGTH_SHORT);
                    return;
                }
                if (databaseHelper.updateRefuel(id, locData.longDate(), editField3.getText().toString(), editField2.getText().toString(), editMainField.getText().toString(), editField4.getText().toString()))
                    onBackPressed();
            }
        });
    }

    private void user() {
        analyticsTracker.setScreenName(SCREEN_USER);
        analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());
        controlLayouts(CrudChoices.EXPAND_USER);
        btnFormControl.setVisibility(View.GONE);
        final User user = databaseHelper.user();
        txtMainField.setText(getString(R.string.label_phone_number));
        txtField1.setText(getString(R.string.label_password));
        txtField2.setText(getString(R.string.hint_edit_first_name));
        txtField3.setText(getString(R.string.hint_edit_last_name));
        txtField4.setText(getString(R.string.label_email));
        editMainField.setText(user.mobile);
        editField1.setText(user.getPassword());
        editField2.setText(user.firstName);
        editField3.setText(user.lastName);
        editField4.setText(user.email);
        imgBtnField1.setVisibility(View.VISIBLE);
        imgBtnField1.setImageResource(R.drawable.ic_visibility_black_24dp);
        imgBtnField1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean isReleased = event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL;
                boolean isPressed = event.getAction() == MotionEvent.ACTION_DOWN;

                if (isReleased)
                    editField1.setInputType(129);
                else if (isPressed)
                    editField1.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                editField1.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
                editField1.setSelection(editField1.length());
                return false;
            }
        });
        editField1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        editField2.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editField3.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editField4.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        editMainField.setFocusable(false);
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editField1.getText().toString().isEmpty()) {
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.message_fill_password), Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (!editField4.getText().toString().isEmpty() && !userInterface.validateEmail(editField4.getText().toString())) {
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.message_invalid_email), Snackbar.LENGTH_SHORT).show();
                    return;
                }
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(SCREEN_USER + ACTION_ADD).build());
                if (databaseHelper.updateUser(user.getId(), editField1.getText().toString(), editField2.getText().toString(), editField3.getText().toString(), editField4.getText().toString())) {
                    manualSync.syncUser(Update.this, user.getId(), editField1.getText().toString(), editField2.getText().toString(), editField3.getText().toString(), editField4.getText().toString());
                    onBackPressed();
                }
            }
        });
    }

    private void service() {
        analyticsTracker.setScreenName(SCREEN_SERVICE);
        analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());
        Service service = databaseHelper.service(Arrays.asList(DatabaseSchema.COLUMN_ID), new String[]{id});
        try {
            String vehicleId = service.getVehicleId();
            locData.storeVId(vehicleId);
        } catch (Exception e) { Snackbar.make(findViewById(android.R.id.content), R.string.message_activity_load_error, Snackbar.LENGTH_SHORT).show(); onBackPressed(); return; }
        txtMainField.setText(Html.fromHtml(getString(R.string.label_cost) + "<sup>*</sup>"));
        txtField1.setText(R.string.label_date);
        txtField2.setText(R.string.label_workshop);
        txtField3.setText(R.string.label_odometer_reading);
        txtField4.setText(R.string.label_details);
        editMainField.setText(service.cost);
        editField1.setText(userInterface.date(service.date));
        try {
            editField2.setText(databaseHelper.workshop(Arrays.asList(DatabaseSchema.COLUMN_ID), new String[]{service.getWorkshopId()}).name);
        } catch (Exception e) { editField2.setText(""); }
        editField3.setText(service.odo);
        editField4.setText(service.details);
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
                        analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(SCREEN_SERVICE + ACTION_DATE).build());
                        userInterface.setDatePickerDialog(getString(R.string.label_date), editField1);
                        userInterface.showDatePickerDialog();
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
                            for (Workshop item : workshops) {
                                if (item.name.equals(editField2.getText().toString())) {
                                    locData.storeWorkshopId(item.getId());
                                    break;
                                }
                            }
                            if (databaseHelper.updateService(id, locData.longDate(), locData.workshopId(), editMainField.getText().toString(), editField3.getText().toString(), editField4.getText().toString()))
                                onBackPressed();
                        } catch (Exception e) {
                            if (databaseHelper.updateService(id, locData.longDate(), editMainField.getText().toString(), editField3.getText().toString(), editField4.getText().toString()))
                                onBackPressed();
                        }
                    }
                });
            }
        }, 1);
    }

    private boolean emptyField() {
        return (editMainField.getText().toString().isEmpty());
    }

    private void mapViews() {
        txtMainField = (AppCompatTextView) findViewById(R.id.txt_main_field);
        txtField1 = (AppCompatTextView) findViewById(R.id.txt_field_1);
        txtField2 = (AppCompatTextView) findViewById(R.id.txt_field_2);
        txtField3 = (AppCompatTextView) findViewById(R.id.txt_field_3);
        txtField4 = (AppCompatTextView) findViewById(R.id.txt_field_4);
        txtRetrieveType = (AppCompatTextView) findViewById(R.id.txt_retrieve_type);
        editMainField = (AppCompatEditText) findViewById(R.id.edit_main_field);
        editField1 = (AppCompatEditText) findViewById(R.id.edit_field_1);
        editField2 = (AppCompatEditText) findViewById(R.id.edit_field_2);
        editField3 = (AppCompatEditText) findViewById(R.id.edit_field_3);
        editField4 = (AppCompatEditText) findViewById(R.id.edit_field_4);
        linearField1 = (LinearLayoutCompat) findViewById(R.id.linear_field_1);
        linearField2 = (LinearLayoutCompat) findViewById(R.id.linear_field_2);
        linearField3 = (LinearLayoutCompat) findViewById(R.id.linear_field_3);
        linearField4 = (LinearLayoutCompat) findViewById(R.id.linear_field_4);
        imgBtnField1 = (AppCompatImageButton) findViewById(R.id.field_1_list);
        imgBtnField2 = (AppCompatImageButton) findViewById(R.id.field_2_list);
        imgBtnField3 = (AppCompatImageButton) findViewById(R.id.field_3_list);
        imgBtnField4 = (AppCompatImageButton) findViewById(R.id.field_4_list);
        btnFormControl = (AppCompatButton) findViewById(R.id.button_form_control);
        btnDone = (AppCompatButton) findViewById(R.id.button_create_record);
        btnBack = (AppCompatImageButton) findViewById(R.id.button_back);
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
                break;
            case CrudChoices.EXPAND_VEHICLE:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(SCREEN_VEHICLE + ACTION_FORM_EXPAND).build());
                linearField1.setVisibility(View.VISIBLE);
                linearField2.setVisibility(View.VISIBLE);
                linearField3.setVisibility(View.VISIBLE);
                linearField4.setVisibility(View.GONE);
                break;
            case CrudChoices.EXPAND_REFUEL:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(SCREEN_REFUEL + ACTION_FORM_EXPAND).build());
                linearField1.setVisibility(View.VISIBLE);
                linearField2.setVisibility(View.VISIBLE);
                linearField3.setVisibility(View.VISIBLE);
                linearField4.setVisibility(View.VISIBLE);
                break;
            case CrudChoices.EXPAND_SERVICE:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(SCREEN_SERVICE + ACTION_FORM_EXPAND).build());
                linearField1.setVisibility(View.VISIBLE);
                linearField2.setVisibility(View.GONE);
                linearField3.setVisibility(View.VISIBLE);
                linearField4.setVisibility(View.VISIBLE);
                break;
            case CrudChoices.EXPAND_USER:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(SCREEN_USER + ACTION_FORM_EXPAND).build());
                linearField1.setVisibility(View.VISIBLE);
                linearField2.setVisibility(View.VISIBLE);
                linearField3.setVisibility(View.VISIBLE);
                linearField4.setVisibility(View.VISIBLE);
                break;
        }
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

    TextWatcher mailFieldWatcher = new TextWatcher() {
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
}
