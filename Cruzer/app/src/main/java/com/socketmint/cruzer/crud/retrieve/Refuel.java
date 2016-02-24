package com.socketmint.cruzer.crud.retrieve;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutCompat;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.socketmint.cruzer.CruzerApp;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.dataholder.vehicle.Vehicle;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.ui.UiElement;

import java.util.Collections;

public class Refuel extends Fragment {
    private static final String TAG = "RetrieveRefuel";
    private AppCompatTextView textVehicleName, textAmount, textOdometer, textDate, textVolume, textRate;
    private LinearLayoutCompat layoutOdometer, layoutDate, layoutRate, layoutVolume;

    private DatabaseHelper databaseHelper;
    private UiElement uiElement;

    private String id;
    private Tracker analyticsTracker;

    public static Refuel newInstance(String id) {
        Refuel fragment = new Refuel();
        Bundle args = new Bundle();
        args.putString(Constants.Bundle.ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_view_refuel, container, false);

        analyticsTracker = ((CruzerApp) getActivity().getApplication()).getAnalyticsTracker();
        databaseHelper = new DatabaseHelper(getActivity().getApplicationContext());
        uiElement = new UiElement(getActivity());
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
        textVehicleName = (AppCompatTextView) v.findViewById(R.id.text_vehicle_name);
        textAmount = (AppCompatTextView) v.findViewById(R.id.text_amount);
        textOdometer = (AppCompatTextView) v.findViewById(R.id.text_odometer);
        textDate = (AppCompatTextView) v.findViewById(R.id.text_date);
        textVolume = (AppCompatTextView) v.findViewById(R.id.text_refuel_volume);
        textRate = (AppCompatTextView) v.findViewById(R.id.text_refuel_rate);

        layoutDate = (LinearLayoutCompat) v.findViewById(R.id.layout_date);
        layoutOdometer = (LinearLayoutCompat) v.findViewById(R.id.layout_odometer);
        layoutRate = (LinearLayoutCompat) v.findViewById(R.id.layout_refuel_rate);
        layoutVolume = (LinearLayoutCompat) v.findViewById(R.id.layout_refuel_volume);
    }

    private void setContent() {
        com.socketmint.cruzer.dataholder.expense.Refuel refuel = databaseHelper.refuel(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{id});

        textVehicleName.setText(vehicleName(databaseHelper.vehicle(refuel.getVehicleId())));
        textAmount.setText(Html.fromHtml(getString(R.string.text_amount, refuel.cost)));
        textOdometer.setText(Html.fromHtml(getString(R.string.text_odometer, refuel.odo)));
        textDate.setText(Html.fromHtml(uiElement.date(refuel.date)));
        textVolume.setText(Html.fromHtml(getString(R.string.text_volume, refuel.volume)));
        textRate.setText(Html.fromHtml(getString(R.string.text_rate, refuel.rate)));

        layoutOdometer.setVisibility((refuel.odo.isEmpty()) ? View.GONE : View.VISIBLE);
        layoutDate.setVisibility((refuel.date.isEmpty()) ? View.GONE : View.VISIBLE);
        layoutVolume.setVisibility((refuel.volume.isEmpty()) ? View.GONE : View.VISIBLE);
        layoutRate.setVisibility((refuel.rate.isEmpty()) ? View.GONE : View.VISIBLE);
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
