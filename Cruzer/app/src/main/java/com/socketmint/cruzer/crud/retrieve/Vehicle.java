package com.socketmint.cruzer.crud.retrieve;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.socketmint.cruzer.CruzerApp;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.manage.Constants;

public class Vehicle extends Fragment {
    private static final String TAG = "RetrieveVehicle";
    private AppCompatTextView textReg, textVehicleName, textCompany, textModel;
    private LinearLayoutCompat layoutVehicleName, layoutCompany, layoutModel;

    private String id;
    private DatabaseHelper databaseHelper;

    private Tracker analyticsTracker;

    public static Vehicle newInstance(String id) {
        Vehicle fragment = new Vehicle();
        Bundle args = new Bundle();
        args.putString(Constants.Bundle.ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_view_vehicle, container, false);

        analyticsTracker = ((CruzerApp) getActivity().getApplication()).getAnalyticsTracker();
        databaseHelper = new DatabaseHelper(getActivity().getApplicationContext());
        id = getArguments().getString(Constants.Bundle.ID, "");

        initializeViews(view);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        analyticsTracker.setScreenName(TAG);
        analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());
        setContent();
    }

    private void initializeViews(View v) {
        textReg = (AppCompatTextView) v.findViewById(R.id.text_vehicle_reg);
        textVehicleName = (AppCompatTextView) v.findViewById(R.id.text_vehicle_name);
        textCompany = (AppCompatTextView) v.findViewById(R.id.text_vehicle_company);
        textModel = (AppCompatTextView) v.findViewById(R.id.text_vehicle_model);

        layoutVehicleName = (LinearLayoutCompat) v.findViewById(R.id.layout_vehicle_name);
        layoutCompany = (LinearLayoutCompat) v.findViewById(R.id.layout_vehicle_company);
        layoutModel = (LinearLayoutCompat) v.findViewById(R.id.layout_vehicle_model);
    }

    private void setContent() {
        com.socketmint.cruzer.dataholder.Vehicle vehicle = databaseHelper.vehicle(id);

        textReg.setText(vehicle.reg);
        textVehicleName.setText(vehicle.name);
        if (vehicle.model != null) {
            textCompany.setText(vehicle.model.manu.name);
            textModel.setText(vehicle.model.name);
            layoutCompany.setVisibility(View.VISIBLE);
            layoutModel.setVisibility(View.VISIBLE);
        } else {
            layoutCompany.setVisibility(View.GONE);
            layoutModel.setVisibility(View.GONE);
        }
        layoutVehicleName.setVisibility((vehicle.name == null || vehicle.name.isEmpty()) ? View.GONE : View.VISIBLE);
    }
}
