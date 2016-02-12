package com.socketmint.cruzer.crud.create;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.socketmint.cruzer.CruzerApp;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.crud.ChoiceDialog;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.main.History;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.LocData;

public class Vehicle extends Fragment implements View.OnClickListener {
    private static final String TAG = "AddVehicle ";
    private static final String ACTION_ADD_VEHICLE = "Create Vehicle";
    private static final String ACTION_SELECT_COMPANY = "Choose Vehicle Company";
    private static final String ACTION_SELECT_MODEL = "Choose Vehicle Model";
    private AppCompatEditText editRegistration, editVehicleCompany, editVehicleModel, editVehicleName;

    private ChoiceDialog choiceDialog;
    private DatabaseHelper databaseHelper;
    private LocData locData = new LocData();

    private Tracker analyticsTracker;

    public static Vehicle newInstance() {
        Vehicle fragment = new Vehicle();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_vehicle, container, false);

        analyticsTracker = ((CruzerApp) getActivity().getApplication()).getAnalyticsTracker();

        locData.formInstance(getActivity());
        choiceDialog = new ChoiceDialog(getActivity());
        databaseHelper = new DatabaseHelper(getActivity().getApplicationContext());

        initializeViews(view);

        return view;
    }

    private void initializeViews(View v) {
        editRegistration = (AppCompatEditText) v.findViewById(R.id.edit_registration);
        editVehicleCompany = (AppCompatEditText) v.findViewById(R.id.edit_vehicle_company);
        editVehicleModel = (AppCompatEditText) v.findViewById(R.id.edit_vehicle_model);
        editVehicleName = (AppCompatEditText) v.findViewById(R.id.edit_vehicle_name);

        InputFilter[] lengthFilter = { new InputFilter.AllCaps() };
        editRegistration.setFilters(lengthFilter);

        editVehicleCompany.addTextChangedListener(companyChange);
        editVehicleCompany.setOnClickListener(this);
        editVehicleModel.setOnClickListener(this);
        v.findViewById(R.id.button_create_record).setOnClickListener(this);
        v.findViewById(R.id.button_vehicle_company_list).setOnClickListener(this);
        v.findViewById(R.id.button_vehicle_model_list).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.edit_vehicle_company:
            case R.id.button_vehicle_company_list:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(TAG + ACTION_SELECT_COMPANY).build());
                choiceDialog.chooseManufacturer(editVehicleCompany);
                break;
            case R.id.edit_vehicle_model:
            case R.id.button_vehicle_model_list:
                analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(TAG + ACTION_SELECT_MODEL).build());
                choiceDialog.chooseModel(editVehicleModel);
                break;
            case R.id.button_create_record:
                add();
                break;
        }
    }

    private void add() {
        if (editRegistration.getText().toString().isEmpty()) {
            Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_fill_details, Snackbar.LENGTH_SHORT).show();
            return;
        }
        analyticsTracker.send(new HitBuilders.EventBuilder().setCategory(Constants.GoogleAnalytics.EVENT_CLICK).setAction(ACTION_ADD_VEHICLE).build());
        if (!editRegistration.getText().toString().matches(".*\\d+.*")) {
            Snackbar.make(getActivity().findViewById(android.R.id.content), getString(R.string.message_invalid_registration), Snackbar.LENGTH_SHORT).show();
            return;
        }
        String registration = editRegistration.getText().toString();
        registration = registration.replaceAll("[^0-9a-zA-Z]","");
        if (databaseHelper.addVehicle(registration, editVehicleName.getText().toString(), databaseHelper.user().getId(), locData.modelId())) {
            startActivity(new Intent(getActivity(), History.class));
            getActivity().finish();
        }
    }

    TextWatcher companyChange = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            editVehicleModel.setText("");
            locData.storeModelId("");
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };
}
