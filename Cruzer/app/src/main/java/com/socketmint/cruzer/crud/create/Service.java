package com.socketmint.cruzer.crud.create;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatSpinner;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.socketmint.cruzer.CruzerApp;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.dataholder.vehicle.Vehicle;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.ui.UiElement;

import java.util.ArrayList;
import java.util.List;

public class Service extends Fragment implements View.OnClickListener {
    private static final String TAG = "CreateService ";
    private static final String ACTION_DATE = "Select Date";
    private static final String ACTION_ADD_SERVICE = "Create Service";
    private AppCompatEditText editAmount, editOdometer, editDate, editTime, editNotes;
    private AppCompatSpinner spinner;

    private UiElement uiElement;
    private DatabaseHelper databaseHelper;
    private List<String> list = new ArrayList<>();
    private List<Vehicle> vehicles;

    private String vehicleId;
    private Tracker analyticsTracker;

    public static Service newInstance(String vehicleId) {
        Service fragment = new Service();
        Bundle args = new Bundle();
        args.putString(Constants.Bundle.VEHICLE_ID, vehicleId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_create_service, container, false);

        analyticsTracker = ((CruzerApp) getActivity().getApplication()).getAnalyticsTracker();
        uiElement = new UiElement(getActivity());
        databaseHelper = new DatabaseHelper(getActivity().getApplicationContext());

        vehicleId = getArguments().getString(Constants.Bundle.VEHICLE_ID, "");
        if (vehicleId.isEmpty()) {
            Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_init_fail, Snackbar.LENGTH_SHORT).show();
            getActivity().finish();
            return null;
        }
        adjustVehicleId();
        vehicles = databaseHelper.vehicles();

        initializeViews(view);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        analyticsTracker.setScreenName(TAG.trim());
        analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());
        setDefaultValues();
    }

    private void adjustVehicleId() {
        Vehicle vehicle = databaseHelper.vehicle(vehicleId);
        vehicle = (vehicle != null) ? vehicle : databaseHelper.firstVehicle();
        vehicleId = vehicle.getId();
    }

    private void initializeViews(View v) {
        editAmount = (AppCompatEditText) v.findViewById(R.id.edit_amount);
        editOdometer = (AppCompatEditText) v.findViewById(R.id.edit_odometer);
        editDate = (AppCompatEditText) v.findViewById(R.id.edit_date);
        editTime = (AppCompatEditText) v.findViewById(R.id.edit_time);
        editNotes = (AppCompatEditText) v.findViewById(R.id.edit_notes);
        spinner = (AppCompatSpinner) v.findViewById(R.id.spinner_vehicle);

        editAmount.addTextChangedListener(amountWatcher);
        editOdometer.addTextChangedListener(odometerWatcher);

        editDate.setOnClickListener(this);
        editTime.setOnClickListener(this);
        v.findViewById(R.id.button_create_record).setOnClickListener(this);
    }

    private void setDefaultValues() {
        editDate.setText(uiElement.currentDate());
        editTime.setText(uiElement.currentTime());

        list.clear();
        String original = "";
        for (Vehicle item : vehicles) {
            String string;
            if (item.name == null || item.name.isEmpty()) {
                if (item.model != null)
                    string = item.model.name + ", " + item.model.manu.name;
                else
                    string = item.reg;
            } else
                string = item.name;
            if (item.getId().equals(vehicleId))
                original = string;
            list.add(string);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity().getApplicationContext(), R.layout.spinner_item, R.id.text_spinner_item, list);
        spinner.setAdapter(adapter);
        spinner.setSelection(list.indexOf(original));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.edit_date:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(TAG + ACTION_DATE).build());
                uiElement.datePickerDialog(editDate);
                break;
            case R.id.edit_time:
                uiElement.timePickerDialog(editTime);
                break;
            case R.id.button_create_record:
                if (editAmount.getText().toString().isEmpty() || editAmount.getText().toString().equals("0")) {
                    Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_fill_amount, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_ADD_SERVICE).build());
                int index = list.indexOf(spinner.getSelectedItem().toString());
                String amount = editAmount.getText().toString().replaceAll("[^0-9.]+","").trim();
                String odometer = editOdometer.getText().toString().replaceAll("[^0-9.]+", "").trim();
                if (databaseHelper.addService(vehicles.get(index).getId(), uiElement.date(editDate.getText().toString(), editTime.getText().toString()), amount, odometer, editNotes.getText().toString()) != null)
                    getActivity().onBackPressed();
                break;
        }
    }

    TextWatcher amountWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (!s.toString().startsWith(getString(R.string.text_rupee)) && !s.toString().isEmpty()) {
                String original = s.toString().replaceAll("[^0-9.]+","").trim();
                editAmount.setText((original.isEmpty()) ? "" : getString(R.string.text_rupee).concat(original));
                Selection.setSelection(editAmount.getText(), editAmount.getText().length());
            }
        }
    };

    TextWatcher odometerWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (!s.toString().endsWith(getString(R.string.text_odometer_suffix)) && !s.toString().isEmpty()) {
                String original = s.toString().replaceAll("[^0-9.]+", "").trim();
                editOdometer.setText((original.isEmpty()) ? "" : original.concat(getString(R.string.text_odometer_suffix)));
                Selection.setSelection(editOdometer.getText(), original.length());
            }
        }
    };
}
