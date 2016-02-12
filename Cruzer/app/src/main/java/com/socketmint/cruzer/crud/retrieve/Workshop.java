package com.socketmint.cruzer.crud.retrieve;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.socketmint.cruzer.CruzerApp;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.manage.Constants;

import org.json.JSONException;
import org.json.JSONObject;

public class Workshop extends Fragment {
    private static final String TAG = "RetrieveWorkshop";
    private AppCompatTextView textWorkshopName, textManager, textMobile, textAddress, textCity, textArea, textOfferings;

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
        try {
            JSONObject object = new JSONObject(id);
            String name = object.getString(DatabaseSchema.Workshops.COLUMN_NAME);
            String manager = object.getString(DatabaseSchema.Workshops.COLUMN_MANAGER);
            String contact = object.getString(DatabaseSchema.Workshops.COLUMN_CONTACT);
            String address = object.getString(DatabaseSchema.Workshops.COLUMN_ADDRESS);
            String city = object.getString(DatabaseSchema.Workshops.COLUMN_CITY_ID);
            String area = object.getString(DatabaseSchema.Workshops.COLUMN_AREA);
            String offerings = object.getString(DatabaseSchema.Workshops.COLUMN_OFFERINGS);
            analyticsTracker.setScreenName(Constants.GoogleAnalytics.EVENT_WORKSHOP_DISPLAY + " : " + name);
            analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());
            textWorkshopName.setText(name);
            textManager.setText(manager);
            textMobile.setText(contact);
            textAddress.setText(address);
            textCity.setText(city);
            textArea.setText(area);
            textOfferings.setText(offerings);
            v.findViewById(R.id.layout_workshop_manager).setVisibility((manager.isEmpty() || manager.equalsIgnoreCase("null")) ? View.GONE : View.VISIBLE);
            v.findViewById(R.id.layout_workshop_contact).setVisibility((contact.isEmpty() || contact.equalsIgnoreCase("null")) ? View.GONE : View.VISIBLE);
            v.findViewById(R.id.layout_workshop_address).setVisibility((address.isEmpty() || address.equalsIgnoreCase("null")) ? View.GONE : View.VISIBLE);
            v.findViewById(R.id.layout_workshop_city).setVisibility((city.isEmpty() || city.equalsIgnoreCase("null")) ? View.GONE : View.VISIBLE);
            v.findViewById(R.id.layout_workshop_area).setVisibility((area.isEmpty() || area.equalsIgnoreCase("null")) ? View.GONE : View.VISIBLE);
            v.findViewById(R.id.layout_workshop_offerings).setVisibility((offerings.isEmpty() || offerings.equalsIgnoreCase("null")) ? View.GONE : View.VISIBLE);
        } catch (JSONException e) {
            Log.e(TAG, "can not parse workshop json");
            getActivity().onBackPressed();
        }
    }
}
