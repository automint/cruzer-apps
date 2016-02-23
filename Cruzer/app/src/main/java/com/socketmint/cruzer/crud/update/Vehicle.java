package com.socketmint.cruzer.crud.update;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.socketmint.cruzer.R;
import com.socketmint.cruzer.crud.ChoiceDialog;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.LocData;

public class Vehicle extends Fragment implements View.OnClickListener {
//    private static final String TAG = "UpdateVehicle";
    private AppCompatEditText editReg, editCompany, editModel, editVehicleName;

    private ChoiceDialog choiceDialog;
    private DatabaseHelper databaseHelper;
    private LocData locData = new LocData();

    private String id;

    public static Vehicle newInstance(String id) {
        Vehicle fragment = new Vehicle();
        Bundle args = new Bundle();
        args.putString(Constants.Bundle.ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_create_vehicle, container, false);

        locData.formInstance(getActivity());
        choiceDialog = new ChoiceDialog(getActivity());
        databaseHelper = new DatabaseHelper(getActivity().getApplicationContext());

        id = getArguments().getString(Constants.Bundle.ID, "");

        initializeViews(view);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        setContent();
    }

    private void initializeViews(View v) {
        editReg = (AppCompatEditText) v.findViewById(R.id.edit_registration);
        editCompany = (AppCompatEditText) v.findViewById(R.id.edit_vehicle_company);
        editModel = (AppCompatEditText) v.findViewById(R.id.edit_vehicle_model);
        editVehicleName = (AppCompatEditText) v.findViewById(R.id.edit_vehicle_name);

        InputFilter[] lengthFilter = { new InputFilter.AllCaps() };
        editReg.setFilters(lengthFilter);

        editCompany.addTextChangedListener(companyChange);
        editCompany.setOnClickListener(this);
        editModel.setOnClickListener(this);
        v.findViewById(R.id.button_create_record).setOnClickListener(this);
        v.findViewById(R.id.button_vehicle_company_list).setOnClickListener(this);
        v.findViewById(R.id.button_vehicle_model_list).setOnClickListener(this);
    }

    private void setContent() {
        com.socketmint.cruzer.dataholder.Vehicle vehicle = databaseHelper.vehicle(id);
        editVehicleName.setText(vehicle.name);
        editCompany.setText((vehicle.model != null) ? vehicle.model.manu.name : "");
        editModel.setText((vehicle.model != null) ? vehicle.model.name : "");
        editReg.setText(vehicle.reg);
        locData.storeManuId((vehicle.model != null) ? vehicle.model.getManuId() : "");
        locData.storeModelId(vehicle.getModelId());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.edit_vehicle_company:
            case R.id.button_vehicle_company_list:
                choiceDialog.chooseManufacturer(editCompany);
                break;
            case R.id.edit_vehicle_model:
            case R.id.button_vehicle_model_list:
                if (editCompany.getText().toString().isEmpty()) {
                    Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_select_manu, Snackbar.LENGTH_SHORT).show();
                    break;
                }
                choiceDialog.chooseModel(editModel, true);
                break;
            case R.id.button_create_record:
                if (editReg.getText().toString().isEmpty() || (locData.modelId().isEmpty() && !databaseHelper.vehicle(id).getModelId().isEmpty())) {
                    Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_fill_details, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (!editReg.getText().toString().matches(".*\\d+.*")) {
                    Snackbar.make(getActivity().findViewById(android.R.id.content), getString(R.string.message_invalid_registration), Snackbar.LENGTH_SHORT).show();
                    return;
                }
                String registration = editReg.getText().toString();
                registration = registration.replaceAll("[^0-9a-zA-Z]","");
                if (databaseHelper.updateVehicle(id, registration, editVehicleName.getText().toString(), (editModel.getText().toString().isEmpty() ? "" : locData.modelId()))) {
                    getActivity().onBackPressed();
                }
                break;
        }
    }

    TextWatcher companyChange = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            editModel.setText("");
            locData.storeModelId("");
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };
}
