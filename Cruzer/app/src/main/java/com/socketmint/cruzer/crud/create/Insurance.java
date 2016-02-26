package com.socketmint.cruzer.crud.create;

/**
 * Fragment for creating an insurance entry for particular vehicle
 * @author ndkcha
 * @since 26
 * @version 26
 */

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.dataholder.insurance.InsuranceCompany;
import com.socketmint.cruzer.dataholder.vehicle.*;
import com.socketmint.cruzer.dataholder.vehicle.Vehicle;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.ui.UiElement;

import java.util.ArrayList;
import java.util.List;

public class Insurance extends Fragment implements View.OnClickListener {
    private static final String TAG = "CreateInsurance";

    private AppCompatSpinner spinnerVehicle;

    private DatabaseHelper databaseHelper;
    private List<Vehicle> vehicles = new ArrayList<>();
    private List<String> vehicleList = new ArrayList<>();

    private String vehicleId;

    public static Insurance newInstance(String vehicleId) {
        Insurance fragment = new Insurance();
        Bundle args = new Bundle();
        args.putString(Constants.Bundle.VEHICLE_ID, vehicleId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_create_insurance, container, false);

        databaseHelper = new DatabaseHelper(getActivity().getApplicationContext());

        vehicleId = getArguments().getString(Constants.Bundle.VEHICLE_ID, "");
        if (vehicleId.isEmpty()) {
            Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_init_fail, Snackbar.LENGTH_SHORT).show();
            getActivity().finish();
            return null;
        }

        adjustVehicleId();
        vehicles = databaseHelper.vehicles();

        initializeViews(v);
        setDefaultValues();

        return v;
    }

    private void adjustVehicleId() {
        com.socketmint.cruzer.dataholder.vehicle.Vehicle vehicle = databaseHelper.vehicle(vehicleId);
        vehicle = (vehicle != null) ? vehicle : databaseHelper.firstVehicle();
        vehicleId = vehicle.getId();
    }

    private void initializeViews(View v) {
        spinnerVehicle = (AppCompatSpinner) v.findViewById(R.id.spinner_vehicle);

        v.findViewById(R.id.fab_insurance_photo).setOnClickListener(this);
    }

    private void setDefaultValues() {
        vehicleList.clear();
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
            vehicleList.add(string);
        }
        ArrayAdapter<String> vehicleAdapter = new ArrayAdapter<>(getActivity().getApplicationContext(), R.layout.spinner_item, R.id.text_spinner_item, vehicleList);
        spinnerVehicle.setAdapter(vehicleAdapter);
        spinnerVehicle.setSelection(vehicleList.indexOf(original));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab_insurance_photo:
                int vehicleIndex = vehicleList.indexOf(spinnerVehicle.getSelectedItem().toString());
                String vehicleId = vehicles.get(vehicleIndex).getId();
                Log.d(TAG, "vehicle index = " + vehicleIndex + " | vehicle id = " + vehicleId);
                break;
        }
    }
}
