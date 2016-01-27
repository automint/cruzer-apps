package com.socketmint.cruzer.crud.create.vehicle;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.socketmint.cruzer.CruzerApp;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.crud.ChoiceDialog;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.main.ViewHistory;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.LocData;
import com.socketmint.cruzer.ui.UserInterface;

public class VehicleEssential extends Fragment implements View.OnClickListener {
    private static final String TAG = "AddVehicle";
    private static final String ACTION_ADD_VEHICLE = "Add Vehicle";
    private AppCompatEditText editRegistration, editVehicleCompany, editVehicleModel, editVehicleName;

    private UserInterface userInterface = UserInterface.getInstance();
    private ChoiceDialog choiceDialog;
    private LocData locData = new LocData();
    private DatabaseHelper databaseHelper;

    private Tracker analyticsTracker;

    public static VehicleEssential newInstance() {
        VehicleEssential fragment = new VehicleEssential();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_vehicle, container, false);

        analyticsTracker = ((CruzerApp) getActivity().getApplication()).getAnalyticsTracker();

        userInterface.changeActivity(getActivity());
        locData.formInstance(getActivity());
        choiceDialog = new ChoiceDialog(getActivity());
        databaseHelper = new DatabaseHelper(getActivity().getApplicationContext());

        initializeViews(view);

        return view;
    }

    private void initializeViews(View v) {
        ((AppCompatTextView) v.findViewById(R.id.label_registration)).setTypeface(userInterface.font(UserInterface.font.roboto_light));
        ((AppCompatTextView) v.findViewById(R.id.label_vehicle_company)).setTypeface(userInterface.font(UserInterface.font.roboto_light));
        ((AppCompatTextView) v.findViewById(R.id.label_vehicle_model)).setTypeface(userInterface.font(UserInterface.font.roboto_light));
        ((AppCompatTextView) v.findViewById(R.id.label_vehicle_name)).setTypeface(userInterface.font(UserInterface.font.roboto_light));

        editRegistration = (AppCompatEditText) v.findViewById(R.id.edit_registration);
        editVehicleCompany = (AppCompatEditText) v.findViewById(R.id.edit_vehicle_company);
        editVehicleModel = (AppCompatEditText) v.findViewById(R.id.edit_vehicle_model);
        editVehicleName = (AppCompatEditText) v.findViewById(R.id.edit_vehicle_name);
        AppCompatButton buttonDone = (AppCompatButton) v.findViewById(R.id.button_create_record);

        editRegistration.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
        editVehicleCompany.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
        editVehicleModel.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
        editVehicleName.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
        buttonDone.setTypeface(userInterface.font(UserInterface.font.copse));

        InputFilter[] lengthFilter = { new InputFilter.LengthFilter(12) };
        editRegistration.setFilters(lengthFilter);
        editVehicleName.setFilters(lengthFilter);

        buttonDone.setOnClickListener(this);
        editVehicleCompany.setOnClickListener(this);
        editVehicleModel.setOnClickListener(this);
        v.findViewById(R.id.button_vehicle_company_list).setOnClickListener(this);
        v.findViewById(R.id.button_vehicle_model_list).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.edit_vehicle_company:
            case R.id.button_vehicle_company_list:
                choiceDialog.chooseManufecturer(editVehicleCompany);
                break;
            case R.id.edit_vehicle_model:
            case R.id.button_vehicle_model_list:
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
            Snackbar.make(getActivity().findViewById(android.R.id.content), getString(R.string.message_invalid_entry), Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (databaseHelper.addVehicle(editRegistration.getText().toString(), editVehicleName.getText().toString(), databaseHelper.user().getId(), locData.modelId())) {
            startActivity(new Intent(getActivity(), ViewHistory.class));
            getActivity().finish();
        }
    }
}
