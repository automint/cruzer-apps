package com.socketmint.cruzer.crud.update;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.dataholder.insurance.InsuranceCompany;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.ui.UiElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Insurance extends Fragment implements View.OnClickListener{
    private static final String TAG = "CreateInsurance";

    private AppCompatSpinner spinnerInsuranceCompany;
    private UiElement uiElement;
    private DatabaseHelper databaseHelper;
    private List<InsuranceCompany> insuranceCompanies = new ArrayList<>();
    private List<String> companies = new ArrayList<>();
    private AppCompatEditText editPolicyNo, editStartDate, editEndDate, editPremium, editDetails;
    private String vehicleId, uploadVehicleId;
    private AppCompatButton btnAdd;
    private String id;

    public static Insurance newInstance(String id) {
        Insurance fragment = new Insurance();
        Bundle args = new Bundle();
        args.putString(Constants.Bundle.ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_create_insurance, container, false);

        databaseHelper = new DatabaseHelper(getActivity().getApplicationContext());
        uiElement = new UiElement(getActivity());

        id = getArguments().getString(Constants.Bundle.ID, "");
        initializeViews(v);

        return v;
    }

    private void initializeViews(View v) {
        spinnerInsuranceCompany = (AppCompatSpinner) v.findViewById(R.id.spinner_insurance_company);

        editPolicyNo = (AppCompatEditText) v.findViewById(R.id.edit_insurance_policy_no);
        editStartDate = (AppCompatEditText) v.findViewById(R.id.edit_insurance_start_date);
        editEndDate = (AppCompatEditText) v.findViewById(R.id.edit_insurance_end_date);
        editPremium = (AppCompatEditText) v.findViewById(R.id.edit_insurance_premium);
        editDetails = (AppCompatEditText) v.findViewById(R.id.edit_insurance_notes);

        editStartDate.setOnClickListener(this);
        editEndDate.setOnClickListener(this);
        v.findViewById(R.id.button_create_record).setOnClickListener(this);
        v.findViewById(R.id.spinner_vehicle).setVisibility(View.GONE);
    }

    @Override
    public void onStart() {
        super.onStart();
        setContents();
    }

    private void setContents() {
        com.socketmint.cruzer.dataholder.insurance.Insurance insurance = databaseHelper.insurance(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{id});
        insuranceCompanies.clear();
        insuranceCompanies = databaseHelper.insuranceCompanies();
        String currentCompany = "";
        for(InsuranceCompany company : insuranceCompanies){
            companies.add(company.company);
            if (insurance.getCompanyId().equals(company.getId()))
                currentCompany = company.company;
        }
        editPolicyNo.setText(insurance.policyNo);
        editStartDate.setText(uiElement.date(insurance.startDate));
        editEndDate.setText(uiElement.date(insurance.endDate));
        editPremium.setText(insurance.premium);
        editDetails.setText(insurance.details);

        ArrayAdapter<String> companyAdapter = new ArrayAdapter<>(getActivity().getApplicationContext(), R.layout.spinner_item, R.id.text_spinner_item, companies);
        spinnerInsuranceCompany.setAdapter(companyAdapter);
        int index = companies.indexOf(currentCompany);
        spinnerInsuranceCompany.setSelection(index);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.edit_insurance_start_date:
                uiElement.datePickerDialog(editStartDate, false);
                break;

            case R.id.edit_insurance_end_date:
                uiElement.datePickerDialog(editEndDate, false);
                break;

            case R.id.button_create_record:
                if(editPremium.getText().toString().isEmpty() || editPremium.getText().toString().equals("0")){
                    Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_fill_premium, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (editPolicyNo.getText().toString().isEmpty() || editPolicyNo.getText().toString().equals("0")){
                    Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_fill_puc_no, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if(editStartDate.getText().toString().isEmpty() || editEndDate.getText().toString().isEmpty()){
                    Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_fill_date, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if(spinnerInsuranceCompany.getSelectedItem().toString().isEmpty()){
                    Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.message_fill_insurance_company, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                String premium = editPremium.getText().toString().replaceAll("[^0-9.]+","").trim();
                String policyNo = editPolicyNo.getText().toString().replaceAll("[^0-9.]+", "").trim();

                int companyIndex = companies.indexOf(spinnerInsuranceCompany.getSelectedItem().toString());
                Log.e(TAG, "Insurance Company Id : " + insuranceCompanies.get(companyIndex).getId());
                if(databaseHelper.updateInsurance(id, insuranceCompanies.get(companyIndex).getId(), policyNo, uiElement.date(editStartDate.getText().toString(), uiElement.currentTime()), uiElement.date(editEndDate.getText().toString(), uiElement.currentTime()), premium, editDetails.getText().toString()))
                    getActivity().onBackPressed();
                break;
        }
    }
}
