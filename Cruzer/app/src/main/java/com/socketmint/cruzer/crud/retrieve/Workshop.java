package com.socketmint.cruzer.crud.retrieve;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.socketmint.cruzer.CruzerApp;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.dataholder.City;
import com.socketmint.cruzer.manage.Constants;

import java.util.Collections;

public class Workshop extends Fragment {
    private AppCompatTextView textWorkshopName, textManager, textMobile, textAddress, textCity, textArea, textOfferings;

    private DatabaseHelper databaseHelper;
    private Tracker analyticsTracker;

    public static Workshop newInstance(String id) {
        Workshop fragment = new Workshop();
        Bundle args = new Bundle();
        args.putString(Constants.Bundle.ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_workshop, container, false);

        analyticsTracker = ((CruzerApp) getActivity().getApplication()).getAnalyticsTracker();
        databaseHelper = new DatabaseHelper(getActivity().getApplicationContext());

        String id = getArguments().getString(Constants.Bundle.ID);
        initializeViews(view);
        setDetails(view, id);

        return view;
    }

    private void initializeViews(View v) {
        textWorkshopName = (AppCompatTextView) v.findViewById(R.id.text_workshop_name);
        textManager = (AppCompatTextView) v.findViewById(R.id.text_workshop_manager);
        textMobile = (AppCompatTextView) v.findViewById(R.id.text_workshop_mobile);
        textAddress = (AppCompatTextView) v.findViewById(R.id.text_workshop_address);
        textCity = (AppCompatTextView) v.findViewById(R.id.text_workshop_city);
        textArea = (AppCompatTextView) v.findViewById(R.id.text_workshop_area);
        textOfferings = (AppCompatTextView) v.findViewById(R.id.text_workshop_offerings);
    }

    private void setDetails(View v, String id) {
        com.socketmint.cruzer.dataholder.Workshop workshop = databaseHelper.workshop(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{id});
        City city = databaseHelper.city(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{workshop.getCityId()});
        String cityName = (city != null) ? city.city : "";
        analyticsTracker.setScreenName(Constants.GoogleAnalytics.EVENT_WORKSHOP_DISPLAY + " : " + workshop.name);
        analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());
        textWorkshopName.setText(workshop.name);
        textManager.setText(workshop.manager);
        textMobile.setText(workshop.contact);
        textAddress.setText(workshop.address);
        textCity.setText(cityName);
        textArea.setText(workshop.area);
        textOfferings.setText(workshop.offerings);
        v.findViewById(R.id.layout_workshop_manager).setVisibility((workshop.manager.isEmpty()) ? View.GONE : View.VISIBLE);
        v.findViewById(R.id.layout_workshop_contact).setVisibility((workshop.contact.isEmpty() || workshop.contact.equalsIgnoreCase("null")) ? View.GONE : View.VISIBLE);
        v.findViewById(R.id.layout_workshop_address).setVisibility((workshop.address.isEmpty()) ? View.GONE : View.VISIBLE);
        v.findViewById(R.id.layout_workshop_city).setVisibility((cityName.isEmpty()) ? View.GONE :View.VISIBLE);
        v.findViewById(R.id.layout_workshop_area).setVisibility((workshop.area.isEmpty()) ? View.GONE : View.VISIBLE);
        v.findViewById(R.id.layout_workshop_offerings).setVisibility((workshop.offerings.isEmpty()) ? View.GONE : View.VISIBLE);
    }
}
