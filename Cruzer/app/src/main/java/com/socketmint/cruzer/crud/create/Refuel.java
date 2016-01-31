package com.socketmint.cruzer.crud.create;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.LocData;
import com.socketmint.cruzer.ui.UiElement;

public class Refuel extends Fragment implements View.OnClickListener {
    private AppCompatEditText editAmount, editOdometer, editDate, editVolume, editRate;

    private UiElement uiElement;
    private LocData locData = new LocData();
    private DatabaseHelper databaseHelper;

    private String vehicleId;

    public static Refuel newInstance(String vehicleId) {
        Refuel fragment = new Refuel();
        Bundle args = new Bundle();
        args.putString(Constants.Bundle.VEHICLE_ID, vehicleId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_create_refuel, container, false);

        uiElement = new UiElement(getActivity());
        locData.formInstance(getActivity());
        databaseHelper = new DatabaseHelper(getActivity().getApplicationContext());

        vehicleId = getArguments().getString(Constants.Bundle.VEHICLE_ID, "");
        if (vehicleId.isEmpty()) {
            Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_init_fail, Snackbar.LENGTH_SHORT).show();
            getActivity().finish();
        }

        initializeView(view);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        setDefaultValues();
    }

    private void initializeView(View v) {
        editAmount = (AppCompatEditText) v.findViewById(R.id.edit_amount);
        editOdometer = (AppCompatEditText) v.findViewById(R.id.edit_odometer);
        editDate = (AppCompatEditText) v.findViewById(R.id.edit_date);
        editVolume = (AppCompatEditText) v.findViewById(R.id.edit_volume);
        editRate = (AppCompatEditText) v.findViewById(R.id.edit_refuel_rate);

        editRate.addTextChangedListener(rateWatcher);
        editVolume.addTextChangedListener(volumeWatcher);
        editAmount.addTextChangedListener(amountWatcher);

        editDate.setOnClickListener(this);
        v.findViewById(R.id.button_create_record).setOnClickListener(this);
    }

    private void setDefaultValues() {
        editDate.setText(uiElement.currentDate());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.edit_date:
                uiElement.setDatePickerDialog(getString(R.string.label_date), editDate);
                uiElement.showDatePickerDialog();
                break;
            case R.id.button_create_record:
                if (editAmount.getText().toString().isEmpty() || editAmount.getText().toString().equals("0")) {
                    Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_fill_amount, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (databaseHelper.addRefuel(vehicleId, locData.longDate(), editRate.getText().toString(), editVolume.getText().toString(), editAmount.getText().toString(), editOdometer.getText().toString()))
                    getActivity().onBackPressed();
                break;
        }
    }

    TextWatcher volumeWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!editAmount.getText().toString().isEmpty() && !editVolume.getText().toString().isEmpty() && editVolume.isFocused()) {
                double amount = Double.parseDouble(editAmount.getText().toString());
                double volume = Double.parseDouble(editVolume.getText().toString());
                double rate = amount / volume;
                editRate.setText(String.format("%.2f", rate));
            }
            if (editVolume.getText().toString().isEmpty() && editVolume.isFocused())
                editRate.setText("");
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    TextWatcher rateWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!editAmount.getText().toString().isEmpty() && !editRate.getText().toString().isEmpty() && editRate.isFocused()) {
                double amount = Double.parseDouble(editAmount.getText().toString());
                double rate = Double.parseDouble(editRate.getText().toString());
                double volume = amount / rate;
                editVolume.setText(String.format("%.2f", volume));
            }
            if (editRate.getText().toString().isEmpty() && editRate.isFocused())
                editVolume.setText("");
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    TextWatcher amountWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!editRate.getText().toString().isEmpty() && !editAmount.getText().toString().isEmpty() && editAmount.isFocused()) {
                double amount = Double.parseDouble(editAmount.getText().toString());
                double rate = Double.parseDouble(editRate.getText().toString());
                double volume = amount / rate;
                editVolume.setText(String.format("%.2f", volume));
            }
            if (editAmount.getText().toString().isEmpty() && editAmount.isFocused()) {
                editRate.setText("");
                editVolume.setText("");
            }
            if (!editVolume.getText().toString().isEmpty() && !editAmount.getText().toString().isEmpty() && editAmount.isFocused()) {
                double amount = Double.parseDouble(editAmount.getText().toString());
                double volume = Double.parseDouble(editVolume.getText().toString());
                double rate = amount / volume;
                editRate.setText(String.format("%.2f", rate));
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };
}
