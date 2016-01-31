package com.socketmint.cruzer.crud.create;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatEditText;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.LocData;
import com.socketmint.cruzer.ui.UiElement;

public class Service extends Fragment implements View.OnClickListener {
    private AppCompatEditText editAmount, editOdometer, editDate, editNotes;

    private UiElement uiElement;
    private LocData locData = new LocData();
    private DatabaseHelper databaseHelper;

    private String vehicleId;

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

        uiElement = new UiElement(getActivity());
        locData.formInstance(getActivity());
        databaseHelper = new DatabaseHelper(getActivity().getApplicationContext());

        vehicleId = getArguments().getString(Constants.Bundle.VEHICLE_ID, "");
        if (vehicleId.isEmpty()) {
            Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_init_fail, Snackbar.LENGTH_SHORT).show();
            getActivity().finish();
        }

        initializeViews(view);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        setDefaultValues();
    }

    private void initializeViews(View v) {
        editAmount = (AppCompatEditText) v.findViewById(R.id.edit_amount);
        editOdometer = (AppCompatEditText) v.findViewById(R.id.edit_odometer);
        editDate = (AppCompatEditText) v.findViewById(R.id.edit_date);
        editNotes = (AppCompatEditText) v.findViewById(R.id.edit_notes);

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
                if (databaseHelper.addService(vehicleId, locData.longDate(), editAmount.getText().toString(), editOdometer.getText().toString(), editNotes.getText().toString()) != null)
                    getActivity().onBackPressed();
                break;
        }
    }
}
