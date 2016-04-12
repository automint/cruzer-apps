package com.socketmint.cruzer.crud.create;

/**
 * Fragment for creating an PUC entry for particular vehicle
 * @author ndkcha
 * @since 26
 * @version 26
 */

import android.Manifest;
import android.app.Activity;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.os.Process;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.google.android.gms.analytics.HitBuilders;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.dataholder.insurance.InsuranceCompany;
import com.socketmint.cruzer.dataholder.vehicle.Vehicle;
import com.socketmint.cruzer.manage.Amazon;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.ui.UiElement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PUC extends Fragment implements View.OnClickListener {
    private static final String TAG = "CreatePUC";

    private AppCompatSpinner spinnerVehicle;
    private UiElement uiElement;
    private DatabaseHelper databaseHelper;
    private List<Vehicle> vehicles = new ArrayList<>();
    private List<String> vehicleList = new ArrayList<>();
    private AppCompatEditText editStartDate, editEndDate, editPucNo, editFees, editDetails;
    private String vehicleId;

    public static PUC newInstance(String vehicleId) {
        PUC fragment = new PUC();
        Bundle args = new Bundle();
        args.putString(Constants.Bundle.VEHICLE_ID, vehicleId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_create_puc, container, false);

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

        editPucNo = (AppCompatEditText) v.findViewById(R.id.edit_puc_no);
        editStartDate = (AppCompatEditText) v.findViewById(R.id.edit_puc_start_date);
        editEndDate = (AppCompatEditText) v.findViewById(R.id.edit_puc_end_date);
        editFees = (AppCompatEditText) v.findViewById(R.id.edit_puc_fees);
        editDetails = (AppCompatEditText) v.findViewById(R.id.edit_puc_notes);

        v.findViewById(R.id.button_create_record).setOnClickListener(this);
        v.findViewById(R.id.edit_puc_start_date).setOnClickListener(this);
        v.findViewById(R.id.edit_puc_end_date).setOnClickListener(this);
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
        final ArrayAdapter<String> vehicleAdapter = new ArrayAdapter<>(getActivity().getApplicationContext(), R.layout.spinner_item, R.id.text_spinner_item, vehicleList);
        spinnerVehicle.setAdapter(vehicleAdapter);
        spinnerVehicle.setSelection(vehicleList.indexOf(original));
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.edit_puc_start_date:
                uiElement.datePickerDialog(editStartDate, false);
                break;

            case R.id.edit_puc_end_date:
                uiElement.datePickerDialog(editEndDate, false);
                break;

            case R.id.button_create_record:
                if(editFees.getText().toString().isEmpty() || editFees.getText().toString().equals("0")){
                    Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_fill_amount, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (editPucNo.getText().toString().isEmpty() || editPucNo.getText().toString().equals("0")){
                    Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_fill_puc_no, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if(editStartDate.getText().toString().isEmpty() || editEndDate.getText().toString().isEmpty()){
                    Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_fill_date, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                int index = vehicleList.indexOf(spinnerVehicle.getSelectedItem().toString());
                String fees = editFees.getText().toString().replaceAll("[^0-9.]+","").trim();
                String pucNo = editPucNo.getText().toString().replaceAll("[^0-9.]+", "").trim();
                Log.e(TAG,"vehicle Id : " + vehicles.get(index).getId());
                if(databaseHelper.addPUC(vehicles.get(index).getId(), pucNo, uiElement.date(editStartDate.getText().toString(), uiElement.currentTime()), uiElement.date(editEndDate.getText().toString(), uiElement.currentTime()), fees, editDetails.getText().toString()))
                    getActivity().onBackPressed();
                break;
        }
    }
}
