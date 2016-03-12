package com.socketmint.cruzer.crud.retrieve;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutCompat;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.dataholder.insurance.InsuranceCompany;
import com.socketmint.cruzer.dataholder.vehicle.Vehicle;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.ui.UiElement;

import java.util.Collections;

/**
 * Fragment to display insurance information in details
 * @author ndkcha
 * @since 26
 * @version 26
 */

public class Insurance extends Fragment {
    private static final String TAG = "ViewInsurance";

    private AppCompatTextView textAmount, textVehicleName, textCompany, textPolicyNo, textStartDate, textEndDate, textDetails;
    private LinearLayoutCompat layoutStartDate, layoutEndDate, layoutDetails, layoutPolicyNumber;

    private DatabaseHelper databaseHelper;
    private UiElement uiElement;
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
        View v = inflater.inflate(R.layout.fragment_view_insurance, container, false);

        databaseHelper = new DatabaseHelper(getActivity().getApplicationContext());
        uiElement = new UiElement(getActivity());
        id = getArguments().getString(Constants.Bundle.ID, "");

        initializeViews(v);
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        setContent();
    }

    private void initializeViews(View v) {
        textAmount = (AppCompatTextView) v.findViewById(R.id.text_amount);
        textVehicleName = (AppCompatTextView) v.findViewById(R.id.text_vehicle_name);
        textCompany = (AppCompatTextView) v.findViewById(R.id.text_insurance_company);
        textPolicyNo = (AppCompatTextView) v.findViewById(R.id.text_policy_number);
        textStartDate = (AppCompatTextView) v.findViewById(R.id.text_start_date);
        textEndDate = (AppCompatTextView) v.findViewById(R.id.text_end_date);
        textDetails = (AppCompatTextView) v.findViewById(R.id.text_notes);

        layoutStartDate = (LinearLayoutCompat) v.findViewById(R.id.layout_start_date);
        layoutEndDate = (LinearLayoutCompat) v.findViewById(R.id.layout_end_date);
        layoutDetails = (LinearLayoutCompat) v.findViewById(R.id.layout_notes);
        layoutPolicyNumber = (LinearLayoutCompat) v.findViewById(R.id.layout_policy_number);
    }

    private void  setContent() {
        com.socketmint.cruzer.dataholder.insurance.Insurance insurance = databaseHelper.insurance(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{id});

        textVehicleName.setText(vehicleName(databaseHelper.vehicle(insurance.getVehicleId())));
        textAmount.setText(getString(R.string.text_amount, insurance.premium));
        InsuranceCompany insuranceCompany = databaseHelper.insuranceCompany(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{insurance.getCompanyId()});
        String company = (insuranceCompany == null) ? "" : getString(R.string.text_workshop_name, insuranceCompany.company);
        textCompany.setText(company);
        textPolicyNo.setText(insurance.policyNo);
        textStartDate.setText(uiElement.retrieveDate(insurance.startDate));
        textEndDate.setText(uiElement.retrieveDate(insurance.endDate));
        textDetails.setText(insurance.details);

        layoutStartDate.setVisibility(insurance.startDate.isEmpty() ? View.GONE : View.VISIBLE);
        layoutEndDate.setVisibility(insurance.endDate.isEmpty() ? View.GONE  : View.VISIBLE);
        layoutDetails.setVisibility(insurance.details.isEmpty() ? View.GONE : View.VISIBLE);
        layoutPolicyNumber.setVisibility(insurance.policyNo.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private SpannableString vehicleName(Vehicle vehicle) {
        if (vehicle.name == null) {
            if (vehicle.model != null) {
                String t = vehicle.model.name + ", " + vehicle.model.manu.name;
                SpannableString title = new SpannableString(t);
                title.setSpan(new RelativeSizeSpan(0.7f), vehicle.model.name.length(), title.length(), 0);
                return title;
            } else
                return (new SpannableString(vehicle.reg));
        } else {
            if (vehicle.name.isEmpty()) {
                if (vehicle.model != null) {
                    String t = vehicle.model.name + ", " + vehicle.model.manu.name;
                    SpannableString title = new SpannableString(t);
                    title.setSpan(new RelativeSizeSpan(0.7f), vehicle.model.name.length(), title.length(), 0);
                    return title;
                } else
                    return (new SpannableString(vehicle.reg));
            } else
                return (new SpannableString(vehicle.name));
        }
    }
}
