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
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.ui.UserInterface;

import java.util.Arrays;

public class Workshop extends Fragment {
    private AppCompatTextView textWorkshopName, textManager, textMobile, textAddress, textCity, textArea, textOfferings;

    private UserInterface userInterface = UserInterface.getInstance();
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

        userInterface.changeActivity(getActivity());
        databaseHelper = new DatabaseHelper(getActivity().getApplicationContext());

        String id = getArguments().getString(Constants.Bundle.ID);
        initializeViews(view);
        setDetails(view, id);

        return view;
    }

    private void initializeViews(View v) {
        ((AppCompatTextView) v.findViewById(R.id.label_workshop_manager)).setTypeface(userInterface.font(UserInterface.font.roboto_light_italic));
        ((AppCompatTextView) v.findViewById(R.id.label_workshop_contact)).setTypeface(userInterface.font(UserInterface.font.roboto_light_italic));
        ((AppCompatTextView) v.findViewById(R.id.label_workshop_address)).setTypeface(userInterface.font(UserInterface.font.roboto_light_italic));
        ((AppCompatTextView) v.findViewById(R.id.label_workshop_city)).setTypeface(userInterface.font(UserInterface.font.roboto_light_italic));
        ((AppCompatTextView) v.findViewById(R.id.label_workshop_area)).setTypeface(userInterface.font(UserInterface.font.roboto_light_italic));
        ((AppCompatTextView) v.findViewById(R.id.label_workshop_offerings)).setTypeface(userInterface.font(UserInterface.font.roboto_light_italic));

        textWorkshopName = (AppCompatTextView) v.findViewById(R.id.text_workshop_name);
        textManager = (AppCompatTextView) v.findViewById(R.id.text_workshop_manager);
        textMobile = (AppCompatTextView) v.findViewById(R.id.text_workshop_mobile);
        textAddress = (AppCompatTextView) v.findViewById(R.id.text_workshop_address);
        textCity = (AppCompatTextView) v.findViewById(R.id.text_workshop_city);
        textArea = (AppCompatTextView) v.findViewById(R.id.text_workshop_area);
        textOfferings = (AppCompatTextView) v.findViewById(R.id.text_workshop_offerings);

        textWorkshopName.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
        textManager.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
        textMobile.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
        textAddress.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
        textCity.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
        textArea.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
        textOfferings.setTypeface(userInterface.font(UserInterface.font.roboto_regular));
    }

    private void setDetails(View v, String id) {
        com.socketmint.cruzer.dataholder.Workshop workshop = databaseHelper.workshop(Arrays.asList(DatabaseSchema.COLUMN_ID), new String[]{id});
        analyticsTracker.setScreenName(Constants.GoogleAnalytics.EVENT_WORKSHOP_DISPLAY + " : " + workshop.name);
        analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());
        textWorkshopName.setText(workshop.name);
        textManager.setText(workshop.manager);
        textMobile.setText(workshop.contact);
        textAddress.setText(workshop.address);
        textCity.setText(workshop.city);
        textArea.setText(workshop.area);
        textOfferings.setText(workshop.offerings);
        v.findViewById(R.id.card_workshop_manager).setVisibility((workshop.manager.isEmpty()) ? View.GONE : View.VISIBLE);
        v.findViewById(R.id.card_workshop_contact).setVisibility((workshop.contact.isEmpty() || workshop.contact.equalsIgnoreCase("null")) ? View.GONE : View.VISIBLE);
        v.findViewById(R.id.card_workshop_address).setVisibility((workshop.address.isEmpty()) ? View.GONE : View.VISIBLE);
        v.findViewById(R.id.card_workshop_city).setVisibility((workshop.city.isEmpty()) ? View.GONE :View.VISIBLE);
        v.findViewById(R.id.card_workshop_area).setVisibility((workshop.area.isEmpty()) ? View.GONE : View.VISIBLE);
        v.findViewById(R.id.card_workshop_offerings).setVisibility((workshop.offerings.isEmpty()) ? View.GONE : View.VISIBLE);
    }
}
